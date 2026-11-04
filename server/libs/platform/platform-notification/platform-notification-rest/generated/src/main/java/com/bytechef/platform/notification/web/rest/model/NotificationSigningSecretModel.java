package com.bytechef.platform.notification.web.rest.model;

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
 * A regenerated webhook signing secret.
 */

@Schema(name = "NotificationSigningSecret", description = "A regenerated webhook signing secret.")
@JsonTypeName("NotificationSigningSecret")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-04T19:06:18.157671+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class NotificationSigningSecretModel {

  private @Nullable String signingSecret;

  public NotificationSigningSecretModel signingSecret(@Nullable String signingSecret) {
    this.signingSecret = signingSecret;
    return this;
  }

  /**
   * The signing secret
   * @return signingSecret
   */
  
  @Schema(name = "signingSecret", description = "The signing secret", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("signingSecret")
  public @Nullable String getSigningSecret() {
    return signingSecret;
  }

  @JsonProperty("signingSecret")
  public void setSigningSecret(@Nullable String signingSecret) {
    this.signingSecret = signingSecret;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationSigningSecretModel notificationSigningSecret = (NotificationSigningSecretModel) o;
    return Objects.equals(this.signingSecret, notificationSigningSecret.signingSecret);
  }

  @Override
  public int hashCode() {
    return Objects.hash(signingSecret);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationSigningSecretModel {\n");
    sb.append("    signingSecret: ").append(toIndentedString(signingSecret)).append("\n");
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

