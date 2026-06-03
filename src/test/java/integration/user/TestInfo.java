package integration.user;

import static ch.blw.agate.common.services.AuthenticationService.AGATE_AGRIDATA_PRODUCER_ROLE;
import static integration.testutils.TestUserEnum.EINWILLIGER_ERIKA;
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

  @Test
  void givenAuthUser_whenGetUserToken_thenUserTokenReturned() {
    Map<String, Object> userToken = AuthTestUtils.requestAs(EINWILLIGER_ERIKA)
        .when().get(UserController.PATH + "/user-token")
        .then().statusCode(200)
        .extract().as(new TypeRef<>() {
        });

    assertThat(userToken).isNotNull();
    assertThat(userToken).containsEntry("sub", EINWILLIGER_ERIKA.getSub());
    assertThat(userToken).containsEntry("loginid", EINWILLIGER_ERIKA.getAgateLoginId());
    assertThat(userToken).containsEntry("KT_ID_P", EINWILLIGER_ERIKA.getKtIdP());
    assertThat(userToken).containsEntry("extId", EINWILLIGER_ERIKA.getExtId());

    @SuppressWarnings("unchecked")
    var roles = (List<Map<String, Object>>) ((Map<String, Object>) userToken.get("realm_access")).get("roles");
    assertThat(roles).extracting(r -> r.get("string"))
        .containsExactlyInAnyOrder(AGATE_AGRIDATA_PRODUCER_ROLE);
  }
}
