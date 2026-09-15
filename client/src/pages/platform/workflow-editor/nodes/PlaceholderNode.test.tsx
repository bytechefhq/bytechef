import {NodeDataType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {ReactFlowProvider} from '@xyflow/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import {WorkflowEditorReadOnlyContext} from '../providers/workflowEditorReadOnlyContext';
import PlaceholderNode from './PlaceholderNode';

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu', () => ({
    default: ({children}: {children: ReactNode}) => <div data-testid="add-node-popover">{children}</div>,
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: {mutate: vi.fn()}}),
}));

const placeholderData = {label: '+', name: 'placeholder_1'} as NodeDataType;

const renderPlaceholderNode = (readOnly: boolean) =>
    render(
        <ReactFlowProvider>
            <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
                <PlaceholderNode data={placeholderData} id="placeholder_1" />
            </WorkflowEditorReadOnlyContext.Provider>
        </ReactFlowProvider>
    );

describe('PlaceholderNode', () => {
    it('offers adding a node when the editor is editable', () => {
        renderPlaceholderNode(false);

        expect(screen.getByTitle('Click to add a node')).toBeInTheDocument();
        expect(screen.getByTestId('add-node-popover')).toBeInTheDocument();
    });

    it('offers no add-node affordance in read-only mode', () => {
        renderPlaceholderNode(true);

        expect(screen.queryByTitle('Click to add a node')).not.toBeInTheDocument();
        expect(screen.queryByTestId('add-node-popover')).not.toBeInTheDocument();
        expect(screen.queryByText('+')).not.toBeInTheDocument();
    });
});
