package com.bytechef.ee.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The target configuration and the connected users to move to it.
 */

@Schema(name = "ConfigurationMoveRequest", description = "The target configuration and the connected users to move to it.")
@JsonTypeName("ConfigurationMoveRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-03T02:20:55.684519+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ConfigurationMoveRequestModel {

  private List<Long> connectedUserIds = new ArrayList<>();

  private Long targetId;

  public ConfigurationMoveRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConfigurationMoveRequestModel(List<Long> connectedUserIds, Long targetId) {
    this.connectedUserIds = connectedUserIds;
    this.targetId = targetId;
  }

  public ConfigurationMoveRequestModel connectedUserIds(List<Long> connectedUserIds) {
    this.connectedUserIds = connectedUserIds;
    return this;
  }

  public ConfigurationMoveRequestModel addConnectedUserIdsItem(Long connectedUserIdsItem) {
    if (this.connectedUserIds == null) {
      this.connectedUserIds = new ArrayList<>();
    }
    this.connectedUserIds.add(connectedUserIdsItem);
    return this;
  }

  /**
   * The ids of the connected users to move.
   * @return connectedUserIds
   */
  @NotNull 
  @Schema(name = "connectedUserIds", description = "The ids of the connected users to move.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectedUserIds")
  public List<Long> getConnectedUserIds() {
    return connectedUserIds;
  }

  @JsonProperty("connectedUserIds")
  public void setConnectedUserIds(List<Long> connectedUserIds) {
    this.connectedUserIds = connectedUserIds;
  }

  public ConfigurationMoveRequestModel targetId(Long targetId) {
    this.targetId = targetId;
    return this;
  }

  /**
   * The id of the configuration to move the connected users to.
   * @return targetId
   */
  @NotNull 
  @Schema(name = "targetId", description = "The id of the configuration to move the connected users to.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("targetId")
  public Long getTargetId() {
    return targetId;
  }

  @JsonProperty("targetId")
  public void setTargetId(Long targetId) {
    this.targetId = targetId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfigurationMoveRequestModel configurationMoveRequest = (ConfigurationMoveRequestModel) o;
    return Objects.equals(this.connectedUserIds, configurationMoveRequest.connectedUserIds) &&
        Objects.equals(this.targetId, configurationMoveRequest.targetId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectedUserIds, targetId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfigurationMoveRequestModel {\n");
    sb.append("    connectedUserIds: ").append(toIndentedString(connectedUserIds)).append("\n");
    sb.append("    targetId: ").append(toIndentedString(targetId)).append("\n");
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

