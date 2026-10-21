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
 * ProxyResponseModel
 */

@JsonTypeName("ProxyResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:31.849354+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ProxyResponseModel {

  private Integer status;

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

  private BodyEncodingEnum bodyEncoding;

  public ProxyResponseModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProxyResponseModel(Integer status, Map<String, List<String>> headers, BodyEncodingEnum bodyEncoding) {
    this.status = status;
    this.headers = headers;
    this.bodyEncoding = bodyEncoding;
  }

  public ProxyResponseModel status(Integer status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull 
  @Schema(name = "status", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public Integer getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(Integer status) {
    this.status = status;
  }

  public ProxyResponseModel headers(Map<String, List<String>> headers) {
    this.headers = headers;
    return this;
  }

  public ProxyResponseModel putHeadersItem(String key, List<String> headersItem) {
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
  @NotNull 
  @Schema(name = "headers", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("headers")
  public Map<String, List<String>> getHeaders() {
    return headers;
  }

  @JsonProperty("headers")
  public void setHeaders(Map<String, List<String>> headers) {
    this.headers = headers;
  }

  public ProxyResponseModel body(String body) {
    this.body = JsonNullable.of(body);
    return this;
  }

  /**
   * Get body
   * @return body
   */
  
  @Schema(name = "body", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("body")
  public JsonNullable<String> getBody() {
    return body;
  }

  public void setBody(JsonNullable<String> body) {
    this.body = body;
  }

  public ProxyResponseModel bodyEncoding(BodyEncodingEnum bodyEncoding) {
    this.bodyEncoding = bodyEncoding;
    return this;
  }

  /**
   * Get bodyEncoding
   * @return bodyEncoding
   */
  @NotNull 
  @Schema(name = "bodyEncoding", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("bodyEncoding")
  public BodyEncodingEnum getBodyEncoding() {
    return bodyEncoding;
  }

  @JsonProperty("bodyEncoding")
  public void setBodyEncoding(BodyEncodingEnum bodyEncoding) {
    this.bodyEncoding = bodyEncoding;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProxyResponseModel proxyResponse = (ProxyResponseModel) o;
    return Objects.equals(this.status, proxyResponse.status) &&
        Objects.equals(this.headers, proxyResponse.headers) &&
        equalsNullable(this.body, proxyResponse.body) &&
        Objects.equals(this.bodyEncoding, proxyResponse.bodyEncoding);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, headers, hashCodeNullable(body), bodyEncoding);
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
    sb.append("class ProxyResponseModel {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    headers: ").append(toIndentedString(headers)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    bodyEncoding: ").append(toIndentedString(bodyEncoding)).append("\n");
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

