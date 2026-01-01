package com.bytechef.ee.embedded.unified.web.rest.crm.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
/**
 * Additional metadata about the error.
 */

@Schema(name = "errors_inner_meta", description = "Additional metadata about the error.")
@JsonTypeName("errors_inner_meta")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:57.236842+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class ErrorsInnerMetaModel {

  private @Nullable Object cause;

  /**
   * The origin of the error.
   */
  public enum OriginEnum {
    REMOTE_PROVIDER("remote-provider"),
    
    BYTECHEF("bytechef");

    private final String value;

    OriginEnum(String value) {
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
    public static OriginEnum fromValue(String value) {
      for (OriginEnum b : OriginEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private OriginEnum origin;

  private @Nullable String applicationName;

  public ErrorsInnerMetaModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ErrorsInnerMetaModel(OriginEnum origin) {
    this.origin = origin;
  }

  public ErrorsInnerMetaModel cause(@Nullable Object cause) {
    this.cause = cause;
    return this;
  }

  /**
   * The cause of the error. Usually the underlying error from the remote Provider.
   * @return cause
   */
  
  @Schema(name = "cause", description = "The cause of the error. Usually the underlying error from the remote Provider.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("cause")
  public @Nullable Object getCause() {
    return cause;
  }

  public void setCause(@Nullable Object cause) {
    this.cause = cause;
  }

  public ErrorsInnerMetaModel origin(OriginEnum origin) {
    this.origin = origin;
    return this;
  }

  /**
   * The origin of the error.
   * @return origin
   */
  @NotNull 
  @Schema(name = "origin", example = "remote-provider", description = "The origin of the error.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("origin")
  public OriginEnum getOrigin() {
    return origin;
  }

  public void setOrigin(OriginEnum origin) {
    this.origin = origin;
  }

  public ErrorsInnerMetaModel applicationName(@Nullable String applicationName) {
    this.applicationName = applicationName;
    return this;
  }

  /**
   * The name of the application that generated the error.
   * @return applicationName
   */
  
  @Schema(name = "application_name", example = "MyCompany Production", description = "The name of the application that generated the error.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("application_name")
  public @Nullable String getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(@Nullable String applicationName) {
    this.applicationName = applicationName;
  }
    /**
    * A container for additional, undeclared properties.
    * This is a holder for any undeclared properties as specified with
    * the 'additionalProperties' keyword in the OAS document.
    */
    private Map<String, Object> additionalProperties;

    /**
    * Set the additional (undeclared) property with the specified name and value.
    * If the property does not already exist, create it otherwise replace it.
    */
    @JsonAnySetter
    public ErrorsInnerMetaModel putAdditionalProperty(String key, Object value) {
        if (this.additionalProperties == null) {
            this.additionalProperties = new HashMap<String, Object>();
        }
        this.additionalProperties.put(key, value);
        return this;
    }

    /**
    * Return the additional (undeclared) property.
    */
    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    /**
    * Return the additional (undeclared) property with the specified name.
    */
    public Object getAdditionalProperty(String key) {
        if (this.additionalProperties == null) {
            return null;
        }
        return this.additionalProperties.get(key);
    }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ErrorsInnerMetaModel errorsInnerMeta = (ErrorsInnerMetaModel) o;
    return Objects.equals(this.cause, errorsInnerMeta.cause) &&
        Objects.equals(this.origin, errorsInnerMeta.origin) &&
        Objects.equals(this.applicationName, errorsInnerMeta.applicationName) &&
    Objects.equals(this.additionalProperties, errorsInnerMeta.additionalProperties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cause, origin, applicationName, additionalProperties);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ErrorsInnerMetaModel {\n");
    sb.append("    cause: ").append(toIndentedString(cause)).append("\n");
    sb.append("    origin: ").append(toIndentedString(origin)).append("\n");
    sb.append("    applicationName: ").append(toIndentedString(applicationName)).append("\n");
    
    sb.append("    additionalProperties: ").append(toIndentedString(additionalProperties)).append("\n");
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

