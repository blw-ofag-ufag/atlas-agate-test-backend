package integration.user;

import static integration.testutils.TestUserEnum.DATENANBIETER_LUKAS;
import static integration.testutils.TestUserEnum.EINWILLIGER_ERIKA;
import static org.assertj.core.api.Assertions.assertThat;

import ch.blw.agate.user.controller.UserController;
import ch.blw.agate.user.dto.DummyData;
import integration.testutils.AuthTestUtils;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProducerInfoTest {

  @Test
  void givenEinwilliger_whenGetProducerInfo_thenReturnsDummyData() {
    DummyData body = AuthTestUtils.requestAs(EINWILLIGER_ERIKA)
        .when().get(UserController.PATH + "/producer-info")
        .then().statusCode(200)
        .extract().as(DummyData.class);

    assertThat(body.role()).isEqualTo("producer");
    assertThat(body.userId()).isEqualTo(EINWILLIGER_ERIKA.getSub());
  }

  @Test
  void givenNonEinwilliger_whenGetProducerInfo_thenForbidden() {
    AuthTestUtils.requestAs(DATENANBIETER_LUKAS)
        .when().get(UserController.PATH + "/producer-info")
        .then().statusCode(403);
  }
}
