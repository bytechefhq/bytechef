import {UseMutationResult} from '@tanstack/react-query';
import {ReactNode, createContext, useContext} from 'react';

export interface DeleteWorkflowNodeParameter200ResponseModelI {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: {[key: string]: any};
}

export interface DeleteWorkflowNodeParameterRequestModelI {
    path: string;
    workflowNodeName: string;
}

export interface DeleteWorkflowNodeParameterRequestI {
    id: string;
    deleteWorkflowNodeParameterRequestModel?: DeleteWorkflowNodeParameterRequestModelI;
}

export interface UpdateWorkflowNodeParameter200ResponseModelI {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: {[key: string]: any};
    displayConditions?: {[key: string]: boolean};
}

export interface UpdateWorkflowNodeParameterRequestModelI {
    path: string;
    value?: object;
    workflowNodeName: string;
}

export interface UpdateWorkflowNodeParameterRequestI {
    id: string;
    updateWorkflowNodeParameterRequestModel?: UpdateWorkflowNodeParameterRequestModelI;
}

export interface WorkflowNodeParameterMutationStateI {
    deleteWorkflowNodeParameterMutation?: UseMutationResult<
        DeleteWorkflowNodeParameter200ResponseModelI,
        Error,
        DeleteWorkflowNodeParameterRequestI,
        unknown
    >;
    updateWorkflowNodeParameterMutation?: UseMutationResult<
        UpdateWorkflowNodeParameter200ResponseModelI,
        Error,
        UpdateWorkflowNodeParameterRequestI,
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
