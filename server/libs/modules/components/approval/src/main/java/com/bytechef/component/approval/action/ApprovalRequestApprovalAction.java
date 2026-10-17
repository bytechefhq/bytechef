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
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.PUBLIC_URL;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.RESUME_ID;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.RESUME_URL;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.SHORT_RESUME_ID;

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
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ActionContextAware.ApprovalLinks;
import com.bytechef.platform.component.definition.ApprovalFormInputs;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
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

    private static final String APPROVED = "approved";
    private static final String APPROVED_BY = "approvedBy";
    private static final String COMMENT = "comment";
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
            .help("", "https://docs.bytechef.io/reference/components/approval_v1#request-approval")
            .perform((MultipleConnectionsPerformFunction) approvalRequestApprovalAction::perform)
            .resumePerform(ApprovalRequestApprovalAction::resumePerform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    protected static OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        List<Map<String, ?>> inputs = inputParameters.getList(INPUTS, new TypeReference<>() {}, List.of());
        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        properties.add(bool(APPROVED));
        properties.add(string(COMMENT));
        properties.add(string(APPROVED_BY));

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

        Instant expiresAt = getExpiresAt(inputParameters);

        ApprovalLinks approvalLinks = actionContextAware.getApprovalLinks(expiresAt);

        if (approvalLinks == null) {
            throw new IllegalStateException(
                "Cannot generate approval form URL. Ensure the server's public URL is configured and the workflow is " +
                    "running in a proper execution context.");
        }

        String formUrl = approvalLinks.formUrl();

        if (!actionContextAware.isEditorEnvironment()) {
            ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

            List<ClusterElement> approvalChannels = clusterElementMap.getClusterElements(APPROVAL_CHANNELS);

            deliverToChannels(
                approvalChannels, inputParameters, componentConnections, approvalLinks, expiresAt,
                actionContextAware);
        }

        suspend(context, formUrl, expiresAt);

        return null;
    }

    private void deliverToChannels(
        List<ClusterElement> approvalChannels, Parameters inputParameters,
        Map<String, ComponentConnection> componentConnections, ApprovalLinks approvalLinks, Instant expiresAt,
        ActionContextAware actionContextAware) {

        if (approvalChannels.isEmpty()) {
            return;
        }

        int deliveredCount = 0;
        List<String> failureMessages = new ArrayList<>();
        List<Exception> failureExceptions = new ArrayList<>();

        for (ClusterElement approvalChannel : approvalChannels) {
            ComponentConnection componentConnection = componentConnections.get(
                approvalChannel.getWorkflowNodeName());

            Map<String, Object> channelInputParameters = MapUtils.concat(
                new HashMap<>(inputParameters.toMap()), new HashMap<>(approvalChannel.getParameters()));

            channelInputParameters.put(EXPIRES_AT, expiresAt.toString());
            channelInputParameters.put(PUBLIC_URL, approvalLinks.publicUrl());
            channelInputParameters.put(RESUME_ID, approvalLinks.resumeId());
            channelInputParameters.put(RESUME_URL, approvalLinks.resumeUrl());

            if (approvalLinks.shortResumeId() == null) {
                channelInputParameters.remove(SHORT_RESUME_ID);
            } else {
                channelInputParameters.put(SHORT_RESUME_ID, approvalLinks.shortResumeId());
            }

            try {
                clusterElementDefinitionService.executeApprovalChannel(
                    approvalChannel.getComponentName(), approvalChannel.getComponentVersion(),
                    approvalChannel.getClusterElementName(), channelInputParameters, approvalLinks.formUrl(),
                    componentConnection, actionContextAware);

                deliveredCount++;
            } catch (Exception exception) {
                failureMessages.add(
                    approvalChannel.getComponentName() + "/" + approvalChannel.getClusterElementName() + ": " +
                        getRootCauseMessage(exception));
                failureExceptions.add(exception);

                log.warn(
                    "Approval channel {}/{} failed to deliver the approval request",
                    approvalChannel.getComponentName(), approvalChannel.getClusterElementName(), exception);
            }
        }

        if (deliveredCount == 0) {
            IllegalStateException illegalStateException = new IllegalStateException(
                "None of the " + approvalChannels.size() + " configured approval channels could deliver the " +
                    "approval request. " + String.join("; ", failureMessages));

            failureExceptions.forEach(illegalStateException::addSuppressed);

            throw illegalStateException;
        }
    }

    private static String getRootCauseMessage(Throwable throwable) {
        Throwable rootCause = throwable;

        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        String message = rootCause.getMessage();

        if (message == null || message.isBlank()) {
            message = throwable.getMessage();
        }

        if (message == null || message.isBlank()) {
            Class<?> rootCauseClass = rootCause.getClass();

            return rootCauseClass.getSimpleName();
        }

        return message;
    }

    private static void suspend(ActionContext context, String formUrl, Instant expiresAt) {
        Instant raisedAt = Instant.now();

        context.suspend(
            new Suspend(
                Map.of(
                    FORM_URL, formUrl, MetadataConstants.APPROVAL_RAISED_AT, raisedAt.toString(),
                    MetadataConstants.APPROVAL_RESUME, true),
                expiresAt));
    }

    private static Instant getExpiresAt(Parameters inputParameters) {
        Integer expiresIn = inputParameters.getInteger(EXPIRES_IN);

        if (expiresIn == null || expiresIn < 1) {
            return Instant.now()
                .plus(ActionContextAware.DEFAULT_APPROVAL_EXPIRY);
        }

        String expiresInUnit = inputParameters.getString(EXPIRES_IN_UNIT, EXPIRES_IN_UNIT_DAYS);

        ChronoUnit chronoUnit = EXPIRES_IN_UNIT_HOURS.equals(expiresInUnit) ? ChronoUnit.HOURS : ChronoUnit.DAYS;

        return Instant.now()
            .plus(expiresIn, chronoUnit);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static ResumeResponse resumePerform(
        Parameters inputParameters, Parameters connectionParameters, Parameters continueParameters, Parameters data,
        ActionContext context) {

        Map<String, Object> submittedData = new HashMap<>(data.toMap());

        ApprovalFormInputs.Resolution resolution = ApprovalFormInputs.resolve(
            inputParameters.getList(INPUTS, List.of()), submittedData,
            Boolean.TRUE.equals(submittedData.get(APPROVED)));

        Map<String, Object> dataMap = resolution.data();

        if (dataMap.get(COMMENT) == null) {
            dataMap.put(COMMENT, "");
        }

        if (dataMap.get(APPROVED_BY) == null) {
            dataMap.put(APPROVED_BY, "");
        }

        // ResumeResponse.of() wraps the map as {data, resumed}, but the output schema declared by this action is
        // flat (top-level `approved` + form-field names), so expressions like ${approval_1.name} work. Reshape the
        // response to match the schema by replacing the wrapped contents with the flat data.
        ResumeResponse response = ResumeResponse.of(dataMap);

        response.clear();
        response.putAll(dataMap);

        return response;
    }
}
