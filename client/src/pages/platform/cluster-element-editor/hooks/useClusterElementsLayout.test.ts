import {act, renderHook} from '@testing-library/react';
import {Node} from '@xyflow/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../../workflow-editor/stores/useWorkflowEditorStore';
import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';
import useClusterElementsLayout from './useClusterElementsLayout';

const {getClusterElementsLayoutElementsMock} = vi.hoisted(() => ({
    getClusterElementsLayoutElementsMock: vi.fn(),
}));

// The real layout is exercised by its own tests; here it only needs to be observable and to hand back
// positions that differ from the ones a user would have dragged a node to.
vi.mock('@/pages/platform/workflow-editor/utils/layoutUtils', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/pages/platform/workflow-editor/utils/layoutUtils')>()),
    getClusterElementsLayoutElements: getClusterElementsLayoutElementsMock,
}));

const {createClusterElementsNodesMock} = vi.hoisted(() => ({
    createClusterElementsNodesMock: vi.fn(),
}));

vi.mock('@/pages/platform/cluster-element-editor/utils/createClusterElementsNodes', () => ({
    default: createClusterElementsNodesMock,
}));

vi.mock('@/pages/platform/cluster-element-editor/utils/createClusterElementsEdges', () => ({
    default: () => [],
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    ComponentDefinitionKeys: {componentDefinition: () => ['componentDefinition']},
    useGetComponentDefinitionQuery: () => ({data: undefined}),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({fetchQuery: vi.fn()}),
}));

const ROOT_NODE_NAME = 'aiAgent_1';

const clusterElementNode = (name: string, label: string): Node => ({
    data: {label},
    id: name,
    parentId: ROOT_NODE_NAME,
    position: {x: 0, y: 0},
    type: 'workflow',
});

const buildWorkflowDefinition = (revision: number) =>
    JSON.stringify({
        tasks: [{clusterElements: {}, name: ROOT_NODE_NAME, revision}],
    });

const setWorkflowDefinition = (revision: number) => {
    useWorkflowDataStore.setState({
        workflow: {definition: buildWorkflowDefinition(revision), id: 'workflow-1'},
    } as Parameters<typeof useWorkflowDataStore.setState>[0]);
};

// Positions a user would have dragged the nodes to, which a re-layout would discard.
const handPlacedNodes = (): Array<Node> => [
    {data: {label: 'root'}, id: ROOT_NODE_NAME, position: {x: 900, y: 900}, type: 'workflow'},
    {...clusterElementNode('anthropic_1', 'stale'), position: {x: 700, y: 700}},
];

describe('useClusterElementsLayout', () => {
    beforeEach(() => {
        getClusterElementsLayoutElementsMock.mockReset();
        getClusterElementsLayoutElementsMock.mockImplementation(({nodes}: {nodes: Array<Node>}) => ({
            edges: [],
            nodes: nodes.map((node) => ({...node, position: {x: 0, y: 0}})),
        }));

        createClusterElementsNodesMock.mockReset();
        createClusterElementsNodesMock.mockReturnValue([clusterElementNode('anthropic_1', 'fresh')]);

        useClusterElementsDataStore.getState().reset();
        useClusterElementsDataStore.setState({nodes: handPlacedNodes()});

        useWorkflowEditorStore.setState({
            mainClusterRootComponentDefinition: {name: 'aiAgent'},
            nestedClusterRootsComponentDefinitions: {},
            rootClusterElementNodeData: {
                componentName: 'aiAgent',
                operationName: 'chat',
                type: 'aiAgent/v1',
                workflowNodeName: ROOT_NODE_NAME,
            },
        } as Parameters<typeof useWorkflowEditorStore.setState>[0]);

        setWorkflowDefinition(1);
    });

    it('lays out the canvas when nodes are locked', () => {
        renderHook(() => useClusterElementsLayout());

        expect(getClusterElementsLayoutElementsMock).toHaveBeenCalled();
        expect(useClusterElementsDataStore.getState().nodes.map((node) => node.position)).toEqual([
            {x: 0, y: 0},
            {x: 0, y: 0},
        ]);
    });

    it('keeps hand-placed positions when nodes are unlocked and the node set is unchanged', () => {
        useClusterElementsDataStore.setState({nodesLocked: false});

        const {rerender} = renderHook(() => useClusterElementsLayout());

        act(() => setWorkflowDefinition(2));
        rerender();

        expect(getClusterElementsLayoutElementsMock).not.toHaveBeenCalled();

        const {nodes} = useClusterElementsDataStore.getState();

        expect(nodes.map((node) => node.position)).toEqual([
            {x: 900, y: 900},
            {x: 700, y: 700},
        ]);
        // Positions are pinned, but the rebuilt data still reaches the canvas: the element node picks up its new
        // label, and the root node picks up the freshly rebuilt data it is given by the layout pass.
        expect(nodes[1].data.label).toBe('fresh');
        expect(nodes[0].data.workflowNodeName).toBe(ROOT_NODE_NAME);
    });

    it('lays out the canvas when unlocked and an element is added', () => {
        useClusterElementsDataStore.setState({nodesLocked: false});

        const {rerender} = renderHook(() => useClusterElementsLayout());

        createClusterElementsNodesMock.mockReturnValue([
            clusterElementNode('anthropic_1', 'fresh'),
            clusterElementNode('activeCampaign_2', 'added'),
        ]);

        act(() => setWorkflowDefinition(2));
        rerender();

        expect(getClusterElementsLayoutElementsMock).toHaveBeenCalled();
    });

    it('lays out the canvas when unlocked and a layout reset is requested', () => {
        useClusterElementsDataStore.setState({nodesLocked: false});

        const {rerender} = renderHook(() => useClusterElementsLayout());

        act(() => useClusterElementsDataStore.getState().incrementLayoutResetCounter());
        rerender();

        expect(getClusterElementsLayoutElementsMock).toHaveBeenCalled();
        expect(useClusterElementsDataStore.getState().nodes.map((node) => node.position)).toEqual([
            {x: 0, y: 0},
            {x: 0, y: 0},
        ]);
    });
});
