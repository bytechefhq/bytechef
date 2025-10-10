package com.bytechef.ee.platform.apiconnector.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ImportOpenApiSpecificationRequestModel
 */

@JsonTypeName("importOpenApiSpecification_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-07T14:49:50.020869+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class ImportOpenApiSpecificationRequestModel {

  private String name;

  private @Nullable String icon;

  private String specification;

  public ImportOpenApiSpecificationRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ImportOpenApiSpecificationRequestModel(String name, String specification) {
    this.name = name;
    this.specification = specification;
  }

  public ImportOpenApiSpecificationRequestModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The component name of an API Connector.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The component name of an API Connector.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ImportOpenApiSpecificationRequestModel icon(@Nullable String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * The icon of an API Connector.
   * @return icon
   */
  
  @Schema(name = "icon", description = "The icon of an API Connector.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("icon")
  public @Nullable String getIcon() {
    return icon;
  }

  public void setIcon(@Nullable String icon) {
    this.icon = icon;
  }

  public ImportOpenApiSpecificationRequestModel specification(String specification) {
    this.specification = specification;
    return this;
  }

  /**
   * The OpenAPI specification.
   * @return specification
   */
  @NotNull 
  @Schema(name = "specification", description = "The OpenAPI specification.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("specification")
  public String getSpecification() {
    return specification;
  }

  public void setSpecification(String specification) {
    this.specification = specification;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImportOpenApiSpecificationRequestModel importOpenApiSpecificationRequest = (ImportOpenApiSpecificationRequestModel) o;
    return Objects.equals(this.name, importOpenApiSpecificationRequest.name) &&
        Objects.equals(this.icon, importOpenApiSpecificationRequest.icon) &&
        Objects.equals(this.specification, importOpenApiSpecificationRequest.specification);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, icon, specification);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ImportOpenApiSpecificationRequestModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    specification: ").append(toIndentedString(specification)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

