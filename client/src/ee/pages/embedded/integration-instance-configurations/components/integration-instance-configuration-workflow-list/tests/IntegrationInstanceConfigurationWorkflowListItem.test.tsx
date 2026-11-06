import {TooltipProvider} from '@/components/ui/tooltip';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import {IntegrationInstanceConfigurationWorkflow, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
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
    () => ({default: () => null})
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

const renderItem = (showOpenInIntegration?: boolean) =>
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
                    isMcpWorkflow={false}
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

    it('marks the workflow label with the workflow icon', () => {
        const {container} = renderItem();

        expect(container.querySelector('svg.lucide-workflow')).toBeInTheDocument();
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
