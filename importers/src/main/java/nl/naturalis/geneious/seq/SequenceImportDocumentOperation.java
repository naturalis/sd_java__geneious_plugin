package nl.naturalis.geneious.seq;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import javax.swing.JFileChooser;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.plugin.DocumentOperation;
import com.biomatters.geneious.publicapi.plugin.DocumentSelectionSignature;
import com.biomatters.geneious.publicapi.plugin.GeneiousActionOptions;
import com.biomatters.geneious.publicapi.plugin.Options;
import com.biomatters.geneious.publicapi.utilities.GuiUtilities;

import jebl.util.ProgressListener;
import nl.naturalis.geneious.gui.Ab1FastaFileFilter;
import nl.naturalis.geneious.gui.GeneiousGUI;
import nl.naturalis.geneious.gui.ShowDialog;
import nl.naturalis.geneious.gui.log.GuiLogManager;
import nl.naturalis.geneious.gui.log.GuiLogger;
import nl.naturalis.geneious.gui.log.LogSession;
import nl.naturalis.geneious.util.CommonUtils;
import nl.naturalis.geneious.util.RuntimeSettings;

import static nl.naturalis.geneious.Setting.OPERATION_FINISHED;
import static nl.naturalis.geneious.SettingsManager.settingsManager;

/**
 * Framework-plumbing class used to import AB1 and fasta files. Instantiates a {@link SequenceImporter} and lets it do
 * most of the work.
 *
 * @author Ayco Holleman
 */
public class SequenceImportDocumentOperation extends DocumentOperation {

  @SuppressWarnings("unused")
  private static final GuiLogger guiLogger = GuiLogManager.getLogger(SequenceImportDocumentOperation.class);

  public SequenceImportDocumentOperation() {
    super();
  }

  @Override
  public GeneiousActionOptions getActionOptions() {
    return new GeneiousActionOptions("AB1/Fasta Import")
        .setMainMenuLocation(GeneiousActionOptions.MainMenu.Tools, .99990)
        .setInMainToolbar(true, .99990)
        .setInPopupMenu(true, .99990)
        .setAvailableToWorkflows(true);
  }

  @Override
  public List<AnnotatedPluginDocument> performOperation(AnnotatedPluginDocument[] docs, ProgressListener progress, Options options) {
    String lastFinished = (String) settingsManager().get(OPERATION_FINISHED);
    LocalDateTime ldt = LocalDateTime.parse(lastFinished);
    if(ChronoUnit.SECONDS.between(ldt, LocalDateTime.now()) < 120) {
      ShowDialog.waitTimeNotOverYet();
      return null;
    }
    if (CommonUtils.checkTargetFolderNotNull()) {
      JFileChooser fc = newFileChooser();
      if (fc.showOpenDialog(GuiUtilities.getMainFrame()) == JFileChooser.APPROVE_OPTION) {
        RuntimeSettings.INSTANCE.setAb1FastaFolder(fc.getCurrentDirectory());
        try (LogSession session = GuiLogManager.startSession("AB1/Fasta import")) {
          SequenceImporter importer = new SequenceImporter(fc.getSelectedFiles());
          importer.execute();
        }
      }
    }
    String finished = String.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    settingsManager().setAndSave(OPERATION_FINISHED, finished);
    return Collections.emptyList();
  }

  @Override
  public String getHelp() {
    return "Imports one or more AB1/fasta files and parses the file name to create extra search fields";
  }

  @Override
  public DocumentSelectionSignature[] getSelectionSignatures() {
    return new DocumentSelectionSignature[0];
  }

  private static JFileChooser newFileChooser() {
    JFileChooser fc = new JFileChooser(RuntimeSettings.INSTANCE.getAb1FastaFolder());
    fc.setDialogTitle("Choose AB1/fasta files to import");
    fc.setApproveButtonText("Import files");
    fc.setMultiSelectionEnabled(true);
    fc.addChoosableFileFilter(new Ab1FastaFileFilter(true, true));
    fc.addChoosableFileFilter(new Ab1FastaFileFilter(true, false));
    fc.addChoosableFileFilter(new Ab1FastaFileFilter(false, true));
    fc.setAcceptAllFileFilterUsed(false);
    GeneiousGUI.scale(fc, .6, .5, 800, 560);
    return fc;
  }

}
