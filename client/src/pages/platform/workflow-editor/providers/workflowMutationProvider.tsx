import {WorkflowModel} from '@/middleware/platform/configuration';
import {UpdateWorkflowRequestI} from '@/mutations/platform/workflows.mutations';
import {UseMutationResult} from '@tanstack/react-query';
import {createContext, useContext} from 'react';

export interface WorkflowMutationStateI {
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequestI, unknown>;
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
