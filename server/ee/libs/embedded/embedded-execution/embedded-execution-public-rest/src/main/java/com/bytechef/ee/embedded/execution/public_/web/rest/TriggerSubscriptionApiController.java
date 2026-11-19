/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionErrorCode;
import com.bytechef.ee.embedded.configuration.dto.ComponentTriggerDTO;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionDTO;
import com.bytechef.ee.embedded.configuration.exception.TriggerSubscriptionEnableException;
import com.bytechef.ee.embedded.configuration.exception.TriggerSubscriptionErrorType;
import com.bytechef.ee.embedded.configuration.facade.TriggerSubscriptionFacade;
import com.bytechef.ee.embedded.execution.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ErrorsInnerMetaModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ErrorsInnerMetaModel.OriginEnum;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ErrorsInnerModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.TriggerModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.TriggerSubscriptionErrorCodeModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.TriggerSubscriptionErrorModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.TriggerSubscriptionFailureModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.TriggerSubscriptionModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.TriggerSubscriptionRequestModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.TriggerSubscriptionStatusModel;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class TriggerSubscriptionApiController implements TriggerSubscriptionApi {

    private static final String INTERNAL_ERROR_MESSAGE = "Internal error while processing the trigger subscription";
    private static final String NOT_FOUND_CODE = "NOT_FOUND";
    private static final HttpStatusCode REMOTE_PROVIDER_ERROR_STATUS = HttpStatusCode.valueOf(499);

    private static final Logger log = LoggerFactory.getLogger(TriggerSubscriptionApiController.class);

    private final EnvironmentService environmentService;
    private final TriggerSubscriptionFacade triggerSubscriptionFacade;

    @SuppressFBWarnings("EI")
    public TriggerSubscriptionApiController(
        EnvironmentService environmentService, TriggerSubscriptionFacade triggerSubscriptionFacade) {

        this.environmentService = environmentService;
        this.triggerSubscriptionFacade = triggerSubscriptionFacade;
    }

    @Override
    public ResponseEntity<TriggerSubscriptionModel> createTriggerSubscription(
        String externalUserId, String componentName, Integer componentVersion, String triggerName,
        TriggerSubscriptionRequestModel triggerSubscriptionRequestModel, @Nullable EnvironmentModel xEnvironment,
        @Nullable Long xInstanceId) {

        TriggerSubscriptionDTO triggerSubscriptionDTO = triggerSubscriptionFacade.createTriggerSubscription(
            externalUserId, componentName, componentVersion, triggerName,
            new HashMap<String, Object>(triggerSubscriptionRequestModel.getInput()),
            triggerSubscriptionRequestModel.getName(),
            triggerSubscriptionRequestModel.getExternalId(), triggerSubscriptionRequestModel.getWebhookUrl(),
            xInstanceId, getEnvironment(xEnvironment));

        return ResponseEntity.ok(toTriggerSubscriptionModel(triggerSubscriptionDTO));
    }

    @Override
    public ResponseEntity<Void> deleteTriggerSubscription(
        String externalUserId, Long id, @Nullable EnvironmentModel xEnvironment) {

        triggerSubscriptionFacade.deleteTriggerSubscription(externalUserId, id, getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> disableTriggerSubscription(
        String externalUserId, Long id, @Nullable EnvironmentModel xEnvironment) {

        triggerSubscriptionFacade.enableTriggerSubscription(externalUserId, id, false, getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableTriggerSubscription(
        String externalUserId, Long id, @Nullable EnvironmentModel xEnvironment) {

        triggerSubscriptionFacade.enableTriggerSubscription(externalUserId, id, true, getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<List<TriggerModel>> getComponentTriggers(
        String externalUserId, String componentName, Integer componentVersion,
        @Nullable EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            triggerSubscriptionFacade.getComponentTriggers(externalUserId, componentName, componentVersion)
                .stream()
                .map(TriggerSubscriptionApiController::toTriggerModel)
                .toList());
    }

    @Override
    public ResponseEntity<TriggerSubscriptionModel> getTriggerSubscription(
        String externalUserId, Long id, @Nullable EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            toTriggerSubscriptionModel(
                triggerSubscriptionFacade.getTriggerSubscription(externalUserId, id, getEnvironment(xEnvironment))));
    }

    @Override
    public ResponseEntity<List<TriggerSubscriptionModel>> getTriggerSubscriptions(
        String externalUserId, @Nullable EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            triggerSubscriptionFacade.getTriggerSubscriptions(externalUserId, getEnvironment(xEnvironment))
                .stream()
                .map(TriggerSubscriptionApiController::toTriggerSubscriptionModel)
                .toList());
    }

    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<TriggerSubscriptionFailureModel> handleConfigurationException(
        ConfigurationException configurationException) {

        if (configurationException.getEntityClass() != TriggerSubscription.class) {
            log.error("Trigger subscription request failed with an internal configuration error",
                configurationException);

            return toFailureResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, TriggerSubscriptionErrorCode.INTERNAL_ERROR.name(),
                INTERNAL_ERROR_MESSAGE, OriginEnum.BYTECHEF, null);
        }

        int errorKey = configurationException.getErrorKey();

        if (errorKey == TriggerSubscriptionErrorType.TRIGGER_SUBSCRIPTION_NOT_FOUND.getErrorKey()) {
            return toFailureResponse(
                HttpStatus.NOT_FOUND, NOT_FOUND_CODE, configurationException.getMessage(), OriginEnum.BYTECHEF, null);
        }

        if (errorKey == TriggerSubscriptionErrorType.NO_TRIGGER.getErrorKey()) {
            return toFailureResponse(
                HttpStatus.BAD_REQUEST, TriggerSubscriptionErrorCode.NO_TRIGGER.name(),
                configurationException.getMessage(), OriginEnum.BYTECHEF, null);
        }

        if (errorKey == TriggerSubscriptionErrorType.USER_INPUT_ERROR.getErrorKey()) {
            return toFailureResponse(
                HttpStatus.BAD_REQUEST, TriggerSubscriptionErrorCode.USER_INPUT_ERROR.name(),
                configurationException.getMessage(), OriginEnum.BYTECHEF, null);
        }

        log.error("Trigger subscription request failed with error key {}", errorKey, configurationException);

        return toFailureResponse(
            HttpStatus.INTERNAL_SERVER_ERROR, TriggerSubscriptionErrorCode.INTERNAL_ERROR.name(),
            INTERNAL_ERROR_MESSAGE, OriginEnum.BYTECHEF, null);
    }

    @ExceptionHandler(TriggerSubscriptionEnableException.class)
    public ResponseEntity<TriggerSubscriptionFailureModel> handleTriggerSubscriptionEnableException(
        TriggerSubscriptionEnableException triggerSubscriptionEnableException) {

        TriggerSubscriptionErrorCode errorCode = triggerSubscriptionEnableException.getErrorCode();

        if (errorCode == TriggerSubscriptionErrorCode.INTERNAL_ERROR) {
            return toFailureResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, errorCode.name(), triggerSubscriptionEnableException.getMessage(),
                OriginEnum.BYTECHEF, triggerSubscriptionEnableException.getTriggerSubscriptionId());
        }

        if (errorCode == TriggerSubscriptionErrorCode.USER_INPUT_ERROR) {
            return toFailureResponse(
                HttpStatus.BAD_REQUEST, errorCode.name(), triggerSubscriptionEnableException.getMessage(),
                OriginEnum.BYTECHEF, triggerSubscriptionEnableException.getTriggerSubscriptionId());
        }

        return toFailureResponse(
            REMOTE_PROVIDER_ERROR_STATUS, errorCode.name(), triggerSubscriptionEnableException.getMessage(),
            OriginEnum.REMOTE_PROVIDER, triggerSubscriptionEnableException.getTriggerSubscriptionId());
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    @Override
    public ResponseEntity<TriggerSubscriptionModel> updateTriggerSubscription(
        String externalUserId, Long id, TriggerSubscriptionRequestModel triggerSubscriptionRequestModel,
        @Nullable EnvironmentModel xEnvironment) {

        TriggerSubscriptionDTO triggerSubscriptionDTO = triggerSubscriptionFacade.updateTriggerSubscription(
            externalUserId, id, new HashMap<String, Object>(triggerSubscriptionRequestModel.getInput()),
            triggerSubscriptionRequestModel.getName(), triggerSubscriptionRequestModel.getExternalId(),
            triggerSubscriptionRequestModel.getWebhookUrl(), getEnvironment(xEnvironment));

        return ResponseEntity.ok(toTriggerSubscriptionModel(triggerSubscriptionDTO));
    }

    private Environment getEnvironment(@Nullable EnvironmentModel xEnvironment) {
        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }

    private static ResponseEntity<TriggerSubscriptionFailureModel> toFailureResponse(
        HttpStatusCode httpStatusCode, String code, @Nullable String message, OriginEnum origin,
        @Nullable Long triggerSubscriptionId) {

        String detail = message == null ? code : message;

        ErrorsInnerModel errorsInnerModel = new ErrorsInnerModel()
            .code(code)
            .detail(detail)
            .id(String.valueOf(UUID.randomUUID()))
            .meta(new ErrorsInnerMetaModel().origin(origin))
            .problemType(code)
            .status(String.valueOf(httpStatusCode.value()))
            .title(detail);

        TriggerSubscriptionFailureModel triggerSubscriptionFailureModel = new TriggerSubscriptionFailureModel()
            .errors(List.of(errorsInnerModel))
            .triggerSubscriptionId(triggerSubscriptionId);

        return ResponseEntity.status(httpStatusCode)
            .body(triggerSubscriptionFailureModel);
    }

    private static @Nullable OffsetDateTime toOffsetDateTime(@Nullable Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private static TriggerModel toTriggerModel(ComponentTriggerDTO componentTriggerDTO) {
        return new TriggerModel()
            .description(componentTriggerDTO.description())
            .hasDynamicOutput(componentTriggerDTO.hasDynamicOutput())
            .inputSchema(componentTriggerDTO.inputSchema())
            .name(componentTriggerDTO.name())
            .outputSchema(componentTriggerDTO.outputSchema())
            .title(componentTriggerDTO.title())
            .type(componentTriggerDTO.type());
    }

    private static @Nullable TriggerSubscriptionErrorModel toTriggerSubscriptionErrorModel(
        TriggerSubscriptionDTO triggerSubscriptionDTO) {

        TriggerSubscriptionErrorCode errorCode = triggerSubscriptionDTO.errorCode();

        if (errorCode == null) {
            return null;
        }

        return new TriggerSubscriptionErrorModel()
            .code(TriggerSubscriptionErrorCodeModel.valueOf(errorCode.name()))
            .message(triggerSubscriptionDTO.errorMessage());
    }

    private static TriggerSubscriptionModel toTriggerSubscriptionModel(TriggerSubscriptionDTO triggerSubscriptionDTO) {
        return new TriggerSubscriptionModel()
            .componentName(triggerSubscriptionDTO.componentName())
            .componentVersion(triggerSubscriptionDTO.componentVersion())
            .createdDate(toOffsetDateTime(triggerSubscriptionDTO.createdDate()))
            .error(toTriggerSubscriptionErrorModel(triggerSubscriptionDTO))
            .externalId(triggerSubscriptionDTO.externalId())
            .id(triggerSubscriptionDTO.id())
            .input(new HashMap<String, Object>(triggerSubscriptionDTO.input()))
            .lastDeliveryError(triggerSubscriptionDTO.lastDeliveryError())
            .lastDeliveryStatus(triggerSubscriptionDTO.lastDeliveryStatus())
            .lastEventDate(toOffsetDateTime(triggerSubscriptionDTO.lastEventDate()))
            .lastModifiedDate(toOffsetDateTime(triggerSubscriptionDTO.lastModifiedDate()))
            .name(triggerSubscriptionDTO.name())
            .staticWebhookUrl(triggerSubscriptionDTO.staticWebhookUrl())
            .status(TriggerSubscriptionStatusModel.valueOf(triggerSubscriptionDTO.status()
                .name()))
            .triggerName(triggerSubscriptionDTO.triggerName())
            .webhookUrl(triggerSubscriptionDTO.webhookUrl());
    }
}
