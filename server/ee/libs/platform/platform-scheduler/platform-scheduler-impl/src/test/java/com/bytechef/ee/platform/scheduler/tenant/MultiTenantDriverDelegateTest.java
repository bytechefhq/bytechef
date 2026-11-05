/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
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
    private static final String PUBLIC_SEARCH_PATH = "SET search_path TO public";
    private static final OperableTrigger TRIGGER = mock(OperableTrigger.class);
    private static final TriggerKey TRIGGER_KEY = TriggerKey.triggerKey("trigger", GROUP_NAME);
    private static final GroupMatcher<TriggerKey> TRIGGER_KEY_GROUP_MATCHER =
        GroupMatcher.triggerGroupEquals(GROUP_NAME);

    private final Connection connection = mock(Connection.class);
    private final DriverDelegate driverDelegate = mock(DriverDelegate.class);
    private final MultiTenantDriverDelegate multiTenantDriverDelegate = new MultiTenantDriverDelegate(driverDelegate);
    private final List<Statement> statements = new ArrayList<>();

    @BeforeEach
    @SuppressFBWarnings(value = {
        "OBL", "ODR"
    }, justification = "Statements are Mockito mocks")
    void beforeEach() throws SQLException {
        when(connection.createStatement()).thenAnswer(invocation -> {
            Statement statement = mock(Statement.class);

            statements.add(statement);

            return statement;
        });
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @ParameterizedTest
    @MethodSource({
        "misfireAndJobCalls", "triggerCalls", "calendarFiredTriggerAndSchedulerStateCalls"
    })
    void testDelegatedCallRunsOnPublicSchemaAndRestoresTenantSchema(DelegateCall delegateCall, Object expectedResult)
        throws Exception {

        delegateCall.invoke(doReturn(expectedResult).when(driverDelegate), connection);

        TenantContext.setCurrentTenantId("000002");

        Object result = delegateCall.invoke(multiTenantDriverDelegate, connection);

        assertThat(result).isEqualTo(expectedResult);
        assertThat(statements).hasSize(2);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("000002");

        InOrder inOrder = inOrder(statements.get(0), driverDelegate, statements.get(1));

        inOrder.verify(statements.get(0))
            .execute(PUBLIC_SEARCH_PATH);
        inOrder.verify(statements.get(0))
            .close();

        delegateCall.invoke(inOrder.verify(driverDelegate), connection);

        inOrder.verify(statements.get(1))
            .execute("SET search_path TO bytechef_000002");
        inOrder.verify(statements.get(1))
            .close();
    }

    @Test
    void testClearDataRunsOnPublicSchemaAndRestoresTenantSchema() throws Exception {
        TenantContext.setCurrentTenantId("000003");

        multiTenantDriverDelegate.clearData(connection);

        InOrder inOrder = inOrder(statements.get(0), driverDelegate, statements.get(1));

        inOrder.verify(statements.get(0))
            .execute(PUBLIC_SEARCH_PATH);
        inOrder.verify(driverDelegate)
            .clearData(connection);
        inOrder.verify(statements.get(1))
            .execute("SET search_path TO bytechef_000003");
    }

    @Test
    void testDefaultTenantRestoresPublicSchema() throws Exception {
        doReturn(true).when(driverDelegate)
            .jobExists(connection, JOB_KEY);

        assertThat(multiTenantDriverDelegate.jobExists(connection, JOB_KEY)).isTrue();

        assertThat(statements).hasSize(2);

        verify(statements.get(0)).execute(PUBLIC_SEARCH_PATH);
        verify(statements.get(1)).execute(PUBLIC_SEARCH_PATH);
    }

    @Test
    void testSearchPathFollowsTenantContextAcrossCalls() throws Exception {
        TenantContext.runWithTenantId("000004", () -> multiTenantDriverDelegate.jobExists(connection, JOB_KEY));
        TenantContext.runWithTenantId("000005", () -> multiTenantDriverDelegate.jobExists(connection, JOB_KEY));

        assertThat(statements).hasSize(4);

        verify(statements.get(0)).execute(PUBLIC_SEARCH_PATH);
        verify(statements.get(1)).execute("SET search_path TO bytechef_000004");
        verify(statements.get(2)).execute(PUBLIC_SEARCH_PATH);
        verify(statements.get(3)).execute("SET search_path TO bytechef_000005");

        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testTenantSchemaIsRestoredWhenDelegateThrowsSqlException() throws Exception {
        SQLException sqlException = new SQLException("deadlock detected");

        doThrow(sqlException).when(driverDelegate)
            .deleteTrigger(connection, TRIGGER_KEY);

        TenantContext.setCurrentTenantId("000002");

        assertThatThrownBy(() -> multiTenantDriverDelegate.deleteTrigger(connection, TRIGGER_KEY))
            .isSameAs(sqlException);

        assertThat(statements).hasSize(2);

        verify(statements.get(0)).execute(PUBLIC_SEARCH_PATH);
        verify(statements.get(1)).execute("SET search_path TO bytechef_000002");
    }

    @Test
    void testTenantSchemaIsRestoredWhenDelegateThrowsRuntimeException() throws Exception {
        IllegalStateException illegalStateException = new IllegalStateException("unexpected");

        doThrow(illegalStateException).when(driverDelegate)
            .selectTriggerState(connection, TRIGGER_KEY);

        TenantContext.setCurrentTenantId("000002");

        assertThatThrownBy(() -> multiTenantDriverDelegate.selectTriggerState(connection, TRIGGER_KEY))
            .isSameAs(illegalStateException);

        verify(statements.get(1)).execute("SET search_path TO bytechef_000002");
    }

    @Test
    void testTenantSchemaIsRestoredWhenDelegateThrowsCheckedSerializationException() throws Exception {
        IOException ioException = new IOException("cannot serialize job data");

        doThrow(ioException).when(driverDelegate)
            .insertJobDetail(connection, JOB_DETAIL);

        TenantContext.setCurrentTenantId("000002");

        assertThatThrownBy(() -> multiTenantDriverDelegate.insertJobDetail(connection, JOB_DETAIL))
            .satisfies(throwable -> assertThat(findCause(throwable, IOException.class)).isSameAs(ioException));

        assertThat(statements).hasSize(2);

        verify(statements.get(1)).execute("SET search_path TO bytechef_000002");
    }

    @Test
    @SuppressFBWarnings(value = {
        "OBL", "ODR"
    }, justification = "Statements are Mockito mocks")
    void testDelegateIsNotCalledWhenSwitchingToPublicSchemaFails() throws Exception {
        SQLException sqlException = new SQLException("connection closed");

        when(connection.createStatement()).thenThrow(sqlException);

        assertThatThrownBy(() -> multiTenantDriverDelegate.jobExists(connection, JOB_KEY))
            .isSameAs(sqlException);

        verifyNoInteractions(driverDelegate);
    }

    @Test
    @SuppressFBWarnings(value = {
        "OBL", "ODR"
    }, justification = "Statements are Mockito mocks")
    void testEveryCallUsesFreshStatementsThatAreClosed() throws Exception {
        multiTenantDriverDelegate.jobExists(connection, JOB_KEY);
        multiTenantDriverDelegate.triggerExists(connection, TRIGGER_KEY);

        assertThat(statements).hasSize(4);

        for (Statement statement : statements) {
            verify(statement).close();
        }

        verify(connection, times(4)).createStatement();
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

    private static Throwable findCause(Throwable throwable, Class<? extends Throwable> causeClass) {
        Throwable current = throwable;

        while (current != null && !causeClass.isInstance(current)) {
            current = current.getCause();
        }

        return current;
    }

    @FunctionalInterface
    interface DelegateCall {

        Object invoke(DriverDelegate delegate, Connection connection) throws Exception;
    }
}
