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
import java.util.List;

/**
 * @version ee
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AiObservabilityAlertFilter.ModelEquals.class, name = "MODEL_EQUALS"),
    @JsonSubTypes.Type(value = AiObservabilityAlertFilter.ModelIn.class, name = "MODEL_IN"),
    @JsonSubTypes.Type(value = AiObservabilityAlertFilter.ProjectEquals.class, name = "PROJECT_EQUALS"),
    @JsonSubTypes.Type(value = AiObservabilityAlertFilter.UserEquals.class, name = "USER_EQUALS"),
    @JsonSubTypes.Type(value = AiObservabilityAlertFilter.And.class, name = "AND"),
    @JsonSubTypes.Type(value = AiObservabilityAlertFilter.Or.class, name = "OR")
})
public sealed interface AiObservabilityAlertFilter {

    record ModelEquals(String model) implements AiObservabilityAlertFilter {
        public ModelEquals {
            if (model == null || model.isBlank()) {
                throw new IllegalArgumentException("ModelEquals.model must not be blank");
            }
        }
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    record ModelIn(List<String> models) implements AiObservabilityAlertFilter {
        public ModelIn {
            if (models == null || models.isEmpty()) {
                throw new IllegalArgumentException("ModelIn.models must be non-empty");
            }

            models = List.copyOf(models);
        }
    }

    record ProjectEquals(long projectId) implements AiObservabilityAlertFilter {
    }

    record UserEquals(String userId) implements AiObservabilityAlertFilter {
        public UserEquals {
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("UserEquals.userId must not be blank");
            }
        }
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    record And(List<AiObservabilityAlertFilter> filters) implements AiObservabilityAlertFilter {
        public And {
            if (filters == null || filters.size() < 2) {
                throw new IllegalArgumentException("And requires at least two child filters");
            }

            filters = List.copyOf(filters);
        }
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    record Or(List<AiObservabilityAlertFilter> filters) implements AiObservabilityAlertFilter {
        public Or {
            if (filters == null || filters.size() < 2) {
                throw new IllegalArgumentException("Or requires at least two child filters");
            }

            filters = List.copyOf(filters);
        }
    }

    static AiObservabilityAlertFilter fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        return JsonUtils.read(json, AiObservabilityAlertFilter.class);
    }

    static String toJson(AiObservabilityAlertFilter filter) {
        return filter == null ? null : JsonUtils.write(filter);
    }
}
