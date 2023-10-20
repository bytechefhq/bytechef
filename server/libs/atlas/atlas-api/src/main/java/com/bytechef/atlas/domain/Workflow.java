
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

package com.bytechef.atlas.domain;

import com.bytechef.atlas.constant.WorkflowConstants;
import com.bytechef.error.Errorable;
import com.bytechef.error.ExecutionError;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.workflow.mapper.WorkflowReader;
import com.bytechef.atlas.workflow.mapper.WorkflowResource;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapValueUtils;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

/**
 * Workflows are the blueprints that describe the execution of a job.
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@Table
public final class Workflow implements Errorable, Persistable<String> {

    public enum Format {
        JSON(1),
        YAML(2);

        private final int id;

        Format(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Format parse(String filename) {
            Assert.notNull(filename, "Filename '%s' can not be null".formatted(filename));

            String extension = Optional.of(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1))
                .orElse("");

            return Objects.equals(extension.toLowerCase(), "json") ? JSON : YAML;
        }

        public static Format valueOf(int id) {
            return switch (id) {
                case 1 -> Format.JSON;
                case 2 -> Format.YAML;
                default -> throw new IllegalStateException("Unexpected value: %s".formatted(id));
            };
        }
    }

    public enum SourceType {
        CLASSPATH, FILESYSTEM, GIT, JDBC
    }

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column
    private String definition;

    @Transient
    private String description;

    @Transient
    private ExecutionError error;

    @Column
    private int format;

    @Id
    private String id;

    @Transient
    private List<Input> inputs = Collections.emptyList();

    @Transient
    private boolean isNew;

    @Transient
    private String label;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Transient
    private List<Output> outputs = Collections.emptyList();

    @Transient
    private SourceType sourceType;

    @Transient
    private int maxRetries;

    @Transient
    private List<WorkflowTask> tasks = Collections.emptyList();

    @Version
    private int version;

    public Workflow() {
    }

    @PersistenceCreator
    public Workflow(String definition, String id, int format) throws Exception {
        this(definition, Format.valueOf(format), id, WorkflowReader.readWorkflowMap(
            new WorkflowResource(
                id, new ByteArrayResource(definition.getBytes(StandardCharsets.UTF_8)), Format.valueOf(format))));
    }

    public Workflow(String definition, Format format, String id, Map<String, Object> source) {
        Assert.notNull(definition, "'definition' must not be null");
        Assert.notNull(format, "'format' must not be null");
        Assert.notNull(id, "'id' must not be null");
        Assert.notNull(source, "'source' must not be null");

        this.definition = definition;
        this.format = format.getId();
        this.description = MapValueUtils.getString(source, WorkflowConstants.DESCRIPTION);
        this.id = id;
        this.inputs = CollectionUtils.map(
            MapValueUtils.getList(source, WorkflowConstants.INPUTS, Map.class, Collections.emptyList()),
            map -> new Input(
                MapValueUtils.getRequiredString(map, WorkflowConstants.NAME),
                MapValueUtils.getString(map, WorkflowConstants.LABEL),
                MapValueUtils.getString(map, WorkflowConstants.TYPE, "string"),
                MapValueUtils.getBoolean(map, WorkflowConstants.REQUIRED, false)));
        this.label = MapValueUtils.getString(source, WorkflowConstants.LABEL);
        this.outputs = CollectionUtils.map(
            MapValueUtils.getList(
                source, WorkflowConstants.OUTPUTS, Map.class, Collections.emptyList()),
            map -> new Output(
                MapValueUtils.getRequiredString(map, WorkflowConstants.NAME),
                MapValueUtils.getRequiredString(map, WorkflowConstants.VALUE)));
        this.maxRetries = MapValueUtils.getInteger(source, WorkflowConstants.MAX_RETRIES, 0);
        this.tasks = CollectionUtils.map(
            MapValueUtils.getList(source, WorkflowConstants.TASKS, Map.class, Collections.emptyList()),
            WorkflowTask::of);
    }

    public Workflow(String definition, Format format) {
        this.definition = definition;
        this.format = format.getId();
    }

    public Workflow(String definition, Format format, SourceType sourceType) {
        this.definition = definition;
        this.format = format.getId();
        this.sourceType = sourceType;
    }

    public Workflow(String id, String definition) {
        this.id = id;
        this.definition = definition;
    }

    public Workflow(String id, ExecutionError error) {
        this.id = id;
        this.error = error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Workflow workflow = (Workflow) o;

        return Objects.equals(id, workflow.id);
    }

    public <T> Optional<T> fetchExtension(Class<T> extensionClass) {
        // TODO

        return Optional.empty();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public String getDefinition() {
        return definition;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public ExecutionError getError() {
        return error;
    }

    public Format getFormat() {
        return Format.valueOf(format);
    }

    /** Returns the unique identifier of the workflow. */
    public String getId() {
        return id;
    }

    /** Returns the workflow's expected inputs */
    public List<Input> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    /** Returns a descriptive name for the workflow. */
    public String getLabel() {
        return label;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /** Returns the workflow's expected outputs */
    public List<Output> getOutputs() {
        return Collections.unmodifiableList(outputs);
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    /**
     * Defines the maximum number of times a task may retry.
     *
     * @return int the maximum number of retries.
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /** Returns the steps that make up the workflow. */
    public List<WorkflowTask> getTasks() {
        return new ArrayList<>(tasks);
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public void setFormat(Format format) {
        this.format = format.getId();
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Workflow{" +
            ", id=" + id +
            ", label='" + label + '\'' +
            ", definition='" + definition + '\'' +
            ", format=" + format +
            ", sourceType=" + sourceType +
            ", inputs=" + inputs +
            ", outputs=" + outputs +
            ", error=" + error +
            ", maxRetries=" + maxRetries +
            ", tasks=" + tasks +
            ", version=" + version +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }

    public record Input(String name, String label, String type, boolean required) {
    }

    public record Output(String name, Object value) {
    }
}
