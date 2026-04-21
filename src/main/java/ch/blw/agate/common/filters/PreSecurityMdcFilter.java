package ch.blw.agate.common.filters;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.impl.jose.JWT;
import io.vertx.ext.web.RoutingContext;
import java.util.Optional;
import java.util.UUID;
import org.jboss.logging.MDC;

/**
 * Adds request identifiers, user sub, and API paths to the logging context before security checks.
 * It improves traceability for early request stages.
 */
public class PreSecurityMdcFilter {

  public static final String REQUEST_ID_MDC_FIELD = "requestId";
  public static final String API_MDC_FIELD = "api";
  private static final String USER_ID_MDC_FIELD = "userId";

  // Higher numbers = higher priority (runs earlier)
  @RouteFilter(1501)
  void beforeSecurity(RoutingContext ctx) {
    MDC.put(REQUEST_ID_MDC_FIELD, UUID.randomUUID().toString());
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
