import defaultEdges from '@/shared/defaultEdges';
import defaultNodes from '@/shared/defaultNodes';

/* eslint-disable sort-keys */
import {ComponentDefinitionBasic, TaskDispatcherDefinition, Workflow} from '@/shared/middleware/platform/configuration';
import {DataPillType, WorkflowNodeType} from '@/shared/types';
import {Edge, Node, OnEdgesChange, OnNodesChange, applyEdgeChanges, applyNodeChanges} from '@xyflow/react';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

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

    latestComponentDefinition: ComponentDefinitionBasic | null;
    setLatestComponentDefinition: (latestComponentDefinition: ComponentDefinitionBasic | null) => void;

    nodes: Node[];
    setNodes: (nodes: Node[]) => void;
    onNodesChange: OnNodesChange;

    reset: () => void;

    taskDispatcherDefinitions: Array<TaskDispatcherDefinition>;
    setTaskDispatcherDefinitions: (taskDispatcherDefinitions: Array<TaskDispatcherDefinition>) => void;

    workflow: Workflow & WorkflowDataType;
    setWorkflow: (workflow: Workflow) => void;
}

const useWorkflowDataStore = create<WorkflowDataStateI>()(
    devtools(
        (set, get) => ({
            componentActions: [],

            componentDefinitions: [],
            setComponentDefinitions: (componentDefinitions) => set((state) => ({...state, componentDefinitions})),

            dataPills: [],
            setDataPills: (dataPills) => set((state) => ({...state, dataPills})),

            edges: defaultEdges,
            setEdges: (edges) => {
                set({edges});
            },
            onEdgesChange: (changes) => {
                set({
                    edges: applyEdgeChanges(changes, get().edges),
                });
            },

            latestComponentDefinition: null,
            setLatestComponentDefinition: (latestComponentDefinition) =>
                set((state) => ({...state, latestComponentDefinition})),

            nodes: defaultNodes,
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
                    workflow: {
                        actionNames: [],
                        nodeNames: ['trigger_1'],
                    },
                })),

            taskDispatcherDefinitions: [],
            setTaskDispatcherDefinitions: (taskDispatcherDefinitions) =>
                set((state) => ({...state, taskDispatcherDefinitions})),

            workflow: {
                nodeNames: ['trigger_1'],
            },
            setWorkflow: (workflow) =>
                set((state) => {
                    const workflowNodes: Array<{name: string; type: string}> = [
                        workflow.triggers?.[0] || (defaultNodes[0].data as {name: string; type: string}),
                        ...(workflow?.tasks || []),
                    ];

                    return {
                        ...state,
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
                        },
                    };
                }),
        }),
        {name: 'workflow-data'}
    )
);

export default useWorkflowDataStore;
