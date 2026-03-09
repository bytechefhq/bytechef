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
        version?: number
    ) => void;
}

function updateClusterElementParameters(
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    clusterElements: Record<string, any>,
    workflowNodeName: string,
    parameters: Record<string, object>
): boolean {
    for (const elementValue of Object.values(clusterElements)) {
        if (!elementValue) {
            continue;
        }

        const elements = Array.isArray(elementValue) ? elementValue : [elementValue];

        for (const element of elements) {
            if (element.name === workflowNodeName) {
                element.parameters = parameters;

                return true;
            }

            if (element.clusterElements) {
                if (updateClusterElementParameters(element.clusterElements, workflowNodeName, parameters)) {
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
    parameters: Record<string, object>
): boolean {
    for (const task of tasks) {
        if (task.name === workflowNodeName) {
            task.parameters = parameters;

            return true;
        }

        if (task.clusterElements) {
            if (updateClusterElementParameters(task.clusterElements, workflowNodeName, parameters)) {
                return true;
            }
        }

        if (task.parameters) {
            let found = false;

            forEachNestedTaskGroup(task.parameters as Record<string, unknown>, (subtasks) => {
                if (!found && updateTaskParametersInTasks(subtasks, workflowNodeName, parameters)) {
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
            updateWorkflowNodeParameters: (workflowNodeName, parameters, version) =>
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

                    if (definition.tasks) {
                        updateTaskParametersInTasks(definition.tasks, workflowNodeName, parameters);
                    }

                    const updatedTasks = workflow.tasks?.map((task) =>
                        task.name === workflowNodeName ? {...task, parameters} : task
                    );

                    return {
                        ...state,
                        workflow: {
                            ...workflow,
                            definition: JSON.stringify(definition, null, SPACE),
                            tasks: updatedTasks,
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
