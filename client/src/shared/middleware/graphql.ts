import { endpointUrl, fetchParams } from './config';
import { useMutation, useQuery, UseMutationOptions, UseQueryOptions } from '@tanstack/react-query';
export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
export type MakeEmpty<T extends { [key: string]: unknown }, K extends keyof T> = { [_ in K]?: never };
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };

function fetcher<TData, TVariables>(query: string, variables?: TVariables) {
  return async (): Promise<TData> => {
    const res = await fetch(endpointUrl as string, {
    method: "POST",
    ...(fetchParams),
      body: JSON.stringify({ query, variables }),
    });

    const json = await res.json();

    if (json.errors) {
      const { message } = json.errors[0];

      throw new Error(message);
    }

    return json.data;
  }
}
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: { input: string; output: string; }
  String: { input: string; output: string; }
  Boolean: { input: boolean; output: boolean; }
  Int: { input: number; output: number; }
  Float: { input: number; output: number; }
  Long: { input: any; output: any; }
  Map: { input: any; output: any; }
};

export type Category = {
  __typename?: 'Category';
  id?: Maybe<Scalars['ID']['output']>;
  name?: Maybe<Scalars['String']['output']>;
};

export type ConnectedUser = {
  __typename?: 'ConnectedUser';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['String']['output']>;
  environment: Environment;
  externalId: Scalars['String']['output'];
  id: Scalars['ID']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['String']['output']>;
  version?: Maybe<Scalars['Int']['output']>;
};

export type ConnectedUserPage = {
  __typename?: 'ConnectedUserPage';
  content: Array<Maybe<ConnectedUser>>;
  number: Scalars['Int']['output'];
  size: Scalars['Int']['output'];
  totalElements: Scalars['Int']['output'];
  totalPages: Scalars['Int']['output'];
};

export type ConnectedUserProject = {
  __typename?: 'ConnectedUserProject';
  connectedUser: ConnectedUser;
  connectedUserProjectWorkflows: Array<ConnectedUserProjectWorkflow>;
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['String']['output']>;
  environment?: Maybe<Environment>;
  id: Scalars['ID']['output'];
  lastExecutionDate?: Maybe<Scalars['String']['output']>;
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['String']['output']>;
  projectId: Scalars['ID']['output'];
  projectVersion?: Maybe<Scalars['Int']['output']>;
  version?: Maybe<Scalars['Int']['output']>;
};

export type ConnectedUserProjectWorkflow = {
  __typename?: 'ConnectedUserProjectWorkflow';
  connectedUserId: Scalars['ID']['output'];
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['String']['output']>;
  enabled: Scalars['Boolean']['output'];
  id: Scalars['ID']['output'];
  lastExecutionDate?: Maybe<Scalars['String']['output']>;
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['String']['output']>;
  projectId: Scalars['ID']['output'];
  version?: Maybe<Scalars['Int']['output']>;
  workflow: Workflow;
  workflowReferenceCode: Scalars['ID']['output'];
  workflowVersion: Scalars['Int']['output'];
};

export type CreateMcpProjectWithWorkflowsInput = {
  mcpServerId: Scalars['ID']['input'];
  projectId: Scalars['ID']['input'];
  projectVersion: Scalars['Int']['input'];
  selectedWorkflowIds: Array<Scalars['String']['input']>;
};

export enum Environment {
  Development = 'DEVELOPMENT',
  Production = 'PRODUCTION',
  Staging = 'STAGING'
}

export type Integration = {
  __typename?: 'Integration';
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
};

export type McpComponent = {
  __typename?: 'McpComponent';
  componentName: Scalars['String']['output'];
  componentVersion: Scalars['Int']['output'];
  connectionId?: Maybe<Scalars['ID']['output']>;
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  mcpServerId: Scalars['ID']['output'];
  mcpTools?: Maybe<Array<Maybe<McpTool>>>;
  version?: Maybe<Scalars['Int']['output']>;
};

export type McpComponentInput = {
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
  connectionId?: InputMaybe<Scalars['ID']['input']>;
  mcpServerId: Scalars['ID']['input'];
};

export type McpComponentWithToolsInput = {
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
  connectionId?: InputMaybe<Scalars['ID']['input']>;
  mcpServerId: Scalars['ID']['input'];
  tools: Array<McpToolInputForComponent>;
  version?: InputMaybe<Scalars['Int']['input']>;
};

export type McpProject = {
  __typename?: 'McpProject';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  mcpProjectWorkflows?: Maybe<Array<Maybe<McpProjectWorkflow>>>;
  mcpServerId: Scalars['ID']['output'];
  project?: Maybe<Project>;
  projectDeploymentId: Scalars['ID']['output'];
  projectVersion?: Maybe<Scalars['Int']['output']>;
  version?: Maybe<Scalars['Int']['output']>;
};

export type McpProjectWorkflow = {
  __typename?: 'McpProjectWorkflow';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  mcpProjectId: Scalars['Long']['output'];
  projectDeploymentWorkflow?: Maybe<ProjectDeploymentWorkflow>;
  projectDeploymentWorkflowId: Scalars['Long']['output'];
  version?: Maybe<Scalars['Int']['output']>;
  workflow?: Maybe<Workflow>;
};

export type McpProjectWorkflowInput = {
  mcpProjectId: Scalars['Long']['input'];
  projectDeploymentWorkflowId: Scalars['Long']['input'];
};

export type McpProjectWorkflowUpdateInput = {
  mcpProjectId?: InputMaybe<Scalars['Long']['input']>;
  projectDeploymentWorkflowId?: InputMaybe<Scalars['Long']['input']>;
};

export type McpServer = {
  __typename?: 'McpServer';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  enabled: Scalars['Boolean']['output'];
  environment: Environment;
  id: Scalars['ID']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  mcpComponents?: Maybe<Array<Maybe<McpComponent>>>;
  name: Scalars['String']['output'];
  tags?: Maybe<Array<Maybe<Tag>>>;
  type: ModeType;
  version?: Maybe<Scalars['Int']['output']>;
};

export type McpServerInput = {
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  environment: Environment;
  name: Scalars['String']['input'];
  type: ModeType;
};

export enum McpServerOrderBy {
  CreatedDateAsc = 'CREATED_DATE_ASC',
  CreatedDateDesc = 'CREATED_DATE_DESC',
  LastModifiedDateAsc = 'LAST_MODIFIED_DATE_ASC',
  LastModifiedDateDesc = 'LAST_MODIFIED_DATE_DESC',
  NameAsc = 'NAME_ASC',
  NameDesc = 'NAME_DESC'
}

export type McpServerUpdateInput = {
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  name?: InputMaybe<Scalars['String']['input']>;
};

export type McpTool = {
  __typename?: 'McpTool';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  mcpComponentId: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  parameters?: Maybe<Scalars['Map']['output']>;
  version?: Maybe<Scalars['Int']['output']>;
};

export type McpToolInput = {
  mcpComponentId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  parameters?: InputMaybe<Scalars['Map']['input']>;
};

export type McpToolInputForComponent = {
  name: Scalars['String']['input'];
  parameters?: InputMaybe<Scalars['Map']['input']>;
};

export enum ModeType {
  Automation = 'AUTOMATION',
  Embedded = 'EMBEDDED'
}

export type Mutation = {
  __typename?: 'Mutation';
  _empty?: Maybe<Scalars['String']['output']>;
  createMcpComponent?: Maybe<McpComponent>;
  createMcpComponentWithTools?: Maybe<McpComponent>;
  createMcpProjectWithWorkflows?: Maybe<McpProject>;
  createMcpProjectWorkflow?: Maybe<McpProjectWorkflow>;
  createMcpServer?: Maybe<McpServer>;
  createMcpTool?: Maybe<McpTool>;
  deleteMcpComponent?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpProject?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpProjectWorkflow?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpServer?: Maybe<Scalars['Boolean']['output']>;
  updateMcpComponentWithTools?: Maybe<McpComponent>;
  updateMcpProjectWorkflow?: Maybe<McpProjectWorkflow>;
  updateMcpServer?: Maybe<McpServer>;
  updateMcpServerTags?: Maybe<Array<Maybe<Tag>>>;
};


export type MutationCreateMcpComponentArgs = {
  input: McpComponentInput;
};


export type MutationCreateMcpComponentWithToolsArgs = {
  input: McpComponentWithToolsInput;
};


export type MutationCreateMcpProjectWithWorkflowsArgs = {
  input: CreateMcpProjectWithWorkflowsInput;
};


export type MutationCreateMcpProjectWorkflowArgs = {
  input: McpProjectWorkflowInput;
};


export type MutationCreateMcpServerArgs = {
  input: McpServerInput;
};


export type MutationCreateMcpToolArgs = {
  input: McpToolInput;
};


export type MutationDeleteMcpComponentArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteMcpProjectArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteMcpProjectWorkflowArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteMcpServerArgs = {
  id: Scalars['ID']['input'];
};


export type MutationUpdateMcpComponentWithToolsArgs = {
  id: Scalars['ID']['input'];
  input: McpComponentWithToolsInput;
};


export type MutationUpdateMcpProjectWorkflowArgs = {
  id: Scalars['ID']['input'];
  input: McpProjectWorkflowUpdateInput;
};


export type MutationUpdateMcpServerArgs = {
  id: Scalars['ID']['input'];
  input: McpServerUpdateInput;
};


export type MutationUpdateMcpServerTagsArgs = {
  id: Scalars['ID']['input'];
  tags: Array<TagInput>;
};

export type Project = {
  __typename?: 'Project';
  category?: Maybe<Category>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  tags?: Maybe<Array<Maybe<Tag>>>;
};

export type ProjectDeploymentWorkflow = {
  __typename?: 'ProjectDeploymentWorkflow';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  enabled: Scalars['Boolean']['output'];
  id: Scalars['ID']['output'];
  inputs?: Maybe<Scalars['Map']['output']>;
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  projectDeploymentId: Scalars['Long']['output'];
  version?: Maybe<Scalars['Int']['output']>;
  workflowId: Scalars['String']['output'];
};

export type Query = {
  __typename?: 'Query';
  _empty?: Maybe<Scalars['String']['output']>;
  connectedUser?: Maybe<ConnectedUser>;
  connectedUserProjects: Array<ConnectedUserProject>;
  connectedUsers?: Maybe<ConnectedUserPage>;
  integration?: Maybe<Integration>;
  mcpComponent?: Maybe<McpComponent>;
  mcpComponents?: Maybe<Array<Maybe<McpComponent>>>;
  mcpComponentsByServerId?: Maybe<Array<Maybe<McpComponent>>>;
  mcpProject?: Maybe<McpProject>;
  mcpProjectWorkflow?: Maybe<McpProjectWorkflow>;
  mcpProjectWorkflows?: Maybe<Array<Maybe<McpProjectWorkflow>>>;
  mcpProjectWorkflowsByMcpProjectId?: Maybe<Array<Maybe<McpProjectWorkflow>>>;
  mcpProjectWorkflowsByProjectDeploymentWorkflowId?: Maybe<Array<Maybe<McpProjectWorkflow>>>;
  mcpProjects?: Maybe<Array<Maybe<McpProject>>>;
  mcpProjectsByServerId?: Maybe<Array<Maybe<McpProject>>>;
  mcpServer?: Maybe<McpServer>;
  mcpServerTags?: Maybe<Array<Maybe<Tag>>>;
  mcpServers?: Maybe<Array<Maybe<McpServer>>>;
  mcpTool?: Maybe<McpTool>;
  mcpTools?: Maybe<Array<Maybe<McpTool>>>;
  mcpToolsByComponentId?: Maybe<Array<Maybe<McpTool>>>;
  project?: Maybe<Project>;
  projects?: Maybe<Array<Maybe<Project>>>;
};


export type QueryConnectedUserArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryConnectedUserProjectsArgs = {
  connectedUserId?: InputMaybe<Scalars['ID']['input']>;
  environment?: InputMaybe<Environment>;
};


export type QueryConnectedUsersArgs = {
  createDateFrom?: InputMaybe<Scalars['String']['input']>;
  createDateTo?: InputMaybe<Scalars['String']['input']>;
  environment?: InputMaybe<Scalars['Int']['input']>;
  integrationId?: InputMaybe<Scalars['ID']['input']>;
  name?: InputMaybe<Scalars['String']['input']>;
  pageNumber?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryIntegrationArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpComponentArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpComponentsByServerIdArgs = {
  mcpServerId?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpProjectArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpProjectWorkflowArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpProjectWorkflowsByMcpProjectIdArgs = {
  mcpProjectId?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpProjectWorkflowsByProjectDeploymentWorkflowIdArgs = {
  projectDeploymentWorkflowId?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpProjectsByServerIdArgs = {
  mcpServerId?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpServerArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpServerTagsArgs = {
  type: ModeType;
};


export type QueryMcpServersArgs = {
  orderBy?: InputMaybe<McpServerOrderBy>;
  type: ModeType;
};


export type QueryMcpToolArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpToolsByComponentIdArgs = {
  mcpComponentId?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryProjectArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};

export type Tag = {
  __typename?: 'Tag';
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
};

export type TagInput = {
  id?: InputMaybe<Scalars['ID']['input']>;
  name: Scalars['String']['input'];
};

export type Workflow = {
  __typename?: 'Workflow';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  label: Scalars['String']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  version?: Maybe<Scalars['Int']['output']>;
};

export type CreateMcpProjectWithWorkflowsMutationVariables = Exact<{
  input: CreateMcpProjectWithWorkflowsInput;
}>;


export type CreateMcpProjectWithWorkflowsMutation = { __typename?: 'Mutation', createMcpProjectWithWorkflows?: { __typename?: 'McpProject', id: string, mcpServerId: string, projectDeploymentId: string, projectVersion?: number | null } | null };

export type DeleteMcpProjectMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteMcpProjectMutation = { __typename?: 'Mutation', deleteMcpProject?: boolean | null };

export type McpProjectsByServerIdQueryVariables = Exact<{
  mcpServerId: Scalars['ID']['input'];
}>;


export type McpProjectsByServerIdQuery = { __typename?: 'Query', mcpProjectsByServerId?: Array<{ __typename?: 'McpProject', id: string, projectDeploymentId: string, mcpServerId: string, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null, projectVersion?: number | null, project?: { __typename?: 'Project', id: string, name: string, category?: { __typename?: 'Category', id?: string | null, name?: string | null } | null, tags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null } | null, mcpProjectWorkflows?: Array<{ __typename?: 'McpProjectWorkflow', id: string, mcpProjectId: any, projectDeploymentWorkflowId: any, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null, projectDeploymentWorkflow?: { __typename?: 'ProjectDeploymentWorkflow', id: string, projectDeploymentId: any, inputs?: any | null, workflowId: string } | null, workflow?: { __typename?: 'Workflow', id: string, label: string } | null } | null> | null } | null> | null };

export type ProjectByIdQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type ProjectByIdQuery = { __typename?: 'Query', project?: { __typename?: 'Project', id: string, name: string } | null };

export type UpdateMcpServerMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: McpServerUpdateInput;
}>;


export type UpdateMcpServerMutation = { __typename?: 'Mutation', updateMcpServer?: { __typename?: 'McpServer', id: string, name: string, enabled: boolean } | null };

export type UpdateMcpServerTagsMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  tags: Array<TagInput> | TagInput;
}>;


export type UpdateMcpServerTagsMutation = { __typename?: 'Mutation', updateMcpServerTags?: Array<{ __typename?: 'Tag', id: string } | null> | null };

export type ConnectedUserProjectsQueryVariables = Exact<{
  connectedUserId?: InputMaybe<Scalars['ID']['input']>;
  environment?: InputMaybe<Environment>;
}>;


export type ConnectedUserProjectsQuery = { __typename?: 'Query', connectedUserProjects: Array<{ __typename?: 'ConnectedUserProject', id: string, environment?: Environment | null, lastExecutionDate?: string | null, projectId: string, projectVersion?: number | null, connectedUser: { __typename?: 'ConnectedUser', id: string, environment: Environment, externalId: string }, connectedUserProjectWorkflows: Array<{ __typename?: 'ConnectedUserProjectWorkflow', id: string, connectedUserId: string, enabled: boolean, lastExecutionDate?: string | null, projectId: string, workflowReferenceCode: string, workflowVersion: number, workflow: { __typename?: 'Workflow', id: string, label: string } }> }> };

export type IntegrationByIdQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type IntegrationByIdQuery = { __typename?: 'Query', integration?: { __typename?: 'Integration', id: string, name: string } | null };

export type CreateMcpComponentMutationVariables = Exact<{
  input: McpComponentInput;
}>;


export type CreateMcpComponentMutation = { __typename?: 'Mutation', createMcpComponent?: { __typename?: 'McpComponent', id: string, componentName: string, componentVersion: number, mcpServerId: string, connectionId?: string | null } | null };

export type CreateMcpComponentWithToolsMutationVariables = Exact<{
  input: McpComponentWithToolsInput;
}>;


export type CreateMcpComponentWithToolsMutation = { __typename?: 'Mutation', createMcpComponentWithTools?: { __typename?: 'McpComponent', id: string, componentName: string, componentVersion: number, mcpServerId: string, connectionId?: string | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null } | null };

export type CreateMcpServerMutationVariables = Exact<{
  input: McpServerInput;
}>;


export type CreateMcpServerMutation = { __typename?: 'Mutation', createMcpServer?: { __typename?: 'McpServer', id: string, name: string, type: ModeType, environment: Environment, enabled: boolean } | null };

export type CreateMcpToolMutationVariables = Exact<{
  input: McpToolInput;
}>;


export type CreateMcpToolMutation = { __typename?: 'Mutation', createMcpTool?: { __typename?: 'McpTool', id: string, name: string, mcpComponentId: string, parameters?: any | null } | null };

export type DeleteMcpComponentMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteMcpComponentMutation = { __typename?: 'Mutation', deleteMcpComponent?: boolean | null };

export type DeleteMcpServerMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteMcpServerMutation = { __typename?: 'Mutation', deleteMcpServer?: boolean | null };

export type McpComponentsByServerIdQueryVariables = Exact<{
  mcpServerId: Scalars['ID']['input'];
}>;


export type McpComponentsByServerIdQuery = { __typename?: 'Query', mcpComponentsByServerId?: Array<{ __typename?: 'McpComponent', id: string, componentName: string, componentVersion: number, connectionId?: string | null, mcpServerId: string, version?: number | null, mcpTools?: Array<{ __typename?: 'McpTool', id: string, mcpComponentId: string, name: string } | null> | null } | null> | null };

export type McpServerTagsQueryVariables = Exact<{
  type: ModeType;
}>;


export type McpServerTagsQuery = { __typename?: 'Query', mcpServerTags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null };

export type McpServersQueryVariables = Exact<{
  type: ModeType;
}>;


export type McpServersQuery = { __typename?: 'Query', mcpServers?: Array<{ __typename?: 'McpServer', id: string, name: string, type: ModeType, environment: Environment, enabled: boolean, lastModifiedDate?: any | null, mcpComponents?: Array<{ __typename?: 'McpComponent', id: string, mcpServerId: string, componentName: string, componentVersion: number } | null> | null, tags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null } | null> | null };

export type McpToolsByComponentIdQueryVariables = Exact<{
  mcpComponentId: Scalars['ID']['input'];
}>;


export type McpToolsByComponentIdQuery = { __typename?: 'Query', mcpToolsByComponentId?: Array<{ __typename?: 'McpTool', id: string, name: string, mcpComponentId: string, parameters?: any | null } | null> | null };

export type UpdateMcpComponentWithToolsMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: McpComponentWithToolsInput;
}>;


export type UpdateMcpComponentWithToolsMutation = { __typename?: 'Mutation', updateMcpComponentWithTools?: { __typename?: 'McpComponent', id: string, componentName: string, componentVersion: number, mcpServerId: string, connectionId?: string | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null } | null };



export const CreateMcpProjectWithWorkflowsDocument = `
    mutation createMcpProjectWithWorkflows($input: CreateMcpProjectWithWorkflowsInput!) {
  createMcpProjectWithWorkflows(input: $input) {
    id
    mcpServerId
    projectDeploymentId
    projectVersion
  }
}
    `;

export const useCreateMcpProjectWithWorkflowsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateMcpProjectWithWorkflowsMutation, TError, CreateMcpProjectWithWorkflowsMutationVariables, TContext>) => {
    
    return useMutation<CreateMcpProjectWithWorkflowsMutation, TError, CreateMcpProjectWithWorkflowsMutationVariables, TContext>(
      {
    mutationKey: ['createMcpProjectWithWorkflows'],
    mutationFn: (variables?: CreateMcpProjectWithWorkflowsMutationVariables) => fetcher<CreateMcpProjectWithWorkflowsMutation, CreateMcpProjectWithWorkflowsMutationVariables>(CreateMcpProjectWithWorkflowsDocument, variables)(),
    ...options
  }
    )};

export const DeleteMcpProjectDocument = `
    mutation deleteMcpProject($id: ID!) {
  deleteMcpProject(id: $id)
}
    `;

export const useDeleteMcpProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteMcpProjectMutation, TError, DeleteMcpProjectMutationVariables, TContext>) => {
    
    return useMutation<DeleteMcpProjectMutation, TError, DeleteMcpProjectMutationVariables, TContext>(
      {
    mutationKey: ['deleteMcpProject'],
    mutationFn: (variables?: DeleteMcpProjectMutationVariables) => fetcher<DeleteMcpProjectMutation, DeleteMcpProjectMutationVariables>(DeleteMcpProjectDocument, variables)(),
    ...options
  }
    )};

export const McpProjectsByServerIdDocument = `
    query mcpProjectsByServerId($mcpServerId: ID!) {
  mcpProjectsByServerId(mcpServerId: $mcpServerId) {
    id
    projectDeploymentId
    mcpServerId
    project {
      id
      name
      category {
        id
        name
      }
      tags {
        id
        name
      }
    }
    mcpProjectWorkflows {
      id
      mcpProjectId
      projectDeploymentWorkflowId
      projectDeploymentWorkflow {
        id
        projectDeploymentId
        inputs
        workflowId
      }
      workflow {
        id
        label
      }
      createdBy
      createdDate
      lastModifiedBy
      lastModifiedDate
      version
    }
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
    projectVersion
  }
}
    `;

export const useMcpProjectsByServerIdQuery = <
      TData = McpProjectsByServerIdQuery,
      TError = unknown
    >(
      variables: McpProjectsByServerIdQueryVariables,
      options?: Omit<UseQueryOptions<McpProjectsByServerIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpProjectsByServerIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpProjectsByServerIdQuery, TError, TData>(
      {
    queryKey: ['mcpProjectsByServerId', variables],
    queryFn: fetcher<McpProjectsByServerIdQuery, McpProjectsByServerIdQueryVariables>(McpProjectsByServerIdDocument, variables),
    ...options
  }
    )};

export const ProjectByIdDocument = `
    query projectById($id: ID!) {
  project(id: $id) {
    id
    name
  }
}
    `;

export const useProjectByIdQuery = <
      TData = ProjectByIdQuery,
      TError = unknown
    >(
      variables: ProjectByIdQueryVariables,
      options?: Omit<UseQueryOptions<ProjectByIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ProjectByIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ProjectByIdQuery, TError, TData>(
      {
    queryKey: ['projectById', variables],
    queryFn: fetcher<ProjectByIdQuery, ProjectByIdQueryVariables>(ProjectByIdDocument, variables),
    ...options
  }
    )};

export const UpdateMcpServerDocument = `
    mutation updateMcpServer($id: ID!, $input: McpServerUpdateInput!) {
  updateMcpServer(id: $id, input: $input) {
    id
    name
    enabled
  }
}
    `;

export const useUpdateMcpServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpServerMutation, TError, UpdateMcpServerMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpServerMutation, TError, UpdateMcpServerMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpServer'],
    mutationFn: (variables?: UpdateMcpServerMutationVariables) => fetcher<UpdateMcpServerMutation, UpdateMcpServerMutationVariables>(UpdateMcpServerDocument, variables)(),
    ...options
  }
    )};

export const UpdateMcpServerTagsDocument = `
    mutation updateMcpServerTags($id: ID!, $tags: [TagInput!]!) {
  updateMcpServerTags(id: $id, tags: $tags) {
    id
  }
}
    `;

export const useUpdateMcpServerTagsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpServerTagsMutation, TError, UpdateMcpServerTagsMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpServerTagsMutation, TError, UpdateMcpServerTagsMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpServerTags'],
    mutationFn: (variables?: UpdateMcpServerTagsMutationVariables) => fetcher<UpdateMcpServerTagsMutation, UpdateMcpServerTagsMutationVariables>(UpdateMcpServerTagsDocument, variables)(),
    ...options
  }
    )};

export const ConnectedUserProjectsDocument = `
    query connectedUserProjects($connectedUserId: ID, $environment: Environment) {
  connectedUserProjects(
    connectedUserId: $connectedUserId
    environment: $environment
  ) {
    id
    connectedUser {
      id
      environment
      externalId
    }
    connectedUserProjectWorkflows {
      id
      connectedUserId
      enabled
      lastExecutionDate
      projectId
      workflowReferenceCode
      workflowVersion
      workflow {
        id
        label
      }
    }
    environment
    lastExecutionDate
    projectId
    projectVersion
  }
}
    `;

export const useConnectedUserProjectsQuery = <
      TData = ConnectedUserProjectsQuery,
      TError = unknown
    >(
      variables?: ConnectedUserProjectsQueryVariables,
      options?: Omit<UseQueryOptions<ConnectedUserProjectsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ConnectedUserProjectsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ConnectedUserProjectsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['connectedUserProjects'] : ['connectedUserProjects', variables],
    queryFn: fetcher<ConnectedUserProjectsQuery, ConnectedUserProjectsQueryVariables>(ConnectedUserProjectsDocument, variables),
    ...options
  }
    )};

export const IntegrationByIdDocument = `
    query integrationById($id: ID!) {
  integration(id: $id) {
    id
    name
  }
}
    `;

export const useIntegrationByIdQuery = <
      TData = IntegrationByIdQuery,
      TError = unknown
    >(
      variables: IntegrationByIdQueryVariables,
      options?: Omit<UseQueryOptions<IntegrationByIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<IntegrationByIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<IntegrationByIdQuery, TError, TData>(
      {
    queryKey: ['integrationById', variables],
    queryFn: fetcher<IntegrationByIdQuery, IntegrationByIdQueryVariables>(IntegrationByIdDocument, variables),
    ...options
  }
    )};

export const CreateMcpComponentDocument = `
    mutation createMcpComponent($input: McpComponentInput!) {
  createMcpComponent(input: $input) {
    id
    componentName
    componentVersion
    mcpServerId
    connectionId
  }
}
    `;

export const useCreateMcpComponentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateMcpComponentMutation, TError, CreateMcpComponentMutationVariables, TContext>) => {
    
    return useMutation<CreateMcpComponentMutation, TError, CreateMcpComponentMutationVariables, TContext>(
      {
    mutationKey: ['createMcpComponent'],
    mutationFn: (variables?: CreateMcpComponentMutationVariables) => fetcher<CreateMcpComponentMutation, CreateMcpComponentMutationVariables>(CreateMcpComponentDocument, variables)(),
    ...options
  }
    )};

export const CreateMcpComponentWithToolsDocument = `
    mutation createMcpComponentWithTools($input: McpComponentWithToolsInput!) {
  createMcpComponentWithTools(input: $input) {
    id
    componentName
    componentVersion
    mcpServerId
    connectionId
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `;

export const useCreateMcpComponentWithToolsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateMcpComponentWithToolsMutation, TError, CreateMcpComponentWithToolsMutationVariables, TContext>) => {
    
    return useMutation<CreateMcpComponentWithToolsMutation, TError, CreateMcpComponentWithToolsMutationVariables, TContext>(
      {
    mutationKey: ['createMcpComponentWithTools'],
    mutationFn: (variables?: CreateMcpComponentWithToolsMutationVariables) => fetcher<CreateMcpComponentWithToolsMutation, CreateMcpComponentWithToolsMutationVariables>(CreateMcpComponentWithToolsDocument, variables)(),
    ...options
  }
    )};

export const CreateMcpServerDocument = `
    mutation createMcpServer($input: McpServerInput!) {
  createMcpServer(input: $input) {
    id
    name
    type
    environment
    enabled
  }
}
    `;

export const useCreateMcpServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateMcpServerMutation, TError, CreateMcpServerMutationVariables, TContext>) => {
    
    return useMutation<CreateMcpServerMutation, TError, CreateMcpServerMutationVariables, TContext>(
      {
    mutationKey: ['createMcpServer'],
    mutationFn: (variables?: CreateMcpServerMutationVariables) => fetcher<CreateMcpServerMutation, CreateMcpServerMutationVariables>(CreateMcpServerDocument, variables)(),
    ...options
  }
    )};

export const CreateMcpToolDocument = `
    mutation createMcpTool($input: McpToolInput!) {
  createMcpTool(input: $input) {
    id
    name
    mcpComponentId
    parameters
  }
}
    `;

export const useCreateMcpToolMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateMcpToolMutation, TError, CreateMcpToolMutationVariables, TContext>) => {
    
    return useMutation<CreateMcpToolMutation, TError, CreateMcpToolMutationVariables, TContext>(
      {
    mutationKey: ['createMcpTool'],
    mutationFn: (variables?: CreateMcpToolMutationVariables) => fetcher<CreateMcpToolMutation, CreateMcpToolMutationVariables>(CreateMcpToolDocument, variables)(),
    ...options
  }
    )};

export const DeleteMcpComponentDocument = `
    mutation deleteMcpComponent($id: ID!) {
  deleteMcpComponent(id: $id)
}
    `;

export const useDeleteMcpComponentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteMcpComponentMutation, TError, DeleteMcpComponentMutationVariables, TContext>) => {
    
    return useMutation<DeleteMcpComponentMutation, TError, DeleteMcpComponentMutationVariables, TContext>(
      {
    mutationKey: ['deleteMcpComponent'],
    mutationFn: (variables?: DeleteMcpComponentMutationVariables) => fetcher<DeleteMcpComponentMutation, DeleteMcpComponentMutationVariables>(DeleteMcpComponentDocument, variables)(),
    ...options
  }
    )};

export const DeleteMcpServerDocument = `
    mutation deleteMcpServer($id: ID!) {
  deleteMcpServer(id: $id)
}
    `;

export const useDeleteMcpServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteMcpServerMutation, TError, DeleteMcpServerMutationVariables, TContext>) => {
    
    return useMutation<DeleteMcpServerMutation, TError, DeleteMcpServerMutationVariables, TContext>(
      {
    mutationKey: ['deleteMcpServer'],
    mutationFn: (variables?: DeleteMcpServerMutationVariables) => fetcher<DeleteMcpServerMutation, DeleteMcpServerMutationVariables>(DeleteMcpServerDocument, variables)(),
    ...options
  }
    )};

export const McpComponentsByServerIdDocument = `
    query mcpComponentsByServerId($mcpServerId: ID!) {
  mcpComponentsByServerId(mcpServerId: $mcpServerId) {
    id
    componentName
    componentVersion
    connectionId
    mcpServerId
    mcpTools {
      id
      mcpComponentId
      name
    }
    version
  }
}
    `;

export const useMcpComponentsByServerIdQuery = <
      TData = McpComponentsByServerIdQuery,
      TError = unknown
    >(
      variables: McpComponentsByServerIdQueryVariables,
      options?: Omit<UseQueryOptions<McpComponentsByServerIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpComponentsByServerIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpComponentsByServerIdQuery, TError, TData>(
      {
    queryKey: ['mcpComponentsByServerId', variables],
    queryFn: fetcher<McpComponentsByServerIdQuery, McpComponentsByServerIdQueryVariables>(McpComponentsByServerIdDocument, variables),
    ...options
  }
    )};

export const McpServerTagsDocument = `
    query mcpServerTags($type: ModeType!) {
  mcpServerTags(type: $type) {
    id
    name
  }
}
    `;

export const useMcpServerTagsQuery = <
      TData = McpServerTagsQuery,
      TError = unknown
    >(
      variables: McpServerTagsQueryVariables,
      options?: Omit<UseQueryOptions<McpServerTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpServerTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpServerTagsQuery, TError, TData>(
      {
    queryKey: ['mcpServerTags', variables],
    queryFn: fetcher<McpServerTagsQuery, McpServerTagsQueryVariables>(McpServerTagsDocument, variables),
    ...options
  }
    )};

export const McpServersDocument = `
    query mcpServers($type: ModeType!) {
  mcpServers(type: $type, orderBy: NAME_ASC) {
    id
    name
    type
    environment
    enabled
    mcpComponents {
      id
      mcpServerId
      componentName
      componentVersion
    }
    tags {
      id
      name
    }
    lastModifiedDate
  }
}
    `;

export const useMcpServersQuery = <
      TData = McpServersQuery,
      TError = unknown
    >(
      variables: McpServersQueryVariables,
      options?: Omit<UseQueryOptions<McpServersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpServersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpServersQuery, TError, TData>(
      {
    queryKey: ['mcpServers', variables],
    queryFn: fetcher<McpServersQuery, McpServersQueryVariables>(McpServersDocument, variables),
    ...options
  }
    )};

export const McpToolsByComponentIdDocument = `
    query mcpToolsByComponentId($mcpComponentId: ID!) {
  mcpToolsByComponentId(mcpComponentId: $mcpComponentId) {
    id
    name
    mcpComponentId
    parameters
  }
}
    `;

export const useMcpToolsByComponentIdQuery = <
      TData = McpToolsByComponentIdQuery,
      TError = unknown
    >(
      variables: McpToolsByComponentIdQueryVariables,
      options?: Omit<UseQueryOptions<McpToolsByComponentIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpToolsByComponentIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpToolsByComponentIdQuery, TError, TData>(
      {
    queryKey: ['mcpToolsByComponentId', variables],
    queryFn: fetcher<McpToolsByComponentIdQuery, McpToolsByComponentIdQueryVariables>(McpToolsByComponentIdDocument, variables),
    ...options
  }
    )};

export const UpdateMcpComponentWithToolsDocument = `
    mutation updateMcpComponentWithTools($id: ID!, $input: McpComponentWithToolsInput!) {
  updateMcpComponentWithTools(id: $id, input: $input) {
    id
    componentName
    componentVersion
    mcpServerId
    connectionId
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `;

export const useUpdateMcpComponentWithToolsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpComponentWithToolsMutation, TError, UpdateMcpComponentWithToolsMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpComponentWithToolsMutation, TError, UpdateMcpComponentWithToolsMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpComponentWithTools'],
    mutationFn: (variables?: UpdateMcpComponentWithToolsMutationVariables) => fetcher<UpdateMcpComponentWithToolsMutation, UpdateMcpComponentWithToolsMutationVariables>(UpdateMcpComponentWithToolsDocument, variables)(),
    ...options
  }
    )};
