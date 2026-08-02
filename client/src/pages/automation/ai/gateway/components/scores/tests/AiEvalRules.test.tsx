import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import {AiEvalRuleType} from '../../../types';
import AiEvalRules from '../AiEvalRules';

vi.mock('@/shared/middleware/graphql', () => ({
    useRunAiEvalRuleOnHistoricalTracesMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

const evalRule: AiEvalRuleType = {
    createdDate: '1700000000000',
    delaySeconds: 30,
    enabled: true,
    filters: null,
    id: '1',
    lastModifiedDate: '1700000000000',
    model: 'gpt-4',
    name: 'Helpfulness',
    projectId: null,
    promptTemplate: 'Rate the response',
    samplingRate: 0.5,
    scoreConfigId: '1',
    version: 1,
    workspaceId: '1',
};

const renderRules = () => {
    render(<AiEvalRules evalRules={[evalRule]} isLoading={false} />);
};

const openHistoryDialog = () => {
    fireEvent.click(screen.getByRole('button', {name: 'Run on History'}));
};

describe('AiEvalRules run-on-history dialog', () => {
    it('renders with the dialog role', () => {
        renderRules();

        openHistoryDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its title', () => {
        renderRules();

        openHistoryDialog();

        expect(screen.getByRole('dialog', {name: 'Run Helpfulness on History'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        renderRules();

        openHistoryDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('associates every label with its control', () => {
        renderRules();

        openHistoryDialog();

        expect(screen.getByLabelText('Start Date')).toBeInTheDocument();
        expect(screen.getByLabelText('End Date')).toBeInTheDocument();
    });
});
