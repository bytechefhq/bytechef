import {Collapsible} from '@/components/ui/collapsible';
import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationInstanceConfigurationListItem from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-list/IntegrationInstanceConfigurationListItem';
import {IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {QueryClient} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    captureIntegrationInstanceConfigurationEnabledMock: vi.fn(),
    configurationsQueryMock: vi.fn(),
    connectedUsers: [] as unknown[],
    deleteIntegrationInstanceConfigurationMock: vi.fn(),
    integrationConfigurations: [] as unknown[] | undefined,
    integrationConfigurationsError: false,
    moveImpactQueryMock: vi.fn(),
    moveImpactResult: {data: undefined} as Record<string, unknown>,
    moveMutateAsyncMock: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    IntegrationInstanceConfigurationKeys: {integrationInstanceConfigurations: ['integrationInstanceConfigurations']},
    useGetIntegrationInstanceConfigurationConnectedUsersQuery: () => ({data: hoisted.connectedUsers}),
    useGetIntegrationInstanceConfigurationMoveImpactQuery: (...args: unknown[]) => {
        hoisted.moveImpactQueryMock(...args);

        return hoisted.moveImpactResult;
    },
    useGetIntegrationInstanceConfigurationsQuery: (...args: unknown[]) => {
        hoisted.configurationsQueryMock(...args);

        return {data: hoisted.integrationConfigurations, isError: hoisted.integrationConfigurationsError};
    },
}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations', () => ({
    useDeleteIntegrationInstanceConfigurationMutation: ({onSuccess}: {onSuccess?: () => void}) => ({
        isPending: false,
        mutate: (id: number) => {
            hoisted.deleteIntegrationInstanceConfigurationMock(id);

            onSuccess?.();
        },
    }),
    useEnableIntegrationInstanceConfigurationMutation: ({
        onSuccess,
    }: {
        onSuccess?: (result: void, variables: {enable: boolean; id: number}) => void;
    }) => ({
        isPending: false,
        mutate: (variables: {enable: boolean; id: number}) => onSuccess?.(undefined, variables),
    }),
    useMoveIntegrationInstanceConfigurationConnectedUsersMutation: (options?: {onSuccess?: () => void}) => ({
        mutateAsync: async (variables: unknown) => {
            const response = await hoisted.moveMutateAsyncMock(variables);

            options?.onSuccess?.();

            return response;
        },
    }),
    useUpdateIntegrationInstanceConfigurationTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({
        captureIntegrationInstanceConfigurationEnabled: hoisted.captureIntegrationInstanceConfigurationEnabledMock,
    }),
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog',
    () => ({
        default: () => <div data-testid="integration-instance-configuration-dialog" />,
    })
);

vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <svg data-src={src} data-testid="integration-icon" />,
}));

const integrationInstanceConfiguration = {
    enabled: true,
    environmentId: 2,
    id: 1,
    integrationId: 7,
    integrationInstanceConfigurationWorkflows: [{workflowId: 'workflow-1'}, {workflowId: 'workflow-2'}],
    integrationVersion: 1,
    name: 'My Instance Configuration',
    tags: [],
} as unknown as IntegrationInstanceConfiguration;

const renderItem = (
    overrides: Partial<IntegrationInstanceConfiguration> = {},
    showIntegrationIcon?: boolean,
    showTags?: boolean
) =>
    render(
        <TooltipProvider>
            <Collapsible>
                <IntegrationInstanceConfigurationListItem
                    integrationInstanceConfiguration={{...integrationInstanceConfiguration, ...overrides}}
                    showIntegrationIcon={showIntegrationIcon}
                    showTags={showTags}
                />
            </Collapsible>
        </TooltipProvider>
    );

const openMenu = async () => {
    renderItem();

    screen
        .getAllByRole('button')
        .find((button) => button.getAttribute('aria-haspopup') === 'menu')!
        .focus();

    await userEvent.keyboard('{ArrowDown}');
};

const getWorkflowsTrigger = () => screen.getByRole('button', {hidden: true, name: /2 workflows/});

describe('IntegrationInstanceConfigurationListItem row click', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('should expand the workflows when the row is clicked', async () => {
        renderItem();

        await userEvent.click(screen.getByText('My Instance Configuration'));

        await waitFor(() => expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'true'));
    });

    it('should keep the workflows collapsed when a menu row is clicked', async () => {
        await openMenu();

        expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'false');

        await userEvent.click(screen.getByRole('menuitem', {name: 'Update Integration Version'}));

        expect(await screen.findByTestId('integration-instance-configuration-dialog')).toBeInTheDocument();
        expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'false');
    });

    it('should keep the workflows collapsed when the menu itself is clicked', async () => {
        await openMenu();

        await userEvent.click(screen.getByRole('menu'));

        expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'false');
    });
});

describe('IntegrationInstanceConfigurationListItem integration icon', () => {
    const integration = {icon: 'icon.svg'} as IntegrationInstanceConfiguration['integration'];

    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('should show the integration icon by default', () => {
        renderItem({integration});

        expect(screen.getByTestId('integration-icon')).toHaveAttribute('data-src', 'icon.svg');
    });

    it('should hide the integration icon when showIntegrationIcon is false', () => {
        renderItem({integration}, false);

        expect(screen.queryByTestId('integration-icon')).not.toBeInTheDocument();
    });
});

describe('IntegrationInstanceConfigurationListItem tags', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('should show the tags by default', () => {
        renderItem();

        expect(screen.getByText('Tags:')).toBeInTheDocument();
    });

    it('should hide the tags when showTags is false', () => {
        renderItem({}, undefined, false);

        expect(screen.queryByText('Tags:')).not.toBeInTheDocument();
    });
});

describe('IntegrationInstanceConfigurationListItem enable analytics', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('should capture the enabled event when the configuration is turned on', async () => {
        renderItem({enabled: false});

        await userEvent.click(screen.getByRole('switch'));

        expect(hoisted.captureIntegrationInstanceConfigurationEnabledMock).toHaveBeenCalledTimes(1);
    });

    it('should not capture the enabled event when the configuration is turned off', async () => {
        renderItem();

        await userEvent.click(screen.getByRole('switch'));

        expect(hoisted.captureIntegrationInstanceConfigurationEnabledMock).not.toHaveBeenCalled();
    });
});

describe('IntegrationInstanceConfigurationListItem delete', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('refreshes the integration list after deleting so the configuration count updates', async () => {
        const invalidateQueriesSpy = vi.spyOn(QueryClient.prototype, 'invalidateQueries');

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteIntegrationInstanceConfigurationMock).toHaveBeenCalledWith(1);
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['integrations']});

        invalidateQueriesSpy.mockRestore();
    });
});

describe('IntegrationInstanceConfigurationListItem Move Users', () => {
    beforeEach(() => {
        windowResizeObserver();

        Element.prototype.hasPointerCapture = vi.fn().mockReturnValue(false);
        Element.prototype.releasePointerCapture = vi.fn();
        Element.prototype.setPointerCapture = vi.fn();

        environmentStore.setState({currentEnvironmentId: 2});

        hoisted.connectedUsers = [
            {connectedUserId: 11, email: 'jane@example.com', externalId: 'ext-11', name: 'Jane'},
            {connectedUserId: 12, externalId: 'ext-12'},
        ];
        hoisted.integrationConfigurations = [
            {enabled: true, id: 1, integrationVersion: 1, name: 'My Instance Configuration'},
            {enabled: true, id: 5, integrationVersion: 3, name: 'Other Configuration'},
        ];
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();

        hoisted.connectedUsers = [];
        hoisted.integrationConfigurations = [];
        hoisted.integrationConfigurationsError = false;
        hoisted.moveImpactResult = {data: undefined};
    });

    it("Move Users lists the configuration's connected users and other configurations of the same integration and environment as targets", async () => {
        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Move Users'}));

        expect(hoisted.configurationsQueryMock).toHaveBeenCalledWith({environmentId: 2, integrationId: 7}, true);
        expect(await screen.findByText('Jane')).toBeInTheDocument();
        expect(screen.getByText('ext-12')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox', {name: 'Target'}));

        expect(await screen.findByRole('option', {name: 'Other Configuration · v3'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: /My Instance Configuration/})).not.toBeInTheDocument();
    });

    it('moves the selected users to the chosen configuration and refreshes the configurations', async () => {
        const invalidateQueriesSpy = vi.spyOn(QueryClient.prototype, 'invalidateQueries');

        hoisted.moveMutateAsyncMock.mockResolvedValue({
            movedConnectedUserIds: [11, 12],
            needsAttentionConnectedUserIds: [],
            skipped: [],
        });

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Move Users'}));
        await userEvent.click(await screen.findByRole('combobox', {name: 'Target'}));
        await userEvent.click(await screen.findByRole('option', {name: 'Other Configuration · v3'}));
        await userEvent.click(screen.getByRole('button', {name: 'Move'}));

        expect(await screen.findByText('2 moved, 0 need attention')).toBeInTheDocument();
        expect(hoisted.moveMutateAsyncMock).toHaveBeenCalledWith({
            configurationMoveRequest: {connectedUserIds: [11, 12], targetId: 5},
            id: 1,
        });
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['integrationInstanceConfigurations']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['connectedUsers']});

        invalidateQueriesSpy.mockRestore();
    });

    it('disables Move while the move preview loads', async () => {
        hoisted.moveImpactResult = {data: undefined, isError: false, isLoading: true};

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Move Users'}));
        await userEvent.click(await screen.findByRole('combobox', {name: 'Target'}));
        await userEvent.click(await screen.findByRole('option', {name: 'Other Configuration · v3'}));

        expect(screen.getByRole('button', {name: 'Move'})).toBeDisabled();
    });

    it('says when the move preview fails and still allows the move', async () => {
        hoisted.moveImpactResult = {data: undefined, isError: true, isLoading: false};

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Move Users'}));
        await userEvent.click(await screen.findByRole('combobox', {name: 'Target'}));
        await userEvent.click(await screen.findByRole('option', {name: 'Other Configuration · v3'}));

        expect(screen.getByText('Could not load the move preview.')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Move'})).toBeEnabled();
    });

    it('notes the MCP servers that use either configuration', async () => {
        hoisted.moveImpactResult = {
            data: {mcpServerCount: 2, skipped: [], workflows: []},
            isError: false,
            isLoading: false,
        };

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Move Users'}));

        expect(await screen.findByText(/^2 MCP servers use one of these configurations\./)).toBeInTheDocument();
    });

    it('disables Move Users with a tooltip when the integration has no other configuration in the environment', async () => {
        hoisted.integrationConfigurations = [{enabled: true, id: 1, integrationVersion: 1, name: 'Mine'}];

        await openMenu();

        const item = screen.getByRole('menuitem', {name: 'Move Users'});

        expect(item).toHaveAttribute('aria-disabled', 'true');

        await userEvent.hover(item.parentElement as HTMLElement);

        await waitFor(async () =>
            expect((await screen.findAllByText('No other configuration in this environment')).length).toBeGreaterThan(0)
        );
    });

    it('disables Move Users with a tooltip when the configurations fail to load', async () => {
        hoisted.integrationConfigurations = undefined;
        hoisted.integrationConfigurationsError = true;

        await openMenu();

        const item = screen.getByRole('menuitem', {name: 'Move Users'});

        expect(item).toHaveAttribute('aria-disabled', 'true');

        await userEvent.hover(item.parentElement as HTMLElement);

        await waitFor(async () =>
            expect(
                (await screen.findAllByText("Couldn't load the configurations. Try again later.")).length
            ).toBeGreaterThan(0)
        );
    });

    it('disables Move Users without a tooltip while the configurations load', async () => {
        hoisted.integrationConfigurations = undefined;

        await openMenu();

        const item = screen.getByRole('menuitem', {name: 'Move Users'});

        expect(item).toHaveAttribute('aria-disabled', 'true');

        await userEvent.hover(item.parentElement as HTMLElement);

        await expect(
            screen.findByText('No other configuration in this environment', {}, {timeout: 1500})
        ).rejects.toThrow();
    });
});
