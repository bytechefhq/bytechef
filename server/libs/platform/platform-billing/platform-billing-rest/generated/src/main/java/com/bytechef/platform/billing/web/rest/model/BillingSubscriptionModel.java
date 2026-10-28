package com.bytechef.platform.billing.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A billing subscription.
 */

@Schema(name = "BillingSubscription", description = "A billing subscription.")
@JsonTypeName("BillingSubscription")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-28T13:52:37.197604+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class BillingSubscriptionModel {

  private @Nullable String planName;

  private @Nullable String status;

  private @Nullable Integer productUnitLimit;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime currentPeriodEnd;

  private @Nullable Boolean cancelAtPeriodEnd;

  private @Nullable String scheduledPlanName;

  private @Nullable Integer jobsExecuted;

  /**
   * The subscription model.
   */
  public enum SubscriptionModelEnum {
    METERED("METERED"),
    
    PREPAID("PREPAID");

    private final String value;

    SubscriptionModelEnum(String value) {
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
    public static SubscriptionModelEnum fromValue(String value) {
      for (SubscriptionModelEnum b : SubscriptionModelEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private @Nullable SubscriptionModelEnum subscriptionModel;

  private @Nullable Integer planUnitTotal;

  private @Nullable Integer planUnitUsed;

  private @Nullable Integer extraUnitTotal;

  private @Nullable Integer extraUnitUsed;

  public BillingSubscriptionModel planName(@Nullable String planName) {
    this.planName = planName;
    return this;
  }

  /**
   * The plan name.
   * @return planName
   */
  
  @Schema(name = "planName", description = "The plan name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("planName")
  public @Nullable String getPlanName() {
    return planName;
  }

  @JsonProperty("planName")
  public void setPlanName(@Nullable String planName) {
    this.planName = planName;
  }

  public BillingSubscriptionModel status(@Nullable String status) {
    this.status = status;
    return this;
  }

  /**
   * The subscription status.
   * @return status
   */
  
  @Schema(name = "status", description = "The subscription status.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public @Nullable String getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable String status) {
    this.status = status;
  }

  public BillingSubscriptionModel productUnitLimit(@Nullable Integer productUnitLimit) {
    this.productUnitLimit = productUnitLimit;
    return this;
  }

  /**
   * The number of units that can be used in the current billing period under flat plan.
   * @return productUnitLimit
   */
  
  @Schema(name = "productUnitLimit", description = "The number of units that can be used in the current billing period under flat plan.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("productUnitLimit")
  public @Nullable Integer getProductUnitLimit() {
    return productUnitLimit;
  }

  @JsonProperty("productUnitLimit")
  public void setProductUnitLimit(@Nullable Integer productUnitLimit) {
    this.productUnitLimit = productUnitLimit;
  }

  public BillingSubscriptionModel currentPeriodEnd(@Nullable OffsetDateTime currentPeriodEnd) {
    this.currentPeriodEnd = currentPeriodEnd;
    return this;
  }

  /**
   * The current period end date.
   * @return currentPeriodEnd
   */
  @Valid 
  @Schema(name = "currentPeriodEnd", description = "The current period end date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("currentPeriodEnd")
  public @Nullable OffsetDateTime getCurrentPeriodEnd() {
    return currentPeriodEnd;
  }

  @JsonProperty("currentPeriodEnd")
  public void setCurrentPeriodEnd(@Nullable OffsetDateTime currentPeriodEnd) {
    this.currentPeriodEnd = currentPeriodEnd;
  }

  public BillingSubscriptionModel cancelAtPeriodEnd(@Nullable Boolean cancelAtPeriodEnd) {
    this.cancelAtPeriodEnd = cancelAtPeriodEnd;
    return this;
  }

  /**
   * Whether the subscription cancels at period end.
   * @return cancelAtPeriodEnd
   */
  
  @Schema(name = "cancelAtPeriodEnd", description = "Whether the subscription cancels at period end.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("cancelAtPeriodEnd")
  public @Nullable Boolean getCancelAtPeriodEnd() {
    return cancelAtPeriodEnd;
  }

  @JsonProperty("cancelAtPeriodEnd")
  public void setCancelAtPeriodEnd(@Nullable Boolean cancelAtPeriodEnd) {
    this.cancelAtPeriodEnd = cancelAtPeriodEnd;
  }

  public BillingSubscriptionModel scheduledPlanName(@Nullable String scheduledPlanName) {
    this.scheduledPlanName = scheduledPlanName;
    return this;
  }

  /**
   * The plan name scheduled to take effect at the next billing period (set when a downgrade is pending).
   * @return scheduledPlanName
   */
  
  @Schema(name = "scheduledPlanName", description = "The plan name scheduled to take effect at the next billing period (set when a downgrade is pending).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("scheduledPlanName")
  public @Nullable String getScheduledPlanName() {
    return scheduledPlanName;
  }

  @JsonProperty("scheduledPlanName")
  public void setScheduledPlanName(@Nullable String scheduledPlanName) {
    this.scheduledPlanName = scheduledPlanName;
  }

  public BillingSubscriptionModel jobsExecuted(@Nullable Integer jobsExecuted) {
    this.jobsExecuted = jobsExecuted;
    return this;
  }

  /**
   * Number of job executions used in the current billing period.
   * @return jobsExecuted
   */
  
  @Schema(name = "jobsExecuted", description = "Number of job executions used in the current billing period.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("jobsExecuted")
  public @Nullable Integer getJobsExecuted() {
    return jobsExecuted;
  }

  @JsonProperty("jobsExecuted")
  public void setJobsExecuted(@Nullable Integer jobsExecuted) {
    this.jobsExecuted = jobsExecuted;
  }

  public BillingSubscriptionModel subscriptionModel(@Nullable SubscriptionModelEnum subscriptionModel) {
    this.subscriptionModel = subscriptionModel;
    return this;
  }

  /**
   * The subscription model.
   * @return subscriptionModel
   */
  
  @Schema(name = "subscriptionModel", description = "The subscription model.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("subscriptionModel")
  public @Nullable SubscriptionModelEnum getSubscriptionModel() {
    return subscriptionModel;
  }

  @JsonProperty("subscriptionModel")
  public void setSubscriptionModel(@Nullable SubscriptionModelEnum subscriptionModel) {
    this.subscriptionModel = subscriptionModel;
  }

  public BillingSubscriptionModel planUnitTotal(@Nullable Integer planUnitTotal) {
    this.planUnitTotal = planUnitTotal;
    return this;
  }

  /**
   * The number of plan units granted for the current billing period (PREPAID).
   * @return planUnitTotal
   */
  
  @Schema(name = "planUnitTotal", description = "The number of plan units granted for the current billing period (PREPAID).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("planUnitTotal")
  public @Nullable Integer getPlanUnitTotal() {
    return planUnitTotal;
  }

  @JsonProperty("planUnitTotal")
  public void setPlanUnitTotal(@Nullable Integer planUnitTotal) {
    this.planUnitTotal = planUnitTotal;
  }

  public BillingSubscriptionModel planUnitUsed(@Nullable Integer planUnitUsed) {
    this.planUnitUsed = planUnitUsed;
    return this;
  }

  /**
   * The number of plan units consumed in the current billing period (PREPAID).
   * @return planUnitUsed
   */
  
  @Schema(name = "planUnitUsed", description = "The number of plan units consumed in the current billing period (PREPAID).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("planUnitUsed")
  public @Nullable Integer getPlanUnitUsed() {
    return planUnitUsed;
  }

  @JsonProperty("planUnitUsed")
  public void setPlanUnitUsed(@Nullable Integer planUnitUsed) {
    this.planUnitUsed = planUnitUsed;
  }

  public BillingSubscriptionModel extraUnitTotal(@Nullable Integer extraUnitTotal) {
    this.extraUnitTotal = extraUnitTotal;
    return this;
  }

  /**
   * The number of purchased extra units (PREPAID).
   * @return extraUnitTotal
   */
  
  @Schema(name = "extraUnitTotal", description = "The number of purchased extra units (PREPAID).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("extraUnitTotal")
  public @Nullable Integer getExtraUnitTotal() {
    return extraUnitTotal;
  }

  @JsonProperty("extraUnitTotal")
  public void setExtraUnitTotal(@Nullable Integer extraUnitTotal) {
    this.extraUnitTotal = extraUnitTotal;
  }

  public BillingSubscriptionModel extraUnitUsed(@Nullable Integer extraUnitUsed) {
    this.extraUnitUsed = extraUnitUsed;
    return this;
  }

  /**
   * The number of purchased extra units consumed (PREPAID).
   * @return extraUnitUsed
   */
  
  @Schema(name = "extraUnitUsed", description = "The number of purchased extra units consumed (PREPAID).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("extraUnitUsed")
  public @Nullable Integer getExtraUnitUsed() {
    return extraUnitUsed;
  }

  @JsonProperty("extraUnitUsed")
  public void setExtraUnitUsed(@Nullable Integer extraUnitUsed) {
    this.extraUnitUsed = extraUnitUsed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BillingSubscriptionModel billingSubscription = (BillingSubscriptionModel) o;
    return Objects.equals(this.planName, billingSubscription.planName) &&
        Objects.equals(this.status, billingSubscription.status) &&
        Objects.equals(this.productUnitLimit, billingSubscription.productUnitLimit) &&
        Objects.equals(this.currentPeriodEnd, billingSubscription.currentPeriodEnd) &&
        Objects.equals(this.cancelAtPeriodEnd, billingSubscription.cancelAtPeriodEnd) &&
        Objects.equals(this.scheduledPlanName, billingSubscription.scheduledPlanName) &&
        Objects.equals(this.jobsExecuted, billingSubscription.jobsExecuted) &&
        Objects.equals(this.subscriptionModel, billingSubscription.subscriptionModel) &&
        Objects.equals(this.planUnitTotal, billingSubscription.planUnitTotal) &&
        Objects.equals(this.planUnitUsed, billingSubscription.planUnitUsed) &&
        Objects.equals(this.extraUnitTotal, billingSubscription.extraUnitTotal) &&
        Objects.equals(this.extraUnitUsed, billingSubscription.extraUnitUsed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(planName, status, productUnitLimit, currentPeriodEnd, cancelAtPeriodEnd, scheduledPlanName, jobsExecuted, subscriptionModel, planUnitTotal, planUnitUsed, extraUnitTotal, extraUnitUsed);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BillingSubscriptionModel {\n");
    sb.append("    planName: ").append(toIndentedString(planName)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    productUnitLimit: ").append(toIndentedString(productUnitLimit)).append("\n");
    sb.append("    currentPeriodEnd: ").append(toIndentedString(currentPeriodEnd)).append("\n");
    sb.append("    cancelAtPeriodEnd: ").append(toIndentedString(cancelAtPeriodEnd)).append("\n");
    sb.append("    scheduledPlanName: ").append(toIndentedString(scheduledPlanName)).append("\n");
    sb.append("    jobsExecuted: ").append(toIndentedString(jobsExecuted)).append("\n");
    sb.append("    subscriptionModel: ").append(toIndentedString(subscriptionModel)).append("\n");
    sb.append("    planUnitTotal: ").append(toIndentedString(planUnitTotal)).append("\n");
    sb.append("    planUnitUsed: ").append(toIndentedString(planUnitUsed)).append("\n");
    sb.append("    extraUnitTotal: ").append(toIndentedString(extraUnitTotal)).append("\n");
    sb.append("    extraUnitUsed: ").append(toIndentedString(extraUnitUsed)).append("\n");
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

