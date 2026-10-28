import {render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowExecutionSheetStore from '../../../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionSheet from '../WorkflowExecutionSheet';

const {queryState} = vi.hoisted(() => ({
    queryState: {workflowExecution: undefined as unknown},
}));

vi.mock('@/ee/shared/queries/embedded/workflowExecutions.queries', () => ({
    useGetIntegrationWorkflowExecutionQuery: () => ({data: queryState.workflowExecution, isLoading: false}),
}));

vi.mock('../WorkflowExecutionDetail', () => ({
    default: ({workflowExecutionId}: {workflowExecutionId: number}) => (
        <div>{`Execution details ${workflowExecutionId}`}</div>
    ),
}));

const renderSheet = (workflowExecution: object) => {
    queryState.workflowExecution = workflowExecution;

    return render(<WorkflowExecutionSheet />);
};

describe('embedded WorkflowExecutionSheet', () => {
    beforeEach(() => {
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
});
