package ch.blw.agate.user.soap;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class XmlAdapterTest {

  private final OffsetDateTimeXmlAdapter offsetDateTimeAdapter = new OffsetDateTimeXmlAdapter();
  private final LocalDateXmlAdapter localDateAdapter = new LocalDateXmlAdapter();

  @Test
  void offsetDateTime_parsesOffsetAndMarshalsBack() {
    OffsetDateTime value = OffsetDateTime.parse("2026-04-20T14:40:53+02:00");

    assertThat(offsetDateTimeAdapter.unmarshal("2026-04-20T14:40:53+02:00")).isEqualTo(value);
    assertThat(offsetDateTimeAdapter.marshal(value)).isEqualTo("2026-04-20T14:40:53+02:00");
    assertThat(offsetDateTimeAdapter.unmarshal(null)).isNull();
    assertThat(offsetDateTimeAdapter.marshal(null)).isNull();
  }

  @Test
  void offsetDateTime_treatsNaiveInputAsUtc() {
    assertThat(offsetDateTimeAdapter.unmarshal("2026-04-20T00:00:00").getOffset()).isEqualTo(ZoneOffset.UTC);
  }

  @Test
  void localDate_parsesAndMarshalsBack() {
    assertThat(localDateAdapter.unmarshal("2026-04-20")).isEqualTo(LocalDate.of(2026, 4, 20));
    assertThat(localDateAdapter.marshal(LocalDate.of(2026, 4, 20))).isEqualTo("2026-04-20");
    assertThat(localDateAdapter.unmarshal(null)).isNull();
    assertThat(localDateAdapter.marshal(null)).isNull();
  }
}
