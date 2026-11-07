import {render, screen} from '@testing-library/react';
import {ReactFlowProvider} from '@xyflow/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {GRAPH_START_SOURCE_HANDLE_SUFFIX} from '../utils/graph/graphHandleSuffixes';
import GraphStartNode from './GraphStartNode';

const {directionStoreState} = vi.hoisted(() => ({
    directionStoreState: {layoutDirection: 'TB'},
}));

vi.mock('../stores/useLayoutDirectionStore', () => ({
    default: (selector: (state: {layoutDirection: string}) => unknown) => selector(directionStoreState),
}));

const GRAPH_START_ID = 'graph_1-graph-start';

function renderStartNode() {
    const {container} = render(
        <ReactFlowProvider>
            <GraphStartNode id={GRAPH_START_ID} />
        </ReactFlowProvider>
    );

    return container.querySelector(`[data-handleid="${GRAPH_START_ID}${GRAPH_START_SOURCE_HANDLE_SUFFIX}"]`);
}

describe('GraphStartNode', () => {
    beforeEach(() => {
        directionStoreState.layoutDirection = 'TB';
    });

    it('renders the start pill with a source handle scoped to the graph start', () => {
        const sourceHandle = renderStartNode();

        expect(screen.getByText('Start')).toBeInTheDocument();
        expect(sourceHandle).not.toBeNull();
        expect(sourceHandle?.className).toContain('source');
    });

    it('places the source handle at the bottom in a top-to-bottom layout', () => {
        const sourceHandle = renderStartNode();

        expect(sourceHandle?.className).toContain('react-flow__handle-bottom');
    });

    it('moves the source handle to the right in a left-to-right layout', () => {
        directionStoreState.layoutDirection = 'LR';

        const sourceHandle = renderStartNode();

        expect(sourceHandle?.className).toContain('react-flow__handle-right');
    });
});
