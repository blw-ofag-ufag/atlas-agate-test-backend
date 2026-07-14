package ch.blw.agate.user.soap;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.runtime.TokensHelper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

@Named("bearerTokenInterceptor")
@Singleton
public class BearerTokenInterceptor extends AbstractPhaseInterceptor<Message> {

  private final OidcClient oidcClient;
  private final TokensHelper tokens = new TokensHelper();

  @Inject
  public BearerTokenInterceptor(OidcClient oidcClient) {
    super(Phase.PRE_PROTOCOL);
    this.oidcClient = oidcClient;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void handleMessage(Message message) {
    String accessToken = tokens.getTokens(oidcClient).await().indefinitely().getAccessToken();

    Map<String, List<String>> headers = (Map<String, List<String>>) message.computeIfAbsent(
        Message.PROTOCOL_HEADERS, key -> new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER));
    headers.put("Authorization", List.of("Bearer " + accessToken));
  }
}
