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

package com.bytechef.component.ai.llm.util;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import com.bytechef.component.definition.Parameters;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public final class StreamChatUtils {

    private StreamChatUtils() {
    }

    public static SseEmitterHandler createSseEmitterHandler(
        Flow.Publisher<?> publisher, Parameters inputParameters, ActionContext context) {

        return emitter -> publisher.subscribe(
            new Flow.Subscriber<Object>() {

                private final AtomicBoolean failed = new AtomicBoolean();
                private final AtomicBoolean textReceived = new AtomicBoolean();
                private final StringBuilder responseTextBuilder = new StringBuilder();
                private Flow.@Nullable Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;

                    emitter.addTimeoutListener(subscription::cancel);

                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(Object item) {
                    if (item instanceof String text) {
                        responseTextBuilder.append(text);

                        textReceived.set(true);
                    }

                    try {
                        emitter.send(item);
                    } catch (Exception exception) {
                        context.log(log -> log.trace(exception.getMessage(), exception));

                        if (subscription != null) {
                            subscription.cancel();
                        }

                        if (failed.compareAndSet(false, true)) {
                            emitter.error(exception);
                        }
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    if (failed.compareAndSet(false, true)) {
                        emitter.error(throwable);
                    }
                }

                @Override
                public void onComplete() {
                    if (failed.get()) {
                        return;
                    }

                    try {
                        Object output = ModelUtils.toChatResponse(
                            textReceived.get() ? responseTextBuilder.toString() : null, inputParameters, true, context);

                        if (output != null) {
                            emitter.setOutput(output);
                        }
                    } catch (RuntimeException exception) {
                        context.log(log -> log.warn(
                            "Unable to convert the streamed response into the task output: {}",
                            exception.getMessage(), exception));
                    }

                    emitter.complete();
                }
            });
    }
}
