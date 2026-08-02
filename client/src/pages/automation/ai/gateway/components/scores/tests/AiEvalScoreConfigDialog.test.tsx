import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import {AiEvalScoreConfigType} from '../../../types';
import AiEvalScoreConfigDialog from '../AiEvalScoreConfigDialog';

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    AiEvalScoreDataType: {Boolean: 'BOOLEAN', Categorical: 'CATEGORICAL', Numeric: 'NUMERIC'},
    useCreateAiEvalScoreConfigMutation: () => ({isPending: false, mutate: vi.fn()}),
    useDeleteAiEvalScoreConfigMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateAiEvalScoreConfigMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

const categoricalConfig: AiEvalScoreConfigType = {
    categories: '["good", "bad"]',
    createdDate: '2026-01-01T00:00:00Z',
    dataType: 'CATEGORICAL' as AiEvalScoreConfigType['dataType'],
    description: 'A categorical score',
    id: '1',
    lastModifiedDate: '2026-01-01T00:00:00Z',
    maxValue: null,
    minValue: null,
    name: 'Quality',
    version: 1,
    workspaceId: '1',
};

const renderDialog = (onClose = vi.fn()) => {
    render(<AiEvalScoreConfigDialog onClose={onClose} />);

    return onClose;
};

describe('AiEvalScoreConfigDialog', () => {
    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its title', () => {
        renderDialog();

        expect(screen.getByRole('dialog', {name: 'New Score Config'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('associates every label with its control', () => {
        renderDialog();

        expect(screen.getByLabelText('Name')).toBeInTheDocument();
        expect(screen.getByLabelText('Data Type')).toBeInTheDocument();
        expect(screen.getByLabelText('Min Value')).toBeInTheDocument();
        expect(screen.getByLabelText('Max Value')).toBeInTheDocument();
        expect(screen.getByLabelText('Description')).toBeInTheDocument();
    });

    it('associates the categories label when data type is categorical', () => {
        render(<AiEvalScoreConfigDialog editingConfig={categoricalConfig} onClose={vi.fn()} />);

        expect(screen.getByLabelText('Categories (JSON array)')).toBeInTheDocument();
    });

    it('renders the data type control as a Radix select trigger', () => {
        renderDialog();

        expect(screen.getByLabelText('Data Type')).toHaveAttribute('data-slot', 'select-trigger');
    });

    it('asks for confirmation before deleting instead of deleting straight away', () => {
        render(<AiEvalScoreConfigDialog editingConfig={categoricalConfig} onClose={vi.fn()} />);

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(screen.getByRole('alertdialog', {name: 'Delete Quality score config?'})).toBeInTheDocument();
    });
});
