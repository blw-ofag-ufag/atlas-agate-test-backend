package ch.blw.agate.user.controller;

import ch.blw.agate.user.dto.TvdUserDto;
import ch.blw.agate.user.soap.AgateUserQueryService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path(TvdUserController.PATH)
@Tag(name = "TVD Users", description = "Queries user data from the Agate SOAP API")
@RunOnVirtualThread
@RequiredArgsConstructor
@RolesAllowed(TvdUserController.TESTSUPPORT_ROLE)
public class TvdUserController {

  /** Held by the Playwright client {@code agate-playwright}; the path policy {@code api-protected} only requires a login. */
  public static final String TESTSUPPORT_ROLE = "agate.Agate_Testsupport";

  public static final String PATH = "/api/tvd";

  private final AgateUserQueryService agateUserQueryService;

  @GET
  @Path("/users")
  @Operation(
      operationId = "queryTvdUsers",
      description = "Queries users from the Agate SOAP API (queryUsers) and returns them as DTOs.")
  @Produces(MediaType.APPLICATION_JSON)
  public List<TvdUserDto> queryUsers() {
    return agateUserQueryService.queryUsers();
  }

  @GET
  @Path("/users/by-login-id")
  @Operation(
      operationId = "getTvdUsersByLoginId",
      description = "Fetches users from the Agate SOAP API (getUsersByLoginId) by loginId and returns them as DTOs.")
  @Produces(MediaType.APPLICATION_JSON)
  public List<TvdUserDto> getUsersByLoginId(@QueryParam("loginId") List<String> loginIds) {
    return agateUserQueryService.getUsersByLoginId(loginIds);
  }
}
