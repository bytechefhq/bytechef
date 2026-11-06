/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.domain;

import com.bytechef.commons.util.JsonUtils;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;

/**
 * @version ee
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AiObservabilityExportFilter.DateRange.class, name = "DATE_RANGE"),
    @JsonSubTypes.Type(value = AiObservabilityExportFilter.ProjectIn.class, name = "PROJECT_IN"),
    @JsonSubTypes.Type(value = AiObservabilityExportFilter.ModelIn.class, name = "MODEL_IN"),
    @JsonSubTypes.Type(value = AiObservabilityExportFilter.StatusIn.class, name = "STATUS_IN"),
    @JsonSubTypes.Type(value = AiObservabilityExportFilter.All.class, name = "ALL")
})
public sealed interface AiObservabilityExportFilter {

    record DateRange(Instant start, Instant end) implements AiObservabilityExportFilter {
        public DateRange {
            if (start == null || end == null) {
                throw new IllegalArgumentException("DateRange start and end must be non-null");
            }

            if (end.isBefore(start)) {
                throw new IllegalArgumentException(
                    "DateRange end (" + end + ") must not be before start (" + start + ")");
            }
        }
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    record ProjectIn(List<Long> projectIds) implements AiObservabilityExportFilter {
        public ProjectIn {
            if (projectIds == null || projectIds.isEmpty()) {
                throw new IllegalArgumentException("ProjectIn.projectIds must be non-empty");
            }

            projectIds = List.copyOf(projectIds);
        }
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    record ModelIn(List<String> models) implements AiObservabilityExportFilter {
        public ModelIn {
            if (models == null || models.isEmpty()) {
                throw new IllegalArgumentException("ModelIn.models must be non-empty");
            }

            models = List.copyOf(models);
        }
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    record StatusIn(List<String> statuses) implements AiObservabilityExportFilter {
        public StatusIn {
            if (statuses == null || statuses.isEmpty()) {
                throw new IllegalArgumentException("StatusIn.statuses must be non-empty");
            }

            statuses = List.copyOf(statuses);
        }
    }

    record All() implements AiObservabilityExportFilter {
    }

    static AiObservabilityExportFilter fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        return JsonUtils.read(json, AiObservabilityExportFilter.class);
    }

    static String toJson(AiObservabilityExportFilter filter) {
        return filter == null ? null : JsonUtils.write(filter);
    }
}
