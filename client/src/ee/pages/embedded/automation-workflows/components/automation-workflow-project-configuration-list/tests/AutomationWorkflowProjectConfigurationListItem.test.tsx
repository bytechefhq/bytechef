import {TooltipProvider} from '@/components/ui/tooltip';
import useAutomationWorkflowExecutionsSheetStore from '@/ee/pages/embedded/automation-workflows/stores/useAutomationWorkflowExecutionsSheetStore';
import {AutomationWorkflowProjectConfigurationsQuery} from '@/shared/middleware/graphql';
import {resetAll, screen, userEvent, waitFor, windowResizeObserver, within} from '@/shared/util/test-utils';
import {render} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectConfigurationListItem from '../AutomationWorkflowProjectConfigurationListItem';

const mocks = vi.hoisted(() => ({
    enableConfigurationMutate: vi.fn(),
    enableWorkflowMutate: vi.fn(),
    invalidateQueries: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
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

const environments = [
    {id: 1, name: 'Development'},
    {id: 2, name: 'Staging'},
];

type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];

const configuration: AutomationWorkflowProjectConfigurationType = {
    automationHubVisible: true,
    enabled: false,
    environmentId: '2',
    id: 'configuration-1',
    lastExecutionDate: null,
    lastModifiedDate: null,
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
    overrides: {configuration?: typeof configuration; onDelete?: () => void; project?: typeof project} = {}
) =>
    render(
        <TooltipProvider>
            <AutomationWorkflowProjectConfigurationListItem
                configuration={overrides.configuration ?? configuration}
                environments={environments}
                onDelete={overrides.onDelete ?? vi.fn()}
                onEdit={vi.fn()}
                project={overrides.project ?? project}
            />
        </TooltipProvider>
    );

describe('AutomationWorkflowProjectConfigurationListItem', () => {
    beforeEach(() => {
        windowResizeObserver();

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

        await user.click(screen.getByText('Staging'));

        expect(screen.queryByRole('button', {name: 'Mailer'})).not.toBeInTheDocument();

        await user.click(screen.getByText('Staging'));

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

    it('explains that deleting a configuration pauses automations until it is recreated', async () => {
        const user = userEvent.setup();

        renderItem();

        await user.click(screen.getByRole('button', {name: 'Configuration Actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Delete Configuration'}));

        expect(
            await screen.findByText(
                "Connected users' automations from this environment are paused and come back if you recreate the configuration.",
                {exact: false}
            )
        ).toBeInTheDocument();
        expect(screen.queryByText(/cannot be undone/)).not.toBeInTheDocument();
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
});
