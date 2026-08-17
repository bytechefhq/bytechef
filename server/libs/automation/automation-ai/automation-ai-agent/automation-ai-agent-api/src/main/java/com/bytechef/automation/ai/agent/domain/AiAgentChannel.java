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

package com.bytechef.automation.ai.agent.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Domain class for an inbound trigger (e.g. webhook, chat, schedule) attached to an {@link AiAgent}. Rows
 * cascade-delete with their owning agent.
 *
 * @author Ivica Cardic
 */
@Table("ai_agent_channel")
public final class AiAgentChannel {

    @Id
    private Long id;

    @Column("agent_id")
    private long agentId;

    @Column("channel_type")
    private String channelType;

    @Column("position")
    private int position;

    @Column
    private MapWrapper parameters = new MapWrapper();

    @Column("connection_id")
    private @Nullable Long connectionId;

    public AiAgentChannel() {
    }

    public AiAgentChannel(long agentId, String channelType) {
        this.agentId = agentId;
        this.channelType = channelType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AiAgentChannel that = (AiAgentChannel) o;

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

    public long getAgentId() {
        return agentId;
    }

    public void setAgentId(long agentId) {
        this.agentId = agentId;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Map<String, ?> getParameters() {
        return parameters.getMap();
    }

    public void setParameters(Map<String, ?> parameters) {
        this.parameters = new MapWrapper(parameters);
    }

    public @Nullable Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(@Nullable Long connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public String toString() {
        return "AiAgentChannel{" +
            "id=" + id +
            ", agentId=" + agentId +
            ", channelType='" + channelType + '\'' +
            ", position=" + position +
            ", parameters=" + parameters +
            ", connectionId=" + connectionId +
            '}';
    }
}
