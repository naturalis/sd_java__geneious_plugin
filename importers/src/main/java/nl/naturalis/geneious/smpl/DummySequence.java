package nl.naturalis.geneious.smpl;

import java.util.Date;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.DocumentUtilities;
import com.biomatters.geneious.publicapi.implementations.sequence.DefaultNucleotideSequence;

import static nl.naturalis.geneious.note.NaturalisField.*;
import nl.naturalis.geneious.note.NaturalisNote;

/**
 * An extension of Geneious's {@code DefaultNucleotideSequence} class solely meant to create dummy documents. The dummy
 * documents will be removed as soon as the real fasta and AB1 sequences are imported, providing them with the
 * annotations saved to the dummy document.
 *
 * @author Ayco Holleman
 */
public class DummySequence extends DefaultNucleotideSequence {

  /**
   * The nucleotide sequence used for all dummy documents: "NNNNNNNNNN"
   */
  public static final String DUMMY_SEQUENCE = "NNNNNNNNNN";
  /**
   * The plate ID used for all documents: "AA000"
   */
  public static final String DUMMY_PCR_PLATE_ID = "AA000";
  /**
   * The marker used for all documents: "Dum"
   */
  public static final String DUMMY_MARKER = "Dum";

  private final NaturalisNote note;

  /**
   * No-arg constructor, required by Geneious framework, but it seems we can rely on the other constructor being called
   * when it matters.
   */
  public DummySequence() {
    super();
    this.note = null;
  }

  /**
   * Creates a dummy sequence with the specified annotations.
   * 
   * @param note
   */
  public DummySequence(NaturalisNote note) {
    super(name(note), "", DUMMY_SEQUENCE, new Date());
    this.note = note;
  }

  /**
   * Wraps the sequence into a Geneious document and saves it to the database.
   * 
   * @return
   */
  public AnnotatedPluginDocument wrap() {
    AnnotatedPluginDocument document = DocumentUtilities.createAnnotatedPluginDocument(this);
    note.setDocumentVersion(0);
    note.castAndSet(SEQ_PCR_PLATE_ID, DUMMY_PCR_PLATE_ID);
    note.castAndSet(SEQ_MARKER, DUMMY_MARKER);
    note.castAndSet(SEQ_EXTRACT_ID, note.get(SMPL_EXTRACT_ID));
    note.attachTo(document);
    return document;
  }

  private static String name(NaturalisNote note) {
    return note.getExtractId() + " (dummy)";
  }

}