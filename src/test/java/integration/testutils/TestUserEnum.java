package integration.testutils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TestUserEnum {
  EINWILLIGER_ERIKA("blw.test.producer.erika@proton.me_eid\\9000\\0000001",
      "3477580",
      "FLXXA0001", "184723", "eid\\9000\\0000001"),
  DATENANBIETER_LUKAS("blw.test.producer.lukas@proton.me_eid\\9000\\0000002",
      "3477580",
      "FLXXA0001", "184723", "eid\\9000\\0000002");

  private final String username;
  private final String agateLoginId;
  private final String ktIdP;
  private final String extId;
  private final String sub;
}
