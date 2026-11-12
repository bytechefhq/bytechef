/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionErrorCode;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionDTO;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionTestEventResultDTO;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionWebhookSettingsDTO;
import com.bytechef.ee.embedded.configuration.exception.TriggerSubscriptionEnableException;
import com.bytechef.ee.embedded.configuration.facade.TriggerSubscriptionAdminFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class TriggerSubscriptionGraphQlController {

    private static final String INTERNAL_ERROR_MESSAGE = "Internal error while enabling the trigger subscription";

    private final TriggerSubscriptionAdminFacade triggerSubscriptionAdminFacade;

    @SuppressFBWarnings("EI")
    public TriggerSubscriptionGraphQlController(TriggerSubscriptionAdminFacade triggerSubscriptionAdminFacade) {
        this.triggerSubscriptionAdminFacade = triggerSubscriptionAdminFacade;
    }

    @QueryMapping
    public List<TriggerSubscriptionDTO> connectedUserTriggerSubscriptions(@Argument long connectedUserId) {
        return triggerSubscriptionAdminFacade.getConnectedUserTriggerSubscriptions(connectedUserId);
    }

    @QueryMapping
    public TriggerSubscriptionWebhookSettingsDTO triggerSubscriptionWebhookSettings(@Argument long environmentId) {
        return triggerSubscriptionAdminFacade.getWebhookSettings(environmentId);
    }

    @MutationMapping
    public boolean deleteTriggerSubscription(@Argument long id) {
        triggerSubscriptionAdminFacade.deleteTriggerSubscription(id);

        return true;
    }

    @MutationMapping
    public boolean enableTriggerSubscription(@Argument long id, @Argument boolean enable) {
        triggerSubscriptionAdminFacade.enableTriggerSubscription(id, enable);

        return true;
    }

    @MutationMapping
    public TriggerSubscriptionWebhookSettingsDTO rotateTriggerSubscriptionWebhookSecret(
        @Argument long environmentId) {

        return triggerSubscriptionAdminFacade.rotateSigningSecret(environmentId);
    }

    @MutationMapping
    public TriggerSubscriptionTestEventResultDTO sendTriggerSubscriptionTestEvent(@Argument long environmentId) {
        return triggerSubscriptionAdminFacade.sendTestEvent(environmentId);
    }

    @MutationMapping
    public TriggerSubscriptionWebhookSettingsDTO updateTriggerSubscriptionWebhookUrl(
        @Argument long environmentId, @Argument String webhookUrl) {

        return triggerSubscriptionAdminFacade.updateWebhookUrl(environmentId, webhookUrl);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleTriggerSubscriptionEnableException(
        TriggerSubscriptionEnableException triggerSubscriptionEnableException,
        DataFetchingEnvironment dataFetchingEnvironment) {

        TriggerSubscriptionErrorCode errorCode = triggerSubscriptionEnableException.getErrorCode();

        if (errorCode == TriggerSubscriptionErrorCode.INTERNAL_ERROR) {
            return GraphqlErrorBuilder.newError(dataFetchingEnvironment)
                .errorType(ErrorType.INTERNAL_ERROR)
                .message(INTERNAL_ERROR_MESSAGE)
                .extensions(Map.of("errorCode", errorCode.name()))
                .build();
        }

        return GraphqlErrorBuilder.newError(dataFetchingEnvironment)
            .errorType(ErrorType.BAD_REQUEST)
            .message(triggerSubscriptionEnableException.getMessage())
            .extensions(Map.of("errorCode", errorCode.name()))
            .build();
    }
}
