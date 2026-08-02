import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import AiObservabilityExportJobDialog from '../AiObservabilityExportJobDialog';

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    AiObservabilityExportFormat: {Csv: 'CSV', Json: 'JSON', Jsonl: 'JSONL'},
    AiObservabilityExportScope: {
        Prompts: 'PROMPTS',
        RequestLogs: 'REQUEST_LOGS',
        Sessions: 'SESSIONS',
        Traces: 'TRACES',
    },
    useCreateAiObservabilityExportJobMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

const renderDialog = (onClose = vi.fn()) => {
    render(<AiObservabilityExportJobDialog onClose={onClose} />);

    return onClose;
};

describe('AiObservabilityExportJobDialog', () => {
    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its title', () => {
        renderDialog();

        expect(screen.getByRole('dialog', {name: 'New Export'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('associates every label with its control', () => {
        renderDialog();

        expect(screen.getByLabelText('Scope')).toBeInTheDocument();
        expect(screen.getByLabelText('Format')).toBeInTheDocument();
    });

    it('renders the scope control as a Radix select trigger', () => {
        renderDialog();

        expect(screen.getByLabelText('Scope')).toHaveAttribute('data-slot', 'select-trigger');
    });

    it('renders the format control as a Radix select trigger', () => {
        renderDialog();

        expect(screen.getByLabelText('Format')).toHaveAttribute('data-slot', 'select-trigger');
    });
});
