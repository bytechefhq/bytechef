import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {WorkflowExecutions} from '../WorkflowExecutions';

const {useWorkflowExecutionsMock} = vi.hoisted(() => ({
    useWorkflowExecutionsMock: vi.fn(),
}));

vi.mock('@/ee/pages/embedded/workflow-executions/hooks/useWorkflowExecutions', () => ({
    useWorkflowExecutions: useWorkflowExecutionsMock,
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    useGetIntegrationInstanceConfigurationQuery: () => ({data: undefined}),
    useGetIntegrationInstanceConfigurationsQuery: () => ({data: undefined}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    useGetIntegrationVersionWorkflowsQuery: () => ({data: undefined}),
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    useGetIntegrationsQuery: () => ({data: undefined}),
}));

vi.mock('@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet', () => ({
    default: () => null,
}));

vi.mock('@/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet', () => ({
    default: () => null,
}));

vi.mock('@/shared/hooks/useOnEnvironmentChange', () => ({
    useOnEnvironmentChange: vi.fn(),
}));

const connectedUserProjects = [
    {
        connectedUser: {environmentId: '0', externalId: 'user-1', id: '1'},
        connectedUserProjectWorkflows: [
            {workflow: {id: 'copy-workflow', label: 'Copied workflow'}, workflowUuid: 'copy-uuid'},
            {workflow: {id: 'reference-uuid', label: 'Referenced workflow'}, workflowUuid: 'reference-uuid'},
        ],
        environmentId: '0',
        id: '10',
        projectId: '100',
    },
    {
        connectedUser: {environmentId: '0', externalId: 'user-2', id: '2'},
        connectedUserProjectWorkflows: [
            {workflow: {id: 'reference-uuid', label: 'Referenced workflow'}, workflowUuid: 'reference-uuid'},
        ],
        environmentId: '0',
        id: '20',
        projectId: '200',
    },
];

const renderWorkflowExecutions = (automations: number) =>
    render(
        <TooltipProvider>
            <MemoryRouter initialEntries={[`/embedded/executions?automations=${automations}`]}>
                <WorkflowExecutions />
            </MemoryRouter>
        </TooltipProvider>
    );

const getWorkflowComboBox = () => {
    const workflowLabel = screen.getByText('Workflow', {selector: 'label'});

    return within(workflowLabel.parentElement!).getByRole('combobox');
};

describe('WorkflowExecutions', () => {
    beforeEach(() => {
        useWorkflowExecutionsMock.mockReset();

        useWorkflowExecutionsMock.mockReturnValue({
            connectedUserProjects,
            refetchWorkflowExecutions: vi.fn(),
            workflowExecutionPage: undefined,
            workflowExecutionsError: null,
            workflowExecutionsIsFetching: false,
            workflowExecutionsIsLoading: false,
        });
    });

    it('lists the connected users workflows once each in the automations workflow filter', async () => {
        renderWorkflowExecutions(1);

        await userEvent.click(getWorkflowComboBox());

        expect(await screen.findByRole('option', {name: 'Copied workflow'})).toBeInTheDocument();
        expect(screen.getAllByRole('option', {name: 'Referenced workflow'})).toHaveLength(1);
    });

    it('sends the picked workflow to the automation executions query', async () => {
        renderWorkflowExecutions(1);

        await userEvent.click(getWorkflowComboBox());

        await userEvent.click(await screen.findByRole('option', {name: 'Referenced workflow'}));

        await waitFor(() => {
            expect(useWorkflowExecutionsMock).toHaveBeenLastCalledWith(
                1,
                expect.objectContaining({workflowId: 'reference-uuid'})
            );
        });
    });

    it('does not offer the automation workflows for integrations', async () => {
        renderWorkflowExecutions(0);

        await userEvent.click(getWorkflowComboBox());

        expect(screen.queryByRole('option', {name: 'Copied workflow'})).not.toBeInTheDocument();
    });
});
