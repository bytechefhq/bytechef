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

package com.bytechef.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.agui.core.exception.AGUIException;
import com.bytechef.ai.copilot.agent.SliceSpringAIAgent;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.util.Source;
import com.bytechef.automation.ai.tool.AssetFileToolCallbacksFactory;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author Ivica Cardic
 */
final class AssetFileAgentConfigurationTest {

    private static final Pattern BACKTICK_IDENTIFIER_PATTERN = Pattern.compile("`([A-Za-z][A-Za-z0-9_.]*)`");

    private final AssetFileAgentConfiguration configuration = newConfiguration();

    private final SecurityContextRehydrator securityContextRehydrator = mock(SecurityContextRehydrator.class);

    private final AssetFileToolCallbacksFactory assetFileToolCallbacksFactory =
        new AssetFileToolCallbacksFactory(mock(AssetFileFacade.class), null);

    @Test
    void testAskAgentIdAndToolList() throws AGUIException {
        SliceSpringAIAgent askAgent = configuration.assetFileAskSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class), assetFileToolCallbacksFactory, securityContextRehydrator,
            emptyProvider());

        assertThat(askAgent.getAgentId()).isEqualTo(CopilotAgentType.ASSET_FILE_ASK.key());

        Set<String> askToolNames = toolNames(
            configuration.askToolCallbacks(securityContextRehydrator, assetFileToolCallbacksFactory));

        assertThat(askToolNames).containsExactlyInAnyOrder("listAssetFiles", "getAssetFileContent");
    }

    @Test
    void testBuildAgentIdAndToolList() throws AGUIException {
        SliceSpringAIAgent buildAgent = configuration.assetFileBuildSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class), assetFileToolCallbacksFactory, securityContextRehydrator,
            emptyProvider());

        assertThat(buildAgent.getAgentId()).isEqualTo(CopilotAgentType.ASSET_FILE_BUILD.key());

        Set<String> buildToolNames = toolNames(
            configuration.buildToolCallbacks(securityContextRehydrator, assetFileToolCallbacksFactory));

        assertThat(buildToolNames).containsExactlyInAnyOrder(
            "listAssetFiles", "getAssetFileContent", "createAssetFile", "createBinaryAssetFile",
            "updateAssetFileContent", "cloneAssetFile", "createAssetFileFromUrl");
    }

    @Test
    void testBuildPromptToolsAreRegisteredOnWriteToolCallbacks() {
        Set<String> promptToolNames = extractBacktickIdentifiers(getResourceField("promptAssetFileBuildResource"));
        Set<String> writeToolNames = toolNames(
            configuration.buildToolCallbacks(securityContextRehydrator, assetFileToolCallbacksFactory));

        assertThat(promptToolNames).isNotEmpty();
        assertThat(writeToolNames).containsAll(promptToolNames);
    }

    @Test
    void testAskPromptToolsAreRegisteredOnReadToolCallbacks() {
        Set<String> promptToolNames = extractBacktickIdentifiers(getResourceField("promptAssetFileAskResource"));
        Set<String> readToolNames = toolNames(
            configuration.askToolCallbacks(securityContextRehydrator, assetFileToolCallbacksFactory));

        assertThat(promptToolNames).isNotEmpty();
        assertThat(readToolNames).containsAll(promptToolNames);
    }

    @Test
    void testDerivedAgentIdsMatchCopilotAgentTypeKeys() {
        assertThat(Source.ASSET_FILE.name()
            .toLowerCase() + "_ask").isEqualTo(CopilotAgentType.ASSET_FILE_ASK.key());
        assertThat(Source.ASSET_FILE.name()
            .toLowerCase() + "_build").isEqualTo(CopilotAgentType.ASSET_FILE_BUILD.key());
    }

    private static AssetFileAgentConfiguration newConfiguration() {
        AssetFileAgentConfiguration configuration = new AssetFileAgentConfiguration();

        setResourceField(configuration, "promptAssetFileAskResource", "prompt_asset_file_ask.txt");
        setResourceField(configuration, "promptAssetFileBuildResource", "prompt_asset_file_build.txt");

        return configuration;
    }

    private static void setResourceField(
        AssetFileAgentConfiguration configuration, String fieldName, String classpathResource) {

        try {
            Field field = AssetFileAgentConfiguration.class.getDeclaredField(fieldName);

            field.setAccessible(true);
            field.set(configuration, new ClassPathResource(classpathResource));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Resource getResourceField(String fieldName) {
        try {
            Field field = AssetFileAgentConfiguration.class.getDeclaredField(fieldName);

            field.setAccessible(true);

            return (Resource) field.get(configuration);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Set<String> extractBacktickIdentifiers(Resource resource) {
        String content = readResource(resource);

        Matcher matcher = BACKTICK_IDENTIFIER_PATTERN.matcher(content);

        Set<String> identifiers = new HashSet<>();

        while (matcher.find()) {
            identifiers.add(matcher.group(1));
        }

        return identifiers;
    }

    private static String readResource(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read resource: " + resource.getDescription(), exception);
        }
    }

    private static Set<String> toolNames(List<ToolCallback> toolCallbacks) {
        Set<String> toolNames = new HashSet<>();

        for (ToolCallback toolCallback : toolCallbacks) {
            toolNames.add(
                toolCallback.getToolDefinition()
                    .name());
        }

        return toolNames;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }
}
