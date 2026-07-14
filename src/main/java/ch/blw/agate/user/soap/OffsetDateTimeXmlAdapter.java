package ch.blw.agate.user.soap;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class OffsetDateTimeXmlAdapter extends XmlAdapter<String, OffsetDateTime> {

  @Override
  public OffsetDateTime unmarshal(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    var parsed = DateTimeFormatter.ISO_DATE_TIME.parseBest(value, OffsetDateTime::from, LocalDateTime::from);
    return parsed instanceof OffsetDateTime odt
        ? odt
        : ((LocalDateTime) parsed).atOffset(ZoneOffset.UTC);
  }

  @Override
  public String marshal(OffsetDateTime value) {
    return value == null ? null : value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }
}
