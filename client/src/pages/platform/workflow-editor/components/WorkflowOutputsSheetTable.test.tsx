import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowOutputsSheetTable from './WorkflowOutputsSheetTable';

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        cancelWorkflowQueries: vi.fn(),
        invalidateWorkflowQueries: vi.fn(),
        updateWorkflowMutation: {mutate: vi.fn()},
    }),
}));

const longOutputValue =
    'a-very-long-output-value-that-would-otherwise-stretch-the-workflow-outputs-panel-past-its-visible-width';

const workflow: Workflow = {
    outputs: [{name: 'longOutput', value: longOutputValue as unknown as object}],
};

const renderTable = () => render(<WorkflowOutputsSheetTable workflow={workflow} />);

beforeEach(() => {
    useWorkflowDataStore.setState({componentDefinitions: []});
});

afterEach(() => {
    resetAll();
});

describe('WorkflowOutputsSheetTable', () => {
    it('should truncate a long output value instead of widening the table', () => {
        renderTable();

        const valueCell = screen.getByTitle(longOutputValue);

        expect(valueCell).toHaveClass('overflow-hidden');

        expect(valueCell.closest('table')).toHaveClass('table-fixed');
    });

    it('should truncate a long output name instead of widening the table', () => {
        renderTable();

        expect(screen.getByText('longOutput')).toHaveClass('truncate');
    });

    it('should expose the full output value through the cell title', () => {
        renderTable();

        expect(screen.getByTitle(longOutputValue)).toBeInTheDocument();
    });

    it('should offer editing and deleting an output when the editor is editable', () => {
        renderTable();

        expect(screen.getAllByRole('button')).toHaveLength(2);
    });

    it('should offer neither editing nor deleting an output in read-only mode', () => {
        render(
            <WorkflowEditorReadOnlyContext.Provider value={true}>
                <WorkflowOutputsSheetTable workflow={workflow} />
            </WorkflowEditorReadOnlyContext.Provider>
        );

        expect(screen.getByText('longOutput')).toBeInTheDocument();
        expect(screen.queryAllByRole('button')).toHaveLength(0);
    });

    it('should not offer creating the first output in read-only mode', () => {
        render(
            <WorkflowEditorReadOnlyContext.Provider value={true}>
                <WorkflowOutputsSheetTable workflow={{outputs: []}} />
            </WorkflowEditorReadOnlyContext.Provider>
        );

        expect(screen.getByText('No outputs')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New Output'})).not.toBeInTheDocument();
    });
});
