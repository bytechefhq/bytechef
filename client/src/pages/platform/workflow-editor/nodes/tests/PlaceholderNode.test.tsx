import {FINAL_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {fireEvent, render, screen} from '@testing-library/react';
import {ReactFlowProvider} from '@xyflow/react';
import {describe, expect, it, vi} from 'vitest';

import {WorkflowEditorReadOnlyContext} from '../../providers/workflowEditorReadOnlyContext';
import {CANVAS_DRAG_DATA_TYPE, TRIGGER_DRAG_DATA_TYPE} from '../../utils/canvasDragData';
import PlaceholderNode from '../PlaceholderNode';

vi.mock('../../components/WorkflowNodesPopoverMenu', () => ({
    default: ({children}: {children: React.ReactNode}) => <div data-testid="add-node-popover">{children}</div>,
}));

vi.mock('../../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: undefined}),
}));

const renderPlaceholder = (id: string) =>
    render(
        <ReactFlowProvider>
            <PlaceholderNode data={{label: '+'} as NodeDataType} id={id} />
        </ReactFlowProvider>
    );

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
    it('renders the final placeholder as a dashed box with a plus icon', () => {
        renderPlaceholder(FINAL_PLACEHOLDER_NODE_ID);

        const placeholderBox = screen.getByTitle('Click to add a node');

        expect(placeholderBox).toHaveClass('size-12', 'border-dashed');
        expect(placeholderBox.querySelector('svg')).toBeInTheDocument();
        expect(placeholderBox).not.toHaveTextContent('+');
    });

    it('keeps an in-chain placeholder as the small text chip', () => {
        renderPlaceholder('condition_1-placeholder');

        const placeholderBox = screen.getByTitle('Click to add a node');

        expect(placeholderBox).not.toHaveClass('border-dashed');
        expect(placeholderBox).toHaveTextContent('+');
    });

    it('keeps its size and shows the drop highlight while a task is dragged over it', () => {
        renderPlaceholder(FINAL_PLACEHOLDER_NODE_ID);

        const placeholderBox = screen.getByTitle('Click to add a node');

        fireEvent.dragEnter(placeholderBox, {dataTransfer: {types: [CANVAS_DRAG_DATA_TYPE]}});

        expect(placeholderBox).toHaveClass('size-12');
        expect(placeholderBox).not.toHaveClass('absolute');
        expect(screen.getByTestId('dropzone-highlight')).toBeInTheDocument();
    });

    it('shows no drop highlight while a trigger is dragged over it', () => {
        renderPlaceholder(FINAL_PLACEHOLDER_NODE_ID);

        fireEvent.dragEnter(screen.getByTitle('Click to add a node'), {
            dataTransfer: {types: [CANVAS_DRAG_DATA_TYPE, TRIGGER_DRAG_DATA_TYPE]},
        });

        expect(screen.queryByTestId('dropzone-highlight')).not.toBeInTheDocument();
    });

    describe('read-only mode', () => {
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
});
