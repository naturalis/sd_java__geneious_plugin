package nl.naturalis.geneious.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Optional;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;

import nl.naturalis.geneious.DocumentType;
import nl.naturalis.geneious.gui.log.GuiLogManager;
import nl.naturalis.geneious.gui.log.GuiLogger;

import static nl.naturalis.geneious.note.NaturalisField.DOCUMENT_VERSION;
import static nl.naturalis.geneious.note.NaturalisField.SMPL_EXTRACT_ID;
import static nl.naturalis.geneious.util.DocumentUtils.getDateModifield;

/**
 * Provides various types of lookups on a collection of Geneious documents (presumably fetched from the database).
 */
public class DocumentResultSetInspector {

  private static final GuiLogger guiLogger = GuiLogManager.getLogger(DocumentResultSetInspector.class);

  // Sort descending on document version or creation date
  private static Comparator<StoredDocument> comparator = (doc1, doc2) -> {
    Integer v1 = doc1.getNaturalisNote().get(DOCUMENT_VERSION);
    Integer v2 = doc2.getNaturalisNote().get(DOCUMENT_VERSION);
    if (v1 == null) {
      if (v2 != null) {
        return -1; // Prefer anything over null
      }
    }
    if (v2 == null) {
      return 1;
    }
    int i = v2.compareTo(v1);
    if (i == 0) {
      return getDateModifield(doc2.getGeneiousDocument()).compareTo(getDateModifield(doc1.getGeneiousDocument()));
    }
    return i;
  };

  private final EnumMap<DocumentType, HashMap<String, StoredDocument>> byTypeByExtractId;

  /**
   * Creates a new DocumentResultSetInspector for the specified documents.
   * 
   * @param documents
   */
  public DocumentResultSetInspector(Collection<AnnotatedPluginDocument> documents) {
    this.byTypeByExtractId = new EnumMap<>(DocumentType.class);
    cacheDocuments(documents);
  }

  /**
   * Returns the latest version of the document with the specified extract ID and type, or an empty optional if this combination does not
   * exist yet in the database.
   * 
   * @param extractID
   * @param type
   * @return
   */
  public Optional<StoredDocument> find(String extractID, DocumentType type) {
    HashMap<String, StoredDocument> subcache = byTypeByExtractId.get(type);
    if (subcache != null) {
      return Optional.ofNullable(subcache.get(extractID));
    }
    return Optional.empty();
  }

  /**
   * Convenience method, equivalent to calling {@code findLatestVersion(extractID, DocumentType.DUMMY)}.
   * 
   * @param extractID
   * @return
   */
  public Optional<StoredDocument> findDummy(String extractID) {
    return find(extractID, DocumentType.DUMMY);
  }

  private void cacheDocuments(Collection<AnnotatedPluginDocument> documents) {
    for (AnnotatedPluginDocument document : documents) {
      StoredDocument doc = new StoredDocument(document);
      String extractId = doc.getNaturalisNote().get(SMPL_EXTRACT_ID);
      switch (doc.getType()) {
        case AB1:
        case FASTA:
        case DUMMY:
          byTypeByExtractId
              .computeIfAbsent(doc.getType(), (k) -> new HashMap<>())
              .merge(extractId, doc, (d1, d2) -> comparator.compare(d1, d2) < 0 ? d2 : d1);
          break;
        case UNKNOWN:
        default:
          guiLogger.warn("Unexpected Geneious document type: %s (document ignored)", document.getDocumentClass());
      }
    }
  }

}
