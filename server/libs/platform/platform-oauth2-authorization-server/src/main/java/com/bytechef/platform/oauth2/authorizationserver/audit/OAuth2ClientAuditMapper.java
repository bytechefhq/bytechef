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

package com.bytechef.platform.oauth2.authorizationserver.audit;

import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.AuditOutcome;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class OAuth2ClientAuditMapper implements AuditMapper {

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        return Map.of();
    }

    @Override
    public boolean shouldRecord(AuditInvocation auditInvocation) {
        if (auditInvocation.outcome() != AuditOutcome.SUCCESS) {
            return true;
        }

        return !Boolean.FALSE.equals(auditInvocation.result());
    }
}
