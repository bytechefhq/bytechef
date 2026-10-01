import useClusterElementsViewModeStore from '@/pages/platform/workflow-editor/stores/useClusterElementsViewModeStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementNodes from './useClusterElementNodes';

const {AI_AGENT_DEFINITION} = vi.hoisted(() => {
    const AI_AGENT_DEFINITION: ComponentDefinition = {
        clusterElement: false,
        clusterElementTypes: [
            {label: 'Model', multipleElements: false, name: 'MODEL'},
            {label: 'Tools', multipleElements: true, name: 'TOOLS'},
        ],
        clusterRoot: true,
        connectionRequired: false,
        name: 'aiAgent',
        version: 1,
    };

    return {AI_AGENT_DEFINITION};
});

vi.mock('@/shared/middleware/platform/configuration', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/platform/configuration')>()),
    ComponentDefinitionApi: class {
        getComponentDefinition({componentName}: {componentName: string}) {
            if (componentName === 'openai' || componentName === 'anthropic') {
                return Promise.resolve({clusterElementTypes: [], name: componentName, version: 1});
            }

            if (componentName === 'aiAgent') {
                return Promise.resolve(AI_AGENT_DEFINITION);
            }

            return Promise.reject(new Error(`Unexpected component: ${componentName}`));
        }
    },
}));

const WORKFLOW_DEFINITION = JSON.stringify({
    tasks: [
        {
            clusterElements: {
                model: {label: 'GPT', name: 'model_1', type: 'openai/v1/model'},
            },
            label: 'AI Agent',
            name: 'aiAgent_1',
            type: 'aiAgent/v1/chat',
        },
    ],
});

const WORKFLOW_DEFINITION_TWO_ROOTS = JSON.stringify({
    tasks: [
        {
            clusterElements: {model: {label: 'GPT', name: 'model_1', type: 'openai/v1/model'}},
            label: 'AI Agent 1',
            name: 'aiAgent_1',
            type: 'aiAgent/v1/chat',
        },
        {
            clusterElements: {model: {label: 'Claude', name: 'model_2', type: 'anthropic/v1/model'}},
            label: 'AI Agent 2',
            name: 'aiAgent_2',
            type: 'aiAgent/v1/chat',
        },
    ],
});

const WORKFLOW_DEFINITION_WITH_BROKEN_ROOT = JSON.stringify({
    tasks: [
        {
            clusterElements: {model: {label: 'GPT', name: 'model_1', type: 'openai/v1/model'}},
            label: 'AI Agent 1',
            name: 'aiAgent_1',
            type: 'aiAgent/v1/chat',
        },
        {
            clusterElements: {model: {label: 'GPT', name: 'model_2', type: 'openai/v1/model'}},
            label: 'Broken Agent',
            name: 'brokenAgent_1',
            type: 'brokenAgent/v1/chat',
        },
    ],
});

const WORKFLOW_DEFINITION_WITH_BROKEN_NESTED_MODEL = JSON.stringify({
    tasks: [
        {
            clusterElements: {model: {label: 'GPT', name: 'model_1', type: 'openai/v1/model'}},
            label: 'AI Agent 1',
            name: 'aiAgent_1',
            type: 'aiAgent/v1/chat',
        },
        {
            clusterElements: {model: {label: 'Broken', name: 'model_2', type: 'brokenModel/v1/model'}},
            label: 'AI Agent 2',
            name: 'aiAgent_2',
            type: 'aiAgent/v1/chat',
        },
    ],
});

const CLUSTER_ROOT_IDS = ['aiAgent_1'];
const TWO_CLUSTER_ROOT_IDS = ['aiAgent_1', 'aiAgent_2'];
const HEALTHY_AND_BROKEN_ROOT_IDS = ['aiAgent_1', 'brokenAgent_1'];
const EMPTY_ROOT_IDS: string[] = [];

describe('useClusterElementNodes', () => {
    let queryClient: QueryClient;

    function wrapper({children}: {children: ReactNode}) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    beforeEach(() => {
        queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        useWorkflowDataStore.setState({
            workflow: {definition: WORKFLOW_DEFINITION, id: 'workflow_1'},
        } as Parameters<typeof useWorkflowDataStore.setState>[0]);
        useWorkflowEditorStore.setState({
            clusterRootComponentDefinitions: {aiAgent_1: AI_AGENT_DEFINITION},
            nestedClusterRootsComponentDefinitions: {},
            rootClusterElementNodeData: undefined,
        });
        useClusterElementsViewModeStore.setState({clusterElementsViewMode: 'dialog'});
    });

    it('builds one node per declared element type, all parented to the root', async () => {
        const {result} = renderHook(() => useClusterElementNodes(CLUSTER_ROOT_IDS), {wrapper});

        await waitFor(() => {
            expect(result.current.definitionsReady).toBe(true);
        });

        const nodeIds = result.current.nodesByRootId.aiAgent_1.map((node) => node.id);

        expect(nodeIds).toContain('model_1');
        expect(result.current.nodesByRootId.aiAgent_1.every((node) => node.parentId === 'aiAgent_1')).toBe(true);
        expect(result.current.nodesByRootId.aiAgent_1.some((node) => node.type === 'placeholder')).toBe(true);
    });

    it("adds the root's own card when in dialog view mode for it", async () => {
        useClusterElementsViewModeStore.setState({clusterElementsViewMode: 'dialog'});
        useWorkflowEditorStore.setState({
            rootClusterElementNodeData: {
                componentName: 'aiAgent',
                name: 'aiAgent_1',
                type: 'aiAgent/v1/chat',
                workflowNodeName: 'aiAgent_1',
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
            } as any,
        });

        const {result} = renderHook(() => useClusterElementNodes(CLUSTER_ROOT_IDS), {wrapper});

        await waitFor(() => {
            expect(result.current.definitionsReady).toBe(true);
        });

        expect(result.current.nodesByRootId.aiAgent_1.map((node) => node.id)).toContain('aiAgent_1');
    });

    it("does not add the root's own card when rootClusterElementNodeData is seeded but view mode is box", async () => {
        useClusterElementsViewModeStore.setState({clusterElementsViewMode: 'box'});
        useWorkflowEditorStore.setState({
            rootClusterElementNodeData: {
                componentName: 'aiAgent',
                name: 'aiAgent_1',
                type: 'aiAgent/v1/chat',
                workflowNodeName: 'aiAgent_1',
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
            } as any,
        });

        const {result} = renderHook(() => useClusterElementNodes(CLUSTER_ROOT_IDS), {wrapper});

        await waitFor(() => {
            expect(result.current.definitionsReady).toBe(true);
        });

        expect(result.current.nodesByRootId.aiAgent_1.map((node) => node.id)).not.toContain('aiAgent_1');
    });

    it('returns no nodes at all for a root whose own definition has not resolved yet', () => {
        useWorkflowEditorStore.setState({clusterRootComponentDefinitions: {}});

        const {result} = renderHook(() => useClusterElementNodes(CLUSTER_ROOT_IDS), {wrapper});

        expect(result.current.nodesByRootId.aiAgent_1).toEqual([]);
    });

    it("fetches a root's own component definition itself when nothing has pre-seeded it, the main-canvas box path", async () => {
        useWorkflowEditorStore.setState({clusterRootComponentDefinitions: {}});

        const {result} = renderHook(() => useClusterElementNodes(CLUSTER_ROOT_IDS), {wrapper});

        await waitFor(() => {
            expect(result.current.definitionsReady).toBe(true);
        });

        expect(useWorkflowEditorStore.getState().clusterRootComponentDefinitions.aiAgent_1).toEqual(
            AI_AGENT_DEFINITION
        );
        expect(result.current.nodesByRootId.aiAgent_1.map((node) => node.id)).toContain('model_1');
    });

    it('is not ready when the shared nested-definitions map holds only an unrelated component', () => {
        useWorkflowEditorStore.setState({
            nestedClusterRootsComponentDefinitions: {
                unrelatedComponent: {
                    actionClusterElementTypes: {},
                    clusterElementClusterElementTypes: {},
                    clusterElementTypes: [],
                },
            },
        });

        const {result} = renderHook(() => useClusterElementNodes(CLUSTER_ROOT_IDS), {wrapper});

        expect(result.current.definitionsReady).toBe(false);
    });

    it('resolves nested definitions independently across multiple roots', async () => {
        useWorkflowDataStore.setState({
            workflow: {definition: WORKFLOW_DEFINITION_TWO_ROOTS, id: 'workflow_1'},
        } as Parameters<typeof useWorkflowDataStore.setState>[0]);
        useWorkflowEditorStore.setState({
            clusterRootComponentDefinitions: {aiAgent_1: AI_AGENT_DEFINITION, aiAgent_2: AI_AGENT_DEFINITION},
            nestedClusterRootsComponentDefinitions: {},
        });

        const {result} = renderHook(() => useClusterElementNodes(TWO_CLUSTER_ROOT_IDS), {wrapper});

        expect(result.current.definitionsReady).toBe(false);

        await waitFor(() => {
            expect(result.current.definitionsReady).toBe(true);
        });

        expect(result.current.nodesByRootId.aiAgent_1.map((node) => node.id)).toContain('model_1');
        expect(result.current.nodesByRootId.aiAgent_2.map((node) => node.id)).toContain('model_2');
    });

    it('returns the same nodesByRootId/edgesByRootId references across renders when no roots are requested', () => {
        const {rerender, result} = renderHook(() => useClusterElementNodes(EMPTY_ROOT_IDS), {wrapper});

        const firstNodesByRootId = result.current.nodesByRootId;
        const firstEdgesByRootId = result.current.edgesByRootId;

        act(() => {
            useWorkflowDataStore.setState({
                workflow: {definition: WORKFLOW_DEFINITION_TWO_ROOTS, id: 'workflow_1'},
            } as Parameters<typeof useWorkflowDataStore.setState>[0]);
        });

        rerender();

        expect(result.current.nodesByRootId).toBe(firstNodesByRootId);
        expect(result.current.edgesByRootId).toBe(firstEdgesByRootId);
    });

    it("fails open when one root's own definition fetch rejects: that root stays unboxed and everything else still settles", async () => {
        useWorkflowDataStore.setState({
            workflow: {definition: WORKFLOW_DEFINITION_WITH_BROKEN_ROOT, id: 'workflow_1'},
        } as Parameters<typeof useWorkflowDataStore.setState>[0]);
        useWorkflowEditorStore.setState({
            clusterRootComponentDefinitions: {},
            nestedClusterRootsComponentDefinitions: {},
        });

        const {result} = renderHook(() => useClusterElementNodes(HEALTHY_AND_BROKEN_ROOT_IDS), {wrapper});

        await waitFor(() => {
            expect(result.current.definitionsReady).toBe(true);
        });

        expect(useWorkflowEditorStore.getState().clusterRootComponentDefinitions.aiAgent_1).toEqual(
            AI_AGENT_DEFINITION
        );
        expect(useWorkflowEditorStore.getState().clusterRootComponentDefinitions.brokenAgent_1).toBeUndefined();
        expect(result.current.nodesByRootId.aiAgent_1.map((node) => node.id)).toContain('model_1');
        expect(result.current.nodesByRootId.brokenAgent_1).toEqual([]);
    });

    it("fails open when one nested element's definition fetch rejects: definitionsReady still settles and both roots still build", async () => {
        useWorkflowDataStore.setState({
            workflow: {definition: WORKFLOW_DEFINITION_WITH_BROKEN_NESTED_MODEL, id: 'workflow_1'},
        } as Parameters<typeof useWorkflowDataStore.setState>[0]);
        useWorkflowEditorStore.setState({
            clusterRootComponentDefinitions: {aiAgent_1: AI_AGENT_DEFINITION, aiAgent_2: AI_AGENT_DEFINITION},
            nestedClusterRootsComponentDefinitions: {},
        });

        const {result} = renderHook(() => useClusterElementNodes(TWO_CLUSTER_ROOT_IDS), {wrapper});

        await waitFor(() => {
            expect(result.current.definitionsReady).toBe(true);
        });

        expect(useWorkflowEditorStore.getState().nestedClusterRootsComponentDefinitions.openai).toBeDefined();
        expect(result.current.nodesByRootId.aiAgent_1.map((node) => node.id)).toContain('model_1');

        expect(useWorkflowEditorStore.getState().nestedClusterRootsComponentDefinitions.brokenModel).toBeUndefined();
        expect(result.current.nodesByRootId.aiAgent_2.map((node) => node.id)).toContain('model_2');
    });
});
