package ch.blw.agate.user.soap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Map;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.Test;

class BearerTokenInterceptorTest {

  @Test
  @SuppressWarnings("unchecked")
  void handleMessage_addsBearerAuthorizationHeader() {
    Tokens tokens = mock(Tokens.class);
    when(tokens.getAccessToken()).thenReturn("token-abc");
    OidcClient oidcClient = mock(OidcClient.class);
    when(oidcClient.getTokens()).thenReturn(Uni.createFrom().item(tokens));

    Message message = new MessageImpl();
    new BearerTokenInterceptor(oidcClient).handleMessage(message);

    Map<String, List<String>> headers = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
    assertThat(headers.get("Authorization")).containsExactly("Bearer token-abc");
  }
}
