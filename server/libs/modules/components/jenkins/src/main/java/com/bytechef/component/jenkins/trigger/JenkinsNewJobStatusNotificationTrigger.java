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

package com.bytechef.component.jenkins.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class JenkinsNewJobStatusNotificationTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newJobStatusNotification")
        .title("New Job Status Notification")
        .description("Triggers when job statuses are changed.")
        .type(TriggerType.STATIC_WEBHOOK)
        .output(
            outputSchema(
                object()
                    .properties(
                        string("name")
                            .description("Name of the Jenkins job."),
                        string("display_name")
                            .description("Display name of the Jenkins job."),
                        string("url")
                            .description("URL of the Jenkins job."),
                        object("build")
                            .description("Jenkins job build.")
                            .properties(
                                string("full_url")
                                    .description("Full URL of the Jenkins job."),
                                integer("number")
                                    .description("Number of the Jenkins job."),
                                integer("queue_id")
                                    .description("Queue ID of the Jenkins job."),
                                number("timestamp")
                                    .description("Timestamp of the Jenkins job notification."),
                                integer("duration")
                                    .description("Duration of the Jenkins job."),
                                string("phase")
                                    .description("Phase of the Jenkins job."),
                                string("url")
                                    .description("URL of Jenkins job build."),
                                object("scm")
                                    .properties(
                                        array("changes")
                                            .description("Changes of the Jenkins build."),
                                        array("culprits")
                                            .description("Culprits of the Jenkins build changes.")),
                                string("log")
                                    .description("Log for the Jenkins job build."),
                                string("notes")
                                    .description("Additional notes of the Jenkins job build."),
                                object("artifacts")
                                    .description("Artifacts of Jenkins job build.")))))
        .webhookRequest(JenkinsNewJobStatusNotificationTrigger::webhookRequest);

    private JenkinsNewJobStatusNotificationTrigger() {
    }

    protected static Map<String, Object> webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        return body.getContent(new TypeReference<>() {});
    }
}
