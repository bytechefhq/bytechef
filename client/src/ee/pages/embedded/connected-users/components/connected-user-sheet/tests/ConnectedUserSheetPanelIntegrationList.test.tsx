import ConnectedUserSheetPanelIntegrationList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationList';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: [{name: 'github'}]}),
}));

const hoisted = vi.hoisted(() => ({provisionableIntegrations: [{integration: {id: 5}}] as unknown[]}));

vi.mock('@/ee/shared/queries/embedded/provisionableIntegrations.queries', () => ({
    useGetProvisionableIntegrationsQuery: () => ({data: hoisted.provisionableIntegrations}),
}));

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-integration-dialog/ProvisionIntegrationDialog',
    () => ({
        default: ({connectedUserId}: {connectedUserId: number}) => <div>{`provision dialog ${connectedUserId}`}</div>,
    })
);

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationListItem',
    () => ({
        default: ({connectedUserIntegrationInstance}: {connectedUserIntegrationInstance: {id: number}}) => (
            <div>{`integration instance ${connectedUserIntegrationInstance.id}`}</div>
        ),
    })
);

const renderList = (connectedUserIntegrationInstances: ConnectedUserIntegrationInstance[]) =>
    render(
        <MemoryRouter>
            <ConnectedUserSheetPanelIntegrationList
                connectedUser={{externalId: 'external-user-1'}}
                connectedUserId={1050}
                connectedUserIntegrationInstances={connectedUserIntegrationInstances}
            />
        </MemoryRouter>
    );

describe('ConnectedUserSheetPanelIntegrationList', () => {
    beforeEach(() => {
        hoisted.provisionableIntegrations = [{integration: {id: 5}}];
    });

    it("lists the user's integrations", () => {
        renderList([{componentName: 'github', id: 3} as ConnectedUserIntegrationInstance]);
        expect(screen.getByText('integration instance 3')).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: 'No integrations yet'})).not.toBeInTheDocument();
    });

    it('shows an empty card with the Provision Integration button when the user has no integrations', async () => {
        renderList([]);

        expect(screen.getByRole('heading', {name: 'No integrations yet'})).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Provision Integration'}));

        expect(screen.getByText('provision dialog 1050')).toBeInTheDocument();
    });

    it('offers to open the Integrations page when no integration can be provisioned', () => {
        hoisted.provisionableIntegrations = [];

        renderList([]);

        expect(screen.getByText(/No integration can be provisioned yet/)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Go to Integrations'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Provision Integration'})).not.toBeInTheDocument();
    });
});
