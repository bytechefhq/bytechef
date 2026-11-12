import {TooltipProvider} from '@/components/ui/tooltip';
import useAutomationExecutionsSheetStore from '@/ee/pages/embedded/automations/stores/useAutomationExecutionsSheetStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserProjectWorkflowListItem, {
    ConnectedUserProjectWorkflowType,
    ConnectedUserWorkflowTemplateType,
} from '../ConnectedUserProjectWorkflowListItem';

const hoisted = vi.hoisted(() => ({
    copyToClipboard: vi.fn(),
}));

vi.mock('@uidotdev/usehooks', () => ({
    useCopyToClipboard: () => [null, hoisted.copyToClipboard],
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useDeleteConnectedUserProjectWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnableConnectedUserProjectWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/shared/components/workflow/WorkflowTriggerAndComponentsRow', () => ({
    default: ({
        filteredComponentNames,
        workflow,
    }: {
        filteredComponentNames?: string[];
        workflow: {triggers?: Array<{label?: string; type?: string}>; workflowTriggerComponentNames?: string[]};
    }) => (
        <div data-testid="trigger-and-components-row">
            {`${workflow.triggers?.[0]?.label ?? ''}|${(workflow.workflowTriggerComponentNames ?? []).join(',')}|${filteredComponentNames?.join(',')}`}
        </div>
    ),
}));

const STATIC_WEBHOOK_URL = 'https://bytechef.example.com/webhooks/abc123';

const createConnectedUserProjectWorkflow = (
    staticWebhookUrl: string | null,
    overrides: Partial<ConnectedUserProjectWorkflowType> = {}
): ConnectedUserProjectWorkflowType => ({
    automationUuid: null,
    connectedUserId: '1050',
    copiedFromWorkflowUuid: null,
    enabled: true,
    id: '1052',
    lastExecutionDate: null,
    projectId: '0',
    staticWebhookUrl,
    workflow: {
        id: 'be530417-2226-41d4-aae9-c4fc76f0791a',
        label: 'WhatsApp Agent - Human reply',
        triggers: [],
    },
    workflowUuid: 'be530417-2226-41d4-aae9-c4fc76f0791a',
    workflowVersion: 5,
    ...overrides,
});

const renderListItem = (
    connectedUserProjectWorkflow: ConnectedUserProjectWorkflowType,
    projectName?: string,
    workflowTemplate?: ConnectedUserWorkflowTemplateType,
    configurationProjectVersion?: number
) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <TooltipProvider>
                <ul>
                    <ConnectedUserProjectWorkflowListItem
                        configurationProjectVersion={configurationProjectVersion}
                        connectedUserProjectWorkflow={connectedUserProjectWorkflow}
                        projectName={projectName}
                        workflowTemplate={workflowTemplate}
                    />
                </ul>
            </TooltipProvider>
        </QueryClientProvider>
    );

describe('ConnectedUserProjectWorkflowListItem', () => {
    beforeEach(() => {
        hoisted.copyToClipboard.mockClear();

        useAutomationExecutionsSheetStore.setState({
            automationExecutionsSheetOpen: false,
            connectedUserId: undefined,
            projectName: undefined,
            workflow: undefined,
        });
    });

    it("opens the workflow's executions for the connected user when the workflow is clicked", async () => {
        renderListItem(createConnectedUserProjectWorkflow(null), 'Automation Playground');

        await userEvent.click(screen.getByRole('button', {name: 'WhatsApp Agent - Human reply'}));

        const state = useAutomationExecutionsSheetStore.getState();

        expect(state.automationExecutionsSheetOpen).toBe(true);
        expect(state.connectedUserId).toBe(1050);
        expect(state.projectName).toBe('Automation Playground');
        expect(state.workflow).toEqual({
            label: 'WhatsApp Agent - Human reply',
            workflowId: 'be530417-2226-41d4-aae9-c4fc76f0791a',
            workflowUuid: 'be530417-2226-41d4-aae9-c4fc76f0791a',
        });
    });

    it("opens the workflow's executions when the row is clicked outside the workflow name", async () => {
        renderListItem(createConnectedUserProjectWorkflow(null), 'Automation Playground');

        await userEvent.click(screen.getByRole('listitem').firstElementChild as HTMLElement);

        expect(useAutomationExecutionsSheetStore.getState().automationExecutionsSheetOpen).toBe(true);
    });

    it('shows the static webhook url and copies it', async () => {
        renderListItem(createConnectedUserProjectWorkflow(STATIC_WEBHOOK_URL));

        expect(screen.getByText(STATIC_WEBHOOK_URL)).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Copy webhook URL'}));

        expect(hoisted.copyToClipboard).toHaveBeenCalledWith(STATIC_WEBHOOK_URL);
    });

    it('shows no webhook url for a workflow without a static webhook trigger', () => {
        renderListItem(createConnectedUserProjectWorkflow(null));

        expect(screen.getByText('WhatsApp Agent - Human reply')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Copy webhook URL'})).not.toBeInTheDocument();
    });

    it('does not let an unpublished draft be enabled', () => {
        renderListItem(createConnectedUserProjectWorkflow(null, {enabled: false, workflowVersion: null}));

        expect(screen.getByText('V1 DRAFT')).toBeInTheDocument();
        expect(screen.getByRole('switch', {name: 'Enable WhatsApp Agent - Human reply'})).toBeDisabled();
    });

    it('lets a running workflow that is flagged as a draft be disabled', () => {
        renderListItem(createConnectedUserProjectWorkflow(null, {enabled: true, workflowVersion: null}));

        expect(screen.getByRole('switch', {name: 'Enable WhatsApp Agent - Human reply'})).toBeEnabled();
    });

    it('shows a provisioned workflow at the project version of its configuration and lets it be enabled', () => {
        renderListItem(
            createConnectedUserProjectWorkflow(null, {
                automationUuid: '3f6d2a40-0d6b-4f0e-9a51-6c1a7f1b2c3d',
                enabled: false,
                workflowVersion: null,
            }),
            undefined,
            undefined,
            3
        );

        expect(screen.queryByText('V1 DRAFT')).not.toBeInTheDocument();
        expect(screen.getByText('V3')).toBeInTheDocument();
        expect(screen.getByRole('switch', {name: 'Enable WhatsApp Agent - Human reply'})).toBeEnabled();
    });

    it('shows no version for a provisioned workflow whose configuration is unknown', () => {
        renderListItem(
            createConnectedUserProjectWorkflow(null, {
                automationUuid: '3f6d2a40-0d6b-4f0e-9a51-6c1a7f1b2c3d',
                enabled: false,
                workflowVersion: null,
            })
        );

        expect(screen.queryByText('V1 DRAFT')).not.toBeInTheDocument();
        expect(screen.getByRole('switch', {name: 'Enable WhatsApp Agent - Human reply'})).toBeEnabled();
    });

    it('shows the trigger and components of the source template workflow', () => {
        renderListItem(
            createConnectedUserProjectWorkflow(null, {
                workflow: {
                    id: 'workflow-1',
                    label: 'Greet on Request',
                    triggers: [{name: 'trigger_1', parameters: {}, type: 'webhook/v1/awaitWorkflowAndRespond'}],
                },
            }),
            'Automation Playground',
            {
                components: [{icon: 'slack.svg', name: 'slack', title: 'Slack'}],
                description: null,
                label: 'Greet on Request',
                lastModifiedDate: null,
                permissionExpression: null,
                triggers: [{icon: 'webhook.svg', name: 'trigger_1', title: 'Await Workflow and Respond'}],
                workflowId: 'template-workflow-1',
                workflowUuid: 'template-greeting',
            }
        );

        expect(screen.getByTestId('trigger-and-components-row')).toHaveTextContent(
            'Await Workflow and Respond|trigger_1|trigger_1,slack'
        );
    });

    it('shows no components for a workflow without a source template', () => {
        renderListItem(createConnectedUserProjectWorkflow(null));

        expect(screen.queryByTestId('trigger-and-components-row')).not.toBeInTheDocument();
    });
});
