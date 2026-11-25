package ch.blw.agate.user.soap;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class LocalDateXmlAdapter extends XmlAdapter<String, LocalDate> {

  @Override
  public LocalDate unmarshal(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return DateTimeFormatter.ISO_DATE.parse(value, LocalDate::from);
  }

  @Override
  public String marshal(LocalDate value) {
    return value == null ? null : value.format(DateTimeFormatter.ISO_LOCAL_DATE);
  }
}
