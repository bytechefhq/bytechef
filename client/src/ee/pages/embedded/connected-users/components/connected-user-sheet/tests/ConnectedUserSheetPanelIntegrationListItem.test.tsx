import {TooltipProvider} from '@/components/ui/tooltip';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {QueryClient} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserSheetPanelIntegrationListItem from '../ConnectedUserSheetPanelIntegrationListItem';

const hoisted = vi.hoisted(() => ({
    configurationsQueryMock: vi.fn(),
    integrationConfigurations: [] as unknown[] | undefined,
    integrationConfigurationsError: false,
    moveImpactQueryMock: vi.fn(),
    moveMutateAsyncMock: vi.fn(),
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', () => ({
    UNION_ENVIRONMENT_ID: -1,
    useEmbeddedScopeState: () => ({editionUnknown: false, error: false, granted: true, loading: false}),
    useHasEmbeddedScope: () => true,
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
    IntegrationInstanceKeys: {integrationInstance: (id: number) => ['integrationInstance', id]},
    useGetIntegrationInstanceQuery: () => ({data: undefined}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    useGetIntegrationVersionWorkflowsQuery: () => ({data: []}),
}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstances.mutations', () => ({
    useDeleteIntegrationInstanceMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnableIntegrationInstanceMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations', () => ({
    useMoveIntegrationInstanceConfigurationConnectedUsersMutation: () => ({mutateAsync: hoisted.moveMutateAsyncMock}),
}));

vi.mock('react-inlinesvg', () => ({
    default: () => <svg data-testid="integration-icon" />,
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

const renderItem = () =>
    render(
        <TooltipProvider>
            <ConnectedUserSheetPanelIntegrationListItem
                componentDefinition={componentDefinition}
                connectedUser={{email: 'jane@example.com', externalId: 'ext-9', name: 'Jane'}}
                connectedUserId={9}
                connectedUserIntegrationInstance={connectedUserIntegrationInstance}
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
    });

    it('sheet integration row offers Change configuration', async () => {
        await openMenu();

        expect(screen.getAllByRole('menuitem').map((item) => item.textContent)).toEqual([
            'Change configuration…',
            'Delete',
        ]);
        expect(hoisted.configurationsQueryMock).toHaveBeenCalledWith({environmentId: 1, integrationId: 7}, true);
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
        expect(screen.getByText('Jane')).toBeInTheDocument();

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
