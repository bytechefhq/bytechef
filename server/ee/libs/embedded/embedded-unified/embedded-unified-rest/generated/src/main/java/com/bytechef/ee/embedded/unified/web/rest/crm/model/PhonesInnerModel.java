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
 * PhonesInnerModel
 */

@JsonTypeName("phones_inner")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:57.236842+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class PhonesInnerModel {

  private JsonNullable<String> phoneNumber = JsonNullable.<String>undefined();

  /**
   * Gets or Sets phoneNumberType
   */
  public enum PhoneNumberTypeEnum {
    PRIMARY("PRIMARY"),
    
    MOBILE("MOBILE"),
    
    FAX("FAX"),
    
    OTHER("OTHER");

    private final String value;

    PhoneNumberTypeEnum(String value) {
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
    public static PhoneNumberTypeEnum fromValue(String value) {
      for (PhoneNumberTypeEnum b : PhoneNumberTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private PhoneNumberTypeEnum phoneNumberType;

  public PhonesInnerModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PhonesInnerModel(String phoneNumber, PhoneNumberTypeEnum phoneNumberType) {
    this.phoneNumber = JsonNullable.of(phoneNumber);
    this.phoneNumberType = phoneNumberType;
  }

  public PhonesInnerModel phoneNumber(String phoneNumber) {
    this.phoneNumber = JsonNullable.of(phoneNumber);
    return this;
  }

  /**
   * Get phoneNumber
   * @return phoneNumber
   */
  @NotNull 
  @Schema(name = "phoneNumber", example = "+14151234567", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("phoneNumber")
  public JsonNullable<String> getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(JsonNullable<String> phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public PhonesInnerModel phoneNumberType(PhoneNumberTypeEnum phoneNumberType) {
    this.phoneNumberType = phoneNumberType;
    return this;
  }

  /**
   * Get phoneNumberType
   * @return phoneNumberType
   */
  @NotNull 
  @Schema(name = "phoneNumberType", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("phoneNumberType")
  public PhoneNumberTypeEnum getPhoneNumberType() {
    return phoneNumberType;
  }

  public void setPhoneNumberType(PhoneNumberTypeEnum phoneNumberType) {
    this.phoneNumberType = phoneNumberType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PhonesInnerModel phonesInner = (PhonesInnerModel) o;
    return Objects.equals(this.phoneNumber, phonesInner.phoneNumber) &&
        Objects.equals(this.phoneNumberType, phonesInner.phoneNumberType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(phoneNumber, phoneNumberType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PhonesInnerModel {\n");
    sb.append("    phoneNumber: ").append(toIndentedString(phoneNumber)).append("\n");
    sb.append("    phoneNumberType: ").append(toIndentedString(phoneNumberType)).append("\n");
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

