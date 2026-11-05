/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.tenant;

import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.DriverDelegate;
import org.quartz.impl.jdbcjobstore.FiredTriggerRecord;
import org.quartz.impl.jdbcjobstore.NoSuchDelegateException;
import org.quartz.impl.jdbcjobstore.SchedulerStateRecord;
import org.quartz.impl.jdbcjobstore.TriggerStatus;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.OperableTrigger;
import org.quartz.utils.Key;
import org.slf4j.Logger;

/**
 * Multi-tenant Quartz scheduler driver delegate that sets PostgreSQL search_path per tenant.
 *
 * <p>
 * Every call is forwarded to the wrapped, already initialized delegate (e.g. {@code PostgreSQLDelegate}) with the
 * connection's search_path switched to {@code public}, where the Quartz tables live, and switched back to the current
 * tenant's schema afterwards. Checked exceptions the wrapped delegate throws are propagated unchanged, so that
 * {@code JobStoreSupport} can handle them as it does for a plain delegate.
 *
 * <p>
 * The class deliberately implements {@link DriverDelegate} instead of extending {@code StdJDBCDelegate}: an inherited
 * {@code StdJDBCDelegate} was never initialized (its scheduler name is {@code null} and its table prefix the default),
 * so any method it served instead of the wrapper silently queried {@code SCHED_NAME = 'null'}. Implementing the
 * interface makes the compiler require every abstract method, and {@code MultiTenantDriverDelegateTest} checks that the
 * default methods are overridden as well.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MultiTenantDriverDelegate implements DriverDelegate {

    private static final String PUBLIC_SCHEMA = "public";

    private final DriverDelegate delegate;

    @SuppressFBWarnings("EI")
    public MultiTenantDriverDelegate(DriverDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void initialize(
        Logger logger, String tablePrefix, String schedName, String instanceId, ClassLoadHelper classLoadHelper,
        boolean useProperties, String initString) throws NoSuchDelegateException {

        delegate.initialize(logger, tablePrefix, schedName, instanceId, classLoadHelper, useProperties, initString);
    }

    @Override
    public boolean isUsingEnhancedStatements() {
        return delegate.isUsingEnhancedStatements();
    }

    @Override
    public void setUseEnhancedStatements(boolean useEnhancedStatements) {
        delegate.setUseEnhancedStatements(useEnhancedStatements);
    }

    @Override
    public int updateTriggerStatesFromOtherStates(Connection conn, String newState, String oldState1, String oldState2)
        throws SQLException {

        return execute(conn, (connection) -> delegate.updateTriggerStatesFromOtherStates(
            connection, newState, oldState1, oldState2));
    }

    @Override
    public List<TriggerKey> selectMisfiredTriggers(Connection conn, long ts) throws SQLException {
        return execute(conn, (connection) -> delegate.selectMisfiredTriggers(connection, ts));
    }

    @Override
    public List<TriggerKey> selectMisfiredTriggersInState(Connection conn, String state, long ts) throws SQLException {
        return execute(conn, (connection) -> delegate.selectMisfiredTriggersInState(connection, state, ts));
    }

    @Override
    public boolean hasMisfiredTriggersInState(
        Connection conn, String state1, long ts, int count, List<TriggerKey> resultList) throws SQLException {

        return execute(conn, (connection) -> delegate.hasMisfiredTriggersInState(
            connection, state1, ts, count, resultList));
    }

    @Override
    public int countMisfiredTriggersInState(Connection conn, String state1, long ts) throws SQLException {
        return execute(conn, (connection) -> delegate.countMisfiredTriggersInState(connection, state1, ts));
    }

    @Override
    public List<TriggerKey> selectMisfiredTriggersInGroupInState(
        Connection conn, String groupName, String state, long ts) throws SQLException {

        return execute(conn, (connection) -> delegate.selectMisfiredTriggersInGroupInState(
            connection, groupName, state, ts));
    }

    @Override
    public List<OperableTrigger> selectTriggersForRecoveringJobs(Connection conn)
        throws SQLException, IOException, ClassNotFoundException {

        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.selectTriggersForRecoveringJobs(publicSearchPathScope.connection());
        }
    }

    @Override
    public int deleteFiredTriggers(Connection conn) throws SQLException {
        return execute(conn, delegate::deleteFiredTriggers);
    }

    @Override
    public int deleteFiredTriggers(Connection conn, String instanceId) throws SQLException {
        return execute(conn, (connection) -> delegate.deleteFiredTriggers(connection, instanceId));
    }

    @Override
    public int insertJobDetail(Connection conn, JobDetail job) throws IOException, SQLException {
        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.insertJobDetail(publicSearchPathScope.connection(), job);
        }
    }

    @Override
    public int updateJobDetail(Connection conn, JobDetail job) throws IOException, SQLException {
        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.updateJobDetail(publicSearchPathScope.connection(), job);
        }
    }

    @Override
    public List<TriggerKey> selectTriggerKeysForJob(Connection conn, JobKey jobKey) throws SQLException {
        return execute(conn, (connection) -> delegate.selectTriggerKeysForJob(connection, jobKey));
    }

    @Override
    public int deleteJobDetail(Connection conn, JobKey jobKey) throws SQLException {
        return execute(conn, (connection) -> delegate.deleteJobDetail(connection, jobKey));
    }

    @Override
    public boolean isJobNonConcurrent(Connection conn, JobKey jobKey) throws SQLException {
        return execute(conn, (connection) -> delegate.isJobNonConcurrent(connection, jobKey));
    }

    @Override
    public boolean jobExists(Connection conn, JobKey jobKey) throws SQLException {
        return execute(conn, (connection) -> delegate.jobExists(connection, jobKey));
    }

    @Override
    public int updateJobData(Connection conn, JobDetail job) throws IOException, SQLException {
        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.updateJobData(publicSearchPathScope.connection(), job);
        }
    }

    @Override
    public JobDetail selectJobDetail(Connection conn, JobKey jobKey, ClassLoadHelper loadHelper)
        throws ClassNotFoundException, IOException, SQLException {

        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.selectJobDetail(publicSearchPathScope.connection(), jobKey, loadHelper);
        }
    }

    @Override
    public List<JobDetail> selectJobDetails(
        Connection conn, GroupMatcher<JobKey> matcher, ClassLoadHelper loadHelper)
        throws ClassNotFoundException, IOException, SQLException {

        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.selectJobDetails(publicSearchPathScope.connection(), matcher, loadHelper);
        }
    }

    @Override
    public int selectNumJobs(Connection conn) throws SQLException {
        return execute(conn, delegate::selectNumJobs);
    }

    @Override
    public List<String> selectJobGroups(Connection conn) throws SQLException {
        return execute(conn, delegate::selectJobGroups);
    }

    @Override
    public Set<JobKey> selectJobsInGroup(Connection conn, GroupMatcher<JobKey> matcher) throws SQLException {
        return execute(conn, (connection) -> delegate.selectJobsInGroup(connection, matcher));
    }

    @Override
    public int insertTrigger(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail)
        throws SQLException, IOException {

        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.insertTrigger(publicSearchPathScope.connection(), trigger, state, jobDetail);
        }
    }

    @Override
    public int updateTrigger(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail)
        throws SQLException, IOException {

        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.updateTrigger(publicSearchPathScope.connection(), trigger, state, jobDetail);
        }
    }

    @Override
    public boolean triggerExists(Connection conn, TriggerKey triggerKey) throws SQLException {
        return execute(conn, (connection) -> delegate.triggerExists(connection, triggerKey));
    }

    @Override
    public int updateTriggerState(Connection conn, TriggerKey triggerKey, String state) throws SQLException {
        return execute(conn, (connection) -> delegate.updateTriggerState(connection, triggerKey, state));
    }

    @Override
    public int updateTriggerStateFromOtherState(
        Connection conn, TriggerKey triggerKey, String newState, String oldState) throws SQLException {

        return execute(conn, (connection) -> delegate.updateTriggerStateFromOtherState(
            connection, triggerKey, newState, oldState));
    }

    @Override
    public int updateTriggerStateFromOtherStates(
        Connection conn, TriggerKey triggerKey, String newState, String oldState1, String oldState2, String oldState3)
        throws SQLException {

        return execute(conn, (connection) -> delegate.updateTriggerStateFromOtherStates(
            connection, triggerKey, newState, oldState1, oldState2, oldState3));
    }

    @Override
    public int updateTriggerGroupStateFromOtherStates(
        Connection conn, GroupMatcher<TriggerKey> matcher, String newState, String oldState1, String oldState2,
        String oldState3) throws SQLException {

        return execute(conn, (connection) -> delegate.updateTriggerGroupStateFromOtherStates(
            connection, matcher, newState, oldState1, oldState2, oldState3));
    }

    @Override
    public int updateTriggerGroupStateFromOtherState(
        Connection conn, GroupMatcher<TriggerKey> matcher, String newState, String oldState) throws SQLException {

        return execute(conn, (connection) -> delegate.updateTriggerGroupStateFromOtherState(
            connection, matcher, newState, oldState));
    }

    @Override
    public int updateTriggerStatesForJob(Connection conn, JobKey jobKey, String state) throws SQLException {
        return execute(conn, (connection) -> delegate.updateTriggerStatesForJob(connection, jobKey, state));
    }

    @Override
    public int updateTriggerStatesForJobFromOtherState(Connection conn, JobKey jobKey, String state, String oldState)
        throws SQLException {

        return execute(conn, (connection) -> delegate.updateTriggerStatesForJobFromOtherState(
            connection, jobKey, state, oldState));
    }

    @Override
    public int deleteTrigger(Connection conn, TriggerKey triggerKey) throws SQLException {
        return execute(conn, (connection) -> delegate.deleteTrigger(connection, triggerKey));
    }

    @Override
    public int selectNumTriggersForJob(Connection conn, JobKey jobKey) throws SQLException {
        return execute(conn, (connection) -> delegate.selectNumTriggersForJob(connection, jobKey));
    }

    @Override
    public JobDetail selectJobForTrigger(Connection conn, ClassLoadHelper loadHelper, TriggerKey triggerKey)
        throws ClassNotFoundException, SQLException, IOException {

        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.selectJobForTrigger(publicSearchPathScope.connection(), loadHelper, triggerKey);
        }
    }

    @Override
    public JobDetail selectJobForTrigger(
        Connection conn, ClassLoadHelper loadHelper, TriggerKey triggerKey, boolean loadJobClass)
        throws ClassNotFoundException, SQLException, IOException {

        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.selectJobForTrigger(
                publicSearchPathScope.connection(), loadHelper, triggerKey, loadJobClass);
        }
    }

    @Override
    public List<OperableTrigger> selectTriggersForJob(Connection conn, JobKey jobKey)
        throws SQLException, ClassNotFoundException, IOException, JobPersistenceException {

        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.selectTriggersForJob(publicSearchPathScope.connection(), jobKey);
        }
    }

    @Override
    public List<OperableTrigger> selectTriggersForCalendar(Connection conn, String calName)
        throws SQLException, ClassNotFoundException, IOException, JobPersistenceException {

        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.selectTriggersForCalendar(publicSearchPathScope.connection(), calName);
        }
    }

    @Override
    public OperableTrigger selectTrigger(Connection conn, TriggerKey triggerKey)
        throws SQLException, ClassNotFoundException, IOException, JobPersistenceException {

        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.selectTrigger(publicSearchPathScope.connection(), triggerKey);
        }
    }

    @Override
    public JobDataMap selectTriggerJobDataMap(Connection conn, String triggerName, String groupName)
        throws SQLException, ClassNotFoundException, IOException {

        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.selectTriggerJobDataMap(publicSearchPathScope.connection(), triggerName, groupName);
        }
    }

    @Override
    public String selectTriggerState(Connection conn, TriggerKey triggerKey) throws SQLException {
        return execute(conn, (connection) -> delegate.selectTriggerState(connection, triggerKey));
    }

    @Override
    public TriggerStatus selectTriggerStatus(Connection conn, TriggerKey triggerKey) throws SQLException {
        return execute(conn, (connection) -> delegate.selectTriggerStatus(connection, triggerKey));
    }

    @Override
    public int selectNumTriggers(Connection conn) throws SQLException {
        return execute(conn, delegate::selectNumTriggers);
    }

    @Override
    public List<String> selectTriggerGroups(Connection conn) throws SQLException {
        return execute(conn, delegate::selectTriggerGroups);
    }

    @Override
    public List<String> selectTriggerGroups(Connection conn, GroupMatcher<TriggerKey> matcher) throws SQLException {
        return execute(conn, (connection) -> delegate.selectTriggerGroups(connection, matcher));
    }

    @Override
    public Set<TriggerKey> selectTriggersInGroup(Connection conn, GroupMatcher<TriggerKey> matcher)
        throws SQLException {

        return execute(conn, (connection) -> delegate.selectTriggersInGroup(connection, matcher));
    }

    @Override
    public List<TriggerKey> selectTriggersInState(Connection conn, String state) throws SQLException {
        return execute(conn, (connection) -> delegate.selectTriggersInState(connection, state));
    }

    @Override
    public int insertPausedTriggerGroup(Connection conn, String groupName) throws SQLException {
        return execute(conn, (connection) -> delegate.insertPausedTriggerGroup(connection, groupName));
    }

    @Override
    public int deletePausedTriggerGroup(Connection conn, String groupName) throws SQLException {
        return execute(conn, (connection) -> delegate.deletePausedTriggerGroup(connection, groupName));
    }

    @Override
    public int deletePausedTriggerGroup(Connection conn, GroupMatcher<TriggerKey> matcher) throws SQLException {
        return execute(conn, (connection) -> delegate.deletePausedTriggerGroup(connection, matcher));
    }

    @Override
    public int deleteAllPausedTriggerGroups(Connection conn) throws SQLException {
        return execute(conn, delegate::deleteAllPausedTriggerGroups);
    }

    @Override
    public boolean isTriggerGroupPaused(Connection conn, String groupName) throws SQLException {
        return execute(conn, (connection) -> delegate.isTriggerGroupPaused(connection, groupName));
    }

    @Override
    public Set<String> selectPausedTriggerGroups(Connection conn) throws SQLException {
        return execute(conn, delegate::selectPausedTriggerGroups);
    }

    @Override
    public boolean isExistingTriggerGroup(Connection conn, String groupName) throws SQLException {
        return execute(conn, (connection) -> delegate.isExistingTriggerGroup(connection, groupName));
    }

    @Override
    public int insertCalendar(Connection conn, String calendarName, Calendar calendar)
        throws IOException, SQLException {

        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.insertCalendar(publicSearchPathScope.connection(), calendarName, calendar);
        }
    }

    @Override
    public int updateCalendar(Connection conn, String calendarName, Calendar calendar)
        throws IOException, SQLException {

        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.updateCalendar(publicSearchPathScope.connection(), calendarName, calendar);
        }
    }

    @Override
    public boolean calendarExists(Connection conn, String calendarName) throws SQLException {
        return execute(conn, (connection) -> delegate.calendarExists(connection, calendarName));
    }

    @Override
    public Calendar selectCalendar(Connection conn, String calendarName)
        throws ClassNotFoundException, IOException, SQLException {

        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.selectCalendar(publicSearchPathScope.connection(), calendarName);
        }
    }

    @Override
    public boolean calendarIsReferenced(Connection conn, String calendarName) throws SQLException {
        return execute(conn, (connection) -> delegate.calendarIsReferenced(connection, calendarName));
    }

    @Override
    public int deleteCalendar(Connection conn, String calendarName) throws SQLException {
        return execute(conn, (connection) -> delegate.deleteCalendar(connection, calendarName));
    }

    @Override
    public int selectNumCalendars(Connection conn) throws SQLException {
        return execute(conn, delegate::selectNumCalendars);
    }

    @Override
    public List<String> selectCalendars(Connection conn) throws SQLException {
        return execute(conn, delegate::selectCalendars);
    }

    @Override
    @SuppressWarnings("deprecation")
    public long selectNextFireTime(Connection conn) throws SQLException {
        return execute(conn, delegate::selectNextFireTime);
    }

    @Override
    public Key<?> selectTriggerForFireTime(Connection conn, long fireTime) throws SQLException {
        return execute(conn, (connection) -> delegate.selectTriggerForFireTime(connection, fireTime));
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<TriggerKey> selectTriggerToAcquire(Connection conn, long noLaterThan, long noEarlierThan)
        throws SQLException {

        return execute(conn, (connection) -> delegate.selectTriggerToAcquire(connection, noLaterThan, noEarlierThan));
    }

    @Override
    public List<TriggerKey> selectTriggerToAcquire(Connection conn, long noLaterThan, long noEarlierThan, int maxCount)
        throws SQLException {

        return execute(conn, (connection) -> delegate.selectTriggerToAcquire(
            connection, noLaterThan, noEarlierThan, maxCount));
    }

    @Override
    public List<OperableTrigger> getTriggersByJobAndTriggerGroup(
        Connection conn, GroupMatcher<JobKey> jobMatcher, GroupMatcher<TriggerKey> triggerMatcher)
        throws SQLException, ClassNotFoundException, IOException, JobPersistenceException {

        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return delegate.getTriggersByJobAndTriggerGroup(
                publicSearchPathScope.connection(), jobMatcher, triggerMatcher);
        }
    }

    @Override
    public int insertFiredTrigger(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail)
        throws SQLException {

        return execute(conn, (connection) -> delegate.insertFiredTrigger(connection, trigger, state, jobDetail));
    }

    @Override
    public int updateFiredTrigger(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail)
        throws SQLException {

        return execute(conn, (connection) -> delegate.updateFiredTrigger(connection, trigger, state, jobDetail));
    }

    @Override
    public List<FiredTriggerRecord> selectFiredTriggerRecords(Connection conn, String triggerName, String groupName)
        throws SQLException {

        return execute(conn, (connection) -> delegate.selectFiredTriggerRecords(connection, triggerName, groupName));
    }

    @Override
    public List<FiredTriggerRecord> selectFiredTriggerRecordsByJob(Connection conn, String jobName, String groupName)
        throws SQLException {

        return execute(conn, (connection) -> delegate.selectFiredTriggerRecordsByJob(connection, jobName, groupName));
    }

    @Override
    public List<FiredTriggerRecord> selectInstancesFiredTriggerRecords(Connection conn, String instanceName)
        throws SQLException {

        return execute(conn, (connection) -> delegate.selectInstancesFiredTriggerRecords(connection, instanceName));
    }

    @Override
    public Set<String> selectFiredTriggerInstanceNames(Connection conn) throws SQLException {
        return execute(conn, delegate::selectFiredTriggerInstanceNames);
    }

    @Override
    public int deleteFiredTrigger(Connection conn, String entryId) throws SQLException {
        return execute(conn, (connection) -> delegate.deleteFiredTrigger(connection, entryId));
    }

    @Override
    public int selectJobExecutionCount(Connection conn, JobKey jobKey) throws SQLException {
        return execute(conn, (connection) -> delegate.selectJobExecutionCount(connection, jobKey));
    }

    @Override
    public int insertSchedulerState(Connection conn, String instanceId, long checkInTime, long interval)
        throws SQLException {

        return execute(
            conn, (connection) -> delegate.insertSchedulerState(connection, instanceId, checkInTime, interval));
    }

    @Override
    public int deleteSchedulerState(Connection conn, String instanceId) throws SQLException {
        return execute(conn, (connection) -> delegate.deleteSchedulerState(connection, instanceId));
    }

    @Override
    public int updateSchedulerState(Connection conn, String instanceId, long checkInTime) throws SQLException {
        return execute(conn, (connection) -> delegate.updateSchedulerState(connection, instanceId, checkInTime));
    }

    @Override
    public List<SchedulerStateRecord> selectSchedulerStateRecords(Connection conn, String instanceId)
        throws SQLException {

        return execute(conn, (connection) -> delegate.selectSchedulerStateRecords(connection, instanceId));
    }

    @Override
    public void clearData(Connection conn) throws SQLException {
        execute(conn, (connection) -> {
            delegate.clearData(connection);

            return null;
        });
    }

    private static <T> T execute(Connection conn, SqlDelegateCall<T> sqlDelegateCall) throws SQLException {
        try (PublicSearchPathScope publicSearchPathScope = PublicSearchPathScope.enter(conn)) {
            return sqlDelegateCall.apply(publicSearchPathScope.connection());
        }
    }

    /**
     * Sets the PostgreSQL search_path to the specified schema.
     *
     * <p>
     * <b>Security Note:</b> The SQL injection suppression is for schema name injection which cannot be parameterized in
     * PostgreSQL. The search_path is set using a tenant ID that is validated to contain only alphanumeric characters,
     * underscores, and hyphens. This is a PostgreSQL limitation for DDL/schema operations, not a security
     * vulnerability.
     */
    @SuppressFBWarnings("SQL_INJECTION_JDBC")
    private static void setSearchPath(Connection connection, String databaseSchemaName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + databaseSchemaName);
        }
    }

    @FunctionalInterface
    private interface SqlDelegateCall<T> {

        T apply(Connection connection) throws SQLException;
    }

    /**
     * Switches a connection's search_path to {@code public} when entered and back to the current tenant's schema when
     * closed. Used with try-with-resources, so that an exception thrown by the delegate stays the primary exception and
     * a failure to restore the tenant schema is attached to it as suppressed, instead of replacing it.
     */
    private static final class PublicSearchPathScope implements AutoCloseable {

        private final Connection connection;

        private PublicSearchPathScope(Connection connection) {
            this.connection = connection;
        }

        static PublicSearchPathScope enter(Connection connection) throws SQLException {
            setSearchPath(connection, PUBLIC_SCHEMA);

            return new PublicSearchPathScope(connection);
        }

        Connection connection() {
            return connection;
        }

        @Override
        public void close() throws SQLException {
            setSearchPath(connection, TenantContext.getCurrentDatabaseSchema());
        }
    }
}
