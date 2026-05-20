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

package com.bytechef.component.ai.llm.openai.action;

import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler.SseEmitter;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class OpenAiStreamChatActionTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final SseEmitter mockedEmitter = mock(SseEmitter.class);
    private final Flow.Subscription mockedSubscription = mock(Flow.Subscription.class);

    private Flow.Subscriber<Object> subscriber;

    @BeforeEach
    void setUp() {
        subscriber = new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                mockedEmitter.addTimeoutListener(subscription::cancel);
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Object item) {
                try {
                    mockedEmitter.send(item);
                } catch (Exception exception) {
                    mockedContext.log(log -> log.trace(exception.getMessage(), exception));
                    subscription.cancel();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                mockedEmitter.error(throwable);
            }

            @Override
            public void onComplete() {
                mockedEmitter.complete();
            }
        };
    }

    @Test
    void testOnSubscribeSetsTimeoutListenerAndRequestsAll() {
        subscriber.onSubscribe(mockedSubscription);

        ArgumentCaptor<Runnable> timeoutCaptor = forClass(Runnable.class);

        verify(mockedEmitter)
            .addTimeoutListener(timeoutCaptor.capture());

        Runnable timeout = timeoutCaptor.getValue();
        timeout.run();

        verify(mockedSubscription)
            .cancel();
        verify(mockedSubscription)
            .request(Long.MAX_VALUE);
    }

    @Test
    void testOnNextSendsItemToEmitter() {
        subscriber.onSubscribe(mockedSubscription);

        Object item = "chunk";

        subscriber.onNext(item);

        verify(mockedEmitter)
            .send(item);
    }

    @Test
    void testOnNextLogsAndCancelsOnSendException() {
        subscriber.onSubscribe(mockedSubscription);

        doThrow(new RuntimeException("send failed"))
            .when(mockedEmitter)
            .send(any());

        subscriber.onNext("chunk");

        verify(mockedContext)
            .log(any());
        verify(mockedSubscription)
            .cancel();
    }

    @Test
    void testOnErrorForwardsThrowableToEmitter() {
        Throwable throwable = new RuntimeException("stream error");

        subscriber.onError(throwable);

        verify(mockedEmitter)
            .error(throwable);
    }

    @Test
    void testOnCompleteCompletesEmitter() {
        subscriber.onComplete();

        verify(mockedEmitter)
            .complete();
    }
}
