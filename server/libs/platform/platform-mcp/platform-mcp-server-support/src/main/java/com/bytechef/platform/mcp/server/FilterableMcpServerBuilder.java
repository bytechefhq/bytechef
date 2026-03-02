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

package com.bytechef.platform.mcp.server;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
import io.modelcontextprotocol.util.DefaultMcpUriTemplateManagerFactory;
import io.modelcontextprotocol.util.McpUriTemplateManagerFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * Builder for creating {@link FilterableMcpAsyncServer} instances with tool filtering support.
 *
 * @author Ivica Cardic
 */
public class FilterableMcpServerBuilder {

    private final McpStreamableServerTransportProvider transportProvider;

    private McpSchema.Implementation serverInfo = new McpSchema.Implementation("mcp-server", "1.0.0");

    private McpSchema.ServerCapabilities serverCapabilities;

    private List<McpServerFeatures.AsyncToolSpecification> tools = new ArrayList<>();

    private Map<String, McpServerFeatures.AsyncResourceSpecification> resources = new HashMap<>();

    private Map<String, McpServerFeatures.AsyncResourceTemplateSpecification> resourceTemplates =
        new HashMap<>();

    private Map<String, McpServerFeatures.AsyncPromptSpecification> prompts = new HashMap<>();

    private Map<McpSchema.CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions =
        new HashMap<>();

    private List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers =
        new ArrayList<>();

    private String instructions;

    private Duration requestTimeout = Duration.ofSeconds(10);

    private McpUriTemplateManagerFactory uriTemplateManagerFactory = new DefaultMcpUriTemplateManagerFactory();

    private Function<McpAsyncServerExchange, List<McpServerFeatures.AsyncToolSpecification>> toolFilter;

    @SuppressFBWarnings("EI")
    public FilterableMcpServerBuilder(McpStreamableServerTransportProvider transportProvider) {
        this.transportProvider = transportProvider;
    }

    public FilterableMcpServerBuilder serverInfo(String name, String version) {
        this.serverInfo = new McpSchema.Implementation(name, version);

        return this;
    }

    public FilterableMcpServerBuilder capabilities(McpSchema.ServerCapabilities capabilities) {
        this.serverCapabilities = capabilities;

        return this;
    }

    public FilterableMcpServerBuilder tools(List<McpServerFeatures.AsyncToolSpecification> tools) {
        this.tools.addAll(tools);

        return this;
    }

    public FilterableMcpServerBuilder resources(
        Map<String, McpServerFeatures.AsyncResourceSpecification> resources) {

        this.resources.putAll(resources);

        return this;
    }

    public FilterableMcpServerBuilder resourceTemplates(
        Map<String, McpServerFeatures.AsyncResourceTemplateSpecification> resourceTemplates) {

        this.resourceTemplates.putAll(resourceTemplates);

        return this;
    }

    public FilterableMcpServerBuilder prompts(
        Map<String, McpServerFeatures.AsyncPromptSpecification> prompts) {

        this.prompts.putAll(prompts);

        return this;
    }

    public FilterableMcpServerBuilder completions(
        Map<McpSchema.CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions) {

        this.completions.putAll(completions);

        return this;
    }

    public FilterableMcpServerBuilder rootsChangeConsumers(
        List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers) {

        this.rootsChangeConsumers.addAll(rootsChangeConsumers);

        return this;
    }

    public FilterableMcpServerBuilder instructions(String instructions) {
        this.instructions = instructions;

        return this;
    }

    public FilterableMcpServerBuilder requestTimeout(Duration timeout) {
        this.requestTimeout = timeout;

        return this;
    }

    public FilterableMcpServerBuilder uriTemplateManagerFactory(
        McpUriTemplateManagerFactory uriTemplateManagerFactory) {

        this.uriTemplateManagerFactory = uriTemplateManagerFactory;

        return this;
    }

    public FilterableMcpServerBuilder toolFilter(
        Function<McpAsyncServerExchange, List<McpServerFeatures.AsyncToolSpecification>> toolFilter) {

        this.toolFilter = toolFilter;

        return this;
    }

    public FilterableMcpAsyncServer build() {
        return new FilterableMcpAsyncServer(
            transportProvider,
            McpJsonMapper.getDefault(),
            serverInfo,
            serverCapabilities,
            instructions,
            tools,
            resources,
            resourceTemplates,
            prompts,
            completions,
            rootsChangeConsumers,
            requestTimeout,
            uriTemplateManagerFactory,
            JsonSchemaValidator.getDefault(),
            toolFilter);
    }
}
