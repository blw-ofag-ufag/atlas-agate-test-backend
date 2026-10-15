package ch.blw.agate.common.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.jupiter.api.Test;

class OpenApiAudienceExtensionTest {

  private final OpenApiAudienceExtension filter = new OpenApiAudienceExtension();

  @Test
  void givenNoInfo_thenInfoIsCreatedWithAudience() {
    OpenAPI openApi = OASFactory.createOpenAPI();

    filter.filterOpenAPI(openApi);

    assertThat(openApi.getInfo().getExtensions()).containsEntry("x-audience", "public");
  }

  @Test
  void givenExistingInfo_thenAudienceIsAddedAndInfoKept() {
    OpenAPI openApi = OASFactory.createOpenAPI()
        .info(OASFactory.createInfo().title("agate test API"));

    filter.filterOpenAPI(openApi);

    assertThat(openApi.getInfo().getTitle()).isEqualTo("agate test API");
    assertThat(openApi.getInfo().getExtensions()).containsEntry("x-audience", "public");
  }
}
