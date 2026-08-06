package integration.user;

import static integration.testutils.TestUserEnum.EINWILLIGER_ERIKA;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.when;

import ch.blw.agate.user.controller.TvdUserController;
import ch.blw.agate.user.dto.TvdUserDto;
import ch.blw.agate.user.soap.AgateUserQueryService;
import integration.testutils.AuthTestUtils;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TvdUserQueryTest {

  @InjectMock
  AgateUserQueryService agateUserQueryService;

  @Test
  void givenAuthUser_whenQueryUsers_thenReturnsMappedDtos() {
    when(agateUserQueryService.queryUsers()).thenReturn(List.of(
        new TvdUserDto("3365033", "Ramon", "Rüfenacht", "184723", "Default"),
        new TvdUserDto("9811215", "David", "Oberli", "184724", "Default")));

    AuthTestUtils.requestAs(EINWILLIGER_ERIKA)
        .when().get(TvdUserController.PATH + "/users")
        .then().statusCode(200)
        .body("loginId", hasItems("3365033", "9811215"))
        .body("name", hasItems("Rüfenacht", "Oberli"));
  }

  @Test
  void givenNoAuth_whenQueryUsers_thenUnauthorized() {
    RestAssured.given()
        .when().get(TvdUserController.PATH + "/users")
        .then().statusCode(401);
  }

  @Test
  void givenAuthUser_whenGetUsersByLoginId_thenReturnsMappedDtos() {
    when(agateUserQueryService.getUsersByLoginId(List.of("3365033"))).thenReturn(List.of(
        new TvdUserDto("3365033", "Ramon", "Rüfenacht", "184723", "Default")));

    AuthTestUtils.requestAs(EINWILLIGER_ERIKA)
        .queryParam("loginId", "3365033")
        .when().get(TvdUserController.PATH + "/users/by-login-id")
        .then().statusCode(200)
        .body("loginId", hasItems("3365033"))
        .body("name", hasItems("Rüfenacht"));
  }

  @Test
  void givenNoAuth_whenGetUsersByLoginId_thenUnauthorized() {
    RestAssured.given()
        .queryParam("loginId", "3365033")
        .when().get(TvdUserController.PATH + "/users/by-login-id")
        .then().statusCode(401);
  }
}
