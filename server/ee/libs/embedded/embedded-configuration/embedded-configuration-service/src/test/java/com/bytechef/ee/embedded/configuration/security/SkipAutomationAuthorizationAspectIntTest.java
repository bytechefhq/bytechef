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

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.SkipAutomationAuthorization;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = SkipAutomationAuthorizationAspectIntTest.Config.class)
@TestPropertySource(properties = "bytechef.edition=ee")
class SkipAutomationAuthorizationAspectIntTest {

    @Autowired
    private AnnotatedBridge annotatedBridge;

    @Autowired
    private PlainBean plainBean;

    @BeforeEach
    @AfterEach
    void assertCleanState() {
        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testAnnotatedClassEnablesSkipDuringInvocationAndClearsAfter() {
        assertThat(annotatedBridge.captureSkipDuringCall()).isTrue();

        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testNonAnnotatedClassDoesNotEnableSkip() {
        assertThat(plainBean.captureSkipDuringCall()).isFalse();
    }

    @SpringBootConfiguration
    @EnableAspectJAutoProxy
    @Import({
        AnnotatedBridge.class, PlainBean.class, SkipAutomationAuthorizationAspect.class
    })
    static class Config {
    }

    @Service
    @SkipAutomationAuthorization
    static class AnnotatedBridge {

        public boolean captureSkipDuringCall() {
            return AutomationAuthorizationContext.isSkipChecks();
        }
    }

    @Service
    static class PlainBean {

        public boolean captureSkipDuringCall() {
            return AutomationAuthorizationContext.isSkipChecks();
        }
    }
}
