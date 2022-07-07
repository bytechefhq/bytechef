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

package com.bytechef;

import com.bytechef.atlas.MapObject;
import com.bytechef.atlas.coordinator.event.EventListenerChain;
import com.bytechef.atlas.event.EventListener;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.task.execution.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.evaluator.spel.SpelTaskEvaluator;
import com.bytechef.atlas.task.execution.evaluator.spel.TempDir;
import com.bytechef.hermes.encryption.EncryptionKey;
import com.bytechef.hermes.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.hermes.file.storage.converter.FileEntryConverter;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

/**
 * @author Ivica Cardic
 */
@ComponentScan
@EnableAutoConfiguration
@SpringBootConfiguration
public class IntTestConfiguration {

    @EnableCaching
    @EnableConfigurationProperties(CacheProperties.class)
    @TestConfiguration
    public class CacheConfiguration {}

    @TestConfiguration
    public static class EncryptionKeyConfiguration {

        @Bean
        EncryptionKey encryptionKey() {
            return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
        }
    }

    @TestConfiguration
    public static class EventConfiguration {

        @Bean
        @Primary
        EventListenerChain eventListener(List<EventListener> eventListeners) {
            return new EventListenerChain(eventListeners);
        }

        @Bean
        EventPublisher eventPublisher(EventListener eventListener) {
            return eventListener::onApplicationEvent;
        }
    }

    @TestConfiguration
    public static class FileStorageConfiguration {

        @PostConstruct
        void afterPropertiesSet() {
            MapObject.addConverter(new FileEntryConverter());
        }

        @Bean
        FileStorageService fileStorageService() {
            return new Base64FileStorageService();
        }
    }

    @TestConfiguration
    public static class MessageBrokerConfiguration {

        @Bean
        MessageBroker messageBroker() {
            return new SyncMessageBroker();
        }
    }

    @TestConfiguration
    public static class TaskEvaluatorConfiguration {

        @Bean
        TaskEvaluator taskEvaluator(Environment environment) {
            return SpelTaskEvaluator.builder()
                    .environment(environment)
                    .methodExecutor("tempDir", new TempDir())
                    .build();
        }
    }
}
