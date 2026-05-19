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

package com.bytechef.component.ai.llm.router.nano.gpt.model;

import static com.bytechef.component.ai.llm.router.nano.gpt.constant.NanoGptConstants.BASE_URL;

import com.bytechef.component.ai.llm.router.model.RouterChatModel;
import java.util.Map;

/**
 * @author Marko Kriskovic
 */
public class NanoGptChatModel extends RouterChatModel {

    private final Double minP;
    private final Integer minTokens;
    private final Integer mirostatMode;
    private final Double mirostatTau;
    private final Double mirostatEta;
    private final Double repetitionPenalty;
    private final Double tfs;
    private final Double topA;
    private final Double typicalP;

    private NanoGptChatModel(Builder builder) {
        super(BASE_URL, builder);
        this.minP = builder.minP;
        this.minTokens = builder.minTokens;
        this.mirostatMode = builder.mirostatMode;
        this.mirostatTau = builder.mirostatTau;
        this.mirostatEta = builder.mirostatEta;
        this.repetitionPenalty = builder.repetitionPenalty;
        this.tfs = builder.tfs;
        this.topA = builder.topA;
        this.typicalP = builder.typicalP;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected void addProviderSpecificParams(Map<String, Object> body) {
        if (reasoning != null) {
            body.put("reasoning_effort", reasoning);
        }

        if (minP != null) {
            body.put("min_p", minP);
        }

        if (minTokens != null) {
            body.put("min_tokens", minTokens);
        }

        if (mirostatMode != null) {
            body.put("mirostat_mode", mirostatMode);
        }

        if (mirostatTau != null) {
            body.put("mirostat_tau", mirostatTau);
        }

        if (mirostatEta != null) {
            body.put("mirostat_eta", mirostatEta);
        }

        if (repetitionPenalty != null) {
            body.put("repetition_penalty", repetitionPenalty);
        }

        if (tfs != null) {
            body.put("tfs", tfs);
        }

        if (topA != null) {
            body.put("top_a", topA);
        }

        if (typicalP != null) {
            body.put("typical_p", typicalP);
        }
    }

    public static class Builder extends RouterChatModel.Builder<Builder> {

        private Double minP;
        private Integer minTokens;
        private Integer mirostatMode;
        private Double mirostatTau;
        private Double mirostatEta;
        private Double repetitionPenalty;
        private Double tfs;
        private Double topA;
        private Double typicalP;

        public Builder minP(Double minP) {
            this.minP = minP;

            return this;
        }

        public Builder minTokens(Integer minTokens) {
            this.minTokens = minTokens;

            return this;
        }

        public Builder mirostatMode(Integer mirostatMode) {
            this.mirostatMode = mirostatMode;

            return this;
        }

        public Builder mirostatTau(Double mirostatTau) {
            this.mirostatTau = mirostatTau;

            return this;
        }

        public Builder mirostatEta(Double mirostatEta) {
            this.mirostatEta = mirostatEta;

            return this;
        }

        public Builder repetitionPenalty(Double repetitionPenalty) {
            this.repetitionPenalty = repetitionPenalty;

            return this;
        }

        public Builder tfs(Double tfs) {
            this.tfs = tfs;

            return this;
        }

        public Builder topA(Double topA) {
            this.topA = topA;

            return this;
        }

        public Builder typicalP(Double typicalP) {
            this.typicalP = typicalP;

            return this;
        }

        public NanoGptChatModel build() {
            return new NanoGptChatModel(this);
        }
    }
}
