import useClusterElementNodes from '@/pages/platform/cluster-element-editor/hooks/useClusterElementNodes';
import useClusterElementsViewModeStore from '@/pages/platform/workflow-editor/stores/useClusterElementsViewModeStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {Node} from '@xyflow/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {CLUSTER_FRAME_HEADER_HEIGHT, getClusterMemberSize} from './clusterFrameGeometry';
import {layoutClusterFrames} from './layoutClusterFrames';

const {AI_AGENT_DEFINITION, SUB_AGENT_DEFINITION} = vi.hoisted(() => {
    const AI_AGENT_DEFINITION: ComponentDefinition = {
        clusterElement: false,
        clusterElementTypes: [
            {label: 'Model', multipleElements: false, name: 'MODEL'},
            {label: 'Memory', multipleElements: false, name: 'MEMORY'},
            {label: 'Tools', multipleElements: true, name: 'TOOLS'},
        ],
        clusterRoot: true,
        connectionRequired: false,
        name: 'aiAgent',
        version: 1,
    };

    const SUB_AGENT_DEFINITION = {
        clusterElementTypes: [{label: 'Model', multipleElements: false, name: 'MODEL'}],
        name: 'subAgent',
        version: 1,
    };

    return {AI_AGENT_DEFINITION, SUB_AGENT_DEFINITION};
});

vi.mock('@/shared/middleware/platform/configuration', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/platform/configuration')>()),
    ComponentDefinitionApi: class {
        getComponentDefinition({componentName}: {componentName: string}) {
            if (componentName === 'aiAgent') {
                return Promise.resolve(AI_AGENT_DEFINITION);
            }

            if (componentName === 'subAgent') {
                return Promise.resolve(SUB_AGENT_DEFINITION);
            }

            return Promise.resolve({clusterElementTypes: [], name: componentName, version: 1});
        }
    },
}));

const WORKFLOW_DEFINITION = JSON.stringify({
    tasks: [
        {
            clusterElements: {
                memory: {label: 'Memory', name: 'memory_1', type: 'redis/v1/memory'},
                model: {label: 'GPT', name: 'model_1', type: 'openai/v1/model'},
                tools: [
                    {label: 'Slack', name: 'tool_1', type: 'slack/v1/sendMessage'},
                    {label: 'Jira', name: 'tool_2', type: 'jira/v1/createIssue'},
                    {
                        clusterElements: {
                            model: {label: 'Claude', name: 'nested_model_1', type: 'anthropic/v1/model'},
                        },
                        label: 'Sub agent',
                        name: 'sub_agent_1',
                        type: 'subAgent/v1/tools',
                    },
                ],
            },
            label: 'AI Agent',
            name: 'aiAgent_1',
            type: 'aiAgent/v1/chat',
        },
    ],
});

const CLUSTER_ROOT_ID = 'aiAgent_1';
const CLUSTER_ROOT_IDS = [CLUSTER_ROOT_ID];

function buildClusterRootCanvasNode(): Node {
    return {
        data: {
            clusterElements: JSON.parse(WORKFLOW_DEFINITION).tasks[0].clusterElements,
            clusterRoot: true,
            componentName: 'aiAgent',
            label: 'AI Agent',
            name: CLUSTER_ROOT_ID,
            workflowNodeName: CLUSTER_ROOT_ID,
        },
        id: CLUSTER_ROOT_ID,
        position: {x: 0, y: 0},
        type: 'clusterRoot',
    };
}

describe('cluster frame, from a workflow definition', () => {
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
            clusterRootComponentDefinitions: {},
            nestedClusterRootsComponentDefinitions: {},
            rootClusterElementNodeData: undefined,
        });
        useClusterElementsViewModeStore.setState({clusterElementsViewMode: 'box'});
    });

    async function buildBox() {
        const {result} = renderHook(() => useClusterElementNodes(CLUSTER_ROOT_IDS), {wrapper});

        await waitFor(() => {
            expect(result.current.definitionsReady).toBe(true);
        });

        await waitFor(() => {
            expect(result.current.nodesByRootId[CLUSTER_ROOT_ID].length).toBeGreaterThan(0);
        });

        const framed = layoutClusterFrames(
            [buildClusterRootCanvasNode()],
            [],
            {edgesByRootId: result.current.edgesByRootId, nodesByRootId: result.current.nodesByRootId},
            {}
        );

        const rootNode = framed.outerNodes.find((node) => node.id === CLUSTER_ROOT_ID)!;

        return {frame: (rootNode.data as {clusterFrame: {height: number; width: number}}).clusterFrame, framed};
    }

    it('places every member at a distinct position, none left stacked at the origin', async () => {
        const {framed} = await buildBox();

        const elementMembers = framed.memberNodes.filter((node) => node.type !== 'placeholder');

        expect(elementMembers.length).toBeGreaterThan(3);

        const positionKeys = elementMembers.map((node) => `${node.position.x},${node.position.y}`);

        expect(new Set(positionKeys).size).toBe(positionKeys.length);

        const directMembers = elementMembers.filter((node) => node.parentId === CLUSTER_ROOT_ID);

        expect(directMembers.every((node) => node.position.y > CLUSTER_FRAME_HEADER_HEIGHT)).toBe(true);
    });

    it('sizes the frame so that every member fits inside it', async () => {
        const {frame, framed} = await buildBox();

        const framePositions = new Map<string, {x: number; y: number}>();

        for (const memberNode of framed.memberNodes) {
            const parentPosition =
                memberNode.parentId === CLUSTER_ROOT_ID
                    ? {x: 0, y: 0}
                    : (framePositions.get(memberNode.parentId ?? '') ?? {x: 0, y: 0});

            framePositions.set(memberNode.id, {
                x: parentPosition.x + memberNode.position.x,
                y: parentPosition.y + memberNode.position.y,
            });
        }

        for (const memberNode of framed.memberNodes) {
            const position = framePositions.get(memberNode.id)!;
            const size = getClusterMemberSize(memberNode);

            expect(position.x).toBeGreaterThanOrEqual(0);
            expect(position.y).toBeGreaterThanOrEqual(0);
            expect(position.x + size.width).toBeLessThanOrEqual(frame.width);
            expect(position.y + size.height).toBeLessThanOrEqual(frame.height);
        }
    });

    it('leaves nested members parented to their nested root, not to the box root', async () => {
        const {framed} = await buildBox();

        const nestedMember = framed.memberNodes.find((node) => node.id === 'nested_model_1')!;

        expect(nestedMember).toBeDefined();
        expect(nestedMember.parentId).toBe('sub_agent_1');

        expect(nestedMember.position.y).toBeLessThan(
            framed.memberNodes.find((node) => node.id === 'sub_agent_1')!.position.y
        );
    });
});
