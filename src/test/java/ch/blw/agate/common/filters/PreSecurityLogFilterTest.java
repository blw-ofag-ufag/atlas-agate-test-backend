package ch.blw.agate.common.filters;

import static ch.blw.agate.common.utils.LogUtil.structuredFields;
import static org.assertj.core.api.Assertions.assertThat;

import ch.blw.agate.common.utils.RecordingLogHandler;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link PreSecurityLogFilter} logs the incoming request <em>before</em> security runs
 * (so login failures rejected with 401 on {@code /api} are still logged) and that OpenTelemetry has
 * populated a valid {@code traceId}/{@code spanId} in the MDC at that point.
 */
@QuarkusTest
class PreSecurityLogFilterTest {

  // PreSecurityLogFilter only logs paths under contextRoot + "/api" (see URIS_TO_APPLY).
  private static final String API_PATH = "/api/does-not-exist";
  private static final String ALL_ZERO_TRACE_ID = "00000000000000000000000000000000";

  private static final SimpleFormatter FORMATTER = new SimpleFormatter();

  // RecordingLogHandler snapshots the MDC on publish, so traceId/spanId survive the request thread.
  private final RecordingLogHandler logHandler = new RecordingLogHandler(
      rec -> rec.getLoggerName().equals(PreSecurityLogFilter.class.getName()));
  private Logger filterLogger;
  private Level originalLevel;

  @BeforeEach
  void attachLogHandler() {
    filterLogger = Logger.getLogger(PreSecurityLogFilter.class.getName());
    // The test profile raises the root level to WARN; force INFO so the filter's INFO request log
    // is emitted (and, being below DEBUG, takes the synchronous non-body branch).
    originalLevel = filterLogger.getLevel();
    filterLogger.setLevel(Level.INFO);
    filterLogger.addHandler(logHandler);
  }

  @AfterEach
  void detachLogHandler() {
    filterLogger.removeHandler(logHandler);
    filterLogger.setLevel(originalLevel);
    logHandler.close();
  }

  @Test
  void givenUnauthenticatedApiRequest_whenRejectedBySecurity_thenPreSecurityFilterLogsRequestWithTraceId() {
    RestAssured.given()
        .when().get(API_PATH)
        .then().statusCode(401);

    // "rest.response" does not contain "rest.request", so this selects the request record.
    LogRecord log = logHandler.getRecords().stream()
        .filter(rec -> rec.getMessage() != null
            && FORMATTER.formatMessage(rec).contains("rest.request"))
        .findFirst()
        .orElseThrow(() -> new AssertionError(
            "PreSecurityLogFilter did not log the incoming request before security ran"));
    Map<String, String> mdc = ((ExtLogRecord) log).getMdcCopy();

    // The request path is logged for correlation.
    assertThat(FORMATTER.formatMessage(log)).contains(API_PATH);

    // The kv(...) structured arguments must reach the record so the JSON encoder can map them
    // onto top-level fields.
    // The uri is prefixed with quarkus.http.root-path, hence endsWith rather than equality.
    assertThat(structuredFields(log))
        .containsEntry("operation", "rest.request")
        .containsEntry("method", "GET")
        .hasEntrySatisfying("uri", uri -> assertThat((String) uri).endsWith(API_PATH));

    // OpenTelemetry must have populated a valid trace context at the pre-security stage.
    assertThat(mdc.get("traceId"))
        .as("traceId must be present in MDC at the pre-security filter")
        .isNotNull()
        .hasSize(32)
        .isNotEqualTo(ALL_ZERO_TRACE_ID);
    assertThat(mdc.get("spanId"))
        .as("spanId must be present in MDC at the pre-security filter")
        .isNotNull()
        .isNotBlank();
  }
}
