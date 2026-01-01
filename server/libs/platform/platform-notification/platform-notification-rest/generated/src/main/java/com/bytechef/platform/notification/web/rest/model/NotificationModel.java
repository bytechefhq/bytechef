package com.bytechef.platform.notification.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.notification.web.rest.model.NotificationEventModel;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:56.357983+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
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
  public enum TypeEnum {
    EMAIL("EMAIL"),
    
    WEBHOOK("WEBHOOK");

    private final String value;

    TypeEnum(String value) {
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
    public static TypeEnum fromValue(String value) {
      for (TypeEnum b : TypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private TypeEnum type;

  @Valid
  private Map<String, Object> settings = new HashMap<>();

  @Valid
  private List<@Valid NotificationEventModel> notificationEvents = new ArrayList<>();

  @Valid
  private List<Long> notificationEventIds = new ArrayList<>();

  private @Nullable Integer version;

  public NotificationModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NotificationModel(String name, TypeEnum type, Map<String, Object> settings) {
    this.name = name;
    this.type = type;
    this.settings = settings;
  }

  public NotificationModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * Notification Id
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "Notification Id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public NotificationModel createdBy(@Nullable String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
   */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdBy")
  public @Nullable String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(@Nullable String createdBy) {
    this.createdBy = createdBy;
  }

  public NotificationModel createdDate(@Nullable OffsetDateTime createdDate) {
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
  public @Nullable OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public NotificationModel lastModifiedBy(@Nullable String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * The last modified by.
   * @return lastModifiedBy
   */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedBy")
  public @Nullable String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(@Nullable String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public NotificationModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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
  public @Nullable OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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

  public NotificationModel type(TypeEnum type) {
    this.type = type;
    return this;
  }

  /**
   * Type of the notification
   * @return type
   */
  @NotNull 
  @Schema(name = "type", description = "Type of the notification", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public TypeEnum getType() {
    return type;
  }

  public void setType(TypeEnum type) {
    this.type = type;
  }

  public NotificationModel settings(Map<String, Object> settings) {
    this.settings = settings;
    return this;
  }

  public NotificationModel putSettingsItem(String key, Object settingsItem) {
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
  public Map<String, Object> getSettings() {
    return settings;
  }

  public void setSettings(Map<String, Object> settings) {
    this.settings = settings;
  }

  public NotificationModel notificationEvents(List<@Valid NotificationEventModel> notificationEvents) {
    this.notificationEvents = notificationEvents;
    return this;
  }

  public NotificationModel addNotificationEventsItem(NotificationEventModel notificationEventsItem) {
    if (this.notificationEvents == null) {
      this.notificationEvents = new ArrayList<>();
    }
    this.notificationEvents.add(notificationEventsItem);
    return this;
  }

  /**
   * List of events for which notification will be triggered
   * @return notificationEvents
   */
  @Valid 
  @Schema(name = "notificationEvents", accessMode = Schema.AccessMode.READ_ONLY, description = "List of events for which notification will be triggered", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("notificationEvents")
  public List<@Valid NotificationEventModel> getNotificationEvents() {
    return notificationEvents;
  }

  public void setNotificationEvents(List<@Valid NotificationEventModel> notificationEvents) {
    this.notificationEvents = notificationEvents;
  }

  public NotificationModel notificationEventIds(List<Long> notificationEventIds) {
    this.notificationEventIds = notificationEventIds;
    return this;
  }

  public NotificationModel addNotificationEventIdsItem(Long notificationEventIdsItem) {
    if (this.notificationEventIds == null) {
      this.notificationEventIds = new ArrayList<>();
    }
    this.notificationEventIds.add(notificationEventIdsItem);
    return this;
  }

  /**
   * List of event ids for which notification will be triggered
   * @return notificationEventIds
   */
  
  @Schema(name = "notificationEventIds", description = "List of event ids for which notification will be triggered", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("notificationEventIds")
  public List<Long> getNotificationEventIds() {
    return notificationEventIds;
  }

  public void setNotificationEventIds(List<Long> notificationEventIds) {
    this.notificationEventIds = notificationEventIds;
  }

  public NotificationModel version(@Nullable Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
   */
  
  @Schema(name = "__version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("__version")
  public @Nullable Integer getVersion() {
    return version;
  }

  public void setVersion(@Nullable Integer version) {
    this.version = version;
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
        Objects.equals(this.type, notification.type) &&
        Objects.equals(this.settings, notification.settings) &&
        Objects.equals(this.notificationEvents, notification.notificationEvents) &&
        Objects.equals(this.notificationEventIds, notification.notificationEventIds) &&
        Objects.equals(this.version, notification.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, createdBy, createdDate, lastModifiedBy, lastModifiedDate, name, type, settings, notificationEvents, notificationEventIds, version);
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
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    settings: ").append(toIndentedString(settings)).append("\n");
    sb.append("    notificationEvents: ").append(toIndentedString(notificationEvents)).append("\n");
    sb.append("    notificationEventIds: ").append(toIndentedString(notificationEventIds)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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

