import {TooltipProvider} from '@/components/ui/tooltip';
import useAutomationWorkflowExecutionsSheetStore from '@/ee/pages/embedded/automation-workflows/stores/useAutomationWorkflowExecutionsSheetStore';
import {AutomationWorkflowProjectConfigurationsQuery} from '@/shared/middleware/graphql';
import {resetAll, screen, userEvent, waitFor, windowResizeObserver, within} from '@/shared/util/test-utils';
import {render} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectConfigurationListItem from '../AutomationWorkflowProjectConfigurationListItem';

const mocks = vi.hoisted(() => ({
    deleteImpact: {connectedUserCount: 3, referenceWorkflowCount: 2} as
        {connectedUserCount: number; referenceWorkflowCount: number} | undefined,
    deleteImpactError: false,
    deniedScopes: new Set<string>(),
    enableConfigurationMutate: vi.fn(),
    enableWorkflowMutate: vi.fn(),
    invalidateQueries: vi.fn(),
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/hooks/useHasEmbeddedScope')>()),
    useHasEmbeddedScope: (scope: string) => !mocks.deniedScopes.has(scope),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationWorkflowProjectConfigurationConnectedUsersQuery: () => ({data: undefined}),
    useAutomationWorkflowProjectConfigurationDeleteImpactQuery: (
        _variables: unknown,
        options?: {enabled?: boolean}
    ) => ({
        data:
            options?.enabled && mocks.deleteImpact
                ? {automationWorkflowProjectConfigurationDeleteImpact: mocks.deleteImpact}
                : undefined,
        isError: mocks.deleteImpactError,
    }),
    useEnableAutomationWorkflowProjectConfigurationMutation: () => ({
        isPending: false,
        mutate: mocks.enableConfigurationMutate,
    }),
    useEnableAutomationWorkflowProjectConfigurationWorkflowMutation: () => ({
        isPending: false,
        mutate: mocks.enableWorkflowMutate,
    }),
}));

vi.mock('@/shared/components/workflow/WorkflowTriggerAndComponentsRow', () => ({
    default: () => null,
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: mocks.invalidateQueries}),
}));

type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];

const configuration: AutomationWorkflowProjectConfigurationType = {
    automationHubVisible: true,
    connectedUserCount: 0,
    enabled: false,
    environmentId: '2',
    id: 'configuration-1',
    lastExecutionDate: null,
    lastModifiedDate: null,
    name: 'STAGING',
    permissionExpression: null,
    projectId: 'project-1',
    projectVersion: 2,
    workflows: [
        {
            enabled: true,
            label: 'Mailer',
            lastExecutionDate: null,
            providedConnections: null,
            providedInputs: null,
            workflowId: 'pinned-wf-1',
            workflowUuid: 'wf-1',
        },
    ],
};

const project = {
    categoryId: null,
    configurationCount: 0,
    description: null,
    id: 'project-1',
    lastPublishedDate: null,
    lastPublishedVersion: 2,
    name: 'Mail Project',
    permissionExpression: null,
    published: true,
    tagIds: [],
    version: 3,
    workflowTemplates: [
        {
            components: [],
            description: null,
            label: 'Mailer',
            lastExecutionDate: null,
            lastModifiedDate: null,
            permissionExpression: null,
            triggers: [],
            workflowId: 'draft-wf-1',
            workflowUuid: 'wf-1',
        },
    ],
};

const renderItem = (
    overrides: {
        configuration?: typeof configuration;
        onDelete?: () => void;
        onEdit?: () => void;
        configurations?: (typeof configuration)[];
        onUpdateVersion?: () => void;
        project?: typeof project;
    } = {}
) =>
    render(
        <TooltipProvider>
            <AutomationWorkflowProjectConfigurationListItem
                configuration={overrides.configuration ?? configuration}
                configurations={overrides.configurations}
                onDelete={overrides.onDelete ?? vi.fn()}
                onEdit={overrides.onEdit ?? vi.fn()}
                onUpdateVersion={overrides.onUpdateVersion ?? vi.fn()}
                project={overrides.project ?? project}
            />
        </TooltipProvider>
    );

describe('AutomationWorkflowProjectConfigurationListItem', () => {
    beforeEach(() => {
        mocks.deniedScopes.clear();

        windowResizeObserver();

        mocks.deleteImpact = {connectedUserCount: 3, referenceWorkflowCount: 2};
        mocks.deleteImpactError = false;

        useAutomationWorkflowExecutionsSheetStore.setState({
            automationWorkflowExecutionsSheetOpen: false,
            environmentId: undefined,
            projectName: undefined,
            workflow: undefined,
        });
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it("opens the workflow's executions for the configuration's environment on click", async () => {
        const user = userEvent.setup();

        renderItem();

        await user.click(await screen.findByRole('button', {name: 'Mailer'}));

        const state = useAutomationWorkflowExecutionsSheetStore.getState();

        expect(state.automationWorkflowExecutionsSheetOpen).toBe(true);
        expect(state.environmentId).toBe(2);
        expect(state.projectName).toBe('Mail Project');
        expect(state.workflow).toEqual({label: 'Mailer', workflowId: 'pinned-wf-1', workflowUuid: 'wf-1'});
    });

    it('opens the executions of a pinned workflow the latest draft no longer has', async () => {
        const user = userEvent.setup();

        renderItem({project: {...project, workflowTemplates: []}});

        await user.click(await screen.findByRole('button', {name: 'Mailer'}));

        const state = useAutomationWorkflowExecutionsSheetStore.getState();

        expect(state.automationWorkflowExecutionsSheetOpen).toBe(true);
        expect(state.workflow).toEqual({label: 'Mailer', workflowId: 'pinned-wf-1', workflowUuid: 'wf-1'});
    });

    it('collapses and expands the workflows when the row is clicked', async () => {
        const user = userEvent.setup();

        renderItem();

        expect(screen.getByRole('button', {name: 'Mailer'})).toBeInTheDocument();

        await user.click(screen.getByText('STAGING'));

        expect(screen.queryByRole('button', {name: 'Mailer'})).not.toBeInTheDocument();

        await user.click(screen.getByText('STAGING'));

        expect(screen.getByRole('button', {name: 'Mailer'})).toBeInTheDocument();
    });

    it('shows the newer version hint when the project published a later version', () => {
        renderItem({project: {...project, lastPublishedVersion: 3}});

        expect(screen.getByText('V2')).toBeInTheDocument();
        expect(screen.getByText('Newer version available')).toBeInTheDocument();
    });

    it('does not show the newer version hint when the configuration is current', () => {
        renderItem();

        expect(screen.queryByText('Newer version available')).not.toBeInTheDocument();
    });

    it('toggles the configuration', async () => {
        const user = userEvent.setup();

        renderItem();

        mocks.enableConfigurationMutate.mockImplementation((_variables, options: {onSuccess: () => void}) =>
            options.onSuccess()
        );

        await user.click(screen.getByRole('switch', {name: 'Enable Configuration'}));

        expect(mocks.enableConfigurationMutate.mock.calls[0][0]).toEqual({enable: true, id: 'configuration-1'});
        expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automationWorkflowProjectConfigurations']});
        expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automationWorkflowProjects']});
    });

    it('offers editing and updating the project version as separate actions', async () => {
        const user = userEvent.setup();

        const onEdit = vi.fn();
        const onUpdateVersion = vi.fn();

        renderItem({onEdit, onUpdateVersion});

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Update Project Version'}));

        expect(onUpdateVersion).toHaveBeenCalledWith(configuration);
        expect(onEdit).not.toHaveBeenCalled();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Edit Configuration'}));

        expect(onEdit).toHaveBeenCalledWith(configuration);
        expect(onUpdateVersion).toHaveBeenCalledTimes(1);
    });

    it('states how many workflows and users deleting the configuration removes', async () => {
        const user = userEvent.setup();

        renderItem();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Delete Configuration'}));

        expect(
            await screen.findByText('This deletes 2 workflows of 3 users. Copies keep working. This cannot be undone.')
        ).toBeInTheDocument();
        expect(screen.queryByText(/paused/)).not.toBeInTheDocument();
    });

    it('shows the configuration name without the environment', () => {
        renderItem();

        expect(screen.getByText('STAGING')).toBeInTheDocument();
        expect(screen.queryByText('Staging')).not.toBeInTheDocument();
    });

    it('shows the connected user count', () => {
        renderItem({configuration: {...configuration, connectedUserCount: 3}});

        expect(screen.getByText('3 users')).toBeInTheDocument();
    });

    it('uses the singular for one connected user', () => {
        renderItem({configuration: {...configuration, connectedUserCount: 1}});

        expect(screen.getByText('1 user')).toBeInTheDocument();
    });

    it('orders the menu as Edit, Update Project Version, Move Users, separator, Delete', async () => {
        const user = userEvent.setup();

        renderItem();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));

        const menu = await screen.findByRole('menu');

        const entries = Array.from(menu.querySelectorAll('[role="menuitem"], [role="separator"]'));

        expect(entries.map((entry) => entry.getAttribute('aria-label') || entry.getAttribute('role'))).toEqual([
            'Edit Configuration',
            'Update Project Version',
            'Move Users',
            'separator',
            'Delete Configuration',
        ]);
        expect(within(menu).getByText('Move Users…')).toBeInTheDocument();
    });

    it('disables Move Users with a tooltip when there is no other configuration in the environment', async () => {
        const user = userEvent.setup();

        renderItem({configurations: [{...configuration, environmentId: '1', id: 'other'}]});

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));

        const moveUsersItem = await screen.findByRole('menuitem', {name: 'Move Users'});

        expect(moveUsersItem).toHaveAttribute('aria-disabled', 'true');

        await user.hover(moveUsersItem.parentElement as HTMLElement);

        expect((await screen.findAllByText('No other configuration in this environment')).length).toBeGreaterThan(0);
    });

    it('does not offer the configuration itself as a move target', async () => {
        const user = userEvent.setup();

        renderItem({configuration: {...configuration, connectedUserCount: 2}, configurations: [configuration]});

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));

        expect(await screen.findByRole('menuitem', {name: 'Move Users'})).toHaveAttribute('aria-disabled', 'true');
    });

    it('disables Move Users with a tooltip when the configuration has no connected users', async () => {
        const user = userEvent.setup();

        renderItem({
            configuration: {...configuration, connectedUserCount: 0},
            configurations: [{...configuration, id: 'other', name: 'BETA'}],
        });

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));

        const moveUsersItem = await screen.findByRole('menuitem', {name: 'Move Users'});

        expect(moveUsersItem).toHaveAttribute('aria-disabled', 'true');

        await user.hover(moveUsersItem.parentElement as HTMLElement);

        expect((await screen.findAllByText('No connected users on this configuration')).length).toBeGreaterThan(0);
    });

    it('keeps delete confirmation disabled until the impact has loaded', async () => {
        const user = userEvent.setup();

        mocks.deleteImpact = undefined;

        renderItem();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Delete Configuration'}));

        const alertDialog = await screen.findByRole('alertdialog');

        expect(within(alertDialog).getByRole('button', {name: 'Delete'})).toBeDisabled();
    });

    it('keeps delete confirmation disabled and explains when the impact fails to load', async () => {
        const user = userEvent.setup();

        mocks.deleteImpact = undefined;
        mocks.deleteImpactError = true;

        renderItem();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Delete Configuration'}));

        const alertDialog = await screen.findByRole('alertdialog');

        expect(within(alertDialog).getByText('Could not load what this deletes.')).toBeInTheDocument();
        expect(within(alertDialog).getByRole('button', {name: 'Delete'})).toBeDisabled();
    });

    it('enables Move Users when another configuration shares the environment', async () => {
        const user = userEvent.setup();

        renderItem({
            configuration: {...configuration, connectedUserCount: 2},
            configurations: [configuration, {...configuration, id: 'other', name: 'BETA'}],
        });

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));

        expect(await screen.findByRole('menuitem', {name: 'Move Users'})).not.toHaveAttribute('aria-disabled', 'true');
    });

    it('shows the hidden from hub badge when the configuration is not visible in the hub', () => {
        renderItem({configuration: {...configuration, automationHubVisible: false}});

        expect(screen.getByText('Hidden from hub')).toBeInTheDocument();
    });

    it('does not show the hidden from hub badge when the configuration is visible in the hub', () => {
        renderItem();

        expect(screen.queryByText('Hidden from hub')).not.toBeInTheDocument();
    });

    it('deletes the configuration after confirming', async () => {
        const user = userEvent.setup();

        const onDelete = vi.fn();

        renderItem({onDelete});

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Delete Configuration'}));

        expect(onDelete).not.toHaveBeenCalled();

        const alertDialog = await screen.findByRole('alertdialog');

        await user.click(within(alertDialog).getByRole('button', {name: 'Delete'}));

        await waitFor(() => expect(onDelete).toHaveBeenCalledWith(configuration));
        await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    });

    it('shows when the configuration last executed', () => {
        const lastExecutionDate = new Date('2026-10-01T10:15:00Z');

        renderItem({configuration: {...configuration, lastExecutionDate: lastExecutionDate.toISOString()}});

        expect(
            screen.getByText(
                `Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`
            )
        ).toBeInTheDocument();
    });

    it('toggles a workflow of the configuration', async () => {
        const user = userEvent.setup();

        renderItem();

        mocks.enableWorkflowMutate.mockImplementation((_variables, options: {onSuccess: () => void}) =>
            options.onSuccess()
        );

        await user.click(await screen.findByRole('switch', {name: 'Enable Mailer'}));

        expect(mocks.enableWorkflowMutate.mock.calls[0][0]).toEqual({
            enable: false,
            id: 'configuration-1',
            workflowUuid: 'wf-1',
        });
        expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automationWorkflowProjectConfigurations']});
    });

    it('offers no configuration actions and disables the toggle without edit and delete scopes', () => {
        mocks.deniedScopes = new Set([
            'INTEGRATION_INSTANCE_CONFIGURATION_DELETE',
            'INTEGRATION_INSTANCE_CONFIGURATION_EDIT',
        ]);

        renderItem();

        expect(screen.getByRole('switch', {name: 'Enable Configuration'})).toBeDisabled();
        expect(screen.queryByRole('button', {name: 'Configuration Actions'})).not.toBeInTheDocument();
    });

    it('offers only Delete with the delete scope alone', async () => {
        mocks.deniedScopes = new Set(['INTEGRATION_INSTANCE_CONFIGURATION_EDIT']);

        renderItem();

        await userEvent.setup().click(screen.getByRole('button', {name: 'Configuration Actions'}));

        expect((await screen.findAllByRole('menuitem')).map((item) => item.textContent)).toEqual(['Delete']);
    });

    it('hides Move Users without the connected user view scope', async () => {
        mocks.deniedScopes = new Set(['CONNECTED_USER_VIEW']);

        renderItem();

        await userEvent.setup().click(screen.getByRole('button', {name: 'Configuration Actions'}));

        expect(await screen.findByRole('menuitem', {name: 'Edit Configuration'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Move Users'})).not.toBeInTheDocument();
    });
});
