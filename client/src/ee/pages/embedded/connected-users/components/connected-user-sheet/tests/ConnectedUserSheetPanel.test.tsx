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
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelToolInvocationList',
    () => ({
        default: ({connectedUserId}: {connectedUserId: number}) => (
            <div>{`tool invocation list ${connectedUserId}`}</div>
        ),
    })
);

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelWorkflowList',
    () => ({default: () => <div>workflow list</div>})
);

describe('ConnectedUserSheetPanel', () => {
    it("shows the connected user's tool invocations on the Tool Invocations tab", async () => {
        render(<ConnectedUserSheetPanel connectedUser={{externalId: 'external-user-1', id: 7} as ConnectedUser} />);

        expect(screen.queryByText('tool invocation list 7')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('tab', {name: 'Tool Invocations'}));

        expect(screen.getByText('tool invocation list 7')).toBeInTheDocument();
    });
});
