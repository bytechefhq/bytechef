/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.Audited;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test that boots a minimal Spring context with method security enabled and verifies {@link AuditAspect}
 * actually wraps Spring Security's {@code AuthorizationManagerBeforeMethodInterceptor}. The unit test
 * {@code AuditAspectTest#testAspectIsOrderedHighestPrecedence} only confirms the {@code @Order(HIGHEST_PRECEDENCE)}
 * annotation is present; this test confirms the resulting interceptor chain actually fires this aspect <em>around</em>
 * the security advisor so DENIED events are recorded.
 *
 * <p>
 * If a future Spring Security upgrade reorders the authorization advisor (e.g., to {@code HIGHEST_PRECEDENCE}), this
 * test fails — without it, the regression would surface as silently missing DENIED audit rows in production with no
 * failing unit test.
 *
 * <p>
 * The {@code SingleConnectionPool} tests run the {@link AuditAspect} against a real database with a connection pool of
 * exactly one connection. An audited call made while the caller's transaction still holds that connection must not need
 * a second one: if the audit write asked the pool for another connection it would time out, the aspect would swallow
 * the failure and the audit row would silently be missing.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings(
    value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
    justification = "Guarded calls inside assertThatThrownBy are expected to throw, so their return value is irrelevant")
class AuditAspectIntTest {

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(classes = AuditAspectIntTest.Config.class)
    @Import({
        AuditAspect.class, AuditMapperResolver.class
    })
    class ProxyChain {

        @Autowired
        private GuardedTestService guardedTestService;

        @Autowired
        private AuditEventService auditEventService;

        @BeforeEach
        void setUp() {
            reset(auditEventService);
            SecurityContextHolder.clearContext();
        }

        @Test
        void testAllowedPathRecordsAllowedAuditEventThroughProxyChain() {
            authenticate("alice", "ROLE_ADMIN");

            String result = guardedTestService.adminOnlyMethod();

            assertThat(result).isEqualTo("ok");

            PersistentAuditEvent captured = captureSavedEvent();

            assertThat(captured.getData()).containsEntry("result", "ALLOWED");
            assertThat(captured.getPrincipal()).isEqualTo("alice");
        }

        @Test
        void testDeniedPathRecordsDeniedAuditEventEvenThoughSecurityRejects() {
            authenticate("bob", "ROLE_USER");

            // Without the aspect ordering, Spring Security's authorization advisor would throw BEFORE this aspect's
            // proceed() runs and the DENIED branch would never fire. If this assertion fails, the aspect ordering is
            // wrong — fix the @Order on AuditAspect.
            assertThatThrownBy(() -> guardedTestService.adminOnlyMethod())
                .isInstanceOf(AccessDeniedException.class);

            PersistentAuditEvent captured = captureSavedEvent();

            assertThat(captured.getData()).containsEntry("result", "DENIED");
            assertThat(captured.getPrincipal()).isEqualTo("bob");
        }

        private void authenticate(String principal, String authority) {
            SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                    principal, "n/a", List.of(new SimpleGrantedAuthority(authority))));
        }

        private PersistentAuditEvent captureSavedEvent() {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<PersistentAuditEvent>> captor = ArgumentCaptor.forClass(List.class);

            verify(auditEventService, times(1)).saveAll(captor.capture());

            List<PersistentAuditEvent> savedEvents = captor.getValue();

            assertThat(savedEvents).hasSize(1);

            return savedEvents.getFirst();
        }
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(
        classes = AuditIntTestConfiguration.class,
        properties = {
            "spring.datasource.hikari.maximum-pool-size=1", "spring.datasource.hikari.connection-timeout=1000"
        })
    @ActiveProfiles("testint")
    @Import({
        PostgreSQLContainerConfiguration.class, AuditAspectIntTest.GuardedServiceConfiguration.class
    })
    class SingleConnectionPool {

        @Autowired
        private AuditedCaller auditedCaller;

        @Autowired
        private OuterGuardedService outerGuardedService;

        @Autowired
        private UnauditedCaller unauditedCaller;

        @Autowired
        private PersistenceAuditEventRepository persistenceAuditEventRepository;

        @BeforeEach
        void beforeEach() {
            persistenceAuditEventRepository.deleteAll();

            SecurityContextHolder.clearContext();
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testNestedAuditedCallInsideTransactionRecordsBothEventsWithOneConnection() {
            authenticate("ROLE_ADMIN");

            assertThat(outerGuardedService.callInner()).isEqualTo("inner");

            assertThat(findRecordedMethodResults()).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                    OuterGuardedService.class.getName() + ".callInner", "ALLOWED",
                    InnerGuardedService.class.getName() + ".adminOnly", "ALLOWED"));
        }

        @Test
        void testNestedDeniedCallInsideTransactionIsRecorded() {
            authenticate("ROLE_USER");

            assertThatThrownBy(() -> outerGuardedService.callInnerAsAnyone())
                .isInstanceOf(AccessDeniedException.class);

            assertThat(findRecordedMethodResults()).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                    OuterGuardedService.class.getName() + ".callInnerAsAnyone", "DENIED",
                    InnerGuardedService.class.getName() + ".adminOnly", "DENIED"));
        }

        @Test
        void testAuditedCallInsideUnauditedTransactionIsWrittenOnTheCallersConnection() {
            authenticate("ROLE_ADMIN");

            assertThat(unauditedCaller.callInner()).isEqualTo("inner");

            assertThat(findRecordedMethodResults()).containsExactlyInAnyOrderEntriesOf(
                Map.of(InnerGuardedService.class.getName() + ".adminOnly", "ALLOWED"));
        }

        @Test
        void testAuditedCallInsideUnauditedTransactionThatRollsBackLeavesTheCallerUsable() {
            authenticate("ROLE_ADMIN");

            assertThatThrownBy(() -> unauditedCaller.callInnerThenFail())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("caller failed");

            assertThat(findRecordedMethodResults()).isEmpty();
        }

        @Test
        void testAuditedCallInsideUnauditedReadOnlyTransactionDoesNotBreakTheCaller() {
            authenticate("ROLE_ADMIN");

            assertThat(unauditedCaller.callInnerReadOnly()).isEqualTo("inner");

            assertThat(findRecordedMethodResults()).isEmpty();
        }

        @Test
        void testCaptureReadsInsideNestedAuditedTransactionsUseTheHeldConnection() {
            authenticate("ROLE_ADMIN");

            assertThat(auditedCaller.callAuditedInner()).isEqualTo("inner");

            Page<PersistentAuditEvent> page = persistenceAuditEventRepository
                .findAllFiltered(new AuditEventFilter(null, "CAPTURE_TEST", null, null, null), PageRequest.of(0, 25));

            assertThat(page.getContent())
                .hasSize(2)
                .allSatisfy(persistentAuditEvent -> assertThat(persistentAuditEvent.getData())
                    .containsEntry("result", "SUCCESS")
                    .containsKey("captured")
                    .doesNotContainKey("mapperError"));
        }

        @Test
        void testFailedCaptureReadInsideUnauditedTransactionLeavesTheCallerUsable() {
            authenticate("ROLE_ADMIN");

            assertThat(unauditedCaller.callFailingCaptureThenWrite()).isEqualTo("inner");

            Page<PersistentAuditEvent> page =
                persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

            assertThat(page.getContent())
                .extracting(PersistentAuditEvent::getEventType)
                .containsExactlyInAnyOrder("FAILING_CAPTURE_TEST", "CALLER_WRITE");
            assertThat(page.getContent())
                .filteredOn(persistentAuditEvent -> "FAILING_CAPTURE_TEST".equals(persistentAuditEvent.getEventType()))
                .singleElement()
                .satisfies(persistentAuditEvent -> assertThat(persistentAuditEvent.getData())
                    .containsEntry("result", "SUCCESS")
                    .doesNotContainKey("captured"));
        }

        @Test
        void testFailedAuditInsertInsideUnauditedTransactionLeavesTheCallerUsable() {
            authenticate("ROLE_ADMIN");

            assertThat(unauditedCaller.callWhileAuditInsertFailsThenWrite()).isEqualTo("inner");

            Page<PersistentAuditEvent> page =
                persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

            assertThat(page.getContent())
                .extracting(PersistentAuditEvent::getEventType)
                .containsExactly("CALLER_WRITE");
        }

        @Test
        void testNestedAuditedCallIsRecordedAsRolledBackWhenTheEnclosingTransactionRollsBack() {
            authenticate("ROLE_ADMIN");

            assertThatThrownBy(() -> auditedCaller.callAuditedInnerThenFail())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("caller failed");

            Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
                new AuditEventFilter(null, "CAPTURE_TEST", null, null, null), PageRequest.of(0, 25));

            assertThat(page.getContent())
                .extracting(persistentAuditEvent -> persistentAuditEvent.getData()
                    .get("result"))
                .containsExactlyInAnyOrder("ERROR", "ROLLED_BACK");
        }

        private void authenticate(String authority) {
            SecurityContext securityContext = SecurityContextHolder.getContext();

            securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "n/a",
                    List.of(new SimpleGrantedAuthority(authority))));
        }

        private Map<String, String> findRecordedMethodResults() {
            Page<PersistentAuditEvent> page = persistenceAuditEventRepository
                .findAllFiltered(new AuditEventFilter(null, "PERMISSION_CHECK", null, null, null),
                    PageRequest.of(0, 25));

            return page.getContent()
                .stream()
                .map(PersistentAuditEvent::getData)
                .collect(Collectors.toMap(data -> data.get("method"), data -> data.get("result")));
        }
    }

    // @SpringBootConfiguration (not @TestConfiguration): Spring ignores @TestConfiguration when it is passed
    // directly via @SpringBootTest(classes = ...), producing "Unable to find a @SpringBootConfiguration".
    // @SpringBootConfiguration is a @Configuration specialization that Spring accepts as the primary source.
    @SpringBootConfiguration
    @EnableAspectJAutoProxy
    @EnableMethodSecurity
    static class Config {

        @Bean
        AuditEventService auditEventService() {
            return mock(AuditEventService.class);
        }

        @Bean
        GuardedTestService guardedTestService() {
            return new GuardedTestService();
        }
    }

    @Service
    static class GuardedTestService {

        @PreAuthorize("hasRole('ADMIN')")
        public String adminOnlyMethod() {
            return "ok";
        }
    }

    @Configuration
    @EnableMethodSecurity
    static class GuardedServiceConfiguration {

        @Bean
        InnerGuardedService innerGuardedService() {
            return new InnerGuardedService();
        }

        @Bean
        OuterGuardedService outerGuardedService(InnerGuardedService innerGuardedService) {
            return new OuterGuardedService(innerGuardedService);
        }

        @Bean
        UnauditedCaller unauditedCaller(
            InnerGuardedService innerGuardedService, JdbcTemplate jdbcTemplate,
            PersistenceAuditEventRepository persistenceAuditEventRepository) {

            return new UnauditedCaller(innerGuardedService, jdbcTemplate, persistenceAuditEventRepository);
        }

        @Bean
        FailingCaptureAuditMapper failingCaptureAuditMapper(JdbcTemplate jdbcTemplate) {
            return new FailingCaptureAuditMapper(jdbcTemplate);
        }

        @Bean
        CountingAuditMapper countingAuditMapper(PersistenceAuditEventRepository persistenceAuditEventRepository) {
            return new CountingAuditMapper(persistenceAuditEventRepository);
        }

        @Bean
        AuditedCaller auditedCaller(InnerGuardedService innerGuardedService) {
            return new AuditedCaller(innerGuardedService);
        }
    }

    static class InnerGuardedService {

        @PreAuthorize("hasRole('ADMIN')")
        @Transactional
        public String adminOnly() {
            return "inner";
        }

        @Audited(event = "CAPTURE_TEST", mapper = CountingAuditMapper.class)
        @PreAuthorize("hasRole('ADMIN')")
        @Transactional
        public String auditedAdminOnly() {
            return "inner";
        }

        @Audited(event = "FAILING_CAPTURE_TEST", mapper = FailingCaptureAuditMapper.class)
        @PreAuthorize("hasRole('ADMIN')")
        @Transactional
        public String failingCaptureAdminOnly() {
            return "inner";
        }

        @Audited(event = "REJECTED_BY_THE_DATABASE", mapper = CountingAuditMapper.class)
        @PreAuthorize("hasRole('ADMIN')")
        @Transactional
        public String rejectedAuditAdminOnly() {
            return "inner";
        }
    }

    static class OuterGuardedService {

        private final InnerGuardedService innerGuardedService;

        OuterGuardedService(InnerGuardedService innerGuardedService) {
            this.innerGuardedService = innerGuardedService;
        }

        @PreAuthorize("hasRole('ADMIN')")
        @Transactional
        public String callInner() {
            return innerGuardedService.adminOnly();
        }

        @PreAuthorize("isAuthenticated()")
        @Transactional
        public String callInnerAsAnyone() {
            return innerGuardedService.adminOnly();
        }
    }

    static class UnauditedCaller {

        private final InnerGuardedService innerGuardedService;
        private final JdbcTemplate jdbcTemplate;
        private final PersistenceAuditEventRepository persistenceAuditEventRepository;

        UnauditedCaller(
            InnerGuardedService innerGuardedService, JdbcTemplate jdbcTemplate,
            PersistenceAuditEventRepository persistenceAuditEventRepository) {

            this.innerGuardedService = innerGuardedService;
            this.jdbcTemplate = jdbcTemplate;
            this.persistenceAuditEventRepository = persistenceAuditEventRepository;
        }

        @Transactional
        public String callFailingCaptureThenWrite() {
            String result = innerGuardedService.failingCaptureAdminOnly();

            writeCallerEvent();

            return result;
        }

        @Transactional
        public String callWhileAuditInsertFailsThenWrite() {
            jdbcTemplate.execute(
                "ALTER TABLE persistent_audit_event ADD CONSTRAINT reject_audit_event "
                    + "CHECK (event_type <> 'REJECTED_BY_THE_DATABASE')");

            String result = innerGuardedService.rejectedAuditAdminOnly();

            jdbcTemplate.execute("ALTER TABLE persistent_audit_event DROP CONSTRAINT reject_audit_event");

            writeCallerEvent();

            return result;
        }

        private void writeCallerEvent() {
            PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

            persistentAuditEvent.setEventDate(LocalDateTime.now());
            persistentAuditEvent.setEventType("CALLER_WRITE");
            persistentAuditEvent.setPrincipal("alice");

            persistenceAuditEventRepository.save(persistentAuditEvent);
        }

        @Transactional
        public String callInner() {
            return innerGuardedService.adminOnly();
        }

        @Transactional
        public String callInnerThenFail() {
            innerGuardedService.adminOnly();

            throw new IllegalStateException("caller failed");
        }

        @Transactional(readOnly = true)
        public String callInnerReadOnly() {
            return innerGuardedService.adminOnly();
        }
    }

    static class AuditedCaller {

        private final InnerGuardedService innerGuardedService;

        AuditedCaller(InnerGuardedService innerGuardedService) {
            this.innerGuardedService = innerGuardedService;
        }

        @Audited(event = "CAPTURE_TEST", mapper = CountingAuditMapper.class)
        @PreAuthorize("hasRole('ADMIN')")
        @Transactional
        public String callAuditedInner() {
            return innerGuardedService.auditedAdminOnly();
        }

        @Audited(event = "CAPTURE_TEST", mapper = CountingAuditMapper.class)
        @PreAuthorize("hasRole('ADMIN')")
        @Transactional
        public String callAuditedInnerThenFail() {
            innerGuardedService.auditedAdminOnly();

            throw new IllegalStateException("caller failed");
        }
    }

    static class CountingAuditMapper implements AuditMapper {

        private final PersistenceAuditEventRepository persistenceAuditEventRepository;

        CountingAuditMapper(PersistenceAuditEventRepository persistenceAuditEventRepository) {
            this.persistenceAuditEventRepository = persistenceAuditEventRepository;
        }

        @Override
        public Object capture(AuditInvocation auditInvocation) {
            return persistenceAuditEventRepository.count();
        }

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            return auditInvocation.captured() == null
                ? Map.of() : Map.of("captured", String.valueOf(auditInvocation.captured()));
        }
    }

    static class FailingCaptureAuditMapper implements AuditMapper {

        private final JdbcTemplate jdbcTemplate;

        FailingCaptureAuditMapper(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public Object capture(AuditInvocation auditInvocation) {
            return jdbcTemplate.queryForObject("SELECT 1 / 0", Integer.class);
        }

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            return auditInvocation.captured() == null
                ? Map.of() : Map.of("captured", String.valueOf(auditInvocation.captured()));
        }
    }
}
