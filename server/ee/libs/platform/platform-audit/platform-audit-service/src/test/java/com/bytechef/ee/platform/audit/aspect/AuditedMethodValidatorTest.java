/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.Audited;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AuditedMethodValidatorTest {

    @Test
    void testValidWiringPasses() {
        try (GenericApplicationContext genericApplicationContext = new GenericApplicationContext()) {
            genericApplicationContext.registerBean(ValidService.class);
            genericApplicationContext.registerBean(FirstAuditMapper.class);
            genericApplicationContext.refresh();

            assertThatCode(() -> validate(genericApplicationContext)).doesNotThrowAnyException();
        }
    }

    @Test
    void testMissingMapperBeanFails() {
        try (GenericApplicationContext genericApplicationContext = new GenericApplicationContext()) {
            genericApplicationContext.registerBean(ValidService.class);
            genericApplicationContext.refresh();

            assertThatThrownBy(() -> validate(genericApplicationContext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ValidService.write")
                .hasMessageContaining("FirstAuditMapper")
                .hasMessageContaining("0 beans");
        }
    }

    @Test
    void testAmbiguousMapperBeanFails() {
        try (GenericApplicationContext genericApplicationContext = new GenericApplicationContext()) {
            genericApplicationContext.registerBean(ValidService.class);
            genericApplicationContext.registerBean("firstAuditMapper", FirstAuditMapper.class);
            genericApplicationContext.registerBean("secondAuditMapper", FirstAuditMapper.class);
            genericApplicationContext.refresh();

            assertThatThrownBy(() -> validate(genericApplicationContext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2 beans");
        }
    }

    @Test
    void testPrivateMethodFails() {
        try (GenericApplicationContext genericApplicationContext = new GenericApplicationContext()) {
            genericApplicationContext.registerBean(PrivateMethodService.class);
            genericApplicationContext.registerBean(FirstAuditMapper.class);
            genericApplicationContext.refresh();

            assertThatThrownBy(() -> validate(genericApplicationContext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PrivateMethodService.write")
                .hasMessageContaining("private");
        }
    }

    @Test
    void testFinalMethodFails() {
        try (GenericApplicationContext genericApplicationContext = new GenericApplicationContext()) {
            genericApplicationContext.registerBean(FinalMethodService.class);
            genericApplicationContext.registerBean(FirstAuditMapper.class);
            genericApplicationContext.refresh();

            assertThatThrownBy(() -> validate(genericApplicationContext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FinalMethodService.write")
                .hasMessageContaining("final");
        }
    }

    @Test
    void testStaticMethodFails() {
        try (GenericApplicationContext genericApplicationContext = new GenericApplicationContext()) {
            genericApplicationContext.registerBean(StaticMethodService.class);
            genericApplicationContext.registerBean(FirstAuditMapper.class);
            genericApplicationContext.refresh();

            assertThatThrownBy(() -> validate(genericApplicationContext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("StaticMethodService.write")
                .hasMessageContaining("static");
        }
    }

    @Test
    void testBlankEventFails() {
        try (GenericApplicationContext genericApplicationContext = new GenericApplicationContext()) {
            genericApplicationContext.registerBean(BlankEventService.class);
            genericApplicationContext.registerBean(FirstAuditMapper.class);
            genericApplicationContext.refresh();

            assertThatThrownBy(() -> validate(genericApplicationContext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BlankEventService.write")
                .hasMessageContaining("blank event");
        }
    }

    @Test
    void testOverLongEventFails() {
        try (GenericApplicationContext genericApplicationContext = new GenericApplicationContext()) {
            genericApplicationContext.registerBean(OverLongEventService.class);
            genericApplicationContext.registerBean(FirstAuditMapper.class);
            genericApplicationContext.refresh();

            assertThatThrownBy(() -> validate(genericApplicationContext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OverLongEventService.write")
                .hasMessageContaining("longer than 256");
        }
    }

    @Test
    void testAuditedInterfaceMethodFails() {
        try (GenericApplicationContext genericApplicationContext = new GenericApplicationContext()) {
            genericApplicationContext.registerBean(InterfaceAuditedService.class);
            genericApplicationContext.registerBean(FirstAuditMapper.class);
            genericApplicationContext.refresh();

            assertThatThrownBy(() -> validate(genericApplicationContext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AuditedOperations.write")
                .hasMessageContaining("interface");
        }
    }

    @Test
    void testEventTheMapperDoesNotHandleFails() {
        try (GenericApplicationContext genericApplicationContext = new GenericApplicationContext()) {
            genericApplicationContext.registerBean(UnhandledEventService.class);
            genericApplicationContext.registerBean(OtherEventsAuditMapper.class);
            genericApplicationContext.refresh();

            assertThatThrownBy(() -> validate(genericApplicationContext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UnhandledEventService.write")
                .hasMessageContaining("does not handle");
        }
    }

    @Test
    void testEventTheMapperHandlesPasses() {
        try (GenericApplicationContext genericApplicationContext = new GenericApplicationContext()) {
            genericApplicationContext.registerBean(HandledEventService.class);
            genericApplicationContext.registerBean(OtherEventsAuditMapper.class);
            genericApplicationContext.refresh();

            assertThatCode(() -> validate(genericApplicationContext)).doesNotThrowAnyException();
        }
    }

    private static void validate(GenericApplicationContext genericApplicationContext) {
        new AuditedMethodValidator(genericApplicationContext.getBeanFactory()).afterSingletonsInstantiated();
    }

    static class ValidService {

        @Audited(event = "TEST_EVENT", mapper = FirstAuditMapper.class)
        public void write() {
            // Scanned for its annotation only.
        }
    }

    static class BlankEventService {

        @Audited(event = " ", mapper = FirstAuditMapper.class)
        public void write() {
            // Scanned for its annotation only.
        }
    }

    static class OverLongEventService {

        @Audited(
            event = "EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE"
                + "EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE"
                + "EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE",
            mapper = FirstAuditMapper.class)
        public void write() {
            // Scanned for its annotation only.
        }
    }

    interface AuditedOperations {

        @Audited(event = "TEST_EVENT", mapper = FirstAuditMapper.class)
        void write();
    }

    static class InterfaceAuditedService implements AuditedOperations {

        @Override
        public void write() {
            // Scanned for its interface's annotation only.
        }
    }

    static class UnhandledEventService {

        @Audited(event = "TEST_EVENT", mapper = OtherEventsAuditMapper.class)
        public void write() {
            // Scanned for its annotation only.
        }
    }

    static class HandledEventService {

        @Audited(event = "OTHER_EVENT", mapper = OtherEventsAuditMapper.class)
        public void write() {
            // Scanned for its annotation only.
        }
    }

    static class OtherEventsAuditMapper implements AuditMapper {

        @Override
        public Set<String> events() {
            return Set.of("OTHER_EVENT");
        }

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            return Map.of();
        }
    }

    static class PrivateMethodService {

        @Audited(event = "TEST_EVENT", mapper = FirstAuditMapper.class)
        @SuppressWarnings("PMD.UnusedPrivateMethod")
        private void write() {
            // Scanned for its annotation only, via reflection.
        }
    }

    static class FinalMethodService {

        @Audited(event = "TEST_EVENT", mapper = FirstAuditMapper.class)
        public final void write() {
            // Scanned for its annotation only.
        }
    }

    static class StaticMethodService {

        @Audited(event = "TEST_EVENT", mapper = FirstAuditMapper.class)
        public static void write() {
            // Scanned for its annotation only.
        }
    }

    static class FirstAuditMapper implements AuditMapper {

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            return Map.of();
        }
    }
}
