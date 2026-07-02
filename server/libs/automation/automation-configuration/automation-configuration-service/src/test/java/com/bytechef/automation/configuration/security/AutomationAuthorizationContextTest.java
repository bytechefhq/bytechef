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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class AutomationAuthorizationContextTest {

    @Test
    void testIsSkipChecksDefaultsFalse() {
        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testCallSkippingChecksEnablesDuringCallAndRestoresAfter() throws Throwable {
        AtomicBoolean insideValue = new AtomicBoolean(false);

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            insideValue.set(AutomationAuthorizationContext.isSkipChecks());

            return null;
        });

        assertThat(insideValue.get()).isTrue();
        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testCallSkippingChecksRestoresOnException() {
        assertThatThrownBy(() -> AutomationAuthorizationContext.callSkippingChecks(() -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testNestedCallsRestoreToOuterValue() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            AutomationAuthorizationContext.callSkippingChecks(() -> null);

            assertThat(AutomationAuthorizationContext.isSkipChecks()).isTrue();

            return null;
        });

        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testSkipDoesNotLeakToNewThread() throws Throwable {
        AtomicBoolean otherThreadValue = new AtomicBoolean(true);

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            Thread thread = new Thread(() -> otherThreadValue.set(AutomationAuthorizationContext.isSkipChecks()));

            thread.start();
            thread.join();

            return null;
        });

        assertThat(otherThreadValue.get()).isFalse();
    }
}
