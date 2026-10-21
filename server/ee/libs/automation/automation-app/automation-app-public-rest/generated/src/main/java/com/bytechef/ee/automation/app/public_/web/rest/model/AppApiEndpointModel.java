package com.bytechef.ee.automation.app.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
 * An endpoint of an API collection bound to the calling app, called at /api/v{collectionVersion}/{contextPath}/{path}.
 */

@Schema(name = "AppApiEndpoint", description = "An endpoint of an API collection bound to the calling app, called at /api/v{collectionVersion}/{contextPath}/{path}.")
@JsonTypeName("AppApiEndpoint")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:31.849354+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppApiEndpointModel {

  private String name;

  /**
   * Gets or Sets httpMethod
   */
  public enum HttpMethodEnum {
    DELETE("DELETE"),
    
    GET("GET"),
    
    PATCH("PATCH"),
    
    POST("POST"),
    
    PUT("PUT");

    private final String value;

    HttpMethodEnum(String value) {
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
    public static HttpMethodEnum fromValue(String value) {
      for (HttpMethodEnum b : HttpMethodEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private HttpMethodEnum httpMethod;

  private JsonNullable<String> path = JsonNullable.<String>undefined();

  private JsonNullable<Map<String, Object>> requestSchema = JsonNullable.<Map<String, Object>>undefined();

  private JsonNullable<Map<String, Object>> responseSchema = JsonNullable.<Map<String, Object>>undefined();

  public AppApiEndpointModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppApiEndpointModel(String name, HttpMethodEnum httpMethod) {
    this.name = name;
    this.httpMethod = httpMethod;
  }

  public AppApiEndpointModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public AppApiEndpointModel httpMethod(HttpMethodEnum httpMethod) {
    this.httpMethod = httpMethod;
    return this;
  }

  /**
   * Get httpMethod
   * @return httpMethod
   */
  @NotNull 
  @Schema(name = "httpMethod", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("httpMethod")
  public HttpMethodEnum getHttpMethod() {
    return httpMethod;
  }

  @JsonProperty("httpMethod")
  public void setHttpMethod(HttpMethodEnum httpMethod) {
    this.httpMethod = httpMethod;
  }

  public AppApiEndpointModel path(String path) {
    this.path = JsonNullable.of(path);
    return this;
  }

  /**
   * The endpoint path, relative to the collection's /v{collectionVersion}/{contextPath}.
   * @return path
   */
  
  @Schema(name = "path", description = "The endpoint path, relative to the collection's /v{collectionVersion}/{contextPath}.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("path")
  public JsonNullable<String> getPath() {
    return path;
  }

  public void setPath(JsonNullable<String> path) {
    this.path = path;
  }

  public AppApiEndpointModel requestSchema(Map<String, Object> requestSchema) {
    this.requestSchema = JsonNullable.of(requestSchema);
    return this;
  }

  public AppApiEndpointModel putRequestSchemaItem(String key, Object requestSchemaItem) {
    if (this.requestSchema == null || !this.requestSchema.isPresent() || this.requestSchema.get() == null) {
      this.requestSchema = JsonNullable.of(new HashMap<>());
    }
    this.requestSchema.get().put(key, requestSchemaItem);
    return this;
  }

  /**
   * The JSON Schemas of the request (headers, parameters, body) declared by the endpoint workflow's API request trigger; null when they cannot be read.
   * @return requestSchema
   */
  
  @Schema(name = "requestSchema", description = "The JSON Schemas of the request (headers, parameters, body) declared by the endpoint workflow's API request trigger; null when they cannot be read.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("requestSchema")
  public JsonNullable<Map<String, Object>> getRequestSchema() {
    return requestSchema;
  }

  public void setRequestSchema(JsonNullable<Map<String, Object>> requestSchema) {
    this.requestSchema = requestSchema;
  }

  public AppApiEndpointModel responseSchema(Map<String, Object> responseSchema) {
    this.responseSchema = JsonNullable.of(responseSchema);
    return this;
  }

  public AppApiEndpointModel putResponseSchemaItem(String key, Object responseSchemaItem) {
    if (this.responseSchema == null || !this.responseSchema.isPresent() || this.responseSchema.get() == null) {
      this.responseSchema = JsonNullable.of(new HashMap<>());
    }
    this.responseSchema.get().put(key, responseSchemaItem);
    return this;
  }

  /**
   * The JSON Schemas of the responses (success, invalidInput, internalError, forbidden) declared by the endpoint workflow's API request trigger; null when they cannot be read.
   * @return responseSchema
   */
  
  @Schema(name = "responseSchema", description = "The JSON Schemas of the responses (success, invalidInput, internalError, forbidden) declared by the endpoint workflow's API request trigger; null when they cannot be read.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("responseSchema")
  public JsonNullable<Map<String, Object>> getResponseSchema() {
    return responseSchema;
  }

  public void setResponseSchema(JsonNullable<Map<String, Object>> responseSchema) {
    this.responseSchema = responseSchema;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppApiEndpointModel appApiEndpoint = (AppApiEndpointModel) o;
    return Objects.equals(this.name, appApiEndpoint.name) &&
        Objects.equals(this.httpMethod, appApiEndpoint.httpMethod) &&
        equalsNullable(this.path, appApiEndpoint.path) &&
        equalsNullable(this.requestSchema, appApiEndpoint.requestSchema) &&
        equalsNullable(this.responseSchema, appApiEndpoint.responseSchema);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, httpMethod, hashCodeNullable(path), hashCodeNullable(requestSchema), hashCodeNullable(responseSchema));
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
    sb.append("class AppApiEndpointModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    httpMethod: ").append(toIndentedString(httpMethod)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    requestSchema: ").append(toIndentedString(requestSchema)).append("\n");
    sb.append("    responseSchema: ").append(toIndentedString(responseSchema)).append("\n");
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

