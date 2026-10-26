/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.apiconnector.configuration.service.WebScrapeService.CrawlResult;
import com.bytechef.ee.platform.apiconnector.configuration.service.WebScrapeService.ScrapeResult;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.ai.guardrails.AiGuardrailsAdvisorProvider;
import com.bytechef.platform.ai.guardrails.GuardrailSurface;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiConnectorAiServiceTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderProvider =
        mock(ObjectProvider.class);
    private final ApiConnectorGenerationJobService jobService = mock(ApiConnectorGenerationJobService.class);
    private final ChatModel chatModel = mock(ChatModel.class);
    private final WebScrapeService webScrapeService = mock(WebScrapeService.class);

    private final ApiConnectorAiServiceImpl service = new ApiConnectorAiServiceImpl(
        aiGuardrailsAdvisorProviderProvider, jobService, chatModel, webScrapeService);

    private void givenLlmReturns(String text) {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();

        when(chatModel.getOptions()).thenReturn(ChatOptions.builder()
            .build());
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
    }

    @Test
    void testGenerationPassesThroughTheGuardrailsAdvisor() {
        AiGuardrailsAdvisorProvider aiGuardrailsAdvisorProvider = mock(AiGuardrailsAdvisorProvider.class);
        RecordingAdvisor recordingAdvisor = new RecordingAdvisor();

        when(aiGuardrailsAdvisorProviderProvider.getIfAvailable()).thenReturn(aiGuardrailsAdvisorProvider);
        when(aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(null, GuardrailSurface.API_CONNECTOR))
            .thenReturn(Optional.of(recordingAdvisor));
        when(webScrapeService.scrape("https://docs.example.com"))
            .thenReturn(ScrapeResult.success("# API docs"));

        givenLlmReturns("openapi: \"3.0.0\"");

        service.generateOpenApiSpecification("https://docs.example.com");

        assertThat(recordingAdvisor.advisedCall).isTrue();
    }

    private static final class RecordingAdvisor implements CallAdvisor {

        private boolean advisedCall;

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
            advisedCall = true;

            return callAdvisorChain.nextCall(chatClientRequest);
        }

        @Override
        public String getName() {
            return "RecordingAdvisor";
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }

    @Test
    void testGenerateScrapesAndCleansResponse() {
        when(webScrapeService.scrape("https://docs.example.com"))
            .thenReturn(ScrapeResult.success("# API docs"));
        givenLlmReturns("```yaml\nopenapi: \"3.0.0\"\n```");

        String specification = service.generateOpenApiSpecification("https://docs.example.com");

        assertThat(specification).isEqualTo("openapi: \"3.0.0\"");
    }

    @Test
    void testGenerateThrowsWhenScrapeFails() {
        when(webScrapeService.scrape(any())).thenReturn(ScrapeResult.failure("boom"));

        assertThatThrownBy(() -> service.generateOpenApiSpecification("https://docs.example.com"))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testCleanOpenApiResponseStripsPlainFence() {
        when(webScrapeService.scrape(any())).thenReturn(ScrapeResult.success("# docs"));
        givenLlmReturns("```\nopenapi: \"3.0.0\"\n```");

        String specification = service.generateOpenApiSpecification("https://docs.example.com");

        assertThat(specification).isEqualTo("openapi: \"3.0.0\"");
    }

    @Test
    void testAsyncWithMaxPagesOneScrapes() {
        when(jobService.isCancellationRequested("j1")).thenReturn(false);
        when(webScrapeService.scrape("https://docs.example.com"))
            .thenReturn(ScrapeResult.success("# API docs"));
        givenLlmReturns("openapi: \"3.0.0\"");

        service.generateOpenApiSpecificationAsync("j1", "https://docs.example.com", null, 1);

        verify(webScrapeService).scrape("https://docs.example.com");
        verify(webScrapeService, never()).crawl(any(), anyInt(), anyList());
        verify(jobService).markAsProcessing("j1");
        verify(jobService).markAsCompleted(eq("j1"), any());
    }

    @Test
    void testAsyncWithMaxPagesGreaterThanOneCrawls() {
        when(jobService.isCancellationRequested("j1")).thenReturn(false);
        when(webScrapeService.crawl("https://docs.example.com", 5, List.of()))
            .thenReturn(CrawlResult.success("# combined", List.of("https://docs.example.com")));
        givenLlmReturns("openapi: \"3.0.0\"");

        service.generateOpenApiSpecificationAsync("j1", "https://docs.example.com", null, 5);

        verify(webScrapeService).crawl("https://docs.example.com", 5, List.of());
        verify(jobService).markAsProcessing("j1");
        verify(jobService).markAsCompleted(eq("j1"), any());
    }

    @Test
    void testAsyncMarksFailedWhenFetchFails() {
        when(jobService.isCancellationRequested("j1")).thenReturn(false);
        when(webScrapeService.scrape(any())).thenReturn(ScrapeResult.failure("boom"));

        service.generateOpenApiSpecificationAsync("j1", "https://docs.example.com", null, 1);

        verify(jobService).markAsFailed(eq("j1"), any());
        verify(jobService, never()).markAsCompleted(any(), any());
    }
}
