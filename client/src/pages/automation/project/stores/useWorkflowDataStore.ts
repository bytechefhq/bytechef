/* eslint-disable sort-keys */
import {DataPillType} from '@/types/types';
import {create} from 'zustand';

type ComponentActionsType = Array<{
    componentName: string;
    actionName: string;
    workflowAlias?: string;
}>;

interface WorkflowDefinitionState {
    componentNames: string[];
    setComponentNames: (componentNames: string[]) => void;

    componentActions: ComponentActionsType;
    setComponentActions: (componentActions: ComponentActionsType) => void;

    dataPills: Array<DataPillType>;
    setDataPills: (dataPills: Array<DataPillType>) => void;
}

const useWorkflowDataStore = create<WorkflowDefinitionState>((set) => ({
    componentNames: [],
    setComponentNames: (componentNames) =>
        set((state) => ({...state, componentNames})),

    componentActions: [],
    setComponentActions: (componentActions) =>
        set((state) => ({...state, componentActions})),

    dataPills: [],
    setDataPills: (dataPills) => set((state) => ({...state, dataPills})),
}));

export default useWorkflowDataStore;
