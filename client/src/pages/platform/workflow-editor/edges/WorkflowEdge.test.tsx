import {render} from '@testing-library/react';
import {Position, ReactFlowProvider} from '@xyflow/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import WorkflowEdge from './WorkflowEdge';

vi.mock('../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: undefined}),
}));

vi.mock('../hooks/useCanvasDropzone', () => ({
    default: () => ({dropzoneHandlers: {}, isDropzoneActive: false}),
}));

vi.mock('../hooks/useWorkflowIsRunning', () => ({
    default: () => false,
}));

vi.mock('../components/WorkflowNodesPopoverMenu', () => ({
    default: ({children}: {children: ReactNode}) => <>{children}</>,
}));

vi.mock('./useExecutedEdgeStatus', () => ({
    default: (edgeId: string) => (edgeId === 'task_1=>task_3' ? 'COMPLETED' : undefined),
}));

describe('WorkflowEdge', () => {
    it('colors the edge with the shared executed-edge status, which bridges disabled tasks', () => {
        const {container} = render(
            <ReactFlowProvider>
                <svg>
                    <WorkflowEdge
                        id="task_1=>task_3"
                        source="task_1"
                        sourcePosition={Position.Bottom}
                        sourceX={100}
                        sourceY={100}
                        target="task_3"
                        targetPosition={Position.Top}
                        targetX={100}
                        targetY={300}
                    />
                </svg>
            </ReactFlowProvider>
        );

        expect(container.querySelector('path.stroke-green-500')).not.toBeNull();
    });
});
