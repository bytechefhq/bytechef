
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.coordinator.config;

import com.bytechef.error.ErrorHandler;
import com.bytechef.error.Errorable;
import com.bytechef.hermes.coordinator.TriggerCoordinator;
import com.bytechef.hermes.coordinator.completion.TriggerCompletionHandler;
import com.bytechef.hermes.coordinator.error.TriggerExecutionErrorHandler;
import com.bytechef.message.broker.MessageBroker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class TriggerCoordinatorConfiguration {

    @Bean
    TriggerCompletionHandler triggerCompletionHandler(ErrorHandler<? super Errorable> errorHandle) {
        return new TriggerCompletionHandler(errorHandle);
    }

    @Bean
    TriggerCoordinator triggerCoordinator(
        MessageBroker messageBroker, TriggerCompletionHandler triggerCompletionHandler) {

        return new TriggerCoordinator(messageBroker, triggerCompletionHandler);
    }

    @Bean
    TriggerExecutionErrorHandler triggerExecutionErrorHandler() {
        return new TriggerExecutionErrorHandler();
    }
}
