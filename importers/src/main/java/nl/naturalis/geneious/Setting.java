package nl.naturalis.geneious;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Symbolic constants for configuration settings for the Naturalis plugin.
 *
 * @author Ayco Holleman
 */
public enum Setting {

  /**
   * Whether or not to show DEBUG messages in the log file.
   */
  DEBUG("nl.naturalis.geneious.log.debug"),
  /**
   * The end time of the previous operation executed by the user
   */
  LAST_FINISHED("nl.naturalis.geneious.operation.lastEndTime"),
  /**
   * The minimum wait time (in seconds) between any two operations. This works around a bug in Geneious, which currently
   * does not provide a reliable way of establishing whether all documents have ben indexed.
   */
  MIN_WAIT_TIME("nl.naturalis.geneious.operation.minWaitTime"),
  /**
   * AB1 file extensions.
   */
  AB1_EXTS("nl.naturalis.geneious.seq.ext.ab1"),
  /**
   * Fasta file extensions.
   */
  FASTA_EXTS("nl.naturalis.geneious.seq.ext.fasta"),
  /**
   * Always write fasta sequences to temporary files.
   */
  DISABLE_FASTA_CACHE("nl.naturalis.geneious.seq.disableFastaCache"),
  /**
   * Remove tempoerary fasta files from file system when done.
   */
  DELETE_TMP_FASTAS("nl.naturalis.geneious.seq.deleteTmpFastas");

  private static final HashMap<String, Setting> reverse = new HashMap<>(values().length, 1F);

  static {
    Arrays.stream(values()).forEach(s -> reverse.put(s.getName(), s));
  }

  /**
   * Returns the setting corresponding to the provided name, which is supposed to be the fully-qualified name known to
   * Geneious.
   * 
   * @param name
   * @return
   */
  public static Setting forName(String name) {
    return reverse.get(name);
  }

  private final String name;

  private Setting(String name) {
    this.name = name;
  }

  /**
   * Returns the fully-qualified name by which Geneious knows this configuration setting.
   * 
   * @return
   */
  public String getName() {
    return name;
  }

}
