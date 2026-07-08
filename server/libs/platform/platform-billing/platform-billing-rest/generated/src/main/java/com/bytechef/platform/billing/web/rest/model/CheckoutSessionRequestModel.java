package com.bytechef.platform.billing.web.rest.model;

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
 * A checkout session request.
 */

@Schema(name = "CheckoutSessionRequest", description = "A checkout session request.")
@JsonTypeName("CheckoutSessionRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-08T14:03:14.706845+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class CheckoutSessionRequestModel {

  /**
   * The plan name.
   */
  public enum PlanNameEnum {
    STARTER("STARTER"),
    
    GROWTH("GROWTH");

    private final String value;

    PlanNameEnum(String value) {
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
    public static PlanNameEnum fromValue(String value) {
      for (PlanNameEnum b : PlanNameEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private PlanNameEnum planName;

  public CheckoutSessionRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CheckoutSessionRequestModel(PlanNameEnum planName) {
    this.planName = planName;
  }

  public CheckoutSessionRequestModel planName(PlanNameEnum planName) {
    this.planName = planName;
    return this;
  }

  /**
   * The plan name.
   * @return planName
   */
  @NotNull 
  @Schema(name = "planName", description = "The plan name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("planName")
  public PlanNameEnum getPlanName() {
    return planName;
  }

  @JsonProperty("planName")
  public void setPlanName(PlanNameEnum planName) {
    this.planName = planName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CheckoutSessionRequestModel checkoutSessionRequest = (CheckoutSessionRequestModel) o;
    return Objects.equals(this.planName, checkoutSessionRequest.planName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(planName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CheckoutSessionRequestModel {\n");
    sb.append("    planName: ").append(toIndentedString(planName)).append("\n");
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

