package com.bytechef.ee.embedded.configuration.web.rest.model;

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
 * Which connected user moves to which version, and optionally to which configuration.
 */

@Schema(name = "ConfigurationChangeVersionRequest", description = "Which connected user moves to which version, and optionally to which configuration.")
@JsonTypeName("ConfigurationChangeVersionRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-20T07:40:29.877920+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class ConfigurationChangeVersionRequestModel {

  private Long connectedUserId;

  private @Nullable Long targetId;

  private Integer version;

  public ConfigurationChangeVersionRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConfigurationChangeVersionRequestModel(Long connectedUserId, Integer version) {
    this.connectedUserId = connectedUserId;
    this.version = version;
  }

  public ConfigurationChangeVersionRequestModel connectedUserId(Long connectedUserId) {
    this.connectedUserId = connectedUserId;
    return this;
  }

  /**
   * The id of the connected user to move.
   * @return connectedUserId
   */
  @NotNull 
  @Schema(name = "connectedUserId", description = "The id of the connected user to move.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectedUserId")
  public Long getConnectedUserId() {
    return connectedUserId;
  }

  @JsonProperty("connectedUserId")
  public void setConnectedUserId(Long connectedUserId) {
    this.connectedUserId = connectedUserId;
  }

  public ConfigurationChangeVersionRequestModel targetId(@Nullable Long targetId) {
    this.targetId = targetId;
    return this;
  }

  /**
   * The id of an enabled configuration already on that version. When absent, one is chosen or a copy of the source is created.
   * @return targetId
   */
  
  @Schema(name = "targetId", description = "The id of an enabled configuration already on that version. When absent, one is chosen or a copy of the source is created.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("targetId")
  public @Nullable Long getTargetId() {
    return targetId;
  }

  @JsonProperty("targetId")
  public void setTargetId(@Nullable Long targetId) {
    this.targetId = targetId;
  }

  public ConfigurationChangeVersionRequestModel version(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * The published version to move the connected user to.
   * @return version
   */
  @NotNull 
  @Schema(name = "version", description = "The published version to move the connected user to.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("version")
  public Integer getVersion() {
    return version;
  }

  @JsonProperty("version")
  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfigurationChangeVersionRequestModel configurationChangeVersionRequest = (ConfigurationChangeVersionRequestModel) o;
    return Objects.equals(this.connectedUserId, configurationChangeVersionRequest.connectedUserId) &&
        Objects.equals(this.targetId, configurationChangeVersionRequest.targetId) &&
        Objects.equals(this.version, configurationChangeVersionRequest.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectedUserId, targetId, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfigurationChangeVersionRequestModel {\n");
    sb.append("    connectedUserId: ").append(toIndentedString(connectedUserId)).append("\n");
    sb.append("    targetId: ").append(toIndentedString(targetId)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

