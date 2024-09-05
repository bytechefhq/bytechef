import {
    DeleteWorkflowNodeParameter200Response,
    DeleteWorkflowNodeParameterOperationRequest,
    UpdateWorkflowNodeParameter200Response,
    UpdateWorkflowNodeParameterOperationRequest,
} from '@/shared/middleware/platform/configuration';
import {UseMutationResult} from '@tanstack/react-query';
import {ReactNode, createContext, useContext} from 'react';

export interface WorkflowNodeParameterMutationStateI {
    deleteWorkflowNodeParameterMutation?: UseMutationResult<
        DeleteWorkflowNodeParameter200Response,
        Error,
        DeleteWorkflowNodeParameterOperationRequest,
        unknown
    >;
    updateWorkflowNodeParameterMutation?: UseMutationResult<
        UpdateWorkflowNodeParameter200Response,
        Error,
        UpdateWorkflowNodeParameterOperationRequest,
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
