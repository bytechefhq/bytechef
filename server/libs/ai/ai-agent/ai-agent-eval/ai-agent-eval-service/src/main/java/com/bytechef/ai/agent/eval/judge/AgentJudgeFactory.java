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

package com.bytechef.ai.agent.eval.judge;

import com.bytechef.ai.agent.eval.constant.AgentJudgeType;
import java.util.Map;
import org.springaicommunity.judge.Judge;
import org.springaicommunity.judge.JudgeType;
import org.springaicommunity.judge.Judges;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Factory that converts persisted judge configuration (type + configuration map) into agent-judge {@link Judge}
 * instances, wrapped with {@link org.springaicommunity.judge.NamedJudge} for name-based lookups.
 *
 * @author ByteChef
 */
@Component
public class AgentJudgeFactory {

    @SuppressWarnings("unchecked")
    public Judge createJudge(
        String name, AgentJudgeType type, Map<String, Object> configuration,
        ChatClient.Builder chatClientBuilder) {

        Judge judge = switch (type) {
            case LLM_RULE -> new LlmRuleJudge(
                name, chatClientBuilder, (String) configuration.get("rule"));
            case CONTAINS_TEXT -> new ContainsTextJudge(
                (String) configuration.get("text"),
                (String) configuration.get("mode"));
            case REGEX_MATCH -> new RegexMatchJudge(
                (String) configuration.get("pattern"),
                (String) configuration.get("mode"));
            case RESPONSE_LENGTH -> {
                Number maxLengthValue = (Number) configuration.get("maxLength");
                Number minLengthValue = (Number) configuration.get("minLength");

                yield new ResponseLengthJudge(
                    maxLengthValue != null ? maxLengthValue.intValue() : null,
                    minLengthValue != null ? minLengthValue.intValue() : null);
            }
            case JSON_SCHEMA -> new JsonSchemaJudge(
                (Map<String, Object>) configuration.get("schema"));
            case SIMILARITY -> new SimilarityJudge(
                (String) configuration.get("expectedOutput"),
                ((Number) configuration.get("threshold")).doubleValue(),
                (String) configuration.get("algorithm"));
        };

        JudgeType judgeType = (type == AgentJudgeType.LLM_RULE) ? JudgeType.LLM_POWERED : JudgeType.DETERMINISTIC;

        return Judges.named(judge, name, type.name(), judgeType);
    }

}
