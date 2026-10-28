import {TooltipProvider} from '@/components/ui/tooltip';
import useAutomationWorkflowExecutionsSheetStore from '@/ee/pages/embedded/automation-workflows/stores/useAutomationWorkflowExecutionsSheetStore';
import {resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {render} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectConfigurationListItem from '../AutomationWorkflowProjectConfigurationListItem';

const mocks = vi.hoisted(() => ({
    enableConfigurationMutate: vi.fn(),
    enableWorkflowMutate: vi.fn(),
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

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: vi.fn()}),
}));

const environments = [
    {id: 1, name: 'Development'},
    {id: 2, name: 'Staging'},
];

const configuration = {
    automationHubVisible: true,
    enabled: false,
    environmentId: '2',
    id: 'configuration-1',
    lastModifiedDate: null,
    projectId: 'project-1',
    projectVersion: 2,
    workflows: [{enabled: true, label: 'Mailer', workflowId: 'pinned-wf-1', workflowUuid: 'wf-1'}],
};

const project = {
    categoryId: null,
    description: null,
    id: 'project-1',
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
            lastModifiedDate: null,
            permissionExpression: null,
            triggers: [],
            workflowId: 'draft-wf-1',
            workflowUuid: 'wf-1',
        },
    ],
};

const renderItem = (overrides: {configuration?: typeof configuration; project?: typeof project} = {}) =>
    render(
        <TooltipProvider>
            <AutomationWorkflowProjectConfigurationListItem
                configuration={overrides.configuration ?? configuration}
                environments={environments}
                onDelete={vi.fn()}
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

        await user.click(screen.getByText('1 workflow'));
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

        await user.click(screen.getByText('1 workflow'));
        await user.click(await screen.findByRole('button', {name: 'Mailer'}));

        const state = useAutomationWorkflowExecutionsSheetStore.getState();

        expect(state.automationWorkflowExecutionsSheetOpen).toBe(true);
        expect(state.workflow).toEqual({label: 'Mailer', workflowId: 'pinned-wf-1', workflowUuid: 'wf-1'});
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

        await user.click(screen.getByRole('switch', {name: 'Enable Configuration'}));

        expect(mocks.enableConfigurationMutate.mock.calls[0][0]).toEqual({enable: true, id: 'configuration-1'});
    });

    it('shows the hidden from hub badge when the configuration is not visible in the hub', () => {
        renderItem({configuration: {...configuration, automationHubVisible: false}});

        expect(screen.getByText('Hidden from hub')).toBeInTheDocument();
    });
});
