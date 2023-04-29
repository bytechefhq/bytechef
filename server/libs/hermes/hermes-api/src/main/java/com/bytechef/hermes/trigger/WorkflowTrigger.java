
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.trigger;

import com.bytechef.atlas.constant.WorkflowConstants;
import com.bytechef.commons.util.MapValueUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class WorkflowTrigger implements Serializable, Trigger {

    static {
        MapValueUtils.addConverter(new WorkflowTriggerConverter());
    }

    private String componentName;
    private int componentVersion;
    private String name;
    private String label;
    private Map<String, Object> parameters;
    private String timeout;
    private String type;
    private String triggerName;

    private WorkflowTrigger() {
    }

    public WorkflowTrigger(Map<String, Object> source) {
        Assert.notNull(source, "'source' must not be null");

        if (source.containsKey(WorkflowConstants.LABEL)) {
            this.label = MapValueUtils.getString(source, WorkflowConstants.LABEL);
        }

        if (source.containsKey(WorkflowConstants.NAME)) {
            this.name = MapValueUtils.getString(source, WorkflowConstants.NAME);
        }

        if (source.containsKey(WorkflowConstants.PARAMETERS)) {
            this.parameters = MapValueUtils.getMap(source, WorkflowConstants.PARAMETERS, Collections.emptyMap());
        }

        if (source.containsKey(WorkflowConstants.TIMEOUT)) {
            this.timeout = MapValueUtils.getString(source, WorkflowConstants.TIMEOUT);
        }

        this.type = MapValueUtils.getRequiredString(source, WorkflowConstants.TYPE);

        String[] typeItems = type.split("/");

        this.componentName = typeItems[0];
        this.componentVersion = Integer.parseInt(typeItems[1].replace("v", ""));
        this.triggerName = typeItems[2];
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

    public <T> Optional<T> fetchExtension(Class<T> extensionClass) {
        // TODO

        return Optional.empty();
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, name, parameters, timeout, type);
    }

    public String getComponentName() {
        return componentName;
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public String getTimeout() {
        return timeout;
    }

    public String getTriggerName() {
        return triggerName;
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
        return "WorkflowTask{" + ", label='"
            + label + '\'' + ", name='"
            + name + '\'' + ", timeout='"
            + timeout + '\'' + ", type='"
            + type + '\'' + ", parameters='"
            + parameters + '\'' + '}';
    }

    private static class WorkflowTriggerConverter implements Converter<Map, WorkflowTrigger> {

        @Override
        @SuppressWarnings("unchecked")
        public WorkflowTrigger convert(Map source) {
            return new WorkflowTrigger(source);
        }
    }
}
