/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AuditIntTestConfiguration.class,
    properties = {
        "spring.datasource.hikari.maximum-pool-size=1", "spring.datasource.hikari.connection-timeout=1000"
    })
@ActiveProfiles("testint")
@Import({
    PostgreSQLContainerConfiguration.class, AuditAspectIntTest.GuardedServiceConfiguration.class
})
@SuppressFBWarnings(
    value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
    justification = "Guarded calls inside assertThatThrownBy are expected to throw, so their return value is irrelevant")
class AuditAspectIntTest {

    @Autowired
    private AuditAspect auditAspect;

    @MockitoSpyBean
    private AuditEventService auditEventService;

    @Autowired
    private AuditedBusinessService auditedBusinessService;

    @Autowired
    private AuditedCaller auditedCaller;

    @Autowired
    private AuditedTestService auditedTestService;

    @Autowired
    private ClassGuardedTestService classGuardedTestService;

    @Autowired
    private GuardedTestService guardedTestService;

    @Autowired
    private InnerGuardedService innerGuardedService;

    @Autowired
    private OuterGuardedService outerGuardedService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private UnauditedCaller unauditedCaller;

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
    void testAllowedPathRecordsAllowedAuditEventThroughProxyChain() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(guardedTestService.adminOnlyMethod()).isEqualTo("ok");

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo("PERMISSION_CHECK");
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("alice");
        assertThat(persistentAuditEvent.getData())
            .containsOnlyKeys("method", "result")
            .containsEntry("result", "ALLOWED");
    }

    @Test
    void testDeniedPathRecordsDeniedAuditEventEvenThoughSecurityRejects() {
        authenticate("bob", "ROLE_USER");

        assertThatThrownBy(() -> guardedTestService.adminOnlyMethod())
            .isInstanceOf(AccessDeniedException.class);

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getData()).containsEntry("result", "DENIED");
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("bob");
    }

    @Test
    void testClassLevelPreAuthorizeRecordsAllowedAuditEvent() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(classGuardedTestService.updateSetting()).isEqualTo("ok");

        assertThat(findSingle().getData()).containsEntry("result", "ALLOWED");
    }

    @Test
    void testClassLevelPreAuthorizeRecordsDeniedAuditEvent() {
        authenticate("bob", "ROLE_USER");

        assertThatThrownBy(() -> classGuardedTestService.updateSetting())
            .isInstanceOf(AccessDeniedException.class);

        assertThat(findSingle().getData()).containsEntry("result", "DENIED");
    }

    @Test
    void testAccessDeniedThrownByTheMethodIsRecordedAsDeniedAndPropagates() {
        authenticate("alice", "ROLE_USER");

        AccessDeniedException accessDeniedException = new AccessDeniedException("denied");

        assertThatThrownBy(() -> guardedTestService.rethrow(accessDeniedException)).isSameAs(accessDeniedException);

        assertThat(findSingle().getData()).containsEntry("result", "DENIED");
    }

    @Test
    void testUnexpectedExceptionIsRecordedAsErrorAndPropagates() {
        authenticate("alice", "ROLE_USER");

        RuntimeException runtimeException = new RuntimeException("boom");

        assertThatThrownBy(() -> guardedTestService.rethrow(runtimeException)).isSameAs(runtimeException);

        assertThat(findSingle().getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", RuntimeException.class.getName());
    }

    @Test
    void testAspectIsOrderedHighestPrecedence() {
        Class<? extends AuditAspect> auditAspectClass = auditAspect.getClass();

        assertThat(OrderUtils.getOrder(auditAspectClass)).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    void testSuccessfulReadIsNotRecorded() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(guardedTestService.getSomething()).isEqualTo("ok");

        assertThat(findAll()).isEmpty();
    }

    @Test
    void testSuccessfulMutationIsRecorded() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(guardedTestService.createSomething()).isEqualTo("ok");

        assertThat(findSingle().getData()).containsEntry(
            "method", GuardedTestService.class.getName() + ".createSomething");
    }

    @Test
    void testDeniedReadIsRecorded() {
        authenticate("bob", "ROLE_USER");

        assertThatThrownBy(() -> guardedTestService.getSomething()).isInstanceOf(AccessDeniedException.class);

        assertThat(findSingle().getData()).containsEntry("result", "DENIED");
    }

    @Test
    void testFailedReadIsRecorded() {
        authenticate("alice", "ROLE_ADMIN");

        assertThatThrownBy(() -> guardedTestService.getSomethingThatFails())
            .isInstanceOf(IllegalStateException.class);

        assertThat(findSingle().getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalStateException.class.getName());
    }

    @Test
    void testUnrecognizedMethodNameIsRecorded() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(guardedTestService.doSomething()).isEqualTo("ok");

        assertThat(findSingle().getData()).containsEntry("result", "ALLOWED");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "get", "getSomething", "isAdmin", "hasPermission", "listProjects", "existsById", "countEvents",
        "fetchEventTypes", "findAll", "loadWorkflow", "readFile", "searchProjects"
    })
    void testReadStyleMethodNameIsRecognized(String methodName) {
        assertThat(AuditAspect.isReadMethod(methodName)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "getaway", "listen", "listenForEvents", "isolate", "issueToken", "hashPassword", "readmit", "counter",
        "createSomething", "doSomething"
    })
    void testMutationStyleMethodNameIsNotRecognizedAsARead(String methodName) {
        assertThat(AuditAspect.isReadMethod(methodName)).isFalse();
    }

    @Test
    void testPermissionCheckWithoutASecurityContextIsStoredUnderTheMissingContextPrincipal() {
        assertThat(guardedTestService.updateAnonymously()).isEqualTo("ok");

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("__NO_SECURITY_CONTEXT__");
        assertThat(persistentAuditEvent.getData()).containsEntry("result", "ALLOWED");
    }

    @Test
    void testTokenPrincipalWithoutUserDetailsIsStoredByItsName() {
        Principal servicePrincipal = () -> "service-account";

        authenticate(servicePrincipal, "ROLE_ADMIN");

        assertThat(guardedTestService.createSomething()).isEqualTo("ok");

        assertThat(findSingle().getPrincipal()).isEqualTo("service-account");
    }

    @Test
    void testOverLongPrincipalIsTruncatedToFitItsColumn() {
        authenticate("p".repeat(299) + "END", "ROLE_ADMIN");

        assertThat(guardedTestService.createSomething()).isEqualTo("ok");

        assertThat(findSingle().getPrincipal()).isEqualTo("p".repeat(253) + "END");
    }

    @Test
    void testAuditedMethodWritesOneDomainEventWithMapperData() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(auditedTestService.updateUser(42L)).isEqualTo("ok");

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo("TEST_EVENT");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("userId", "42")
            .containsEntry("resultValue", "ok")
            .containsEntry("result", "SUCCESS")
            .containsEntry("method", AuditedTestService.class.getName() + ".updateUser")
            .doesNotContainKey("mapperError");
    }

    @Test
    void testDeniedAuditedMethodIsStoredWithoutTheResult() {
        authenticate("bob", "ROLE_USER");

        assertThatThrownBy(() -> auditedTestService.updateUser(42L)).isInstanceOf(AccessDeniedException.class);

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo("TEST_EVENT");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "DENIED")
            .containsEntry("userId", "42")
            .doesNotContainKey("resultValue");
    }

    @Test
    void testThrowingMapperStillWritesTheRow() {
        assertThat(auditedTestService.updateUserWithThrowingMapper(42L)).isEqualTo("ok");

        assertThat(findSingle().getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mapperError", IllegalArgumentException.class.getName());
    }

    @Test
    void testMapperLinkageErrorStillWritesTheRowAndKeepsTheBusinessResult() {
        assertThat(auditedTestService.updateUserWithLinkageErrorMapper(42L)).isEqualTo("ok");

        assertThat(findSingle().getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mapperError", NoClassDefFoundError.class.getName());
    }

    @Test
    void testNullMapperDataIsStoredAsMapperError() {
        assertThat(auditedTestService.updateUserWithNullDataMapper(42L)).isEqualTo("ok");

        assertThat(findSingle().getData()).containsEntry("mapperError", "NULL_DATA");
    }

    @Test
    void testMapperCannotOverwriteReservedKeysAndLongKeysAndValuesAreTruncated() {
        assertThat(auditedTestService.updateUserWithForgingMapper(42L)).isEqualTo("ok");

        Map<String, String> data = findSingle().getData();

        assertThat(data)
            .containsEntry("result", "SUCCESS")
            .containsEntry("method", AuditedTestService.class.getName() + ".updateUserWithForgingMapper")
            .containsEntry("k".repeat(256), "v".repeat(253) + "END")
            .doesNotContainKey("errorClass")
            .doesNotContainKey("mapperError");
    }

    @Test
    void testShouldRecordFalseWritesNothingAndStillCleansUpTheFrameAndBuffer() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(auditedTestService.updateUserWithSkippingMapper(42L)).isEqualTo("ok");

        assertThat(findAll()).isEmpty();
        assertThat(AuditFrame.current()).isNull();

        assertThat(auditedTestService.updateUser(42L)).isEqualTo("ok");

        assertThat(findSingle().getData()).containsEntry("userId", "42");
    }

    @Test
    void testThrowingShouldRecordStillWritesARowWithMapperError() {
        assertThat(auditedTestService.updateUserWithThrowingShouldRecordMapper(42L)).isEqualTo("ok");

        assertThat(findSingle().getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mapperError", IllegalStateException.class.getName());
    }

    @Test
    void testAuditedCallWithoutASecurityContextIsStoredUnderTheSystemPrincipal() {
        assertThat(auditedBusinessService.write()).isEqualTo("written");

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            new AuditEventFilter("BUSINESS_TEST", null, null, null, true, null), PageRequest.of(0, 25));

        assertThat(page.getContent())
            .singleElement()
            .satisfies(persistentAuditEvent -> {
                assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("SYSTEM");
                assertThat(persistentAuditEvent.getData())
                    .containsEntry("result", "SUCCESS")
                    .containsEntry("method", AuditedBusinessService.class.getName() + ".write");
            });
    }

    @Test
    void testFailedAuditedCallIsStoredAsAnErrorAfterItsOwnTransactionRollsBack() {
        authenticate("alice", "ROLE_ADMIN");

        assertThatThrownBy(() -> auditedBusinessService.writeThenFail())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("business failed");

        assertThat(findAll())
            .singleElement()
            .satisfies(persistentAuditEvent -> {
                assertThat(persistentAuditEvent.getEventType()).isEqualTo("BUSINESS_TEST");
                assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("alice");
                assertThat(persistentAuditEvent.getData())
                    .containsEntry("result", "ERROR")
                    .containsEntry("errorClass", IllegalStateException.class.getName())
                    .containsEntry("method", AuditedBusinessService.class.getName() + ".writeThenFail");
            });
    }

    @Test
    void testNestedAuditedCallInsideTransactionRecordsBothEventsWithOneConnection() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(outerGuardedService.callInner()).isEqualTo("inner");

        assertThat(findRecordedMethodResults()).containsExactlyInAnyOrderEntriesOf(
            Map.of(
                OuterGuardedService.class.getName() + ".callInner", "ALLOWED",
                InnerGuardedService.class.getName() + ".adminOnly", "ALLOWED"));
    }

    @Test
    void testNestedCallInsideTransactionIsBufferedUntilTheTransactionIsGone() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(outerGuardedService.callInnerThenCountAuditEvents()).isZero();

        assertThat(findRecordedMethodResults()).containsExactlyInAnyOrderEntriesOf(
            Map.of(
                OuterGuardedService.class.getName() + ".callInnerThenCountAuditEvents", "ALLOWED",
                InnerGuardedService.class.getName() + ".adminOnly", "ALLOWED"));
    }

    @Test
    void testNestedDeniedCallInsideTransactionIsRecorded() {
        authenticate("alice", "ROLE_USER");

        assertThatThrownBy(() -> outerGuardedService.callInnerAsAnyone())
            .isInstanceOf(AccessDeniedException.class);

        assertThat(findRecordedMethodResults()).containsExactlyInAnyOrderEntriesOf(
            Map.of(
                OuterGuardedService.class.getName() + ".callInnerAsAnyone", "DENIED",
                InnerGuardedService.class.getName() + ".adminOnly", "DENIED"));
    }

    @Test
    void testAuditedCallInsideUnauditedTransactionIsWrittenOnTheCallersConnection() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(unauditedCaller.callInner()).isEqualTo("inner");

        assertThat(findRecordedMethodResults()).containsExactlyInAnyOrderEntriesOf(
            Map.of(InnerGuardedService.class.getName() + ".adminOnly", "ALLOWED"));
    }

    @Test
    void testAuditedCallInsideUnauditedTransactionThatRollsBackLeavesTheCallerUsable() {
        authenticate("alice", "ROLE_ADMIN");

        assertThatThrownBy(() -> unauditedCaller.callInnerThenFail())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("caller failed");

        assertThat(findAll()).isEmpty();
    }

    @Test
    void testAuditedCallInsideUnauditedReadOnlyTransactionDoesNotBreakTheCaller() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(unauditedCaller.callInnerReadOnly()).isEqualTo("inner");

        assertThat(findAll()).isEmpty();
    }

    @Test
    void testCaptureReadsInsideNestedAuditedTransactionsUseTheHeldConnection() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(auditedCaller.callAuditedInner()).isEqualTo("inner");

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            new AuditEventFilter("CAPTURE_TEST", null, null, null, true, null), PageRequest.of(0, 25));

        assertThat(page.getContent())
            .hasSize(2)
            .allSatisfy(persistentAuditEvent -> assertThat(persistentAuditEvent.getData())
                .containsEntry("result", "SUCCESS")
                .containsKey("captured")
                .doesNotContainKey("mapperError"));
    }

    @Test
    void testFailedCaptureReadInsideUnauditedTransactionLeavesTheCallerUsable() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(unauditedCaller.callFailingCaptureThenWrite()).isEqualTo("inner");

        List<PersistentAuditEvent> persistentAuditEvents = findAll();

        assertThat(persistentAuditEvents)
            .extracting(PersistentAuditEvent::getEventType)
            .containsExactlyInAnyOrder("FAILING_CAPTURE_TEST", "CALLER_WRITE");
        assertThat(persistentAuditEvents)
            .filteredOn(persistentAuditEvent -> "FAILING_CAPTURE_TEST".equals(persistentAuditEvent.getEventType()))
            .singleElement()
            .satisfies(persistentAuditEvent -> assertThat(persistentAuditEvent.getData())
                .containsEntry("result", "SUCCESS")
                .doesNotContainKey("captured"));
    }

    @Test
    void testFailedAuditInsertInsideUnauditedTransactionLeavesTheCallerUsable() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(unauditedCaller.callWhileAuditInsertFailsThenWrite()).isEqualTo("inner");

        assertThat(findAll())
            .extracting(PersistentAuditEvent::getEventType)
            .containsExactly("CALLER_WRITE");
    }

    @Test
    void testNestedAuditedCallIsRecordedAsRolledBackWhenTheEnclosingTransactionRollsBack() {
        authenticate("alice", "ROLE_ADMIN");

        assertThatThrownBy(() -> auditedCaller.callAuditedInnerThenFail())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("caller failed");

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            new AuditEventFilter("CAPTURE_TEST", null, null, null, true, null), PageRequest.of(0, 25));

        assertThat(page.getContent())
            .extracting(PersistentAuditEvent::getData)
            .extracting(data -> data.get("result"))
            .containsExactlyInAnyOrder("ERROR", "ROLLED_BACK");
    }

    @Test
    void testAuditPersistenceFailureDoesNotMaskTheBusinessResult() {
        doThrow(new IllegalStateException("audit backend down"))
            .when(auditEventService)
            .saveAll(any());

        authenticate("alice", "ROLE_ADMIN");

        assertThat(guardedTestService.createSomething()).isEqualTo("ok");

        verify(auditEventService, times(1)).saveAll(any());
        assertThat(findAll()).isEmpty();
    }

    @Test
    void testStackOverflowWhileWritingDoesNotReplaceTheBusinessException() {
        doThrow(new StackOverflowError())
            .when(auditEventService)
            .saveAll(any());

        authenticate("alice", "ROLE_USER");

        IllegalStateException businessException = new IllegalStateException("business failure");

        assertThatThrownBy(() -> guardedTestService.rethrow(businessException)).isSameAs(businessException);

        assertThat(findAll()).isEmpty();
    }

    @Test
    void testAuditEventConstructionFailureDoesNotMaskTheBusinessResult() throws Throwable {
        MethodSignature methodSignature = mockMethodSignature(
            "com.bytechef.platform.security.service.ApiKeyService", PlatformTarget.class.getMethod("updateSetting"));

        when(methodSignature.getDeclaringTypeName()).thenThrow(new RuntimeException("boom building the audit event"));

        ProceedingJoinPoint proceedingJoinPoint = mockProceedingJoinPoint(methodSignature);

        when(proceedingJoinPoint.proceed()).thenReturn("ok");

        assertThat(auditAspect.audit(proceedingJoinPoint)).isEqualTo("ok");

        assertThat(findAll()).isEmpty();
        assertThat(AuditFrame.current()).isNull();
    }

    @Test
    void testOuterEventConstructionFailureStillWritesTheBufferedInnerEvent() throws Throwable {
        authenticate("alice", "ROLE_ADMIN");

        MethodSignature methodSignature = mockMethodSignature(
            "com.bytechef.platform.security.service.ApiKeyService", PlatformTarget.class.getMethod("updateSetting"));

        when(methodSignature.getDeclaringTypeName()).thenThrow(new RuntimeException("boom building the audit event"));

        ProceedingJoinPoint proceedingJoinPoint = mockProceedingJoinPoint(methodSignature);
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        when(proceedingJoinPoint.proceed())
            .thenAnswer(
                invocation -> transactionTemplate.execute(transactionStatus -> innerGuardedService.adminOnly()));

        assertThat(auditAspect.audit(proceedingJoinPoint)).isEqualTo("inner");

        assertThat(findSingle().getData())
            .containsEntry("method", InnerGuardedService.class.getName() + ".adminOnly")
            .containsEntry("result", "ALLOWED");
        assertThat(AuditFrame.current()).isNull();
    }

    @ParameterizedTest
    @CsvSource(value = {
        "com.bytechef.ee.embedded.configuration.facade.ConnectedUserFacade,EMBEDDED",
        "com.bytechef.automation.configuration.facade.ProjectFacade,AUTOMATION",
        "com.bytechef.ee.automation.configuration.service.WorkspaceUserService,AUTOMATION",
        "com.bytechef.automation.embedded.SomeFacade,EMBEDDED",
        "com.bytechef.platform.security.service.ApiKeyService,NULL",
        "TestService,NULL"
    }, nullValues = "NULL")
    void testPackagePlatformRule(String declaringTypeName, PlatformType expectedPlatform) {
        assertThat(AuditAspect.resolvePackagePlatform(declaringTypeName)).isEqualTo(expectedPlatform);
    }

    @Test
    void testPermissionCheckOnEmbeddedTypeRecordsEmbeddedPlatform() throws Throwable {
        ProceedingJoinPoint proceedingJoinPoint = mockProceedingJoinPoint(
            mockMethodSignature(
                "com.bytechef.ee.embedded.configuration.facade.ConnectedUserFacade",
                PlatformTarget.class.getMethod("updateSetting")));

        when(proceedingJoinPoint.proceed()).thenReturn("ok");

        auditAspect.audit(proceedingJoinPoint);

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo("PERMISSION_CHECK");
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.EMBEDDED);
    }

    @Test
    void testPermissionCheckOnAutomationTypeRecordsAutomationPlatform() throws Throwable {
        ProceedingJoinPoint proceedingJoinPoint = mockProceedingJoinPoint(
            mockMethodSignature(
                "com.bytechef.automation.configuration.facade.ProjectFacade",
                PlatformTarget.class.getMethod("updateSetting")));

        when(proceedingJoinPoint.proceed()).thenThrow(new AccessDeniedException("denied"));

        assertThatThrownBy(() -> auditAspect.audit(proceedingJoinPoint)).isInstanceOf(AccessDeniedException.class);

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo("PERMISSION_CHECK");
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData()).containsEntry("result", "DENIED");
    }

    @Test
    void testPermissionCheckOnPlatformNeutralTypeRecordsNoPlatform() throws Throwable {
        ProceedingJoinPoint proceedingJoinPoint = mockProceedingJoinPoint(
            mockMethodSignature(
                "com.bytechef.platform.security.service.ApiKeyService",
                PlatformTarget.class.getMethod("updateSetting")));

        when(proceedingJoinPoint.proceed()).thenReturn("ok");

        auditAspect.audit(proceedingJoinPoint);

        assertThat(findSingle().getPlatform()).isNull();
    }

    @Test
    void testMapperPlatformWinsOverThePackageRule() throws Throwable {
        ProceedingJoinPoint proceedingJoinPoint = mockProceedingJoinPoint(
            mockMethodSignature(
                "com.bytechef.automation.configuration.service.ProjectService",
                PlatformTarget.class.getMethod("updateWithEmbeddedPlatform", long.class)),
            42L);

        when(proceedingJoinPoint.proceed()).thenReturn("ok");

        auditAspect.audit(proceedingJoinPoint);

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.EMBEDDED);
        assertThat(persistentAuditEvent.getData()).doesNotContainKey("mapperError");
    }

    @Test
    void testNullMapperPlatformFallsBackToThePackageRule() throws Throwable {
        ProceedingJoinPoint proceedingJoinPoint = mockProceedingJoinPoint(
            mockMethodSignature(
                "com.bytechef.automation.configuration.service.ProjectService",
                PlatformTarget.class.getMethod("updateWithoutPlatform", long.class)),
            42L);

        when(proceedingJoinPoint.proceed()).thenReturn("ok");

        auditAspect.audit(proceedingJoinPoint);

        assertThat(findSingle().getPlatform()).isEqualTo(PlatformType.AUTOMATION);
    }

    @Test
    void testThrowingMapperPlatformFallsBackToThePackageRuleAndStillWritesTheRow() throws Throwable {
        ProceedingJoinPoint proceedingJoinPoint = mockProceedingJoinPoint(
            mockMethodSignature(
                "com.bytechef.ee.embedded.configuration.service.IntegrationService",
                PlatformTarget.class.getMethod("updateWithThrowingPlatform", long.class)),
            42L);

        when(proceedingJoinPoint.proceed()).thenReturn("ok");

        assertThat(auditAspect.audit(proceedingJoinPoint)).isEqualTo("ok");

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.EMBEDDED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("userId", "42")
            .containsEntry("mapperError", IllegalStateException.class.getName());
    }

    @Test
    void testThrowingMapperStillGetsThePackagePlatform() throws Throwable {
        ProceedingJoinPoint proceedingJoinPoint = mockProceedingJoinPoint(
            mockMethodSignature(
                "com.bytechef.automation.configuration.service.ProjectService",
                PlatformTarget.class.getMethod("updateWithThrowingMapper", long.class)),
            42L);

        when(proceedingJoinPoint.proceed()).thenReturn("ok");

        auditAspect.audit(proceedingJoinPoint);

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("mapperError", IllegalArgumentException.class.getName());
    }

    private void authenticate(Object principal, String authority) {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private List<PersistentAuditEvent> findAll() {
        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        return page.getContent();
    }

    private Map<String, String> findRecordedMethodResults() {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            new AuditEventFilter("PERMISSION_CHECK", null, null, null, true, null), PageRequest.of(0, 25));

        return page.getContent()
            .stream()
            .map(PersistentAuditEvent::getData)
            .collect(Collectors.toMap(data -> data.get("method"), data -> data.get("result")));
    }

    private PersistentAuditEvent findSingle() {
        List<PersistentAuditEvent> persistentAuditEvents = findAll();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    private static MethodSignature mockMethodSignature(String declaringTypeName, Method method) {
        MethodSignature methodSignature = mock(MethodSignature.class);

        String[] parameterNames = Arrays.stream(method.getParameters())
            .map(Parameter::getName)
            .toArray(String[]::new);

        when(methodSignature.getDeclaringTypeName()).thenReturn(declaringTypeName);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getParameterNames()).thenReturn(parameterNames);

        return methodSignature;
    }

    private static ProceedingJoinPoint mockProceedingJoinPoint(MethodSignature methodSignature, Object... arguments) {
        ProceedingJoinPoint proceedingJoinPoint = mock(ProceedingJoinPoint.class);

        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(proceedingJoinPoint.getArgs()).thenReturn(arguments);

        return proceedingJoinPoint;
    }

    @Configuration
    @EnableMethodSecurity
    static class GuardedServiceConfiguration {

        @Bean
        AuditedBusinessService auditedBusinessService(
            PersistenceAuditEventRepository persistenceAuditEventRepository) {

            return new AuditedBusinessService(persistenceAuditEventRepository);
        }

        @Bean
        AuditedCaller auditedCaller(InnerGuardedService innerGuardedService) {
            return new AuditedCaller(innerGuardedService);
        }

        @Bean
        AuditedTestService auditedTestService() {
            return new AuditedTestService();
        }

        @Bean
        ClassGuardedTestService classGuardedTestService() {
            return new ClassGuardedTestService();
        }

        @Bean
        CountingAuditMapper countingAuditMapper(PersistenceAuditEventRepository persistenceAuditEventRepository) {
            return new CountingAuditMapper(persistenceAuditEventRepository);
        }

        @Bean
        EmbeddedPlatformAuditMapper embeddedPlatformAuditMapper() {
            return new EmbeddedPlatformAuditMapper();
        }

        @Bean
        FailingCaptureAuditMapper failingCaptureAuditMapper(JdbcTemplate jdbcTemplate) {
            return new FailingCaptureAuditMapper(jdbcTemplate);
        }

        @Bean
        ForgingAuditMapper forgingAuditMapper() {
            return new ForgingAuditMapper();
        }

        @Bean
        GuardedTestService guardedTestService() {
            return new GuardedTestService();
        }

        @Bean
        InnerGuardedService innerGuardedService() {
            return new InnerGuardedService();
        }

        @Bean
        LinkageErrorAuditMapper linkageErrorAuditMapper() {
            return new LinkageErrorAuditMapper();
        }

        @Bean
        NullDataAuditMapper nullDataAuditMapper() {
            return new NullDataAuditMapper();
        }

        @Bean
        OuterGuardedService outerGuardedService(
            InnerGuardedService innerGuardedService, PersistenceAuditEventRepository persistenceAuditEventRepository) {

            return new OuterGuardedService(innerGuardedService, persistenceAuditEventRepository);
        }

        @Bean
        SkippingAuditMapper skippingAuditMapper() {
            return new SkippingAuditMapper();
        }

        @Bean
        ThrowingAuditMapper throwingAuditMapper() {
            return new ThrowingAuditMapper();
        }

        @Bean
        ThrowingPlatformAuditMapper throwingPlatformAuditMapper() {
            return new ThrowingPlatformAuditMapper();
        }

        @Bean
        ThrowingShouldRecordAuditMapper throwingShouldRecordAuditMapper() {
            return new ThrowingShouldRecordAuditMapper();
        }

        @Bean
        UnauditedCaller unauditedCaller(
            InnerGuardedService innerGuardedService, JdbcTemplate jdbcTemplate,
            PersistenceAuditEventRepository persistenceAuditEventRepository) {

            return new UnauditedCaller(innerGuardedService, jdbcTemplate, persistenceAuditEventRepository);
        }

        @Bean
        UserAuditMapper userAuditMapper() {
            return new UserAuditMapper();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    static class ClassGuardedTestService {

        public String updateSetting() {
            return "ok";
        }
    }

    static class GuardedTestService {

        @PreAuthorize("hasRole('ADMIN')")
        public String adminOnlyMethod() {
            return "ok";
        }

        @PreAuthorize("hasRole('ADMIN')")
        public String createSomething() {
            return "ok";
        }

        @PreAuthorize("hasRole('ADMIN')")
        public String doSomething() {
            return "ok";
        }

        @PreAuthorize("hasRole('ADMIN')")
        public String getSomething() {
            return "ok";
        }

        @PreAuthorize("hasRole('ADMIN')")
        public String getSomethingThatFails() {
            throw new IllegalStateException("read failed");
        }

        @PreAuthorize("isAuthenticated()")
        public String rethrow(RuntimeException runtimeException) {
            throw runtimeException;
        }

        @PreAuthorize("permitAll()")
        public String updateAnonymously() {
            return "ok";
        }
    }

    static class AuditedTestService {

        @Audited(event = "TEST_EVENT", mapper = UserAuditMapper.class)
        @PreAuthorize("hasRole('ADMIN')")
        public String updateUser(long userId) {
            return "ok";
        }

        @Audited(event = "TEST_EVENT", mapper = ForgingAuditMapper.class)
        public String updateUserWithForgingMapper(long userId) {
            return "ok";
        }

        @Audited(event = "TEST_EVENT", mapper = LinkageErrorAuditMapper.class)
        public String updateUserWithLinkageErrorMapper(long userId) {
            return "ok";
        }

        @Audited(event = "TEST_EVENT", mapper = NullDataAuditMapper.class)
        public String updateUserWithNullDataMapper(long userId) {
            return "ok";
        }

        @Audited(event = "TEST_EVENT", mapper = SkippingAuditMapper.class)
        public String updateUserWithSkippingMapper(long userId) {
            return "ok";
        }

        @Audited(event = "TEST_EVENT", mapper = ThrowingAuditMapper.class)
        public String updateUserWithThrowingMapper(long userId) {
            return "ok";
        }

        @Audited(event = "TEST_EVENT", mapper = ThrowingShouldRecordAuditMapper.class)
        public String updateUserWithThrowingShouldRecordMapper(long userId) {
            return "ok";
        }
    }

    static class PlatformTarget {

        public void updateSetting() {
        }

        @Audited(event = "TEST_EVENT", mapper = EmbeddedPlatformAuditMapper.class)
        public void updateWithEmbeddedPlatform(long userId) {
        }

        @Audited(event = "TEST_EVENT", mapper = ThrowingAuditMapper.class)
        public void updateWithThrowingMapper(long userId) {
        }

        @Audited(event = "TEST_EVENT", mapper = ThrowingPlatformAuditMapper.class)
        public void updateWithThrowingPlatform(long userId) {
        }

        @Audited(event = "TEST_EVENT", mapper = UserAuditMapper.class)
        public void updateWithoutPlatform(long userId) {
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
        private final PersistenceAuditEventRepository persistenceAuditEventRepository;

        OuterGuardedService(
            InnerGuardedService innerGuardedService, PersistenceAuditEventRepository persistenceAuditEventRepository) {

            this.innerGuardedService = innerGuardedService;
            this.persistenceAuditEventRepository = persistenceAuditEventRepository;
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

        @PreAuthorize("hasRole('ADMIN')")
        @Transactional
        public long callInnerThenCountAuditEvents() {
            innerGuardedService.adminOnly();

            return persistenceAuditEventRepository.count();
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

    static class AuditedBusinessService {

        private final PersistenceAuditEventRepository persistenceAuditEventRepository;

        AuditedBusinessService(PersistenceAuditEventRepository persistenceAuditEventRepository) {
            this.persistenceAuditEventRepository = persistenceAuditEventRepository;
        }

        @Audited(event = "BUSINESS_TEST", mapper = CountingAuditMapper.class)
        @Transactional
        public String write() {
            return "written";
        }

        @Audited(event = "BUSINESS_TEST", mapper = CountingAuditMapper.class)
        @PreAuthorize("hasRole('ADMIN')")
        @Transactional
        public String writeThenFail() {
            PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

            persistentAuditEvent.setEventDate(LocalDateTime.now());
            persistentAuditEvent.setEventType("BUSINESS_WRITE");
            persistentAuditEvent.setPrincipal("alice");

            persistenceAuditEventRepository.save(persistentAuditEvent);

            throw new IllegalStateException("business failed");
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

    abstract static class AbstractUserAuditMapper implements AuditMapper {

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            Map<String, String> data = new HashMap<>();

            data.put("userId", String.valueOf(auditInvocation.argument("userId")));

            if (auditInvocation.result() != null) {
                data.put("resultValue", String.valueOf(auditInvocation.result()));
            }

            return data;
        }
    }

    static class UserAuditMapper extends AbstractUserAuditMapper {
    }

    static class EmbeddedPlatformAuditMapper extends AbstractUserAuditMapper {

        @Override
        public PlatformType platform(AuditInvocation auditInvocation) {
            return PlatformType.EMBEDDED;
        }
    }

    static class ThrowingPlatformAuditMapper extends AbstractUserAuditMapper {

        @Override
        public PlatformType platform(AuditInvocation auditInvocation) {
            throw new IllegalStateException("bad platform");
        }
    }

    static class ThrowingAuditMapper implements AuditMapper {

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            throw new IllegalArgumentException("bad mapper");
        }
    }

    static class LinkageErrorAuditMapper implements AuditMapper {

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            throw new NoClassDefFoundError("com/example/Missing");
        }
    }

    static class NullDataAuditMapper implements AuditMapper {

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            return null;
        }
    }

    static class ForgingAuditMapper implements AuditMapper {

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            return Map.of(
                "result", "FORGED", "method", "com.evil.Forged.method", "errorClass", "com.evil.Forged",
                "mapperError", "FORGED_ERROR", "k".repeat(300), "v".repeat(299) + "END");
        }
    }

    static class SkippingAuditMapper implements AuditMapper {

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            throw new AssertionError("map must not run when shouldRecord returns false");
        }

        @Override
        public boolean shouldRecord(AuditInvocation auditInvocation) {
            return false;
        }
    }

    static class ThrowingShouldRecordAuditMapper implements AuditMapper {

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            throw new AssertionError("map must not run when shouldRecord throws");
        }

        @Override
        public boolean shouldRecord(AuditInvocation auditInvocation) {
            throw new IllegalStateException("bad shouldRecord");
        }
    }
}
