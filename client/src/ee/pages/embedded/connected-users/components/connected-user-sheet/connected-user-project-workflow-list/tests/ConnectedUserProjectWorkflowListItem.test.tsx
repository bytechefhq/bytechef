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

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowEditDialog',
    () => ({
        default: ({
            configurationId,
            connectedUserProjectWorkflowId,
            projectId,
            workflowUuid,
        }: {
            configurationId: string;
            connectedUserProjectWorkflowId: string;
            projectId: string;
            workflowUuid: string;
        }) => (
            <div data-testid="edit-dialog">
                {`${connectedUserProjectWorkflowId}|${configurationId}|${projectId}|${workflowUuid}`}
            </div>
        ),
    })
);

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
    attentionReason: null,
    automationUuid: null,
    connectedUserId: '1050',
    copiedFromWorkflowUuid: null,
    enabled: true,
    id: '1052',
    lastExecutionDate: null,
    projectId: '0',
    provisionedBy: null,
    staticWebhookUrl,
    toolOnly: false,
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
    configuration?: {configurationEnabled?: boolean; configurationId: string; enabled?: boolean; projectId: string}
) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <TooltipProvider>
                <ul>
                    <ConnectedUserProjectWorkflowListItem
                        configuration={configuration}
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

    it('opens the Edit dialog for a reference under a configuration', async () => {
        renderListItem(
            createConnectedUserProjectWorkflow(null, {automationUuid: 'template-uuid'}),
            'Automation Playground',
            undefined,
            {configurationId: '60', projectId: '20'}
        );

        await userEvent.click(screen.getByRole('button', {name: 'More actions'}));
        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

        expect(screen.getByTestId('edit-dialog')).toHaveTextContent('1052|60|20|template-uuid');
    });

    it('offers no Edit for a copy', async () => {
        renderListItem(createConnectedUserProjectWorkflow(null), undefined, undefined, {
            configurationId: '60',
            projectId: '20',
        });

        await userEvent.click(screen.getByRole('button', {name: 'More actions'}));

        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Edit'})).not.toBeInTheDocument();
    });

    it('offers no Edit for a reference without a configuration', async () => {
        renderListItem(createConnectedUserProjectWorkflow(null, {automationUuid: 'template-uuid'}));

        await userEvent.click(screen.getByRole('button', {name: 'More actions'}));

        expect(screen.queryByRole('menuitem', {name: 'Edit'})).not.toBeInTheDocument();
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

    it('copies the static webhook url', async () => {
        renderListItem(createConnectedUserProjectWorkflow(STATIC_WEBHOOK_URL));

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

    it('marks a reference an admin provisioned as provisioned', () => {
        renderListItem(
            createConnectedUserProjectWorkflow(null, {
                automationUuid: 'template-uuid',
                provisionedBy: 'admin@localhost.com',
                workflowVersion: null,
            })
        );

        expect(screen.getByText('Provisioned')).toBeInTheDocument();
        expect(screen.queryByText('Self-created')).not.toBeInTheDocument();
    });

    it('marks a reference the connected user created through the API as self-created', () => {
        renderListItem(
            createConnectedUserProjectWorkflow(null, {automationUuid: 'template-uuid', workflowVersion: null})
        );

        expect(screen.getByText('Self-created')).toBeInTheDocument();
        expect(screen.queryByText('Provisioned')).not.toBeInTheDocument();
    });

    it('shows no version for a provisioned workflow, whose configuration already shows the project version', () => {
        renderListItem(
            createConnectedUserProjectWorkflow(null, {
                automationUuid: '3f6d2a40-0d6b-4f0e-9a51-6c1a7f1b2c3d',
                enabled: false,
                workflowVersion: null,
            })
        );

        expect(screen.queryByText(/^V\d/)).not.toBeInTheDocument();
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

    it('shows the template trigger of a provisioned workflow, which carries no triggers of its own', () => {
        renderListItem(
            createConnectedUserProjectWorkflow(null, {
                automationUuid: '3f6d2a40-0d6b-4f0e-9a51-6c1a7f1b2c3d',
                workflow: {id: 'workflow-1', label: 'Read Airtable record', triggers: []},
                workflowVersion: null,
            }),
            undefined,
            {
                components: [{icon: 'airtable.svg', name: 'airtable', title: 'Airtable'}],
                description: null,
                label: 'Read Airtable record',
                lastModifiedDate: null,
                permissionExpression: null,
                triggers: [{icon: 'manual.svg', name: 'manual', title: 'Manual'}],
                workflowId: 'template-workflow-1',
                workflowUuid: '3f6d2a40-0d6b-4f0e-9a51-6c1a7f1b2c3d',
            }
        );

        expect(screen.getByTestId('trigger-and-components-row')).toHaveTextContent('Manual|manual|manual,airtable');
    });

    it('explains that a turned on reference waits for the configuration to set its internal-only inputs', async () => {
        renderListItem(
            createConnectedUserProjectWorkflow(null, {
                attentionReason: 'AWAITING_CONFIGURATION',
                automationUuid: 'template-uuid',
            }),
            'Automation Playground',
            undefined,
            {configurationEnabled: true, configurationId: '60', projectId: '20'}
        );

        const enableSwitch = screen.getByRole('switch', {name: 'Enable WhatsApp Agent - Human reply'});

        expect(enableSwitch).toBeEnabled();

        await userEvent.hover(enableSwitch.parentElement as HTMLElement);

        expect(
            await screen.findByRole('tooltip', {
                name: 'Waiting for the configuration to set its required internal-only inputs. This workflow runs once they are set.',
            })
        ).toBeInTheDocument();
    });

    it('shows a reference of a disabled configuration as switched off and locked', async () => {
        renderListItem(
            createConnectedUserProjectWorkflow(null, {
                attentionReason: 'TURNED_OFF_BY_ADMINISTRATOR',
                automationUuid: 'template-uuid',
            }),
            'Automation Playground',
            undefined,
            {configurationEnabled: false, configurationId: '60', projectId: '20'}
        );

        const enableSwitch = screen.getByRole('switch', {name: 'Enable WhatsApp Agent - Human reply'});

        expect(enableSwitch).not.toBeChecked();
        expect(enableSwitch).toBeDisabled();

        await userEvent.hover(enableSwitch.parentElement as HTMLElement);

        expect(
            await screen.findByRole('tooltip', {
                name: 'The configuration is disabled. This workflow runs again when the configuration is enabled.',
            })
        ).toBeInTheDocument();
    });

    it('shows a reference of a configuration turned off for this user as switched off and locked', async () => {
        renderListItem(
            createConnectedUserProjectWorkflow(null, {
                attentionReason: 'TURNED_OFF_BY_ADMINISTRATOR',
                automationUuid: 'template-uuid',
            }),
            'Automation Playground',
            undefined,
            {configurationEnabled: true, configurationId: '60', enabled: false, projectId: '20'}
        );

        const enableSwitch = screen.getByRole('switch', {name: 'Enable WhatsApp Agent - Human reply'});

        expect(enableSwitch).not.toBeChecked();
        expect(enableSwitch).toBeDisabled();

        await userEvent.hover(enableSwitch.parentElement as HTMLElement);

        expect(
            await screen.findByRole('tooltip', {
                name: 'The configuration is turned off for this user. This workflow runs again when it is turned on.',
            })
        ).toBeInTheDocument();
    });

    it('shows a reference whose workflow is disabled in an enabled configuration as switched off and locked', async () => {
        renderListItem(
            createConnectedUserProjectWorkflow(null, {
                attentionReason: 'TURNED_OFF_BY_ADMINISTRATOR',
                automationUuid: 'template-uuid',
            }),
            'Automation Playground',
            undefined,
            {configurationEnabled: true, configurationId: '60', projectId: '20'}
        );

        const enableSwitch = screen.getByRole('switch', {name: 'Enable WhatsApp Agent - Human reply'});

        expect(enableSwitch).not.toBeChecked();
        expect(enableSwitch).toBeDisabled();

        await userEvent.hover(enableSwitch.parentElement as HTMLElement);

        expect(
            await screen.findByRole('tooltip', {
                name: 'This workflow is disabled in the configuration. It runs again when it is enabled there.',
            })
        ).toBeInTheDocument();
    });

    it('keeps the switch on and usable for a reference that needs attention for another reason', () => {
        renderListItem(
            createConnectedUserProjectWorkflow(null, {
                attentionReason: 'MISSING_CONNECTION:slack',
                automationUuid: 'template-uuid',
            })
        );

        const enableSwitch = screen.getByRole('switch', {name: 'Enable WhatsApp Agent - Human reply'});

        expect(enableSwitch).toBeChecked();
        expect(enableSwitch).toBeEnabled();
    });

    it('shows no components for a workflow without a source template', () => {
        renderListItem(createConnectedUserProjectWorkflow(null));

        expect(screen.queryByTestId('trigger-and-components-row')).not.toBeInTheDocument();
    });

    it('shows no own switch and a Tool only hint for a tool-only automation', () => {
        renderListItem(createConnectedUserProjectWorkflow(null, {toolOnly: true}));

        expect(screen.queryByRole('switch')).not.toBeInTheDocument();
        expect(screen.getByText('Tool only')).toBeInTheDocument();
        expect(screen.getByText('WhatsApp Agent - Human reply')).toBeInTheDocument();
    });

    it('keeps the own switch and no hint for a regular automation', () => {
        renderListItem(createConnectedUserProjectWorkflow(null));

        expect(screen.getByRole('switch')).toBeInTheDocument();
        expect(screen.queryByText('Tool only')).not.toBeInTheDocument();
    });

    it('copies the referenced automation uuid for a reference, which the request trigger runs it by', async () => {
        renderListItem(createConnectedUserProjectWorkflow(null, {automationUuid: 'template-uuid'}));

        await userEvent.click(screen.getByRole('button', {name: 'More actions'}));
        await userEvent.click(screen.getByRole('menuitem', {name: 'Copy Workflow UUID'}));

        expect(hoisted.copyToClipboard).toHaveBeenCalledWith('template-uuid');
    });

    it('copies the own workflow uuid for a copy', async () => {
        renderListItem(createConnectedUserProjectWorkflow(null));

        await userEvent.click(screen.getByRole('button', {name: 'More actions'}));
        await userEvent.click(screen.getByRole('menuitem', {name: 'Copy Workflow UUID'}));

        expect(hoisted.copyToClipboard).toHaveBeenCalledWith('be530417-2226-41d4-aae9-c4fc76f0791a');
    });
});
