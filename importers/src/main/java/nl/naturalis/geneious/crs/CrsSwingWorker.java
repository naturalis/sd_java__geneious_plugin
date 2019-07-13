package nl.naturalis.geneious.crs;

import static com.biomatters.geneious.publicapi.documents.DocumentUtilities.addAndReturnGeneratedDocuments;
import static nl.naturalis.geneious.log.GuiLogger.format;
import static nl.naturalis.geneious.log.GuiLogger.plural;
import static nl.naturalis.geneious.note.NaturalisField.SMPL_REGISTRATION_NUMBER;
import static nl.naturalis.geneious.util.JsonUtil.toJson;
import static nl.naturalis.geneious.util.PreconditionValidator.ALL_DOCUMENTS_IN_SAME_DATABASE;
import static nl.naturalis.geneious.util.PreconditionValidator.AT_LEAST_ONE_DOCUMENT_SELECTED;

import java.util.Collections;
import java.util.List;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;

import nl.naturalis.geneious.NonFatalException;
import nl.naturalis.geneious.PluginSwingWorker;
import nl.naturalis.geneious.StoredDocument;
import nl.naturalis.geneious.csv.InvalidRowException;
import nl.naturalis.geneious.csv.RowSupplier;
import nl.naturalis.geneious.log.GuiLogManager;
import nl.naturalis.geneious.log.GuiLogger;
import nl.naturalis.geneious.note.NaturalisNote;
import nl.naturalis.geneious.util.ImportStats;
import nl.naturalis.geneious.util.Messages.Debug;
import nl.naturalis.geneious.util.Messages.Info;
import nl.naturalis.geneious.util.PreconditionValidator;
import nl.naturalis.geneious.util.StoredDocumentList;
import nl.naturalis.geneious.util.StoredDocumentTable;

/**
 * Manages and coordinates the import of CRS files into Geneious.
 * 
 * @author Ayco Holleman
 */
class CrsSwingWorker extends PluginSwingWorker {

  private static final GuiLogger guiLogger = GuiLogManager.getLogger(CrsSwingWorker.class);

  private final CrsImportConfig cfg;

  CrsSwingWorker(CrsImportConfig cfg) {
    this.cfg = cfg;
  }

  @Override
  protected List<AnnotatedPluginDocument> performOperation() throws NonFatalException {
    int required = AT_LEAST_ONE_DOCUMENT_SELECTED | ALL_DOCUMENTS_IN_SAME_DATABASE;
    PreconditionValidator validator = new PreconditionValidator(cfg.getSelectedDocuments(), required);
    validator.validate();
    guiLogger.info("Loading CRS file " + cfg.getFile().getPath());
    List<String[]> rows = new RowSupplier(cfg).getAllRows();
    int numRows = rows.size() - cfg.getSkipLines();
    guiLogger.info("CRS file contains %s row%s (excluding header rows)", numRows, plural(numRows));
    StoredDocumentTable<String> selectedDocuments = new StoredDocumentTable<>(cfg.getSelectedDocuments(), this::getRegno);
    StoredDocumentList updated = new StoredDocumentList(selectedDocuments.size());
    int good = 0, bad = 0, unused = 0;
    NaturalisNote note;
    for (int i = cfg.getSkipLines(); i < rows.size(); ++i) {
      if((note = createNote(rows, i)) == null) {
        ++bad;
        continue;
      }
      ++good;
      String regno = note.get(SMPL_REGISTRATION_NUMBER);
      Debug.scanningSelectedDocuments(guiLogger, "reg.no", regno);
      StoredDocumentList docs = selectedDocuments.get(regno);
      if(docs == null) {
        Debug.noDocumentsMatchingKey(guiLogger, i + 1);
        ++unused;
      } else {
        Debug.foundDocumensMatchingKey(guiLogger, "CRS file", docs);
        for (StoredDocument doc : docs) {
          if(doc.attach(note)) {
            updated.add(doc);
          } else {
            Debug.noNewValues(guiLogger, "CRS file", "reg.no", regno);
          }
        }
      }
    }

    updated.forEach(StoredDocument::saveAnnotations);
    List<AnnotatedPluginDocument> all = updated.unwrap();
    if(!all.isEmpty()) {
      all = addAndReturnGeneratedDocuments(all, true, Collections.emptyList());
    }

    new ImportStats()
        .rowStats(good, bad, unused)
        .docStats(cfg.getSelectedDocuments().size(), updated.size())
        .print(guiLogger);

    guiLogger.info("UNUSED ROW (explanation): The row's registration number did not");
    guiLogger.info("          correspond to any of the selected documents, but may or");
    guiLogger.info("          may not correspond to other, unselected documents.");
    Info.operationCompletedSuccessfully(guiLogger, "CRS Import");
    return all;
  }

  private NaturalisNote createNote(List<String[]> rows, int rownum) {
    String[] values = rows.get(rownum);
    CrsRow row = new CrsRow(cfg.getColumnNumbers(), values);
    // Convert rownum to user-friendly (one-based) line number
    guiLogger.debugf(() -> format("Line %s: %s", rownum + 1, toJson(values)));
    CrsNoteFactory factory = new CrsNoteFactory(rownum + 1, row);
    try {
      NaturalisNote note = factory.createNote();
      guiLogger.debugf(() -> format("Note created: %s", toJson(note)));
      return note;
    } catch (InvalidRowException e) {
      guiLogger.error(e.getMessage());
      return null;
    }
  }

  private String getRegno(StoredDocument sd) {
    return sd.getNaturalisNote().get(SMPL_REGISTRATION_NUMBER);
  }

  @Override
  protected String getLogTitle() {
    return "CRS Import";
  }
}