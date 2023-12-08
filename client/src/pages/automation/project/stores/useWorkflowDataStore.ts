/* eslint-disable sort-keys */
import {ComponentDefinitionBasicModel, TaskDispatcherDefinitionModel} from '@/middleware/hermes/configuration';
import {DataPillType} from '@/types/types';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

type ComponentActionsType = Array<{
    componentName: string;
    actionName: string;
    workflowAlias?: string;
}>;

interface WorkflowDefinitionState {
    componentDefinitions: Array<ComponentDefinitionBasicModel>;
    setComponentDefinitions: (componentDefinitions: Array<ComponentDefinitionBasicModel>) => void;

    componentNames: string[];
    setComponentNames: (componentNames: string[]) => void;

    componentActions: ComponentActionsType;
    setComponentActions: (componentActions: ComponentActionsType) => void;

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
