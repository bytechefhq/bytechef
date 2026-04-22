package com.bytechef.ee.automation.ai.gateway.public_.web.rest.model;

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
 * A routing policy.
 */

@Schema(name = "RoutingPolicy", description = "A routing policy.")
@JsonTypeName("RoutingPolicy")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-07T21:40:02.582901+02:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class RoutingPolicyModel {

  private @Nullable String id;

  private @Nullable String name;

  private @Nullable String strategy;

  private @Nullable Boolean enabled;

  public RoutingPolicyModel id(@Nullable String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  
  @Schema(name = "id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable String getId() {
    return id;
  }

  public void setId(@Nullable String id) {
    this.id = id;
  }

  public RoutingPolicyModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The policy name.
   * @return name
   */
  
  @Schema(name = "name", description = "The policy name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  public RoutingPolicyModel strategy(@Nullable String strategy) {
    this.strategy = strategy;
    return this;
  }

  /**
   * The routing strategy used by this policy.
   * @return strategy
   */
  
  @Schema(name = "strategy", description = "The routing strategy used by this policy.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("strategy")
  public @Nullable String getStrategy() {
    return strategy;
  }

  public void setStrategy(@Nullable String strategy) {
    this.strategy = strategy;
  }

  public RoutingPolicyModel enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Whether the policy is enabled.
   * @return enabled
   */
  
  @Schema(name = "enabled", description = "Whether the policy is enabled.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public @Nullable Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(@Nullable Boolean enabled) {
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
    RoutingPolicyModel routingPolicy = (RoutingPolicyModel) o;
    return Objects.equals(this.id, routingPolicy.id) &&
        Objects.equals(this.name, routingPolicy.name) &&
        Objects.equals(this.strategy, routingPolicy.strategy) &&
        Objects.equals(this.enabled, routingPolicy.enabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, strategy, enabled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoutingPolicyModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    strategy: ").append(toIndentedString(strategy)).append("\n");
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

