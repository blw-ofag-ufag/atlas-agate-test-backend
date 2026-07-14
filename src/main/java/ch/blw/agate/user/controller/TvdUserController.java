package ch.blw.agate.user.controller;

import ch.blw.agate.user.dto.TvdUserDto;
import ch.blw.agate.user.soap.AgateUserQueryService;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path(TvdUserController.PATH)
@Tag(name = "TVD Users", description = "Queries user data from the Agate SOAP API")
@RunOnVirtualThread
@RequiredArgsConstructor
@Authenticated
public class TvdUserController {

  public static final String PATH = "/api/tvd/v1";

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
}
