package ch.blw.agate.common.filters;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.impl.jose.JWT;
import io.vertx.ext.web.RoutingContext;
import java.util.Optional;
import org.jboss.logging.MDC;

/**
 * Adds user sub and API paths to the logging context before security checks. It improves
 * traceability for early request stages, e.g. to analyze login failures that are rejected before
 * any endpoint is reached. The actual request/response logging is done by {@link
 * PreSecurityLogFilter}.
 *
 * <p>Request correlation is provided by OpenTelemetry: the {@code traceId} and {@code spanId} are
 * put into the MDC automatically by the {@code quarkus-opentelemetry} extension, which also
 * continues an inbound W3C {@code traceparent} header or starts a new trace when none is present.
 */
public class PreSecurityMdcFilter {

  public static final String API_MDC_FIELD = "api";
  private static final String USER_ID_MDC_FIELD = "userId";

  // Higher numbers = higher priority (runs earlier)
  @RouteFilter(1501)
  void beforeSecurity(RoutingContext ctx) {
    MDC.put(USER_ID_MDC_FIELD, getUserId(ctx));
    String path = ctx.request().path();
    MDC.put(API_MDC_FIELD, path);
    String query = ctx.request().query();
    MDC.put("query", query != null ? query : "");

    ctx.next();
  }

  private String getUserId(RoutingContext context) {
    String authHeader = context.request().getHeader(HttpHeaders.AUTHORIZATION);
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      JsonObject jwtData = JWT.parse(token);
      return Optional.ofNullable(jwtData.getJsonObject("payload")).map(payload -> payload.getString("sub"))
          .orElse("unknown");
    }
    return "anonymous";
  }


}
