import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserSheet from '../ConnectedUserSheet';

const hoisted = vi.hoisted(() => ({
    connectedUserQuery: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/connectedUsers.queries', () => ({
    useGetConnectedUserQuery: hoisted.connectedUserQuery,
}));

vi.mock('@/ee/pages/embedded/connected-users/stores/useConnectedUserSheetStore', () => ({
    default: (selector: (state: object) => unknown) =>
        selector({connectedUserId: 7, connectedUserSheetOpen: true, setConnectedUserSheetOpen: vi.fn()}),
}));

vi.mock('@/ee/pages/embedded/automations/components/automation-executions-sheet/AutomationExecutionsSheet', () => ({
    default: () => null,
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-workflow-executions-sheet/IntegrationInstanceConfigurationWorkflowExecutionsSheet',
    () => ({default: () => null})
);

vi.mock('@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanel', () => ({
    default: () => <div>Connected user panel</div>,
}));

describe('ConnectedUserSheet', () => {
    beforeEach(() => {
        hoisted.connectedUserQuery.mockReset();
    });

    it('shows a skeleton of the profile and the tabs while the connected user loads', () => {
        hoisted.connectedUserQuery.mockReturnValue({data: undefined, isLoading: true});

        render(<ConnectedUserSheet />);

        expect(screen.getByRole('status', {name: 'Loading connected user'})).toBeInTheDocument();
        expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
        expect(screen.queryByText('Connected user panel')).not.toBeInTheDocument();
    });

    it('shows the panel once the connected user has loaded', () => {
        hoisted.connectedUserQuery.mockReturnValue({
            data: {environmentId: 1, externalId: 'user-7', id: 7, integrationInstances: []},
            isLoading: false,
        });

        render(<ConnectedUserSheet />);

        expect(screen.getByText('Connected user panel')).toBeInTheDocument();
        expect(screen.queryByRole('status', {name: 'Loading connected user'})).not.toBeInTheDocument();
    });
});
