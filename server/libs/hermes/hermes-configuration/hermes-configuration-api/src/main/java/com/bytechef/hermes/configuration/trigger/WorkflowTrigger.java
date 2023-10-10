
/*
 * Copyright 2023-present ByteChef Inc.
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
 */

package com.bytechef.hermes.configuration.trigger;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.Validate;
import org.springframework.core.convert.converter.Converter;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class WorkflowTrigger implements Serializable, Trigger {

    private final Map<String, Object> extensions = new HashMap<>();
    private String name;
    private String label;
    private Map<String, ?> parameters = Collections.emptyMap();
    private String timeout;
    private String type;

    private WorkflowTrigger() {
    }

    public WorkflowTrigger(Map<String, ?> source) {
        Validate.notNull(source, "'source' must not be null");

        for (Map.Entry<String, ?> entry : source.entrySet()) {
            if (WorkflowConstants.LABEL.equals(entry.getKey())) {
                this.label = MapUtils.getString(source, WorkflowConstants.LABEL);
            } else if (WorkflowConstants.NAME.equals(entry.getKey())) {
                this.name = MapUtils.getString(source, WorkflowConstants.NAME);
            } else if (WorkflowConstants.PARAMETERS.equals(entry.getKey())) {
                this.parameters = MapUtils.getMap(source, WorkflowConstants.PARAMETERS, Collections.emptyMap());
            } else if (WorkflowConstants.TIMEOUT.equals(entry.getKey())) {
                this.timeout = MapUtils.getString(source, WorkflowConstants.TIMEOUT);
            } else if (WorkflowConstants.TYPE.equals(entry.getKey())) {
                this.type = MapUtils.getString(source, WorkflowConstants.TYPE);
            } else {
                extensions.put(entry.getKey(), entry.getValue());
            }
        }

        Validate.notNull(name, "'name' must not be null");
        Validate.notNull(type, "'type' must not be null");
    }

    public static WorkflowTrigger of(Map<String, Object> source) {
        return new WorkflowTrigger(source);
    }

    public static List<WorkflowTrigger> of(Workflow workflow) {
        return workflow.getExtensions("triggers", WorkflowTrigger.class, List.of());
    }

    public static WorkflowTrigger of(String triggerName, Workflow workflow) {
        return CollectionUtils.getFirst(
            workflow.getExtensions("triggers", WorkflowTrigger.class, List.of()),
            workflowTrigger -> Objects.equals(triggerName, workflowTrigger.name));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WorkflowTrigger that = (WorkflowTrigger) o;

        return Objects.equals(label, that.label)
            && Objects.equals(name, that.name)
            && parameters.equals(that.parameters)
            && Objects.equals(timeout, that.timeout)
            && Objects.equals(type, that.type);
    }

    public <T> T getExtension(String name, Class<T> elementType, T defaultValue) {
        return MapUtils.get(extensions, name, elementType, defaultValue);
    }

    public <T> List<T> getExtensions(String name, Class<T> elementType, List<T> defaultValue) {
        return MapUtils.getList(extensions, name, elementType, defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, name, parameters, timeout, type);
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public Map<String, ?> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public String getTimeout() {
        return timeout;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        if (label != null) {
            map.put(WorkflowConstants.LABEL, label);
        }

        if (name != null) {
            map.put(WorkflowConstants.NAME, name);
        }

        map.put(WorkflowConstants.PARAMETERS, parameters);

        if (timeout != null) {
            map.put(WorkflowConstants.TIMEOUT, timeout);
        }

        map.put(WorkflowConstants.TYPE, type);

        return Collections.unmodifiableMap(map);
    }

    @Override
    public String toString() {
        return "WorkflowTrigger{" +
            "name='" + name + '\'' +
            ", label='" + label + '\'' +
            ", timeout='" + timeout + '\'' +
            ", type='" + type + '\'' +
            ", parameters=" + parameters +
            ", extensions=" + extensions +
            '}';
    }

    public static class WorkflowTriggerConverter implements Converter<Map, WorkflowTrigger> {

        @Override
        @SuppressWarnings("unchecked")
        public WorkflowTrigger convert(Map source) {
            return new WorkflowTrigger(source);
        }
    }
}
