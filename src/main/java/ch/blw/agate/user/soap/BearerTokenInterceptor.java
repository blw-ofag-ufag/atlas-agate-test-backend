package ch.blw.agate.user.soap;

import io.quarkus.oidc.client.OidcClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

@Named("bearerTokenInterceptor")
@ApplicationScoped
public class BearerTokenInterceptor extends AbstractPhaseInterceptor<Message> {

  @Inject
  OidcClient oidcClient;

  public BearerTokenInterceptor() {
    super(Phase.PRE_PROTOCOL);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void handleMessage(Message message) {
    String accessToken = oidcClient.getTokens().await().indefinitely().getAccessToken();

    Map<String, List<String>> headers = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
    if (headers == null) {
      headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      message.put(Message.PROTOCOL_HEADERS, headers);
    }
    headers.put("Authorization", List.of("Bearer " + accessToken));
  }
}
