package nl.naturalis.geneious.crs;

import java.util.List;

import javax.swing.SwingWorker;

import com.biomatters.geneious.publicapi.databaseservice.DatabaseServiceException;

import nl.naturalis.geneious.StoredDocument;
import nl.naturalis.geneious.csv.InvalidRowException;
import nl.naturalis.geneious.csv.RowSupplier;
import nl.naturalis.geneious.gui.log.GuiLogManager;
import nl.naturalis.geneious.gui.log.GuiLogger;
import nl.naturalis.geneious.note.NaturalisNote;
import nl.naturalis.geneious.util.APDList;
import nl.naturalis.geneious.util.StoredDocumentList;
import nl.naturalis.geneious.util.StoredDocumentTable;

import static nl.naturalis.geneious.gui.log.GuiLogger.format;
import static nl.naturalis.geneious.gui.log.GuiLogger.plural;
import static nl.naturalis.geneious.note.NaturalisField.SMPL_REGISTRATION_NUMBER;
import static nl.naturalis.geneious.util.DebugUtil.toJson;

/**
 * Does the actual work of importing a CRS file into Geneious.
 */
class CrsImporter extends SwingWorker<APDList, Void> {

  private static final GuiLogger guiLogger = GuiLogManager.getLogger(CrsImporter.class);

  private final CrsImportConfig cfg;

  CrsImporter(CrsImportConfig cfg) {
    this.cfg = cfg;
  }

  /**
   * Enriches the documents selected within the GUI with data from a CRS file. The rows within the CRS files are matched
   * to the selected documents using the registration number annotation (set during sample sheet import).
   */
  @Override
  protected APDList doInBackground() throws DatabaseServiceException {
    return importCrsFile();
  }

  private APDList importCrsFile() {
    guiLogger.info("Loading CRS file " + cfg.getFile().getPath());
    List<String[]> rows = new RowSupplier(cfg).getAllRows();
    StoredDocumentTable<String> selectedDocuments = new StoredDocumentTable<>(cfg.getSelectedDocuments(), this::getRegno);
    StoredDocumentList updates = new StoredDocumentList(selectedDocuments.size());
    int good = 0, bad = 0, unused = 0;
    NaturalisNote note;
    for (int i = 0; i < rows.size(); ++i) {
      if ((note = createNote(rows, i)) == null) {
        ++bad;
        continue;
      }
      ++good;
      String regno = note.get(SMPL_REGISTRATION_NUMBER);
      guiLogger.debugf(() -> format("Scanning selected documents for reg.no. %s", regno));
      StoredDocumentList docs = selectedDocuments.get(regno);
      if (docs == null) {
        int line = cfg.getLine(i);
        guiLogger.debugf(() -> format("Not found. Row at line %s remains unused", line));
        ++unused;
      } else {
        guiLogger.debugf(() -> format("Found %1$s document%2$s. Updating document%2$s", docs.size(), plural(docs)));
        for (StoredDocument doc : docs) {
          if (doc.attach(note)) {
            updates.add(doc);
          } else {
            String fmt = "Document with reg.no. %s not updated (no new values in CRS file)";
            guiLogger.debugf(() -> format(fmt, regno));
          }
        }
      }
    }
    updates.forEach(StoredDocument::saveAnnotations);
    int selected = cfg.getSelectedDocuments().size();
    int unchanged = selected - updates.size();
    guiLogger.info("Number of valid rows in CRS file .......: %3d", good);
    guiLogger.info("Number of empty/bad rows in CRS file ...: %3d", bad);
    guiLogger.info("Number of unused rows in CRS file ......: %3d", unused);
    guiLogger.info("Number of selected documents ...........: %3d", selected);
    guiLogger.info("Number of updated documents ............: %3d", updates.size());
    guiLogger.info("Number of unchanged documents ..........: %3d", unchanged);
    guiLogger.info("UNUSED ROW (explanation): The row's registration number did not");
    guiLogger.info("          correspond to any of the selected documents, but may or");
    guiLogger.info("          may not correspond to other, unselected documents.");
    return null; // Tells Geneious that we didn't create any new documents.
  }

  private NaturalisNote createNote(List<String[]> rows, int rownum) {
    CrsRow row = new CrsRow(cfg.getColumnNumbers(), rows.get(rownum));
    if (row.isEmpty()) {
      guiLogger.debugf(() -> format("Ignoring empty row at line %s", cfg.getLine(rownum)));
      return null;
    }
    guiLogger.debugf(() -> format("Line %s: %s", cfg.getLine(rownum), toJson(rows.get(rownum))));
    CrsNoteFactory factory = new CrsNoteFactory(cfg.getLine(rownum), row);
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

}
