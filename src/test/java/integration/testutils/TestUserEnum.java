package integration.testutils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TestUserEnum {
  PRODUCER_LUKAS("blw.test.producer.lukas@proton.me",
      "3477580",
      "FLXXA0001", "184723", "f:8a3a58d6-b5d3-482b-830a-6fd4791f4649:184723");

  private final String username;
  private final String agateLoginId;
  private final String ktIdP;
  private final String extId;
  private final String sub;
}
