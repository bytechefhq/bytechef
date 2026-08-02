import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import AiPromptDialog from '../AiPromptDialog';

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateAiPromptMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateAiPromptMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

const renderDialog = (onClose = vi.fn()) => {
    render(
        <QueryClientProvider client={new QueryClient()}>
            <AiPromptDialog onClose={onClose} workspaceId="1" />
        </QueryClientProvider>
    );

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
