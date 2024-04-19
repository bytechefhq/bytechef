import {
    DeleteWorkflowNodeParameter200ResponseModel,
    DeleteWorkflowNodeParameterRequest,
    UpdateWorkflowNodeParameter200ResponseModel,
    UpdateWorkflowNodeParameterRequest,
} from '@/middleware/platform/configuration';
import {UseMutationResult} from '@tanstack/react-query';
import {ReactNode, createContext, useContext} from 'react';

export interface WorkflowNodeParameterMutationStateI {
    deleteWorkflowNodeParameterMutation?: UseMutationResult<
        DeleteWorkflowNodeParameter200ResponseModel,
        Error,
        DeleteWorkflowNodeParameterRequest,
        unknown
    >;
    updateWorkflowNodeParameterMutation?: UseMutationResult<
        UpdateWorkflowNodeParameter200ResponseModel,
        Error,
        UpdateWorkflowNodeParameterRequest,
        unknown
    >;
}

export interface WorkflowNodeParameterMutationProviderProps {
    children: ReactNode;
    value: WorkflowNodeParameterMutationStateI;
}

const WorkflowNodeParameterMutationProviderContext = createContext<WorkflowNodeParameterMutationStateI | undefined>(
    undefined
);

export const WorkflowNodeParameterMutationProvider = ({
    children,
    value,
}: WorkflowNodeParameterMutationProviderProps) => {
    return (
        <WorkflowNodeParameterMutationProviderContext.Provider value={value}>
            {children}
        </WorkflowNodeParameterMutationProviderContext.Provider>
    );
};

export const useWorkflowNodeParameterMutation = () => {
    const context = useContext(WorkflowNodeParameterMutationProviderContext);

    return context || {};
};
