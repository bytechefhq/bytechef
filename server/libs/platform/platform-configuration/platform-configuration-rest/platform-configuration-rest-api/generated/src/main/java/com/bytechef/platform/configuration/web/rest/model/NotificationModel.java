package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.NotificationEventModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * A Notification definition.
 */

@Schema(name = "Notification", description = "A Notification definition.")
@JsonTypeName("Notification")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-04-13T22:23:09.531481+02:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
public class NotificationModel {

  private @Nullable Long id;

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private String name;

  /**
   * Type of the notification
   */
  public enum NotificationTypeEnum {
    EMAIL("EMAIL"),
    
    WEBHOOK("WEBHOOK");

    private String value;

    NotificationTypeEnum(String value) {
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
    public static NotificationTypeEnum fromValue(String value) {
      for (NotificationTypeEnum b : NotificationTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private NotificationTypeEnum notificationType;

  @Valid
  private Map<String, String> settings = new HashMap<>();

  @Valid
  private List<@Valid NotificationEventModel> events = new ArrayList<>();

  public NotificationModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NotificationModel(String name, NotificationTypeEnum notificationType, Map<String, String> settings, List<@Valid NotificationEventModel> events) {
    this.name = name;
    this.notificationType = notificationType;
    this.settings = settings;
    this.events = events;
  }

  public NotificationModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * Notification Id
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "Notification Id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public NotificationModel createdBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
   */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdBy")
  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public NotificationModel createdDate(OffsetDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * The created date.
   * @return createdDate
   */
  @Valid 
  @Schema(name = "createdDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The created date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdDate")
  public OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(OffsetDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public NotificationModel lastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * The last modified by.
   * @return lastModifiedBy
   */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedBy")
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public NotificationModel lastModifiedDate(OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * The last modified date.
   * @return lastModifiedDate
   */
  @Valid 
  @Schema(name = "lastModifiedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedDate")
  public OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public NotificationModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Notification name
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "Notification name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public NotificationModel notificationType(NotificationTypeEnum notificationType) {
    this.notificationType = notificationType;
    return this;
  }

  /**
   * Type of the notification
   * @return notificationType
   */
  @NotNull 
  @Schema(name = "notificationType", description = "Type of the notification", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("notificationType")
  public NotificationTypeEnum getNotificationType() {
    return notificationType;
  }

  public void setNotificationType(NotificationTypeEnum notificationType) {
    this.notificationType = notificationType;
  }

  public NotificationModel settings(Map<String, String> settings) {
    this.settings = settings;
    return this;
  }

  public NotificationModel putSettingsItem(String key, String settingsItem) {
    if (this.settings == null) {
      this.settings = new HashMap<>();
    }
    this.settings.put(key, settingsItem);
    return this;
  }

  /**
   * Notification type related settings
   * @return settings
   */
  @NotNull 
  @Schema(name = "settings", description = "Notification type related settings", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("settings")
  public Map<String, String> getSettings() {
    return settings;
  }

  public void setSettings(Map<String, String> settings) {
    this.settings = settings;
  }

  public NotificationModel events(List<@Valid NotificationEventModel> events) {
    this.events = events;
    return this;
  }

  public NotificationModel addEventsItem(NotificationEventModel eventsItem) {
    if (this.events == null) {
      this.events = new ArrayList<>();
    }
    this.events.add(eventsItem);
    return this;
  }

  /**
   * List of events for which notification will be triggered
   * @return events
   */
  @NotNull @Valid 
  @Schema(name = "events", description = "List of events for which notification will be triggered", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("events")
  public List<@Valid NotificationEventModel> getEvents() {
    return events;
  }

  public void setEvents(List<@Valid NotificationEventModel> events) {
    this.events = events;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationModel notification = (NotificationModel) o;
    return Objects.equals(this.id, notification.id) &&
        Objects.equals(this.createdBy, notification.createdBy) &&
        Objects.equals(this.createdDate, notification.createdDate) &&
        Objects.equals(this.lastModifiedBy, notification.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, notification.lastModifiedDate) &&
        Objects.equals(this.name, notification.name) &&
        Objects.equals(this.notificationType, notification.notificationType) &&
        Objects.equals(this.settings, notification.settings) &&
        Objects.equals(this.events, notification.events);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, createdBy, createdDate, lastModifiedBy, lastModifiedDate, name, notificationType, settings, events);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    notificationType: ").append(toIndentedString(notificationType)).append("\n");
    sb.append("    settings: ").append(toIndentedString(settings)).append("\n");
    sb.append("    events: ").append(toIndentedString(events)).append("\n");
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

