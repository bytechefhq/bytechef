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
 * AddressesInnerModel
 */

@JsonTypeName("addresses_inner")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:37:03.860012+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class AddressesInnerModel {

  /**
   * Gets or Sets addressType
   */
  public enum AddressTypeEnum {
    PRIMARY("primary"),
    
    MAILING("mailing"),
    
    OTHER("other"),
    
    BILLING("billing"),
    
    SHIPPING("shipping");

    private final String value;

    AddressTypeEnum(String value) {
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
    public static AddressTypeEnum fromValue(String value) {
      for (AddressTypeEnum b : AddressTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private AddressTypeEnum addressType;

  private JsonNullable<String> city = JsonNullable.<String>undefined();

  private JsonNullable<String> country = JsonNullable.<String>undefined();

  private JsonNullable<String> postalCode = JsonNullable.<String>undefined();

  private JsonNullable<String> state = JsonNullable.<String>undefined();

  private JsonNullable<String> street1 = JsonNullable.<String>undefined();

  private JsonNullable<String> street2 = JsonNullable.<String>undefined();

  public AddressesInnerModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AddressesInnerModel(AddressTypeEnum addressType, String city, String country, String postalCode, String state, String street1, String street2) {
    this.addressType = addressType;
    this.city = JsonNullable.of(city);
    this.country = JsonNullable.of(country);
    this.postalCode = JsonNullable.of(postalCode);
    this.state = JsonNullable.of(state);
    this.street1 = JsonNullable.of(street1);
    this.street2 = JsonNullable.of(street2);
  }

  public AddressesInnerModel addressType(AddressTypeEnum addressType) {
    this.addressType = addressType;
    return this;
  }

  /**
   * Get addressType
   * @return addressType
   */
  @NotNull 
  @Schema(name = "addressType", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("addressType")
  public AddressTypeEnum getAddressType() {
    return addressType;
  }

  public void setAddressType(AddressTypeEnum addressType) {
    this.addressType = addressType;
  }

  public AddressesInnerModel city(String city) {
    this.city = JsonNullable.of(city);
    return this;
  }

  /**
   * Get city
   * @return city
   */
  @NotNull 
  @Schema(name = "city", example = "San Francisco", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("city")
  public JsonNullable<String> getCity() {
    return city;
  }

  public void setCity(JsonNullable<String> city) {
    this.city = city;
  }

  public AddressesInnerModel country(String country) {
    this.country = JsonNullable.of(country);
    return this;
  }

  /**
   * Get country
   * @return country
   */
  @NotNull 
  @Schema(name = "country", example = "USA", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("country")
  public JsonNullable<String> getCountry() {
    return country;
  }

  public void setCountry(JsonNullable<String> country) {
    this.country = country;
  }

  public AddressesInnerModel postalCode(String postalCode) {
    this.postalCode = JsonNullable.of(postalCode);
    return this;
  }

  /**
   * Get postalCode
   * @return postalCode
   */
  @NotNull 
  @Schema(name = "postalCode", example = "94107", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("postalCode")
  public JsonNullable<String> getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(JsonNullable<String> postalCode) {
    this.postalCode = postalCode;
  }

  public AddressesInnerModel state(String state) {
    this.state = JsonNullable.of(state);
    return this;
  }

  /**
   * Get state
   * @return state
   */
  @NotNull 
  @Schema(name = "state", example = "CA", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("state")
  public JsonNullable<String> getState() {
    return state;
  }

  public void setState(JsonNullable<String> state) {
    this.state = state;
  }

  public AddressesInnerModel street1(String street1) {
    this.street1 = JsonNullable.of(street1);
    return this;
  }

  /**
   * Get street1
   * @return street1
   */
  @NotNull 
  @Schema(name = "street1", example = "525 Brannan", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("street1")
  public JsonNullable<String> getStreet1() {
    return street1;
  }

  public void setStreet1(JsonNullable<String> street1) {
    this.street1 = street1;
  }

  public AddressesInnerModel street2(String street2) {
    this.street2 = JsonNullable.of(street2);
    return this;
  }

  /**
   * Get street2
   * @return street2
   */
  @NotNull 
  @Schema(name = "street2", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("street2")
  public JsonNullable<String> getStreet2() {
    return street2;
  }

  public void setStreet2(JsonNullable<String> street2) {
    this.street2 = street2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AddressesInnerModel addressesInner = (AddressesInnerModel) o;
    return Objects.equals(this.addressType, addressesInner.addressType) &&
        Objects.equals(this.city, addressesInner.city) &&
        Objects.equals(this.country, addressesInner.country) &&
        Objects.equals(this.postalCode, addressesInner.postalCode) &&
        Objects.equals(this.state, addressesInner.state) &&
        Objects.equals(this.street1, addressesInner.street1) &&
        Objects.equals(this.street2, addressesInner.street2);
  }

  @Override
  public int hashCode() {
    return Objects.hash(addressType, city, country, postalCode, state, street1, street2);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AddressesInnerModel {\n");
    sb.append("    addressType: ").append(toIndentedString(addressType)).append("\n");
    sb.append("    city: ").append(toIndentedString(city)).append("\n");
    sb.append("    country: ").append(toIndentedString(country)).append("\n");
    sb.append("    postalCode: ").append(toIndentedString(postalCode)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    street1: ").append(toIndentedString(street1)).append("\n");
    sb.append("    street2: ").append(toIndentedString(street2)).append("\n");
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

