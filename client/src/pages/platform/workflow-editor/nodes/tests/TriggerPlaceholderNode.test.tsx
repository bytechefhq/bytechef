import {TRIGGER_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {fireEvent, render, screen} from '@testing-library/react';
import {ReactFlowProvider} from '@xyflow/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import useLayoutDirectionStore from '../../stores/useLayoutDirectionStore';
import {CANVAS_DRAG_DATA_TYPE, TRIGGER_DRAG_DATA_TYPE} from '../../utils/canvasDragData';
import TriggerPlaceholderNode from '../TriggerPlaceholderNode';

vi.mock('../../components/WorkflowNodesPopoverMenu', () => ({
    default: ({children}: {children: React.ReactNode}) => <div data-testid="trigger-picker">{children}</div>,
}));

vi.mock('../../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: undefined}),
}));

describe('TriggerPlaceholderNode', () => {
    afterEach(() => {
        useLayoutDirectionStore.setState({layoutDirection: 'TB'});
    });

    it('renders a + slot wrapped in the trigger picker', () => {
        render(
            <ReactFlowProvider>
                <TriggerPlaceholderNode data={{label: '+'} as NodeDataType} id={TRIGGER_PLACEHOLDER_NODE_ID} />
            </ReactFlowProvider>
        );

        expect(screen.getByTestId('trigger-picker')).toBeInTheDocument();
        expect(screen.getByTitle('Click to add a trigger')).toBeInTheDocument();
    });

    const renderSlotConnector = () => {
        const {container} = render(
            <ReactFlowProvider>
                <TriggerPlaceholderNode data={{label: '+'} as NodeDataType} id={TRIGGER_PLACEHOLDER_NODE_ID} />
            </ReactFlowProvider>
        );

        return container.querySelector('div[aria-hidden="true"]');
    };

    it('draws a sideways connector back to the trigger row in TB', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'TB'});

        expect(renderSlotConnector()).toHaveClass('right-full', 'border-t-2');
    });

    it('draws an upward connector back to the trigger column in LR', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'LR'});

        expect(renderSlotConnector()).toHaveClass('bottom-full', 'border-l-2');
    });

    it('shows the drop highlight only while a trigger is dragged over it', () => {
        render(
            <ReactFlowProvider>
                <TriggerPlaceholderNode data={{label: '+'} as NodeDataType} id={TRIGGER_PLACEHOLDER_NODE_ID} />
            </ReactFlowProvider>
        );

        const slot = screen.getByTitle('Click to add a trigger');

        fireEvent.dragEnter(slot, {dataTransfer: {types: [CANVAS_DRAG_DATA_TYPE]}});

        expect(screen.queryByTestId('dropzone-highlight')).not.toBeInTheDocument();

        fireEvent.dragEnter(slot, {dataTransfer: {types: [CANVAS_DRAG_DATA_TYPE, TRIGGER_DRAG_DATA_TYPE]}});

        expect(screen.getByTestId('dropzone-highlight')).toBeInTheDocument();
    });
});
