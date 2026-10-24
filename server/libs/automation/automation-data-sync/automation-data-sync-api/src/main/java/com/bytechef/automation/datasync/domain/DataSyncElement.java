/*
 * Copyright 2025 ByteChef
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

package com.bytechef.automation.datasync.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * One slot of a Data Sync's {@code dataStream/v1/stream} task: the source reader, the destination writer, or the
 * field-mapper processor. Identity is in columns, not in {@code parameters}, so a list can name the two sides without
 * parsing JSON.
 *
 * @author Ivica Cardic
 */
@Table("data_sync_element")
public final class DataSyncElement {

    /** Persisted as its ordinal; append only. */
    public enum Kind {
        SOURCE, DESTINATION, PROCESSOR
    }

    public static final String PROCESSOR_COMPONENT_NAME = "dataStreamProcessor";
    public static final int PROCESSOR_COMPONENT_VERSION = 1;
    public static final String PROCESSOR_OPERATION_NAME = "fieldMapper";

    @Id
    private Long id;

    @Column("data_sync_id")
    private long dataSyncId;

    @Column("kind")
    private int kind;

    @Column("component_name")
    private String componentName;

    @Column("component_version")
    private int componentVersion;

    @Column("operation_name")
    private String operationName;

    @Column
    private MapWrapper parameters = new MapWrapper();

    @Column("connection_id")
    private @Nullable Long connectionId;

    public DataSyncElement() {
    }

    public DataSyncElement(long dataSyncId, Kind kind) {
        this.dataSyncId = dataSyncId;
        this.kind = kind.ordinal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DataSyncElement that = (DataSyncElement) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getDataSyncId() {
        return dataSyncId;
    }

    public void setDataSyncId(long dataSyncId) {
        this.dataSyncId = dataSyncId;
    }

    public Kind getKind() {
        return Kind.values()[kind];
    }

    public void setKind(Kind kind) {
        this.kind = kind.ordinal();
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(int componentVersion) {
        this.componentVersion = componentVersion;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public Map<String, ?> getParameters() {
        return parameters.getMap();
    }

    public void setParameters(@Nullable Map<String, ?> parameters) {
        this.parameters = parameters == null ? new MapWrapper() : new MapWrapper(parameters);
    }

    public @Nullable Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(@Nullable Long connectionId) {
        this.connectionId = connectionId;
    }

    /** {@code <componentName>/v<componentVersion>/<operationName>}, the workflow node type this row renders as. */
    public String getType() {
        return componentName + "/v" + componentVersion + "/" + operationName;
    }

    @Override
    public String toString() {
        return "DataSyncElement{" +
            "id=" + id +
            ", dataSyncId=" + dataSyncId +
            ", kind=" + getKind() +
            ", type='" + getType() + '\'' +
            ", connectionId=" + connectionId +
            '}';
    }
}
