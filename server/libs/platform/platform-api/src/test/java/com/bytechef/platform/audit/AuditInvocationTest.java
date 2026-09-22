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

package com.bytechef.platform.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditInvocationTest {

    @Test
    void testArgumentsKeepNullValuesAndCannotBeModified() {
        Map<String, Object> arguments = new HashMap<>();

        arguments.put("userId", 7L);
        arguments.put("customRoleId", null);

        AuditInvocation auditInvocation = new AuditInvocation(
            "TEST_EVENT", arguments, null, null, AuditOutcome.SUCCESS, null);

        arguments.put("userId", 8L);

        assertThat(auditInvocation.argument("userId")).isEqualTo(7L);
        assertThat(auditInvocation.arguments()).containsKey("customRoleId");
        assertThat(auditInvocation.argument("customRoleId")).isNull();
        assertThatThrownBy(() -> auditInvocation.arguments()
            .put("other", 1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testRequireArgumentReturnsAPresentValue() {
        AuditInvocation auditInvocation = new AuditInvocation(
            "TEST_EVENT", Map.of("userId", 7L), null, null, AuditOutcome.SUCCESS, null);

        assertThat(auditInvocation.requireArgument("userId")).isEqualTo(7L);
    }

    @Test
    void testRequireArgumentFailsForAnUnknownName() {
        AuditInvocation auditInvocation = new AuditInvocation(
            "TEST_EVENT", Map.of("userId", 7L), null, null, AuditOutcome.SUCCESS, null);

        assertThatThrownBy(() -> auditInvocation.requireArgument("memberId"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("memberId")
            .hasMessageContaining("[userId]");
    }

    @Test
    void testRequireArgumentFailsForANullValue() {
        Map<String, Object> arguments = new HashMap<>();

        arguments.put("userId", null);

        AuditInvocation auditInvocation = new AuditInvocation(
            "TEST_EVENT", arguments, null, null, AuditOutcome.SUCCESS, null);

        assertThatThrownBy(() -> auditInvocation.requireArgument("userId"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("userId is null");
    }

    @Test
    void testErrorClassIsRequiredExactlyForTheErrorOutcome() {
        assertThatThrownBy(
            () -> new AuditInvocation("TEST_EVENT", Map.of(), null, null, AuditOutcome.ERROR, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
            () -> new AuditInvocation(
                "TEST_EVENT", Map.of(), null, null, AuditOutcome.DENIED, IllegalStateException.class))
                    .isInstanceOf(IllegalArgumentException.class);
        assertThat(
            new AuditInvocation("TEST_EVENT", Map.of(), null, null, AuditOutcome.ERROR, IllegalStateException.class)
                .errorClass()).isEqualTo(IllegalStateException.class);
    }

    @Test
    void testEventArgumentsAndOutcomeAreRequired() {
        assertThatThrownBy(() -> new AuditInvocation(null, Map.of(), null, null, AuditOutcome.SUCCESS, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("event");
        assertThatThrownBy(() -> new AuditInvocation("TEST_EVENT", null, null, null, AuditOutcome.SUCCESS, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("arguments");
        assertThatThrownBy(() -> new AuditInvocation("TEST_EVENT", Map.of(), null, null, null, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("outcome");
    }

    @Test
    void testCaptureDefaultsToNull() {
        AuditMapper auditMapper = auditInvocation -> Map.of();

        assertThat(auditMapper.capture(
            new AuditInvocation("TEST_EVENT", Map.of(), null, null, AuditOutcome.SUCCESS, null))).isNull();
    }
}
