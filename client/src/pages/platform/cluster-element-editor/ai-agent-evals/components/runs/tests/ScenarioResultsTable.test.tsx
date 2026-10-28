import {
    AiAgentEvalResultConclusion,
    AiAgentEvalResultStatus,
    AiAgentJudgeScope,
    AiAgentJudgeType,
    AiAgentJudgeVerdictStatus,
    AiAgentScenarioType,
} from '@/shared/middleware/graphql';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it} from 'vitest';

import ScenarioResultsTable from '../ScenarioResultsTable';

const buildVerdict = (id: string, status: AiAgentJudgeVerdictStatus) => ({
    explanation: 'explanation',
    id,
    judgeName: `judge ${id}`,
    judgeScope: AiAgentJudgeScope.Scenario,
    judgeType: AiAgentJudgeType.LlmRule,
    reasonCode: null,
    score: null,
    status,
});

const buildResult = (
    conclusion: AiAgentEvalResultConclusion | null,
    score: number | null,
    verdicts: ReturnType<typeof buildVerdict>[]
) => ({
    conclusion,
    createdDate: null,
    errorMessage: null,
    id: 'result-1',
    inputTokens: 0,
    outputTokens: 0,
    runIndex: null,
    scenario: {
        createdDate: null,
        expectedOutput: null,
        id: 'scenario-1',
        judges: [],
        lastModifiedDate: null,
        maxTurns: null,
        name: 'Refund request',
        personaPrompt: null,
        type: AiAgentScenarioType.SingleTurn,
        userMessage: 'I want a refund',
    },
    score,
    status: AiAgentEvalResultStatus.Completed,
    transcriptFile: null,
    verdicts,
});

afterEach(() => {
    resetAll();
});

describe('ScenarioResultsTable', () => {
    it('should show an inconclusive scenario instead of a score', () => {
        render(
            <ScenarioResultsTable
                results={[
                    buildResult(AiAgentEvalResultConclusion.Inconclusive, null, [
                        buildVerdict('1', AiAgentJudgeVerdictStatus.Pass),
                        buildVerdict('2', AiAgentJudgeVerdictStatus.Error),
                    ]),
                ]}
            />
        );

        expect(screen.getByText('Inconclusive')).toBeInTheDocument();
        expect(screen.queryByText('--')).not.toBeInTheDocument();
    });

    it('should count only judges that decided', () => {
        render(
            <ScenarioResultsTable
                results={[
                    buildResult(AiAgentEvalResultConclusion.Inconclusive, null, [
                        buildVerdict('1', AiAgentJudgeVerdictStatus.Pass),
                        buildVerdict('2', AiAgentJudgeVerdictStatus.Fail),
                        buildVerdict('3', AiAgentJudgeVerdictStatus.Error),
                    ]),
                ]}
            />
        );

        expect(screen.getByText('1/2 passed')).toBeInTheDocument();
        expect(screen.getByLabelText('1 judge(s) did not decide, 1 errored')).toBeInTheDocument();
    });

    it('should show the score of a decided scenario', () => {
        render(
            <ScenarioResultsTable
                results={[
                    buildResult(AiAgentEvalResultConclusion.Fail, 0.5, [
                        buildVerdict('1', AiAgentJudgeVerdictStatus.Pass),
                        buildVerdict('2', AiAgentJudgeVerdictStatus.Fail),
                    ]),
                ]}
            />
        );

        expect(screen.getByText('50%')).toBeInTheDocument();
        expect(screen.getByText('1/2 passed')).toBeInTheDocument();
        expect(screen.queryByText('Inconclusive')).not.toBeInTheDocument();
    });

    it('should show N/A when no judges applied', () => {
        render(<ScenarioResultsTable results={[buildResult(AiAgentEvalResultConclusion.NotApplicable, null, [])]} />);

        expect(screen.getByText('N/A')).toBeInTheDocument();
    });
});
