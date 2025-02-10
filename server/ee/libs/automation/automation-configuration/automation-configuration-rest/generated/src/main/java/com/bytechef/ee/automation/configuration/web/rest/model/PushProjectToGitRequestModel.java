package com.bytechef.ee.automation.configuration.web.rest.model;

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
 * PushProjectToGitRequestModel
 */

@JsonTypeName("pushProjectToGit_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-02-09T08:09:18.185792+01:00[Europe/Zagreb]", comments = "Generator version: 7.11.0")
public class PushProjectToGitRequestModel {

  private @Nullable String commitMessage;

  public PushProjectToGitRequestModel commitMessage(String commitMessage) {
    this.commitMessage = commitMessage;
    return this;
  }

  /**
   * Get commitMessage
   * @return commitMessage
   */
  
  @Schema(name = "commitMessage", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("commitMessage")
  public String getCommitMessage() {
    return commitMessage;
  }

  public void setCommitMessage(String commitMessage) {
    this.commitMessage = commitMessage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PushProjectToGitRequestModel pushProjectToGitRequest = (PushProjectToGitRequestModel) o;
    return Objects.equals(this.commitMessage, pushProjectToGitRequest.commitMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(commitMessage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PushProjectToGitRequestModel {\n");
    sb.append("    commitMessage: ").append(toIndentedString(commitMessage)).append("\n");
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

