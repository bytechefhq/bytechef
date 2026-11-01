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

package com.bytechef.platform.ai.provider.audit;

import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class AiProviderAuditMapper implements AuditMapper {

    static final String UNPARSEABLE_URL = "[unparseable]";

    private static final String URL = "url";

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        if (!(auditInvocation.argument(URL) instanceof String url) || url.isBlank()) {
            return Map.of();
        }

        String redactedUrl = redact(url);

        if (redactedUrl == null) {
            return Map.of(URL, UNPARSEABLE_URL);
        }

        return Map.of(URL, redactedUrl);
    }

    static @Nullable String redact(String url) {
        URI uri;

        try {
            uri = new URI(url.strip());
        } catch (URISyntaxException uriSyntaxException) {
            return null;
        }

        String host = uri.getHost();

        if (uri.getScheme() == null || host == null) {
            return null;
        }

        try {
            URI redactedUri = new URI(uri.getScheme(), null, host, uri.getPort(), uri.getPath(), null, null);

            return redactedUri.toString();
        } catch (URISyntaxException uriSyntaxException) {
            return null;
        }
    }
}
