package ch.blw.agate.common.filters;

import static ch.blw.agate.common.utils.LogUtil.structuredFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.blw.agate.common.utils.RecordingLogHandler;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.LogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Covers the branches of {@link PreSecurityLogFilter} that the {@code @QuarkusTest} in
 * {@link PreSecurityLogFilterTest} does not reach (query string, request body at DEBUG, binary
 * content types, OPTIONS and non-API paths) with a mocked {@link RoutingContext}.
 */
@ExtendWith(MockitoExtension.class)
class PreSecurityLogFilterUnitTest {

  private static final String CONTEXT_ROOT = "/test-backend";

  private static final Predicate<LogRecord> FILTER_LOGS =
      rec -> rec.getLoggerName().equals(PreSecurityLogFilter.class.getName());

  static final RecordingLogHandler LOG_HANDLER = new RecordingLogHandler(FILTER_LOGS);

  private static final Logger LOGGER = Logger.getLogger(PreSecurityLogFilter.class.getName());

  private static java.util.logging.Level originalLevel;

  @Mock
  private RoutingContext ctx;

  @Mock
  private HttpServerRequest request;

  @Mock
  private HttpServerResponse response;

  private PreSecurityLogFilter filter;

  @BeforeAll
  static void attachHandler() {
    originalLevel = LOGGER.getLevel();
    LOGGER.addHandler(LOG_HANDLER);
  }

  @AfterAll
  static void detachHandler() {
    LOGGER.removeHandler(LOG_HANDLER);
    LOGGER.setLevel(originalLevel);
  }

  @BeforeEach
  void setUp() {
    filter = new PreSecurityLogFilter();
    filter.contextRoot = CONTEXT_ROOT;
    lenient().when(ctx.request()).thenReturn(request);
    lenient().when(ctx.response()).thenReturn(response);
  }

  @AfterEach
  void clearLogs() {
    LOG_HANDLER.getRecords().clear();
  }

  @Test
  void givenApiRequestWithQuery_whenInfo_thenRequestAndResponseAreLogged() {
    LOGGER.setLevel(Level.INFO);
    givenRequest(HttpMethod.GET, CONTEXT_ROOT + "/api/user", "page=2", null);
    when(ctx.get("request.startTime")).thenReturn(System.currentTimeMillis());
    when(response.getStatusCode()).thenReturn(200);

    filter.logRequests(ctx);
    bodyEndHandler().handle(null);

    List<LogRecord> logs = LOG_HANDLER.getRecords();
    assertThat(logs).hasSize(2);
    assertThat(structuredFields(logs.get(0)))
        .containsEntry("operation", "rest.request")
        .containsEntry("uri", CONTEXT_ROOT + "/api/user?page=2")
        .containsEntry("contentType", "");
    assertThat(structuredFields(logs.get(1)))
        .containsEntry("operation", "rest.response")
        .containsEntry("status", 200);
    verify(ctx).next();
  }

  @Test
  void givenJsonBody_whenDebug_thenBodyIsLogged() {
    LOGGER.setLevel(Level.DEBUG);
    givenRequest(HttpMethod.POST, CONTEXT_ROOT + "/api/user", null, "application/json");

    filter.logRequests(ctx);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Buffer>> bodyHandler = ArgumentCaptor.forClass(Handler.class);
    verify(request).bodyHandler(bodyHandler.capture());
    bodyHandler.getValue().handle(Buffer.buffer("{\"name\":\"test\"}"));

    assertThat(LOG_HANDLER.getRecords()).hasSize(1);
    assertThat(structuredFields(LOG_HANDLER.getRecords().get(0)))
        .containsEntry("operation", "rest.request")
        .containsEntry("contentType", "application/json")
        .containsEntry("body", "{\"name\":\"test\"}");
  }

  @Test
  void givenBinaryContentType_whenDebug_thenBodyIsNotRead() {
    LOGGER.setLevel(Level.DEBUG);
    givenRequest(HttpMethod.POST, CONTEXT_ROOT + "/api/upload", null, "Application/PDF");

    filter.logRequests(ctx);

    verify(request, never()).bodyHandler(any());
    assertThat(LOG_HANDLER.getRecords()).hasSize(1);
    assertThat(structuredFields(LOG_HANDLER.getRecords().get(0))).doesNotContainKey("body");
  }

  @Test
  void givenNonApiPath_thenNothingIsLogged() {
    LOGGER.setLevel(Level.INFO);
    when(request.path()).thenReturn(CONTEXT_ROOT + "/q/health");

    filter.logRequests(ctx);

    assertThat(LOG_HANDLER.getRecords()).isEmpty();
    verify(ctx).next();
  }

  @Test
  void givenOptionsRequest_thenLoggedOnlyAtTrace() {
    when(request.path()).thenReturn(CONTEXT_ROOT + "/api/user");
    when(request.method()).thenReturn(HttpMethod.OPTIONS);

    LOGGER.setLevel(Level.INFO);
    assertThat(filter.logEnabled(ctx)).isFalse();

    LOGGER.setLevel(Level.TRACE);
    assertThat(filter.logEnabled(ctx)).isTrue();
  }

  @Test
  void givenLevelAboveInfo_thenLoggingIsDisabled() {
    LOGGER.setLevel(Level.WARN);

    assertThat(filter.logEnabled(ctx)).isFalse();
  }

  private void givenRequest(HttpMethod method, String path, String query, String contentType) {
    when(request.method()).thenReturn(method);
    when(request.path()).thenReturn(path);
    lenient().when(request.query()).thenReturn(query);
    lenient().when(request.getHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(contentType);
  }

  @SuppressWarnings("unchecked")
  private Handler<Void> bodyEndHandler() {
    ArgumentCaptor<Handler<Void>> captor = ArgumentCaptor.forClass(Handler.class);
    verify(ctx).addBodyEndHandler(captor.capture());
    return captor.getValue();
  }
}
