package com.bytechef.ee.automation.app.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
 * ProxyRequestModel
 */

@JsonTypeName("ProxyRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:31.849354+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ProxyRequestModel {

  /**
   * Gets or Sets method
   */
  public enum MethodEnum {
    GET("GET"),
    
    POST("POST"),
    
    PUT("PUT"),
    
    PATCH("PATCH"),
    
    DELETE("DELETE"),
    
    HEAD("HEAD");

    private final String value;

    MethodEnum(String value) {
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
    public static MethodEnum fromValue(String value) {
      for (MethodEnum b : MethodEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private MethodEnum method;

  private String path;

  private Map<String, List<String>> query = new HashMap<>();

  private Map<String, List<String>> headers = new HashMap<>();

  private JsonNullable<String> body = JsonNullable.<String>undefined();

  /**
   * Gets or Sets bodyEncoding
   */
  public enum BodyEncodingEnum {
    UTF8("UTF8"),
    
    BASE64("BASE64");

    private final String value;

    BodyEncodingEnum(String value) {
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
    public static BodyEncodingEnum fromValue(String value) {
      for (BodyEncodingEnum b : BodyEncodingEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private BodyEncodingEnum bodyEncoding = BodyEncodingEnum.UTF8;

  private @Nullable String contentType;

  public ProxyRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProxyRequestModel(MethodEnum method, String path) {
    this.method = method;
    this.path = path;
  }

  public ProxyRequestModel method(MethodEnum method) {
    this.method = method;
    return this;
  }

  /**
   * Get method
   * @return method
   */
  @NotNull 
  @Schema(name = "method", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("method")
  public MethodEnum getMethod() {
    return method;
  }

  @JsonProperty("method")
  public void setMethod(MethodEnum method) {
    this.method = method;
  }

  public ProxyRequestModel path(String path) {
    this.path = path;
    return this;
  }

  /**
   * Relative to the connection's base URI; must start with a single '/'.
   * @return path
   */
  @NotNull 
  @Schema(name = "path", description = "Relative to the connection's base URI; must start with a single '/'.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("path")
  public String getPath() {
    return path;
  }

  @JsonProperty("path")
  public void setPath(String path) {
    this.path = path;
  }

  public ProxyRequestModel query(Map<String, List<String>> query) {
    this.query = query;
    return this;
  }

  public ProxyRequestModel putQueryItem(String key, List<String> queryItem) {
    if (this.query == null) {
      this.query = new HashMap<>();
    }
    this.query.put(key, queryItem);
    return this;
  }

  /**
   * Get query
   * @return query
   */
  
  @Schema(name = "query", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("query")
  public Map<String, List<String>> getQuery() {
    return query;
  }

  @JsonProperty("query")
  public void setQuery(Map<String, List<String>> query) {
    this.query = query;
  }

  public ProxyRequestModel headers(Map<String, List<String>> headers) {
    this.headers = headers;
    return this;
  }

  public ProxyRequestModel putHeadersItem(String key, List<String> headersItem) {
    if (this.headers == null) {
      this.headers = new HashMap<>();
    }
    this.headers.put(key, headersItem);
    return this;
  }

  /**
   * Get headers
   * @return headers
   */
  
  @Schema(name = "headers", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("headers")
  public Map<String, List<String>> getHeaders() {
    return headers;
  }

  @JsonProperty("headers")
  public void setHeaders(Map<String, List<String>> headers) {
    this.headers = headers;
  }

  public ProxyRequestModel body(String body) {
    this.body = JsonNullable.of(body);
    return this;
  }

  /**
   * UTF-8 text, or base64 when bodyEncoding is BASE64.
   * @return body
   */
  
  @Schema(name = "body", description = "UTF-8 text, or base64 when bodyEncoding is BASE64.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("body")
  public JsonNullable<String> getBody() {
    return body;
  }

  public void setBody(JsonNullable<String> body) {
    this.body = body;
  }

  public ProxyRequestModel bodyEncoding(BodyEncodingEnum bodyEncoding) {
    this.bodyEncoding = bodyEncoding;
    return this;
  }

  /**
   * Get bodyEncoding
   * @return bodyEncoding
   */
  
  @Schema(name = "bodyEncoding", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("bodyEncoding")
  public BodyEncodingEnum getBodyEncoding() {
    return bodyEncoding;
  }

  @JsonProperty("bodyEncoding")
  public void setBodyEncoding(BodyEncodingEnum bodyEncoding) {
    this.bodyEncoding = bodyEncoding;
  }

  public ProxyRequestModel contentType(@Nullable String contentType) {
    this.contentType = contentType;
    return this;
  }

  /**
   * Get contentType
   * @return contentType
   */
  
  @Schema(name = "contentType", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("contentType")
  public @Nullable String getContentType() {
    return contentType;
  }

  @JsonProperty("contentType")
  public void setContentType(@Nullable String contentType) {
    this.contentType = contentType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProxyRequestModel proxyRequest = (ProxyRequestModel) o;
    return Objects.equals(this.method, proxyRequest.method) &&
        Objects.equals(this.path, proxyRequest.path) &&
        Objects.equals(this.query, proxyRequest.query) &&
        Objects.equals(this.headers, proxyRequest.headers) &&
        equalsNullable(this.body, proxyRequest.body) &&
        Objects.equals(this.bodyEncoding, proxyRequest.bodyEncoding) &&
        Objects.equals(this.contentType, proxyRequest.contentType);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, path, query, headers, hashCodeNullable(body), bodyEncoding, contentType);
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
    sb.append("class ProxyRequestModel {\n");
    sb.append("    method: ").append(toIndentedString(method)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    query: ").append(toIndentedString(query)).append("\n");
    sb.append("    headers: ").append(toIndentedString(headers)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    bodyEncoding: ").append(toIndentedString(bodyEncoding)).append("\n");
    sb.append("    contentType: ").append(toIndentedString(contentType)).append("\n");
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

