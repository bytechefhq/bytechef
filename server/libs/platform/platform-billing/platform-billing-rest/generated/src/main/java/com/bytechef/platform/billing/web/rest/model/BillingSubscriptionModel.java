package com.bytechef.platform.billing.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-08T14:03:14.706845+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class BillingSubscriptionModel {

  private @Nullable String planName;

  private @Nullable String status;

  private @Nullable Integer taskLimit;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime currentPeriodEnd;

  private @Nullable Boolean cancelAtPeriodEnd;

  private @Nullable String scheduledPlanName;

  private @Nullable Integer tasksUsed;

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

  public BillingSubscriptionModel taskLimit(@Nullable Integer taskLimit) {
    this.taskLimit = taskLimit;
    return this;
  }

  /**
   * The task limit.
   * @return taskLimit
   */
  
  @Schema(name = "taskLimit", description = "The task limit.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("taskLimit")
  public @Nullable Integer getTaskLimit() {
    return taskLimit;
  }

  @JsonProperty("taskLimit")
  public void setTaskLimit(@Nullable Integer taskLimit) {
    this.taskLimit = taskLimit;
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

  public BillingSubscriptionModel tasksUsed(@Nullable Integer tasksUsed) {
    this.tasksUsed = tasksUsed;
    return this;
  }

  /**
   * Number of task executions used in the current billing period.
   * @return tasksUsed
   */
  
  @Schema(name = "tasksUsed", description = "Number of task executions used in the current billing period.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("tasksUsed")
  public @Nullable Integer getTasksUsed() {
    return tasksUsed;
  }

  @JsonProperty("tasksUsed")
  public void setTasksUsed(@Nullable Integer tasksUsed) {
    this.tasksUsed = tasksUsed;
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
        Objects.equals(this.taskLimit, billingSubscription.taskLimit) &&
        Objects.equals(this.currentPeriodEnd, billingSubscription.currentPeriodEnd) &&
        Objects.equals(this.cancelAtPeriodEnd, billingSubscription.cancelAtPeriodEnd) &&
        Objects.equals(this.scheduledPlanName, billingSubscription.scheduledPlanName) &&
        Objects.equals(this.tasksUsed, billingSubscription.tasksUsed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(planName, status, taskLimit, currentPeriodEnd, cancelAtPeriodEnd, scheduledPlanName, tasksUsed);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BillingSubscriptionModel {\n");
    sb.append("    planName: ").append(toIndentedString(planName)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    taskLimit: ").append(toIndentedString(taskLimit)).append("\n");
    sb.append("    currentPeriodEnd: ").append(toIndentedString(currentPeriodEnd)).append("\n");
    sb.append("    cancelAtPeriodEnd: ").append(toIndentedString(cancelAtPeriodEnd)).append("\n");
    sb.append("    scheduledPlanName: ").append(toIndentedString(scheduledPlanName)).append("\n");
    sb.append("    tasksUsed: ").append(toIndentedString(tasksUsed)).append("\n");
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

