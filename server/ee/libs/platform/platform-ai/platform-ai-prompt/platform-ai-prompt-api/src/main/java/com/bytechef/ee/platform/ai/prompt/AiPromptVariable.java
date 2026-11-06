/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.prompt;

import com.bytechef.commons.util.JsonUtils;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AiPromptVariable.StringVar.class, name = "STRING"),
    @JsonSubTypes.Type(value = AiPromptVariable.NumberVar.class, name = "NUMBER"),
    @JsonSubTypes.Type(value = AiPromptVariable.BooleanVar.class, name = "BOOLEAN"),
    @JsonSubTypes.Type(value = AiPromptVariable.EnumVar.class, name = "ENUM")
})
public sealed interface AiPromptVariable {

    String name();

    boolean required();

    record StringVar(String name, boolean required, String defaultValue) implements AiPromptVariable {
        public StringVar {
            requireValidName(name);
        }
    }

    record NumberVar(String name, boolean required, Double defaultValue) implements AiPromptVariable {
        public NumberVar {
            requireValidName(name);
        }
    }

    record BooleanVar(String name, boolean required, Boolean defaultValue) implements AiPromptVariable {
        public BooleanVar {
            requireValidName(name);
        }
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    record EnumVar(String name, boolean required, String defaultValue, List<String> options)
        implements AiPromptVariable {

        public EnumVar {
            requireValidName(name);

            if (options == null || options.isEmpty()) {
                throw new IllegalArgumentException("EnumVar '" + name + "' must have at least one option");
            }

            if (defaultValue != null && !options.contains(defaultValue)) {
                throw new IllegalArgumentException(
                    "EnumVar '" + name + "' defaultValue " + defaultValue + " is not in options " + options);
            }

            options = List.copyOf(options);
        }
    }

    private static void requireValidName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("variable name must not be blank");
        }
    }

    static List<AiPromptVariable> parseList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        return JsonUtils.readList(json, AiPromptVariable.class);
    }

    static String toJson(List<AiPromptVariable> variables) {
        return variables == null ? null : JsonUtils.write(variables);
    }
}
