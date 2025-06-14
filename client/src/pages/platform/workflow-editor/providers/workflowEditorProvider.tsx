import {
    AuthorizationType,
    ComponentDefinitionBasic,
    CredentialStatus,
    DeleteWorkflowNodeParameter200Response,
    DeleteWorkflowNodeParameterOperationRequest,
    Environment,
    Tag,
    UpdateWorkflowNodeParameter200Response,
    UpdateWorkflowNodeParameterOperationRequest,
} from '@/shared/middleware/platform/configuration';
import {GetComponentDefinitionsRequestI} from '@/shared/queries/platform/componentDefinitions.queries';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {UseMutationResult, UseQueryResult} from '@tanstack/react-query';
import {createContext, useContext} from 'react';

export interface CreateConnectionMutationProps {
    onSuccess?: (result: number, variables: ConnectionI) => void;
    onError?: (error: Error, variables: ConnectionI) => void;
}

export interface ConnectionI {
    readonly active?: boolean;
    authorizationType?: AuthorizationType;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    readonly authorizationParameters?: {[key: string]: any};
    componentName: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    readonly connectionParameters?: {[key: string]: any};
    connectionVersion: number;
    readonly createdBy?: string;
    readonly createdDate?: Date;
    credentialStatus?: CredentialStatus;
    environment?: Environment;
    readonly id?: number;
    readonly lastModifiedBy?: string;
    readonly lastModifiedDate?: Date;
    name: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters: {[key: string]: any};
    tags?: Array<Tag>;
    version?: number;
    workspaceId?: number;
}

export interface ConnectionKeysI {
    connection: (id: number) => (string | number)[];
    connectionTags: string[];
    connections: string[];
    filteredConnections: (filters: RequestI) => (string | RequestI)[];
}

export interface RequestI {
    componentName?: string;
    connectionVersion?: number;
    tagId?: number;
}

export interface StartWebhookTriggerTestRequestI {
    workflowId: string;
}

export interface StopWebhookTriggerTestRequestI {
    workflowId: string;
}

export interface StartWebhookTriggerTest200ResponseI {
    webhookUrl?: string;
}

interface WebhookTriggerTestApiI {
    startWebhookTriggerTest(
        requestParameters: StartWebhookTriggerTestRequestI
    ): Promise<StartWebhookTriggerTest200ResponseI>;
    stopWebhookTriggerTest(requestParameters: StopWebhookTriggerTestRequestI): Promise<void>;
}

export interface WorkflowReadOnlyStateI {
    useGetComponentDefinitionsQuery: (
        request: GetComponentDefinitionsRequestI,
        enabled?: boolean
    ) => UseQueryResult<Array<ComponentDefinitionBasic>, Error>;
}

export interface WorkflowEditorStateI extends WorkflowReadOnlyStateI {
    ConnectionKeys: ConnectionKeysI;
    deleteWorkflowNodeParameterMutation: UseMutationResult<
        DeleteWorkflowNodeParameter200Response,
        Error,
        DeleteWorkflowNodeParameterOperationRequest,
        unknown
    >;
    invalidateWorkflowQueries: () => void;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    updateWorkflowNodeParameterMutation: UseMutationResult<
        UpdateWorkflowNodeParameter200Response,
        Error,
        UpdateWorkflowNodeParameterOperationRequest,
        unknown
    >;
    useCreateConnectionMutation: (
        props?: CreateConnectionMutationProps
    ) => UseMutationResult<number, Error, ConnectionI, unknown>;
    useGetConnectionTagsQuery: () => UseQueryResult<Tag[], Error>;
    useGetConnectionsQuery: (request: RequestI, enabled?: boolean) => UseQueryResult<ConnectionI[], Error>;
    webhookTriggerTestApi: WebhookTriggerTestApiI;
}

export interface WorkflowEditorProviderProps {
    children: React.ReactNode;
    value: WorkflowEditorStateI;
}

export interface WorkflowReadOnlyProviderProps {
    children: React.ReactNode;
    value: WorkflowReadOnlyStateI;
}

const WorkflowEditorProviderContext = createContext<WorkflowEditorStateI | undefined>(undefined);

const WorkflowReadoOnlyProviderContext = createContext<WorkflowReadOnlyStateI | undefined>(undefined);

export const WorkflowEditorProvider = ({children, value}: WorkflowEditorProviderProps) => (
    <WorkflowEditorProviderContext.Provider value={value}>{children}</WorkflowEditorProviderContext.Provider>
);

export const WorkflowReadOnlyProvider = ({children, value}: WorkflowReadOnlyProviderProps) => (
    <WorkflowReadoOnlyProviderContext.Provider value={value}>{children}</WorkflowReadoOnlyProviderContext.Provider>
);

export const useWorkflowEditor = (): WorkflowEditorStateI => {
    const editorContext = useContext(WorkflowEditorProviderContext);
    const readOnlyContext = useContext(WorkflowReadoOnlyProviderContext);

    const context = editorContext ? editorContext : readOnlyContext;

    if (context === undefined) {
        throw new Error(
            'useWorkflowEditor must be used within a WorkflowEditorProvider/WorkflowReadoOnlyProviderContext'
        );
    }

    return context as WorkflowEditorStateI;
};
