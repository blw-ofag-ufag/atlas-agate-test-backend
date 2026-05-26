package ch.blw.agate.common.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PreSecurityLogFilterTest {

  private static final String LOGGER_NAME = PreSecurityLogFilter.class.getName();
  private static final String CONTEXT_ROOT = "/test-backend";

  private PreSecurityLogFilter filter;
  private RoutingContext ctx;
  private HttpServerRequest request;
  private HttpServerResponse response;
  private Level originalLevel;

  @BeforeEach
  void setUp() {
    Logger logger = Logger.getLogger(LOGGER_NAME);
    originalLevel = logger.getLevel();
    // Force INFO so log.isInfoEnabled() is true but debug/trace are off — the
    // default branch through logRequests that registers addBodyEndHandler.
    logger.setLevel(Level.INFO);

    filter = new PreSecurityLogFilter();
    filter.contextRoot = CONTEXT_ROOT;

    ctx = mock(RoutingContext.class);
    request = mock(HttpServerRequest.class);
    response = mock(HttpServerResponse.class);

    lenient().when(ctx.request()).thenReturn(request);
    lenient().when(ctx.response()).thenReturn(response);
  }

  @AfterEach
  void tearDown() {
    Logger.getLogger(LOGGER_NAME).setLevel(originalLevel);
  }

  @ParameterizedTest(name = "[{index}] path={0} -> logEnabled={1}")
  @CsvSource({
      "/test-backend/api/user/v1/info, true",
      "/test-backend/api,             true",
      "/test-backend/q/health,        false",
      "/test-backend/q/metrics,       false",
      "/q/swagger-ui,                 false",
      "/api/user/v1/info,             false",
      "/,                             false"
  })
  void givenPath_whenLogEnabled_thenMatchesExpected(String path, boolean expected) {
    when(request.path()).thenReturn(path);
    when(request.method()).thenReturn(HttpMethod.GET);

    assertThat(filter.logEnabled(ctx)).isEqualTo(expected);
  }

  @Test
  void givenOptionsOnApiPath_whenLogEnabled_thenFalseAtInfoLevel() {
    when(request.path()).thenReturn("/test-backend/api/user/v1/info");
    when(request.method()).thenReturn(HttpMethod.OPTIONS);

    // OPTIONS requires TRACE; INFO -> trace disabled -> not logged.
    assertThat(filter.logEnabled(ctx)).isFalse();
  }

  @Test
  void givenOptionsOnApiPath_whenTraceEnabled_thenLogEnabled() {
    Logger.getLogger(LOGGER_NAME).setLevel(Level.FINEST);
    when(request.path()).thenReturn("/test-backend/api/user/v1/info");
    when(request.method()).thenReturn(HttpMethod.OPTIONS);

    assertThat(filter.logEnabled(ctx)).isTrue();
  }

  @Test
  void givenLoggerAboveInfo_whenLogEnabled_thenReturnsFalse() {
    Logger.getLogger(LOGGER_NAME).setLevel(Level.WARNING);
    when(request.path()).thenReturn("/test-backend/api/user/v1/info");
    when(request.method()).thenReturn(HttpMethod.GET);

    assertThat(filter.logEnabled(ctx)).isFalse();
  }

  @Test
  void givenEmptyContextRoot_whenLogEnabled_thenApiPathStillMatches() {
    filter.contextRoot = "";
    when(request.path()).thenReturn("/api/user/v1/info");
    when(request.method()).thenReturn(HttpMethod.GET);

    assertThat(filter.logEnabled(ctx)).isTrue();
  }

  @Test
  void givenApiRequest_whenLogRequests_thenRegistersBodyEndHandlerAndCallsNext() {
    when(request.path()).thenReturn("/test-backend/api/user/v1/info");
    when(request.method()).thenReturn(HttpMethod.GET);
    when(request.query()).thenReturn("filter=foo");
    when(request.getHeader(eq(HttpHeaders.CONTENT_TYPE))).thenReturn("application/json");

    filter.logRequests(ctx);

    verify(ctx).put(eq("request.startTime"), any(Long.class));
    verify(ctx).addBodyEndHandler(any());
    verify(ctx).next();
    // INFO-level branch should not register a body handler (that's the debug branch).
    verify(request, never()).bodyHandler(any());
  }

  @Test
  void givenNonApiRequest_whenLogRequests_thenSkipsLoggingButCallsNext() {
    when(request.path()).thenReturn("/test-backend/q/health");
    when(request.method()).thenReturn(HttpMethod.GET);

    filter.logRequests(ctx);

    verify(ctx, never()).addBodyEndHandler(any());
    verify(ctx, never()).put(eq("request.startTime"), any());
    verify(ctx).next();
  }

}
