package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppStatusModel;
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
 * The new status of an app.
 */

@Schema(name = "AppStatusUpdate", description = "The new status of an app.")
@JsonTypeName("AppStatusUpdate")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T10:32:24.407292+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AppStatusUpdateModel {

  private AppStatusModel status;

  public AppStatusUpdateModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppStatusUpdateModel(AppStatusModel status) {
    this.status = status;
  }

  public AppStatusUpdateModel status(AppStatusModel status) {
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
  public AppStatusModel getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(AppStatusModel status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppStatusUpdateModel appStatusUpdate = (AppStatusUpdateModel) o;
    return Objects.equals(this.status, appStatusUpdate.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppStatusUpdateModel {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

