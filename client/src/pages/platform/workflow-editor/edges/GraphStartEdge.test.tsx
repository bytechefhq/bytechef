import {render} from '@testing-library/react';
import {Position, ReactFlowProvider} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import GraphStartEdge from './GraphStartEdge';

function renderStartEdge(props: Partial<Parameters<typeof GraphStartEdge>[0]> = {}) {
    const {container} = render(
        <ReactFlowProvider>
            <svg>
                <GraphStartEdge
                    id="graph_1-graph-start-edge"
                    source="graph_1-graph-start"
                    sourcePosition={Position.Bottom}
                    sourceX={100}
                    sourceY={40}
                    target="task_1"
                    targetPosition={Position.Top}
                    targetX={100}
                    targetY={200}
                    {...props}
                />
            </svg>
        </ReactFlowProvider>
    );

    return container.querySelector('path.react-flow__edge-path');
}

describe('GraphStartEdge', () => {
    it('draws a path from the start pill to the entry node', () => {
        const edgePath = renderStartEdge();

        const pathDefinition = edgePath?.getAttribute('d') ?? '';

        expect(pathDefinition.startsWith('M 100,40') || pathDefinition.startsWith('M100 40')).toBe(true);
        expect(pathDefinition).toContain('200');
    });

    it('renders the neutral start edge stroke and forwards the given style', () => {
        const edgePath = renderStartEdge({style: {strokeDasharray: '4 2'}});

        expect(edgePath?.getAttribute('class')).toContain('stroke-stroke-neutral-tertiary');
        expect(edgePath?.getAttribute('style')).toContain('stroke-dasharray: 4 2');
    });
});
