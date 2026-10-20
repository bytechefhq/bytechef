package com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.ApiCollectionMissingConnectionModel;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.ApiCollectionMissingInputModel;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.ApiCollectionModel;
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
 * The outcome of moving an API collection to a newer project version.
 */

@Schema(name = "ApiCollectionVersionUpdate", description = "The outcome of moving an API collection to a newer project version.")
@JsonTypeName("ApiCollectionVersionUpdate")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T01:48:59.391908+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ApiCollectionVersionUpdateModel {

  private @Nullable ApiCollectionModel apiCollection;

  private @Nullable List<String> removedEndpointNames;

  private @Nullable List<String> disabledEndpointNames;

  private @Nullable List<@Valid ApiCollectionMissingConnectionModel> missingConnections;

  private @Nullable List<@Valid ApiCollectionMissingInputModel> missingInputs;

  public ApiCollectionVersionUpdateModel apiCollection(@Nullable ApiCollectionModel apiCollection) {
    this.apiCollection = apiCollection;
    return this;
  }

  /**
   * Get apiCollection
   * @return apiCollection
   */
  @Valid 
  @Schema(name = "apiCollection", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("apiCollection")
  public @Nullable ApiCollectionModel getApiCollection() {
    return apiCollection;
  }

  @JsonProperty("apiCollection")
  public void setApiCollection(@Nullable ApiCollectionModel apiCollection) {
    this.apiCollection = apiCollection;
  }

  public ApiCollectionVersionUpdateModel removedEndpointNames(@Nullable List<String> removedEndpointNames) {
    this.removedEndpointNames = removedEndpointNames;
    return this;
  }

  public ApiCollectionVersionUpdateModel addRemovedEndpointNamesItem(String removedEndpointNamesItem) {
    if (this.removedEndpointNames == null) {
      this.removedEndpointNames = new ArrayList<>();
    }
    this.removedEndpointNames.add(removedEndpointNamesItem);
    return this;
  }

  /**
   * Get removedEndpointNames
   * @return removedEndpointNames
   */
  
  @Schema(name = "removedEndpointNames", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("removedEndpointNames")
  public @Nullable List<String> getRemovedEndpointNames() {
    return removedEndpointNames;
  }

  @JsonProperty("removedEndpointNames")
  public void setRemovedEndpointNames(@Nullable List<String> removedEndpointNames) {
    this.removedEndpointNames = removedEndpointNames;
  }

  public ApiCollectionVersionUpdateModel disabledEndpointNames(@Nullable List<String> disabledEndpointNames) {
    this.disabledEndpointNames = disabledEndpointNames;
    return this;
  }

  public ApiCollectionVersionUpdateModel addDisabledEndpointNamesItem(String disabledEndpointNamesItem) {
    if (this.disabledEndpointNames == null) {
      this.disabledEndpointNames = new ArrayList<>();
    }
    this.disabledEndpointNames.add(disabledEndpointNamesItem);
    return this;
  }

  /**
   * Get disabledEndpointNames
   * @return disabledEndpointNames
   */
  
  @Schema(name = "disabledEndpointNames", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("disabledEndpointNames")
  public @Nullable List<String> getDisabledEndpointNames() {
    return disabledEndpointNames;
  }

  @JsonProperty("disabledEndpointNames")
  public void setDisabledEndpointNames(@Nullable List<String> disabledEndpointNames) {
    this.disabledEndpointNames = disabledEndpointNames;
  }

  public ApiCollectionVersionUpdateModel missingConnections(@Nullable List<@Valid ApiCollectionMissingConnectionModel> missingConnections) {
    this.missingConnections = missingConnections;
    return this;
  }

  public ApiCollectionVersionUpdateModel addMissingConnectionsItem(ApiCollectionMissingConnectionModel missingConnectionsItem) {
    if (this.missingConnections == null) {
      this.missingConnections = new ArrayList<>();
    }
    this.missingConnections.add(missingConnectionsItem);
    return this;
  }

  /**
   * Get missingConnections
   * @return missingConnections
   */
  
  @Schema(name = "missingConnections", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("missingConnections")
  public @Nullable List<@Valid ApiCollectionMissingConnectionModel> getMissingConnections() {
    return missingConnections;
  }

  @JsonProperty("missingConnections")
  public void setMissingConnections(@Nullable List<@Valid ApiCollectionMissingConnectionModel> missingConnections) {
    this.missingConnections = missingConnections;
  }

  public ApiCollectionVersionUpdateModel missingInputs(@Nullable List<@Valid ApiCollectionMissingInputModel> missingInputs) {
    this.missingInputs = missingInputs;
    return this;
  }

  public ApiCollectionVersionUpdateModel addMissingInputsItem(ApiCollectionMissingInputModel missingInputsItem) {
    if (this.missingInputs == null) {
      this.missingInputs = new ArrayList<>();
    }
    this.missingInputs.add(missingInputsItem);
    return this;
  }

  /**
   * Get missingInputs
   * @return missingInputs
   */
  
  @Schema(name = "missingInputs", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("missingInputs")
  public @Nullable List<@Valid ApiCollectionMissingInputModel> getMissingInputs() {
    return missingInputs;
  }

  @JsonProperty("missingInputs")
  public void setMissingInputs(@Nullable List<@Valid ApiCollectionMissingInputModel> missingInputs) {
    this.missingInputs = missingInputs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiCollectionVersionUpdateModel apiCollectionVersionUpdate = (ApiCollectionVersionUpdateModel) o;
    return Objects.equals(this.apiCollection, apiCollectionVersionUpdate.apiCollection) &&
        Objects.equals(this.removedEndpointNames, apiCollectionVersionUpdate.removedEndpointNames) &&
        Objects.equals(this.disabledEndpointNames, apiCollectionVersionUpdate.disabledEndpointNames) &&
        Objects.equals(this.missingConnections, apiCollectionVersionUpdate.missingConnections) &&
        Objects.equals(this.missingInputs, apiCollectionVersionUpdate.missingInputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiCollection, removedEndpointNames, disabledEndpointNames, missingConnections, missingInputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCollectionVersionUpdateModel {\n");
    sb.append("    apiCollection: ").append(toIndentedString(apiCollection)).append("\n");
    sb.append("    removedEndpointNames: ").append(toIndentedString(removedEndpointNames)).append("\n");
    sb.append("    disabledEndpointNames: ").append(toIndentedString(disabledEndpointNames)).append("\n");
    sb.append("    missingConnections: ").append(toIndentedString(missingConnections)).append("\n");
    sb.append("    missingInputs: ").append(toIndentedString(missingInputs)).append("\n");
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

