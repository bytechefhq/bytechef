package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.CredentialStatusModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The integration instance represents a configured integration for a specific user, containing connection and status information
 */

@Schema(name = "IntegrationInstanceBasic", description = "The integration instance represents a configured integration for a specific user, containing connection and status information")
@JsonTypeName("IntegrationInstanceBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-12T08:42:40.890500807+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class IntegrationInstanceBasicModel {

  private @Nullable Long id;

  private @Nullable CredentialStatusModel credentialStatus;

  private Boolean enabled;

  public IntegrationInstanceBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IntegrationInstanceBasicModel(Boolean enabled) {
    this.enabled = enabled;
  }

  public IntegrationInstanceBasicModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an integration instance.
   * @return id
   */
  
  @Schema(name = "id", description = "The id of an integration instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public IntegrationInstanceBasicModel credentialStatus(@Nullable CredentialStatusModel credentialStatus) {
    this.credentialStatus = credentialStatus;
    return this;
  }

  /**
   * Get credentialStatus
   * @return credentialStatus
   */
  @Valid 
  @Schema(name = "credentialStatus", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("credentialStatus")
  public @Nullable CredentialStatusModel getCredentialStatus() {
    return credentialStatus;
  }

  public void setCredentialStatus(@Nullable CredentialStatusModel credentialStatus) {
    this.credentialStatus = credentialStatus;
  }

  public IntegrationInstanceBasicModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If an integration instance is enabled or not
   * @return enabled
   */
  @NotNull 
  @Schema(name = "enabled", description = "If an integration instance is enabled or not", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntegrationInstanceBasicModel integrationInstanceBasic = (IntegrationInstanceBasicModel) o;
    return Objects.equals(this.id, integrationInstanceBasic.id) &&
        Objects.equals(this.credentialStatus, integrationInstanceBasic.credentialStatus) &&
        Objects.equals(this.enabled, integrationInstanceBasic.enabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, credentialStatus, enabled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationInstanceBasicModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    credentialStatus: ").append(toIndentedString(credentialStatus)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

