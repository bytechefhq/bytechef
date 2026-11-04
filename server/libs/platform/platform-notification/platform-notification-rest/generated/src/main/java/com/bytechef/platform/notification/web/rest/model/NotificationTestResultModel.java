package com.bytechef.platform.notification.web.rest.model;

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

/**
 * The outcome of a test send of a notification.
 */

@Schema(name = "NotificationTestResult", description = "The outcome of a test send of a notification.")
@JsonTypeName("NotificationTestResult")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-04T19:56:22.769511+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class NotificationTestResultModel {

  private Boolean success;

  private @Nullable Integer statusCode;

  private @Nullable String responseBody;

  private @Nullable String error;

  private Boolean signed;

  /**
   * Why a webhook test message was sent without a signature: NO_SECRET when the notification has no signing secret yet, URL_CHANGED when the URL differs from the saved one
   */
  public enum UnsignedReasonEnum {
    NO_SECRET("NO_SECRET"),
    
    URL_CHANGED("URL_CHANGED");

    private final String value;

    UnsignedReasonEnum(String value) {
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
    public static UnsignedReasonEnum fromValue(String value) {
      for (UnsignedReasonEnum b : UnsignedReasonEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private @Nullable UnsignedReasonEnum unsignedReason;

  public NotificationTestResultModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NotificationTestResultModel(Boolean success, Boolean signed) {
    this.success = success;
    this.signed = signed;
  }

  public NotificationTestResultModel success(Boolean success) {
    this.success = success;
    return this;
  }

  /**
   * Whether the destination accepted the test message
   * @return success
   */
  @NotNull 
  @Schema(name = "success", description = "Whether the destination accepted the test message", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("success")
  public Boolean getSuccess() {
    return success;
  }

  @JsonProperty("success")
  public void setSuccess(Boolean success) {
    this.success = success;
  }

  public NotificationTestResultModel statusCode(@Nullable Integer statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  /**
   * The HTTP status returned by the destination, when there was a response
   * @return statusCode
   */
  
  @Schema(name = "statusCode", description = "The HTTP status returned by the destination, when there was a response", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("statusCode")
  public @Nullable Integer getStatusCode() {
    return statusCode;
  }

  @JsonProperty("statusCode")
  public void setStatusCode(@Nullable Integer statusCode) {
    this.statusCode = statusCode;
  }

  public NotificationTestResultModel responseBody(@Nullable String responseBody) {
    this.responseBody = responseBody;
    return this;
  }

  /**
   * The response body of the destination, capped at 1000 characters
   * @return responseBody
   */
  
  @Schema(name = "responseBody", description = "The response body of the destination, capped at 1000 characters", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("responseBody")
  public @Nullable String getResponseBody() {
    return responseBody;
  }

  @JsonProperty("responseBody")
  public void setResponseBody(@Nullable String responseBody) {
    this.responseBody = responseBody;
  }

  public NotificationTestResultModel error(@Nullable String error) {
    this.error = error;
    return this;
  }

  /**
   * The reason the test failed
   * @return error
   */
  
  @Schema(name = "error", description = "The reason the test failed", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("error")
  public @Nullable String getError() {
    return error;
  }

  @JsonProperty("error")
  public void setError(@Nullable String error) {
    this.error = error;
  }

  public NotificationTestResultModel signed(Boolean signed) {
    this.signed = signed;
    return this;
  }

  /**
   * Whether the test message was signed
   * @return signed
   */
  @NotNull 
  @Schema(name = "signed", description = "Whether the test message was signed", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("signed")
  public Boolean getSigned() {
    return signed;
  }

  @JsonProperty("signed")
  public void setSigned(Boolean signed) {
    this.signed = signed;
  }

  public NotificationTestResultModel unsignedReason(@Nullable UnsignedReasonEnum unsignedReason) {
    this.unsignedReason = unsignedReason;
    return this;
  }

  /**
   * Why a webhook test message was sent without a signature: NO_SECRET when the notification has no signing secret yet, URL_CHANGED when the URL differs from the saved one
   * @return unsignedReason
   */
  
  @Schema(name = "unsignedReason", description = "Why a webhook test message was sent without a signature: NO_SECRET when the notification has no signing secret yet, URL_CHANGED when the URL differs from the saved one", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("unsignedReason")
  public @Nullable UnsignedReasonEnum getUnsignedReason() {
    return unsignedReason;
  }

  @JsonProperty("unsignedReason")
  public void setUnsignedReason(@Nullable UnsignedReasonEnum unsignedReason) {
    this.unsignedReason = unsignedReason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationTestResultModel notificationTestResult = (NotificationTestResultModel) o;
    return Objects.equals(this.success, notificationTestResult.success) &&
        Objects.equals(this.statusCode, notificationTestResult.statusCode) &&
        Objects.equals(this.responseBody, notificationTestResult.responseBody) &&
        Objects.equals(this.error, notificationTestResult.error) &&
        Objects.equals(this.signed, notificationTestResult.signed) &&
        Objects.equals(this.unsignedReason, notificationTestResult.unsignedReason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(success, statusCode, responseBody, error, signed, unsignedReason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationTestResultModel {\n");
    sb.append("    success: ").append(toIndentedString(success)).append("\n");
    sb.append("    statusCode: ").append(toIndentedString(statusCode)).append("\n");
    sb.append("    responseBody: ").append(toIndentedString(responseBody)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    signed: ").append(toIndentedString(signed)).append("\n");
    sb.append("    unsignedReason: ").append(toIndentedString(unsignedReason)).append("\n");
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

