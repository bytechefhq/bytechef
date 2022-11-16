/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.task;

import com.bytechef.atlas.constants.WorkflowConstants;
import com.bytechef.commons.collection.MapUtils;
import java.beans.Transient;
import java.io.Serializable;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class WorkflowTask implements WorkflowTaskParameter, Task, Serializable {

    public static final WorkflowTask EMPTY_WORKFLOW_TASK = new WorkflowTask();

    private static final List<String> STATIC_FIELDS = List.of(
            WorkflowConstants.FINALIZE,
            WorkflowConstants.LABEL,
            WorkflowConstants.NAME,
            WorkflowConstants.NODE,
            WorkflowConstants.POST,
            WorkflowConstants.PRE,
            WorkflowConstants.TASK_NUMBER,
            WorkflowConstants.TIMEOUT,
            WorkflowConstants.TYPE);

    private List<WorkflowTask> finalize = Collections.emptyList();
    private String label;
    private String name;
    private String node;
    private Map<String, Object> parameters = new HashMap<>();
    private List<WorkflowTask> post = Collections.emptyList();
    private List<WorkflowTask> pre = Collections.emptyList();
    private String timeout;
    private String type;

    public WorkflowTask() {}

    public WorkflowTask(Map<String, Object> source) {
        Assert.notNull(source, "source cannot be null.");

        this.finalize =
                MapUtils.getList(source, WorkflowConstants.FINALIZE, Map.class, Collections.emptyList()).stream()
                        .map(WorkflowTask::new)
                        .toList();
        this.label = MapUtils.getString(source, WorkflowConstants.LABEL);
        this.name = MapUtils.getString(source, WorkflowConstants.NAME);
        this.node = MapUtils.getString(source, WorkflowConstants.NODE);
        this.post = MapUtils.getList(source, WorkflowConstants.POST, Map.class, Collections.emptyList()).stream()
                .map(WorkflowTask::new)
                .toList();
        this.pre = MapUtils.getList(source, WorkflowConstants.PRE, Map.class, Collections.emptyList()).stream()
                .map(WorkflowTask::new)
                .toList();
        this.timeout = MapUtils.getString(source, WorkflowConstants.TIMEOUT);
        this.type = MapUtils.getString(source, WorkflowConstants.TYPE);

        parameters = new HashMap<>();

        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (!STATIC_FIELDS.contains(entry.getKey())) {
                parameters.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public WorkflowTask(WorkflowTask workflowTask) {
        Assert.notNull(workflowTask, "workflowTask cannot be null.");

        this.finalize = workflowTask.getFinalize();
        this.label = workflowTask.getLabel();
        this.name = workflowTask.getName();
        this.node = workflowTask.getNode();
        this.parameters = new HashMap<>(workflowTask.getParameters());
        this.post = workflowTask.getPost();
        this.pre = workflowTask.getPre();
        this.timeout = workflowTask.getTimeout();
        this.type = workflowTask.getType();
    }

    /**
     * Creates a {@link WorkflowTask} instance for the given Key-Value pair.
     *
     * @return The new {@link WorkflowTask}.
     */
    public static WorkflowTask of(String key, Object value) {
        Assert.notNull(key, "key cannot be null");

        return new WorkflowTask(Collections.singletonMap(key, value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WorkflowTask that = (WorkflowTask) o;

        return finalize.equals(that.finalize)
                && Objects.equals(label, that.label)
                && Objects.equals(name, that.name)
                && Objects.equals(node, that.node)
                && parameters.equals(that.parameters)
                && post.equals(that.post)
                && pre.equals(that.pre)
                && Objects.equals(timeout, that.timeout)
                && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(finalize, label, name, node, parameters, post, pre, timeout, type);
    }

    /**
     * The (optional) list of tasks that are to be executed after execution of this task -- regardless of whether it
     * had failed or not.
     *
     * @return the list of {@link WorkflowTask}s to execute after execution of this task -- regardless of whether it
     * had failed or not. Never return a <code>null</code>
     */
    public List<WorkflowTask> getFinalize() {
        return finalize;
    }

    /**
     * Get the human-readable description of the task.
     *
     * @return String
     */
    public String getLabel() {
        return label;
    }

    /**
     * Get the identifier name of the task. Task names are used for assigning the output of one task so it can be
     * later used by subsequent tasks.
     *
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Defines the name of the type of the node that the task execution will be routed to. For instance, if the node
     * value is: "encoder", then the task will be routed to the "encoder" queue which is presumably subscribed to by
     * worker nodes of "encoder" type.
     *
     * @return String
     */
    public String getNode() {
        return node;
    }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    /**
     * The (optional) list of tasks that are to be executed after the succesful execution of this task.
     *
     * @return the list of {@link WorkflowTask}s to execute after the succesful execution of this task. Never return a
     * <code>null</code>
     */
    public List<WorkflowTask> getPost() {
        return post;
    }

    /**
     * The (optional) list of tasks that are to be executed prior to this task.
     *
     * @return the list of {@link WorkflowTask}s to execute prior to the execution of this task.
     *     Never return a <code>null</code>
     */
    public List<WorkflowTask> getPre() {
        return pre;
    }

    /**
     * Returns the timeout expression which describes when this task should be deemed as timed-out.
     *
     * <p>The formats accepted are based on the ISO-8601 duration format with days considered to be exactly 24 hours.
     *
     * @return String
     */
    public String getTimeout() {
        return timeout;
    }

    @Override
    public String getType() {
        return type;
    }

    @Transient
    public WorkflowTask getWorkflowTask(String key) {
        if (!containsKey(key)) {
            return null;
        }

        return new WorkflowTask(getMap(key));
    }

    public List<WorkflowTask> getWorkflowTasks(String key) {
        return getList(key, Map.class, Collections.emptyList()).stream()
                .map(WorkflowTask::new)
                .toList();
    }

    // WorkflowTaskParameter

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>(parameters);

        map.put(
                WorkflowConstants.FINALIZE,
                finalize.stream().map(WorkflowTask::asMap).toList());

        if (label != null) {
            map.put(WorkflowConstants.LABEL, label);
        }

        if (name != null) {
            map.put(WorkflowConstants.NAME, name);
        }

        if (node != null) {
            map.put(WorkflowConstants.NODE, node);
        }

        map.put(WorkflowConstants.POST, post.stream().map(WorkflowTask::asMap).toList());
        map.put(WorkflowConstants.PRE, pre.stream().map(WorkflowTask::asMap).toList());

        if (timeout != null) {
            map.put(WorkflowConstants.TIMEOUT, timeout);
        }

        map.put(WorkflowConstants.TYPE, type);

        return Collections.unmodifiableMap(map);
    }

    @Override
    public boolean containsKey(String key) {
        return parameters.containsKey(key);
    }

    @Override
    public Object get(String key) {
        return parameters.get(key);
    }

    @Override
    public <T> T get(String key, Class<T> returnType) {
        return MapUtils.get(parameters, key, returnType);
    }

    @Override
    public <T> T get(String key, Class<T> returnType, T defaultValue) {
        return MapUtils.get(parameters, key, returnType, defaultValue);
    }

    @Override
    public <T> T[] getArray(String key, Class<T> elementType) {
        return MapUtils.getArray(parameters, key, elementType);
    }

    @Override
    public Boolean getBoolean(String key) {
        return MapUtils.getBoolean(parameters, key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return MapUtils.getBoolean(parameters, key, defaultValue);
    }

    @Override
    public Date getDate(String key) {
        return MapUtils.getDate(parameters, key);
    }

    @Override
    public Double getDouble(String key) {
        return MapUtils.getDouble(parameters, key);
    }

    @Override
    public Double getDouble(String key, double defaultValue) {
        return MapUtils.getDouble(parameters, key, defaultValue);
    }

    @Override
    public Duration getDuration(String key) {
        return MapUtils.getDuration(parameters, key);
    }

    @Override
    public Duration getDuration(String key, String defaultDuration) {
        return MapUtils.getDuration(parameters, key, defaultDuration);
    }

    @Override
    public Float getFloat(String key) {
        return MapUtils.getFloat(parameters, key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        return MapUtils.getFloat(parameters, key, defaultValue);
    }

    @Override
    public Integer getInteger(String key) {
        return MapUtils.getInteger(parameters, key);
    }

    @Override
    public int getInteger(String key, int defaultValue) {
        return MapUtils.getInteger(parameters, key, defaultValue);
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType) {
        return MapUtils.getList(parameters, key, elementType);
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType, List<T> defaultValue) {
        return MapUtils.getList(parameters, key, elementType, defaultValue);
    }

    @Override
    public Long getLong(String key) {
        return MapUtils.getLong(parameters, key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return MapUtils.getLong(parameters, key, defaultValue);
    }

    @Override
    public Map<String, Object> getMap(String key) {
        return MapUtils.getMap(parameters, key);
    }

    @Override
    public Map<String, Object> getMap(String key, Map<String, Object> defaultValue) {
        return MapUtils.getMap(parameters, key, defaultValue);
    }

    @Override
    public String getRequiredString(String key) {
        return MapUtils.getRequiredString(parameters, key);
    }

    @Override
    public <T> T getRequired(String key) {
        return MapUtils.getRequired(parameters, key);
    }

    @Override
    public <T> T getRequired(String key, Class<T> returnType) {
        return MapUtils.getRequired(parameters, key, returnType);
    }

    @Override
    public String getString(String key) {
        return MapUtils.getString(parameters, key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return MapUtils.getString(parameters, key, defaultValue);
    }

    public void put(String key, Object value) {
        parameters.put(key, value);
    }

    @Override
    public String toString() {
        return "WorkflowTask{" + "finalize="
                + finalize + ", label='"
                + label + '\'' + ", name='"
                + name + '\'' + ", node='"
                + node + '\'' + ", post="
                + post + ", pre="
                + pre + ", timeout='"
                + timeout + '\'' + ", type='"
                + type + '\'' + ", parameters='"
                + parameters + '\'' + '}';
    }
}
