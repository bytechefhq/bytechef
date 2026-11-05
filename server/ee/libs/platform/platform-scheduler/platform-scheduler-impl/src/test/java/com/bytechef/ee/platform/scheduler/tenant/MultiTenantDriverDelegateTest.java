/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.DriverDelegate;
import org.quartz.impl.jdbcjobstore.FiredTriggerRecord;
import org.quartz.impl.jdbcjobstore.SchedulerStateRecord;
import org.quartz.impl.jdbcjobstore.TriggerStatus;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.OperableTrigger;
import org.slf4j.Logger;

/**
 * @version ee
 */
class MultiTenantDriverDelegateTest {

    private static final Calendar CALENDAR = mock(Calendar.class);
    private static final String CALENDAR_NAME = "holidays";
    private static final ClassLoadHelper CLASS_LOAD_HELPER = mock(ClassLoadHelper.class);
    private static final String GROUP_NAME = "SCHEDULE_TRIGGER";
    private static final String INSTANCE_ID = "scheduler-instance-1";
    private static final JobDetail JOB_DETAIL = mock(JobDetail.class);
    private static final JobKey JOB_KEY = JobKey.jobKey("job", GROUP_NAME);
    private static final GroupMatcher<JobKey> JOB_KEY_GROUP_MATCHER = GroupMatcher.jobGroupEquals(GROUP_NAME);
    private static final String PUBLIC_SCHEMA = "public";

    /**
     * The search_path {@code BaseDataSource} gives a tenant connection: the tenant schema with the {@code public}
     * fallback.
     */
    private static final String TENANT_SEARCH_PATH = "bytechef_000002, public";

    private static final OperableTrigger TRIGGER = mock(OperableTrigger.class);
    private static final TriggerKey TRIGGER_KEY = TriggerKey.triggerKey("trigger", GROUP_NAME);
    private static final GroupMatcher<TriggerKey> TRIGGER_KEY_GROUP_MATCHER =
        GroupMatcher.triggerGroupEquals(GROUP_NAME);

    private final DriverDelegate driverDelegate = mock(DriverDelegate.class);
    private final MultiTenantDriverDelegate multiTenantDriverDelegate = new MultiTenantDriverDelegate(driverDelegate);
    private SearchPathConnection searchPathConnection;
    private Connection connection;

    @BeforeEach
    void beforeEach() throws SQLException {
        searchPathConnection = new SearchPathConnection(TENANT_SEARCH_PATH);

        connection = searchPathConnection.getConnection();
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @ParameterizedTest
    @MethodSource({
        "misfireAndJobCalls", "triggerCalls", "calendarFiredTriggerAndSchedulerStateCalls",
        "callsPreviouslyServedByUninitializedStdJdbcDelegate"
    })
    void testDelegatedCallRunsOnPublicSchemaAndRestoresConnectionSearchPath(
        DelegateCall delegateCall, Object expectedResult) throws Exception {

        List<String> searchPathsDuringCall = new ArrayList<>();

        delegateCall.invoke(
            doAnswer(invocation -> {
                searchPathsDuringCall.add(searchPathConnection.getSearchPath());

                return expectedResult;
            }).when(driverDelegate),
            connection);

        TenantContext.setCurrentTenantId("000002");

        Object result = delegateCall.invoke(multiTenantDriverDelegate, connection);

        assertThat(result).isEqualTo(expectedResult);
        assertThat(searchPathsDuringCall).containsExactly(PUBLIC_SCHEMA);
        assertThat(searchPathConnection.getSearchPath()).isEqualTo(TENANT_SEARCH_PATH);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("000002");

        delegateCall.invoke(verify(driverDelegate), connection);
    }

    @Test
    void testClearDataRunsOnPublicSchemaAndRestoresConnectionSearchPath() throws Exception {
        List<String> searchPathsDuringCall = new ArrayList<>();

        doAnswer(invocation -> searchPathsDuringCall.add(searchPathConnection.getSearchPath()))
            .when(driverDelegate)
            .clearData(connection);

        TenantContext.setCurrentTenantId("000002");

        multiTenantDriverDelegate.clearData(connection);

        assertThat(searchPathsDuringCall).containsExactly(PUBLIC_SCHEMA);
        assertThat(searchPathConnection.getSearchPath()).isEqualTo(TENANT_SEARCH_PATH);
    }

    @Test
    void testPublicFallbackOfTenantSearchPathIsKeptAfterCall() throws Exception {
        TenantContext.setCurrentTenantId("000002");

        multiTenantDriverDelegate.jobExists(connection, JOB_KEY);

        assertThat(searchPathConnection.getSearchPath())
            .as("Application code running on the connection after a Quartz call must still resolve public objects")
            .isEqualTo(TENANT_SEARCH_PATH)
            .isNotEqualTo(TenantContext.getCurrentDatabaseSchema());
    }

    @Test
    void testDefaultTenantRestoresPublicSchema() throws Exception {
        SearchPathConnection publicSearchPathConnection = new SearchPathConnection(PUBLIC_SCHEMA);

        Connection publicConnection = publicSearchPathConnection.getConnection();

        doReturn(true).when(driverDelegate)
            .jobExists(publicConnection, JOB_KEY);

        assertThat(multiTenantDriverDelegate.jobExists(publicConnection, JOB_KEY)).isTrue();

        assertThat(publicSearchPathConnection.getSearchPath()).isEqualTo(PUBLIC_SCHEMA);
    }

    @Test
    void testSearchPathOfConnectionIsRestoredWhenTenantContextChangedSinceConnectionWasObtained() throws Exception {
        TenantContext.runWithTenantId("000005", () -> multiTenantDriverDelegate.jobExists(connection, JOB_KEY));

        assertThat(searchPathConnection.getSearchPath()).isEqualTo(TENANT_SEARCH_PATH);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testCustomSearchPathOfConnectionIsRestoredUnchanged() throws Exception {
        SearchPathConnection customSearchPathConnection = new SearchPathConnection(
            "bytechef_vectorstore_000002, \"$user\", public");

        TenantContext.setCurrentTenantId("000002");

        multiTenantDriverDelegate.selectNumJobs(customSearchPathConnection.getConnection());

        assertThat(customSearchPathConnection.getSearchPath())
            .isEqualTo("bytechef_vectorstore_000002, \"$user\", public");
    }

    @Test
    void testConnectionSearchPathIsRestoredWhenDelegateThrowsSqlException() throws Exception {
        SQLException sqlException = new SQLException("deadlock detected");

        doThrow(sqlException).when(driverDelegate)
            .deleteTrigger(connection, TRIGGER_KEY);

        TenantContext.setCurrentTenantId("000002");

        assertThatThrownBy(() -> multiTenantDriverDelegate.deleteTrigger(connection, TRIGGER_KEY))
            .isSameAs(sqlException);

        assertThat(searchPathConnection.getSearchPath()).isEqualTo(TENANT_SEARCH_PATH);
    }

    @Test
    void testConnectionSearchPathIsRestoredWhenDelegateThrowsRuntimeException() throws Exception {
        IllegalStateException illegalStateException = new IllegalStateException("unexpected");

        doThrow(illegalStateException).when(driverDelegate)
            .selectTriggerState(connection, TRIGGER_KEY);

        TenantContext.setCurrentTenantId("000002");

        assertThatThrownBy(() -> multiTenantDriverDelegate.selectTriggerState(connection, TRIGGER_KEY))
            .isSameAs(illegalStateException);

        assertThat(searchPathConnection.getSearchPath()).isEqualTo(TENANT_SEARCH_PATH);
    }

    @Test
    void testConnectionSearchPathIsRestoredWhenDelegateThrowsCheckedSerializationException() throws Exception {
        IOException ioException = new IOException("cannot serialize job data");

        doThrow(ioException).when(driverDelegate)
            .insertJobDetail(connection, JOB_DETAIL);

        TenantContext.setCurrentTenantId("000002");

        assertThatThrownBy(() -> multiTenantDriverDelegate.insertJobDetail(connection, JOB_DETAIL))
            .isSameAs(ioException);

        assertThat(searchPathConnection.getSearchPath()).isEqualTo(TENANT_SEARCH_PATH);
    }

    @Test
    void testDelegateIsNotCalledWhenSwitchingToPublicSchemaFails() throws Exception {
        SQLException sqlException = new SQLException("connection closed");

        searchPathConnection.failSearchPathChange(1, sqlException);

        assertThatThrownBy(() -> multiTenantDriverDelegate.jobExists(connection, JOB_KEY))
            .isSameAs(sqlException);

        verifyNoInteractions(driverDelegate);

        assertThat(searchPathConnection.getSearchPath()).isEqualTo(TENANT_SEARCH_PATH);
    }

    @Test
    void testEveryCallSwitchesAndRestoresInOneStatementEachAndClosesThem() throws Exception {
        multiTenantDriverDelegate.jobExists(connection, JOB_KEY);
        multiTenantDriverDelegate.triggerExists(connection, TRIGGER_KEY);

        assertThat(searchPathConnection.getExecutedStatementCount())
            .as("Capturing the search_path must not add a round trip to the switch and the restore of each call")
            .isEqualTo(4);

        searchPathConnection.verifyEveryStatementAndResultSetClosed();
    }

    @Test
    void testEveryDriverDelegateMethodIsDeclaredByMultiTenantDriverDelegate() {
        List<String> inheritedMethods = new ArrayList<>();

        for (Method interfaceMethod : driverDelegateMethods()) {
            Method implementationMethod = findImplementationMethod(interfaceMethod);

            Class<?> declaringClass = implementationMethod.getDeclaringClass();

            if (declaringClass != MultiTenantDriverDelegate.class) {
                inheritedMethods.add(describe(interfaceMethod) + " served by " + declaringClass.getName());
            }
        }

        assertThat(inheritedMethods)
            .as(
                "Every DriverDelegate method must be overridden by MultiTenantDriverDelegate and forwarded to the " +
                    "wrapped delegate, otherwise it bypasses the delegate and the search_path switch")
            .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("connectionMethods")
    void testEveryConnectionMethodIsForwardedWithSameArgumentsOnPublicSchema(Method method) throws Throwable {
        Class<?> returnType = method.getReturnType();

        Object[] arguments = createArguments(method);
        Object expectedResult = createReturnValue(returnType);
        List<String> searchPathsDuringCall = new ArrayList<>();

        DriverDelegate forwardingDelegate = mock(DriverDelegate.class, invocation -> {
            if (method.equals(invocation.getMethod())) {
                searchPathsDuringCall.add(searchPathConnection.getSearchPath());

                return expectedResult;
            }

            return RETURNS_DEFAULTS.answer(invocation);
        });

        TenantContext.setCurrentTenantId("000002");

        Object result = invoke(new MultiTenantDriverDelegate(forwardingDelegate), method, arguments);

        if (returnType.isPrimitive()) {
            assertThat(result).isEqualTo(expectedResult);
        } else {
            assertThat(result).isSameAs(expectedResult);
        }

        assertThat(searchPathsDuringCall).containsExactly(PUBLIC_SCHEMA);
        assertThat(searchPathConnection.getSearchPath()).isEqualTo(TENANT_SEARCH_PATH);

        invoke(verify(forwardingDelegate), method, arguments);
    }

    @ParameterizedTest
    @MethodSource("connectionMethodCheckedExceptions")
    void testEveryDeclaredCheckedExceptionIsPropagatedUnwrapped(
        Method method, Class<? extends Exception> exceptionClass)
        throws Throwable {

        Object[] arguments = createArguments(method);
        Exception exception = exceptionClass.getConstructor(String.class)
            .newInstance("delegate failed");

        invoke(doThrow(exception).when(driverDelegate), method, arguments);

        TenantContext.setCurrentTenantId("000002");

        assertThatThrownBy(() -> invoke(multiTenantDriverDelegate, method, arguments))
            .isSameAs(exception);

        assertThat(searchPathConnection.getSearchPath()).isEqualTo(TENANT_SEARCH_PATH);
    }

    @Test
    void testSelectTriggerKeysForJobIsForwardedSoRemoveJobCanDeleteTriggersFirst() throws Exception {
        List<TriggerKey> triggerKeys = List.of(TRIGGER_KEY);

        doReturn(triggerKeys).when(driverDelegate)
            .selectTriggerKeysForJob(connection, JOB_KEY);

        assertThat(multiTenantDriverDelegate.selectTriggerKeysForJob(connection, JOB_KEY)).isSameAs(triggerKeys);

        verify(driverDelegate).selectTriggerKeysForJob(connection, JOB_KEY);
    }

    @Test
    void testFailureToRestoreSearchPathIsSuppressedOnDelegateException() throws Exception {
        SQLException delegateException = new SQLException("deadlock detected");
        SQLException restoreException = new SQLException("current transaction is aborted");

        searchPathConnection.failSearchPathChange(2, restoreException);

        doThrow(delegateException).when(driverDelegate)
            .deleteTrigger(connection, TRIGGER_KEY);

        TenantContext.setCurrentTenantId("000002");

        assertThatThrownBy(() -> multiTenantDriverDelegate.deleteTrigger(connection, TRIGGER_KEY))
            .isSameAs(delegateException)
            .satisfies(throwable -> assertThat(throwable.getSuppressed()).containsExactly(restoreException));

        searchPathConnection.verifyEveryStatementAndResultSetClosed();
    }

    @Test
    void testFailureToRestoreSearchPathIsThrownWhenDelegateSucceeds() throws Exception {
        SQLException restoreException = new SQLException("connection reset");

        searchPathConnection.failSearchPathChange(2, restoreException);

        TenantContext.setCurrentTenantId("000002");

        assertThatThrownBy(() -> multiTenantDriverDelegate.jobExists(connection, JOB_KEY))
            .isSameAs(restoreException);

        verify(driverDelegate).jobExists(connection, JOB_KEY);
    }

    @Test
    void testInitializeIsForwardedWithoutTouchingConnection() throws Exception {
        Logger logger = mock(Logger.class);

        multiTenantDriverDelegate.initialize(
            logger, "QRTZ_", "scheduler", INSTANCE_ID, CLASS_LOAD_HELPER, true, "triggerPersistenceDelegateClasses=");

        verify(driverDelegate).initialize(
            logger, "QRTZ_", "scheduler", INSTANCE_ID, CLASS_LOAD_HELPER, true, "triggerPersistenceDelegateClasses=");
        verifyNoInteractions(connection);
    }

    @Test
    void testEnhancedStatementsSettingIsForwarded() {
        when(driverDelegate.isUsingEnhancedStatements()).thenReturn(true);

        multiTenantDriverDelegate.setUseEnhancedStatements(true);

        assertThat(multiTenantDriverDelegate.isUsingEnhancedStatements()).isTrue();

        verify(driverDelegate).setUseEnhancedStatements(true);
        verifyNoInteractions(connection);
    }

    /**
     * Calls that were served by the never-initialized {@code StdJDBCDelegate} superclass (which queried
     * {@code SCHED_NAME = 'null'}), stubbed to return empty, or not overridden at all.
     */
    @SuppressWarnings("deprecation")
    static Stream<Arguments> callsPreviouslyServedByUninitializedStdJdbcDelegate() {
        GroupMatcher<JobKey> anyJobGroup = GroupMatcher.anyJobGroup();
        GroupMatcher<TriggerKey> anyTriggerGroup = GroupMatcher.anyTriggerGroup();

        return Stream.of(
            arguments("deleteFiredTriggers", DriverDelegate::deleteFiredTriggers, 3),
            arguments("selectNumJobs", DriverDelegate::selectNumJobs, 5),
            arguments("selectJobGroups", DriverDelegate::selectJobGroups, List.of(GROUP_NAME)),
            arguments("selectNumTriggers", DriverDelegate::selectNumTriggers, 7),
            arguments(
                "selectTriggerGroups", (delegate, connection) -> delegate.selectTriggerGroups(connection),
                List.of(GROUP_NAME)),
            arguments("deleteAllPausedTriggerGroups", DriverDelegate::deleteAllPausedTriggerGroups, 2),
            arguments("selectPausedTriggerGroups", DriverDelegate::selectPausedTriggerGroups, Set.of(GROUP_NAME)),
            arguments("selectNumCalendars", DriverDelegate::selectNumCalendars, 1),
            arguments("selectCalendars", DriverDelegate::selectCalendars, List.of(CALENDAR_NAME)),
            arguments("selectNextFireTime", DriverDelegate::selectNextFireTime, 1_700_000_000_000L),
            arguments(
                "selectFiredTriggerInstanceNames", DriverDelegate::selectFiredTriggerInstanceNames,
                Set.of(INSTANCE_ID)),
            arguments(
                "selectTriggerKeysForJob",
                (delegate, connection) -> delegate.selectTriggerKeysForJob(connection, JOB_KEY), List.of(TRIGGER_KEY)),
            arguments(
                "selectJobDetails",
                (delegate, connection) -> delegate.selectJobDetails(connection, anyJobGroup, CLASS_LOAD_HELPER),
                List.of(JOB_DETAIL)),
            arguments(
                "getTriggersByJobAndTriggerGroup",
                (delegate, connection) -> delegate.getTriggersByJobAndTriggerGroup(
                    connection, anyJobGroup, anyTriggerGroup),
                List.of(TRIGGER)));
    }

    static Stream<Arguments> connectionMethods() {
        return driverDelegateMethods().stream()
            .filter(MultiTenantDriverDelegateTest::takesConnection)
            .map(method -> Arguments.of(Named.of(describe(method), method)));
    }

    static Stream<Arguments> connectionMethodCheckedExceptions() {
        return driverDelegateMethods().stream()
            .filter(MultiTenantDriverDelegateTest::takesConnection)
            .flatMap(method -> Arrays.stream(method.getExceptionTypes())
                .map(exceptionType -> Arguments.of(
                    Named.of(describe(method), method),
                    Named.of(exceptionType.getSimpleName(), exceptionType))));
    }

    static Stream<Arguments> misfireAndJobCalls() {
        List<TriggerKey> triggerKeys = List.of(TRIGGER_KEY);
        List<OperableTrigger> operableTriggers = List.of(TRIGGER);

        return Stream.of(
            arguments(
                "updateTriggerStatesFromOtherStates",
                (delegate, connection) -> delegate.updateTriggerStatesFromOtherStates(connection, "A", "B", "C"), 3),
            arguments(
                "selectMisfiredTriggers", (delegate, connection) -> delegate.selectMisfiredTriggers(connection, 10L),
                triggerKeys),
            arguments(
                "selectMisfiredTriggersInState",
                (delegate, connection) -> delegate.selectMisfiredTriggersInState(connection, "WAITING", 10L),
                triggerKeys),
            arguments(
                "hasMisfiredTriggersInState",
                (delegate, connection) -> delegate.hasMisfiredTriggersInState(
                    connection, "WAITING", 10L, 5, triggerKeys),
                Boolean.TRUE),
            arguments(
                "countMisfiredTriggersInState",
                (delegate, connection) -> delegate.countMisfiredTriggersInState(connection, "WAITING", 10L), 2),
            arguments(
                "selectMisfiredTriggersInGroupInState",
                (delegate, connection) -> delegate.selectMisfiredTriggersInGroupInState(
                    connection, GROUP_NAME, "WAITING", 10L),
                triggerKeys),
            arguments(
                "selectTriggersForRecoveringJobs",
                (delegate, connection) -> delegate.selectTriggersForRecoveringJobs(connection), operableTriggers),
            arguments(
                "deleteFiredTriggers(instanceId)",
                (delegate, connection) -> delegate.deleteFiredTriggers(connection, INSTANCE_ID), 4),
            arguments(
                "insertJobDetail", (delegate, connection) -> delegate.insertJobDetail(connection, JOB_DETAIL), 1),
            arguments(
                "updateJobDetail", (delegate, connection) -> delegate.updateJobDetail(connection, JOB_DETAIL), 1),
            arguments("deleteJobDetail", (delegate, connection) -> delegate.deleteJobDetail(connection, JOB_KEY), 1),
            arguments(
                "isJobNonConcurrent", (delegate, connection) -> delegate.isJobNonConcurrent(connection, JOB_KEY),
                Boolean.TRUE),
            arguments("jobExists", (delegate, connection) -> delegate.jobExists(connection, JOB_KEY), Boolean.TRUE),
            arguments("updateJobData", (delegate, connection) -> delegate.updateJobData(connection, JOB_DETAIL), 1),
            arguments(
                "selectJobDetail",
                (delegate, connection) -> delegate.selectJobDetail(connection, JOB_KEY, CLASS_LOAD_HELPER), JOB_DETAIL),
            arguments(
                "selectJobsInGroup",
                (delegate, connection) -> delegate.selectJobsInGroup(connection, JOB_KEY_GROUP_MATCHER),
                Set.of(JOB_KEY)));
    }

    static Stream<Arguments> triggerCalls() {
        List<TriggerKey> triggerKeys = List.of(TRIGGER_KEY);
        List<OperableTrigger> operableTriggers = List.of(TRIGGER);

        return Stream.of(
            arguments(
                "insertTrigger",
                (delegate, connection) -> delegate.insertTrigger(connection, TRIGGER, "WAITING", JOB_DETAIL), 1),
            arguments(
                "updateTrigger",
                (delegate, connection) -> delegate.updateTrigger(connection, TRIGGER, "WAITING", JOB_DETAIL), 1),
            arguments(
                "triggerExists", (delegate, connection) -> delegate.triggerExists(connection, TRIGGER_KEY),
                Boolean.TRUE),
            arguments(
                "updateTriggerState",
                (delegate, connection) -> delegate.updateTriggerState(connection, TRIGGER_KEY, "PAUSED"), 1),
            arguments(
                "updateTriggerStateFromOtherState",
                (delegate, connection) -> delegate.updateTriggerStateFromOtherState(
                    connection, TRIGGER_KEY, "A", "B"),
                1),
            arguments(
                "updateTriggerStateFromOtherStates",
                (delegate, connection) -> delegate.updateTriggerStateFromOtherStates(
                    connection, TRIGGER_KEY, "A", "B", "C", "D"),
                1),
            arguments(
                "updateTriggerGroupStateFromOtherStates",
                (delegate, connection) -> delegate.updateTriggerGroupStateFromOtherStates(
                    connection, TRIGGER_KEY_GROUP_MATCHER, "A", "B", "C", "D"),
                2),
            arguments(
                "updateTriggerGroupStateFromOtherState",
                (delegate, connection) -> delegate.updateTriggerGroupStateFromOtherState(
                    connection, TRIGGER_KEY_GROUP_MATCHER, "A", "B"),
                2),
            arguments(
                "updateTriggerStatesForJob",
                (delegate, connection) -> delegate.updateTriggerStatesForJob(connection, JOB_KEY, "PAUSED"), 2),
            arguments(
                "updateTriggerStatesForJobFromOtherState",
                (delegate, connection) -> delegate.updateTriggerStatesForJobFromOtherState(
                    connection, JOB_KEY, "A", "B"),
                2),
            arguments("deleteTrigger", (delegate, connection) -> delegate.deleteTrigger(connection, TRIGGER_KEY), 1),
            arguments(
                "selectNumTriggersForJob",
                (delegate, connection) -> delegate.selectNumTriggersForJob(connection, JOB_KEY), 6),
            arguments(
                "selectJobForTrigger",
                (delegate, connection) -> delegate.selectJobForTrigger(connection, CLASS_LOAD_HELPER, TRIGGER_KEY),
                JOB_DETAIL),
            arguments(
                "selectJobForTrigger(loadJobClass)",
                (delegate, connection) -> delegate.selectJobForTrigger(
                    connection, CLASS_LOAD_HELPER, TRIGGER_KEY, false),
                JOB_DETAIL),
            arguments(
                "selectTriggersForJob", (delegate, connection) -> delegate.selectTriggersForJob(connection, JOB_KEY),
                operableTriggers),
            arguments(
                "selectTriggersForCalendar",
                (delegate, connection) -> delegate.selectTriggersForCalendar(connection, CALENDAR_NAME),
                operableTriggers),
            arguments("selectTrigger", (delegate, connection) -> delegate.selectTrigger(connection, TRIGGER_KEY),
                TRIGGER),
            arguments(
                "selectTriggerJobDataMap",
                (delegate, connection) -> delegate.selectTriggerJobDataMap(connection, "trigger", GROUP_NAME),
                new JobDataMap()),
            arguments(
                "selectTriggerState", (delegate, connection) -> delegate.selectTriggerState(connection, TRIGGER_KEY),
                "WAITING"),
            arguments(
                "selectTriggerStatus",
                (delegate, connection) -> delegate.selectTriggerStatus(connection, TRIGGER_KEY),
                new TriggerStatus("WAITING", new Date(0))),
            arguments(
                "selectTriggerGroups(matcher)",
                (delegate, connection) -> delegate.selectTriggerGroups(connection, TRIGGER_KEY_GROUP_MATCHER),
                List.of(GROUP_NAME)),
            arguments(
                "selectTriggersInGroup",
                (delegate, connection) -> delegate.selectTriggersInGroup(connection, TRIGGER_KEY_GROUP_MATCHER),
                Set.of(TRIGGER_KEY)),
            arguments(
                "selectTriggersInState",
                (delegate, connection) -> delegate.selectTriggersInState(connection, "PAUSED"), triggerKeys),
            arguments(
                "insertPausedTriggerGroup",
                (delegate, connection) -> delegate.insertPausedTriggerGroup(connection, GROUP_NAME), 1),
            arguments(
                "deletePausedTriggerGroup(name)",
                (delegate, connection) -> delegate.deletePausedTriggerGroup(connection, GROUP_NAME), 1),
            arguments(
                "deletePausedTriggerGroup(matcher)",
                (delegate, connection) -> delegate.deletePausedTriggerGroup(connection, TRIGGER_KEY_GROUP_MATCHER), 1),
            arguments(
                "isTriggerGroupPaused",
                (delegate, connection) -> delegate.isTriggerGroupPaused(connection, GROUP_NAME), Boolean.TRUE),
            arguments(
                "isExistingTriggerGroup",
                (delegate, connection) -> delegate.isExistingTriggerGroup(connection, GROUP_NAME), Boolean.TRUE));
    }

    static Stream<Arguments> calendarFiredTriggerAndSchedulerStateCalls() {
        List<TriggerKey> triggerKeys = List.of(TRIGGER_KEY);

        FiredTriggerRecord firedTriggerRecord = new FiredTriggerRecord();
        SchedulerStateRecord schedulerStateRecord = new SchedulerStateRecord();

        return Stream.of(
            arguments(
                "insertCalendar",
                (delegate, connection) -> delegate.insertCalendar(connection, CALENDAR_NAME, CALENDAR), 1),
            arguments(
                "updateCalendar",
                (delegate, connection) -> delegate.updateCalendar(connection, CALENDAR_NAME, CALENDAR), 1),
            arguments(
                "calendarExists", (delegate, connection) -> delegate.calendarExists(connection, CALENDAR_NAME),
                Boolean.TRUE),
            arguments(
                "selectCalendar", (delegate, connection) -> delegate.selectCalendar(connection, CALENDAR_NAME),
                CALENDAR),
            arguments(
                "calendarIsReferenced",
                (delegate, connection) -> delegate.calendarIsReferenced(connection, CALENDAR_NAME), Boolean.TRUE),
            arguments(
                "deleteCalendar", (delegate, connection) -> delegate.deleteCalendar(connection, CALENDAR_NAME), 1),
            arguments(
                "selectTriggerForFireTime",
                (delegate, connection) -> delegate.selectTriggerForFireTime(connection, 10L), TRIGGER_KEY),
            arguments(
                "selectTriggerToAcquire",
                (delegate, connection) -> delegate.selectTriggerToAcquire(connection, 20L, 10L), triggerKeys),
            arguments(
                "selectTriggerToAcquire(maxCount)",
                (delegate, connection) -> delegate.selectTriggerToAcquire(connection, 20L, 10L, 3), triggerKeys),
            arguments(
                "insertFiredTrigger",
                (delegate, connection) -> delegate.insertFiredTrigger(connection, TRIGGER, "ACQUIRED", JOB_DETAIL), 1),
            arguments(
                "updateFiredTrigger",
                (delegate, connection) -> delegate.updateFiredTrigger(connection, TRIGGER, "EXECUTING", JOB_DETAIL),
                1),
            arguments(
                "selectFiredTriggerRecords",
                (delegate, connection) -> delegate.selectFiredTriggerRecords(connection, "trigger", GROUP_NAME),
                List.of(firedTriggerRecord)),
            arguments(
                "selectFiredTriggerRecordsByJob",
                (delegate, connection) -> delegate.selectFiredTriggerRecordsByJob(connection, "job", GROUP_NAME),
                List.of(firedTriggerRecord)),
            arguments(
                "selectInstancesFiredTriggerRecords",
                (delegate, connection) -> delegate.selectInstancesFiredTriggerRecords(connection, INSTANCE_ID),
                List.of(firedTriggerRecord)),
            arguments(
                "deleteFiredTrigger", (delegate, connection) -> delegate.deleteFiredTrigger(connection, "entry-1"), 1),
            arguments(
                "selectJobExecutionCount",
                (delegate, connection) -> delegate.selectJobExecutionCount(connection, JOB_KEY), 2),
            arguments(
                "insertSchedulerState",
                (delegate, connection) -> delegate.insertSchedulerState(connection, INSTANCE_ID, 100L, 7500L), 1),
            arguments(
                "deleteSchedulerState",
                (delegate, connection) -> delegate.deleteSchedulerState(connection, INSTANCE_ID), 1),
            arguments(
                "updateSchedulerState",
                (delegate, connection) -> delegate.updateSchedulerState(connection, INSTANCE_ID, 100L), 1),
            arguments(
                "selectSchedulerStateRecords",
                (delegate, connection) -> delegate.selectSchedulerStateRecords(connection, INSTANCE_ID),
                List.of(schedulerStateRecord)));
    }

    private static Arguments arguments(String name, DelegateCall delegateCall, Object expectedResult) {
        return Arguments.of(Named.of(name, delegateCall), expectedResult);
    }

    private static List<Method> driverDelegateMethods() {
        return Arrays.stream(DriverDelegate.class.getMethods())
            .filter(method -> !Modifier.isStatic(method.getModifiers()) && !method.isSynthetic())
            .sorted(Comparator.comparing(MultiTenantDriverDelegateTest::describe))
            .toList();
    }

    private static Method findImplementationMethod(Method interfaceMethod) {
        try {
            return MultiTenantDriverDelegate.class.getMethod(
                interfaceMethod.getName(), interfaceMethod.getParameterTypes());
        } catch (NoSuchMethodException noSuchMethodException) {
            throw new IllegalStateException(noSuchMethodException);
        }
    }

    private static boolean takesConnection(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();

        return parameterTypes.length > 0 && parameterTypes[0] == Connection.class;
    }

    private static String describe(Method method) {
        String parameterTypeNames = Arrays.stream(method.getParameterTypes())
            .map(Class::getSimpleName)
            .collect(Collectors.joining(", "));

        return method.getName() + "(" + parameterTypeNames + ")";
    }

    private Object[] createArguments(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];

        for (int index = 0; index < parameterTypes.length; index++) {
            arguments[index] = createArgument(parameterTypes[index], index);
        }

        return arguments;
    }

    private Object createArgument(Class<?> parameterType, int index) {
        if (parameterType == Connection.class) {
            return connection;
        }

        if (parameterType == boolean.class) {
            return true;
        }

        if (parameterType == int.class) {
            return index + 10;
        }

        if (parameterType == long.class) {
            return index + 1_000L;
        }

        if (parameterType == String.class) {
            return "argument" + index;
        }

        if (parameterType == JobKey.class) {
            return JobKey.jobKey("job" + index, GROUP_NAME);
        }

        if (parameterType == TriggerKey.class) {
            return TriggerKey.triggerKey("trigger" + index, GROUP_NAME);
        }

        if (parameterType == GroupMatcher.class) {
            return GroupMatcher.groupEquals(GROUP_NAME + index);
        }

        if (parameterType == List.class) {
            return new ArrayList<>();
        }

        return mock(parameterType);
    }

    private static Object createReturnValue(Class<?> returnType) {
        if (returnType == void.class) {
            return null;
        }

        if (returnType == boolean.class) {
            return true;
        }

        if (returnType == int.class) {
            return 42;
        }

        if (returnType == long.class) {
            return 4_242L;
        }

        if (returnType == String.class) {
            return "result";
        }

        return mock(returnType);
    }

    private static Object invoke(Object target, Method method, Object[] arguments) throws Throwable {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException invocationTargetException) {
            throw invocationTargetException.getCause();
        }
    }

    @FunctionalInterface
    interface DelegateCall {

        Object invoke(DriverDelegate delegate, Connection connection) throws Exception;
    }
}
