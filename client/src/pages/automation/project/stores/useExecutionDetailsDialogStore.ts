import {create} from 'zustand';

interface ProjectExecutionDetailsState {
    executionDetailsDialogOpen: boolean;
    setExecutionDetailsDialogOpen: (
        executionDetailsDialogOpen: boolean
    ) => void;

    currentExecutionId: number;
    setCurrentExecutionId: (currentExecutionId: number) => void;
}

export const useExecutionDetailsDialogStore =
    create<ProjectExecutionDetailsState>()((set) => ({
        currentExecutionId: 0,
        setCurrentExecutionId: (currentExecutionId) =>
            set((state) => ({...state, currentExecutionId})),

        executionDetailsDialogOpen: false,
        setExecutionDetailsDialogOpen: (executionDetailsDialogOpen) =>
            set((state) => ({...state, executionDetailsDialogOpen})),
    }));

export default useExecutionDetailsDialogStore;
