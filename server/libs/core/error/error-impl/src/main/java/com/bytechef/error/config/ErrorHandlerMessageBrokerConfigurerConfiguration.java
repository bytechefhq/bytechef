
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

package com.bytechef.error.config;

import com.bytechef.error.ErrorHandler;
import com.bytechef.message.broker.SystemMessageRoute;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableConfigurationProperties(ErrorHandlerProperties.class)
public class ErrorHandlerMessageBrokerConfigurerConfiguration {

    @Bean
    MessageBrokerConfigurer<?> errorHandlerMessageBrokerConfigurer(
        ErrorHandler<?> errorHandler, ErrorHandlerProperties errorHandlerProperties) {

        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            ErrorHandlerProperties.ErrorHandlerSubscriptions subscriptions = errorHandlerProperties.getSubscriptions();

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar, SystemMessageRoute.ERRORS, subscriptions.getErrors(), errorHandler,
                "handle");
        };
    }
}
