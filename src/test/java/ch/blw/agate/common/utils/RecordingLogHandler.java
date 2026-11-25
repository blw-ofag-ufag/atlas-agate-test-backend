package ch.blw.agate.common.utils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import lombok.Getter;
import org.jboss.logmanager.ExtLogRecord;

/**
 * Captures {@link LogRecord}s in memory so tests can assert on what was logged.
 *
 * <p>Attach it to the logger under test, assert against {@link #getRecords()}, and detach it again.
 * Only records matching the predicate given to the constructor are kept.</p>
 *
 * <p>{@link ExtLogRecord}s read the MDC/NDC lazily from the current thread, so they are snapshotted
 * on publish. This keeps e.g. {@code traceId} readable after the logging thread has moved on.</p>
 */
public class RecordingLogHandler extends Handler {

  /**
   * -- GETTER --
   * The captured records, in publication order. The returned list is live and mutable, so tests can
   * clear it between cases.
   */
  @Getter
  private final List<LogRecord> records = new CopyOnWriteArrayList<>();
  private final Predicate<LogRecord> filter;

  public RecordingLogHandler(Predicate<LogRecord> filter) {
    this.filter = filter;
  }

  @Override
  public void publish(LogRecord logRecord) {
    if (filter.test(logRecord)) {
      if (logRecord instanceof ExtLogRecord ext) {
        ext.copyAll();
      }
      records.add(logRecord);
    }
  }

  @Override
  public void flush() {
    // nothing buffered
  }

  @Override
  public void close() {
    records.clear();
  }

}
