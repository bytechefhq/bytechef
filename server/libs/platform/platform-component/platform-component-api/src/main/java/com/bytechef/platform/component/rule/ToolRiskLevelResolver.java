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

package com.bytechef.platform.component.rule;

import com.bytechef.component.definition.RiskLevel;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public final class ToolRiskLevelResolver {

    private static final Set<String> CRITICAL_TOKENS = Set.of(
        "delete", "remove", "destroy", "purge", "drop", "truncate", "wipe", "pay", "charge", "refund", "transfer",
        "payout", "withdraw", "revoke");

    private static final Set<String> HIGH_TOKENS = Set.of(
        "send", "post", "publish", "email", "message", "reply", "notify", "invite", "execute", "run", "deploy",
        "cancel", "void", "archive", "approve", "reject", "grant", "share", "submit", "upload", "disable",
        "deactivate");

    private static final Set<String> LOW_TOKENS = Set.of(
        "get", "list", "search", "find", "read", "fetch", "retrieve", "query", "count", "download", "export",
        "check", "lookup", "describe", "exists");

    private static final Pattern TOKEN_BOUNDARY =
        Pattern.compile("[_\\-\\s]+|(?<=[a-z0-9])(?=[A-Z])|(?<=[a-z])(?=[0-9])");

    private ToolRiskLevelResolver() {
    }

    public static RiskLevel resolve(@Nullable RiskLevel declaredRiskLevel, String toolName) {
        if (declaredRiskLevel != null) {
            return declaredRiskLevel;
        }

        Set<String> tokens = Arrays.stream(TOKEN_BOUNDARY.split(toolName))
            .map(token -> token.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());

        if (!Collections.disjoint(tokens, CRITICAL_TOKENS)) {
            return RiskLevel.CRITICAL;
        }

        if (!Collections.disjoint(tokens, HIGH_TOKENS)) {
            return RiskLevel.HIGH;
        }

        if (!Collections.disjoint(tokens, LOW_TOKENS)) {
            return RiskLevel.LOW;
        }

        return RiskLevel.MEDIUM;
    }
}
