package nl.naturalis.geneious.crs;

import static com.biomatters.geneious.publicapi.utilities.IconUtilities.getIconsFromJar;

import java.util.List;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.plugin.DocumentOperation;
import com.biomatters.geneious.publicapi.plugin.DocumentOperationException;
import com.biomatters.geneious.publicapi.plugin.DocumentSelectionSignature;
import com.biomatters.geneious.publicapi.plugin.GeneiousActionOptions;
import com.biomatters.geneious.publicapi.plugin.Options;

import jebl.util.ProgressListener;

/**
 * Hooks the CRS Import operation into the Geneious plugin architecture. Informs Geneious how to display and kick off
 * the CRS Import operation.
 * 
 * @author Ayco Holleman
 */
public class CrsDocumentOperation extends DocumentOperation {

  static final String NAME = "CRS Import";
  static final String DESCRIPTION = "Enriches documents with CRS data";

  // Releative position with menu and toolbar
  private static final double menuPos = .0000000000003;
  private static final double toolPos = .9999999999993;

  public CrsDocumentOperation() {
    super();
  }

  /**
   * The method called by Geneious to kick off the CRS Import operation.
   */
  @Override
  public GeneiousActionOptions getActionOptions() {
    return new GeneiousActionOptions(NAME, DESCRIPTION,
        getIconsFromJar(getClass(), "/images/nbc_blue_square.png"))
            .setMainMenuLocation(GeneiousActionOptions.MainMenu.Tools, menuPos)
            .setInMainToolbar(true, toolPos)
            .setInPopupMenu(true, menuPos)
            .setAvailableToWorkflows(true);
  }

  @Override
  public Options getOptions(AnnotatedPluginDocument... docs) throws DocumentOperationException {
    return new CrsImportOptions();
  }

  @Override
  public String getHelp() {
    return DESCRIPTION;
  }

  @Override
  public DocumentSelectionSignature[] getSelectionSignatures() {
    return new DocumentSelectionSignature[0];
  }

  @Override
  public boolean isDocumentGenerator() {
    return false;
  }

  @Override
  public List<AnnotatedPluginDocument> performOperation(AnnotatedPluginDocument[] docs, ProgressListener progress, Options options) {
    CrsImportOptions opts = (CrsImportOptions) options;
    CrsSwingWorker importer = new CrsSwingWorker(opts.configureOperation());
    importer.execute();
    return null;
  }

}
