package nl.naturalis.geneious.csv;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;

import com.biomatters.geneious.publicapi.components.Dialogs.DialogIcon;
import com.biomatters.geneious.publicapi.utilities.GuiUtilities;
import com.google.common.base.Charsets;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import nl.naturalis.geneious.NaturalisPluginException;
import nl.naturalis.geneious.OperationOptions;
import nl.naturalis.geneious.gui.ShowDialog;
import nl.naturalis.geneious.util.CharsetDetector;

import static java.util.Arrays.asList;

import static com.biomatters.geneious.publicapi.components.Dialogs.showMessageDialog;

import static org.apache.commons.io.FilenameUtils.getExtension;

import static nl.naturalis.geneious.csv.CsvImportUtil.isCsvFile;
import static nl.naturalis.geneious.csv.CsvImportUtil.isSpreadsheet;

/**
 * Abstract base class for classes underpinning a Geneious dialog that requests user input for the import of CSV-like files.
 * 
 * @author Ayco Holleman
 *
 * @param <T> An {@code enum} providing symbolic constants for the columns in a CSV-like file.
 * @param <U> The type of object that will contain the user-provided values.
 */
public abstract class CsvImportOptions<T extends Enum<T>, U extends CsvImportConfig<T>> extends OperationOptions<U> {

  private static final String FILE = "nl.naturalis.geneious.%s.file";
  private static final String LINES_TO_SKIP = "nl.naturalis.geneious.%s.skip";
  private static final String DELIMITER = "nl.naturalis.geneious.%s.delim";
  private static final String SHEET_NAME = "nl.naturalis.geneious.%s.sheet";
  private static final String SELECTED_SHEET = "nl.naturalis.geneious.%s.selectedSheet";

  private static final OptionValue NOT_APPLICABLE = new OptionValue("0", "  n/a  ");
  private static final OptionValue DELIM_INIT = new OptionValue("0", "  --- csv/tsv/txt ---  ");
  private static final OptionValue SHEET_INIT = new OptionValue("0", "  --- spreadsheet ---  ");

  private static final List<OptionValue> DELIM_OPTIONS = asList(
      new OptionValue("\t", "  tab  "),
      new OptionValue(",", "  comma  "),
      new OptionValue(";", "  semi-colon  "),
      new OptionValue("|", "  pipe  "));

  // An identifier provided by the subclasses to differentiate the option names.
  private final String identifier;
  private final FileSelectionOption file;
  private final IntegerOption linesToSkip;
  private final ComboBoxOption<OptionValue> delimiter;
  private final ComboBoxOption<OptionValue> sheet;
  private final IntegerOption selectedSheet;

  public CsvImportOptions(String identifier) {
    this.identifier = identifier;
    this.file = addFileSelectionOption();
    this.linesToSkip = addLinesToSkipOption();
    this.delimiter = addDelimiterOption();
    this.sheet = supportSpreadsheet() ? addSheetOption() : null;
    this.selectedSheet = addSelectedSheetOption();
    file.addChangeListener(this::fileChanged);
  }

  /**
   * Verifies the validity of the user input. Returns null if the user input is valid, otherwise a message indicating what's wrong.
   */
  @Override
  public String verifyOptionsAreValid() {
    String msg = super.verifyOptionsAreValid();
    if (msg != null) {
      return msg;
    }
    if (StringUtils.isBlank(file.getValue())) {
      return "Please select a CSV file or spreadsheet to import";
    }
    String ext = getExtension(file.getValue());
    if (!supportedFileTypes().contains(ext.toLowerCase())) {
      String fmt = "Unsupported file type: %s. Supported file types: %s";
      return String.format(fmt, ext, supportedFileTypesAsString());
    }
    if (isCsvFile(file.getValue())) {
      try {
        Charset charset = CharsetDetector.detectEncoding(Paths.get(file.getValue()));
        if (charset.equals(Charsets.UTF_8)) {
          return null;
        }
        String fileName = FilenameUtils.getName(file.getValue());
        if (ShowDialog.continueWithDetectedCharset(fileName, charset)) {
          return null;
        }
        return "Please select another file";
      } catch (IOException e) {
        throw new NaturalisPluginException(e);
      }
    }
    return null; // Signals to Geneious it can continue
  }

  /**
   * Initializes the provided configuration object with settings common to all operations that import CSV or CSV-like files (Sample Sheet
   * Import, CRS Import and BOLD Import), for example the field delimiter.
   * 
   * @param config
   * @return
   */
  @Override
  protected U configureDefaults(U config) {
    super.configureDefaults(config);
    config.setFile(new File(file.getValue()));
    config.setSkipLines(linesToSkip.getValue());
    config.setDelimiter(delimiter.getValue().getName());
    if (supportSpreadsheet()) {
      config.setSheetNumber(Integer.parseInt(sheet.getValue().getName()));
      config.setSpreadsheetWithFormulas(isSpreadsheetWithFormulas());
    }
    return config;
  }

  /**
   * Returns the current value of the "Lines to skip" field in the options panel.
   * 
   * @return
   */
  protected final int getLinesToSkip() {
    return linesToSkip.getValue();
  }

  /**
   * The text to display before the file selection field in the dialog. Default: "File".
   * 
   * @return
   */
  protected String getDefaultFileSelectionLabel() {
    return "File";
  }

  /**
   * Returns the default number of lines to skip (displayed when the dialog is opened for the very first time).
   * 
   * @return
   */
  protected int getDefaultNumLinesToSkip() {
    return 1;
  }

  /**
   * Whether or not to support spreadsheets (default: {@code false}). Can be overriden by subclasses.
   * 
   * @return
   */
  protected boolean supportSpreadsheet() {
    return false;
  }

  /**
   * Whether or not the spreadsheet (if applicable) may contain formulas. (default: {@code false}). This is currently a hard-coded value
   * (either here or in the subclasses), but we might have to make it dependent on user input, so that's why we include this setting here
   * rather than in {@link CsvImportConfig}. Can be overriden by subclasses.
   * 
   * @return
   */
  protected boolean isSpreadsheetWithFormulas() {
    return false;
  }

  private FileSelectionOption addFileSelectionOption() {
    FileSelectionOption opt = addFileSelectionOption(
        name(FILE),
        getDefaultFileSelectionLabel(),
        "",
        new String[0],
        "Select",
        (x, y) -> supportedFileTypes().contains(getExtension(y)));
    opt.setAllowMultipleSelection(false);
    opt.setFillHorizontalSpace(true);
    opt.setSelectionType(JFileChooser.FILES_ONLY);
    opt.setValue("");
    opt.setDescription("Select a sample sheet to import. Supported formats: " + supportedFileTypesAsString());
    return opt;
  }

  private IntegerOption addLinesToSkipOption() {
    String descr = "The number of lines to skip within the selected file";
    IntegerOption opt = addIntegerOption(name(LINES_TO_SKIP), "Lines to skip", getDefaultNumLinesToSkip(), 0, Integer.MAX_VALUE);
    opt.setDescription(descr);
    return opt;
  }

  private ComboBoxOption<OptionValue> addDelimiterOption() {
    ComboBoxOption<OptionValue> opt = addComboBoxOption(name(DELIMITER), "Field separator", asList(DELIM_INIT), DELIM_INIT);
    opt.setDescription("The character used to separate values within a row");
    opt.setEnabled(false);
    return opt;
  }

  private ComboBoxOption<OptionValue> addSheetOption() {
    ComboBoxOption<OptionValue> opt = addComboBoxOption(name(SHEET_NAME), "Sheet name", asList(SHEET_INIT), SHEET_INIT);
    opt.setFillHorizontalSpace(true);
    opt.setDescription("The name of the sheet (a.k.a. tab) within the spreadsheet.");
    opt.setEnabled(false);
    opt.addChangeListener(() -> {
      OptionValue v = opt.getPossibleOptionValues().get(0);
      if (v != SHEET_INIT && v != NOT_APPLICABLE) {
        selectedSheet.setValue(Integer.valueOf(opt.getValueAsString()));
      }
    });
    return opt;
  }

  private IntegerOption addSelectedSheetOption() {
    IntegerOption opt = addIntegerOption(SELECTED_SHEET, "", -1);
    opt.setHidden();
    return opt;
  }

  private void fileChanged() {
    if (StringUtils.isBlank(file.getValue())) {
      /*
       * When a file has been selected, and then you select another file, the change listener apparently fires twice. The first time the
       * file is empty again. The second time you get the new file.
       */
      return;
    }
    if (supportSpreadsheet()) { // must check that, otherwise sheet is null
      sheet.setEnabled(false);
    }
    delimiter.setEnabled(false);
    if (supportSpreadsheet() && isSpreadsheet(file.getValue())) {
      loadSheetNames();
      sheet.setEnabled(true);
      delimiter.setPossibleValues(asList(NOT_APPLICABLE));
      delimiter.setDefaultValue(NOT_APPLICABLE);
    } else if (CsvImportUtil.isCsvFile(file.getValue())) {
      delimiter.setPossibleValues(DELIM_OPTIONS);
      delimiter.setDefaultValue(DELIM_OPTIONS.get(0));
      delimiter.setEnabled(true);
      if (supportSpreadsheet()) {
        sheet.setPossibleValues(asList(NOT_APPLICABLE));
        sheet.setDefaultValue(NOT_APPLICABLE);
      }
    } else {
      if (supportSpreadsheet()) {
        sheet.setPossibleValues(asList(SHEET_INIT));
        sheet.setDefaultValue(SHEET_INIT);
      }
      delimiter.setPossibleValues(asList(DELIM_INIT));
      delimiter.setDefaultValue(DELIM_INIT);
      String msg = "Unsupported file type";
      showMessageDialog(msg, msg, GuiUtilities.getMainFrame(), DialogIcon.ERROR);
    }
  }

  private void loadSheetNames() {
    try {
      String[] sheetNames = SpreadSheetReader.getSheetNames(new File(file.getValue()));
      List<OptionValue> options = new ArrayList<>(sheetNames.length);
      OptionValue selected = null;
      for (int i = 0; i < sheetNames.length; ++i) {
        String label = sheetNames[i];
        OptionValue opt = new OptionValue(String.valueOf(i), "  " + label + "  ");
        options.add(opt);
        if (i == selectedSheet.getValue()) {
          selected = opt;
        }
      }
      sheet.setPossibleValues(options);
      if (selected == null) {
        sheet.setValue(options.get(0));
      } else {
        sheet.setValue(selected);
      }
    } catch (Exception e) {
      String title = "Error reading spreadsheet";
      String msg = title + ": " + e;
      showMessageDialog(msg, title, GuiUtilities.getMainFrame(), DialogIcon.ERROR);
    }
  }

  private String name(String format) {
    return String.format(format, identifier);
  }

  private ArrayList<String> supportedFileTypes() {
    ArrayList<String> types = new ArrayList<>(8);
    types.add("csv");
    types.add("tsv");
    types.add("txt");
    if (supportSpreadsheet()) {
      types.addAll(CsvImportUtil.spreadSheetFileExts);
    }
    return types;
  }

  private String supportedFileTypesAsString() {
    return supportedFileTypes().stream().map(s -> "*." + s).collect(Collectors.joining("  "));
  }

}
