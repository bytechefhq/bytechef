/* eslint-disable sort-keys */
import {DEFAULT_CANVAS_WIDTH, SPACE} from '@/shared/constants';
import {ComponentDefinitionBasic, TaskDispatcherDefinition, Workflow} from '@/shared/middleware/platform/configuration';
import {DataPillType, WorkflowNodeType} from '@/shared/types';
import {Edge, Node, OnEdgesChange, OnNodesChange, applyEdgeChanges, applyNodeChanges} from '@xyflow/react';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

import {createDefaultEdges, createDefaultNodes} from '../utils/layoutUtils';
import {forEachNestedTaskGroup} from '../utils/taskTraversalUtils';

export type WorkflowDataType = {
    actionNames?: Array<string>;
    nodeNames: Array<string>;
    workflowUuid?: string;
};
interface WorkflowDataStateI {
    workflowNodes: Array<WorkflowNodeType>;

    componentDefinitions: Array<ComponentDefinitionBasic>;
    setComponentDefinitions: (componentDefinitions: Array<ComponentDefinitionBasic>) => void;

    dataPills: Array<DataPillType>;
    setDataPills: (dataPills: Array<DataPillType>) => void;

    edges: Edge[];
    setEdges: (edges: Edge[]) => void;
    onEdgesChange: OnEdgesChange;

    isNodeDragging: boolean;
    setIsNodeDragging: (dragging: boolean) => void;

    savedPositionCrossAxisShift: number;
    setSavedPositionCrossAxisShift: (shift: number) => void;

    isWorkflowLoaded: boolean;
    setIsWorkflowLoaded: (loaded: boolean) => void;

    layoutResetCounter: number;
    incrementLayoutResetCounter: () => void;

    latestComponentDefinition: ComponentDefinitionBasic | null;
    setLatestComponentDefinition: (latestComponentDefinition: ComponentDefinitionBasic | null) => void;

    nodes: Node[];
    setNodes: (nodes: Node[]) => void;
    onNodesChange: OnNodesChange;

    reset: () => void;
    initializeWithCanvasWidth: (canvasWidth: number) => void;

    taskDispatcherDefinitions: Array<TaskDispatcherDefinition>;
    setTaskDispatcherDefinitions: (taskDispatcherDefinitions: Array<TaskDispatcherDefinition>) => void;

    workflow: Workflow & WorkflowDataType;
    setWorkflow: (workflow: Workflow) => void;
    updateWorkflowNodeParameters: (
        workflowNodeName: string,
        parameters: Record<string, object>,
        version?: number,
        metadata?: Record<string, unknown>
    ) => void;
}

function updateClusterElementParameters(
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    clusterElements: Record<string, any>,
    workflowNodeName: string,
    parameters: Record<string, object>,
    metadata?: Record<string, unknown>
): boolean {
    for (const elementValue of Object.values(clusterElements)) {
        if (!elementValue) {
            continue;
        }

        const elements = Array.isArray(elementValue) ? elementValue : [elementValue];

        for (const element of elements) {
            if (element.name === workflowNodeName) {
                element.parameters = parameters;

                if (metadata !== undefined) {
                    element.metadata = metadata;
                }

                return true;
            }

            if (element.clusterElements) {
                if (updateClusterElementParameters(element.clusterElements, workflowNodeName, parameters, metadata)) {
                    return true;
                }
            }
        }
    }

    return false;
}

function updateTaskParametersInTasks(
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    tasks: any[],
    workflowNodeName: string,
    parameters: Record<string, object>,
    metadata?: Record<string, unknown>
): boolean {
    for (const task of tasks) {
        if (task.name === workflowNodeName) {
            task.parameters = parameters;

            if (metadata !== undefined) {
                task.metadata = metadata;
            }

            return true;
        }

        if (task.clusterElements) {
            if (updateClusterElementParameters(task.clusterElements, workflowNodeName, parameters, metadata)) {
                return true;
            }
        }

        if (task.parameters) {
            let found = false;

            forEachNestedTaskGroup(task.parameters as Record<string, unknown>, (subtasks) => {
                if (!found && updateTaskParametersInTasks(subtasks, workflowNodeName, parameters, metadata)) {
                    found = true;
                }
            });

            if (found) {
                return true;
            }
        }
    }

    return false;
}

const useWorkflowDataStore = create<WorkflowDataStateI>()(
    devtools(
        (set, get) => ({
            componentActions: [],

            componentDefinitions: [],
            setComponentDefinitions: (componentDefinitions) => set((state) => ({...state, componentDefinitions})),

            dataPills: [],
            setDataPills: (dataPills) => set((state) => ({...state, dataPills})),

            edges: createDefaultEdges(),
            setEdges: (edges) => {
                set({edges});
            },
            onEdgesChange: (changes) => {
                set({
                    edges: applyEdgeChanges(changes, get().edges),
                });
            },

            isNodeDragging: false,
            setIsNodeDragging: (dragging) => set({isNodeDragging: dragging}),

            savedPositionCrossAxisShift: 0,
            setSavedPositionCrossAxisShift: (shift) => set({savedPositionCrossAxisShift: shift}),

            isWorkflowLoaded: false,
            setIsWorkflowLoaded: (loaded) => set({isWorkflowLoaded: loaded}),

            layoutResetCounter: 0,
            incrementLayoutResetCounter: () => set((state) => ({layoutResetCounter: state.layoutResetCounter + 1})),

            latestComponentDefinition: null,
            setLatestComponentDefinition: (latestComponentDefinition) =>
                set((state) => ({...state, latestComponentDefinition})),

            nodes: createDefaultNodes(DEFAULT_CANVAS_WIDTH),
            setNodes: (nodes) => {
                set({nodes});
            },
            onNodesChange: (changes) => {
                set({
                    nodes: applyNodeChanges(changes, get().nodes),
                });
            },

            reset: () =>
                set(() => ({
                    workflowNodes: [],
                    dataPills: [],
                    edges: createDefaultEdges(),
                    isWorkflowLoaded: false,
                    nodes: createDefaultNodes(DEFAULT_CANVAS_WIDTH),
                    workflow: {
                        actionNames: [],
                        nodeNames: ['trigger_1'],
                    },
                })),

            initializeWithCanvasWidth: (canvasWidth: number) =>
                set(() => ({
                    nodes: createDefaultNodes(canvasWidth),
                })),

            taskDispatcherDefinitions: [],
            setTaskDispatcherDefinitions: (taskDispatcherDefinitions) =>
                set((state) => ({...state, taskDispatcherDefinitions})),

            workflow: {
                nodeNames: ['trigger_1'],
            },
            updateWorkflowNodeParameters: (workflowNodeName, parameters, version, metadata) =>
                set((state) => {
                    const workflow = state.workflow;

                    if (!workflow.definition) {
                        return state;
                    }

                    let definition;

                    try {
                        definition = JSON.parse(workflow.definition);
                    } catch (error) {
                        console.error('Failed to parse workflow definition:', error);

                        return state;
                    }

                    if (definition.triggers) {
                        for (const trigger of definition.triggers) {
                            if (trigger.name === workflowNodeName) {
                                trigger.parameters = parameters;

                                if (metadata !== undefined) {
                                    trigger.metadata = metadata;
                                }

                                break;
                            }
                        }
                    }

                    if (definition.tasks) {
                        updateTaskParametersInTasks(definition.tasks, workflowNodeName, parameters, metadata);
                    }

                    const updatedTriggers = workflow.triggers?.map((trigger) =>
                        trigger.name === workflowNodeName
                            ? {...trigger, parameters, ...(metadata !== undefined ? {metadata} : {})}
                            : trigger
                    );

                    const updatedTasks = workflow.tasks?.map((task) =>
                        task.name === workflowNodeName
                            ? {...task, parameters, ...(metadata !== undefined ? {metadata} : {})}
                            : task
                    );

                    // Keep React Flow node data in sync. useLayout skips re-runs on parameter-only
                    // changes (structural fingerprint), so without this patch nodes[i].data.parameters
                    // stays frozen at the first layout. Clicking a node later then seeds the details
                    // panel from stale data and the user loses recent edits until the next refresh.
                    const updatedNodes = state.nodes.map((node) =>
                        node.id === workflowNodeName
                            ? {
                                  ...node,
                                  data: {
                                      ...node.data,
                                      parameters,
                                      ...(metadata !== undefined ? {metadata} : {}),
                                  },
                              }
                            : node
                    );

                    return {
                        ...state,
                        nodes: updatedNodes,
                        workflow: {
                            ...workflow,
                            definition: JSON.stringify(definition, null, SPACE),
                            tasks: updatedTasks,
                            triggers: updatedTriggers,
                            version: version ?? workflow.version,
                        },
                    };
                }),
            setWorkflow: (workflow) =>
                set((state) => {
                    const workflowNodes: Array<{name: string; type: string}> = [
                        workflow.triggers?.[0] || (createDefaultNodes(1200)[0].data as {name: string; type: string}),
                        ...(workflow?.tasks || []),
                    ];

                    return {
                        ...state,
                        isWorkflowLoaded: true,
                        workflowNodes: workflowNodes.map((workflowNode) => {
                            const name = workflowNode.type!.split('/')[0];
                            const version = +workflowNode.type!.split('/')[1].replace('v', '');
                            const operationName = workflowNode.type!.split('/')[2];

                            return {
                                name,
                                operationName,
                                version,
                                workflowNodeName: workflowNode.name,
                            };
                        }),
                        workflow: {
                            ...workflow,
                            nodeNames: workflowNodes.map((workflowNode) => workflowNode.name),
                            __lastUpdated: Date.now(),
                        },
                    };
                }),
        }),
        {name: 'workflow-data'}
    )
);

export default useWorkflowDataStore;
