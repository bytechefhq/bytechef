import {WorkflowModel} from '@/middleware/automation/configuration';

/* eslint-disable sort-keys */
import {ComponentDefinitionBasicModel, TaskDispatcherDefinitionModel} from '@/middleware/platform/configuration';
import {ComponentActionType, ComponentType, DataPillType} from '@/types/types';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export type WorkflowTaskDataType = {
    actionNames?: Array<string>;
    componentNames: Array<string>;
    nodeNames: Array<string>;
};

interface WorkflowDataStateI {
    componentActions: Array<ComponentActionType>;
    setComponentActions: (componentActions: Array<ComponentActionType>) => void;

    components: Array<ComponentType>;
    setComponents: (components: Array<ComponentType>) => void;

    componentDefinitions: Array<ComponentDefinitionBasicModel>;
    setComponentDefinitions: (componentDefinitions: Array<ComponentDefinitionBasicModel>) => void;

    dataPills: Array<DataPillType>;
    setDataPills: (dataPills: Array<DataPillType>) => void;

    dirty: boolean;
    setDirty: (dirty: boolean) => void;

    projectId: number;
    setProjectId: (projectId: number) => void;

    taskDispatcherDefinitions: Array<TaskDispatcherDefinitionModel>;
    setTaskDispatcherDefinitions: (taskDispatcherDefinitions: Array<TaskDispatcherDefinitionModel>) => void;

    workflow: WorkflowModel & WorkflowTaskDataType;
    setWorkflow: (workflowDefinition: WorkflowModel & WorkflowTaskDataType) => void;
}

const useWorkflowDataStore = create<WorkflowDataStateI>()(
    devtools(
        (set) => ({
            componentActions: [],
            setComponentActions: (componentActions) => set((state) => ({...state, componentActions})),

            components: [],
            setComponents: (components) => set((state) => ({...state, components})),

            componentDefinitions: [],
            setComponentDefinitions: (componentDefinitions) => set((state) => ({...state, componentDefinitions})),

            dirty: false,
            setDirty: (dirty) => set((state) => ({...state, dirty})),

            dataPills: [],
            setDataPills: (dataPills) => set((state) => ({...state, dataPills})),

            projectId: 0,
            setProjectId: (projectId: number) => set((state) => ({...state, projectId})),

            taskDispatcherDefinitions: [],
            setTaskDispatcherDefinitions: (taskDispatcherDefinitions) =>
                set((state) => ({...state, taskDispatcherDefinitions})),

            workflow: {
                componentNames: [],
                nodeNames: [],
            },
            setWorkflow: (workflow) => set(() => ({workflow})),
        }),
        {name: 'workflow-data'}
    )
);

export default useWorkflowDataStore;
