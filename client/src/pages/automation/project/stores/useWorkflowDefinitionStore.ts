/* eslint-disable sort-keys */
import {create} from 'zustand';

type ComponentActionsType = Array<{
    componentName: string;
    actionName: string;
}>;

type DataPillType = {
    name: string;
    value: Array<string>;
};

interface WorkflowDefinitionState {
    componentNames: string[];
    setComponentNames: (componentNames: string[]) => void;

    componentActions: ComponentActionsType;
    setComponentActions: (componentActions: ComponentActionsType) => void;

    dataPills: Array<DataPillType>;
    setDataPills: (dataPills: Array<DataPillType>) => void;
}

const useWorkflowDefinitionStore = create<WorkflowDefinitionState>((set) => ({
    componentNames: [],
    setComponentNames: (componentNames) =>
        set((state) => ({...state, componentNames})),

    componentActions: [],
    setComponentActions: (componentActions) =>
        set((state) => ({...state, componentActions})),

    dataPills: [],
    setDataPills: (dataPills) => set((state) => ({...state, dataPills})),
}));

export default useWorkflowDefinitionStore;
