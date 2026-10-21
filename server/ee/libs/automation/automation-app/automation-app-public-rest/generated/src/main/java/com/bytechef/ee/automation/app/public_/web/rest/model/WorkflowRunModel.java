package com.bytechef.ee.automation.app.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.app.public_.web.rest.model.WorkflowRunErrorModel;
import com.bytechef.ee.automation.app.public_.web.rest.model.WorkflowRunStatusModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
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
 * WorkflowRunModel
 */

@JsonTypeName("WorkflowRun")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:31.849354+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class WorkflowRunModel {

  private String runId;

  private String workflowKey;

  private WorkflowRunStatusModel status;

  private JsonNullable<Object> result = JsonNullable.<Object>undefined();

  private JsonNullable<WorkflowRunErrorModel> error = JsonNullable.<WorkflowRunErrorModel>undefined();

  private JsonNullable<String> approvalFormUrl = JsonNullable.<String>undefined();

  public WorkflowRunModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkflowRunModel(String runId, String workflowKey, WorkflowRunStatusModel status) {
    this.runId = runId;
    this.workflowKey = workflowKey;
    this.status = status;
  }

  public WorkflowRunModel runId(String runId) {
    this.runId = runId;
    return this;
  }

  /**
   * Get runId
   * @return runId
   */
  @NotNull 
  @Schema(name = "runId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("runId")
  public String getRunId() {
    return runId;
  }

  @JsonProperty("runId")
  public void setRunId(String runId) {
    this.runId = runId;
  }

  public WorkflowRunModel workflowKey(String workflowKey) {
    this.workflowKey = workflowKey;
    return this;
  }

  /**
   * Get workflowKey
   * @return workflowKey
   */
  @NotNull 
  @Schema(name = "workflowKey", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflowKey")
  public String getWorkflowKey() {
    return workflowKey;
  }

  @JsonProperty("workflowKey")
  public void setWorkflowKey(String workflowKey) {
    this.workflowKey = workflowKey;
  }

  public WorkflowRunModel status(WorkflowRunStatusModel status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @Schema(name = "status", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public WorkflowRunStatusModel getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(WorkflowRunStatusModel status) {
    this.status = status;
  }

  public WorkflowRunModel result(Object result) {
    this.result = JsonNullable.of(result);
    return this;
  }

  /**
   * The run's result on COMPLETED.
   * @return result
   */
  
  @Schema(name = "result", description = "The run's result on COMPLETED.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("result")
  public JsonNullable<Object> getResult() {
    return result;
  }

  public void setResult(JsonNullable<Object> result) {
    this.result = result;
  }

  public WorkflowRunModel error(WorkflowRunErrorModel error) {
    this.error = JsonNullable.of(error);
    return this;
  }

  /**
   * Get error
   * @return error
   */
  @Valid 
  @Schema(name = "error", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("error")
  public JsonNullable<WorkflowRunErrorModel> getError() {
    return error;
  }

  public void setError(JsonNullable<WorkflowRunErrorModel> error) {
    this.error = error;
  }

  public WorkflowRunModel approvalFormUrl(String approvalFormUrl) {
    this.approvalFormUrl = JsonNullable.of(approvalFormUrl);
    return this;
  }

  /**
   * The hosted approval form on APPROVAL_REQUIRED, when the server has a public URL.
   * @return approvalFormUrl
   */
  
  @Schema(name = "approvalFormUrl", description = "The hosted approval form on APPROVAL_REQUIRED, when the server has a public URL.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("approvalFormUrl")
  public JsonNullable<String> getApprovalFormUrl() {
    return approvalFormUrl;
  }

  public void setApprovalFormUrl(JsonNullable<String> approvalFormUrl) {
    this.approvalFormUrl = approvalFormUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkflowRunModel workflowRun = (WorkflowRunModel) o;
    return Objects.equals(this.runId, workflowRun.runId) &&
        Objects.equals(this.workflowKey, workflowRun.workflowKey) &&
        Objects.equals(this.status, workflowRun.status) &&
        equalsNullable(this.result, workflowRun.result) &&
        equalsNullable(this.error, workflowRun.error) &&
        equalsNullable(this.approvalFormUrl, workflowRun.approvalFormUrl);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(runId, workflowKey, status, hashCodeNullable(result), hashCodeNullable(error), hashCodeNullable(approvalFormUrl));
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
    sb.append("class WorkflowRunModel {\n");
    sb.append("    runId: ").append(toIndentedString(runId)).append("\n");
    sb.append("    workflowKey: ").append(toIndentedString(workflowKey)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    result: ").append(toIndentedString(result)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    approvalFormUrl: ").append(toIndentedString(approvalFormUrl)).append("\n");
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

