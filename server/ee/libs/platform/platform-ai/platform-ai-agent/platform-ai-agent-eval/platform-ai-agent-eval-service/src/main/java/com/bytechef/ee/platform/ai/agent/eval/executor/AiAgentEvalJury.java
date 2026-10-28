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

package com.bytechef.ee.platform.ai.agent.eval.executor;

import com.bytechef.ee.platform.ai.agent.eval.constant.AiAgentEvalResultConclusion;
import com.bytechef.ee.platform.ai.agent.eval.constant.AiAgentJudgeVerdictStatus;
import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.judgment.Judgment;
import io.github.markpollack.judge.judgment.JudgmentReasonCode;
import io.github.markpollack.judge.jury.SimpleJury;
import io.github.markpollack.judge.verdict.Seat;
import io.github.markpollack.judge.verdict.Verdict;
import io.github.markpollack.judge.voting.AllEligiblePassStrategy;
import io.github.markpollack.judge.voting.ErrorHandling;
import io.github.markpollack.judge.voting.ExclusionHandling;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import org.jspecify.annotations.Nullable;

/**
 * Votes a scenario's judges and maps the verdict to the persisted eval result. A scenario passes only when every judge
 * that reached a decision passed; any judge error makes it inconclusive.
 *
 * @author Ivica Cardic
 */
final class AiAgentEvalJury {

    private AiAgentEvalJury() {
    }

    static Verdict vote(List<Judge> judges) {
        SimpleJury.Builder juryBuilder = SimpleJury.builder()
            .votingStrategy(new AllEligiblePassStrategy(ErrorHandling.PROPAGATE, ExclusionHandling.EXCLUDE))
            .parallel(false);

        for (Judge judge : judges) {
            juryBuilder.judge(judge);
        }

        SimpleJury jury = juryBuilder.build();

        return jury.vote();
    }

    static AiAgentEvalResultConclusion toConclusion(Verdict verdict) {
        return switch (verdict.conclusion()) {
            case PASS -> AiAgentEvalResultConclusion.PASS;
            case FAIL -> AiAgentEvalResultConclusion.FAIL;
            case INCONCLUSIVE -> AiAgentEvalResultConclusion.INCONCLUSIVE;
            case NOT_APPLICABLE -> AiAgentEvalResultConclusion.NOT_APPLICABLE;
        };
    }

    @Nullable
    static Double toScore(Verdict verdict, AiAgentEvalResultConclusion conclusion) {
        if (conclusion != AiAgentEvalResultConclusion.PASS && conclusion != AiAgentEvalResultConclusion.FAIL) {
            return null;
        }

        Map<String, Judgment> effectiveJudgments = effectiveJudgments(verdict);

        OptionalDouble averageScore = effectiveJudgments.values()
            .stream()
            .map(Judgment::effectiveScore)
            .filter(OptionalDouble::isPresent)
            .mapToDouble(OptionalDouble::getAsDouble)
            .average();

        return averageScore.isPresent() ? averageScore.getAsDouble() : null;
    }

    /**
     * Returns each seat's judgment as the jury counted it: a judgment the jury rejected (for example an undeclared
     * NOT_APPLICABLE) is replaced by the ERROR it recorded instead.
     */
    static Map<String, Judgment> effectiveJudgments(Verdict verdict) {
        Map<String, Judgment> individualByName = verdict.individualByName();
        Map<String, Judgment> effectiveJudgments = new LinkedHashMap<>();

        for (Seat seat : verdict.seats()) {
            Judgment rejection = seat.rejection();

            effectiveJudgments.put(
                seat.verdictKey(), rejection == null ? individualByName.get(seat.verdictKey()) : rejection);
        }

        return effectiveJudgments;
    }

    static AiAgentJudgeVerdictStatus toStatus(Judgment judgment) {
        return switch (judgment.status()) {
            case PASS -> AiAgentJudgeVerdictStatus.PASS;
            case FAIL -> AiAgentJudgeVerdictStatus.FAIL;
            case ABSTAIN -> AiAgentJudgeVerdictStatus.ABSTAIN;
            case NOT_APPLICABLE -> AiAgentJudgeVerdictStatus.NOT_APPLICABLE;
            case ERROR -> AiAgentJudgeVerdictStatus.ERROR;
        };
    }

    @Nullable
    static Double toScore(Judgment judgment) {
        OptionalDouble effectiveScore = judgment.effectiveScore();

        return effectiveScore.isPresent() ? effectiveScore.getAsDouble() : null;
    }

    @Nullable
    static String toReasonCode(Judgment judgment) {
        JudgmentReasonCode reasonCode = judgment.reasonCode();

        return reasonCode == null ? null : reasonCode.wireName();
    }
}
