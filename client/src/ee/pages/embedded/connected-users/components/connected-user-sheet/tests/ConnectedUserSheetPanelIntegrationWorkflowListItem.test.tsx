import ConnectedUserSheetPanelIntegrationWorkflowListItem from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationWorkflowListItem';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/ee/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import {
    IntegrationInstance,
    IntegrationInstanceConfiguration,
    IntegrationInstanceConfigurationWorkflow,
    Workflow,
} from '@/ee/shared/middleware/embedded/configuration';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/ee/shared/mutations/embedded/integrationInstanceWorkflows.mutations', () => ({
    useEnableIntegrationInstanceWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

const integrationInstance = {
    connectedUserId: 7,
    id: 3,
    integrationInstanceWorkflows: [],
} as unknown as IntegrationInstance;

const integrationInstanceConfiguration = {
    id: 11,
    integrationVersion: 5,
    name: 'Gmail Code',
} as unknown as IntegrationInstanceConfiguration;

const integrationInstanceConfigurationWorkflow = {
    enabled: true,
    workflowId: 'workflow-1',
} as unknown as IntegrationInstanceConfigurationWorkflow;

const workflow = {
    id: 'workflow-1',
    label: 'Notify Slack on New Email',
} as unknown as Workflow;

describe('ConnectedUserSheetPanelIntegrationWorkflowListItem', () => {
    beforeEach(() => {
        windowResizeObserver();

        useIntegrationInstanceConfigurationWorkflowSheetStore.setState({
            connectedUserId: undefined,
            integrationInstanceConfigurationId: undefined,
            integrationInstanceConfigurationWorkflowSheetOpen: false,
            workflow: undefined,
        });
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it("should open the workflow's executions for the connected user when the workflow is clicked", async () => {
        render(
            <ul>
                <ConnectedUserSheetPanelIntegrationWorkflowListItem
                    componentDefinitions={[]}
                    integrationInstance={integrationInstance}
                    integrationInstanceConfiguration={integrationInstanceConfiguration}
                    integrationInstanceConfigurationWorkflow={integrationInstanceConfigurationWorkflow}
                    workflow={workflow}
                />
            </ul>
        );

        await userEvent.click(screen.getByRole('button', {name: 'Notify Slack on New Email'}));

        const state = useIntegrationInstanceConfigurationWorkflowSheetStore.getState();

        expect(state.integrationInstanceConfigurationWorkflowSheetOpen).toBe(true);
        expect(state.connectedUserId).toBe(7);
        expect(state.integrationInstanceConfigurationId).toBe(11);
        expect(state.integrationInstanceConfigurationName).toBe('Gmail Code');
        expect(state.integrationVersion).toBe(5);
        expect(state.workflow).toBe(workflow);
    });
});
