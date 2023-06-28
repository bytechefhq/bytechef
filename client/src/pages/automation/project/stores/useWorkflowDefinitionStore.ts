/* eslint-disable sort-keys */
import {create} from 'zustand';

type ComponentActionsType = Array<{
    componentName: string;
    actionName: string;
}>;

interface WorkflowDefinitionState {
    componentNames: string[];
    setComponentNames: (componentNames: string[]) => void;

    componentActions: ComponentActionsType;
    setComponentActions: (componentActions: ComponentActionsType) => void;
}

const useWorkflowDefinitionStore = create<WorkflowDefinitionState>((set) => ({
    componentNames: [],
    setComponentNames: (componentNames) =>
        set((state) => ({...state, componentNames})),

    componentActions: [],
    setComponentActions: (componentActions) =>
        set((state) => ({...state, componentActions})),
}));

export default useWorkflowDefinitionStore;
