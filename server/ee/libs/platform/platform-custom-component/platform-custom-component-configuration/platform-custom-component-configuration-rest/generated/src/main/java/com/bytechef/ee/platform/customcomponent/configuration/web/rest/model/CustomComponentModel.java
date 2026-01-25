package com.bytechef.ee.platform.customcomponent.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * An custom component.
 */

@Schema(name = "CustomComponent", description = "An custom component.")
@JsonTypeName("CustomComponent")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:37:00.476147+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class CustomComponentModel {

  private @Nullable Integer componentVersion;

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable String description;

  private @Nullable Boolean enabled;

  private @Nullable String icon;

  private @Nullable Long id;

  /**
   * The language in which the component is implemented
   */
  public enum LanguageEnum {
    JAVA("JAVA"),
    
    JAVASCRIPT("JAVASCRIPT"),
    
    PYTHON("PYTHON"),
    
    RUBY("RUBY");

    private final String value;

    LanguageEnum(String value) {
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
    public static LanguageEnum fromValue(String value) {
      for (LanguageEnum b : LanguageEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private @Nullable LanguageEnum language;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private String name;

  private @Nullable String title;

  private @Nullable Integer version;

  public CustomComponentModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CustomComponentModel(String name) {
    this.name = name;
  }

  public CustomComponentModel componentVersion(@Nullable Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * The version of a custom component.
   * @return componentVersion
   */
  
  @Schema(name = "componentVersion", description = "The version of a custom component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentVersion")
  public @Nullable Integer getComponentVersion() {
    return componentVersion;
  }

  public void setComponentVersion(@Nullable Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public CustomComponentModel createdBy(@Nullable String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
   */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdBy")
  public @Nullable String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(@Nullable String createdBy) {
    this.createdBy = createdBy;
  }

  public CustomComponentModel createdDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * The created date.
   * @return createdDate
   */
  @Valid 
  @Schema(name = "createdDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The created date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdDate")
  public @Nullable OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public CustomComponentModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of a custom component.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of a custom component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public CustomComponentModel enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If a custom component is enabled or not.
   * @return enabled
   */
  
  @Schema(name = "enabled", description = "If a custom component is enabled or not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public @Nullable Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
  }

  public CustomComponentModel icon(@Nullable String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * The icon of a custom component.
   * @return icon
   */
  
  @Schema(name = "icon", description = "The icon of a custom component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("icon")
  public @Nullable String getIcon() {
    return icon;
  }

  public void setIcon(@Nullable String icon) {
    this.icon = icon;
  }

  public CustomComponentModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an custom component.
   * @return id
   */
  
  @Schema(name = "id", description = "The id of an custom component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public CustomComponentModel language(@Nullable LanguageEnum language) {
    this.language = language;
    return this;
  }

  /**
   * The language in which the component is implemented
   * @return language
   */
  
  @Schema(name = "language", accessMode = Schema.AccessMode.READ_ONLY, description = "The language in which the component is implemented", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("language")
  public @Nullable LanguageEnum getLanguage() {
    return language;
  }

  public void setLanguage(@Nullable LanguageEnum language) {
    this.language = language;
  }

  public CustomComponentModel lastModifiedBy(@Nullable String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * The last modified by.
   * @return lastModifiedBy
   */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedBy")
  public @Nullable String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(@Nullable String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public CustomComponentModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * The last modified date.
   * @return lastModifiedDate
   */
  @Valid 
  @Schema(name = "lastModifiedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedDate")
  public @Nullable OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public CustomComponentModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a custom component.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of a custom component.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public CustomComponentModel title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * The title of a custom component.
   * @return title
   */
  
  @Schema(name = "title", description = "The title of a custom component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public CustomComponentModel version(@Nullable Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
   */
  
  @Schema(name = "__version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("__version")
  public @Nullable Integer getVersion() {
    return version;
  }

  public void setVersion(@Nullable Integer version) {
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
    CustomComponentModel customComponent = (CustomComponentModel) o;
    return Objects.equals(this.componentVersion, customComponent.componentVersion) &&
        Objects.equals(this.createdBy, customComponent.createdBy) &&
        Objects.equals(this.createdDate, customComponent.createdDate) &&
        Objects.equals(this.description, customComponent.description) &&
        Objects.equals(this.enabled, customComponent.enabled) &&
        Objects.equals(this.icon, customComponent.icon) &&
        Objects.equals(this.id, customComponent.id) &&
        Objects.equals(this.language, customComponent.language) &&
        Objects.equals(this.lastModifiedBy, customComponent.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, customComponent.lastModifiedDate) &&
        Objects.equals(this.name, customComponent.name) &&
        Objects.equals(this.title, customComponent.title) &&
        Objects.equals(this.version, customComponent.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentVersion, createdBy, createdDate, description, enabled, icon, id, language, lastModifiedBy, lastModifiedDate, name, title, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CustomComponentModel {\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    language: ").append(toIndentedString(language)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

