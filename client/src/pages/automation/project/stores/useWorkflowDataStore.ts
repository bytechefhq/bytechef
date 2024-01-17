import {WorkflowModel} from '@/middleware/automation/configuration';

/* eslint-disable sort-keys */
import {ComponentDefinitionBasicModel, TaskDispatcherDefinitionModel} from '@/middleware/platform/configuration';
import {ComponentActionType, ComponentDataType, DataPillType} from '@/types/types';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowDataState {
    componentActions: Array<ComponentActionType>;
    setComponentActions: (componentActions: Array<ComponentActionType>) => void;

    componentData: Array<ComponentDataType>;
    setComponentData: (componentData: Array<ComponentDataType>) => void;

    componentDefinitions: Array<ComponentDefinitionBasicModel>;
    setComponentDefinitions: (componentDefinitions: Array<ComponentDefinitionBasicModel>) => void;

    componentNames: Array<string>;
    setComponentNames: (componentNames: Array<string>) => void;

    dataPills: Array<DataPillType>;
    setDataPills: (dataPills: Array<DataPillType>) => void;

    nodeNames: Array<string>;
    setNodeNames: (nodeNames: Array<string>) => void;

    projectId: number;
    setProjectId: (projectId: number) => void;

    taskDispatcherDefinitions: Array<TaskDispatcherDefinitionModel>;
    setTaskDispatcherDefinitions: (taskDispatcherDefinitions: Array<TaskDispatcherDefinitionModel>) => void;

    workflow: WorkflowModel;
    setWorkflow: (workflowDefinition: WorkflowModel) => void;
}

const useWorkflowDataStore = create<WorkflowDataState>()(
    devtools(
        (set) => ({
            componentActions: [],
            setComponentActions: (componentActions) => set((state) => ({...state, componentActions})),

            componentData: [],
            setComponentData: (componentData) => set(() => ({componentData})),

            componentDefinitions: [],
            setComponentDefinitions: (componentDefinitions) => set((state) => ({...state, componentDefinitions})),

            componentNames: [],
            setComponentNames: (componentNames) => set((state) => ({...state, componentNames})),

            dataPills: [],
            setDataPills: (dataPills) => set((state) => ({...state, dataPills})),

            nodeNames: [],
            setNodeNames: (nodeNames) => set((state) => ({...state, nodeNames})),

            projectId: 0,
            setProjectId: (projectId: number) => set((state) => ({...state, projectId})),

            taskDispatcherDefinitions: [],
            setTaskDispatcherDefinitions: (taskDispatcherDefinitions) =>
                set((state) => ({...state, taskDispatcherDefinitions})),

            workflow: {},
            setWorkflow: (workflow) => set(() => ({workflow})),
        }),
        {name: 'workflow-data'}
    )
);

export default useWorkflowDataStore;
