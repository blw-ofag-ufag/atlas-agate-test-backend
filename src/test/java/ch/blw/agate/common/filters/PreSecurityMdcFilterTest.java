package ch.blw.agate.common.filters;

import static ch.blw.agate.common.filters.PreSecurityMdcFilter.API_MDC_FIELD;
import static ch.blw.agate.common.filters.PreSecurityMdcFilter.REQUEST_ID_MDC_FIELD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PreSecurityMdcFilterTest {

  private static final String USER_ID_MDC_FIELD = "userId";
  private static final String QUERY_MDC_FIELD = "query";

  private PreSecurityMdcFilter filter;
  private RoutingContext ctx;
  private HttpServerRequest request;

  @BeforeEach
  void setUp() {
    filter = new PreSecurityMdcFilter();
    ctx = mock(RoutingContext.class);
    request = mock(HttpServerRequest.class);
    lenient().when(ctx.request()).thenReturn(request);
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void givenAnyRequest_whenBeforeSecurity_thenSetsUuidRequestIdAndCallsNext() {
    stubRequest("/test-backend/api/user/v1/info", null, null);

    filter.beforeSecurity(ctx);

    String requestId = (String) MDC.get(REQUEST_ID_MDC_FIELD);
    assertThat(requestId).isNotNull();
    assertThat(UUID.fromString(requestId)).isNotNull();
    verify(ctx).next();
  }

  @Test
  void givenSuccessiveRequests_whenBeforeSecurity_thenRequestIdsDiffer() {
    stubRequest("/test-backend/api/user/v1/info", null, null);

    filter.beforeSecurity(ctx);
    String first = (String) MDC.get(REQUEST_ID_MDC_FIELD);

    filter.beforeSecurity(ctx);
    String second = (String) MDC.get(REQUEST_ID_MDC_FIELD);

    assertThat(second).isNotEqualTo(first);
  }

  @Test
  void givenRequestPath_whenBeforeSecurity_thenSetsApiMdcField() {
    stubRequest("/test-backend/api/user/v1/info", null, null);

    filter.beforeSecurity(ctx);

    assertThat(MDC.get(API_MDC_FIELD)).isEqualTo("/test-backend/api/user/v1/info");
  }

  @Test
  void givenQueryString_whenBeforeSecurity_thenSetsQueryMdcField() {
    stubRequest("/test-backend/api/user/v1/info", "filter=foo&page=1", null);

    filter.beforeSecurity(ctx);

    assertThat(MDC.get(QUERY_MDC_FIELD)).isEqualTo("filter=foo&page=1");
  }

  @Test
  void givenNullQuery_whenBeforeSecurity_thenSetsQueryToEmptyString() {
    stubRequest("/test-backend/api/user/v1/info", null, null);

    filter.beforeSecurity(ctx);

    assertThat(MDC.get(QUERY_MDC_FIELD)).isEqualTo("");
  }

  @Test
  void givenNoAuthHeader_whenBeforeSecurity_thenUserIdIsAnonymous() {
    stubRequest("/test-backend/api/x", null, null);

    filter.beforeSecurity(ctx);

    assertThat(MDC.get(USER_ID_MDC_FIELD)).isEqualTo("anonymous");
  }

  @Test
  void givenNonBearerAuthHeader_whenBeforeSecurity_thenUserIdIsAnonymous() {
    stubRequest("/test-backend/api/x", null, "Basic dXNlcjpwYXNz");

    filter.beforeSecurity(ctx);

    assertThat(MDC.get(USER_ID_MDC_FIELD)).isEqualTo("anonymous");
  }

  @Test
  void givenBearerTokenWithSub_whenBeforeSecurity_thenUserIdIsSub() {
    String token = buildUnsignedJwt("{\"sub\":\"user-123\"}");
    stubRequest("/test-backend/api/x", null, "Bearer " + token);

    filter.beforeSecurity(ctx);

    assertThat(MDC.get(USER_ID_MDC_FIELD)).isEqualTo("user-123");
  }

  @Test
  void givenBearerTokenWithoutSub_whenBeforeSecurity_thenUserIdIsUnknown() {
    String token = buildUnsignedJwt("{\"name\":\"alice\"}");
    stubRequest("/test-backend/api/x", null, "Bearer " + token);

    filter.beforeSecurity(ctx);

    assertThat(MDC.get(USER_ID_MDC_FIELD)).isEqualTo("unknown");
  }

  private void stubRequest(String path, String query, String authHeader) {
    when(request.path()).thenReturn(path);
    when(request.query()).thenReturn(query);
    when(request.getHeader(eq(HttpHeaders.AUTHORIZATION))).thenReturn(authHeader);
  }

  private String buildUnsignedJwt(String payloadJson) {
    Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
    String header = enc.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    String payload = enc.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
    return header + "." + payload + ".";
  }
}
