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

package com.bytechef.component.ai.agent.utils.cluster.network;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.UrlValidationException;
import com.bytechef.commons.util.UrlValidatorUtils;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Wraps a tool that fetches a URL taken from its input and refuses URLs whose host resolves to a private, loopback or
 * link-local address, so an agent cannot use the tool to reach the server's internal network.
 *
 * @author Ivica Cardic
 */
public class PrivateAddressGuardedToolCallback implements ToolCallback {

    private final Set<String> allowedPrivateHosts;
    private final ToolCallback delegate;
    private final String urlArgumentName;

    public PrivateAddressGuardedToolCallback(
        ToolCallback delegate, String urlArgumentName, Set<String> allowedPrivateHosts) {

        this.allowedPrivateHosts = Set.copyOf(allowedPrivateHosts);
        this.delegate = delegate;
        this.urlArgumentName = urlArgumentName;
    }

    @Override
    public String call(String toolInput) {
        String rejection = validate(toolInput);

        if (rejection != null) {
            return rejection;
        }

        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        String rejection = validate(toolInput);

        if (rejection != null) {
            return rejection;
        }

        return delegate.call(toolInput, toolContext);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    private @Nullable String validate(String toolInput) {
        Map<String, ?> arguments = JsonUtils.readMap(toolInput);

        Object url = arguments.get(urlArgumentName);

        if (!(url instanceof String urlString)) {
            return null;
        }

        try {
            UrlValidatorUtils.validate(urlString.trim(), allowedPrivateHosts);

            return null;
        } catch (UrlValidationException urlValidationException) {
            return "Error: " + urlValidationException.getMessage();
        }
    }
}
