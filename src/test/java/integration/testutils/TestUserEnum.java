package integration.testutils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TestUserEnum {
  EINWILLIGER_ERIKA("eid90000000001",
      "3477580",
      "FLXXA0001", "184723", "eid\\9000\\0000001"),
  DATENANBIETER_LUKAS("eid90000000002",
      "3477580",
      "FLXXA0001", "184723", "eid\\9000\\0000002");

  private final String username;
  private final String agateLoginId;
  private final String ktIdP;
  private final String extId;
  private final String sub;
}
