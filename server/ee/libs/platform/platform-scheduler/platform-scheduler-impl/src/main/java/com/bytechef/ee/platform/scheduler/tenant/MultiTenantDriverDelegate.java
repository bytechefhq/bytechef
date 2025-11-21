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
import org.quartz.impl.jdbcjobstore.SchedulerStateRecord;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;
import org.quartz.impl.jdbcjobstore.TriggerStatus;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.OperableTrigger;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("SQL_INJECTION_JDBC")
public class MultiTenantDriverDelegate extends StdJDBCDelegate {

    private final DriverDelegate delegate;

    @SuppressFBWarnings("EI")
    public MultiTenantDriverDelegate(DriverDelegate delegate) {
        this.delegate = delegate;
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
        return execute(conn, (connection) -> {
            try {
                return delegate.selectTriggersForRecoveringJobs(connection);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public int deleteFiredTriggers(Connection conn) throws SQLException {
        return execute(conn, super::deleteFiredTriggers);
    }

    @Override
    public int deleteFiredTriggers(Connection conn, String instanceId) throws SQLException {
        return execute(conn, (connection) -> delegate.deleteFiredTriggers(connection, instanceId));
    }

    @Override
    public int insertJobDetail(Connection conn, JobDetail job) throws IOException, SQLException {
        return execute(conn, (connection) -> {
            try {
                return delegate.insertJobDetail(connection, job);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public int updateJobDetail(Connection conn, JobDetail job) throws IOException, SQLException {
        return execute(conn, (connection) -> {
            try {
                return delegate.updateJobDetail(connection, job);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public List<TriggerKey> selectTriggerKeysForJob(Connection conn, JobKey jobKey) throws SQLException {
        return List.of();
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
        return execute(conn, (connection) -> {
            try {
                return delegate.updateJobData(connection, job);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public JobDetail selectJobDetail(Connection conn, JobKey jobKey, ClassLoadHelper loadHelper)
        throws ClassNotFoundException, IOException, SQLException {

        return execute(conn, (connection) -> {
            try {
                return delegate.selectJobDetail(connection, jobKey, loadHelper);
            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public int selectNumJobs(Connection conn) throws SQLException {
        return execute(conn, super::selectNumJobs);
    }

    @Override
    public List<String> selectJobGroups(Connection conn) throws SQLException {
        return execute(conn, super::selectJobGroups);
    }

    @Override
    public Set<JobKey> selectJobsInGroup(Connection conn, GroupMatcher<JobKey> matcher) throws SQLException {
        return execute(conn, (connection) -> delegate.selectJobsInGroup(connection, matcher));
    }

    @Override
    public int insertTrigger(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail)
        throws SQLException, IOException {

        return execute(conn, (connection) -> {
            try {
                return delegate.insertTrigger(connection, trigger, state, jobDetail);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public int updateTrigger(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail)
        throws SQLException, IOException {

        return execute(conn, (connection) -> {
            try {
                return delegate.updateTrigger(connection, trigger, state, jobDetail);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
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
        throws ClassNotFoundException, SQLException {
        return execute(conn, (connection) -> {
            try {
                return delegate.selectJobForTrigger(connection, loadHelper, triggerKey);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public JobDetail selectJobForTrigger(
        Connection conn, ClassLoadHelper loadHelper, TriggerKey triggerKey, boolean loadJobClass)
        throws ClassNotFoundException, SQLException {

        return execute(conn, (connection) -> {
            try {
                return delegate.selectJobForTrigger(connection, loadHelper, triggerKey, loadJobClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public List<OperableTrigger> selectTriggersForJob(Connection conn, JobKey jobKey)
        throws SQLException, ClassNotFoundException, IOException, JobPersistenceException {
        return execute(conn, (connection) -> {
            try {
                return delegate.selectTriggersForJob(connection, jobKey);
            } catch (ClassNotFoundException | IOException | JobPersistenceException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public List<OperableTrigger> selectTriggersForCalendar(Connection conn, String calName)
        throws SQLException, ClassNotFoundException, IOException, JobPersistenceException {
        return execute(conn, (connection) -> {
            try {
                return delegate.selectTriggersForCalendar(connection, calName);
            } catch (ClassNotFoundException | IOException | JobPersistenceException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public OperableTrigger selectTrigger(Connection conn, TriggerKey triggerKey)
        throws SQLException, ClassNotFoundException, IOException, JobPersistenceException {

        return execute(conn, (connection) -> {
            try {
                return delegate.selectTrigger(connection, triggerKey);
            } catch (ClassNotFoundException | IOException | JobPersistenceException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public JobDataMap selectTriggerJobDataMap(Connection conn, String triggerName, String groupName)
        throws SQLException, ClassNotFoundException, IOException {

        return execute(conn, (connection) -> {
            try {
                return delegate.selectTriggerJobDataMap(connection, triggerName, groupName);
            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException(e);
            }
        });
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
        return execute(conn, super::selectNumTriggers);
    }

    @Override
    public List<String> selectTriggerGroups(Connection conn) throws SQLException {
        return execute(conn, super::selectTriggerGroups);
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
        return execute(conn, super::deleteAllPausedTriggerGroups);
    }

    @Override
    public boolean isTriggerGroupPaused(Connection conn, String groupName) throws SQLException {
        return execute(conn, (connection) -> delegate.isTriggerGroupPaused(connection, groupName));
    }

    @Override
    public Set<String> selectPausedTriggerGroups(Connection conn) throws SQLException {
        return execute(conn, super::selectPausedTriggerGroups);
    }

    @Override
    public boolean isExistingTriggerGroup(Connection conn, String groupName) throws SQLException {
        return execute(conn, (connection) -> delegate.isExistingTriggerGroup(connection, groupName));
    }

    @Override
    public int insertCalendar(Connection conn, String calendarName, Calendar calendar)
        throws IOException, SQLException {
        return execute(conn, (connection) -> {
            try {
                return delegate.insertCalendar(connection, calendarName, calendar);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public int updateCalendar(Connection conn, String calendarName, Calendar calendar)
        throws IOException, SQLException {
        return execute(conn, (connection) -> {
            try {
                return delegate.updateCalendar(connection, calendarName, calendar);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public boolean calendarExists(Connection conn, String calendarName) throws SQLException {
        return execute(conn, (connection) -> delegate.calendarExists(connection, calendarName));
    }

    @Override
    public Calendar selectCalendar(Connection conn, String calendarName)
        throws ClassNotFoundException, IOException, SQLException {

        return execute(conn, (connection) -> {
            try {
                return delegate.selectCalendar(connection, calendarName);
            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException(e);
            }
        });
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
        return execute(conn, super::selectNumCalendars);
    }

    @Override
    public List<String> selectCalendars(Connection conn) throws SQLException {
        return execute(conn, super::selectCalendars);
    }

    @Override
    public long selectNextFireTime(Connection conn) throws SQLException {
        return execute(conn, super::selectNextFireTime);
    }

    @Override
    public TriggerKey selectTriggerForFireTime(Connection conn, long fireTime) throws SQLException {
        return (TriggerKey) execute(conn, (connection) -> delegate.selectTriggerForFireTime(connection, fireTime));
    }

    @Override
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
        return execute(conn, super::selectFiredTriggerInstanceNames);
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
        return execute(conn,
            (connection) -> delegate.insertSchedulerState(connection, instanceId, checkInTime, interval));
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

    private <T> T execute(Connection connection, FunctionWithSQLException<Connection, T> function) throws SQLException {
        try {
            setSearchPath(connection, "public");

            return function.apply(connection);
        } finally {
            setSearchPath(connection, TenantContext.getCurrentDatabaseSchema());
        }
    }

    private static void setSearchPath(Connection connection, String databaseSchemaName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + databaseSchemaName);
        }
    }

    @FunctionalInterface
    private interface FunctionWithSQLException<Connection, T> {

        T apply(Connection connection) throws SQLException;
    }
}
