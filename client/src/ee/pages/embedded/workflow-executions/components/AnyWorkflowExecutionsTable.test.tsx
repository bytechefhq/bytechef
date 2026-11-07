import {TooltipProvider} from '@/components/ui/tooltip';
import useEmbeddedWorkflowExecutionSheetStore from '@/ee/pages/embedded/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {AnyWorkflowExecutionRowI} from '@/ee/pages/embedded/workflow-executions/utils/anyWorkflowExecutions';
import useAutomationWorkflowExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AnyWorkflowExecutionsTable from './AnyWorkflowExecutionsTable';

vi.mock('./EmbeddedWorkflowExecutionsDropdownMenu', () => ({
    default: () => <button>execution menu</button>,
}));

vi.mock('@/ee/shared/queries/embedded/connectedUsers.queries', () => ({
    useGetConnectedUserQuery: (id: number, enabled?: boolean) => ({
        data: enabled ? {externalId: `external-${id}`, id} : undefined,
    }),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: (_request: object, enabled?: boolean) => ({
        data: enabled ? [{name: 'googleMail', title: 'Gmail'}] : undefined,
    }),
}));

const automationJobRow = {
    automationWorkflowExecution: {
        connectedUserId: 5,
        id: 3,
        job: {id: '3', startDate: new Date('2026-10-20T10:00:00Z'), status: 'COMPLETED'},
        project: {name: 'Sales Sync'},
        projectDeployment: {name: '__EMBEDDED__user-5__DEVELOPMENT'},
        workflow: {label: 'Sync contacts'},
    },
    job: {id: '3', startDate: new Date('2026-10-20T10:00:00Z'), status: 'COMPLETED'},
    workflow: {label: 'Sync contacts'},
} as unknown as AnyWorkflowExecutionRowI;

const automationTriggerRow = {
    automationWorkflowExecution: {
        id: 4,
        project: {name: 'Sales Sync'},
        projectDeployment: {name: '__EMBEDDED__user-5__DEVELOPMENT'},
        workflow: {label: 'Failed trigger'},
    },
    workflow: {label: 'Failed trigger'},
} as unknown as AnyWorkflowExecutionRowI;

const integrationRow = {
    integrationWorkflowExecution: {
        id: 1,
        integration: {componentName: 'googleMail', name: 'Gmail Sync'},
        integrationInstance: {connectedUserId: 9},
        integrationInstanceConfiguration: {integrationVersion: 5},
        job: {id: '1', startDate: new Date('2026-10-20T09:00:00Z'), status: 'FAILED'},
        workflow: {label: 'Notify Slack on New Email'},
    },
    job: {id: '1', startDate: new Date('2026-10-20T09:00:00Z'), status: 'FAILED'},
    workflow: {label: 'Notify Slack on New Email'},
} as unknown as AnyWorkflowExecutionRowI;

const renderTable = (data: AnyWorkflowExecutionRowI[]) =>
    render(
        <TooltipProvider>
            <AnyWorkflowExecutionsTable data={data} />
        </TooltipProvider>
    );

describe('AnyWorkflowExecutionsTable', () => {
    beforeEach(() => {
        useAutomationWorkflowExecutionSheetStore.setState({
            workflowExecutionId: 0,
            workflowExecutionKind: 'JOB',
            workflowExecutionSheetOpen: false,
        });
        useEmbeddedWorkflowExecutionSheetStore.setState({workflowExecutionId: 0, workflowExecutionSheetOpen: false});
    });

    it('shows where each run came from, its connected user and its version', () => {
        renderTable([automationJobRow, integrationRow]);

        expect(screen.getByRole('img', {name: 'Automation'})).toBeInTheDocument();
        expect(screen.getByText('Sales Sync')).toBeInTheDocument();
        expect(screen.getByText('external-5')).toBeInTheDocument();
        expect(screen.queryByText('__EMBEDDED__user-5__DEVELOPMENT')).not.toBeInTheDocument();

        expect(screen.getByRole('img', {name: 'Integration'})).toBeInTheDocument();
        expect(screen.getByText('Gmail')).toBeInTheDocument();
        expect(screen.getByText('external-9')).toBeInTheDocument();
        expect(screen.getByText('V5')).toBeInTheDocument();
    });

    it('shows the source of each run after the workflow as an icon followed by the project or integration name', () => {
        renderTable([automationJobRow, integrationRow]);

        expect(screen.getAllByRole('columnheader').map((header) => header.textContent)).toEqual([
            'Status',
            'Workflow',
            'Source',
            'Connected user',
            'Version',
            'Duration',
            'Execution date',
            'Actions',
        ]);

        const automationCells = screen.getAllByRole('row')[1].querySelectorAll('td');
        const integrationCells = screen.getAllByRole('row')[2].querySelectorAll('td');

        expect(automationCells[2]).toContainElement(screen.getByRole('img', {name: 'Automation'}));
        expect(automationCells[2]).toHaveTextContent(/^Sales Sync$/);
        expect(integrationCells[2]).toContainElement(screen.getByRole('img', {name: 'Integration'}));
        expect(integrationCells[2]).toHaveTextContent(/^Gmail$/);
    });

    it('opens the automation execution sheet for an automation run', async () => {
        useEmbeddedWorkflowExecutionSheetStore.setState({workflowExecutionSheetOpen: true});

        renderTable([automationJobRow, integrationRow]);

        await userEvent.click(screen.getByText('Sync contacts'));

        expect(useAutomationWorkflowExecutionSheetStore.getState()).toEqual(
            expect.objectContaining({
                workflowExecutionId: 3,
                workflowExecutionKind: 'JOB',
                workflowExecutionSheetOpen: true,
            })
        );
        expect(useEmbeddedWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(false);
    });

    it('opens a failed trigger of an automation as a trigger execution', async () => {
        renderTable([automationTriggerRow]);

        await userEvent.click(screen.getByText('Failed trigger'));

        expect(useAutomationWorkflowExecutionSheetStore.getState()).toEqual(
            expect.objectContaining({
                workflowExecutionId: 4,
                workflowExecutionKind: 'TRIGGER_EXECUTION',
                workflowExecutionSheetOpen: true,
            })
        );
    });

    it('opens the integration execution sheet for an integration run', async () => {
        useAutomationWorkflowExecutionSheetStore.setState({workflowExecutionSheetOpen: true});

        renderTable([automationJobRow, integrationRow]);

        await userEvent.click(screen.getByText('Notify Slack on New Email'));

        expect(useEmbeddedWorkflowExecutionSheetStore.getState()).toEqual(
            expect.objectContaining({workflowExecutionId: 1, workflowExecutionSheetOpen: true})
        );
        expect(useAutomationWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(false);
    });
});
