import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {fireEvent, render, screen} from '@testing-library/react';
import {Position, ReactFlowProvider, useStoreApi} from '@xyflow/react';
import {ReactNode, useState} from 'react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import GraphTransitionEdge, {GraphTransitionEdgeDataI} from './GraphTransitionEdge';

// The transition editor is a standalone surface with its own coverage; rendering it as a stub keeps
// this test about WHEN the edge opens it, echoing the transition it was handed.
vi.mock('@/pages/platform/workflow-editor/components/properties/graph/GraphTransitionPopover', () => ({
    default: ({graphId, index}: {graphId: string; index: number}) => <div>{`editing ${graphId}[${index}]`}</div>,
}));

/**
 * `EdgeLabelRenderer` portals its children into the `.react-flow__edgelabel-renderer` div that
 * React Flow's store reaches through `domNode`. A bare `ReactFlowProvider` leaves `domNode` unset,
 * so a label would silently render nothing — this harness supplies the container and points the
 * store at it, leaving the portal itself real.
 */
function EdgeHarness({children}: {children: ReactNode}) {
    const [domNodeReady, setDomNodeReady] = useState(false);

    const store = useStoreApi();

    return (
        <div
            ref={(node) => {
                if (node && !domNodeReady) {
                    // Built imperatively, exactly as `<ReactFlow>` builds it: it is React Flow's
                    // own container, not a styled element of this test's making.
                    const labelContainer = document.createElement('div');

                    labelContainer.className = 'react-flow__edgelabel-renderer';

                    node.appendChild(labelContainer);

                    store.setState({domNode: node});

                    setDomNodeReady(true);
                }
            }}
        >
            <svg>{domNodeReady && children}</svg>
        </div>
    );
}

function renderTransitionEdge(
    data: Partial<GraphTransitionEdgeDataI> = {},
    {selected = false, source = 'task_a', target = 'task_b'} = {}
) {
    return render(
        <ReactFlowProvider>
            <EdgeHarness>
                <GraphTransitionEdge
                    data={{graphId: 'graph_1', index: 0, to: 'task_b', ...data}}
                    id="graph_1-transition-0"
                    selected={selected}
                    source={source}
                    sourcePosition={Position.Right}
                    sourceX={340}
                    sourceY={250}
                    target={target}
                    targetPosition={Position.Left}
                    targetX={600}
                    targetY={250}
                />
            </EdgeHarness>
        </ReactFlowProvider>
    );
}

function getVisibleEdgePath(container: HTMLElement) {
    return container.querySelectorAll('path')[1];
}

describe('GraphTransitionEdge', () => {
    // Which outgoing transition a node takes is decided by these expressions in declared order, so a
    // graph that reveals them one hover at a time cannot be read — the branching IS the picture.
    it('shows the condition label at rest, on hover and after the pointer leaves', () => {
        const {container} = renderTransitionEdge({condition: '=x > 1'});

        expect(screen.getByText('=x > 1')).toBeInTheDocument();

        fireEvent.mouseEnter(container.querySelectorAll('path')[0]!);

        expect(screen.getByText('=x > 1')).toBeInTheDocument();

        fireEvent.mouseLeave(container.querySelectorAll('path')[0]!);

        expect(screen.getByText('=x > 1')).toBeInTheDocument();
    });

    it('shows the condition label while the edge is selected, without hovering', () => {
        renderTransitionEdge({condition: '=x > 1'}, {selected: true});

        expect(screen.getByText('=x > 1')).toBeInTheDocument();
    });

    it('names a dynamic transition with a standing badge at rest', () => {
        renderTransitionEdge({dynamic: true, to: "=nextStep + '_1'"}, {source: 'task_a', target: 'task_a'});

        expect(screen.getByText('dynamic')).toBeInTheDocument();
    });

    // An unconditional transition has nothing to say, and is not the dynamic kind either.
    it('leaves an unconditional static transition unlabelled', () => {
        renderTransitionEdge({});

        expect(screen.queryByText('dynamic')).not.toBeInTheDocument();
    });

    it('labels a dynamic transition with the expression it resolves at run time', () => {
        const {container} = renderTransitionEdge(
            {dynamic: true, to: "=nextStep + '_1'"},
            {source: 'task_a', target: 'task_a'}
        );

        fireEvent.mouseEnter(container.querySelectorAll('path')[0]!);

        expect(screen.getByText("dynamic: =nextStep + '_1'")).toBeInTheDocument();
    });

    it('dashes the stroke of a dynamic transition', () => {
        const {container} = renderTransitionEdge({dynamic: true, to: '=nextStep'}, {source: 'a', target: 'a'});

        expect(getVisibleEdgePath(container)?.getAttribute('style')).toContain('stroke-dasharray: 6 4');
    });

    it('leaves a static transition solid', () => {
        const {container} = renderTransitionEdge({condition: '=x > 1'});

        expect(getVisibleEdgePath(container)?.getAttribute('style')).not.toContain('stroke-dasharray');
    });

    it('paints a dangling transition in the warning color, even while hovered', () => {
        const {container} = renderTransitionEdge({dangling: true, to: 'deleted_task'});

        fireEvent.mouseEnter(container.querySelectorAll('path')[0]!);

        expect(getVisibleEdgePath(container)?.getAttribute('style')).toContain('stroke: rgb(217, 119, 6)');
    });

    it('switches to the active stroke color on hover', () => {
        const {container} = renderTransitionEdge();

        expect(getVisibleEdgePath(container)?.getAttribute('style')).toContain('stroke: rgb(139, 127, 232)');

        fireEvent.mouseEnter(container.querySelectorAll('path')[0]!);

        expect(getVisibleEdgePath(container)?.getAttribute('style')).toContain('stroke: rgb(91, 79, 199)');
    });

    it('draws a bezier path between two distinct members', () => {
        const {container} = renderTransitionEdge();

        const pathDefinition = getVisibleEdgePath(container)?.getAttribute('d');

        expect(pathDefinition).toContain('C');
    });

    // WHEN the editor opens is `GraphTransitionEditorLayer`'s contract now — this edge only publishes
    // where it would hang, for the layer to position it by, and does so whether or not it is selected
    // (the layer decides, and a position for an unselected edge is simply never read).
    it('publishes its label position for the canvas-level editor to hang from', () => {
        renderTransitionEdge({index: 3}, {selected: true});

        const {graphTransitionLabelPositions} = useWorkflowEditorStore.getState();

        expect(graphTransitionLabelPositions['graph_1-transition-0']).toEqual({
            labelX: expect.any(Number),
            labelY: expect.any(Number),
        });
    });

    it('does not render the editor itself, so a relayout recreating this edge cannot take it down', () => {
        renderTransitionEdge({index: 3}, {selected: true});

        expect(screen.queryByText('editing graph_1[3]')).not.toBeInTheDocument();
    });

    it('bows a self transition out to one side instead of drawing it through the box', () => {
        const {container} = renderTransitionEdge({to: 'task_a'}, {source: 'task_a', target: 'task_a'});

        expect(getVisibleEdgePath(container)?.getAttribute('d')).toContain('C ');
    });
});

/**
 * A conditional fan-out from `task_a`: the run routed to `task_b`, which routed on to `task_c`.
 * All three completed, so a rule keyed on the two endpoints' states alone would light `task_a` ->
 * `task_c` as well.
 */
function seedFanOutRun(finalStatus = 'COMPLETED') {
    useWorkflowEditorStore.setState({
        workflowTestExecution: {
            job: {
                taskExecutions: [
                    {
                        children: [
                            {
                                startDate: new Date('2024-01-01T10:00:01'),
                                status: 'COMPLETED',
                                workflowTask: {name: 'task_a'},
                            },
                            {
                                startDate: new Date('2024-01-01T10:00:02'),
                                status: 'COMPLETED',
                                workflowTask: {name: 'task_b'},
                            },
                            {
                                startDate: new Date('2024-01-01T10:00:03'),
                                status: finalStatus,
                                workflowTask: {name: 'task_c'},
                            },
                        ],
                        workflowTask: {name: 'graph_1'},
                    },
                ],
            },
        } as unknown as WorkflowTestExecution,
    });
}

describe('GraphTransitionEdge executed highlighting', () => {
    afterEach(() => {
        useWorkflowEditorStore.setState({workflowTestExecution: undefined});
    });

    it('paints a transition the run took in the executed color', () => {
        seedFanOutRun();

        const {container} = renderTransitionEdge({to: 'task_b'});

        expect(getVisibleEdgePath(container)?.classList.contains('stroke-green-500')).toBe(true);
    });

    it('leaves an untaken sibling transition unhighlighted even though its target completed', () => {
        seedFanOutRun();

        const {container} = renderTransitionEdge({to: 'task_c'}, {source: 'task_a', target: 'task_c'});

        expect(getVisibleEdgePath(container)?.classList.contains('stroke-green-500')).toBe(false);
        expect(getVisibleEdgePath(container)?.getAttribute('style')).toContain('stroke: rgb(139, 127, 232)');
    });

    it('paints a transition that led to a failed visit in the failed color', () => {
        seedFanOutRun('FAILED');

        const {container} = renderTransitionEdge({to: 'task_c'}, {source: 'task_b', target: 'task_c'});

        expect(getVisibleEdgePath(container)?.classList.contains('stroke-red-500')).toBe(true);
    });

    it('leaves every transition unhighlighted before anything ran', () => {
        const {container} = renderTransitionEdge({to: 'task_b'});

        expect(getVisibleEdgePath(container)?.classList.contains('stroke-green-500')).toBe(false);
        expect(getVisibleEdgePath(container)?.classList.contains('stroke-red-500')).toBe(false);
    });
});
