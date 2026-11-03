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
 * The connected users to upgrade to the target configuration.
 */

@Schema(name = "ConfigurationUpgradeRequest", description = "The connected users to upgrade to the target configuration.")
@JsonTypeName("ConfigurationUpgradeRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-03T16:14:51.079815+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ConfigurationUpgradeRequestModel {

  private List<Long> connectedUserIds = new ArrayList<>();

  public ConfigurationUpgradeRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConfigurationUpgradeRequestModel(List<Long> connectedUserIds) {
    this.connectedUserIds = connectedUserIds;
  }

  public ConfigurationUpgradeRequestModel connectedUserIds(List<Long> connectedUserIds) {
    this.connectedUserIds = connectedUserIds;
    return this;
  }

  public ConfigurationUpgradeRequestModel addConnectedUserIdsItem(Long connectedUserIdsItem) {
    if (this.connectedUserIds == null) {
      this.connectedUserIds = new ArrayList<>();
    }
    this.connectedUserIds.add(connectedUserIdsItem);
    return this;
  }

  /**
   * The ids of the connected users to upgrade.
   * @return connectedUserIds
   */
  @NotNull 
  @Schema(name = "connectedUserIds", description = "The ids of the connected users to upgrade.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectedUserIds")
  public List<Long> getConnectedUserIds() {
    return connectedUserIds;
  }

  @JsonProperty("connectedUserIds")
  public void setConnectedUserIds(List<Long> connectedUserIds) {
    this.connectedUserIds = connectedUserIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfigurationUpgradeRequestModel configurationUpgradeRequest = (ConfigurationUpgradeRequestModel) o;
    return Objects.equals(this.connectedUserIds, configurationUpgradeRequest.connectedUserIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectedUserIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfigurationUpgradeRequestModel {\n");
    sb.append("    connectedUserIds: ").append(toIndentedString(connectedUserIds)).append("\n");
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

