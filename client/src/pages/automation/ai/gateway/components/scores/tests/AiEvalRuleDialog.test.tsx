import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import {AiEvalScoreConfigType} from '../../../types';
import AiEvalRuleDialog from '../AiEvalRuleDialog';

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateAiEvalRuleMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

const scoreConfigs: AiEvalScoreConfigType[] = [
    {
        categories: null,
        createdDate: '2026-01-01T00:00:00Z',
        dataType: 'NUMERIC' as AiEvalScoreConfigType['dataType'],
        description: null,
        id: '1',
        lastModifiedDate: '2026-01-01T00:00:00Z',
        maxValue: 1,
        minValue: 0,
        name: 'Relevance',
        version: 1,
        workspaceId: '1',
    },
];

const renderDialog = (onClose = vi.fn()) => {
    render(<AiEvalRuleDialog onClose={onClose} scoreConfigs={scoreConfigs} />);

    return onClose;
};

describe('AiEvalRuleDialog', () => {
    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its title', () => {
        renderDialog();

        expect(screen.getByRole('dialog', {name: 'New Eval Rule'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('associates every label with its control', () => {
        renderDialog();

        expect(screen.getByLabelText('Name')).toBeInTheDocument();
        expect(screen.getByLabelText('Score Config')).toBeInTheDocument();
        expect(screen.getByLabelText('Model')).toBeInTheDocument();
        expect(screen.getByLabelText('Prompt Template')).toBeInTheDocument();
        expect(screen.getByLabelText('Sampling Rate (0.0 - 1.0)')).toBeInTheDocument();
        expect(screen.getByLabelText('Delay (seconds)')).toBeInTheDocument();
        expect(screen.getByLabelText('Enable immediately')).toBeInTheDocument();
    });

    it('renders the score config control as a Radix select trigger', () => {
        renderDialog();

        expect(screen.getByLabelText('Score Config')).toHaveAttribute('data-slot', 'select-trigger');
    });
});
