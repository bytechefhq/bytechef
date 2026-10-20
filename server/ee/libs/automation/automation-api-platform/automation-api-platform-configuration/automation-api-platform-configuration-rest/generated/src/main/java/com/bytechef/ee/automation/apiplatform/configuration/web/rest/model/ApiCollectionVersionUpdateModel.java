package com.bytechef.ee.automation.apiplatform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.ApiCollectionMissingConnectionModel;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.ApiCollectionMissingInputModel;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.ApiCollectionModel;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T01:38:17.244806+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ApiCollectionVersionUpdateModel {

  private @Nullable ApiCollectionModel apiCollection;

  private List<String> disabledEndpointNames = new ArrayList<>();

  private List<@Valid ApiCollectionMissingConnectionModel> missingConnections = new ArrayList<>();

  private List<@Valid ApiCollectionMissingInputModel> missingInputs = new ArrayList<>();

  private List<String> removedEndpointNames = new ArrayList<>();

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

  public ApiCollectionVersionUpdateModel disabledEndpointNames(List<String> disabledEndpointNames) {
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
   * The names of the endpoints carried disabled because the new version needs a connection or input they do not have.
   * @return disabledEndpointNames
   */
  
  @Schema(name = "disabledEndpointNames", description = "The names of the endpoints carried disabled because the new version needs a connection or input they do not have.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("disabledEndpointNames")
  public List<String> getDisabledEndpointNames() {
    return disabledEndpointNames;
  }

  @JsonProperty("disabledEndpointNames")
  public void setDisabledEndpointNames(List<String> disabledEndpointNames) {
    this.disabledEndpointNames = disabledEndpointNames;
  }

  public ApiCollectionVersionUpdateModel missingConnections(List<@Valid ApiCollectionMissingConnectionModel> missingConnections) {
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
  public List<@Valid ApiCollectionMissingConnectionModel> getMissingConnections() {
    return missingConnections;
  }

  @JsonProperty("missingConnections")
  public void setMissingConnections(List<@Valid ApiCollectionMissingConnectionModel> missingConnections) {
    this.missingConnections = missingConnections;
  }

  public ApiCollectionVersionUpdateModel missingInputs(List<@Valid ApiCollectionMissingInputModel> missingInputs) {
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
  public List<@Valid ApiCollectionMissingInputModel> getMissingInputs() {
    return missingInputs;
  }

  @JsonProperty("missingInputs")
  public void setMissingInputs(List<@Valid ApiCollectionMissingInputModel> missingInputs) {
    this.missingInputs = missingInputs;
  }

  public ApiCollectionVersionUpdateModel removedEndpointNames(List<String> removedEndpointNames) {
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
   * The names of the endpoints removed because their workflow is absent from the new version.
   * @return removedEndpointNames
   */
  
  @Schema(name = "removedEndpointNames", description = "The names of the endpoints removed because their workflow is absent from the new version.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("removedEndpointNames")
  public List<String> getRemovedEndpointNames() {
    return removedEndpointNames;
  }

  @JsonProperty("removedEndpointNames")
  public void setRemovedEndpointNames(List<String> removedEndpointNames) {
    this.removedEndpointNames = removedEndpointNames;
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
        Objects.equals(this.disabledEndpointNames, apiCollectionVersionUpdate.disabledEndpointNames) &&
        Objects.equals(this.missingConnections, apiCollectionVersionUpdate.missingConnections) &&
        Objects.equals(this.missingInputs, apiCollectionVersionUpdate.missingInputs) &&
        Objects.equals(this.removedEndpointNames, apiCollectionVersionUpdate.removedEndpointNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiCollection, disabledEndpointNames, missingConnections, missingInputs, removedEndpointNames);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCollectionVersionUpdateModel {\n");
    sb.append("    apiCollection: ").append(toIndentedString(apiCollection)).append("\n");
    sb.append("    disabledEndpointNames: ").append(toIndentedString(disabledEndpointNames)).append("\n");
    sb.append("    missingConnections: ").append(toIndentedString(missingConnections)).append("\n");
    sb.append("    missingInputs: ").append(toIndentedString(missingInputs)).append("\n");
    sb.append("    removedEndpointNames: ").append(toIndentedString(removedEndpointNames)).append("\n");
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

