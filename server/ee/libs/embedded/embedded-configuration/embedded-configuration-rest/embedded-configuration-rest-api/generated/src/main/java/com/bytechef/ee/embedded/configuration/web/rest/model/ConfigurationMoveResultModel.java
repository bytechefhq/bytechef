package com.bytechef.ee.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConfigurationMoveSkippedModel;
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
 * The outcome of moving connected users to another configuration.
 */

@Schema(name = "ConfigurationMoveResult", description = "The outcome of moving connected users to another configuration.")
@JsonTypeName("ConfigurationMoveResult")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-03T02:20:55.684519+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ConfigurationMoveResultModel {

  private List<Long> movedConnectedUserIds = new ArrayList<>();

  private List<Long> needsAttentionConnectedUserIds = new ArrayList<>();

  private List<@Valid ConfigurationMoveSkippedModel> skipped = new ArrayList<>();

  public ConfigurationMoveResultModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConfigurationMoveResultModel(List<Long> movedConnectedUserIds, List<Long> needsAttentionConnectedUserIds, List<@Valid ConfigurationMoveSkippedModel> skipped) {
    this.movedConnectedUserIds = movedConnectedUserIds;
    this.needsAttentionConnectedUserIds = needsAttentionConnectedUserIds;
    this.skipped = skipped;
  }

  public ConfigurationMoveResultModel movedConnectedUserIds(List<Long> movedConnectedUserIds) {
    this.movedConnectedUserIds = movedConnectedUserIds;
    return this;
  }

  public ConfigurationMoveResultModel addMovedConnectedUserIdsItem(Long movedConnectedUserIdsItem) {
    if (this.movedConnectedUserIds == null) {
      this.movedConnectedUserIds = new ArrayList<>();
    }
    this.movedConnectedUserIds.add(movedConnectedUserIdsItem);
    return this;
  }

  /**
   * The ids of the connected users now assigned to the target.
   * @return movedConnectedUserIds
   */
  @NotNull 
  @Schema(name = "movedConnectedUserIds", description = "The ids of the connected users now assigned to the target.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("movedConnectedUserIds")
  public List<Long> getMovedConnectedUserIds() {
    return movedConnectedUserIds;
  }

  @JsonProperty("movedConnectedUserIds")
  public void setMovedConnectedUserIds(List<Long> movedConnectedUserIds) {
    this.movedConnectedUserIds = movedConnectedUserIds;
  }

  public ConfigurationMoveResultModel needsAttentionConnectedUserIds(List<Long> needsAttentionConnectedUserIds) {
    this.needsAttentionConnectedUserIds = needsAttentionConnectedUserIds;
    return this;
  }

  public ConfigurationMoveResultModel addNeedsAttentionConnectedUserIdsItem(Long needsAttentionConnectedUserIdsItem) {
    if (this.needsAttentionConnectedUserIds == null) {
      this.needsAttentionConnectedUserIds = new ArrayList<>();
    }
    this.needsAttentionConnectedUserIds.add(needsAttentionConnectedUserIdsItem);
    return this;
  }

  /**
   * The ids of the moved connected users whose turned-on workflows need attention on the target.
   * @return needsAttentionConnectedUserIds
   */
  @NotNull 
  @Schema(name = "needsAttentionConnectedUserIds", description = "The ids of the moved connected users whose turned-on workflows need attention on the target.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("needsAttentionConnectedUserIds")
  public List<Long> getNeedsAttentionConnectedUserIds() {
    return needsAttentionConnectedUserIds;
  }

  @JsonProperty("needsAttentionConnectedUserIds")
  public void setNeedsAttentionConnectedUserIds(List<Long> needsAttentionConnectedUserIds) {
    this.needsAttentionConnectedUserIds = needsAttentionConnectedUserIds;
  }

  public ConfigurationMoveResultModel skipped(List<@Valid ConfigurationMoveSkippedModel> skipped) {
    this.skipped = skipped;
    return this;
  }

  public ConfigurationMoveResultModel addSkippedItem(ConfigurationMoveSkippedModel skippedItem) {
    if (this.skipped == null) {
      this.skipped = new ArrayList<>();
    }
    this.skipped.add(skippedItem);
    return this;
  }

  /**
   * The connected users left where they were, with the reason.
   * @return skipped
   */
  @NotNull 
  @Schema(name = "skipped", description = "The connected users left where they were, with the reason.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("skipped")
  public List<@Valid ConfigurationMoveSkippedModel> getSkipped() {
    return skipped;
  }

  @JsonProperty("skipped")
  public void setSkipped(List<@Valid ConfigurationMoveSkippedModel> skipped) {
    this.skipped = skipped;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfigurationMoveResultModel configurationMoveResult = (ConfigurationMoveResultModel) o;
    return Objects.equals(this.movedConnectedUserIds, configurationMoveResult.movedConnectedUserIds) &&
        Objects.equals(this.needsAttentionConnectedUserIds, configurationMoveResult.needsAttentionConnectedUserIds) &&
        Objects.equals(this.skipped, configurationMoveResult.skipped);
  }

  @Override
  public int hashCode() {
    return Objects.hash(movedConnectedUserIds, needsAttentionConnectedUserIds, skipped);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfigurationMoveResultModel {\n");
    sb.append("    movedConnectedUserIds: ").append(toIndentedString(movedConnectedUserIds)).append("\n");
    sb.append("    needsAttentionConnectedUserIds: ").append(toIndentedString(needsAttentionConnectedUserIds)).append("\n");
    sb.append("    skipped: ").append(toIndentedString(skipped)).append("\n");
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

