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

package com.bytechef.platform.ai.guardrails;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
public final class McpOutboundRedaction {

    private static final Logger log = LoggerFactory.getLogger(McpOutboundRedaction.class);

    private McpOutboundRedaction() {
    }

    public static String redact(
        String serializedResult, ObjectProvider<McpOutboundRedactorProvider> mcpOutboundRedactorProviderProvider,
        @Nullable Long workspaceId, String surface) {

        McpOutboundRedactor redactor;

        try {
            redactor = fetchRedactor(mcpOutboundRedactorProviderProvider, workspaceId, surface);
        } catch (RuntimeException resolutionException) {
            log.warn(
                "Failed to resolve an outbound MCP redactor on surface {}; refusing the call rather than returning the "
                    + "result unredacted",
                surface, resolutionException);

            throw new McpOutboundRedactionException(
                "Failed to resolve the outbound redaction policy", resolutionException);
        }

        if (redactor == null) {
            return serializedResult;
        }

        try {
            return redactor.redact(serializedResult);
        } catch (RuntimeException redactionException) {
            log.warn(
                "Failed to redact an outbound MCP result on surface {}; refusing the call rather than returning it "
                    + "unredacted",
                surface, redactionException);

            throw new McpOutboundRedactionException("Failed to redact the tool result", redactionException);
        }
    }

    public static @Nullable String redactFailureMessage(
        @Nullable String message, ObjectProvider<McpOutboundRedactorProvider> mcpOutboundRedactorProviderProvider,
        @Nullable Long workspaceId, String surface) {

        if (message == null || message.isBlank()) {
            return null;
        }

        try {
            return redact(message, mcpOutboundRedactorProviderProvider, workspaceId, surface);
        } catch (McpOutboundRedactionException mcpOutboundRedactionException) {
            return null;
        }
    }

    private static @Nullable McpOutboundRedactor fetchRedactor(
        ObjectProvider<McpOutboundRedactorProvider> mcpOutboundRedactorProviderProvider, @Nullable Long workspaceId,
        String surface) {

        McpOutboundRedactorProvider mcpOutboundRedactorProvider = mcpOutboundRedactorProviderProvider.getIfAvailable();

        if (mcpOutboundRedactorProvider == null) {
            return null;
        }

        return mcpOutboundRedactorProvider.fetchRedactor(workspaceId, surface)
            .orElse(null);
    }
}
