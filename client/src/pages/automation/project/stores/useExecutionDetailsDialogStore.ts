import {JobModelStatusEnum, ProjectExecutionModel} from 'middleware/project';
import {create} from 'zustand';

interface ProjectExecutionDetailsState {
    executionDetailsOpen: boolean;
    setExecutionDetailsOpen: (executionDetailsOpen: boolean) => void;

    currentExecution: ProjectExecutionModel;
    setCurrentExecution: (currentExecution: ProjectExecutionModel) => void;
}

export const useExecutionDetailsDialogStore =
    create<ProjectExecutionDetailsState>()((set) => ({
        executionDetailsOpen: false,
        setExecutionDetailsOpen: (executionDetailsOpen) =>
            set((state) => ({...state, executionDetailsOpen})),

        currentExecution: {
            priority: 1,
            startDate: new Date(),
            status: JobModelStatusEnum.Created,
        },
        setCurrentExecution: (currentExecution) =>
            set((state) => ({...state, currentExecution})),
    }));

export default useExecutionDetailsDialogStore;
