import {render, screen} from '@/shared/util/test-utils';
import {ReactFlowProvider} from '@xyflow/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it} from 'vitest';

import {WorkflowEditorReadOnlyContext} from '../../providers/workflowEditorReadOnlyContext';
import useWorkflowIssuesStore from '../../stores/useWorkflowIssuesStore';
import WorkflowIssuesNote from '../WorkflowIssuesNote';

const renderNote = (readOnly = false, fallback: ReactNode = <div>hint</div>) =>
    render(
        <ReactFlowProvider>
            <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
                <WorkflowIssuesNote fallback={fallback} />
            </WorkflowEditorReadOnlyContext.Provider>
        </ReactFlowProvider>
    );

describe('WorkflowIssuesNote', () => {
    beforeEach(() => {
        useWorkflowIssuesStore.getState().reset();
    });

    it('renders the fallback when there are no issues', () => {
        renderNote();

        expect(screen.getByText('hint')).toBeInTheDocument();
    });

    it('replaces the fallback with a count and opens the sidebar on View', () => {
        useWorkflowIssuesStore.getState().setValidatorIssues([
            {kind: 'MISSING_REQUIRED', message: 'a', nodeName: 'n_1', severity: 'ERROR', source: 'VALIDATOR'},
            {kind: 'MISSING_REQUIRED', message: 'b', nodeName: 'n_2', severity: 'WARNING', source: 'VALIDATOR'},
        ]);

        renderNote();

        expect(screen.queryByText('hint')).not.toBeInTheDocument();
        expect(screen.getByText('1 error, 1 warning in this workflow')).toBeInTheDocument();

        screen.getByRole('button', {name: 'View'}).click();

        expect(useWorkflowIssuesStore.getState().issuesSidebarOpen).toBe(true);
    });

    it('renders nothing at all in read-only mode, not even the fallback', () => {
        useWorkflowIssuesStore
            .getState()
            .setValidatorIssues([
                {kind: 'MISSING_REQUIRED', message: 'a', nodeName: 'n_1', severity: 'ERROR', source: 'VALIDATOR'},
            ]);

        const {container} = renderNote(true);

        expect(container).toBeEmptyDOMElement();
    });
});
