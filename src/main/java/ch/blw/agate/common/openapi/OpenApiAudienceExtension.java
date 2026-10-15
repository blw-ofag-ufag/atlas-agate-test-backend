package ch.blw.agate.common.openapi;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;

/** Adds the {@code x-audience} extension to the info section (not configurable via properties). */
@OpenApiFilter(stages = OpenApiFilter.RunStage.BUILD)
public class OpenApiAudienceExtension implements OASFilter {

  @Override
  public void filterOpenAPI(OpenAPI openApi) {
    // info-* properties are applied after build-stage filters, so the Info object may not exist yet
    if (openApi.getInfo() == null) {
      openApi.setInfo(OASFactory.createInfo());
    }
    openApi.getInfo().addExtension("x-audience", "public");
  }
}
