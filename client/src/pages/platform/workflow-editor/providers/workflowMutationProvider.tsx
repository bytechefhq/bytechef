import {UpdateWorkflowMutationType} from '@/shared/types';
import {createContext, useContext} from 'react';

export interface WorkflowMutationStateI {
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

export interface WorkflowMutationProviderProps {
    children: React.ReactNode;
    value: WorkflowMutationStateI;
}

const WorkflowMutationProviderContext = createContext<WorkflowMutationStateI | undefined>(undefined);

export const WorkflowMutationProvider = ({children, value}: WorkflowMutationProviderProps) => {
    return (
        <WorkflowMutationProviderContext.Provider value={value}>{children}</WorkflowMutationProviderContext.Provider>
    );
};

export const useWorkflowMutation = () => {
    const context = useContext(WorkflowMutationProviderContext);

    if (context === undefined) throw new Error('useWorkflowMutation must be used within a WorkflowMutationProvider');

    return context;
};
