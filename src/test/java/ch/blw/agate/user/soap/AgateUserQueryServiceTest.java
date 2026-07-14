package ch.blw.agate.user.soap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.blw.agate.common.exceptions.ExternalWebServiceException;
import ch.blw.agate.user.dto.TvdUserDto;
import ch.blw.agate.user.soap.generated.AdminService;
import ch.blw.agate.user.soap.generated.BusinessException;
import ch.blw.agate.user.soap.generated.QueryUsersResponse;
import ch.blw.agate.user.soap.generated.User;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.List;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AgateUserQueryServiceTest {

  @Test
  void buildQuery_setsNumRecordsAndUser() {
    var query = AgateUserQueryService.buildQuery();

    assertThat(query.getNumRecords()).isEqualTo(AgateUserQueryService.DEFAULT_NUM_RECORDS);
    assertThat(query.getUser()).isNotNull();
  }

  @Test
  void toDto_mapsUserFields() {
    TvdUserDto dto = AgateUserQueryService.toDto(user("3365033", "Ramon", "Rüfenacht", "184723"));

    assertThat(dto).isEqualTo(new TvdUserDto("3365033", "Ramon", "Rüfenacht", "184723", "Default"));
  }

  @Test
  void queryUsers_mapsClientResponseToDtos() throws Exception {
    AdminService client = Mockito.mock(AdminService.class);
    Mockito.when(client.queryUsers(Mockito.any()))
        .thenReturn(List.of(user("3365033", "Ramon", "Rüfenacht", "184723"), user("9811215", "David", "Oberli", "184724")));

    List<TvdUserDto> dtos = new AgateUserQueryService(client).queryUsers();

    assertThat(dtos).extracting(TvdUserDto::loginId).containsExactly("3365033", "9811215");
    assertThat(dtos).extracting(TvdUserDto::name).containsExactly("Rüfenacht", "Oberli");
  }

  @Test
  void queryUsers_wrapsSoapFaultInExternalWebServiceException() throws Exception {
    AdminService client = Mockito.mock(AdminService.class);
    Mockito.when(client.queryUsers(Mockito.any())).thenThrow(new BusinessException("boom"));

    assertThatThrownBy(() -> new AgateUserQueryService(client).queryUsers())
        .isInstanceOf(ExternalWebServiceException.class);
  }

  @Test
  void toDto_mapsUnmarshalledResponseFromSampleXml() throws Exception {
    List<User> users = unmarshalSampleResponse().getReturn();

    List<TvdUserDto> dtos = users.stream().map(AgateUserQueryService::toDto).toList();

    assertThat(dtos).containsExactly(
        new TvdUserDto("3365033", "Ramon", "Rüfenacht", "184723", "Default"),
        new TvdUserDto("9811215", "David", "Oberli", "184724", "Default"));
  }

  private static User user(String loginId, String firstName, String name, String extId) {
    User user = new User();
    user.setLoginId(loginId);
    user.setFirstName(firstName);
    user.setName(name);
    user.setExtId(extId);
    user.setClientName("Default");
    return user;
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
