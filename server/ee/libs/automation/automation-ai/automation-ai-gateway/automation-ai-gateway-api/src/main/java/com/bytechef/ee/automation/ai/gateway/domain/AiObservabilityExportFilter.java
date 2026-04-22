/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import com.bytechef.commons.util.JsonUtils;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;

/**
 * Typed filter predicate for observability export jobs. Replaces the previous opaque {@code filters} JSON blob on
 * {@link AiObservabilityExportJob} with a sealed hierarchy so the export executor knows exactly which variants it must
 * handle — missing a branch becomes a compile-time error instead of "export silently includes everything".
 *
 * <p>
 * Storage: {@link AiObservabilityExportJob#getFilters()} remains a {@code String} column holding JSON. Parse at the
 * boundary via {@link #fromJson(String)}.
 *
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

    // List.copyOf() freezes the list at construction so post-validation mutation of the caller's list cannot
    // corrupt the export filter. @SuppressFBWarnings covers EI_EXPOSE_REP on the auto-generated accessor —
    // the exposed reference is List.copyOf()'s immutable view, so it is safe to return directly.
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

    /**
     * Catch-all "no filter" variant. Declaring it explicitly (instead of treating {@code null} as "all") makes the
     * export executor's switch exhaustive without a default clause, so a newly added filter variant is caught at
     * compile time rather than producing an unfiltered export.
     */
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
