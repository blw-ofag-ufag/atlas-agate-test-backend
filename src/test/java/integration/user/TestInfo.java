package integration.user;

import static ch.blw.agate.common.services.AuthenticationService.AGATE_AGRIDATA_PRODUCER_ROLE;
import static ch.blw.agate.common.services.AuthenticationService.AGATE_BENUTZER_ROLE;
import static integration.testutils.TestUserEnum.PRODUCER_LUKAS;
import static org.assertj.core.api.Assertions.assertThat;

import ch.blw.agate.user.controller.UserController;
import integration.testutils.AuthTestUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

@QuarkusTest
@RequiredArgsConstructor
public class TestInfo {
  // private final Flyway flyway;

//  @BeforeEach
//  void setUp() {
//    // will make sure testdata prior to executing each test
//    flyway.migrate();
//  }

  @Test
  void givenAuthUser_whenGetUserToken_thenUserTokenReturned() {
    Map<String, Object> userToken = AuthTestUtils.requestAs(PRODUCER_LUKAS)
        .when().get(UserController.PATH + "/user-token")
        .then().statusCode(200)
        .extract().as(new TypeRef<>() {
        });

    assertThat(userToken).isNotNull();
    assertThat(userToken.get("sub")).isEqualTo(PRODUCER_LUKAS.getSub());
    assertThat(userToken.get("loginid")).isEqualTo(PRODUCER_LUKAS.getAgateLoginId());
    assertThat(userToken.get("KT_ID_P")).isEqualTo(PRODUCER_LUKAS.getKtIdP());
    assertThat(userToken.get("extId")).isEqualTo(PRODUCER_LUKAS.getExtId());

    @SuppressWarnings("unchecked")
    var roles = (List<Map<String, Object>>) ((Map<String, Object>) userToken.get("realm_access")).get("roles");
    assertThat(roles).extracting(r -> r.get("string"))
        .containsExactlyInAnyOrder(AGATE_AGRIDATA_PRODUCER_ROLE, AGATE_BENUTZER_ROLE);
  }
}
