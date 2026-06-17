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

package com.bytechef.component.ai.llm.ollama.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_SCHEMA;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.F16KV;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.KEEP_ALIVE;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.LOGTS_ALL;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.LOW_VRAM;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MAIN_GPU;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MIROSTAT;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MIROSTAT_ETA;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.MIROSTAT_TAU;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_BATCH;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_CTX;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_GPU;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_KEEP;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.NUM_THREAD;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.PENALIZE_NEW_LINE;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.REPEAT_LAST_N;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.REPEAT_PENALTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.TFSZ;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.TRUNCATE;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.TYPICAL_P;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.URL;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.USE_MLOCK;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.USE_MMAP;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.USE_NUMA;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.VOCAB_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;

/**
 * @author Nikolina Spehar
 */
class OllamaChatActionTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private OllamaApi.Builder mockedApiBuilder;
    private MockedStatic<OllamaApi> ollamaApiMockedStatic;

    private static final String TEST_SCHEMA = "{\"type\":\"object\",\"properties\":{\"answer\":{\"type\":\"string\"}}}";

    private static final Map<String, Object> FULL_INPUT_TEXT = Map.ofEntries(
        Map.entry(MODEL, "llama3"),
        Map.entry(TEMPERATURE, 0.7),
        Map.entry(TOP_P, 0.9),
        Map.entry(TOP_K, 40),
        Map.entry(STOP, List.of("stop")),
        Map.entry(FREQUENCY_PENALTY, 0.5),
        Map.entry(PRESENCE_PENALTY, 0.3),
        Map.entry(SEED, 42),
        Map.entry(KEEP_ALIVE, "5m"),
        Map.entry(F16KV, true),
        Map.entry(LOGTS_ALL, false),
        Map.entry(USE_MMAP, true),
        Map.entry(LOW_VRAM, false),
        Map.entry(MAIN_GPU, 0),
        Map.entry(MIROSTAT, 1),
        Map.entry(MIROSTAT_ETA, 0.1f),
        Map.entry(MIROSTAT_TAU, 5.0f),
        Map.entry(NUM_BATCH, 512),
        Map.entry(NUM_CTX, 2048),
        Map.entry(NUM_GPU, 1),
        Map.entry(NUM_KEEP, 5),
        Map.entry(NUM_THREAD, 8),
        Map.entry(MAX_TOKENS, 1000),
        Map.entry(PENALIZE_NEW_LINE, true),
        Map.entry(REPEAT_LAST_N, 64),
        Map.entry(REPEAT_PENALTY, 1.1),
        Map.entry(TFSZ, 1.0f),
        Map.entry(TRUNCATE, false),
        Map.entry(TYPICAL_P, 1.0f),
        Map.entry(USE_MLOCK, false),
        Map.entry(USE_NUMA, false),
        Map.entry(VOCAB_ONLY, false),
        Map.entry(RESPONSE, Map.of(
            RESPONSE_FORMAT, ChatModel.ResponseFormat.TEXT.name(),
            RESPONSE_SCHEMA, "")));

    private static final Map<String, Object> FULL_INPUT_JSON = Map.ofEntries(
        Map.entry(MODEL, "llama3"),
        Map.entry(TEMPERATURE, 0.7),
        Map.entry(TOP_P, 0.9),
        Map.entry(TOP_K, 40),
        Map.entry(STOP, List.of("stop")),
        Map.entry(FREQUENCY_PENALTY, 0.5),
        Map.entry(PRESENCE_PENALTY, 0.3),
        Map.entry(SEED, 42),
        Map.entry(KEEP_ALIVE, "5m"),
        Map.entry(F16KV, true),
        Map.entry(LOGTS_ALL, false),
        Map.entry(USE_MMAP, true),
        Map.entry(LOW_VRAM, false),
        Map.entry(MAIN_GPU, 0),
        Map.entry(MIROSTAT, 1),
        Map.entry(MIROSTAT_ETA, 0.1f),
        Map.entry(MIROSTAT_TAU, 5.0f),
        Map.entry(NUM_BATCH, 512),
        Map.entry(NUM_CTX, 2048),
        Map.entry(NUM_GPU, 1),
        Map.entry(NUM_KEEP, 5),
        Map.entry(NUM_THREAD, 8),
        Map.entry(MAX_TOKENS, 1000),
        Map.entry(PENALIZE_NEW_LINE, true),
        Map.entry(REPEAT_LAST_N, 64),
        Map.entry(REPEAT_PENALTY, 1.1),
        Map.entry(TFSZ, 1.0f),
        Map.entry(TRUNCATE, false),
        Map.entry(TYPICAL_P, 1.0f),
        Map.entry(USE_MLOCK, false),
        Map.entry(USE_NUMA, false),
        Map.entry(VOCAB_ONLY, false),
        Map.entry(RESPONSE, Map.of(
            RESPONSE_FORMAT, ChatModel.ResponseFormat.JSON.name(),
            RESPONSE_SCHEMA, TEST_SCHEMA)));

    @BeforeEach
    void beforeEach() {
        ollamaApiMockedStatic = mockStatic(OllamaApi.class);
        mockedApiBuilder = mock(OllamaApi.Builder.class);
        OllamaApi mockedApi = mock(OllamaApi.class);

        ollamaApiMockedStatic.when(OllamaApi::builder)
            .thenReturn(mockedApiBuilder);

        when(mockedApiBuilder.baseUrl(stringArgumentCaptor.capture()))
            .thenReturn(mockedApiBuilder);
        when(mockedApiBuilder.build())
            .thenReturn(mockedApi);
    }

    @AfterEach
    void afterEach() {
        ollamaApiMockedStatic.close();
    }

    @Test
    void testCreateChatModelWithCustomUrl() {
        Parameters mockedConnectionParameters = MockParametersFactory.create(
            Map.of(URL, "http://localhost:11434"));
        Parameters mockedInputParameters = MockParametersFactory.create(FULL_INPUT_TEXT);

        org.springframework.ai.chat.model.ChatModel chatModel = OllamaChatAction.CHAT_MODEL.createChatModel(
            mockedInputParameters, mockedConnectionParameters, false);

        assertNotNull(chatModel);
        assertInstanceOf(OllamaChatModel.class, chatModel);

        assertEquals(List.of("http://localhost:11434"), stringArgumentCaptor.getAllValues());

        OllamaChatOptions options = (OllamaChatOptions) chatModel.getDefaultOptions();
        assertOptions(options);
        assertNull(options.getOutputSchema());
    }

    @Test
    void testCreateChatModelWithEmptyUrl() {
        Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(URL, ""));
        Parameters mockedInputParameters = MockParametersFactory.create(FULL_INPUT_TEXT);

        org.springframework.ai.chat.model.ChatModel chatModel = OllamaChatAction.CHAT_MODEL.createChatModel(
            mockedInputParameters, mockedConnectionParameters, false);

        assertNotNull(chatModel);
        assertInstanceOf(OllamaChatModel.class, chatModel);

        verify(mockedApiBuilder, never()).baseUrl(org.mockito.ArgumentMatchers.anyString());

        OllamaChatOptions options = (OllamaChatOptions) chatModel.getDefaultOptions();
        assertOptions(options);
        assertNull(options.getOutputSchema());
    }

    @Test
    void testCreateChatModelWithJsonSchema() {
        Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(URL, ""));
        Parameters mockedInputParameters = MockParametersFactory.create(FULL_INPUT_JSON);

        org.springframework.ai.chat.model.ChatModel chatModel = OllamaChatAction.CHAT_MODEL.createChatModel(
            mockedInputParameters, mockedConnectionParameters, false);

        assertNotNull(chatModel);
        assertInstanceOf(OllamaChatModel.class, chatModel);

        OllamaChatOptions options = (OllamaChatOptions) chatModel.getDefaultOptions();
        assertOptions(options);
        assertEquals(TEST_SCHEMA, options.getOutputSchema());
    }

    private void assertOptions(OllamaChatOptions options) {
        assertEquals("llama3", options.getModel());
        assertEquals(0.7, options.getTemperature());
        assertEquals(0.9, options.getTopP());
        assertEquals(40, options.getTopK());
        assertEquals(List.of("stop"), options.getStop());
        assertEquals(0.5, options.getFrequencyPenalty());
        assertEquals(0.3, options.getPresencePenalty());
        assertEquals(42, options.getSeed());
        assertEquals("5m", options.getKeepAlive());
        assertEquals(true, options.getF16KV());
        assertEquals(false, options.getLogitsAll());
        assertEquals(true, options.getUseMMap());
        assertEquals(false, options.getLowVRAM());
        assertEquals(0, options.getMainGPU());
        assertEquals(1, options.getMirostat());
        assertEquals(0.1f, options.getMirostatEta());
        assertEquals(5.0f, options.getMirostatTau());
        assertEquals(512, options.getNumBatch());
        assertEquals(2048, options.getNumCtx());
        assertEquals(1, options.getNumGPU());
        assertEquals(5, options.getNumKeep());
        assertEquals(8, options.getNumThread());
        assertEquals(1000, options.getNumPredict());
        assertEquals(true, options.getPenalizeNewline());
        assertEquals(64, options.getRepeatLastN());
        assertEquals(1.1, options.getRepeatPenalty());
        assertEquals(1.0f, options.getTfsZ());
        assertEquals(false, options.getTruncate());
        assertEquals(1.0f, options.getTypicalP());
        assertEquals(false, options.getUseMLock());
        assertEquals(false, options.getUseNUMA());
        assertEquals(false, options.getVocabOnly());
    }
}
