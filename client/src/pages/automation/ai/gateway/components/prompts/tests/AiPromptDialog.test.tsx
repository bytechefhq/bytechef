import {fireEvent, render, screen, stubMutation} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiPromptDialog from '../AiPromptDialog';

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateAiPromptMutation: vi.fn(),
    useUpdateAiPromptMutation: vi.fn(),
}));

const {useCreateAiPromptMutation, useUpdateAiPromptMutation} = await import('@/shared/middleware/graphql');

const createPromptMutation = stubMutation(vi.mocked(useCreateAiPromptMutation));
const updatePromptMutation = stubMutation(vi.mocked(useUpdateAiPromptMutation));

beforeEach(() => {
    createPromptMutation.mutate.mockClear();
    updatePromptMutation.mutate.mockClear();
});

const renderDialog = (onClose = vi.fn()) => {
    render(<AiPromptDialog onClose={onClose} workspaceId="1" />);

    return onClose;
};

describe('AiPromptDialog', () => {
    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its title', () => {
        renderDialog();

        expect(screen.getByRole('dialog', {name: 'Create Prompt'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('associates every label with its control', () => {
        renderDialog();

        expect(screen.getByLabelText('Name')).toBeInTheDocument();
        expect(screen.getByLabelText('Description (optional)')).toBeInTheDocument();
    });

    it('disables the submit button until a name is entered', () => {
        renderDialog();

        expect(screen.getByRole('button', {name: 'Create'})).toBeDisabled();

        fireEvent.change(screen.getByLabelText('Name'), {target: {value: 'My Prompt'}});

        expect(screen.getByRole('button', {name: 'Create'})).toBeEnabled();
    });
});
