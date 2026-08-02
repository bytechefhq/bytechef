import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import AiPromptVersionDialog from '../AiPromptVersionDialog';

vi.mock('@/shared/middleware/graphql', () => ({
    AiPromptVersionType: {
        Chat: 'CHAT',
        Text: 'TEXT',
    },
    useCreateAiPromptVersionMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

const renderDialog = (onClose = vi.fn()) => {
    render(<AiPromptVersionDialog onClose={onClose} promptId="1" />);

    return onClose;
};

describe('AiPromptVersionDialog', () => {
    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its title', () => {
        renderDialog();

        expect(screen.getByRole('dialog', {name: 'New Version'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('associates every label with its control', () => {
        renderDialog();

        expect(screen.getByLabelText('Content')).toBeInTheDocument();
        expect(screen.getByLabelText('Type')).toBeInTheDocument();
        expect(screen.getByLabelText('Environment')).toBeInTheDocument();
        expect(screen.getByLabelText('Commit Message')).toBeInTheDocument();
        expect(screen.getByLabelText('Set as active for this environment')).toBeInTheDocument();
    });

    it('renders the type control as a Radix select trigger', () => {
        renderDialog();

        expect(screen.getByLabelText('Type')).toHaveAttribute('data-slot', 'select-trigger');
    });

    it('renders the environment control as a Radix select trigger', () => {
        renderDialog();

        expect(screen.getByLabelText('Environment')).toHaveAttribute('data-slot', 'select-trigger');
    });

    it('shows detected variables extracted from the content', () => {
        renderDialog();

        fireEvent.change(screen.getByLabelText('Content'), {
            target: {value: 'Hello {{name}}, welcome to {{place}}.'},
        });

        expect(screen.getByRole('group', {name: 'Detected Variables'})).toBeInTheDocument();
        expect(screen.getByText('{{name}}')).toBeInTheDocument();
        expect(screen.getByText('{{place}}')).toBeInTheDocument();
    });
});
