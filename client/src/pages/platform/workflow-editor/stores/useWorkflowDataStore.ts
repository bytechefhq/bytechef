/* eslint-disable sort-keys */
import {ComponentDefinitionBasic, TaskDispatcherDefinition, Workflow} from '@/shared/middleware/platform/configuration';
import {ComponentOperationType, DataPillType} from '@/shared/types';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export type WorkflowTaskDataType = {
    actionNames?: Array<string>;
    componentNames: Array<string>;
    nodeNames: Array<string>;
};

interface WorkflowDataStateI {
    componentActions: Array<ComponentOperationType>;
    setComponentActions: (componentActions: Array<ComponentOperationType>) => void;

    componentDefinitions: Array<ComponentDefinitionBasic>;
    setComponentDefinitions: (componentDefinitions: Array<ComponentDefinitionBasic>) => void;

    dataPills: Array<DataPillType>;
    setDataPills: (dataPills: Array<DataPillType>) => void;

    latestComponentDefinition: ComponentDefinitionBasic | null;
    setLatestComponentDefinition: (latestComponentDefinition: ComponentDefinitionBasic | null) => void;

    reset: () => void;

    taskDispatcherDefinitions: Array<TaskDispatcherDefinition>;
    setTaskDispatcherDefinitions: (taskDispatcherDefinitions: Array<TaskDispatcherDefinition>) => void;

    workflow: Workflow & WorkflowTaskDataType;
    setWorkflow: (workflowDefinition: Workflow & WorkflowTaskDataType) => void;
}

const useWorkflowDataStore = create<WorkflowDataStateI>()(
    devtools(
        (set) => ({
            componentActions: [],
            setComponentActions: (componentActions) => set((state) => ({...state, componentActions})),

            componentDefinitions: [],
            setComponentDefinitions: (componentDefinitions) => set((state) => ({...state, componentDefinitions})),

            dataPills: [],
            setDataPills: (dataPills) => set((state) => ({...state, dataPills})),

            latestComponentDefinition: null,
            setLatestComponentDefinition: (latestComponentDefinition) =>
                set((state) => ({...state, latestComponentDefinition})),

            reset: () =>
                set(() => ({
                    componentActions: [],
                    dataPills: [],
                    workflow: {
                        actionNames: [],
                        componentNames: [],
                        nodeNames: ['trigger_1'],
                    },
                })),

            taskDispatcherDefinitions: [],
            setTaskDispatcherDefinitions: (taskDispatcherDefinitions) =>
                set((state) => ({...state, taskDispatcherDefinitions})),

            workflow: {
                componentNames: [],
                nodeNames: ['trigger_1'],
            },
            setWorkflow: (workflow) => set(() => ({workflow})),
        }),
        {name: 'workflow-data'}
    )
);

export default useWorkflowDataStore;
