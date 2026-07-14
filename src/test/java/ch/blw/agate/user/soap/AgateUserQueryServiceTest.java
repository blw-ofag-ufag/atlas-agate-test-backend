package ch.blw.agate.user.soap;

import static org.assertj.core.api.Assertions.assertThat;

import ch.blw.agate.user.dto.TvdUserDto;
import ch.blw.agate.user.soap.generated.QueryUsersResponse;
import ch.blw.agate.user.soap.generated.User;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.List;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.Test;

class AgateUserQueryServiceTest {

  @Test
  void buildQuery_setsNumRecordsAndUser() {
    var query = AgateUserQueryService.buildQuery();

    assertThat(query.getNumRecords()).isEqualTo(AgateUserQueryService.DEFAULT_NUM_RECORDS);
    assertThat(query.getUser()).isNotNull();
  }

  @Test
  void toDto_mapsUserFields() {
    User user = new User();
    user.setLoginId("3365033");
    user.setFirstName("Ramon");
    user.setName("Rüfenacht");
    user.setExtId("184723");
    user.setClientName("Default");

    TvdUserDto dto = AgateUserQueryService.toDto(user);

    assertThat(dto).isEqualTo(new TvdUserDto("3365033", "Ramon", "Rüfenacht", "184723", "Default"));
  }

  @Test
  void toDto_mapsUnmarshalledResponseFromSampleXml() throws Exception {
    List<User> users = unmarshalSampleResponse().getReturn();

    List<TvdUserDto> dtos = users.stream().map(AgateUserQueryService::toDto).toList();

    assertThat(dtos).containsExactly(
        new TvdUserDto("3365033", "Ramon", "Rüfenacht", "184723", "Default"),
        new TvdUserDto("9811215", "David", "Oberli", "184724", "Default"));
  }

  private QueryUsersResponse unmarshalSampleResponse() throws Exception {
    JAXBContext context = JAXBContext.newInstance(QueryUsersResponse.class);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    try (InputStream xml = getClass().getResourceAsStream("/soap/queryUsersResponse-sample.xml")) {
      JAXBElement<QueryUsersResponse> element = unmarshaller.unmarshal(new StreamSource(xml), QueryUsersResponse.class);
      return element.getValue();
    }
  }
}
