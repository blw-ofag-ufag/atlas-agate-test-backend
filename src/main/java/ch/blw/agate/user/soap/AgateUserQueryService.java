package ch.blw.agate.user.soap;

import ch.blw.agate.common.exceptions.ExternalWebServiceException;
import ch.blw.agate.user.dto.TvdUserDto;
import ch.blw.agate.user.soap.generated.AdminService;
import ch.blw.agate.user.soap.generated.BusinessException;
import ch.blw.agate.user.soap.generated.TechnicalException;
import ch.blw.agate.user.soap.generated.User;
import ch.blw.agate.user.soap.generated.UserGetByLoginId;
import ch.blw.agate.user.soap.generated.UserQuery;
import io.quarkiverse.cxf.annotation.CXFClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class AgateUserQueryService {

  static final int DEFAULT_NUM_RECORDS = 1000;

  private final AdminService adminService;

  @Inject
  public AgateUserQueryService(@CXFClient("adminService") AdminService adminService) {
    this.adminService = adminService;
  }

  public List<TvdUserDto> queryUsers() {
    try {
      return adminService.queryUsers(buildQuery()).stream().map(AgateUserQueryService::toDto).toList();
    } catch (BusinessException | TechnicalException e) {
      throw new ExternalWebServiceException("Agate SOAP queryUsers failed", e);
    }
  }

  public List<TvdUserDto> getUsersByLoginId(List<String> loginIds) {
    try {
      return adminService.getUsersByLoginId(buildGetByLoginId(loginIds)).stream()
          .map(AgateUserQueryService::toDto).toList();
    } catch (BusinessException | TechnicalException e) {
      throw new ExternalWebServiceException("Agate SOAP getUsersByLoginId failed", e);
    }
  }

  static UserQuery buildQuery() {
    UserQuery query = new UserQuery();
    query.setNumRecords(DEFAULT_NUM_RECORDS);
    query.setUser(new User());
    return query;
  }

  static UserGetByLoginId buildGetByLoginId(List<String> loginIds) {
    UserGetByLoginId get = new UserGetByLoginId();
    if (loginIds != null) {
      get.getLoginIds().addAll(loginIds);
    }
    return get;
  }

  static TvdUserDto toDto(User user) {
    return TvdUserDto.builder()
        .loginId(user.getLoginId())
        .firstName(user.getFirstName())
        .name(user.getName())
        .extId(user.getExtId())
        .clientName(user.getClientName())
        .build();
  }
}
