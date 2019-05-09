package nl.naturalis.geneious.util;

import static com.biomatters.geneious.publicapi.utilities.GuiUtilities.getMainFrame;
import static nl.naturalis.geneious.Settings.settings;
import static nl.naturalis.geneious.util.QueryUtils.getPingDocument;
import static nl.naturalis.geneious.util.QueryUtils.getTargetDatabase;

import javax.swing.ProgressMonitor;

import com.biomatters.geneious.publicapi.databaseservice.DatabaseServiceException;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;

import nl.naturalis.geneious.gui.log.GuiLogManager;
import nl.naturalis.geneious.gui.log.GuiLogger;

/**
 * Pings the database to ascertain that all documents have been indexed.
 * 
 * @author Ayco Holleman
 *
 */
public class Ping {
  
  public static final String PING_FOLDER_NAME = "#ping";

  private static final int TRY_COUNT = 100;
  private static final double TRY_INTERVAL = 3.0; // seconds

  // The weight (expressed as progress bar progress) of the 1st ping attempts. Since indexing is likely to complete within the 1st batch of
  // pings, we want to make it look as though we're making a lot of progress here (at the expense of the progress bar seeming to grind to a
  // halt if indexing happens to take a lot of time). Tune according to taste.
  private static final int batch0TryCount = 10;
  private static final int batch0TryWeight = 20;
  private static final int batch1TryCount = 10;
  private static final int batch1TryWeight = 5;

  private static final String MSG0 = "Waiting for indexing to complete ...";
  private static final String MSG1 = "Wait aborted after %d attempts";

  private static final GuiLogger guiLogger = GuiLogManager.getLogger(Ping.class);

  /**
   * Sends a ping value to the database and then starts a ping loop that only ends if [1] the ping value came back from the database; [2] the
   * user cancelled the ping loop; [3] the number of pings exceeds 100 (equivalent to about 5 minutes of pinging). If [1] we assume that all
   * documents have been indexed (because the temporary document containing the ping value is itself apparently indexed) and the ping value is
   * cleared. If [2] or [3] the ping value will be stored and subsequent operation are prevented from proceeding until {@link #resume()}
   * returns {@code true}. Must be called at the very end of a {@code DocumentOperation}.
   * 
   * @return
   * @throws DatabaseServiceException
   */
  public static boolean start() throws DatabaseServiceException {
    return new Ping().doStart();
  }

  /**
   * Checks for a lingering ping value and, if present, starts the ping loop all over again. Must be called at the very beginning of a
   * {@code DocumentOperation}.
   * 
   * @return
   * @throws DatabaseServiceException
   */
  public static boolean resume() throws DatabaseServiceException {
    return new Ping().doResume();
  }

  private Ping() {}

  private boolean doStart() throws DatabaseServiceException {
    guiLogger.info(MSG0);
    long timestamp = System.currentTimeMillis();
    String pingValue = getPingValue(timestamp);
    settings().setPingTime(String.valueOf(timestamp));
    PingSequence sequence = new PingSequence(pingValue);
    sequence.save(PING_FOLDER_NAME);
    return startPingLoop(pingValue);
  }

  private boolean doResume() throws DatabaseServiceException {
    String s = settings().getPingTime();
    if (s.isEmpty()) {
      return true;
    }
    guiLogger.info(MSG0);
    long timestamp = Long.parseLong(s);
    String pingValue = getPingValue(timestamp);
    if ((timestamp + (60 * 60 * 1000)) < System.currentTimeMillis()) {
      guiLogger.warn("Resuming with old ping value: %s", pingValue);
      guiLogger.warn("Go to Tools -> Preferences (Naturalis tab) to clear old ping values and documents");
    }
    return startPingLoop(pingValue);
  }

  @SuppressWarnings("static-method")
  private boolean startPingLoop(String pingValue) throws DatabaseServiceException {
    ProgressMonitor pm = new ProgressMonitor(getMainFrame(), MSG0, "", 0, getProgressMax());
    pm.setMillisToDecideToPopup(0);
    pm.setMillisToPopup(0);
    for (int i = 1; i <= TRY_COUNT; ++i) {
      pm.setProgress(getProgress(i));
      sleep();
      if (pm.isCanceled()) {
        pm.close();
        guiLogger.warn(MSG1, i);
        return false;
      }
      AnnotatedPluginDocument apd = getPingDocument(pingValue);
      if (apd != null) {
        pm.setProgress(getProgressMax());
        pm.setNote("Ready!");
        settings().setPingTime("");
        getTargetDatabase().removeChildFolder(PING_FOLDER_NAME);
        guiLogger.info("Indexing complete");
        pm.close();
        return true;
      }
    }
    pm.close();
    guiLogger.warn(MSG1, TRY_COUNT);
    return false;
  }

  private static void sleep() {
    try {
      Thread.sleep((long) TRY_INTERVAL * 1000);
    } catch (InterruptedException e) {
    }
  }

  private static String getPingValue(long timestamp) {
    return new StringBuilder()
        .append("!ping:")
        .append(System.getProperty("user.name"))
        .append("//")
        .append(timestamp)
        .toString();
  }

  private static int getProgressMax() {
    return (batch0TryCount * batch0TryWeight) + (batch1TryCount * batch1TryWeight) + (TRY_COUNT - batch0TryCount - batch1TryCount);
  }

  private static int getProgress(int i) {
    return (i * batch0TryWeight) + (max(i - batch0TryCount) * batch1TryWeight) + max(i - batch0TryCount - batch1TryCount);
  }

  private static int max(int x) {
    return Math.max(x, 0);
  }
}