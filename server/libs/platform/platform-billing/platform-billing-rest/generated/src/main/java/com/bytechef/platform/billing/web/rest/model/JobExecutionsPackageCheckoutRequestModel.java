package com.bytechef.platform.billing.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A job executions package checkout request.
 */

@Schema(name = "JobExecutionsPackageCheckoutRequest", description = "A job executions package checkout request.")
@JsonTypeName("JobExecutionsPackageCheckoutRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-28T13:52:37.197604+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class JobExecutionsPackageCheckoutRequestModel {

  private Integer quantity;

  public JobExecutionsPackageCheckoutRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public JobExecutionsPackageCheckoutRequestModel(Integer quantity) {
    this.quantity = quantity;
  }

  public JobExecutionsPackageCheckoutRequestModel quantity(Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  /**
   * The number of packages to purchase.
   * minimum: 1
   * maximum: 100
   * @return quantity
   */
  @NotNull @Min(value = 1) @Max(value = 100) 
  @Schema(name = "quantity", description = "The number of packages to purchase.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("quantity")
  public Integer getQuantity() {
    return quantity;
  }

  @JsonProperty("quantity")
  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JobExecutionsPackageCheckoutRequestModel jobExecutionsPackageCheckoutRequest = (JobExecutionsPackageCheckoutRequestModel) o;
    return Objects.equals(this.quantity, jobExecutionsPackageCheckoutRequest.quantity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(quantity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobExecutionsPackageCheckoutRequestModel {\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
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

