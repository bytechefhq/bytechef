import ConnectedUserSheetPanelIntegrationList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationList';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: [{icon: 'github.svg', name: 'github', title: 'GitHub'}]}),
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    useGetIntegrationsQuery: () => ({
        data: [
            {id: 7, name: 'GitHub Issues'},
            {id: 8, name: 'GitHub Releases'},
        ],
    }),
}));

vi.mock('react-inlinesvg', () => ({
    default: () => <svg data-testid="integration-icon" />,
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
        renderList([{componentName: 'github', id: 3, integrationId: 7} as ConnectedUserIntegrationInstance]);

        expect(screen.getByText('integration instance 3')).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: 'No integrations yet'})).not.toBeInTheDocument();
    });

    it('groups instances per integration, not per component, under the integration name and icon', () => {
        renderList([
            {componentName: 'github', id: 3, integrationId: 8} as ConnectedUserIntegrationInstance,
            {componentName: 'github', id: 4, integrationId: 7} as ConnectedUserIntegrationInstance,
            {componentName: 'github', id: 5, integrationId: 7} as ConnectedUserIntegrationInstance,
        ]);

        const issuesGroup = screen.getByRole('region', {name: 'GitHub Issues'});
        const releasesGroup = screen.getByRole('region', {name: 'GitHub Releases'});

        expect(within(issuesGroup).getByText('integration instance 4')).toBeInTheDocument();
        expect(within(issuesGroup).getByText('integration instance 5')).toBeInTheDocument();
        expect(within(releasesGroup).getByText('integration instance 3')).toBeInTheDocument();
        expect(within(issuesGroup).getByTestId('integration-icon')).toBeInTheDocument();
        expect(issuesGroup.compareDocumentPosition(releasesGroup) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
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
