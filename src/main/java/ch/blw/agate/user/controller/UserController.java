package ch.blw.agate.user.controller;

import static ch.blw.agate.common.services.AuthenticationService.AGATE_AGRIDATA_PRODUCER_ROLE;

import ch.blw.agate.common.services.AuthenticationService;
import ch.blw.agate.user.dto.DummyData;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path(UserController.PATH)
@Slf4j
@Tag(
    name = "Users",
    description = "Provides user information")
@RunOnVirtualThread
@RequiredArgsConstructor
@Authenticated
public class UserController {

  public static final String PATH = "/api/user/v1";

  private final AuthenticationService authenticationService;

  @GET
  @Path("/user-token")
  @Operation(
      operationId = "getUserToken",
      description = "Retrieves user-token for the currently authenticated user.")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> getUserToken() {
    return authenticationService.getClaims();
  }

  @GET
  @Path("/user-info")
  @Operation(
      operationId = "getUserInfo",
      description = "Retrieves userinfo for the currently authenticated user.")
  @Produces(MediaType.APPLICATION_JSON)
  @Authenticated
  public JsonObject getUserInfo() {
    return authenticationService.getUserInfoOrElseThrow().getJsonObject();
  }


  @GET
  @Path("/producer-info")
  @Operation(
      operationId = "getProducerInfo",
      description = "returns dummy data if the user is a agridata producer")

  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({AGATE_AGRIDATA_PRODUCER_ROLE})
  public DummyData getProducerInfo() {
    return DummyData.builder().role("producer").userId(authenticationService.getSubjectId()).build();
  }

}
