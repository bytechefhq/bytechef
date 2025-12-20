/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.appevent.trigger;

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.platform.component.definition.AppEventComponentDefinition.NEW_EVENT;

import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.ee.embedded.configuration.domain.AppEvent;
import com.bytechef.ee.embedded.configuration.service.AppEventService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AppEventTrigger {

    private static final String APP_EVENT_ID = "appEventId";

    private final AppEventService appEventService;

    public final TriggerDefinition triggerDefinition = trigger(NEW_EVENT)
        .title("New Event")
        .description("Triggers when new app event is sent.")
        .type(TriggerDefinition.TriggerType.STATIC_WEBHOOK)
        .properties(
            integer(APP_EVENT_ID)
                .label("App Event Id")
                .description("The Id of an app event.")
                .options((TriggerDefinition.OptionsFunction<Long>) this::getOptions))
        .output(this::output)
        .webhookRequest(this::webhookRequest);

    @SuppressFBWarnings("EI")
    public AppEventTrigger(AppEventService appEventService) {
        this.appEventService = appEventService;
    }

    protected List<? extends Option<Long>> getOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, TriggerContext context) {

        return appEventService.getAppEvents()
            .stream()
            .map(appEvent -> {
                Long id = appEvent.getId();

                return option(appEvent.getName(), id.longValue());
            })
            .toList();
    }

    protected OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, TriggerContext context) {

        if (!inputParameters.containsKey(APP_EVENT_ID)) {
            return null;
        }

        AppEvent appEvent = appEventService.getAppEvent(inputParameters.getLong(APP_EVENT_ID));

        Object value = context.json(json -> json.read(appEvent.getSchema()));

        return OutputResponse.of(context.outputSchema(outputSchema -> outputSchema.getOutputSchema(value)), value);
    }

    protected Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        return body.getContent();
    }
}
