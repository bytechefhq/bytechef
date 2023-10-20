
            /**
             * The ByteChef Enterprise license (the "Enterprise License")
             * Copyright (c) 2023 - present ByteChef Inc.
             *
             * With regard to the ByteChef Software:
             *
             * This software and associated documentation files (the "Software") may only be
             * used in production, if you (and any entity that you represent) have agreed to,
             * and are in compliance with, the ByteChef Subscription Terms of Service, available
             * via email (support@bytechef.io) (the "Enterprise Terms"), or other
             * agreement governing the use of the Software, as agreed by you and ByteChef,
             * and otherwise have a valid ByteChef Enterprise license for the
             * correct number of user seats. Subject to the foregoing sentence, you are free to
             * modify this Software and publish patches to the Software. You agree that ByteChef
             * and/or its licensors (as applicable) retain all right, title and interest in and
             * to all such modifications and/or patches, and all such modifications and/or
             * patches may only be used, copied, modified, displayed, distributed, or otherwise
             * exploited with a valid ByteChef Enterprise license for the  correct
             * number of user seats.  Notwithstanding the foregoing, you may copy and modify
             * the Software for development and testing purposes, without requiring a
             * subscription.  You agree that ByteChef and/or its licensors (as applicable) retain
             * all right, title and interest in and to all such modifications.  You are not
             * granted any other rights beyond what is expressly stated herein.  Subject to the
             * foregoing, it is forbidden to copy, merge, publish, distribute, sublicense,
             * and/or sell the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
             * SOFTWARE.
             *
             * For all third party components incorporated into the ByteChef Software, those
             * components are licensed under the original license provided by the owner of the
             * applicable component.
             */
            
package com.bytechef.worker.config;

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.ApplyResponse;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.registry.domain.ConnectionDefinition;
import com.bytechef.hermes.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.hermes.component.registry.dto.ComponentConnection;
import com.bytechef.hermes.component.registry.remote.client.facade.RemoteConnectionDefinitionFacadeClient;
import com.bytechef.hermes.component.registry.service.ConnectionDefinitionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

@Configuration
public class DefinitionRegistryConfiguration {

    @Bean("workerConnectionDefinitionService")
    @Primary
    ConnectionDefinitionService connectionDefinitionService(
        @Qualifier("connectionDefinitionService") ConnectionDefinitionService connectionDefinitionService,
        RemoteConnectionDefinitionFacadeClient connectionDefinitionFacadeClient) {

        return new WorkerConnectionDefinitionService(connectionDefinitionService, connectionDefinitionFacadeClient);
    }

    /**
     * Compound ConnectionDefinitionService impl that supports the use case where a component (for example, HttpClient
     * or Script) uses a compatible connection from a different component that can be in a different worker instance.
     */
    private record WorkerConnectionDefinitionService(
        ConnectionDefinitionService connectionDefinitionService,
        RemoteConnectionDefinitionFacadeClient connectionDefinitionFacadeClient)
        implements ConnectionDefinitionService {

        /**
         * Called from the Context.Connection instance.
         */
        @Override
        public ApplyResponse executeAuthorizationApply(
            @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

            if (connectionDefinitionService.connectionExists(componentName, connection.version())) {
                return connectionDefinitionService.executeAuthorizationApply(componentName, connection, context);
            } else {
                return connectionDefinitionFacadeClient.executeAuthorizationApply(componentName, connection);
            }
        }

        /**
         * Called from the ConnectionFacade instance.
         */
        @Override
        public AuthorizationCallbackResponse executeAuthorizationCallback(
            @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context,
            @NonNull String redirectUri) {

            return connectionDefinitionService.executeAuthorizationCallback(
                componentName, connection, context, redirectUri);
        }

        @Override
        public boolean connectionExists(String componentName, int connectionVersion) {
            return connectionDefinitionService.connectionExists(componentName, connectionVersion);
        }

        /**
         * Called from the HttpClientExecutor instance.
         */
        @Override
        public Optional<String> executeBaseUri(
            @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

            if (connectionDefinitionService.connectionExists(componentName, connection.version())) {
                return connectionDefinitionService.executeBaseUri(componentName, connection, context);
            } else {
                return connectionDefinitionFacadeClient.executeBaseUri(componentName, connection);
            }
        }

        @Override
        public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
            @NonNull String componentName, @NonNull ComponentConnection connection, @NonNull Context context) {

            return connectionDefinitionService.getOAuth2AuthorizationParameters(componentName, connection, context);
        }

        @Override
        public Authorization.AuthorizationType getAuthorizationType(
            String componentName, int connectionVersion, String authorizationName) {

            throw new UnsupportedOperationException();
        }

        @Override
        public ConnectionDefinition getConnectionDefinition(String componentName, int componentVersion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ConnectionDefinition> getConnectionDefinitions() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ConnectionDefinition> getConnectionDefinitions(String componentName, Integer componentVersion) {
            throw new UnsupportedOperationException();
        }
    }
}
