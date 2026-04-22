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

package com.bytechef.ee.platform.ai.tool.usage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Insert-only metering row for an expensive non-LLM tool call (e.g. a Firecrawl scrape, an OpenAI image generation, a
 * pptx build) issued from any AI agent surface.
 *
 * <p>
 * {@code unitCount} is normally {@code 1} per call but may be higher (e.g. number of URLs scraped in a Firecrawl
 * batch). Free tools (e.g. local pptx generation) have an estimated cost of {@link BigDecimal#ZERO} but are still
 * recorded so we can track agent reach. {@code metadataJson} is a free-form sidecar (e.g. {@code {"urls": [...]}} or
 * {@code {"imageSize": "1024x1024"}}). The optional {@code ownerId} carries whatever container the agent platform
 * attaches the call to (a AI Hub task id, an AI agent run id, etc.); it is intentionally NOT a foreign key so this
 * table can outlive any specific owner table and remain reusable across agent surfaces.
 *
 * <p>
 * Setters are package-private — only the metering implementation in {@code com.bytechef.ee.platform.ai.tool.usage} (and
 * Spring Data JDBC's reflection-based mapper) may mutate a row. Once persisted the row is read-only.
 *
 * @author Ivica Cardic
 */
@Table("ai_tool_usage")
public class AiToolUsage {

    @Id
    private Long id;

    @Column("user_id")
    private long userId;

    @Column("owner_id")
    private Long ownerId;

    @Column("tool_name")
    private String toolName;

    @Column("unit_count")
    private int unitCount;

    @Column("estimated_cost_usd")
    private BigDecimal estimatedCostUsd;

    @Column("duration_ms")
    private int durationMs;

    @Column("metadata_json")
    private String metadataJson;

    @Column("environment")
    private int environment;

    @Column("created_at")
    private LocalDateTime createdAt;

    AiToolUsage() {
    }

    public Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    void setUserId(long userId) {
        this.userId = userId;
    }

    public @Nullable Long getOwnerId() {
        return ownerId;
    }

    void setOwnerId(@Nullable Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getToolName() {
        return toolName;
    }

    void setToolName(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("AiToolUsage.toolName must not be null or blank");
        }

        this.toolName = toolName;
    }

    public int getUnitCount() {
        return unitCount;
    }

    void setUnitCount(int unitCount) {
        if (unitCount < 0) {
            throw new IllegalArgumentException("AiToolUsage.unitCount must be >= 0 (got " + unitCount + ")");
        }

        this.unitCount = unitCount;
    }

    public BigDecimal getEstimatedCostUsd() {
        return estimatedCostUsd;
    }

    void setEstimatedCostUsd(BigDecimal estimatedCostUsd) {
        if (estimatedCostUsd == null) {
            throw new IllegalArgumentException("AiToolUsage.estimatedCostUsd must not be null");
        }

        if (estimatedCostUsd.signum() < 0) {
            throw new IllegalArgumentException(
                "AiToolUsage.estimatedCostUsd must be >= 0 (got " + estimatedCostUsd + ")");
        }

        this.estimatedCostUsd = estimatedCostUsd;
    }

    public int getDurationMs() {
        return durationMs;
    }

    void setDurationMs(int durationMs) {
        if (durationMs < 0) {
            throw new IllegalArgumentException("AiToolUsage.durationMs must be >= 0 (got " + durationMs + ")");
        }

        this.durationMs = durationMs;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    /**
     * Returns the environment ordinal this row was recorded against. Persisted as an INT for the same reason every
     * other row carrying environment does — denormalised so analytics queries (SUM(cost) GROUP BY environment,
     * tool_name) avoid joining the larger owner-specific tables on the hot dashboard path. Callers translate the
     * ordinal into their domain {@code Environment} enum.
     */
    public int getEnvironment() {
        return environment;
    }

    void setEnvironment(int environment) {
        this.environment = environment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        AiToolUsage that = (AiToolUsage) other;

        // Guard the transient-instance case so two unsaved rows with id=null do not collapse in a HashSet — mirrors
        // the pattern used by the original AiHubToolUsage and other Spring Data JDBC entities here.
        if (id == null) {
            return this == that;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "AiToolUsage{" +
            "id=" + id +
            ", userId=" + userId +
            ", ownerId=" + ownerId +
            ", toolName='" + toolName + '\'' +
            ", unitCount=" + unitCount +
            ", estimatedCostUsd=" + estimatedCostUsd +
            ", durationMs=" + durationMs +
            ", environment=" + environment +
            ", createdAt=" + createdAt +
            '}';
    }
}
