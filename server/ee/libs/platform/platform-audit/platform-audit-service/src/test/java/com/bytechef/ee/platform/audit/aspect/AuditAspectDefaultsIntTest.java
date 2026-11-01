/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import({
    PostgreSQLContainerConfiguration.class, AuditAspectDefaultsIntTest.DefaultsServiceConfiguration.class
})
@SuppressFBWarnings(
    value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
    justification = "Guarded calls inside assertThatThrownBy are expected to throw, so their return value is irrelevant")
class AuditAspectDefaultsIntTest {

    @Autowired
    private DefaultsTestService defaultsTestService;

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
    void testPassedPermissionCheckIsNotRecorded() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(defaultsTestService.updateSetting()).isEqualTo("ok");

        assertThat(findAll()).isEmpty();
    }

    @Test
    void testDeniedPermissionCheckIsRecorded() {
        authenticate("bob", "ROLE_USER");

        assertThatThrownBy(() -> defaultsTestService.updateSetting())
            .isInstanceOf(AccessDeniedException.class);

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo("PERMISSION_CHECK");
        assertThat(persistentAuditEvent.getData()).containsEntry("result", "DENIED");
    }

    @Test
    void testFailedPermissionCheckedCallIsRecorded() {
        authenticate("alice", "ROLE_ADMIN");

        assertThatThrownBy(() -> defaultsTestService.updateSettingThatFails())
            .isInstanceOf(IllegalStateException.class);

        assertThat(findSingle().getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalStateException.class.getName());
    }

    @Test
    void testDataExpressionsRecordArgumentsAndResult() {
        authenticate("alice", "ROLE_ADMIN");

        defaultsTestService.updateRecord(new TestRecord(null, "Invoices"), "finance");

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo("RECORD_UPDATED");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("recordId", "42")
            .containsEntry("name", "Invoices")
            .containsEntry("tag", "finance")
            .doesNotContainKeys("dataError", "captureError");
    }

    @Test
    void testDataExpressionsOnDeniedCallSkipTheMissingResult() {
        authenticate("bob", "ROLE_USER");

        assertThatThrownBy(() -> defaultsTestService.updateRecord(new TestRecord(null, "Invoices"), "finance"))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(findSingle().getData())
            .containsEntry("result", "DENIED")
            .containsEntry("name", "Invoices")
            .doesNotContainKeys("recordId", "dataError");
    }

    @Test
    void testDataExpressionsReadRecordComponentsAndDerivedAccessors() {
        authenticate("alice", "ROLE_ADMIN");

        defaultsTestService.importRecords(List.of("a", "b"));

        assertThat(findSingle().getData())
            .containsEntry("createdCount", "1")
            .containsEntry("skippedCount", "2")
            .doesNotContainKey("dataError");
    }

    @Test
    void testFailingDataExpressionStillWritesTheEvent() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(defaultsTestService.updateWithBrokenExpression()).isEqualTo("ok");

        assertThat(findSingle().getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("dataError", "value")
            .doesNotContainKey("mapperError");
    }

    @Test
    void testFailingDataEntryKeepsTheOtherEntries() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(defaultsTestService.updateWithOneBrokenEntry("finance")).isEqualTo("ok");

        assertThat(findSingle().getData())
            .containsEntry("tag", "finance")
            .containsEntry("dataError", "broken");
    }

    @Test
    void testUndefinedVariableIsReportedInsteadOfDropped() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(defaultsTestService.updateWithUndefinedVariable(7L)).isEqualTo("ok");

        assertThat(findSingle().getData())
            .containsEntry("dataError", "recordId")
            .doesNotContainKey("recordId");
    }

    @Test
    void testUndefinedVariableInsideAnExpressionIsReported() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(defaultsTestService.updateWithNestedUndefinedVariable("finance")).isEqualTo("ok");

        assertThat(findSingle().getData())
            .containsEntry("dataError", "name")
            .doesNotContainKey("name");
    }

    @Test
    void testErrorInOneDataEntryKeepsTheOtherEntries() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(defaultsTestService.updateWithExplodingValue(new ExplodingValue(), "finance")).isEqualTo("ok");

        assertThat(findSingle().getData())
            .containsEntry("tag", "finance")
            .containsEntry("dataError", "exploding");
    }

    @Test
    void testFailingDataExpressionLogsItsStackTrace() {
        authenticate("alice", "ROLE_ADMIN");

        Logger logger = (Logger) LoggerFactory.getLogger(AuditAspect.class);

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();
        logger.addAppender(listAppender);

        try {
            defaultsTestService.updateWithExplodingValue(new ExplodingValue(), "finance");
        } finally {
            logger.detachAppender(listAppender);
        }

        assertThat(listAppender.list)
            .filteredOn(loggingEvent -> loggingEvent.getFormattedMessage()
                .startsWith("AUDIT DATA FAILURE"))
            .singleElement()
            .satisfies(loggingEvent -> assertThat(loggingEvent.getThrowableProxy()
                .getClassName()).isEqualTo(StackOverflowError.class.getName()));
    }

    @Test
    void testResultExpressionIsReportedWhenAParameterIsNamedResult() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(defaultsTestService.updateWithResultParameter("finance", "argument")).isEqualTo("returned");

        assertThat(findSingle().getData())
            .containsEntry("tag", "finance")
            .containsEntry("dataError", "returned")
            .doesNotContainKey("returned");
    }

    @Test
    void testDataEntryCannotOverwriteAMapperKey() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(defaultsTestService.updateWithMapperAndData("finance")).isEqualTo("ok");

        assertThat(findSingle().getData())
            .containsEntry("tag", "mapped")
            .containsEntry("dataError", "tag")
            .doesNotContainKey("mapperError");
    }

    @Test
    void testReservedBlankAndDuplicateKeysAreReported() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(defaultsTestService.updateWithInvalidKeys("finance")).isEqualTo("ok");

        assertThat(findSingle().getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("tag", "finance")
            .containsEntry("dataError", "result,[blank],tag");
    }

    @Test
    void testDataExpressionsCannotCallMethods() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(defaultsTestService.updateWithMethodCall("finance")).isEqualTo("ok");

        assertThat(findSingle().getData())
            .containsEntry("dataError", "length")
            .doesNotContainKey("length");
    }

    @Test
    void testLongDataValueKeepsItsBeginningAndIsMarked() {
        authenticate("alice", "ROLE_ADMIN");

        String value = "a".repeat(300) + "z";

        assertThat(defaultsTestService.updateWithLongValue(value)).isEqualTo("ok");

        PersistentAuditEvent persistentAuditEvent = findSingle();

        Map<String, String> data = persistentAuditEvent.getData();

        String recordedValue = data.get("value");

        assertThat(recordedValue)
            .hasSize(AuditAspect.COLUMN_MAX_LENGTH)
            .startsWith("aaa")
            .endsWith("...");
    }

    @Test
    void testDataExpressionsCannotReachTypes() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(defaultsTestService.updateWithTypeReference()).isEqualTo("ok");

        assertThat(findSingle().getData())
            .containsEntry("dataError", "home")
            .doesNotContainKey("home");
    }

    @Test
    void testDeclaredPlatformIsRecordedWithoutAMapper() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(defaultsTestService.updateOnDeclaredPlatform()).isEqualTo("ok");

        assertThat(findSingle().getPlatform()).isEqualTo(PlatformType.AUTOMATION);
    }

    @Test
    void testMapperPlatformWinsOverTheDeclaredPlatform() {
        authenticate("alice", "ROLE_ADMIN");

        assertThat(defaultsTestService.updateWithMapperPlatform()).isEqualTo("ok");

        assertThat(findSingle().getPlatform()).isEqualTo(PlatformType.EMBEDDED);
    }

    private void authenticate(String principal, String authority) {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private List<PersistentAuditEvent> findAll() {
        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        return page.getContent();
    }

    private PersistentAuditEvent findSingle() {
        List<PersistentAuditEvent> persistentAuditEvents = findAll();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @Configuration
    @EnableMethodSecurity
    static class DefaultsServiceConfiguration {

        @Bean
        DefaultsTestService defaultsTestService() {
            return new DefaultsTestService();
        }

        @Bean
        EmbeddedAuditMapper embeddedAuditMapper() {
            return new EmbeddedAuditMapper();
        }

        @Bean
        TagAuditMapper tagAuditMapper() {
            return new TagAuditMapper();
        }
    }

    static class DefaultsTestService {

        @Audited(event = "RECORD_UPDATED", data = {
            @AuditData(key = "recordId", value = "#result?.id"),
            @AuditData(key = "name", value = "#testRecord.name"),
            @AuditData(key = "tag", value = "#tag")
        })
        @PreAuthorize("hasRole('ADMIN')")
        public TestRecord updateRecord(TestRecord testRecord, String tag) {
            return new TestRecord(42L, testRecord.getName());
        }

        @PreAuthorize("hasRole('ADMIN')")
        public String updateSetting() {
            return "ok";
        }

        @PreAuthorize("hasRole('ADMIN')")
        public String updateSettingThatFails() {
            throw new IllegalStateException("update failed");
        }

        @Audited(event = "BROKEN_EXPRESSION", data = {
            @AuditData(key = "value", value = "#missing.name")
        })
        @PreAuthorize("hasRole('ADMIN')")
        public String updateWithBrokenExpression() {
            return "ok";
        }

        @Audited(event = "RECORDS_IMPORTED", data = {
            @AuditData(key = "createdCount", value = "#result?.createdCount"),
            @AuditData(key = "skippedCount", value = "#result?.skippedCount")
        })
        @PreAuthorize("hasRole('ADMIN')")
        public TestImportResult importRecords(List<String> skippedIds) {
            return new TestImportResult(1, skippedIds);
        }

        @Audited(event = "ONE_BROKEN_ENTRY", data = {
            @AuditData(key = "broken", value = "#tag.missing"),
            @AuditData(key = "tag", value = "#tag")
        })
        @PreAuthorize("hasRole('ADMIN')")
        public String updateWithOneBrokenEntry(String tag) {
            return "ok";
        }

        @Audited(event = "UNDEFINED_VARIABLE", data = {
            @AuditData(key = "recordId", value = "#id")
        })
        @PreAuthorize("hasRole('ADMIN')")
        public String updateWithUndefinedVariable(Long recordId) {
            return "ok";
        }

        @Audited(event = "INVALID_KEYS", data = {
            @AuditData(key = "result", value = "#tag"),
            @AuditData(key = " ", value = "#tag"),
            @AuditData(key = "tag", value = "#tag"),
            @AuditData(key = "tag", value = "#tag")
        })
        @PreAuthorize("hasRole('ADMIN')")
        public String updateWithInvalidKeys(String tag) {
            return "ok";
        }

        @Audited(event = "NESTED_UNDEFINED_VARIABLE", data = {
            @AuditData(key = "name", value = "#unknown ?: #tag")
        })
        @PreAuthorize("hasRole('ADMIN')")
        public String updateWithNestedUndefinedVariable(String tag) {
            return "ok";
        }

        @Audited(event = "EXPLODING_VALUE", data = {
            @AuditData(key = "exploding", value = "#explodingValue"),
            @AuditData(key = "tag", value = "#tag")
        })
        @PreAuthorize("hasRole('ADMIN')")
        public String updateWithExplodingValue(ExplodingValue explodingValue, String tag) {
            return "ok";
        }

        @Audited(event = "RESULT_PARAMETER", data = {
            @AuditData(key = "tag", value = "#tag"),
            @AuditData(key = "returned", value = "#result")
        })
        @PreAuthorize("hasRole('ADMIN')")
        public String updateWithResultParameter(String tag, String result) {
            return "returned";
        }

        @Audited(event = "MAPPER_AND_DATA", mapper = TagAuditMapper.class, data = {
            @AuditData(key = "tag", value = "#tag")
        })
        @PreAuthorize("hasRole('ADMIN')")
        public String updateWithMapperAndData(String tag) {
            return "ok";
        }

        @Audited(event = "METHOD_CALL", data = {
            @AuditData(key = "length", value = "#tag.length()")
        })
        @PreAuthorize("hasRole('ADMIN')")
        public String updateWithMethodCall(String tag) {
            return "ok";
        }

        @Audited(event = "LONG_VALUE", data = {
            @AuditData(key = "value", value = "#value")
        })
        @PreAuthorize("hasRole('ADMIN')")
        public String updateWithLongValue(String value) {
            return "ok";
        }

        @Audited(event = "DECLARED_PLATFORM", platform = PlatformType.AUTOMATION)
        @PreAuthorize("hasRole('ADMIN')")
        public String updateOnDeclaredPlatform() {
            return "ok";
        }

        @Audited(event = "MAPPER_PLATFORM", mapper = EmbeddedAuditMapper.class, platform = PlatformType.AUTOMATION)
        @PreAuthorize("hasRole('ADMIN')")
        public String updateWithMapperPlatform() {
            return "ok";
        }

        @Audited(event = "TYPE_REFERENCE", data = {
            @AuditData(key = "home", value = "T(java.lang.System).getProperty('user.home')")
        })
        @PreAuthorize("hasRole('ADMIN')")
        public String updateWithTypeReference() {
            return "ok";
        }
    }

    static class EmbeddedAuditMapper implements AuditMapper {

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            return Map.of();
        }

        @Override
        public PlatformType platform(AuditInvocation auditInvocation) {
            return PlatformType.EMBEDDED;
        }
    }

    static class TagAuditMapper implements AuditMapper {

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            return Map.of("tag", "mapped");
        }
    }

    static class ExplodingValue {

        @Override
        public String toString() {
            throw new StackOverflowError("toString recursion");
        }
    }

    record TestImportResult(int createdCount, List<String> skippedIds) {

        public int skippedCount() {
            return skippedIds.size();
        }
    }

    static class TestRecord {

        private final Long id;
        private final String name;

        TestRecord(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
