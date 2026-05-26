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

package com.bytechef.component.ai.llm.amazon.bedrock.action;

import static com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants.ACCESS_KEY_ID;
import static com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants.REGION;
import static com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants.SECRET_ACCESS_KEY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;

/**
 * @author Nikolina Spehar
 */
class AmazonBedrockChatActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(
        Map.of(ACCESS_KEY_ID, "access-key-id", SECRET_ACCESS_KEY, "secret-access-key", REGION, "us-east-1"));
    private final Parameters mockedInputParameters = MockParametersFactory.create(
        Map.of(MODEL, "amazon.nova-pro-v1:0", MAX_TOKENS, 1000, TEMPERATURE, 0.7, TOP_P, 0.9));
    private final ArgumentCaptor<Region> regionArgumentCaptor = forClass(Region.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testCreateChatModel() {
        try (MockedStatic<BedrockRuntimeClient> bedrockRuntimeClientMockedStatic =
            mockStatic(BedrockRuntimeClient.class);
            MockedStatic<StaticCredentialsProvider> staticCredentialsProviderMockedStatic =
                mockStatic(StaticCredentialsProvider.class);
            MockedStatic<AwsBasicCredentials> awsBasicCredentialsMockedStatic =
                mockStatic(AwsBasicCredentials.class)) {

            AwsBasicCredentials mockedCredentials = mock(AwsBasicCredentials.class);
            StaticCredentialsProvider mockedCredentialsProvider = mock(StaticCredentialsProvider.class);
            BedrockRuntimeClient mockedBedrockRuntimeClient = mock(BedrockRuntimeClient.class);

            awsBasicCredentialsMockedStatic
                .when(() -> AwsBasicCredentials.create(
                    stringArgumentCaptor.capture(),
                    stringArgumentCaptor.capture()))
                .thenReturn(mockedCredentials);

            staticCredentialsProviderMockedStatic
                .when(() -> StaticCredentialsProvider.create(mockedCredentials))
                .thenReturn(mockedCredentialsProvider);

            BedrockRuntimeClientBuilder mockedBedrockClientBuilder =
                mock(BedrockRuntimeClientBuilder.class);

            bedrockRuntimeClientMockedStatic.when(BedrockRuntimeClient::builder)
                .thenReturn(mockedBedrockClientBuilder);

            when(mockedBedrockClientBuilder.credentialsProvider(mockedCredentialsProvider))
                .thenReturn(mockedBedrockClientBuilder);

            when(mockedBedrockClientBuilder.region(regionArgumentCaptor.capture()))
                .thenReturn(mockedBedrockClientBuilder);

            when(mockedBedrockClientBuilder.build())
                .thenReturn(mockedBedrockRuntimeClient);

            org.springframework.ai.chat.model.ChatModel chatModel = AmazonBedrockChatAction.CHAT_MODEL.createChatModel(
                mockedInputParameters, mockedConnectionParameters, false);

            assertNotNull(chatModel);
            assertInstanceOf(BedrockProxyChatModel.class, chatModel);

            assertEquals(Region.of("us-east-1"), regionArgumentCaptor.getValue());
            assertEquals(List.of("access-key-id", "secret-access-key"), stringArgumentCaptor.getAllValues());

            BedrockProxyChatModel bedrockProxyChatModel = (BedrockProxyChatModel) chatModel;

            ChatOptions bedrockChatOptions = bedrockProxyChatModel.getDefaultOptions();

            assertEquals("amazon.nova-pro-v1:0", bedrockChatOptions.getModel());
            assertEquals(1000, bedrockChatOptions.getMaxTokens());
            assertEquals(0.7, bedrockChatOptions.getTemperature(), 0.0001);
            assertEquals(0.9, bedrockChatOptions.getTopP(), 0.0001);
        }
    }
}
