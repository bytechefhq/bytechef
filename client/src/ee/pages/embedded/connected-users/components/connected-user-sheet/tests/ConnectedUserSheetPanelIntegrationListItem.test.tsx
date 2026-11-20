import {TooltipProvider} from '@/components/ui/tooltip';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {QueryClient} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserSheetPanelIntegrationListItem from '../ConnectedUserSheetPanelIntegrationListItem';

const hoisted = vi.hoisted(() => ({
    changeVersionMutateAsyncMock: vi.fn(),
    configurationsQueryMock: vi.fn(),
    integrationConfigurations: [] as unknown[] | undefined,
    integrationConfigurationsError: false,
    integrationInstance: undefined as unknown,
    integrationVersions: undefined as unknown[] | undefined,
    moveImpactQueryMock: vi.fn(),
    moveMutateAsyncMock: vi.fn(),
    provisionableIntegrations: undefined as unknown[] | undefined,
    workflows: [] as unknown[],
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    IntegrationInstanceConfigurationKeys: {integrationInstanceConfigurations: ['integrationInstanceConfigurations']},
    useGetIntegrationInstanceConfigurationConnectedUsersQuery: () => ({data: undefined}),
    useGetIntegrationInstanceConfigurationMoveImpactQuery: (...args: unknown[]) => {
        hoisted.moveImpactQueryMock(...args);

        return {data: undefined};
    },
    useGetIntegrationInstanceConfigurationQuery: () => ({
        data: {environmentId: 1, id: 1, integrationId: 7, integrationVersion: 1, name: 'Current Configuration'},
    }),
    useGetIntegrationInstanceConfigurationsQuery: (...args: unknown[]) => {
        hoisted.configurationsQueryMock(...args);

        return {data: hoisted.integrationConfigurations, isError: hoisted.integrationConfigurationsError};
    },
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstances.queries', () => ({
    IntegrationInstanceKeys: {
        integrationInstance: (id: number) => ['integrationInstance', id],
        integrationInstanceConnection: (id: number) => ['integrationInstance', id, 'connection'],
    },
    useGetIntegrationInstanceConnectionQuery: () => ({data: undefined}),
    useGetIntegrationInstanceQuery: () => ({data: hoisted.integrationInstance}),
}));

vi.mock('@/ee/shared/queries/embedded/provisionableIntegrations.queries', () => ({
    useGetProvisionableIntegrationsQuery: () => ({data: hoisted.provisionableIntegrations}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationVersions.queries', () => ({
    useGetIntegrationVersionsQuery: () => ({data: hoisted.integrationVersions}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    useGetIntegrationVersionWorkflowsQuery: () => ({data: hoisted.workflows}),
}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstances.mutations', () => ({
    useDeleteIntegrationInstanceMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnableIntegrationInstanceMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateIntegrationInstanceCredentialsMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations', () => ({
    useChangeIntegrationInstanceConfigurationConnectedUserVersionMutation: () => ({
        isPending: false,
        mutateAsync: hoisted.changeVersionMutateAsyncMock,
    }),
    useMoveIntegrationInstanceConfigurationConnectedUsersMutation: () => ({mutateAsync: hoisted.moveMutateAsyncMock}),
}));

const componentDefinition = {icon: 'icon.svg', name: 'github', title: 'GitHub', version: 1};

const connectedUserIntegrationInstance = {
    componentName: 'github',
    enabled: true,
    id: 40,
    integrationId: 7,
    integrationInstanceConfigurationId: 1,
    integrationVersion: 1,
};

const renderItem = (
    integrationInstance: typeof connectedUserIntegrationInstance & {
        provisioned?: boolean;
    } = connectedUserIntegrationInstance
) =>
    render(
        <TooltipProvider>
            <ConnectedUserSheetPanelIntegrationListItem
                componentDefinition={componentDefinition}
                connectedUser={{email: 'jane@example.com', externalId: 'ext-9', name: 'Jane'}}
                connectedUserId={9}
                connectedUserIntegrationInstance={integrationInstance}
            />
        </TooltipProvider>
    );

const openMenu = async () => {
    renderItem();

    screen.getByRole('button', {name: 'More actions'}).focus();

    await userEvent.keyboard('{ArrowDown}');
};

describe('ConnectedUserSheetPanelIntegrationListItem', () => {
    beforeEach(() => {
        windowResizeObserver();

        Element.prototype.hasPointerCapture = vi.fn().mockReturnValue(false);
        Element.prototype.releasePointerCapture = vi.fn();
        Element.prototype.setPointerCapture = vi.fn();

        environmentStore.setState({currentEnvironmentId: 2});

        hoisted.integrationConfigurations = [
            {enabled: true, id: 1, integrationVersion: 1, name: 'Current Configuration'},
            {enabled: true, id: 5, integrationVersion: 3, name: 'Other Configuration'},
            {enabled: false, id: 6, integrationVersion: 3, name: 'Disabled Configuration'},
        ];
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();

        hoisted.integrationConfigurations = [];
        hoisted.integrationConfigurationsError = false;
        hoisted.integrationInstance = undefined;
        hoisted.integrationVersions = undefined;
        hoisted.provisionableIntegrations = undefined;
        hoisted.workflows = [];
    });

    it('leaves tool-only MCP workflows out of the workflows', () => {
        hoisted.integrationInstance = {id: 40, integrationInstanceWorkflows: []};
        hoisted.provisionableIntegrations = [
            {integration: {id: 7}, mcpWorkflowUuids: ['u-1'], toolOnlyMcpWorkflowUuids: ['u-2']},
        ];
        hoisted.workflows = [
            {id: 'workflow-1', label: 'Sync', workflowUuid: 'u-1'},
            {id: 'workflow-2', label: 'Search', workflowUuid: 'u-2'},
        ];

        renderItem();

        expect(screen.getByText('1 workflow')).toBeInTheDocument();
    });

    it('titles the row with the configuration name only', () => {
        hoisted.integrationInstance = {
            id: 40,
            integrationInstanceConfiguration: {name: 'Current Configuration'},
            integrationInstanceWorkflows: [],
        };

        renderItem();

        expect(screen.getByText('Current Configuration')).toBeInTheDocument();
        expect(screen.queryByText('GitHub')).not.toBeInTheDocument();
    });

    it('leaves the Credentials action to the Connections tab even for provisioned integrations', () => {
        renderItem({...connectedUserIntegrationInstance, provisioned: true});

        expect(screen.queryByRole('button', {name: 'Credentials'})).not.toBeInTheDocument();
    });

    it('sheet integration row offers Change configuration', async () => {
        await openMenu();

        expect(screen.getAllByRole('menuitem').map((item) => item.textContent)).toEqual([
            'Change configuration…',
            'Change version…',
            'Delete',
        ]);
        expect(hoisted.configurationsQueryMock).toHaveBeenCalledWith({environmentId: 1, integrationId: 7}, true);
    });

    it('opens the change version dialog with the published versions and calls the change endpoint', async () => {
        hoisted.changeVersionMutateAsyncMock.mockResolvedValue({
            movedConnectedUserIds: [9],
            needsAttentionConnectedUserIds: [],
            skipped: [],
        });
        hoisted.integrationVersions = [
            {status: 'PUBLISHED', version: 1},
            {status: 'PUBLISHED', version: 3},
            {status: 'DRAFT', version: 4},
        ];

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Change version'}));

        expect(await screen.findByRole('heading', {name: 'Change version'})).toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox', {name: 'Version'}));

        expect(screen.getAllByRole('option').map((option) => option.textContent)).toEqual(['V3 (latest)']);

        await userEvent.click(screen.getByRole('option', {name: 'V3 (latest)'}));

        expect(screen.getByText('Moves this connected user to Other Configuration · V3.')).toBeInTheDocument();
        expect(hoisted.moveImpactQueryMock).toHaveBeenLastCalledWith(1, {connectedUserIds: [9], targetId: 5}, true);

        await userEvent.click(screen.getByRole('button', {name: 'Change'}));

        await waitFor(() =>
            expect(hoisted.changeVersionMutateAsyncMock).toHaveBeenCalledWith({
                configurationChangeVersionRequest: {connectedUserId: 9, targetId: 5, version: 3},
                id: 1,
            })
        );
    });

    it('disables Change version with a tooltip when no other published version exists', async () => {
        hoisted.integrationVersions = [
            {status: 'PUBLISHED', version: 1},
            {status: 'DRAFT', version: 2},
        ];

        await openMenu();

        const item = screen.getByRole('menuitem', {name: 'Change version'});

        expect(item).toHaveAttribute('aria-disabled', 'true');

        await userEvent.hover(item.parentElement as HTMLElement);

        await waitFor(async () =>
            expect((await screen.findAllByText('No other published version')).length).toBeGreaterThan(0)
        );
    });

    it('disables Change version without a tooltip while the configurations load', async () => {
        hoisted.integrationConfigurations = undefined;
        hoisted.integrationVersions = [
            {status: 'PUBLISHED', version: 1},
            {status: 'PUBLISHED', version: 3},
        ];

        await openMenu();

        const item = screen.getByRole('menuitem', {name: 'Change version'});

        expect(item).toHaveAttribute('aria-disabled', 'true');

        await userEvent.hover(item.parentElement as HTMLElement);

        await expect(screen.findByText('No other published version', {}, {timeout: 1500})).rejects.toThrow();
    });

    it('disables Change version with a tooltip when the configurations fail to load', async () => {
        hoisted.integrationConfigurations = undefined;
        hoisted.integrationConfigurationsError = true;
        hoisted.integrationVersions = [
            {status: 'PUBLISHED', version: 1},
            {status: 'PUBLISHED', version: 3},
        ];

        await openMenu();

        const item = screen.getByRole('menuitem', {name: 'Change version'});

        expect(item).toHaveAttribute('aria-disabled', 'true');

        await userEvent.hover(item.parentElement as HTMLElement);

        await waitFor(async () =>
            expect(
                (await screen.findAllByText("Couldn't load the configurations. Try again later.")).length
            ).toBeGreaterThan(0)
        );
    });

    it('changes this user to another configuration, labelled by name rather than external id', async () => {
        hoisted.moveMutateAsyncMock.mockResolvedValue({
            movedConnectedUserIds: [9],
            needsAttentionConnectedUserIds: [],
            skipped: [],
        });

        const invalidateQueriesSpy = vi.spyOn(QueryClient.prototype, 'invalidateQueries');

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Change configuration'}));

        expect(await screen.findByRole('heading', {name: 'Change configuration'})).toBeInTheDocument();
        expect(screen.getByText(/^Moving Jane from /)).toBeInTheDocument();
        expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox', {name: 'Target'}));

        expect(await screen.findByRole('option', {name: 'Other Configuration · v3'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: /Current Configuration/})).not.toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Disabled Configuration · v3 (disabled)'})).toBeInTheDocument();

        await userEvent.click(screen.getByRole('option', {name: 'Other Configuration · v3'}));
        await userEvent.click(screen.getByRole('button', {name: 'Move'}));

        await waitFor(() =>
            expect(hoisted.moveMutateAsyncMock).toHaveBeenCalledWith({
                configurationMoveRequest: {connectedUserIds: [9], targetId: 5},
                id: 1,
            })
        );

        invalidateQueriesSpy.mockRestore();
    });

    it('offers a disabled sibling and stays enabled when it is the only one', async () => {
        hoisted.integrationConfigurations = [
            {enabled: true, id: 1, integrationVersion: 1, name: 'Current'},
            {enabled: false, id: 6, integrationVersion: 3, name: 'Disabled Configuration'},
        ];

        await openMenu();

        const item = screen.getByRole('menuitem', {name: 'Change configuration'});

        expect(item).not.toHaveAttribute('aria-disabled', 'true');

        await userEvent.click(item);
        await userEvent.click(await screen.findByRole('combobox', {name: 'Target'}));

        expect(await screen.findByRole('option', {name: 'Disabled Configuration · v3 (disabled)'})).toBeInTheDocument();
    });

    it('disables Change configuration with a tooltip when no other configuration exists', async () => {
        hoisted.integrationConfigurations = [{enabled: true, id: 1, integrationVersion: 1, name: 'Current'}];

        await openMenu();

        const item = screen.getByRole('menuitem', {name: 'Change configuration'});

        expect(item).toHaveAttribute('aria-disabled', 'true');

        await userEvent.hover(item.parentElement as HTMLElement);

        await waitFor(async () =>
            expect((await screen.findAllByText('No other configuration in this environment')).length).toBeGreaterThan(0)
        );
    });

    it('disables Change configuration with a tooltip when the configurations fail to load', async () => {
        hoisted.integrationConfigurations = undefined;
        hoisted.integrationConfigurationsError = true;

        await openMenu();

        const item = screen.getByRole('menuitem', {name: 'Change configuration'});

        expect(item).toHaveAttribute('aria-disabled', 'true');

        await userEvent.hover(item.parentElement as HTMLElement);

        await waitFor(async () =>
            expect(
                (await screen.findAllByText("Couldn't load the configurations. Try again later.")).length
            ).toBeGreaterThan(0)
        );
    });

    it('disables Change configuration without a tooltip while the configurations load', async () => {
        hoisted.integrationConfigurations = undefined;

        await openMenu();

        const item = screen.getByRole('menuitem', {name: 'Change configuration'});

        expect(item).toHaveAttribute('aria-disabled', 'true');

        await userEvent.hover(item.parentElement as HTMLElement);

        await expect(
            screen.findByText('No other configuration in this environment', {}, {timeout: 1500})
        ).rejects.toThrow();
    });
});
