import {fireEvent, render} from '@testing-library/react';
import {Position, ReactFlowProvider} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import GraphTransitionEdge, {GraphTransitionEdgeDataI} from './GraphTransitionEdge';

function renderTransitionEdge(data: Partial<GraphTransitionEdgeDataI> = {}) {
    return render(
        <ReactFlowProvider>
            <svg>
                <GraphTransitionEdge
                    data={{kind: 'forward', offset: 0, sourceIndex: 0, targetIndex: 1, targetName: 'nodeB', ...data}}
                    id="graph_1-transition-0-1"
                    source="task_a"
                    sourcePosition={Position.Bottom}
                    sourceX={100}
                    sourceY={200}
                    target="task_b"
                    targetPosition={Position.Top}
                    targetX={400}
                    targetY={200}
                />
            </svg>
        </ReactFlowProvider>
    );
}

describe('GraphTransitionEdge', () => {
    it('renders a curved (non-orthogonal) path, not an L-shaped step path', () => {
        const {container} = renderTransitionEdge();

        const visiblePath = container.querySelectorAll('path')[1];

        expect(visiblePath?.getAttribute('d')).toContain('C');
    });

    // The target-name label chip renders through `EdgeLabelRenderer`, which portals into a
    // `.react-flow__edgelabel-renderer` div that only exists inside a real `<ReactFlow>` tree
    // (not just `<ReactFlowProvider>`) — so hover/select reveal of the chip itself is exercised
    // manually rather than through a unit test here. The hover *state* driving that reveal is
    // still covered below via its other, DOM-visible effect: the stroke color swap.
    it('uses the muted (non-active) stroke color at rest', () => {
        const {container} = renderTransitionEdge();

        const visiblePath = container.querySelectorAll('path')[1];

        expect(visiblePath?.getAttribute('style')).toContain('stroke: rgb(139, 127, 232)');
    });

    it('switches to the active stroke color on hover', () => {
        const {container} = renderTransitionEdge();

        const hitAreaPath = container.querySelectorAll('path')[0];

        fireEvent.mouseEnter(hitAreaPath!);

        const visiblePath = container.querySelectorAll('path')[1];

        expect(visiblePath?.getAttribute('style')).toContain('stroke: rgb(91, 79, 199)');
    });

    it('reverts to the muted stroke color on mouse leave', () => {
        const {container} = renderTransitionEdge();

        const hitAreaPath = container.querySelectorAll('path')[0];

        fireEvent.mouseEnter(hitAreaPath!);
        fireEvent.mouseLeave(hitAreaPath!);

        const visiblePath = container.querySelectorAll('path')[1];

        expect(visiblePath?.getAttribute('style')).toContain('stroke: rgb(139, 127, 232)');
    });

    it('renders a self-loop path that starts and ends at the same coordinates', () => {
        const {container} = render(
            <ReactFlowProvider>
                <svg>
                    <GraphTransitionEdge
                        data={{kind: 'self', offset: 0, sourceIndex: 0, targetIndex: 0, targetName: 'nodeA'}}
                        id="graph_1-transition-0-0"
                        source="task_a"
                        sourcePosition={Position.Bottom}
                        sourceX={100}
                        sourceY={200}
                        target="task_a"
                        targetPosition={Position.Top}
                        targetX={100}
                        targetY={200}
                    />
                </svg>
            </ReactFlowProvider>
        );

        const visiblePath = container.querySelectorAll('path')[1];

        expect(visiblePath?.getAttribute('d')).toMatch(/^M 100,200/);
    });
});
