import {render} from '@testing-library/react';
import {type Edge, type Node, ReactFlowProvider} from '@xyflow/react';
import {beforeEach, describe, expect, it} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import TaskDispatcherBottomGhostNode from './TaskDispatcherBottomGhostNode';
import TaskDispatcherTopGhostNode from './TaskDispatcherTopGhostNode';

const WORKFLOW_ID = 'workflow-1';
const TOP_GHOST_ID = 'condition_1-condition-top-ghost';
const BOTTOM_GHOST_ID = 'condition_1-condition-bottom-ghost';

const GHOST_DATA = {taskDispatcherId: 'condition_1'};

const NODES: Node[] = [
    {data: GHOST_DATA, id: TOP_GHOST_ID, position: {x: 0, y: 0}},
    {data: GHOST_DATA, id: BOTTOM_GHOST_ID, position: {x: 0, y: 0}},
    {data: {workflowNodeName: 'resultAfter'}, id: 'resultAfter', position: {x: 0, y: 0}},
    {data: {workflowNodeName: 'resultNotAfter'}, id: 'resultNotAfter', position: {x: 0, y: 0}},
];

const EDGES: Edge[] = [
    {
        id: `${TOP_GHOST_ID}=>resultAfter`,
        source: TOP_GHOST_ID,
        sourceHandle: `${TOP_GHOST_ID}-left`,
        target: 'resultAfter',
    },
    {
        id: `${TOP_GHOST_ID}=>resultNotAfter`,
        source: TOP_GHOST_ID,
        sourceHandle: `${TOP_GHOST_ID}-right`,
        target: 'resultNotAfter',
    },
    {
        id: `resultAfter=>${BOTTOM_GHOST_ID}`,
        source: 'resultAfter',
        target: BOTTOM_GHOST_ID,
        targetHandle: `${BOTTOM_GHOST_ID}-left`,
    },
    {
        id: `resultNotAfter=>${BOTTOM_GHOST_ID}`,
        source: 'resultNotAfter',
        target: BOTTOM_GHOST_ID,
        targetHandle: `${BOTTOM_GHOST_ID}-right`,
    },
];

function renderGhostBar(bar: 'bottom' | 'top') {
    const Ghost = bar === 'top' ? TaskDispatcherTopGhostNode : TaskDispatcherBottomGhostNode;
    const ghostId = bar === 'top' ? TOP_GHOST_ID : BOTTOM_GHOST_ID;

    const {container} = render(
        <ReactFlowProvider>
            <Ghost data={GHOST_DATA} id={ghostId} />
        </ReactFlowProvider>
    );

    return (half: 'end' | 'start') => container.querySelector(`[data-ghost-bar-half="${half}"]`)?.className ?? '';
}

describe('task dispatcher ghost bar run colors', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({
            edges: EDGES,
            nodes: NODES,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            workflow: {id: WORKFLOW_ID, nodeNames: []} as any,
        });

        useWorkflowEditorStore.setState({
            workflowTestNodeStates: {
                condition_1: {status: 'COMPLETED'},
                resultNotAfter: {status: 'COMPLETED'},
            },
            workflowTestNodeStatesWorkflowId: WORKFLOW_ID,
        });
    });

    it('stops the top bar color at the center when only the right branch ran', () => {
        const half = renderGhostBar('top');

        expect(half('start')).toContain('flex-1');
        expect(half('start')).not.toContain('bg-');
        expect(half('end')).toContain('bg-green-500');
    });

    it('stops the bottom bar color at the center when only the right branch ran', () => {
        const half = renderGhostBar('bottom');

        expect(half('start')).toContain('flex-1');
        expect(half('start')).not.toContain('bg-');
        expect(half('end')).toContain('bg-green-500');
    });

    it('keeps both halves neutral when the run belongs to another workflow', () => {
        useWorkflowEditorStore.setState({workflowTestNodeStatesWorkflowId: 'other-workflow'});

        const half = renderGhostBar('top');

        expect(half('start')).toContain('flex-1');
        expect(half('start')).not.toContain('bg-');
        expect(half('end')).toContain('flex-1');
        expect(half('end')).not.toContain('bg-');
    });
});
