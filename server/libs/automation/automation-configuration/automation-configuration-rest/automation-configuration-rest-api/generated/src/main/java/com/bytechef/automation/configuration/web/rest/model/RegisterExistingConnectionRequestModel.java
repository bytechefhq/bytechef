package com.bytechef.automation.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.automation.configuration.web.rest.model.TagModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * Contains all required information to register a connection backed by an externally-provisioned credential.
 */

@Schema(name = "RegisterExistingConnectionRequest", description = "Contains all required information to register a connection backed by an externally-provisioned credential.")
@JsonTypeName("RegisterExistingConnectionRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-03T17:34:59.729452+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class RegisterExistingConnectionRequestModel {

  private String componentName;

  private Integer connectionVersion;

  private String credentialRef;

  /**
   * Must be a non-DATABASE store. DATABASE values are rejected.
   */
  public enum CredentialStoreTypeEnum {
    AWS_SECRETS_MANAGER("AWS_SECRETS_MANAGER"),
    
    HASHICORP_VAULT("HASHICORP_VAULT");

    private final String value;

    CredentialStoreTypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static CredentialStoreTypeEnum fromValue(String value) {
      for (CredentialStoreTypeEnum b : CredentialStoreTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private CredentialStoreTypeEnum credentialStoreType;

  private Long environmentId;

  private String name;

  @Valid
  private List<@Valid TagModel> tags = new ArrayList<>();

  private Long workspaceId;

  public RegisterExistingConnectionRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RegisterExistingConnectionRequestModel(String componentName, Integer connectionVersion, String credentialRef, CredentialStoreTypeEnum credentialStoreType, Long environmentId, String name, Long workspaceId) {
    this.componentName = componentName;
    this.connectionVersion = connectionVersion;
    this.credentialRef = credentialRef;
    this.credentialStoreType = credentialStoreType;
    this.environmentId = environmentId;
    this.name = name;
    this.workspaceId = workspaceId;
  }

  public RegisterExistingConnectionRequestModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of a component that uses this connection.
   * @return componentName
   */
  @NotNull 
  @Schema(name = "componentName", description = "The name of a component that uses this connection.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public RegisterExistingConnectionRequestModel connectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
    return this;
  }

  /**
   * The version of a component that uses this connection.
   * @return connectionVersion
   */
  @NotNull 
  @Schema(name = "connectionVersion", description = "The version of a component that uses this connection.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectionVersion")
  public Integer getConnectionVersion() {
    return connectionVersion;
  }

  @JsonProperty("connectionVersion")
  public void setConnectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
  }

  public RegisterExistingConnectionRequestModel credentialRef(String credentialRef) {
    this.credentialRef = credentialRef;
    return this;
  }

  /**
   * External-store path or UUID where the secret already lives.
   * @return credentialRef
   */
  @NotNull 
  @Schema(name = "credentialRef", description = "External-store path or UUID where the secret already lives.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("credentialRef")
  public String getCredentialRef() {
    return credentialRef;
  }

  @JsonProperty("credentialRef")
  public void setCredentialRef(String credentialRef) {
    this.credentialRef = credentialRef;
  }

  public RegisterExistingConnectionRequestModel credentialStoreType(CredentialStoreTypeEnum credentialStoreType) {
    this.credentialStoreType = credentialStoreType;
    return this;
  }

  /**
   * Must be a non-DATABASE store. DATABASE values are rejected.
   * @return credentialStoreType
   */
  @NotNull 
  @Schema(name = "credentialStoreType", description = "Must be a non-DATABASE store. DATABASE values are rejected.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("credentialStoreType")
  public CredentialStoreTypeEnum getCredentialStoreType() {
    return credentialStoreType;
  }

  @JsonProperty("credentialStoreType")
  public void setCredentialStoreType(CredentialStoreTypeEnum credentialStoreType) {
    this.credentialStoreType = credentialStoreType;
  }

  public RegisterExistingConnectionRequestModel environmentId(Long environmentId) {
    this.environmentId = environmentId;
    return this;
  }

  /**
   * The id of an environment.
   * @return environmentId
   */
  @NotNull 
  @Schema(name = "environmentId", description = "The id of an environment.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("environmentId")
  public Long getEnvironmentId() {
    return environmentId;
  }

  @JsonProperty("environmentId")
  public void setEnvironmentId(Long environmentId) {
    this.environmentId = environmentId;
  }

  public RegisterExistingConnectionRequestModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a connection.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of a connection.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public RegisterExistingConnectionRequestModel tags(List<@Valid TagModel> tags) {
    this.tags = tags;
    return this;
  }

  public RegisterExistingConnectionRequestModel addTagsItem(TagModel tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Get tags
   * @return tags
   */
  @Valid 
  @Schema(name = "tags", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("tags")
  public List<@Valid TagModel> getTags() {
    return tags;
  }

  @JsonProperty("tags")
  public void setTags(List<@Valid TagModel> tags) {
    this.tags = tags;
  }

  public RegisterExistingConnectionRequestModel workspaceId(Long workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

  /**
   * The id of a workspace.
   * @return workspaceId
   */
  @NotNull 
  @Schema(name = "workspaceId", description = "The id of a workspace.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workspaceId")
  public Long getWorkspaceId() {
    return workspaceId;
  }

  @JsonProperty("workspaceId")
  public void setWorkspaceId(Long workspaceId) {
    this.workspaceId = workspaceId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RegisterExistingConnectionRequestModel registerExistingConnectionRequest = (RegisterExistingConnectionRequestModel) o;
    return Objects.equals(this.componentName, registerExistingConnectionRequest.componentName) &&
        Objects.equals(this.connectionVersion, registerExistingConnectionRequest.connectionVersion) &&
        Objects.equals(this.credentialRef, registerExistingConnectionRequest.credentialRef) &&
        Objects.equals(this.credentialStoreType, registerExistingConnectionRequest.credentialStoreType) &&
        Objects.equals(this.environmentId, registerExistingConnectionRequest.environmentId) &&
        Objects.equals(this.name, registerExistingConnectionRequest.name) &&
        Objects.equals(this.tags, registerExistingConnectionRequest.tags) &&
        Objects.equals(this.workspaceId, registerExistingConnectionRequest.workspaceId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, connectionVersion, credentialRef, credentialStoreType, environmentId, name, tags, workspaceId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RegisterExistingConnectionRequestModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    connectionVersion: ").append(toIndentedString(connectionVersion)).append("\n");
    sb.append("    credentialRef: ").append(toIndentedString(credentialRef)).append("\n");
    sb.append("    credentialStoreType: ").append(toIndentedString(credentialStoreType)).append("\n");
    sb.append("    environmentId: ").append(toIndentedString(environmentId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    workspaceId: ").append(toIndentedString(workspaceId)).append("\n");
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

