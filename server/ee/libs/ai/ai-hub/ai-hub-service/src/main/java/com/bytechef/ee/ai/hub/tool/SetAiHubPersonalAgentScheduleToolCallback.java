/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgent;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentSchedule;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentScheduleService;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentScheduleService.ScheduleInput;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import com.bytechef.ee.ai.hub.personalagent.ScheduleFrequencyKind;
import com.bytechef.ee.ai.hub.personalagent.ScheduleLifecycleKind;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that sets, replaces, or clears the recurring schedule of a Personal Agent. Delegates
 * to {@link AiHubPersonalAgentScheduleService#upsertOrDelete} — the same seam the GraphQL mutation uses — so the 1:1
 * agent/schedule invariant, next-run computation, and Quartz wiring behave identically regardless of surface.
 *
 * <p>
 * Same security posture as {@link CreateAiHubPersonalAgentToolCallback}: workspace + user identity comes ONLY from the
 * trusted {@link ToolContext}, and the target agent is resolved through
 * {@link AiHubPersonalAgentService#findOwned(long, long, long)} so an id from another workspace or user is rejected.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SetAiHubPersonalAgentScheduleToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "setAiHubPersonalAgentSchedule";

    private static final String DESCRIPTION = """
        Set, replace, or clear the recurring schedule of a Personal Agent (each agent has at most one
        schedule). On each fire the agent runs 'prompt' in a fresh conversation. To schedule: pass
        agentId, title, prompt, frequencyKind and the cadence field that matches it —
        EVERY_X_MINUTES needs intervalMinutes; HOURLY needs minuteOfHour (0-59); DAILY needs timeOfDay
        (HH:mm); WEEKLY needs dayOfWeek (1=Monday..7=Sunday) + timeOfDay; MONTHLY needs dayOfMonth
        (1-31) + timeOfDay; CUSTOM_CRON needs cronExpression (Quartz format). zoneId defaults to UTC.
        Optionally cap total runs with maxRuns. To remove an existing schedule pass {agentId, clear:
        true}. Get agent ids from listAiHubPersonalAgents first.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "agentId": {"type": "number", "description": "Id of the personal agent, from listAiHubPersonalAgents"},
                    "clear": {"type": "boolean", "description": "Pass true to delete the agent's existing schedule; all other fields except agentId are then ignored"},
                    "title": {"type": "string", "description": "Short display title for the scheduled job"},
                    "prompt": {"type": "string", "description": "The instruction the agent executes on every fire"},
                    "frequencyKind": {"type": "string", "enum": ["EVERY_X_MINUTES", "HOURLY", "DAILY", "WEEKLY", "MONTHLY", "CUSTOM_CRON"], "description": "Cadence kind"},
                    "intervalMinutes": {"type": "number", "description": "EVERY_X_MINUTES: minutes between runs"},
                    "minuteOfHour": {"type": "number", "description": "HOURLY: minute of each hour (0-59)"},
                    "timeOfDay": {"type": "string", "description": "DAILY/WEEKLY/MONTHLY: time of day as HH:mm (24h)"},
                    "dayOfWeek": {"type": "number", "description": "WEEKLY: 1=Monday .. 7=Sunday"},
                    "dayOfMonth": {"type": "number", "description": "MONTHLY: day of month (1-31)"},
                    "cronExpression": {"type": "string", "description": "CUSTOM_CRON: Quartz cron expression"},
                    "zoneId": {"type": "string", "description": "IANA time zone id, e.g. Europe/Zagreb; defaults to UTC"},
                    "startDate": {"type": "string", "description": "Optional ISO-8601 local date-time before which the schedule never fires, e.g. 2026-08-01T09:00"},
                    "maxRuns": {"type": "number", "description": "Optional total-run cap; the schedule auto-disables after this many fires"}
                },
                "required": ["agentId"]
            }""";

    private static final String DEFAULT_ZONE_ID = "UTC";

    private final AiHubPersonalAgentService aiHubPersonalAgentService;
    private final AiHubPersonalAgentScheduleService aiHubPersonalAgentScheduleService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SetAiHubPersonalAgentScheduleToolCallback(
        AiHubPersonalAgentService aiHubPersonalAgentService,
        AiHubPersonalAgentScheduleService aiHubPersonalAgentScheduleService) {

        this.aiHubPersonalAgentService = aiHubPersonalAgentService;
        this.aiHubPersonalAgentScheduleService = aiHubPersonalAgentScheduleService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            SetScheduleInput input = jsonMapper.readValue(toolInput, SetScheduleInput.class);

            if (input.agentId() == null) {
                return ToolErrors.toolError(jsonMapper, "agentId is required");
            }

            AiHubToolInvocationContext context =
                AiHubToolInvocationContext.fromToolContext(toolContext);

            if (context == null || context.workspaceId() == null || context.userId() == null) {
                return ToolErrors.toolError(jsonMapper,
                    "Workspace context unavailable — open this chat from the AI Hub of a workspace.");
            }

            Optional<AiHubPersonalAgent> agentOptional = aiHubPersonalAgentService.findOwned(
                input.agentId(), context.workspaceId(), context.userId());

            if (agentOptional.isEmpty()) {
                return ToolErrors.toolError(jsonMapper,
                    "Personal agent %d not found in the current workspace.".formatted(input.agentId()));
            }

            AiHubPersonalAgent agent = agentOptional.get();

            boolean clearRequested = Boolean.TRUE.equals(input.clear());

            ScheduleInput serviceInput;

            if (clearRequested) {
                serviceInput = null;
            } else {
                String validationError = validateScheduleFields(input);

                if (validationError != null) {
                    return ToolErrors.toolError(jsonMapper, validationError);
                }

                serviceInput = toServiceInput(input);
            }

            try {
                AiHubPersonalAgentSchedule schedule = aiHubPersonalAgentScheduleService.upsertOrDelete(
                    input.agentId(), context.workspaceId(), context.userId(), agent.getEnvironment(), serviceInput);

                if (schedule == null) {
                    return jsonMapper.writeValueAsString(
                        new SetScheduleOutput(agent.getId(), agent.getName(), false, null, null));
                }

                return jsonMapper.writeValueAsString(
                    new SetScheduleOutput(
                        agent.getId(), agent.getName(), schedule.isEnabled(), schedule.getTitle(),
                        schedule.getNextRunAt() == null ? null : String.valueOf(schedule.getNextRunAt())));
            } catch (IllegalArgumentException | IllegalStateException invalid) {
                // Cadence validation and the 1:1 schedule invariant surface here — return the message so the LLM
                // can correct the input rather than retry-loop.
                return ToolErrors.toolError(jsonMapper, invalid.getMessage());
            }
        } catch (DateTimeParseException exception) {
            return ToolErrors.toolError(
                jsonMapper, "Invalid date/time value: " + exception.getParsedString()
                    + " (timeOfDay uses HH:mm, startDate uses ISO-8601 local date-time)");
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, SetAiHubPersonalAgentScheduleToolCallback.class, TOOL_NAME, exception);
        }
    }

    private @Nullable String validateScheduleFields(SetScheduleInput input) {
        String title = input.title();

        if (title == null || title.isBlank()) {
            return "title is required when setting a schedule";
        }

        String prompt = input.prompt();

        if (prompt == null || prompt.isBlank()) {
            return "prompt is required when setting a schedule";
        }

        ScheduleFrequencyKind frequencyKind = input.frequencyKind();

        if (frequencyKind == null) {
            return "frequencyKind is required when setting a schedule";
        }

        String zoneId = input.zoneId();

        if (zoneId != null && !zoneId.isBlank()) {
            try {
                ZoneId.of(zoneId);
            } catch (RuntimeException exception) {
                return "Unknown zoneId '%s' — use an IANA id like Europe/Zagreb".formatted(zoneId);
            }
        }

        String cronExpression = input.cronExpression();

        return switch (frequencyKind) {
            case EVERY_X_MINUTES -> input.intervalMinutes() == null
                ? "intervalMinutes is required for EVERY_X_MINUTES" : null;
            case HOURLY -> input.minuteOfHour() == null ? "minuteOfHour is required for HOURLY" : null;
            case DAILY -> input.timeOfDay() == null ? "timeOfDay is required for DAILY" : null;
            case WEEKLY -> input.dayOfWeek() == null || input.timeOfDay() == null
                ? "dayOfWeek and timeOfDay are required for WEEKLY" : null;
            case MONTHLY -> input.dayOfMonth() == null || input.timeOfDay() == null
                ? "dayOfMonth and timeOfDay are required for MONTHLY" : null;
            case CUSTOM_CRON -> cronExpression == null || cronExpression.isBlank()
                ? "cronExpression is required for CUSTOM_CRON" : null;
        };
    }

    private ScheduleInput toServiceInput(SetScheduleInput input) {
        String inputZoneId = input.zoneId();

        String zoneId = inputZoneId == null || inputZoneId.isBlank() ? DEFAULT_ZONE_ID : inputZoneId;

        ScheduleLifecycleKind lifecycleKind =
            input.maxRuns() == null ? ScheduleLifecycleKind.RECURRING : ScheduleLifecycleKind.NUMBER_OF_RUNS;

        String timeOfDay = input.timeOfDay();
        String startDate = input.startDate();

        return new ScheduleInput(
            true,
            input.title(),
            input.prompt(),
            input.frequencyKind(),
            input.intervalMinutes(),
            input.minuteOfHour(),
            timeOfDay == null ? null : LocalTime.parse(timeOfDay),
            input.dayOfWeek(),
            input.dayOfMonth(),
            input.cronExpression(),
            zoneId,
            startDate == null ? null : LocalDateTime.parse(startDate),
            lifecycleKind,
            input.maxRuns());
    }

    public record SetScheduleInput(
        Long agentId, @Nullable Boolean clear, @Nullable String title, @Nullable String prompt,
        @Nullable ScheduleFrequencyKind frequencyKind, @Nullable Integer intervalMinutes,
        @Nullable Integer minuteOfHour, @Nullable String timeOfDay, @Nullable Integer dayOfWeek,
        @Nullable Integer dayOfMonth, @Nullable String cronExpression, @Nullable String zoneId,
        @Nullable String startDate, @Nullable Integer maxRuns) {
    }

    public record SetScheduleOutput(
        long agentId, String agentName, boolean scheduled, @Nullable String title, @Nullable String nextRunAt) {
    }
}
