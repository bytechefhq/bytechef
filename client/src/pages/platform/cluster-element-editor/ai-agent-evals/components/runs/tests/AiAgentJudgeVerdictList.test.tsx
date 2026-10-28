import {AiAgentJudgeScope, AiAgentJudgeType, AiAgentJudgeVerdictStatus} from '@/shared/middleware/graphql';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it} from 'vitest';

import AiAgentJudgeVerdictList from '../AiAgentJudgeVerdictList';

const buildVerdict = (status: AiAgentJudgeVerdictStatus, reasonCode: string | null = null) => ({
    explanation: `${status} explanation`,
    id: status,
    judgeName: `${status} judge`,
    judgeScope: AiAgentJudgeScope.Scenario,
    judgeType: AiAgentJudgeType.LlmRule,
    reasonCode,
    score: null,
    status,
});

afterEach(() => {
    resetAll();
});

describe('AiAgentJudgeVerdictList', () => {
    it('should show a distinct state for every verdict status', () => {
        render(
            <AiAgentJudgeVerdictList
                verdicts={[
                    buildVerdict(AiAgentJudgeVerdictStatus.Pass),
                    buildVerdict(AiAgentJudgeVerdictStatus.Fail),
                    buildVerdict(AiAgentJudgeVerdictStatus.Abstain),
                    buildVerdict(AiAgentJudgeVerdictStatus.NotApplicable),
                    buildVerdict(AiAgentJudgeVerdictStatus.Error, 'judge_failed'),
                ]}
            />
        );

        expect(screen.getByLabelText('Passed')).toBeInTheDocument();
        expect(screen.getByLabelText('Failed')).toBeInTheDocument();
        expect(screen.getByLabelText('Abstained')).toBeInTheDocument();
        expect(screen.getByLabelText('Not applicable')).toBeInTheDocument();
        expect(screen.getByLabelText('Judge error')).toBeInTheDocument();
    });

    it('should label an errored verdict instead of showing it as failed', () => {
        render(<AiAgentJudgeVerdictList verdicts={[buildVerdict(AiAgentJudgeVerdictStatus.Error, 'judge_failed')]} />);

        expect(screen.queryByLabelText('Failed')).not.toBeInTheDocument();
        expect(screen.getByText('Judge error')).toHaveAttribute('title', 'judge_failed');
    });

    it('should not add a status label to decided verdicts', () => {
        render(
            <AiAgentJudgeVerdictList
                verdicts={[buildVerdict(AiAgentJudgeVerdictStatus.Pass), buildVerdict(AiAgentJudgeVerdictStatus.Fail)]}
            />
        );

        expect(screen.queryByText('Judge error')).not.toBeInTheDocument();
        expect(screen.queryByText('Abstained')).not.toBeInTheDocument();
        expect(screen.queryByText('Not applicable')).not.toBeInTheDocument();
    });
});
