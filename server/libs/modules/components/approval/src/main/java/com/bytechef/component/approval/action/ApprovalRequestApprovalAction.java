/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.approval.action;

import static com.bytechef.component.approval.constant.ApprovalConstants.DEFAULT_VALUE;
import static com.bytechef.component.approval.constant.ApprovalConstants.EXPIRES_IN;
import static com.bytechef.component.approval.constant.ApprovalConstants.EXPIRES_IN_UNIT;
import static com.bytechef.component.approval.constant.ApprovalConstants.EXPIRES_IN_UNIT_DAYS;
import static com.bytechef.component.approval.constant.ApprovalConstants.EXPIRES_IN_UNIT_HOURS;
import static com.bytechef.component.approval.constant.ApprovalConstants.FIELD_DESCRIPTION;
import static com.bytechef.component.approval.constant.ApprovalConstants.FIELD_LABEL;
import static com.bytechef.component.approval.constant.ApprovalConstants.FIELD_NAME;
import static com.bytechef.component.approval.constant.ApprovalConstants.FIELD_OPTIONS;
import static com.bytechef.component.approval.constant.ApprovalConstants.FIELD_TYPE;
import static com.bytechef.component.approval.constant.ApprovalConstants.MAX_SELECTION;
import static com.bytechef.component.approval.constant.ApprovalConstants.MIN_SELECTION;
import static com.bytechef.component.approval.constant.ApprovalConstants.MULTIPLE_CHOICE;
import static com.bytechef.component.approval.constant.ApprovalConstants.PLACEHOLDER;
import static com.bytechef.component.approval.constant.ApprovalConstants.REQUIRED;
import static com.bytechef.component.approval.util.FieldType.CHECKBOX;
import static com.bytechef.component.approval.util.FieldType.CUSTOM_HTML;
import static com.bytechef.component.approval.util.FieldType.DATETIME_PICKER;
import static com.bytechef.component.approval.util.FieldType.DATE_PICKER;
import static com.bytechef.component.approval.util.FieldType.EMAIL_INPUT;
import static com.bytechef.component.approval.util.FieldType.FILE_INPUT;
import static com.bytechef.component.approval.util.FieldType.HIDDEN_FIELD;
import static com.bytechef.component.approval.util.FieldType.INPUT;
import static com.bytechef.component.approval.util.FieldType.NUMBER_INPUT;
import static com.bytechef.component.approval.util.FieldType.PASSWORD_INPUT;
import static com.bytechef.component.approval.util.FieldType.RADIO;
import static com.bytechef.component.approval.util.FieldType.SELECT;
import static com.bytechef.component.approval.util.FieldType.TEXTAREA;
import static com.bytechef.component.approval.util.FieldType.fromValue;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.APPROVAL_CHANNELS;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.EXPIRES_AT;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_DESCRIPTION;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_TITLE;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.INPUTS;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.approval.util.FieldType;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.ActionDefinition.ResumePerformFunction.ResumeResponse;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.ai.approval.ApprovalRequestEvents;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.SuspendAwareSseEmitterHandler;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class ApprovalRequestApprovalAction {

    private static final Logger log = LoggerFactory.getLogger(ApprovalRequestApprovalAction.class);

    private static final String FORM_URL = "formUrl";

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    private ApprovalRequestApprovalAction(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    public static ModifiableActionDefinition of(ClusterElementDefinitionService clusterElementDefinitionService) {
        ApprovalRequestApprovalAction approvalRequestApprovalAction = new ApprovalRequestApprovalAction(
            clusterElementDefinitionService);

        return action("requestApproval")
            .title("Request Approval")
            .description("Sends an approval request and waits for a human to approve or reject.")
            .properties(
                string(FORM_TITLE)
                    .label("Form Title")
                    .description("The title for the approval form. Displayed as the main heading.")
                    .required(false),
                string(FORM_DESCRIPTION)
                    .label("Form Description")
                    .description("A description shown under the form title. Use \\n or <br> for line breaks.")
                    .controlType(TEXT_AREA)
                    .required(false),
                array(INPUTS)
                    .label("Form Inputs")
                    .description("Define the form input fields for the approval request.")
                    .items(
                        object()
                            .properties(
                                integer(FIELD_TYPE)
                                    .label("Field Type")
                                    .description("The type of the form field.")
                                    .options(
                                        option("Checkbox", CHECKBOX.getValue()),
                                        option("Custom HTML", CUSTOM_HTML.getValue()),
                                        option("Date Picker", DATE_PICKER.getValue()),
                                        option("Datetime Picker", DATETIME_PICKER.getValue()),
                                        option("Email", EMAIL_INPUT.getValue()),
                                        option("File", FILE_INPUT.getValue()),
                                        option("Hidden Field", HIDDEN_FIELD.getValue()),
                                        option("Input", INPUT.getValue()),
                                        option("Number", NUMBER_INPUT.getValue()),
                                        option("Password", PASSWORD_INPUT.getValue()),
                                        option("Radio Button", RADIO.getValue()),
                                        option("Select", SELECT.getValue()),
                                        option("Textarea", TEXTAREA.getValue()))
                                    .required(true),
                                string(FIELD_LABEL)
                                    .label("Field Label")
                                    .description("The label of the form field."),
                                string(FIELD_NAME)
                                    .label("Field Name")
                                    .description("The name of the form field.")
                                    .displayCondition(
                                        "inputs[index].fieldType != %s".formatted(CUSTOM_HTML.getValue()))
                                    .required(true),
                                string(FIELD_DESCRIPTION)
                                    .label("Field Description")
                                    .description("Description of the form field.")
                                    .required(false),
                                string(PLACEHOLDER)
                                    .label("Placeholder")
                                    .description("The placeholder text.")
                                    .required(false)
                                    .displayCondition(
                                        "contains({%s,%s,%s,%s,%s}, inputs[index].fieldType)".formatted(
                                            EMAIL_INPUT.getValue(), INPUT.getValue(), NUMBER_INPUT.getValue(),
                                            PASSWORD_INPUT.getValue(), TEXTAREA.getValue())),
                                string(DEFAULT_VALUE)
                                    .label("Default value")
                                    .description("Pre-filled or pre-selected value for compatible fields.")
                                    .displayCondition(
                                        "inputs[index].fieldType != %s".formatted(CUSTOM_HTML.getValue()))
                                    .required(false),
                                string(DEFAULT_VALUE)
                                    .label("Default value")
                                    .description("Pre-filled or pre-selected value for compatible fields.")
                                    .controlType(TEXT_AREA)
                                    .displayCondition(
                                        "inputs[index].fieldType == %s".formatted(CUSTOM_HTML.getValue()))
                                    .required(false),
                                array(FIELD_OPTIONS)
                                    .label("Field Options")
                                    .description("The field label/value options.")
                                    .items(
                                        object()
                                            .properties(
                                                string("label")
                                                    .label("Label")
                                                    .required(true),
                                                string("value")
                                                    .label("Value")
                                                    .required(true)))
                                    .required(false)
                                    .displayCondition(
                                        "contains({%s,%s}, inputs[index].fieldType)".formatted(
                                            RADIO.getValue(), SELECT.getValue())),
                                bool(MULTIPLE_CHOICE)
                                    .label("Multiple Choice")
                                    .description("Allow multiple selections.")
                                    .defaultValue(false)
                                    .required(false)
                                    .displayCondition(
                                        "inputs[index].fieldType == %s".formatted(SELECT.getValue())),
                                integer(MIN_SELECTION)
                                    .label("Min selection")
                                    .description("Minimum selections required.")
                                    .required(false)
                                    .displayCondition(
                                        "inputs[index].fieldType == %s and inputs[index].multipleChoice == true"
                                            .formatted(SELECT.getValue())),
                                integer(MAX_SELECTION)
                                    .label("Max selection")
                                    .description("Maximum selections allowed.")
                                    .required(false)
                                    .displayCondition(
                                        "inputs[index].fieldType == %s and inputs[index].multipleChoice == true"
                                            .formatted(SELECT.getValue())),
                                bool(REQUIRED)
                                    .label("Required")
                                    .description("Whether this field is required.")
                                    .defaultValue(false)
                                    .required(false)))
                    .required(false),
                integer(EXPIRES_IN)
                    .label("Expires In")
                    .description(
                        "How long the approval request stays resolvable. When it lapses, the request can no longer " +
                            "be approved and the paused run is failed. Defaults to 60 days.")
                    .minValue(1)
                    .required(false),
                string(EXPIRES_IN_UNIT)
                    .label("Expires In Unit")
                    .description("The time unit for the Expires In value.")
                    .options(
                        option("Hours", EXPIRES_IN_UNIT_HOURS),
                        option("Days", EXPIRES_IN_UNIT_DAYS))
                    .defaultValue(EXPIRES_IN_UNIT_DAYS)
                    .required(false))
            .output(ApprovalRequestApprovalAction::output)
            .perform((MultipleConnectionsPerformFunction) approvalRequestApprovalAction::perform)
            .resumePerform(ApprovalRequestApprovalAction::resumePerform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    protected static OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        List<Map<String, ?>> inputs = inputParameters.getList(INPUTS, new TypeReference<>() {}, List.of());
        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        properties.add(bool("approved"));
        // The reviewer's optional note, valid on BOTH outcomes (approve-with-comment and reject-with-comment alike).
        // "comment" is a reserved key on the approval form; a user-defined field with that name takes precedence and
        // suppresses the built-in comment box.
        properties.add(string("comment"));
        // The verified identity of whoever resolved the approval, when the resolving channel could establish one
        // (e.g. Slack in-place resolution). Absent for the anonymous hosted form, which has no trusted identity.
        properties.add(string("approvedBy"));

        for (Map<String, ?> input : inputs) {
            String fieldName = (String) input.get(FIELD_NAME);

            if (fieldName == null) {
                continue;
            }

            Integer fieldTypeInt = (Integer) input.get(FIELD_TYPE);

            FieldType fieldType = fieldTypeInt != null ? fromValue(fieldTypeInt) : INPUT;

            ModifiableValueProperty<?, ?> property = switch (fieldType) {
                case CHECKBOX -> bool(fieldName);
                case DATE_PICKER -> date(fieldName);
                case DATETIME_PICKER -> dateTime(fieldName);
                case FILE_INPUT -> fileEntry(fieldName);
                case NUMBER_INPUT -> number(fieldName);
                case SELECT -> {
                    Boolean multipleChoice = (Boolean) input.get(MULTIPLE_CHOICE);

                    yield Boolean.TRUE.equals(multipleChoice)
                        ? array(fieldName).items(string()) : string(fieldName);
                }
                default -> string(fieldName);
            };

            properties.add(property);
        }

        return OutputResponse.of(object().properties(properties.toArray(new ModifiableValueProperty[0])));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections, Parameters extensions,
        ActionContext context) throws Exception {

        ActionContextAware actionContextAware = (ActionContextAware) context;

        String resumeUrl = actionContextAware.getResumeUrl();

        if (resumeUrl == null) {
            throw new IllegalStateException(
                "Cannot generate approval form URL. Ensure the server's public URL is configured and the workflow is " +
                    "running in a proper execution context.");
        }

        String formUrl = resumeUrl.replace("/job/resume/", "/resume/");

        Instant expiresAt = getExpiresAt(inputParameters);

        if (actionContextAware.isEditorEnvironment()) {
            // Editor test runs skip channels (they are production transports), but the canvas test run listens on
            // the job's SSE stream. Deliver the same approval card event the chat channel would send by returning a
            // one-shot SSE emitter output: the in-process post-output processor drains it into the test-run stream
            // bridges. The suspend must happen inside the emitter handler — suspending before returning would make
            // the service layer replace the emitter output with the Suspend and the card would never be sent.
            if (actionContextAware.getJobId() != null) {
                return createEditorApprovalRequestEmitterHandler(inputParameters, formUrl, expiresAt,
                    actionContextAware);
            }
        } else {
            ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

            List<ClusterElement> approvalChannels = clusterElementMap.getClusterElements(APPROVAL_CHANNELS);

            deliverToChannels(
                approvalChannels, inputParameters, componentConnections, formUrl, expiresAt, actionContextAware);
        }

        suspend(context, formUrl, expiresAt);

        return null;
    }

    /**
     * Delivers the approval request to every configured channel, best-effort: a channel whose send fails is logged and
     * skipped so the remaining channels (and the suspend itself) still happen — a deleted Slack channel must not
     * prevent the email fallback from delivering, and a delivery failure must pause the run rather than fail it. Only
     * when EVERY configured channel fails is the action failed, because then nobody was notified and pausing would be
     * the silent no-op the fallback advice exists to prevent. Channels additionally receive the computed expiry under
     * the well-known {@code expiresAt} key (ISO-8601) so messages can tell the approver when the request lapses.
     */
    private void deliverToChannels(
        List<ClusterElement> approvalChannels, Parameters inputParameters,
        Map<String, ComponentConnection> componentConnections, String formUrl, Instant expiresAt,
        ActionContextAware actionContextAware) {

        if (approvalChannels.isEmpty()) {
            return;
        }

        int deliveredCount = 0;
        Exception lastException = null;

        for (ClusterElement approvalChannel : approvalChannels) {
            ComponentConnection componentConnection = componentConnections.get(
                approvalChannel.getWorkflowNodeName());

            Map<String, Object> channelInputParameters = MapUtils.concat(
                new HashMap<>(inputParameters.toMap()), new HashMap<>(approvalChannel.getParameters()));

            channelInputParameters.put(EXPIRES_AT, expiresAt.toString());

            try {
                clusterElementDefinitionService.executeApprovalChannel(
                    approvalChannel.getComponentName(), approvalChannel.getComponentVersion(),
                    approvalChannel.getClusterElementName(), channelInputParameters, formUrl, componentConnection,
                    actionContextAware);

                deliveredCount++;
            } catch (Exception exception) {
                lastException = exception;

                log.warn(
                    "Approval channel {}/{} failed to deliver the approval request: {}",
                    approvalChannel.getComponentName(), approvalChannel.getClusterElementName(),
                    exception.getMessage());
            }
        }

        if (deliveredCount == 0) {
            throw new IllegalStateException(
                "None of the " + approvalChannels.size() + " configured approval channels could deliver the " +
                    "approval request.",
                lastException);
        }
    }

    private static void suspend(ActionContext context, String formUrl, Instant expiresAt) {
        context.suspend(new Suspend(Map.of(FORM_URL, formUrl), expiresAt));
    }

    /**
     * Resolves the suspend expiry from the optional Expires In / Expires In Unit properties; an unset or invalid value
     * falls back to the 60-day default.
     */
    private static Instant getExpiresAt(Parameters inputParameters) {
        Integer expiresIn = inputParameters.getInteger(EXPIRES_IN);

        if (expiresIn == null || expiresIn < 1) {
            return Instant.now()
                .plus(60, ChronoUnit.DAYS);
        }

        String expiresInUnit = inputParameters.getString(EXPIRES_IN_UNIT, EXPIRES_IN_UNIT_DAYS);

        ChronoUnit chronoUnit = EXPIRES_IN_UNIT_HOURS.equals(expiresInUnit) ? ChronoUnit.HOURS : ChronoUnit.DAYS;

        return Instant.now()
            .plus(expiresIn, chronoUnit);
    }

    /**
     * Builds the editor-run delivery for the approval card: a {@link SuspendAwareSseEmitterHandler} that sends the
     * {@code approval_request} data event onto the run's SSE stream, suspends the run, and completes. The wrapper
     * carries the action context so the post-output processor can observe the suspend once the stream is drained — the
     * same mechanism the AI agent's streaming action uses for mid-stream suspends.
     */
    private static SuspendAwareSseEmitterHandler createEditorApprovalRequestEmitterHandler(
        Parameters inputParameters, String formUrl, Instant expiresAt, ActionContextAware actionContextAware) {

        Map<String, Object> eventData = ApprovalRequestEvents.buildApprovalRequestEventData(inputParameters, formUrl);

        // The builder reads the expiry from the channel input parameters, which only the production fan-out
        // populates — the editor event gets it from the action's own computed value instead.
        eventData.put(EXPIRES_AT, expiresAt.toString());

        return new SuspendAwareSseEmitterHandler(
            sseEmitter -> {
                sseEmitter.send(eventData);

                suspend(actionContextAware, formUrl, expiresAt);

                sseEmitter.complete();
            },
            actionContextAware);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static ResumeResponse resumePerform(
        Parameters inputParameters, Parameters connectionParameters, Parameters continueParameters, Parameters data,
        ActionContext context) {

        Map<String, Object> dataMap = new HashMap<>(data.toMap());

        // ResumeResponse.of() wraps the map as {data, resumed}, but the output schema declared by this action is
        // flat (top-level `approved` + form-field names), so expressions like ${approval_1.name} work. Reshape the
        // response to match the schema by replacing the wrapped contents with the flat data.
        ResumeResponse response = ResumeResponse.of(dataMap);

        response.clear();
        response.putAll(dataMap);

        return response;
    }
}
