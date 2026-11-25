package ch.blw.agate.common.services;

import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.runtime.OidcJwtCallerPrincipal;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Provides access to user-related security claims. It extracts identifiers, email, and UID from security tokens and ensures validity.
 *
 * @CommentLastReviewed 2025-08-25
 */

@RequestScoped
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

  public static final String AGATE_AGRIDATA_PRODUCER_ROLE = "agridata.ch.Agridata_Einwilliger";
  public static final String AGATE_BENUTZER_ROLE = "agate.AgateBenutzer";

  public static final String ACCESS_TOKEN_CLAIM_AGATE_LOGIN_ID = "loginid";
  private static final String ACCESS_TOKEN_CLAIM_UID = "uid";
  private static final String ACCESS_TOKEN_CLAIM_KT_ID_P = "KT_ID_P";
  private static final String ACCESS_TOKEN_CLAIM_SUB = "sub";


  private final SecurityIdentity securityIdentity;


  public UserInfo getUserInfoOrElseThrow() {
    UserInfo userInfo = securityIdentity.getAttribute("userinfo");
    if (userInfo == null) {
      throw new IllegalStateException("UserInfo of user with subject Id " + getSubjectId() + " not found");
    }
    return userInfo;
  }

  public String getSubjectId() {
    return this.extractClaim(ACCESS_TOKEN_CLAIM_SUB);
  }

  public boolean isAnonymous() {
    return securityIdentity.isAnonymous();
  }

  public Map<String, Object> getClaims() {
    var principal = securityIdentity.getPrincipal();
    if (principal instanceof OidcJwtCallerPrincipal jwt) {
      return jwt.getClaims().getClaimsMap();
    } else {
      return Map.of();
    }
  }

  public Set<String> getRoles() {
    return securityIdentity.getRoles();
  }

  private String extractClaim(String claimName) {
    var principal = securityIdentity.getPrincipal();
    if (!(principal instanceof JsonWebToken jwt)) {
      throw new IllegalStateException(
          "Expected authenticated principal to be a JsonWebToken when extracting claim '" + claimName + "'");
    }
    String claimValue = jwt.getClaim(claimName);
    if (claimValue == null) {
      throw new IllegalStateException("Required claim '" + claimName + "' not found in access token");
    }
    return claimValue;
  }


}
