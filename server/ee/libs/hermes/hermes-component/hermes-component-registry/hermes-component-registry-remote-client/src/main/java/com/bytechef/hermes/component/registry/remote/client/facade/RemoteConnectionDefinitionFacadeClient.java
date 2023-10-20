
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
            
package com.bytechef.hermes.component.registry.remote.client.facade;

import com.bytechef.commons.webclient.DefaultWebClient;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.hermes.component.registry.dto.ComponentConnection;
import com.bytechef.hermes.component.registry.facade.ConnectionDefinitionFacade;
import com.bytechef.hermes.component.registry.remote.client.AbstractWorkerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectionDefinitionFacadeClient extends AbstractWorkerClient
    implements ConnectionDefinitionFacade {

    private static final String CONNECTION_DEFINITION_FACADE = "/connection-definition-facade";

    public RemoteConnectionDefinitionFacadeClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultWebClient, discoveryClient, objectMapper);
    }

    @Override
    public Authorization.ApplyResponse executeAuthorizationApply(
        @NonNull String componentName, @NonNull ComponentConnection connection) {

        return defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, CONNECTION_DEFINITION_FACADE + "/execute-authorization-apply"),
            new ConnectionRequest(componentName, connection), Authorization.ApplyResponse.class);
    }

    @Override
    public Authorization.AuthorizationCallbackResponse executeAuthorizationCallback(
        @NonNull String componentName,
        @NonNull ComponentConnection connection, @NonNull String redirectUri) {

        return defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, CONNECTION_DEFINITION_FACADE + "/execute-authorization-callback"),
            new AuthorizationCallbackRequest(componentName, connection, redirectUri),
            Authorization.AuthorizationCallbackResponse.class);
    }

    @Override
    public Optional<String> executeBaseUri(
        @NonNull String componentName, ComponentConnection connection) {
        return Optional.ofNullable(
            defaultWebClient.post(
                uriBuilder -> toUri(uriBuilder, componentName, CONNECTION_DEFINITION_FACADE + "/execute-base-uri"),
                new ConnectionRequest(componentName, connection), String.class));
    }

    @Override
    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        @NonNull String componentName, ComponentConnection connection) {

        return defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, CONNECTION_DEFINITION_FACADE + "/get-oauth2-authorization-parameters"),
            new ConnectionRequest(componentName, connection),
            OAuth2AuthorizationParameters.class);
    }

    private record AuthorizationCallbackRequest(
        String componentName, ComponentConnection connection, String redirectUri) {
    }

    private record ConnectionRequest(String componentName, ComponentConnection connection) {
    }
}
