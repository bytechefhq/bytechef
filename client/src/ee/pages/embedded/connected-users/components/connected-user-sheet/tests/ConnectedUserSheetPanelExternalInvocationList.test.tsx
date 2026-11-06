import ConnectedUserSheetPanelExternalInvocationList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelExternalInvocationList';
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    externalInvocationLogsQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useExternalInvocationLogsQuery: hoisted.externalInvocationLogsQuery,
}));

const externalInvocationLog = {
    componentName: 'slack',
    componentVersion: 1,
    connectedUserId: 7,
    connectionId: 3,
    createdDate: 1_000,
    durationMs: 42,
    environment: 2,
    errorMessage: null,
    errorType: null,
    externalUserId: 'external-user-1',
    id: '1',
    integrationInstanceId: 4,
    jobId: null,
    kind: 'COMPONENT',
    mcpServerId: 11,
    operationName: 'sendMessage',
    outcome: 'SUCCESS',
    surface: 'MCP_EMBEDDED',
    toolName: 'SLACK_SEND_MESSAGE',
};

describe('ConnectedUserSheetPanelExternalInvocationList', () => {
    beforeEach(() => {
        hoisted.externalInvocationLogsQuery.mockReset();
    });

    it("reads the connected user's embedded external invocations", () => {
        hoisted.externalInvocationLogsQuery.mockReturnValue({data: undefined, isLoading: true});

        render(<ConnectedUserSheetPanelExternalInvocationList connectedUserId={7} />);

        expect(hoisted.externalInvocationLogsQuery).toHaveBeenCalledWith({connectedUserId: 7, type: 'EMBEDDED'});
        expect(screen.getByText('Loading...')).toBeInTheDocument();
    });

    it('lists the recorded invocations', () => {
        hoisted.externalInvocationLogsQuery.mockReturnValue({
            data: {
                externalInvocationLogs: {
                    content: [externalInvocationLog],
                    number: 0,
                    size: 50,
                    totalElements: 1,
                    totalPages: 1,
                },
            },
            isLoading: false,
        });

        render(<ConnectedUserSheetPanelExternalInvocationList connectedUserId={7} />);

        expect(screen.getByText('SLACK_SEND_MESSAGE')).toBeInTheDocument();
        expect(screen.getByText('Embedded MCP')).toBeInTheDocument();
        expect(screen.getByText('slack / sendMessage')).toBeInTheDocument();
        expect(
            screen.queryByText('No external invocations have been recorded for this user yet.')
        ).not.toBeInTheDocument();
    });

    it('shows an error with a retry instead of the empty message when the invocations cannot be loaded', () => {
        const refetch = vi.fn();

        hoisted.externalInvocationLogsQuery.mockReturnValue({
            data: undefined,
            isError: true,
            isLoading: false,
            refetch,
        });

        render(<ConnectedUserSheetPanelExternalInvocationList connectedUserId={7} />);

        expect(screen.getByText('External invocations could not be loaded.')).toBeInTheDocument();
        expect(
            screen.queryByText('No external invocations have been recorded for this user yet.')
        ).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Retry'}));

        expect(refetch).toHaveBeenCalledTimes(1);
    });

    it('says so when nothing has been recorded for the user', () => {
        hoisted.externalInvocationLogsQuery.mockReturnValue({
            data: {externalInvocationLogs: {content: [], number: 0, size: 50, totalElements: 0, totalPages: 0}},
            isLoading: false,
        });

        render(<ConnectedUserSheetPanelExternalInvocationList connectedUserId={7} />);

        expect(screen.getByText('No external invocations have been recorded for this user yet.')).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });
});
