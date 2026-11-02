/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.skill.remote.grpc;

import com.bytechef.ee.remote.grpc.JsonBytes;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.tag.domain.Tag;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Objects;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiSkillProtoMapper {

    private static final TypeReference<AiSkill> AI_SKILL_TYPE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<List<Tag>> TAGS_TYPE_REFERENCE = new TypeReference<>() {};

    private final JsonBytes jsonBytes;

    public AiSkillProtoMapper(ObjectMapper objectMapper) {
        this.jsonBytes = new JsonBytes(objectMapper);
    }

    public AiSkill toAiSkill(ByteString bytes) {
        return Objects.requireNonNull(jsonBytes.read(bytes, AI_SKILL_TYPE_REFERENCE), "aiSkill");
    }

    public List<Tag> toTags(ByteString bytes) {
        List<Tag> tags = jsonBytes.read(bytes, TAGS_TYPE_REFERENCE);

        return tags == null ? List.of() : tags;
    }

    public ByteString write(Object value) {
        return jsonBytes.write(value);
    }
}
