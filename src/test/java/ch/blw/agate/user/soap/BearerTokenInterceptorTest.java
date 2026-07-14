package ch.blw.agate.user.soap;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Map;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BearerTokenInterceptorTest {

  @Test
  @SuppressWarnings("unchecked")
  void handleMessage_addsBearerAuthorizationHeader() {
    Tokens tokens = Mockito.mock(Tokens.class);
    Mockito.when(tokens.getAccessToken()).thenReturn("token-abc");
    OidcClient oidcClient = Mockito.mock(OidcClient.class);
    Mockito.when(oidcClient.getTokens()).thenReturn(Uni.createFrom().item(tokens));

    BearerTokenInterceptor interceptor = new BearerTokenInterceptor();
    interceptor.oidcClient = oidcClient;

    Message message = new MessageImpl();
    interceptor.handleMessage(message);

    Map<String, List<String>> headers = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
    assertThat(headers.get("Authorization")).containsExactly("Bearer token-abc");
  }
}
