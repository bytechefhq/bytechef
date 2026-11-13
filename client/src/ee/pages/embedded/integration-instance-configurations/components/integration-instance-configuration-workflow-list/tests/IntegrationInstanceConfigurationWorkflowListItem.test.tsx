import {TooltipProvider} from '@/components/ui/tooltip';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import {IntegrationInstanceConfigurationWorkflow, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationWorkflowListItem from '../IntegrationInstanceConfigurationWorkflowListItem';

const {rowPropsMock} = vi.hoisted(() => ({rowPropsMock: vi.fn()}));

vi.mock('@/shared/components/workflow/WorkflowTriggerAndComponentsRow', () => ({
    default: (props: object) => {
        rowPropsMock(props);

        return <div>Trigger and components</div>;
    },
}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations', () => ({
    useEnableIntegrationInstanceConfigurationWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/ee/shared/queries/embedded/workflows.queries', () => ({
    useGetWorkflowQuery: () => ({data: undefined}),
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-workflow-list/IntegrationInstanceConfigurationWorkflowListItemDropDownMenu',
    () => ({default: () => <div>Workflow menu</div>})
);

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationEditWorkflowDialog',
    () => ({default: () => null})
);

vi.mock('@/ee/pages/embedded/shared/OpenIntegrationWorkflowButton', () => ({
    default: ({integrationId, workflowUuid}: {integrationId: number; workflowUuid?: string}) => (
        <button type="button">{`Open ${workflowUuid} in integration ${integrationId}`}</button>
    ),
}));

const workflow = {
    id: 'workflow1',
    label: 'Sync contacts',
    triggers: [{type: 'hubspot/v1/newContact'}],
    workflowTriggerComponentNames: ['hubspot'],
    workflowUuid: 'workflow-uuid-1',
} as unknown as Workflow;

const workflowComponentDefinitions = {hubspot: {icon: 'hubspot.svg', name: 'hubspot', title: 'HubSpot'}};

const renderItem = (showOpenInIntegration?: boolean, isMcpWorkflow = false) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <TooltipProvider>
                <IntegrationInstanceConfigurationWorkflowListItem
                    componentName="hubspot"
                    filteredComponentNames={['hubspot', 'logger']}
                    integrationId={1}
                    integrationInstanceConfigurationId={1}
                    integrationInstanceConfigurationName="HubSpot Dev"
                    integrationInstanceConfigurationWorkflow={
                        {enabled: true, workflowId: 'workflow1'} as IntegrationInstanceConfigurationWorkflow
                    }
                    integrationVersion={3}
                    isMcpWorkflow={isMcpWorkflow}
                    showOpenInIntegration={showOpenInIntegration}
                    workflow={workflow}
                    workflowComponentDefinitions={workflowComponentDefinitions as never}
                    workflowTaskDispatcherDefinitions={{}}
                />
            </TooltipProvider>
        </QueryClientProvider>
    );

describe('IntegrationInstanceConfigurationWorkflowListItem', () => {
    beforeEach(() => {
        rowPropsMock.mockClear();

        useIntegrationInstanceConfigurationWorkflowSheetStore.setState({
            integrationInstanceConfigurationWorkflowSheetOpen: false,
        });
    });

    it('does not offer opening the workflow in its integration by default', () => {
        renderItem();

        expect(screen.queryByText(/in integration/)).not.toBeInTheDocument();
    });

    it('offers opening the workflow in its integration when asked to', () => {
        renderItem(true);

        expect(screen.getByText('Open workflow-uuid-1 in integration 1')).toBeInTheDocument();
    });

    it('shows the trigger and components like the projects page', () => {
        renderItem();

        expect(screen.getByText('Trigger and components')).toBeInTheDocument();
        expect(rowPropsMock).toHaveBeenLastCalledWith(
            expect.objectContaining({
                className: 'hidden sm:flex',
                filteredComponentNames: ['hubspot', 'logger'],
                workflow,
                workflowComponentDefinitions,
                workflowTaskDispatcherDefinitions: {},
            })
        );
    });

    it('marks an MCP-exposed workflow without locking it', async () => {
        const {container} = renderItem(false, true);

        expect(container.querySelector('li')).not.toHaveClass('opacity-50');
        expect(screen.getByRole('switch')).toBeEnabled();
        expect(screen.getByText('Workflow menu')).toBeInTheDocument();

        await userEvent.hover(screen.getByText('MCP'));

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Exposed as a tool on an MCP server');
    });

    it("opens the workflow's executions sheet when the workflow is clicked", () => {
        renderItem();

        fireEvent.click(screen.getByText('Sync contacts'));

        expect(useIntegrationInstanceConfigurationWorkflowSheetStore.getState()).toEqual(
            expect.objectContaining({
                integrationInstanceConfigurationId: 1,
                integrationInstanceConfigurationName: 'HubSpot Dev',
                integrationInstanceConfigurationWorkflowSheetOpen: true,
                integrationVersion: 3,
                workflow,
            })
        );
    });
});
