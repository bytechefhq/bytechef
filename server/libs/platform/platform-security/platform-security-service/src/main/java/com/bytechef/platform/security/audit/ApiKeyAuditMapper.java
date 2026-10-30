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

package com.bytechef.platform.security.audit;

import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ApiKeyAuditMapper implements AuditMapper {

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        Map<String, String> data = new LinkedHashMap<>();
        String event = auditInvocation.event();

        if (event.equals(ApiKeyAuditEvents.API_KEY_DELETED)) {
            data.put("apiKeyId", String.valueOf(auditInvocation.requireArgument("id")));

            return data;
        }

        ApiKey apiKey = auditInvocation.result() instanceof ApiKey savedApiKey
            ? savedApiKey : (ApiKey) auditInvocation.requireArgument("apiKey");

        Long apiKeyId = apiKey.getId();

        if (apiKeyId != null) {
            data.put("apiKeyId", String.valueOf(apiKeyId));
        }

        String name = apiKey.getName();

        if (name != null) {
            data.put("name", name);
        }

        PlatformType type = apiKey.getType();

        if (type != null) {
            data.put("type", type.name());
        }

        return data;
    }
}
