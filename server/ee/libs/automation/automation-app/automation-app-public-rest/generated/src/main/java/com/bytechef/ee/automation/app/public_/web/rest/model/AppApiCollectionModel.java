package com.bytechef.ee.automation.app.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.app.public_.web.rest.model.AppApiEndpointModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.lang.Nullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * An API collection bound to the calling app.
 */

@Schema(name = "AppApiCollection", description = "An API collection bound to the calling app.")
@JsonTypeName("AppApiCollection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:31.849354+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppApiCollectionModel {

  private Long apiCollectionId;

  private String name;

  private JsonNullable<String> contextPath = JsonNullable.<String>undefined();

  private Integer collectionVersion;

  private Boolean available;

  private List<@Valid AppApiEndpointModel> endpoints = new ArrayList<>();

  public AppApiCollectionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppApiCollectionModel(Long apiCollectionId, String name, Integer collectionVersion, Boolean available, List<@Valid AppApiEndpointModel> endpoints) {
    this.apiCollectionId = apiCollectionId;
    this.name = name;
    this.collectionVersion = collectionVersion;
    this.available = available;
    this.endpoints = endpoints;
  }

  public AppApiCollectionModel apiCollectionId(Long apiCollectionId) {
    this.apiCollectionId = apiCollectionId;
    return this;
  }

  /**
   * Get apiCollectionId
   * @return apiCollectionId
   */
  @NotNull 
  @Schema(name = "apiCollectionId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("apiCollectionId")
  public Long getApiCollectionId() {
    return apiCollectionId;
  }

  @JsonProperty("apiCollectionId")
  public void setApiCollectionId(Long apiCollectionId) {
    this.apiCollectionId = apiCollectionId;
  }

  public AppApiCollectionModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The collection name; \"#\" followed by the id when the collection no longer exists.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The collection name; \"#\" followed by the id when the collection no longer exists.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public AppApiCollectionModel contextPath(String contextPath) {
    this.contextPath = JsonNullable.of(contextPath);
    return this;
  }

  /**
   * Get contextPath
   * @return contextPath
   */
  
  @Schema(name = "contextPath", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("contextPath")
  public JsonNullable<String> getContextPath() {
    return contextPath;
  }

  public void setContextPath(JsonNullable<String> contextPath) {
    this.contextPath = contextPath;
  }

  public AppApiCollectionModel collectionVersion(Integer collectionVersion) {
    this.collectionVersion = collectionVersion;
    return this;
  }

  /**
   * Get collectionVersion
   * @return collectionVersion
   */
  @NotNull 
  @Schema(name = "collectionVersion", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("collectionVersion")
  public Integer getCollectionVersion() {
    return collectionVersion;
  }

  @JsonProperty("collectionVersion")
  public void setCollectionVersion(Integer collectionVersion) {
    this.collectionVersion = collectionVersion;
  }

  public AppApiCollectionModel available(Boolean available) {
    this.available = available;
    return this;
  }

  /**
   * False when the collection, its deployment or its project is gone or disabled; calls then fail with 409 and endpoints is empty.
   * @return available
   */
  @NotNull 
  @Schema(name = "available", description = "False when the collection, its deployment or its project is gone or disabled; calls then fail with 409 and endpoints is empty.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("available")
  public Boolean getAvailable() {
    return available;
  }

  @JsonProperty("available")
  public void setAvailable(Boolean available) {
    this.available = available;
  }

  public AppApiCollectionModel endpoints(List<@Valid AppApiEndpointModel> endpoints) {
    this.endpoints = endpoints;
    return this;
  }

  public AppApiCollectionModel addEndpointsItem(AppApiEndpointModel endpointsItem) {
    if (this.endpoints == null) {
      this.endpoints = new ArrayList<>();
    }
    this.endpoints.add(endpointsItem);
    return this;
  }

  /**
   * Get endpoints
   * @return endpoints
   */
  @NotNull 
  @Schema(name = "endpoints", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("endpoints")
  public List<@Valid AppApiEndpointModel> getEndpoints() {
    return endpoints;
  }

  @JsonProperty("endpoints")
  public void setEndpoints(List<@Valid AppApiEndpointModel> endpoints) {
    this.endpoints = endpoints;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppApiCollectionModel appApiCollection = (AppApiCollectionModel) o;
    return Objects.equals(this.apiCollectionId, appApiCollection.apiCollectionId) &&
        Objects.equals(this.name, appApiCollection.name) &&
        equalsNullable(this.contextPath, appApiCollection.contextPath) &&
        Objects.equals(this.collectionVersion, appApiCollection.collectionVersion) &&
        Objects.equals(this.available, appApiCollection.available) &&
        Objects.equals(this.endpoints, appApiCollection.endpoints);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiCollectionId, name, hashCodeNullable(contextPath), collectionVersion, available, endpoints);
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppApiCollectionModel {\n");
    sb.append("    apiCollectionId: ").append(toIndentedString(apiCollectionId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    contextPath: ").append(toIndentedString(contextPath)).append("\n");
    sb.append("    collectionVersion: ").append(toIndentedString(collectionVersion)).append("\n");
    sb.append("    available: ").append(toIndentedString(available)).append("\n");
    sb.append("    endpoints: ").append(toIndentedString(endpoints)).append("\n");
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

