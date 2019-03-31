package nl.naturalis.geneious.smpl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.SwingWorker;

import com.biomatters.geneious.publicapi.databaseservice.DatabaseServiceException;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;

import org.apache.commons.lang3.StringUtils;

import nl.naturalis.geneious.csv.InvalidRowException;
import nl.naturalis.geneious.csv.RowSupplier;
import nl.naturalis.geneious.gui.log.GuiLogManager;
import nl.naturalis.geneious.gui.log.GuiLogger;
import nl.naturalis.geneious.note.NaturalisNote;
import nl.naturalis.geneious.seq.DummySequenceDocument;
import nl.naturalis.geneious.util.APDList;
import nl.naturalis.geneious.util.DebugUtil;
import nl.naturalis.geneious.util.StoredDocument;
import nl.naturalis.geneious.util.StoredDocumentList;
import nl.naturalis.geneious.util.StoredDocumentTable;

import static java.util.function.Predicate.not;

import static nl.naturalis.geneious.gui.log.GuiLogger.format;
import static nl.naturalis.geneious.util.QueryUtils.findByExtractID;
import static nl.naturalis.geneious.util.QueryUtils.getTargetDatabaseName;

/**
 * Does the actual work of importing a sample sheet into Geneious.
 */
class SampleSheetImporter extends SwingWorker<APDList, Void> {

  private static final GuiLogger guiLogger = GuiLogManager.getLogger(SampleSheetImporter.class);

  private final SampleSheetImportConfig cfg;

  SampleSheetImporter(SampleSheetImportConfig cfg) {
    this.cfg = cfg;
  }

  /**
   * Enriches the documents selected within the GUI with data from the sample sheet. Documents and sample sheet records
   * are linked using their extract ID. In addition, if requested, this routine will create dummy documents from sample
   * sheet records if their extract ID does not exist yet.
   */
  @Override
  protected APDList doInBackground() throws DatabaseServiceException {
    return importSampleSheet();
  }

  private APDList importSampleSheet() throws DatabaseServiceException {
    if (cfg.isCreateDummies()) {
      return updateOrCreateDummies();
    }
    return updateOnly();
  }

  private APDList updateOrCreateDummies() throws DatabaseServiceException {
    guiLogger.info("Loading sample sheet " + cfg.getFile().getPath());
    List<String[]> rows = new RowSupplier(cfg).getAllRows();
    guiLogger.info("Collecting extract IDs");
    Set<String> extractIds = collectExtractIds(rows);
    StoredDocumentTable selected = new StoredDocumentTable(cfg.getSelectedDocuments());
    guiLogger.info("Searching database %s for matching documents", getTargetDatabaseName());
    Set<String> searchFor = extractIds.stream()
        .filter(not(selected::containsKey))
        .collect(Collectors.toSet());
    StoredDocumentTable unselected = new StoredDocumentTable(findByExtractID(searchFor));
    int numNewExtractIds = extractIds.size() - selected.keySet().size() - unselected.keySet().size();
    guiLogger.info("Sample sheet contains %s new extract ID(s)", numNewExtractIds);
    APDList updates = new APDList();
    APDList dummies = new APDList();
    int good = 0, bad = 0, updatedDummies = 0, unused = 0;
    for (int i = 0; i < rows.size(); ++i) {
      final int rownum = i;
      guiLogger.debugf(() -> format("Processing row: %s", DebugUtil.toJson(rows.get(rownum))));
      SampleSheetRow row = new SampleSheetRow(userFriendly(i), rows.get(i));
      if (row.isEmpty()) {
        guiLogger.debugf(() -> format("Ignoring empty row at line %s", userFriendly(rownum)));
        ++bad;
        continue;
      }
      NaturalisNote note;
      try {
        note = row.extractNote();
      } catch (InvalidRowException e) {
        guiLogger.error(e.getMessage());
        ++bad;
        continue;
      }
      guiLogger.debugf(() -> format("Note created: %s", DebugUtil.toJson(note)));
      ++good;
      String id = note.getExtractId();
      guiLogger.debugf(() -> format("Scanning selected documents for extract ID %s", id));
      StoredDocumentList docs0 = selected.get(id);
      if (docs0 == null) {
        guiLogger.debug(() -> "Not found. Scanning query cache for unselected documents");
        StoredDocumentList docs1 = unselected.get(id);
        if (docs1 == null) {
          guiLogger.debugf(() -> format("Not found. Creating dummy document for extract ID %s", id));
          dummies.add(createDummy(note));
        } else {
          handleUnselectedDocument(docs1, i);
          ++unused;
        }
      } else {
        String fmt0 = "Found. Updating %s selected document(s) with data from row %s";
        guiLogger.debugf(() -> format(fmt0, docs0.size(), userFriendly(rownum)));
        for (StoredDocument doc : docs0) {
          if (note.saveTo(doc)) {
            if (doc.isDummy()) {
              ++updatedDummies;
              guiLogger.debug(() -> "Updating dummy document!");
            }
          } else {
            String fmt1 = "Document with extract ID %s was not updated because sample sheet contained no new values";
            guiLogger.debugf(() -> format(fmt1, id));
          }
        }
      }
    }
    int numSelected = cfg.getSelectedDocuments().size();
    int numUpdates = updates.size();
    int newDummies = dummies.size();
    int numUnchanged = numSelected - numUpdates - updatedDummies;
    guiLogger.info("Number of valid rows in sample sheet .......: %3d", good);
    guiLogger.info("Number of empty/bad rows in sample sheet ...: %3d", bad);
    guiLogger.info("Number of unused rows in sample sheet ......: %3d", unused);
    guiLogger.info("Number of selected documents ...............: %3d", numSelected);
    guiLogger.info("Number of unchanged documents ..............: %3d", numUnchanged);
    guiLogger.info("Number of updated documents ................: %3d", numUpdates);
    guiLogger.info("Number of updated dummies ..................: %3d", updatedDummies);
    guiLogger.info("Number of dummy documents created ..........: %3d", newDummies);
    guiLogger.info("UNUSED ROW: The row's extract ID was found in an existing");
    guiLogger.info("            document, but the  document was not selected");
    guiLogger.info("            and therefore not updated.");
    guiLogger.info("Import type: update existing documents or create dummies");
    guiLogger.info("Import completed successfully");
    updates.addAll(dummies);
    return updates;
  }

  private APDList updateOnly() {
    guiLogger.info("Loading sample sheet " + cfg.getFile().getPath());
    List<String[]> rows = new RowSupplier(cfg).getAllRows();
    StoredDocumentTable selectedDocuments = new StoredDocumentTable(cfg.getSelectedDocuments());
    int numSelected = cfg.getSelectedDocuments().size();
    APDList updates = new APDList(numSelected);
    int good = 0, bad = 0, unused = 0;
    for (int i = 1; i < rows.size(); ++i) {
      final int rownum = i;
      guiLogger.debugf(() -> format("Processing row: %s", DebugUtil.toJson(rows.get(rownum))));
      SampleSheetRow row = new SampleSheetRow(userFriendly(i), rows.get(i));
      if (row.isEmpty()) {
        guiLogger.debugf(() -> format("Ignoring empty row at line %s", userFriendly(rownum)));
        ++bad;
        continue;
      }
      NaturalisNote note;
      try {
        note = row.extractNote();
      } catch (InvalidRowException e) {
        guiLogger.error(e.getMessage());
        ++bad;
        continue;
      }
      guiLogger.debugf(() -> format("Note created: %s", DebugUtil.toJson(note)));
      ++good;
      String id = note.getExtractId();
      guiLogger.debugf(() -> format("Scanning selected documents for extract ID %s", id));
      StoredDocumentList docs = selectedDocuments.get(id);
      if (docs == null) {
        guiLogger.debugf(() -> format("Not found. Row %s remains unused", userFriendly(rownum)));
        ++unused;
      } else {
        for (StoredDocument doc : docs) {
          if (note.saveTo(doc)) {
            guiLogger.debugf(() -> format("Updating document with extract ID %s", id));
            updates.add(doc.getGeneiousDocument());
          } else {
            String fmt = "Document with extract ID %s not updated (no new values in sample sheet)";
            guiLogger.debugf(() -> format(fmt, id));
          }
        }
      }
    }
    int numUpdates = updates.size();
    int numUnchanged = numSelected - numUpdates;
    guiLogger.info("Number of valid rows in sample sheet .......: %3d", good);
    guiLogger.info("Number of empty/bad rows in sample sheet ...: %3d", bad);
    guiLogger.info("Number of unused rows in sample sheet ......: %3d", unused);
    guiLogger.info("Number of selected documents ...............: %3d", numSelected);
    guiLogger.info("Number of updated documents ................: %3d", numUpdates);
    guiLogger.info("Number of unchanged documents ..............: %3d", numUnchanged);
    guiLogger.info("UNUSED ROW: The row's extract ID did not correspond to any");
    guiLogger.info("            of the selected documents.");
    guiLogger.info("Import type: update existing documents; do not create dummies");
    guiLogger.info("Import completed successfully");
    return updates;
  }

  private static Set<String> collectExtractIds(List<String[]> rows) {
    int colno = SampleSheetRow.COLNO_EXTRACT_ID;
    return rows.stream()
        .filter(row -> colno < row.length)
        .filter(row -> StringUtils.isNotBlank(row[colno]))
        .map(row -> "e" + row[colno])
        .collect(Collectors.toSet());
  }

  private static AnnotatedPluginDocument createDummy(NaturalisNote note) {
    AnnotatedPluginDocument apd = new DummySequenceDocument(note).wrap();
    apd.save();
    return apd;
  }

  private void handleUnselectedDocument(StoredDocumentList docs, int row) {
    String extractId = docs.get(0).getNaturalisNote().getExtractId();
    if (docs.size() == 1) {
      String fmt = "Row %s (%s) corresponds to an existing document, but the document was not selected and therefore not updated";
      guiLogger.info(fmt, userFriendly(row), extractId);
    } else {
      String fmt = "Row %s (%s) corresponds to %s existing documents, but they were not selected and therefore not updated";
      guiLogger.info(fmt, userFriendly(row), extractId, docs.size());
    }
  }

  private int userFriendly(int hardcore) {
    return hardcore + cfg.getSkipLines() + 1;
  }

}