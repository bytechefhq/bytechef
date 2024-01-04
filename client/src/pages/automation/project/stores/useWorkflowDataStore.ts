/* eslint-disable sort-keys */
import {ComponentDefinitionBasicModel, TaskDispatcherDefinitionModel} from '@/middleware/hermes/configuration';
import {ComponentActionType, DataPillType} from '@/types/types';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowDefinitionState {
    componentDefinitions: Array<ComponentDefinitionBasicModel>;
    setComponentDefinitions: (componentDefinitions: Array<ComponentDefinitionBasicModel>) => void;

    componentNames: string[];
    setComponentNames: (componentNames: string[]) => void;

    componentActions: Array<ComponentActionType>;
    setComponentActions: (componentActions: Array<ComponentActionType>) => void;

    currentWorkflowId: string;
    setCurrentWorkflowId: (currentWorkflowId: string) => void;

    dataPills: Array<DataPillType>;
    setDataPills: (dataPills: Array<DataPillType>) => void;

    taskDispatcherDefinitions: Array<TaskDispatcherDefinitionModel>;
    setTaskDispatcherDefinitions: (taskDispatcherDefinitions: Array<TaskDispatcherDefinitionModel>) => void;
}

const useWorkflowDataStore = create<WorkflowDefinitionState>()(
    devtools(
        (set) => ({
            componentDefinitions: [],
            setComponentDefinitions: (componentDefinitions) => set((state) => ({...state, componentDefinitions})),

            componentNames: [],
            setComponentNames: (componentNames) => set((state) => ({...state, componentNames})),

            componentActions: [],
            setComponentActions: (componentActions) => set((state) => ({...state, componentActions})),

            currentWorkflowId: '',
            setCurrentWorkflowId: (currentWorkflowId) => set((state) => ({...state, currentWorkflowId})),

            dataPills: [],
            setDataPills: (dataPills) => set((state) => ({...state, dataPills})),

            taskDispatcherDefinitions: [],
            setTaskDispatcherDefinitions: (taskDispatcherDefinitions) =>
                set((state) => ({...state, taskDispatcherDefinitions})),
        }),
        {name: 'workflow-data'}
    )
);

export default useWorkflowDataStore;
