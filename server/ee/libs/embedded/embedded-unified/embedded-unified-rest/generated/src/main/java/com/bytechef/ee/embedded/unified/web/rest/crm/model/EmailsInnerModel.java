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

/**
 * EmailsInnerModel
 */

@JsonTypeName("emails_inner")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:57.236842+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class EmailsInnerModel {

  private String emailAddress;

  /**
   * Gets or Sets emailAddressType
   */
  public enum EmailAddressTypeEnum {
    PRIMARY("primary"),
    
    WORK("work"),
    
    OTHER("other");

    private final String value;

    EmailAddressTypeEnum(String value) {
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
    public static EmailAddressTypeEnum fromValue(String value) {
      for (EmailAddressTypeEnum b : EmailAddressTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private EmailAddressTypeEnum emailAddressType;

  public EmailsInnerModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EmailsInnerModel(String emailAddress, EmailAddressTypeEnum emailAddressType) {
    this.emailAddress = emailAddress;
    this.emailAddressType = emailAddressType;
  }

  public EmailsInnerModel emailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
    return this;
  }

  /**
   * Get emailAddress
   * @return emailAddress
   */
  @NotNull 
  @Schema(name = "emailAddress", example = "hello@bytechef.io", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("emailAddress")
  public String getEmailAddress() {
    return emailAddress;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  public EmailsInnerModel emailAddressType(EmailAddressTypeEnum emailAddressType) {
    this.emailAddressType = emailAddressType;
    return this;
  }

  /**
   * Get emailAddressType
   * @return emailAddressType
   */
  @NotNull 
  @Schema(name = "emailAddressType", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("emailAddressType")
  public EmailAddressTypeEnum getEmailAddressType() {
    return emailAddressType;
  }

  public void setEmailAddressType(EmailAddressTypeEnum emailAddressType) {
    this.emailAddressType = emailAddressType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmailsInnerModel emailsInner = (EmailsInnerModel) o;
    return Objects.equals(this.emailAddress, emailsInner.emailAddress) &&
        Objects.equals(this.emailAddressType, emailsInner.emailAddressType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(emailAddress, emailAddressType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EmailsInnerModel {\n");
    sb.append("    emailAddress: ").append(toIndentedString(emailAddress)).append("\n");
    sb.append("    emailAddressType: ").append(toIndentedString(emailAddressType)).append("\n");
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

