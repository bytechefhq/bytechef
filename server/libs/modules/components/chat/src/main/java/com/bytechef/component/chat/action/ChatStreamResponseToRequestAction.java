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

package com.bytechef.component.chat.action;

import static com.bytechef.component.chat.constant.ChatConstants.ATTACHMENTS;
import static com.bytechef.component.chat.constant.ChatConstants.STREAM;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;

/**
 * @author Ivica Cardic
 */
public class ChatStreamResponseToRequestAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("streamResponseToRequest")
        .title("Stream Response to Chat Request")
        .description("Stream the response to chat request.")
        .properties(
            string(STREAM)
                .label("Stream")
                .description("The stream messages."),
            array(ATTACHMENTS)
                .label("Attachments")
                .description("The attachments of the response.")
                .placeholder("Add attachment")
                .items(fileEntry()))
        .output(ChatStreamResponseToRequestAction::output)
        .perform(ChatStreamResponseToRequestAction::perform);

    protected static OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return OutputResponse.of(object(STREAM));
    }

    protected static SseEmitterHandler perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        @SuppressWarnings("unchecked")
        Publisher<String> publisher = (Publisher<String>) inputParameters.getRequired(STREAM);

        return emitter -> publisher.subscribe(
            new Flow.Subscriber<>() {

                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;

                    emitter.addTimeoutListener(subscription::cancel);

                    // Request unbounded items to start streaming
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(String item) {
                    try {
                        emitter.send(item);
                    } catch (Exception exception) {
                        context.log(log -> log.trace(exception.getMessage(), exception));

                        subscription.cancel();
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    emitter.error(throwable);
                }

                @Override
                public void onComplete() {
                    emitter.complete();
                }
            });
    }
}
