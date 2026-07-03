/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import com.bytechef.ee.ai.hub.util.EnumOrdinals;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Scheduled run definition for an AI Hub personal agent. One agent can own many schedules. Spring Data JDBC row-mapper;
 * setters intentionally permissive for the construct-then-save flow. Ownership and validation invariants live in
 * {@code AiHubPersonalAgentScheduleServiceImpl}, not on this type.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_personal_agent_schedule")
public class AiHubPersonalAgentSchedule {

    @Id
    private Long id;

    @Column("ai_hub_personal_agent_id")
    private long aiHubPersonalAgentId;

    @Column("workspace_id")
    private long workspaceId;

    @Column("user_id")
    private long userId;

    @Column("environment")
    private int environment;

    @Column("title")
    private String title;

    @Column("prompt")
    private String prompt;

    @Column("frequency_kind")
    private int frequencyKind;

    @Column("interval_minutes")
    private @Nullable Integer intervalMinutes;

    @Column("minute_of_hour")
    private @Nullable Integer minuteOfHour;

    @Column("time_of_day")
    private @Nullable LocalTime timeOfDay;

    @Column("day_of_week")
    private @Nullable Integer dayOfWeek;

    @Column("day_of_month")
    private @Nullable Integer dayOfMonth;

    @Column("cron_expression")
    private @Nullable String cronExpression;

    @Column("effective_cron_expression")
    private String effectiveCronExpression;

    @Column("zone_id")
    private String zoneId;

    @Column("start_date")
    private @Nullable LocalDateTime startDate;

    @Column("lifecycle_kind")
    private int lifecycleKind;

    @Column("max_runs")
    private @Nullable Integer maxRuns;

    @Column("remaining_runs")
    private @Nullable Integer remainingRuns;

    @Column("consecutive_failures")
    private int consecutiveFailures;

    @Column("enabled")
    private boolean enabled = true;

    @Column("last_run_at")
    private @Nullable LocalDateTime lastRunAt;

    @Column("next_run_at")
    private @Nullable LocalDateTime nextRunAt;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private LocalDateTime createdDate;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Version
    private int version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getAiHubPersonalAgentId() {
        return aiHubPersonalAgentId;
    }

    public void setAiHubPersonalAgentId(long aiHubPersonalAgentId) {
        this.aiHubPersonalAgentId = aiHubPersonalAgentId;
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Environment getEnvironment() {
        Environment[] environments = Environment.values();

        if (environment < 0 || environment >= environments.length) {
            throw new IllegalStateException("Invalid environment ordinal: " + environment);
        }

        return environments[environment];
    }

    public void setEnvironment(Environment environment) {
        if (environment != null) {
            this.environment = environment.ordinal();
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public ScheduleFrequencyKind getFrequencyKind() {
        return EnumOrdinals.fromOrdinal(frequencyKind, ScheduleFrequencyKind.class);
    }

    public void setFrequencyKind(ScheduleFrequencyKind frequencyKind) {
        if (frequencyKind != null) {
            this.frequencyKind = frequencyKind.ordinal();
        }
    }

    public @Nullable Integer getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(@Nullable Integer intervalMinutes) {
        this.intervalMinutes = intervalMinutes;
    }

    public @Nullable Integer getMinuteOfHour() {
        return minuteOfHour;
    }

    public void setMinuteOfHour(@Nullable Integer minuteOfHour) {
        this.minuteOfHour = minuteOfHour;
    }

    public @Nullable LocalTime getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(@Nullable LocalTime timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public @Nullable Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(@Nullable Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public @Nullable Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(@Nullable Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public @Nullable String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(@Nullable String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getEffectiveCronExpression() {
        return effectiveCronExpression;
    }

    public void setEffectiveCronExpression(String effectiveCronExpression) {
        this.effectiveCronExpression = effectiveCronExpression;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public @Nullable LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(@Nullable LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public ScheduleLifecycleKind getLifecycleKind() {
        return EnumOrdinals.fromOrdinal(lifecycleKind, ScheduleLifecycleKind.class);
    }

    public void setLifecycleKind(ScheduleLifecycleKind lifecycleKind) {
        if (lifecycleKind != null) {
            this.lifecycleKind = lifecycleKind.ordinal();
        }
    }

    public @Nullable Integer getMaxRuns() {
        return maxRuns;
    }

    public void setMaxRuns(@Nullable Integer maxRuns) {
        this.maxRuns = maxRuns;
    }

    public @Nullable Integer getRemainingRuns() {
        return remainingRuns;
    }

    public void setRemainingRuns(@Nullable Integer remainingRuns) {
        this.remainingRuns = remainingRuns;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void setConsecutiveFailures(int consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public @Nullable LocalDateTime getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(@Nullable LocalDateTime lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public @Nullable LocalDateTime getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(@Nullable LocalDateTime nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        AiHubPersonalAgentSchedule that = (AiHubPersonalAgentSchedule) other;

        if (id == null) {
            return this == that;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
