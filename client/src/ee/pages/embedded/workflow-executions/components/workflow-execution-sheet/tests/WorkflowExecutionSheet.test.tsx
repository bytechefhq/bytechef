import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowExecutionSheetStore from '../../../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionSheet from '../WorkflowExecutionSheet';

const {flagState, queryState} = vi.hoisted(() => ({
    flagState: {copilotEnabled: false, ff4077: false},
    queryState: {workflowExecution: undefined as unknown},
}));

vi.mock('@/ee/shared/queries/embedded/workflowExecutions.queries', () => ({
    useGetIntegrationWorkflowExecutionQuery: () => ({data: queryState.workflowExecution, isLoading: false}),
}));

vi.mock('@/shared/components/copilot/CopilotPanel', () => ({default: () => null}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => flagState.ff4077,
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) =>
        selector({ai: {copilot: {enabled: flagState.copilotEnabled}}}),
}));

vi.mock('../WorkflowExecutionDetail', () => ({
    default: ({workflowExecutionId}: {workflowExecutionId: number}) => (
        <div>{`Execution details ${workflowExecutionId}`}</div>
    ),
}));

const renderSheet = (workflowExecution: object) => {
    queryState.workflowExecution = workflowExecution;

    return render(
        <TooltipProvider>
            <WorkflowExecutionSheet />
        </TooltipProvider>
    );
};

describe('embedded WorkflowExecutionSheet', () => {
    beforeEach(() => {
        flagState.copilotEnabled = false;
        flagState.ff4077 = false;
        queryState.workflowExecution = undefined;

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 1, workflowExecutionSheetOpen: true});
    });

    it('shows the integration version after the workflow in the header', () => {
        renderSheet({
            id: 1,
            integration: {name: 'HubSpot'},
            integrationInstanceConfiguration: {integrationVersion: 2},
            job: {id: '1', status: 'COMPLETED'},
            workflow: {label: 'Sync contacts'},
        });

        expect(document.querySelector('header')).toHaveTextContent('HubSpot /Sync contacts/ V2');
        expect(document.body).toHaveTextContent('Execution details 1');
    });

    it('shows no version when the instance configuration has none', () => {
        renderSheet({
            id: 1,
            integration: {name: 'HubSpot'},
            integrationInstanceConfiguration: {},
            job: {id: '1', status: 'COMPLETED'},
            workflow: {label: 'Sync contacts'},
        });

        expect(document.querySelector('header')).toHaveTextContent('HubSpot /Sync contacts');
        expect(document.querySelector('header')).not.toHaveTextContent('/ V');
    });

    it('shows the Copilot button when Copilot and its feature flag are enabled', () => {
        flagState.copilotEnabled = true;
        flagState.ff4077 = true;

        renderSheet({id: 1, integration: {name: 'HubSpot'}, workflow: {label: 'Sync contacts'}});

        expect(screen.getByLabelText('Open Copilot panel')).toBeInTheDocument();
    });

    it('hides the Copilot button while the feature flag is off', () => {
        flagState.copilotEnabled = true;

        renderSheet({id: 1, integration: {name: 'HubSpot'}, workflow: {label: 'Sync contacts'}});

        expect(screen.queryByLabelText('Open Copilot panel')).not.toBeInTheDocument();
    });
});
