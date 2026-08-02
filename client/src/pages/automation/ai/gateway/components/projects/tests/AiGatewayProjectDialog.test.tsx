import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import AiGatewayProjectDialog from '../AiGatewayProjectDialog';

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateAiGatewayProjectMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateAiGatewayProjectMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

const renderDialog = (onClose = vi.fn()) => {
    render(<AiGatewayProjectDialog onClose={onClose} workspaceId="1" />);

    return onClose;
};

describe('AiGatewayProjectDialog', () => {
    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its title', () => {
        renderDialog();

        expect(screen.getByRole('dialog', {name: 'Add Project'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('associates every label with its control', () => {
        renderDialog();

        expect(screen.getByLabelText('Name')).toBeInTheDocument();
        expect(screen.getByLabelText('Slug')).toBeInTheDocument();
        expect(screen.getByLabelText('Description (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Routing Policy ID (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Compression Enabled')).toBeInTheDocument();
        expect(screen.getByLabelText('Retry Max Attempts (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Timeout Seconds (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Caching Enabled')).toBeInTheDocument();
        expect(screen.getByLabelText('Cache TTL Minutes (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Log Retention Days (optional)')).toBeInTheDocument();
    });
});
