
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

import com.bytechef.atlas.constant.WorkflowConstants;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapValueUtils;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class WorkflowTask implements Serializable {

    static {
        MapValueUtils.addConverter(new Converter<Map, WorkflowTask>() {

            @Override
            @SuppressWarnings("unchecked")
            public WorkflowTask convert(Map source) {
                return new WorkflowTask(source);
            }
        });
    }

    public static final WorkflowTask EMPTY_WORKFLOW_TASK = new WorkflowTask();

    private List<WorkflowTask> finalize = Collections.emptyList();
    private String label;
    private String name;
    private String node;
    private Map<String, Object> parameters = Collections.emptyMap();
    private List<WorkflowTask> post = Collections.emptyList();
    private List<WorkflowTask> pre = Collections.emptyList();
    private String timeout;
    private String type;

    public WorkflowTask() {
    }

    public WorkflowTask(Map<String, Object> source) {
        Assert.notNull(source, "'source' must not be null");

        putAll(source);
    }

    public void put(String key, Object value) {
        Assert.notNull(key, "'key' must not b null");

        putAll(Map.of(WorkflowConstants.PARAMETERS, Map.of(key, value)));
    }

    public void putAll(Map<String, Object> source) {
        if (source.containsKey(WorkflowConstants.FINALIZE)) {
            this.finalize = CollectionUtils.concat(
                finalize,
                CollectionUtils.map(
                    MapValueUtils.getList(
                        source, WorkflowConstants.FINALIZE, Map.class, Collections.emptyList()),
                    WorkflowTask::new));
        }

        if (source.containsKey(WorkflowConstants.LABEL)) {
            this.label = MapValueUtils.getString(source, WorkflowConstants.LABEL);
        }

        if (source.containsKey(WorkflowConstants.NAME)) {
            this.name = MapValueUtils.getString(source, WorkflowConstants.NAME);
        }

        if (source.containsKey(WorkflowConstants.NODE)) {
            this.node = MapValueUtils.getString(source, WorkflowConstants.NODE);
        }

        if (source.containsKey(WorkflowConstants.PARAMETERS)) {
            this.parameters = CollectionUtils.concat(
                parameters,
                MapValueUtils.getMap(source, WorkflowConstants.PARAMETERS, Collections.emptyMap()));
        }

        if (source.containsKey(WorkflowConstants.POST)) {
            this.post = CollectionUtils.concat(
                post,
                CollectionUtils.map(
                    MapValueUtils.getList(
                        source, WorkflowConstants.POST, Map.class, Collections.emptyList()),
                    WorkflowTask::new));
        }

        if (source.containsKey(WorkflowConstants.PRE)) {
            this.pre = CollectionUtils.concat(
                pre,
                CollectionUtils.map(
                    MapValueUtils.getList(
                        source, WorkflowConstants.PRE, Map.class, Collections.emptyList()),
                    WorkflowTask::new));
        }

        if (source.containsKey(WorkflowConstants.TIMEOUT)) {
            this.timeout = MapValueUtils.getString(source, WorkflowConstants.TIMEOUT);
        }

        if (source.containsKey(WorkflowConstants.TYPE)) {
            this.type = MapValueUtils.getString(source, WorkflowConstants.TYPE);
        }
    }

    public static WorkflowTask of(String type) {
        WorkflowTask workflowTask = new WorkflowTask();

        workflowTask.type = type;

        return workflowTask;
    }

    /**
     * Creates a {@link WorkflowTask} instance for the given Key-Value pair.
     *
     * @return The new {@link WorkflowTask}.
     */
    public static WorkflowTask of(String key, Object value) {
        Assert.notNull(key, "'key' must not be null");

        if (WorkflowConstants.WORKFLOW_DEFINITION_CONSTANTS.contains(key)) {
            return new WorkflowTask(Map.of(key, value));
        } else {
            return new WorkflowTask(Map.of(WorkflowConstants.PARAMETERS, Collections.singletonMap(key, value)));
        }
    }

    public static WorkflowTask of(String type, String key, Object value) {
        WorkflowTask workflowTask = of(key, value);

        workflowTask.type = type;

        return workflowTask;
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
     * The (optional) list of tasks that are to be executed after execution of this task -- regardless of whether it had
     * failed or not.
     *
     * @return the list of {@link WorkflowTask}s to execute after execution of this task -- regardless of whether it had
     *         failed or not. Never return a <code>null</code>
     */
    public List<WorkflowTask> getFinalize() {
        return Collections.unmodifiableList(finalize);
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
     * Get the identifier name of the task. Task names are used for assigning the output of one task so it can be later
     * used by subsequent tasks.
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
     *         <code>null</code>
     */
    public List<WorkflowTask> getPost() {
        return Collections.unmodifiableList(post);
    }

    /**
     * The (optional) list of tasks that are to be executed prior to this task.
     *
     * @return the list of {@link WorkflowTask}s to execute prior to the execution of this task. Never return a
     *         <code>null</code>
     */
    public List<WorkflowTask> getPre() {
        return Collections.unmodifiableList(pre);
    }

    /**
     * Returns the timeout expression which describes when this task should be deemed as timed-out.
     *
     * <p>
     * The formats accepted are based on the ISO-8601 duration format with days considered to be exactly 24 hours.
     *
     * @return String
     */
    public String getTimeout() {
        return timeout;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(
            WorkflowConstants.FINALIZE,
            finalize.stream()
                .map(WorkflowTask::toMap)
                .toList());

        if (label != null) {
            map.put(WorkflowConstants.LABEL, label);
        }

        if (name != null) {
            map.put(WorkflowConstants.NAME, name);
        }

        if (node != null) {
            map.put(WorkflowConstants.NODE, node);
        }

        map.put(WorkflowConstants.PARAMETERS, parameters);

        map.put(WorkflowConstants.POST, post.stream()
            .map(WorkflowTask::toMap)
            .toList());
        map.put(WorkflowConstants.PRE, pre.stream()
            .map(WorkflowTask::toMap)
            .toList());

        if (timeout != null) {
            map.put(WorkflowConstants.TIMEOUT, timeout);
        }

        map.put(WorkflowConstants.TYPE, type);

        return Collections.unmodifiableMap(map);
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
