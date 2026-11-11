import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import ExternalInvocationDetailsDialog from './ExternalInvocationDetailsDialog';

const baseLog = {
    a2aServerId: null,
    componentName: 'slack',
    componentVersion: 1,
    connectedUserId: 7,
    connectionId: null,
    createdDate: 1_000,
    durationMs: 42,
    environment: null,
    errorMessage: null,
    errorType: null,
    externalUserId: 'external-user-1',
    id: '1',
    integrationInstanceId: 4,
    jobId: null,
    kind: 'COMPONENT',
    mcpServerId: null,
    operationName: 'sendMessage',
    source: 'API_ACTION',
    status: 'SUCCESS',
    toolName: null,
    workflowName: null,
    workspaceId: null,
};

describe('ExternalInvocationDetailsDialog', () => {
    it('shows what was called, how it ended and how long it took', () => {
        render(<ExternalInvocationDetailsDialog externalInvocationLog={baseLog} onClose={vi.fn()} />);

        expect(screen.getByRole('dialog', {name: 'slack / sendMessage'})).toBeInTheDocument();
        expect(screen.getByText('SUCCESS')).toBeInTheDocument();
        expect(screen.getByText('API (Action)')).toBeInTheDocument();
        expect(screen.getByText('42ms')).toBeInTheDocument();
        expect(screen.getByText('external-user-1')).toBeInTheDocument();
        expect(screen.queryByText('Error')).not.toBeInTheDocument();
    });

    it('shows the error of a failed call', () => {
        render(
            <ExternalInvocationDetailsDialog
                externalInvocationLog={{
                    ...baseLog,
                    errorMessage: 'Connection required',
                    errorType: 'ConnectionRequiredException',
                    status: 'ERROR',
                }}
                onClose={vi.fn()}
            />
        );

        expect(screen.getByText('ConnectionRequiredException')).toBeInTheDocument();
        expect(screen.getByText('Connection required')).toBeInTheDocument();
    });

    it('calls onClose when the dialog is dismissed', async () => {
        const onClose = vi.fn();

        render(<ExternalInvocationDetailsDialog externalInvocationLog={baseLog} onClose={onClose} />);

        await userEvent.keyboard('{Escape}');

        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
