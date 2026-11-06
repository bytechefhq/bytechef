import ConnectedUserSheetPanel from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanel';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationList',
    () => ({default: () => <div>integration list</div>})
);

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelMcpServerList',
    () => ({default: () => <div>mcp server list</div>})
);

vi.mock('@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelProfile', () => ({
    default: () => <div>profile</div>,
}));

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelExternalInvocationList',
    () => ({
        default: ({connectedUserId}: {connectedUserId: number}) => (
            <div>{`external invocation list ${connectedUserId}`}</div>
        ),
    })
);

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelWorkflowList',
    () => ({
        default: ({
            connectedUserEmail,
            connectedUserId,
            connectedUserName,
        }: {
            connectedUserEmail?: string;
            connectedUserId: number;
            connectedUserName?: string;
        }) => <div>{`workflow list ${connectedUserId} ${connectedUserName} ${connectedUserEmail}`}</div>,
    })
);

describe('ConnectedUserSheetPanel', () => {
    it("shows the connected user's external invocations on the External Invocations tab", async () => {
        render(<ConnectedUserSheetPanel connectedUser={{externalId: 'external-user-1', id: 7} as ConnectedUser} />);

        expect(screen.queryByText('external invocation list 7')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('tab', {name: 'External Invocations'}));

        expect(screen.getByText('external invocation list 7')).toBeInTheDocument();
    });

    it("passes the connected user's name and email to the Workflows tab", async () => {
        render(
            <ConnectedUserSheetPanel
                connectedUser={
                    {email: 'jane@example.com', externalId: 'external-user-1', id: 7, name: 'Jane'} as ConnectedUser
                }
            />
        );

        await userEvent.click(screen.getByRole('tab', {name: 'Automation Workflows'}));

        expect(screen.getByText('workflow list 7 Jane jane@example.com')).toBeInTheDocument();
    });
});
