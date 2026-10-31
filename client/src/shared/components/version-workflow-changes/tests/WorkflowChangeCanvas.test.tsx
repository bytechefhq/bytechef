import {render, waitFor} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowChangeCanvas from '../WorkflowChangeCanvas';

const mocks = vi.hoisted(() => ({
    buildWorkflowGraph: vi.fn(),
    getLayoutElements: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/utils/buildWorkflowGraph', () => ({default: mocks.buildWorkflowGraph}));

vi.mock('@/pages/platform/workflow-editor/utils/layoutUtils', () => ({
    getLayoutElements: mocks.getLayoutElements,
}));

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    Background: () => null,
    ReactFlow: () => <div data-testid="flow" />,
}));

const workflow = {tasks: [], triggers: []} as never;

const renderCanvas = () =>
    render(
        <WorkflowChangeCanvas
            componentDefinitions={[]}
            highlights={{}}
            taskDispatcherDefinitions={[]}
            workflow={workflow}
        />
    );

describe('WorkflowChangeCanvas', () => {
    beforeEach(() => {
        mocks.buildWorkflowGraph.mockReset();
        mocks.getLayoutElements.mockReset();
    });

    it('skips the layout when only placeholder nodes remain', async () => {
        mocks.buildWorkflowGraph.mockReturnValue({
            edges: [{id: 'e1', source: 'trigger-placeholder', target: 'x'}],
            nodes: [{data: {}, id: 'trigger-placeholder', position: {x: 0, y: 0}, type: 'placeholder'}],
        });

        renderCanvas();

        await waitFor(() => expect(mocks.buildWorkflowGraph).toHaveBeenCalled());

        expect(mocks.getLayoutElements).not.toHaveBeenCalled();
    });

    it('does not reject when the layout fails', async () => {
        mocks.buildWorkflowGraph.mockReturnValue({
            edges: [],
            nodes: [{data: {}, id: 'a', position: {x: 0, y: 0}, type: 'workflow'}],
        });
        mocks.getLayoutElements.mockRejectedValue(new Error('boom'));

        renderCanvas();

        await waitFor(() => expect(mocks.getLayoutElements).toHaveBeenCalled());
    });

    it('drops edges that reference removed placeholder nodes', async () => {
        mocks.buildWorkflowGraph.mockReturnValue({
            edges: [
                {id: 'e1', source: 'trigger-placeholder', target: 'a'},
                {id: 'e2', source: 'a', target: 'b'},
            ],
            nodes: [
                {data: {}, id: 'trigger-placeholder', position: {x: 0, y: 0}, type: 'placeholder'},
                {data: {}, id: 'a', position: {x: 0, y: 0}, type: 'workflow'},
                {data: {}, id: 'b', position: {x: 0, y: 0}, type: 'workflow'},
            ],
        });
        mocks.getLayoutElements.mockResolvedValue({edges: [], nodes: []});

        renderCanvas();

        await waitFor(() => expect(mocks.getLayoutElements).toHaveBeenCalled());

        const {edges, nodes} = mocks.getLayoutElements.mock.calls[0][0];
        const nodeIds = nodes.map((node: {id: string}) => node.id);

        expect(edges.map((edge: {id: string}) => edge.id)).toEqual(['e2']);
        edges.forEach((edge: {source: string; target: string}) => {
            expect(nodeIds).toContain(edge.source);
            expect(nodeIds).toContain(edge.target);
        });
    });
});
