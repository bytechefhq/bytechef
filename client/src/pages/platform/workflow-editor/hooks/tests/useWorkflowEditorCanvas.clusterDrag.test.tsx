import {act, renderHook} from '@testing-library/react';
import {Node, ReactFlowProvider} from '@xyflow/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import {
    fromClusterFrameChildPosition,
    toClusterFrameChildPosition,
} from '../../utils/clusterFrame/clusterFrameGeometry';
import {clearAllWorkflowMutations} from '../../utils/workflowMutationGuard';
import useWorkflowEditorCanvas from '../useWorkflowEditorCanvas';

const {editorContext, updateWorkflowMutationMock} = vi.hoisted(() => {
    const mutation = {isPending: false, mutate: vi.fn()};

    return {
        editorContext: {updateWorkflowMutation: mutation as typeof mutation | undefined},
        updateWorkflowMutationMock: mutation,
    };
});

vi.mock('../useLayout', () => ({
    default: () => ({autoPlacedGraphPositionsRef: {current: {}}}),
}));

vi.mock('../useHandleDrop', () => ({
    default: () => [vi.fn(), vi.fn(), vi.fn(), vi.fn()],
}));

vi.mock('../useStickyNotes', () => ({
    default: () => ({handleAddStickyNote: vi.fn()}),
}));

vi.mock('../../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => editorContext,
}));

const ROOT_ID = 'aiAgent_1';

function buildDefinition(elementPosition: {x: number; y: number}): string {
    return JSON.stringify({
        tasks: [
            {
                clusterElements: {
                    model: {
                        metadata: {ui: {nodePosition: elementPosition}},
                        name: 'model_1',
                        type: 'openAiChat/v1',
                    },
                },
                name: ROOT_ID,
                type: 'aiAgent/v1',
            },
        ],
    });
}

function buildCanvasNodes(elementContentPosition: {x: number; y: number}): Node[] {
    return [
        {
            data: {
                clusterFrame: {clusterRootId: ROOT_ID, height: 320, width: 640},
                clusterRoot: true,
                workflowNodeName: ROOT_ID,
            },
            id: ROOT_ID,
            position: {x: 0, y: 0},
            type: 'workflow',
        },
        {
            data: {clusterElementType: 'model', parentClusterRootId: ROOT_ID, workflowNodeName: 'model_1'},
            draggable: true,
            id: 'model_1',
            measured: {height: 60, width: 200},
            parentId: ROOT_ID,
            position: toClusterFrameChildPosition(elementContentPosition),
            type: 'workflow',
        },
    ];
}

function renderCanvas() {
    return renderHook(() => useWorkflowEditorCanvas({componentDefinitions: [], taskDispatcherDefinitions: []}), {
        wrapper: ({children}: {children: ReactNode}) => <ReactFlowProvider>{children}</ReactFlowProvider>,
    });
}

function findNode(id: string): Node {
    return useWorkflowDataStore.getState().nodes.find((node) => node.id === id)!;
}

function getSavedElementPosition(): {x: number; y: number} | undefined {
    const [[mutationVariables]] = updateWorkflowMutationMock.mutate.mock.calls.slice(-1);

    const savedTasks: Array<{
        clusterElements: {model: {metadata?: {ui?: {nodePosition?: {x: number; y: number}}}}};
    }> = JSON.parse((mutationVariables as {workflow: {definition: string}}).workflow.definition).tasks;

    return savedTasks[0].clusterElements.model.metadata?.ui?.nodePosition;
}

describe('useWorkflowEditorCanvas cluster element dragging', () => {
    beforeEach(() => {
        editorContext.updateWorkflowMutation = updateWorkflowMutationMock;
        updateWorkflowMutationMock.mutate.mockClear();

        clearAllWorkflowMutations();

        useWorkflowDataStore.setState({
            edges: [],
            nodes: buildCanvasNodes({x: 0, y: 0}),
            savedPositionCrossAxisShift: 0,
            workflow: {
                definition: buildDefinition({x: 0, y: 0}),
                id: 'workflow-1',
                nodeNames: [],
                tasks: [{clusterRoot: true, name: ROOT_ID, type: 'aiAgent/v1'}],
                version: 3,
            },
        });
    });

    it('converts a dropped element back through the header offset before persisting it', () => {
        const {result} = renderCanvas();

        act(() => {
            result.current.handleNodeDragStop({} as MouseEvent, {
                ...findNode('model_1'),
                position: toClusterFrameChildPosition({x: 220, y: 140}),
            });
        });

        expect(getSavedElementPosition()).toEqual({x: 220, y: 140});
    });

    it('does not persist the raw parent-relative position unconverted', () => {
        const {result} = renderCanvas();

        const droppedPosition = toClusterFrameChildPosition({x: 220, y: 140});

        act(() => {
            result.current.handleNodeDragStop({} as MouseEvent, {
                ...findNode('model_1'),
                position: droppedPosition,
            });
        });

        expect(getSavedElementPosition()).not.toEqual(droppedPosition);
        expect(getSavedElementPosition()).toEqual(fromClusterFrameChildPosition(droppedPosition));
    });

    it('does nothing when there is no mutation to persist through', () => {
        editorContext.updateWorkflowMutation = undefined;

        const {result} = renderCanvas();

        act(() => {
            result.current.handleNodeDragStop({} as MouseEvent, {
                ...findNode('model_1'),
                position: toClusterFrameChildPosition({x: 220, y: 140}),
            });
        });

        expect(updateWorkflowMutationMock.mutate).not.toHaveBeenCalled();
    });

    it('does nothing when the cluster root task carries no cluster elements', () => {
        useWorkflowDataStore.setState({
            workflow: {
                definition: JSON.stringify({tasks: [{name: ROOT_ID, type: 'aiAgent/v1'}]}),
                id: 'workflow-1',
                nodeNames: [],
                version: 3,
            },
        });

        const {result} = renderCanvas();

        act(() => {
            result.current.handleNodeDragStop({} as MouseEvent, {
                ...findNode('model_1'),
                position: toClusterFrameChildPosition({x: 220, y: 140}),
            });
        });

        expect(updateWorkflowMutationMock.mutate).not.toHaveBeenCalled();
    });
});

describe('useWorkflowEditorCanvas cluster element dragging, nested a level deeper', () => {
    const NESTED_ROOT_ID = 'agenticTool_1';
    const NESTED_ELEMENT_ID = 'nestedModel_1';

    function buildNestedDefinition(elementPosition: {x: number; y: number}): string {
        return JSON.stringify({
            tasks: [
                {
                    clusterElements: {
                        tools: [
                            {
                                clusterElements: {
                                    model: {
                                        metadata: {ui: {nodePosition: elementPosition}},
                                        name: NESTED_ELEMENT_ID,
                                        type: 'openAiChat/v1',
                                    },
                                },
                                clusterRoot: true,
                                name: NESTED_ROOT_ID,
                                type: 'agenticTool/v1',
                            },
                        ],
                    },
                    name: ROOT_ID,
                    type: 'aiAgent/v1',
                },
            ],
        });
    }

    function buildNestedElementNode(elementContentPosition: {x: number; y: number}): Node {
        return {
            data: {
                clusterElementType: 'model',
                parentClusterRootId: NESTED_ROOT_ID,
                topLevelClusterRootId: ROOT_ID,
                workflowNodeName: NESTED_ELEMENT_ID,
            },
            draggable: true,
            id: NESTED_ELEMENT_ID,
            measured: {height: 60, width: 200},
            parentId: NESTED_ROOT_ID,
            position: elementContentPosition,
            type: 'workflow',
        };
    }

    function getSavedNestedElementPosition(): {x: number; y: number} | undefined {
        const [[mutationVariables]] = updateWorkflowMutationMock.mutate.mock.calls.slice(-1);

        const savedTasks: Array<{
            clusterElements: {
                tools: Array<{clusterElements: {model: {metadata?: {ui?: {nodePosition?: {x: number; y: number}}}}}}>;
            };
        }> = JSON.parse((mutationVariables as {workflow: {definition: string}}).workflow.definition).tasks;

        return savedTasks[0].clusterElements.tools[0].clusterElements.model.metadata?.ui?.nodePosition;
    }

    beforeEach(() => {
        editorContext.updateWorkflowMutation = updateWorkflowMutationMock;
        updateWorkflowMutationMock.mutate.mockClear();
        clearAllWorkflowMutations();

        useWorkflowDataStore.setState({
            edges: [],
            nodes: [buildNestedElementNode({x: 0, y: 0})],
            savedPositionCrossAxisShift: 0,
            workflow: {
                definition: buildNestedDefinition({x: 0, y: 0}),
                id: 'workflow-1',
                nodeNames: [],
                tasks: [{clusterRoot: true, name: ROOT_ID, type: 'aiAgent/v1'}],
                version: 3,
            },
        });
    });

    it('resolves through the outermost root rather than the immediate (nested) parent', () => {
        const {result} = renderCanvas();

        act(() => {
            result.current.handleNodeDragStop({} as MouseEvent, {
                ...findNode(NESTED_ELEMENT_ID),
                position: {x: 220, y: 180},
            });
        });

        expect(updateWorkflowMutationMock.mutate).toHaveBeenCalledTimes(1);

        expect(getSavedNestedElementPosition()).toEqual({x: 220, y: 180});
    });
});

describe('useWorkflowEditorCanvas cluster element dragging inside a trigger box', () => {
    const TRIGGER_ID = 'trigger_1';

    beforeEach(() => {
        editorContext.updateWorkflowMutation = updateWorkflowMutationMock;
        updateWorkflowMutationMock.mutate.mockClear();

        clearAllWorkflowMutations();

        useWorkflowDataStore.setState({
            edges: [],
            nodes: [
                {
                    data: {
                        clusterFrame: {clusterRootId: TRIGGER_ID, height: 320, width: 640},
                        clusterRoot: true,
                        trigger: true,
                        workflowNodeName: TRIGGER_ID,
                    },
                    id: TRIGGER_ID,
                    position: {x: 0, y: 0},
                    type: 'workflow',
                },
                {
                    data: {
                        clusterElementType: 'voiceAgent',
                        parentClusterRootId: TRIGGER_ID,
                        workflowNodeName: 'voiceAgent_1',
                    },
                    draggable: true,
                    id: 'voiceAgent_1',
                    measured: {height: 60, width: 200},
                    parentId: TRIGGER_ID,
                    position: toClusterFrameChildPosition({x: 0, y: 0}),
                    type: 'workflow',
                },
            ],
            savedPositionCrossAxisShift: 0,
            workflow: {
                definition: JSON.stringify({
                    tasks: [],
                    triggers: [
                        {
                            clusterElements: {
                                tools: [],
                                voiceAgent: {
                                    metadata: {ui: {nodePosition: {x: 0, y: 0}}},
                                    name: 'voiceAgent_1',
                                    type: 'deepgram/v1/voiceAgent',
                                },
                            },
                            name: TRIGGER_ID,
                            type: 'browser/v1/voiceSession',
                        },
                    ],
                }),
                id: 'workflow-1',
                nodeNames: [],
                tasks: [],
                triggers: [{clusterRoot: true, name: TRIGGER_ID, type: 'browser/v1/voiceSession'}],
                version: 3,
            },
        });
    });

    it('persists a dropped voice agent position into the trigger it hangs off', () => {
        const {result} = renderCanvas();

        act(() => {
            result.current.handleNodeDragStop({} as MouseEvent, {
                ...findNode('voiceAgent_1'),
                position: toClusterFrameChildPosition({x: 220, y: 140}),
            });
        });

        expect(updateWorkflowMutationMock.mutate).toHaveBeenCalled();

        const [[mutationVariables]] = updateWorkflowMutationMock.mutate.mock.calls.slice(-1);

        const savedDefinition = JSON.parse((mutationVariables as {workflow: {definition: string}}).workflow.definition);

        expect(savedDefinition.tasks).toEqual([]);
        expect(savedDefinition.triggers[0].clusterElements.voiceAgent.metadata.ui.nodePosition).toEqual({
            x: 220,
            y: 140,
        });
    });
});
