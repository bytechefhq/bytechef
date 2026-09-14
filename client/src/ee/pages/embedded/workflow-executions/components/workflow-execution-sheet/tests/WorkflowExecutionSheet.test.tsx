import {render} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowExecutionSheetStore from '../../../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionSheet from '../WorkflowExecutionSheet';

const {queryState} = vi.hoisted(() => ({
    queryState: {workflowExecution: undefined as unknown},
}));

vi.mock('@/ee/shared/queries/embedded/workflowExecutions.queries', () => ({
    useGetIntegrationWorkflowExecutionQuery: () => ({data: queryState.workflowExecution, isLoading: false}),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    WorkflowReadOnlyProvider: ({children}: {children: ReactNode}) => <>{children}</>,
}));

vi.mock('@/shared/components/workflow-executions/util/workflowExecution-utils', () => ({
    getWorkflowStatusType: () => 'completed',
}));

vi.mock('../WorkflowExecutionSheetContent', () => ({default: () => <div>Execution content</div>}));

vi.mock(
    '@/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheetWorkflowPanel',
    () => ({default: () => <div>Workflow panel</div>})
);

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
