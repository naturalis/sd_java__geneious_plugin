package nl.naturalis.geneious.smpl;

import java.util.List;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;

import nl.naturalis.geneious.csv.RowSupplierConfig;

/**
 * Stores the user input provided via a Swing dialog.
 *
 * @author Ayco Holleman
 */
public class SampleSheetImportConfig extends RowSupplierConfig {

  private boolean createDummies;

  SampleSheetImportConfig(List<AnnotatedPluginDocument> selectedDocuments) {
    super(selectedDocuments);
  }

  boolean isCreateDummies() {
    return createDummies;
  }

  void setCreateDummies(boolean createDummies) {
    this.createDummies = createDummies;
  }

}