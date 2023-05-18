
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
import com.bytechef.commons.typeconverter.TypeConverter;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapValueUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class WorkflowTask implements Task {

    private List<WorkflowTask> finalize = Collections.emptyList();
    private String label;
    private final Map<String, Object> extensions = new HashMap<>();
    private Map<String, ?> metadata = new HashMap<>();
    private String name;
    private String node;
    private Map<String, ?> parameters = Collections.emptyMap();
    private List<WorkflowTask> post = Collections.emptyList();
    private List<WorkflowTask> pre = Collections.emptyList();
    private String timeout;
    private String type;

    private WorkflowTask() {
    }

    private WorkflowTask(Map<String, ?> source) {
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            if (WorkflowConstants.FINALIZE.equals(entry.getKey())) {
                this.finalize = MapValueUtils.getList(
                    source, WorkflowConstants.FINALIZE, WorkflowTask.class, Collections.emptyList());
            } else if (WorkflowConstants.LABEL.equals(entry.getKey())) {
                this.label = MapValueUtils.getString(source, WorkflowConstants.LABEL);
            } else if (WorkflowConstants.METADATA.equals(entry.getKey())) {
                this.metadata = MapValueUtils.getMap(source, WorkflowConstants.METADATA, Collections.emptyMap());
            } else if (WorkflowConstants.NAME.equals(entry.getKey())) {
                this.name = MapValueUtils.getString(source, WorkflowConstants.NAME);
            } else if (WorkflowConstants.NODE.equals(entry.getKey())) {
                this.node = MapValueUtils.getString(source, WorkflowConstants.NODE);
            } else if (WorkflowConstants.PARAMETERS.equals(entry.getKey())) {
                this.parameters = MapValueUtils.getMap(source, WorkflowConstants.PARAMETERS, Collections.emptyMap());
            } else if (WorkflowConstants.POST.equals(entry.getKey())) {
                this.post = MapValueUtils.getList(
                    source, WorkflowConstants.POST, WorkflowTask.class, Collections.emptyList());
            } else if (WorkflowConstants.PRE.equals(entry.getKey())) {
                this.pre = MapValueUtils.getList(
                    source, WorkflowConstants.PRE, WorkflowTask.class, Collections.emptyList());
            } else if (WorkflowConstants.TIMEOUT.equals(entry.getKey())) {
                this.timeout = MapValueUtils.getString(source, WorkflowConstants.TIMEOUT);
            } else if (WorkflowConstants.TYPE.equals(entry.getKey())) {
                this.type = MapValueUtils.getRequiredString(source, WorkflowConstants.TYPE);
            } else {
                this.extensions.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public static WorkflowTask of(Map<String, ?> source) {
        Assert.notNull(source, "'source' must not be null");

        return new WorkflowTask(source);
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

    public <T> T getExtension(String name, Class<T> elementType, T defaultValue) {
        return MapValueUtils.get(extensions, name, elementType, defaultValue);
    }

    public <T> List<T> getExtensions(String name, Class<T> elementType, List<T> defaultValue) {
        return MapValueUtils.getList(extensions, name, elementType, defaultValue);
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
     * Get the metadata.
     *
     * @return
     */
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
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

        map.put(WorkflowConstants.FINALIZE, CollectionUtils.map(finalize, WorkflowTask::toMap));

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
        map.put(WorkflowConstants.POST, CollectionUtils.map(post, WorkflowTask::toMap));
        map.put(WorkflowConstants.PRE, CollectionUtils.map(pre, WorkflowTask::toMap));

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

    public static class WorkflowTaskConverter implements TypeConverter.Conversion<WorkflowTask> {

        public Object[] getTypeKeys() {
            return new Object[] {
                WorkflowTask.class
            };
        }

        @SuppressWarnings("unchecked")
        public WorkflowTask convert(Object value) {
            return new WorkflowTask((Map<String, ?>) value);
        }
    }
}
