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

export type ApiKey = {
  __typename?: 'ApiKey';
  id?: Maybe<Scalars['ID']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  secretKey?: Maybe<Scalars['String']['output']>;
};

export type Category = {
  __typename?: 'Category';
  id?: Maybe<Scalars['ID']['output']>;
  name?: Maybe<Scalars['String']['output']>;
};

export type ComponentDefinition = {
  __typename?: 'ComponentDefinition';
  connection?: Maybe<ConnectionDefinition>;
  description: Scalars['String']['output'];
  icon: Scalars['String']['output'];
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  title: Scalars['String']['output'];
  version?: Maybe<Scalars['Int']['output']>;
};

export type ComponentDefinitionTuple = {
  __typename?: 'ComponentDefinitionTuple';
  key?: Maybe<Scalars['String']['output']>;
  value: Array<Maybe<ComponentDefinition>>;
};

export type ConnectedUser = {
  __typename?: 'ConnectedUser';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['String']['output']>;
  environmentId: Scalars['ID']['output'];
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
  environmentId: Scalars['ID']['output'];
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
  workflowUuid: Scalars['ID']['output'];
  workflowVersion: Scalars['Int']['output'];
};

export type ConnectionDefinition = {
  __typename?: 'ConnectionDefinition';
  authorizationRequired: Scalars['Boolean']['output'];
  version: Scalars['Int']['output'];
};

export type CreateMcpProjectWithWorkflowsInput = {
  mcpServerId: Scalars['ID']['input'];
  projectId: Scalars['ID']['input'];
  projectVersion: Scalars['Int']['input'];
  selectedWorkflowIds: Array<Scalars['String']['input']>;
};

export type CreateMcpServerForWorkspaceInput = {
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  environmentId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  type: ModeType;
  workspaceId: Scalars['ID']['input'];
};

export type Environment = {
  __typename?: 'Environment';
  id?: Maybe<Scalars['ID']['output']>;
  name?: Maybe<Scalars['String']['output']>;
};

export enum EnvironmentEnum {
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
  environmentId: Scalars['ID']['output'];
  id: Scalars['ID']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  mcpComponents?: Maybe<Array<Maybe<McpComponent>>>;
  name: Scalars['String']['output'];
  secretKey: Scalars['String']['output'];
  tags?: Maybe<Array<Maybe<Tag>>>;
  type: ModeType;
  version?: Maybe<Scalars['Int']['output']>;
};

export type McpServerInput = {
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  environmentId: Scalars['ID']['input'];
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
  _placeholder?: Maybe<Scalars['Boolean']['output']>;
  createApiKey: Scalars['String']['output'];
  createMcpComponent?: Maybe<McpComponent>;
  createMcpComponentWithTools?: Maybe<McpComponent>;
  createMcpProjectWithWorkflows?: Maybe<McpProject>;
  createMcpProjectWorkflow?: Maybe<McpProjectWorkflow>;
  createMcpServer?: Maybe<McpServer>;
  createMcpServerForWorkspace?: Maybe<McpServer>;
  createMcpTool?: Maybe<McpTool>;
  createWorkspaceApiKey: Scalars['String']['output'];
  deleteApiKey: Scalars['Boolean']['output'];
  deleteMcpComponent?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpProject?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpProjectWorkflow?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpServer?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpServerFromWorkspace?: Maybe<Scalars['Boolean']['output']>;
  deleteSharedProject: Scalars['Boolean']['output'];
  deleteSharedWorkflow: Scalars['Boolean']['output'];
  deleteWorkspaceApiKey: Scalars['Boolean']['output'];
  exportSharedProject?: Maybe<Scalars['Boolean']['output']>;
  exportSharedWorkflow: Scalars['Boolean']['output'];
  importProjectTemplate: Scalars['ID']['output'];
  importWorkflowTemplate: Scalars['ID']['output'];
  updateApiKey: Scalars['Boolean']['output'];
  updateMcpComponentWithTools?: Maybe<McpComponent>;
  updateMcpProjectWorkflow?: Maybe<McpProjectWorkflow>;
  updateMcpServer?: Maybe<McpServer>;
  updateMcpServerTags?: Maybe<Array<Maybe<Tag>>>;
  updateMcpServerUrl: Scalars['String']['output'];
};


export type MutationCreateApiKeyArgs = {
  environmentId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  type: ModeType;
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


export type MutationCreateMcpServerForWorkspaceArgs = {
  input: CreateMcpServerForWorkspaceInput;
};


export type MutationCreateMcpToolArgs = {
  input: McpToolInput;
};


export type MutationCreateWorkspaceApiKeyArgs = {
  environmentId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationDeleteApiKeyArgs = {
  id: Scalars['ID']['input'];
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


export type MutationDeleteMcpServerFromWorkspaceArgs = {
  mcpServerId: Scalars['ID']['input'];
};


export type MutationDeleteSharedProjectArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteSharedWorkflowArgs = {
  workflowId: Scalars['String']['input'];
};


export type MutationDeleteWorkspaceApiKeyArgs = {
  apiKeyId: Scalars['ID']['input'];
};


export type MutationExportSharedProjectArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
};


export type MutationExportSharedWorkflowArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  workflowId: Scalars['String']['input'];
};


export type MutationImportProjectTemplateArgs = {
  id: Scalars['String']['input'];
  sharedProject: Scalars['Boolean']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationImportWorkflowTemplateArgs = {
  id: Scalars['String']['input'];
  projectId: Scalars['ID']['input'];
  sharedWorkflow: Scalars['Boolean']['input'];
};


export type MutationUpdateApiKeyArgs = {
  id: Scalars['ID']['input'];
  name: Scalars['String']['input'];
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

export type ProjectInfo = {
  __typename?: 'ProjectInfo';
  description?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
};

export type ProjectTemplate = {
  __typename?: 'ProjectTemplate';
  authorEmail?: Maybe<Scalars['String']['output']>;
  authorName?: Maybe<Scalars['String']['output']>;
  authorRole?: Maybe<Scalars['String']['output']>;
  authorSocialLinks: Array<Maybe<Scalars['String']['output']>>;
  categories: Array<Scalars['String']['output']>;
  components: Array<ComponentDefinitionTuple>;
  description?: Maybe<Scalars['String']['output']>;
  id?: Maybe<Scalars['ID']['output']>;
  lastModifiedDate?: Maybe<Scalars['String']['output']>;
  project?: Maybe<ProjectInfo>;
  projectVersion?: Maybe<Scalars['Int']['output']>;
  publicUrl?: Maybe<Scalars['String']['output']>;
  workflows: Array<WorkflowInfo>;
};

export type Query = {
  __typename?: 'Query';
  _placeholder?: Maybe<Scalars['Boolean']['output']>;
  apiKey?: Maybe<ApiKey>;
  apiKeys?: Maybe<Array<Maybe<ApiKey>>>;
  connectedUser?: Maybe<ConnectedUser>;
  connectedUserProjects: Array<ConnectedUserProject>;
  connectedUsers?: Maybe<ConnectedUserPage>;
  environments?: Maybe<Array<Maybe<Environment>>>;
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
  mcpServerUrl?: Maybe<Scalars['String']['output']>;
  mcpServers?: Maybe<Array<Maybe<McpServer>>>;
  mcpServersByWorkspace?: Maybe<Array<Maybe<McpServer>>>;
  mcpTool?: Maybe<McpTool>;
  mcpTools?: Maybe<Array<Maybe<McpTool>>>;
  mcpToolsByComponentId?: Maybe<Array<Maybe<McpTool>>>;
  preBuiltProjectTemplates: Array<ProjectTemplate>;
  preBuiltWorkflowTemplates: Array<WorkflowTemplate>;
  project?: Maybe<Project>;
  projectTemplate?: Maybe<ProjectTemplate>;
  projects?: Maybe<Array<Maybe<Project>>>;
  sharedProject?: Maybe<SharedProject>;
  sharedWorkflow?: Maybe<SharedWorkflow>;
  workflowTemplate?: Maybe<WorkflowTemplate>;
  workspaceApiKeys: Array<ApiKey>;
};


export type QueryApiKeyArgs = {
  id: Scalars['ID']['input'];
};


export type QueryApiKeysArgs = {
  environmentId: Scalars['ID']['input'];
};


export type QueryConnectedUserArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryConnectedUserProjectsArgs = {
  connectedUserId?: InputMaybe<Scalars['ID']['input']>;
  environmentId?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryConnectedUsersArgs = {
  createDateFrom?: InputMaybe<Scalars['String']['input']>;
  createDateTo?: InputMaybe<Scalars['String']['input']>;
  environmentId?: InputMaybe<Scalars['ID']['input']>;
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


export type QueryMcpServersByWorkspaceArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryMcpToolArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpToolsByComponentIdArgs = {
  mcpComponentId?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryPreBuiltProjectTemplatesArgs = {
  category?: InputMaybe<Scalars['String']['input']>;
  query?: InputMaybe<Scalars['String']['input']>;
};


export type QueryPreBuiltWorkflowTemplatesArgs = {
  category?: InputMaybe<Scalars['String']['input']>;
  query?: InputMaybe<Scalars['String']['input']>;
};


export type QueryProjectArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryProjectTemplateArgs = {
  id: Scalars['String']['input'];
  sharedProject: Scalars['Boolean']['input'];
};


export type QuerySharedProjectArgs = {
  projectUuid: Scalars['String']['input'];
};


export type QuerySharedWorkflowArgs = {
  workflowUuid: Scalars['String']['input'];
};


export type QueryWorkflowTemplateArgs = {
  id: Scalars['String']['input'];
  sharedWorkflow: Scalars['Boolean']['input'];
};


export type QueryWorkspaceApiKeysArgs = {
  workspaceId: Scalars['ID']['input'];
};

export type SharedProject = {
  __typename?: 'SharedProject';
  description?: Maybe<Scalars['String']['output']>;
  exported: Scalars['Boolean']['output'];
  projectVersion?: Maybe<Scalars['Int']['output']>;
  publicUrl?: Maybe<Scalars['String']['output']>;
};

export type SharedWorkflow = {
  __typename?: 'SharedWorkflow';
  description?: Maybe<Scalars['String']['output']>;
  exported: Scalars['Boolean']['output'];
  projectVersion?: Maybe<Scalars['Int']['output']>;
  publicUrl?: Maybe<Scalars['String']['output']>;
};

export type SharedWorkflowInfo = {
  __typename?: 'SharedWorkflowInfo';
  description?: Maybe<Scalars['String']['output']>;
  label: Scalars['String']['output'];
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
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  label: Scalars['String']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  version?: Maybe<Scalars['Int']['output']>;
};

export type WorkflowInfo = {
  __typename?: 'WorkflowInfo';
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['String']['output'];
  label: Scalars['String']['output'];
};

export type WorkflowTemplate = {
  __typename?: 'WorkflowTemplate';
  authorEmail?: Maybe<Scalars['String']['output']>;
  authorName?: Maybe<Scalars['String']['output']>;
  authorRole?: Maybe<Scalars['String']['output']>;
  authorSocialLinks: Array<Maybe<Scalars['String']['output']>>;
  categories: Array<Scalars['String']['output']>;
  components: Array<ComponentDefinition>;
  description?: Maybe<Scalars['String']['output']>;
  id?: Maybe<Scalars['ID']['output']>;
  lastModifiedDate?: Maybe<Scalars['String']['output']>;
  projectVersion?: Maybe<Scalars['Int']['output']>;
  publicUrl?: Maybe<Scalars['String']['output']>;
  workflow: SharedWorkflowInfo;
};

export type CreateMcpProjectWithWorkflowsMutationVariables = Exact<{
  input: CreateMcpProjectWithWorkflowsInput;
}>;


export type CreateMcpProjectWithWorkflowsMutation = { __typename?: 'Mutation', createMcpProjectWithWorkflows?: { __typename?: 'McpProject', id: string, mcpServerId: string, projectDeploymentId: string, projectVersion?: number | null } | null };

export type CreateMcpServerMutationVariables = Exact<{
  input: CreateMcpServerForWorkspaceInput;
}>;


export type CreateMcpServerMutation = { __typename?: 'Mutation', createMcpServerForWorkspace?: { __typename?: 'McpServer', id: string, name: string, type: ModeType, environmentId: string, enabled: boolean } | null };

export type DeleteMcpProjectMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteMcpProjectMutation = { __typename?: 'Mutation', deleteMcpProject?: boolean | null };

export type DeleteMcpServerMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteMcpServerMutation = { __typename?: 'Mutation', deleteMcpServerFromWorkspace?: boolean | null };

export type DeleteSharedProjectMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteSharedProjectMutation = { __typename?: 'Mutation', deleteSharedProject: boolean };

export type DeleteSharedWorkflowMutationVariables = Exact<{
  workflowId: Scalars['String']['input'];
}>;


export type DeleteSharedWorkflowMutation = { __typename?: 'Mutation', deleteSharedWorkflow: boolean };

export type ExportSharedProjectMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
}>;


export type ExportSharedProjectMutation = { __typename?: 'Mutation', exportSharedProject?: boolean | null };

export type ExportSharedWorkflowMutationVariables = Exact<{
  workflowId: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
}>;


export type ExportSharedWorkflowMutation = { __typename?: 'Mutation', exportSharedWorkflow: boolean };

export type ImportProjectTemplateMutationVariables = Exact<{
  id: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
  sharedProject: Scalars['Boolean']['input'];
}>;


export type ImportProjectTemplateMutation = { __typename?: 'Mutation', importProjectTemplate: string };

export type ImportWorkflowTemplateMutationVariables = Exact<{
  workflowUuid: Scalars['String']['input'];
  projectId: Scalars['ID']['input'];
  sharedWorkflow: Scalars['Boolean']['input'];
}>;


export type ImportWorkflowTemplateMutation = { __typename?: 'Mutation', importWorkflowTemplate: string };

export type McpProjectsByServerIdQueryVariables = Exact<{
  mcpServerId: Scalars['ID']['input'];
}>;


export type McpProjectsByServerIdQuery = { __typename?: 'Query', mcpProjectsByServerId?: Array<{ __typename?: 'McpProject', id: string, projectDeploymentId: string, mcpServerId: string, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null, projectVersion?: number | null, project?: { __typename?: 'Project', id: string, name: string, category?: { __typename?: 'Category', id?: string | null, name?: string | null } | null, tags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null } | null, mcpProjectWorkflows?: Array<{ __typename?: 'McpProjectWorkflow', id: string, mcpProjectId: any, projectDeploymentWorkflowId: any, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null, projectDeploymentWorkflow?: { __typename?: 'ProjectDeploymentWorkflow', id: string, projectDeploymentId: any, inputs?: any | null, workflowId: string } | null, workflow?: { __typename?: 'Workflow', id: string, label: string } | null } | null> | null } | null> | null };

export type McpServersByWorkspaceQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type McpServersByWorkspaceQuery = { __typename?: 'Query', mcpServersByWorkspace?: Array<{ __typename?: 'McpServer', id: string, name: string, type: ModeType, environmentId: string, enabled: boolean, lastModifiedDate?: any | null, mcpComponents?: Array<{ __typename?: 'McpComponent', id: string, mcpServerId: string, componentName: string, componentVersion: number } | null> | null, tags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null } | null> | null };

export type PreBuiltProjectTemplatesQueryVariables = Exact<{
  query?: InputMaybe<Scalars['String']['input']>;
  category?: InputMaybe<Scalars['String']['input']>;
}>;


export type PreBuiltProjectTemplatesQuery = { __typename?: 'Query', preBuiltProjectTemplates: Array<{ __typename?: 'ProjectTemplate', authorName?: string | null, categories: Array<string>, description?: string | null, id?: string | null, projectVersion?: number | null, publicUrl?: string | null, components: Array<{ __typename?: 'ComponentDefinitionTuple', key?: string | null, value: Array<{ __typename?: 'ComponentDefinition', icon: string, name: string, title: string, version?: number | null, connection?: { __typename?: 'ConnectionDefinition', version: number } | null } | null> }>, project?: { __typename?: 'ProjectInfo', name: string, description?: string | null } | null, workflows: Array<{ __typename?: 'WorkflowInfo', id: string, label: string }> }> };

export type PreBuiltWorkflowTemplatesQueryVariables = Exact<{
  query?: InputMaybe<Scalars['String']['input']>;
  category?: InputMaybe<Scalars['String']['input']>;
}>;


export type PreBuiltWorkflowTemplatesQuery = { __typename?: 'Query', preBuiltWorkflowTemplates: Array<{ __typename?: 'WorkflowTemplate', authorName?: string | null, categories: Array<string>, description?: string | null, id?: string | null, projectVersion?: number | null, publicUrl?: string | null, components: Array<{ __typename?: 'ComponentDefinition', icon: string, name: string, title: string, version?: number | null, connection?: { __typename?: 'ConnectionDefinition', version: number } | null }>, workflow: { __typename?: 'SharedWorkflowInfo', label: string, description?: string | null } }> };

export type ProjectByIdQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type ProjectByIdQuery = { __typename?: 'Query', project?: { __typename?: 'Project', id: string, name: string } | null };

export type ProjectTemplateQueryVariables = Exact<{
  id: Scalars['String']['input'];
  sharedProject: Scalars['Boolean']['input'];
}>;


export type ProjectTemplateQuery = { __typename?: 'Query', projectTemplate?: { __typename?: 'ProjectTemplate', description?: string | null, projectVersion?: number | null, publicUrl?: string | null, components: Array<{ __typename?: 'ComponentDefinitionTuple', key?: string | null, value: Array<{ __typename?: 'ComponentDefinition', icon: string, name: string, title: string, version?: number | null, connection?: { __typename?: 'ConnectionDefinition', version: number } | null } | null> }>, project?: { __typename?: 'ProjectInfo', name: string } | null, workflows: Array<{ __typename?: 'WorkflowInfo', id: string, label: string }> } | null };

export type SharedProjectQueryVariables = Exact<{
  projectUuid: Scalars['String']['input'];
}>;


export type SharedProjectQuery = { __typename?: 'Query', sharedProject?: { __typename?: 'SharedProject', description?: string | null, exported: boolean, projectVersion?: number | null, publicUrl?: string | null } | null };

export type SharedWorkflowQueryVariables = Exact<{
  workflowUuid: Scalars['String']['input'];
}>;


export type SharedWorkflowQuery = { __typename?: 'Query', sharedWorkflow?: { __typename?: 'SharedWorkflow', description?: string | null, exported: boolean, projectVersion?: number | null, publicUrl?: string | null } | null };

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

export type WorkflowTemplateQueryVariables = Exact<{
  id: Scalars['String']['input'];
  sharedWorkflow: Scalars['Boolean']['input'];
}>;


export type WorkflowTemplateQuery = { __typename?: 'Query', workflowTemplate?: { __typename?: 'WorkflowTemplate', description?: string | null, projectVersion?: number | null, publicUrl?: string | null, workflow: { __typename?: 'SharedWorkflowInfo', label: string }, components: Array<{ __typename?: 'ComponentDefinition', icon: string, name: string, title: string, version?: number | null, connection?: { __typename?: 'ConnectionDefinition', version: number } | null }> } | null };

export type ConnectedUserProjectsQueryVariables = Exact<{
  connectedUserId?: InputMaybe<Scalars['ID']['input']>;
  environmentId?: InputMaybe<Scalars['ID']['input']>;
}>;


export type ConnectedUserProjectsQuery = { __typename?: 'Query', connectedUserProjects: Array<{ __typename?: 'ConnectedUserProject', id: string, environmentId: string, lastExecutionDate?: string | null, projectId: string, projectVersion?: number | null, connectedUser: { __typename?: 'ConnectedUser', id: string, environmentId: string, externalId: string }, connectedUserProjectWorkflows: Array<{ __typename?: 'ConnectedUserProjectWorkflow', id: string, connectedUserId: string, enabled: boolean, lastExecutionDate?: string | null, projectId: string, workflowUuid: string, workflowVersion: number, workflow: { __typename?: 'Workflow', id: string, label: string } }> }> };

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

export type CreateMcpToolMutationVariables = Exact<{
  input: McpToolInput;
}>;


export type CreateMcpToolMutation = { __typename?: 'Mutation', createMcpTool?: { __typename?: 'McpTool', id: string, name: string, mcpComponentId: string, parameters?: any | null } | null };

export type DeleteMcpComponentMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteMcpComponentMutation = { __typename?: 'Mutation', deleteMcpComponent?: boolean | null };

export type EnvironmentsQueryVariables = Exact<{ [key: string]: never; }>;


export type EnvironmentsQuery = { __typename?: 'Query', environments?: Array<{ __typename?: 'Environment', id?: string | null, name?: string | null } | null> | null };

export type McpComponentsByServerIdQueryVariables = Exact<{
  mcpServerId: Scalars['ID']['input'];
}>;


export type McpComponentsByServerIdQuery = { __typename?: 'Query', mcpComponentsByServerId?: Array<{ __typename?: 'McpComponent', id: string, componentName: string, componentVersion: number, connectionId?: string | null, mcpServerId: string, version?: number | null, mcpTools?: Array<{ __typename?: 'McpTool', id: string, mcpComponentId: string, name: string } | null> | null } | null> | null };

export type McpServerUrlQueryVariables = Exact<{ [key: string]: never; }>;


export type McpServerUrlQuery = { __typename?: 'Query', mcpServerUrl?: string | null };

export type McpServerTagsQueryVariables = Exact<{
  type: ModeType;
}>;


export type McpServerTagsQuery = { __typename?: 'Query', mcpServerTags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null };

export type McpServersQueryVariables = Exact<{
  type: ModeType;
}>;


export type McpServersQuery = { __typename?: 'Query', mcpServers?: Array<{ __typename?: 'McpServer', id: string, name: string, type: ModeType, environmentId: string, enabled: boolean, secretKey: string, lastModifiedDate?: any | null, mcpComponents?: Array<{ __typename?: 'McpComponent', id: string, mcpServerId: string, componentName: string, componentVersion: number } | null> | null, tags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null } | null> | null };

export type McpToolsByComponentIdQueryVariables = Exact<{
  mcpComponentId: Scalars['ID']['input'];
}>;


export type McpToolsByComponentIdQuery = { __typename?: 'Query', mcpToolsByComponentId?: Array<{ __typename?: 'McpTool', id: string, name: string, mcpComponentId: string, parameters?: any | null } | null> | null };

export type UpdateMcpComponentWithToolsMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: McpComponentWithToolsInput;
}>;


export type UpdateMcpComponentWithToolsMutation = { __typename?: 'Mutation', updateMcpComponentWithTools?: { __typename?: 'McpComponent', id: string, componentName: string, componentVersion: number, mcpServerId: string, connectionId?: string | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null } | null };

export type UpdateMcpServerUrlMutationVariables = Exact<{ [key: string]: never; }>;


export type UpdateMcpServerUrlMutation = { __typename?: 'Mutation', updateMcpServerUrl: string };



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

export const CreateMcpServerDocument = `
    mutation createMcpServer($input: CreateMcpServerForWorkspaceInput!) {
  createMcpServerForWorkspace(input: $input) {
    id
    name
    type
    environmentId
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

export const DeleteMcpServerDocument = `
    mutation deleteMcpServer($id: ID!) {
  deleteMcpServerFromWorkspace(mcpServerId: $id)
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

export const DeleteSharedProjectDocument = `
    mutation deleteSharedProject($id: ID!) {
  deleteSharedProject(id: $id)
}
    `;

export const useDeleteSharedProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteSharedProjectMutation, TError, DeleteSharedProjectMutationVariables, TContext>) => {
    
    return useMutation<DeleteSharedProjectMutation, TError, DeleteSharedProjectMutationVariables, TContext>(
      {
    mutationKey: ['deleteSharedProject'],
    mutationFn: (variables?: DeleteSharedProjectMutationVariables) => fetcher<DeleteSharedProjectMutation, DeleteSharedProjectMutationVariables>(DeleteSharedProjectDocument, variables)(),
    ...options
  }
    )};

export const DeleteSharedWorkflowDocument = `
    mutation deleteSharedWorkflow($workflowId: String!) {
  deleteSharedWorkflow(workflowId: $workflowId)
}
    `;

export const useDeleteSharedWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteSharedWorkflowMutation, TError, DeleteSharedWorkflowMutationVariables, TContext>) => {
    
    return useMutation<DeleteSharedWorkflowMutation, TError, DeleteSharedWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['deleteSharedWorkflow'],
    mutationFn: (variables?: DeleteSharedWorkflowMutationVariables) => fetcher<DeleteSharedWorkflowMutation, DeleteSharedWorkflowMutationVariables>(DeleteSharedWorkflowDocument, variables)(),
    ...options
  }
    )};

export const ExportSharedProjectDocument = `
    mutation exportSharedProject($id: ID!, $description: String) {
  exportSharedProject(id: $id, description: $description)
}
    `;

export const useExportSharedProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ExportSharedProjectMutation, TError, ExportSharedProjectMutationVariables, TContext>) => {
    
    return useMutation<ExportSharedProjectMutation, TError, ExportSharedProjectMutationVariables, TContext>(
      {
    mutationKey: ['exportSharedProject'],
    mutationFn: (variables?: ExportSharedProjectMutationVariables) => fetcher<ExportSharedProjectMutation, ExportSharedProjectMutationVariables>(ExportSharedProjectDocument, variables)(),
    ...options
  }
    )};

export const ExportSharedWorkflowDocument = `
    mutation exportSharedWorkflow($workflowId: String!, $description: String) {
  exportSharedWorkflow(workflowId: $workflowId, description: $description)
}
    `;

export const useExportSharedWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ExportSharedWorkflowMutation, TError, ExportSharedWorkflowMutationVariables, TContext>) => {
    
    return useMutation<ExportSharedWorkflowMutation, TError, ExportSharedWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['exportSharedWorkflow'],
    mutationFn: (variables?: ExportSharedWorkflowMutationVariables) => fetcher<ExportSharedWorkflowMutation, ExportSharedWorkflowMutationVariables>(ExportSharedWorkflowDocument, variables)(),
    ...options
  }
    )};

export const ImportProjectTemplateDocument = `
    mutation importProjectTemplate($id: String!, $workspaceId: ID!, $sharedProject: Boolean!) {
  importProjectTemplate(
    id: $id
    workspaceId: $workspaceId
    sharedProject: $sharedProject
  )
}
    `;

export const useImportProjectTemplateMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ImportProjectTemplateMutation, TError, ImportProjectTemplateMutationVariables, TContext>) => {
    
    return useMutation<ImportProjectTemplateMutation, TError, ImportProjectTemplateMutationVariables, TContext>(
      {
    mutationKey: ['importProjectTemplate'],
    mutationFn: (variables?: ImportProjectTemplateMutationVariables) => fetcher<ImportProjectTemplateMutation, ImportProjectTemplateMutationVariables>(ImportProjectTemplateDocument, variables)(),
    ...options
  }
    )};

export const ImportWorkflowTemplateDocument = `
    mutation importWorkflowTemplate($workflowUuid: String!, $projectId: ID!, $sharedWorkflow: Boolean!) {
  importWorkflowTemplate(
    id: $workflowUuid
    projectId: $projectId
    sharedWorkflow: $sharedWorkflow
  )
}
    `;

export const useImportWorkflowTemplateMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ImportWorkflowTemplateMutation, TError, ImportWorkflowTemplateMutationVariables, TContext>) => {
    
    return useMutation<ImportWorkflowTemplateMutation, TError, ImportWorkflowTemplateMutationVariables, TContext>(
      {
    mutationKey: ['importWorkflowTemplate'],
    mutationFn: (variables?: ImportWorkflowTemplateMutationVariables) => fetcher<ImportWorkflowTemplateMutation, ImportWorkflowTemplateMutationVariables>(ImportWorkflowTemplateDocument, variables)(),
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

export const McpServersByWorkspaceDocument = `
    query mcpServersByWorkspace($workspaceId: ID!) {
  mcpServersByWorkspace(workspaceId: $workspaceId) {
    id
    name
    type
    environmentId
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

export const useMcpServersByWorkspaceQuery = <
      TData = McpServersByWorkspaceQuery,
      TError = unknown
    >(
      variables: McpServersByWorkspaceQueryVariables,
      options?: Omit<UseQueryOptions<McpServersByWorkspaceQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpServersByWorkspaceQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpServersByWorkspaceQuery, TError, TData>(
      {
    queryKey: ['mcpServersByWorkspace', variables],
    queryFn: fetcher<McpServersByWorkspaceQuery, McpServersByWorkspaceQueryVariables>(McpServersByWorkspaceDocument, variables),
    ...options
  }
    )};

export const PreBuiltProjectTemplatesDocument = `
    query preBuiltProjectTemplates($query: String, $category: String) {
  preBuiltProjectTemplates(query: $query, category: $category) {
    authorName
    categories
    components {
      key
      value {
        connection {
          version
        }
        icon
        name
        title
        version
      }
    }
    description
    id
    project {
      name
      description
    }
    projectVersion
    publicUrl
    workflows {
      id
      label
    }
  }
}
    `;

export const usePreBuiltProjectTemplatesQuery = <
      TData = PreBuiltProjectTemplatesQuery,
      TError = unknown
    >(
      variables?: PreBuiltProjectTemplatesQueryVariables,
      options?: Omit<UseQueryOptions<PreBuiltProjectTemplatesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<PreBuiltProjectTemplatesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<PreBuiltProjectTemplatesQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['preBuiltProjectTemplates'] : ['preBuiltProjectTemplates', variables],
    queryFn: fetcher<PreBuiltProjectTemplatesQuery, PreBuiltProjectTemplatesQueryVariables>(PreBuiltProjectTemplatesDocument, variables),
    ...options
  }
    )};

export const PreBuiltWorkflowTemplatesDocument = `
    query preBuiltWorkflowTemplates($query: String, $category: String) {
  preBuiltWorkflowTemplates(query: $query, category: $category) {
    authorName
    categories
    components {
      connection {
        version
      }
      icon
      name
      title
      version
    }
    description
    id
    projectVersion
    publicUrl
    workflow {
      label
      description
    }
  }
}
    `;

export const usePreBuiltWorkflowTemplatesQuery = <
      TData = PreBuiltWorkflowTemplatesQuery,
      TError = unknown
    >(
      variables?: PreBuiltWorkflowTemplatesQueryVariables,
      options?: Omit<UseQueryOptions<PreBuiltWorkflowTemplatesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<PreBuiltWorkflowTemplatesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<PreBuiltWorkflowTemplatesQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['preBuiltWorkflowTemplates'] : ['preBuiltWorkflowTemplates', variables],
    queryFn: fetcher<PreBuiltWorkflowTemplatesQuery, PreBuiltWorkflowTemplatesQueryVariables>(PreBuiltWorkflowTemplatesDocument, variables),
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

export const ProjectTemplateDocument = `
    query projectTemplate($id: String!, $sharedProject: Boolean!) {
  projectTemplate(id: $id, sharedProject: $sharedProject) {
    components {
      key
      value {
        connection {
          version
        }
        icon
        name
        title
        version
      }
    }
    description
    project {
      name
    }
    projectVersion
    publicUrl
    workflows {
      id
      label
    }
  }
}
    `;

export const useProjectTemplateQuery = <
      TData = ProjectTemplateQuery,
      TError = unknown
    >(
      variables: ProjectTemplateQueryVariables,
      options?: Omit<UseQueryOptions<ProjectTemplateQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ProjectTemplateQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ProjectTemplateQuery, TError, TData>(
      {
    queryKey: ['projectTemplate', variables],
    queryFn: fetcher<ProjectTemplateQuery, ProjectTemplateQueryVariables>(ProjectTemplateDocument, variables),
    ...options
  }
    )};

export const SharedProjectDocument = `
    query sharedProject($projectUuid: String!) {
  sharedProject(projectUuid: $projectUuid) {
    description
    exported
    projectVersion
    publicUrl
  }
}
    `;

export const useSharedProjectQuery = <
      TData = SharedProjectQuery,
      TError = unknown
    >(
      variables: SharedProjectQueryVariables,
      options?: Omit<UseQueryOptions<SharedProjectQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<SharedProjectQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<SharedProjectQuery, TError, TData>(
      {
    queryKey: ['sharedProject', variables],
    queryFn: fetcher<SharedProjectQuery, SharedProjectQueryVariables>(SharedProjectDocument, variables),
    ...options
  }
    )};

export const SharedWorkflowDocument = `
    query sharedWorkflow($workflowUuid: String!) {
  sharedWorkflow(workflowUuid: $workflowUuid) {
    description
    exported
    projectVersion
    publicUrl
  }
}
    `;

export const useSharedWorkflowQuery = <
      TData = SharedWorkflowQuery,
      TError = unknown
    >(
      variables: SharedWorkflowQueryVariables,
      options?: Omit<UseQueryOptions<SharedWorkflowQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<SharedWorkflowQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<SharedWorkflowQuery, TError, TData>(
      {
    queryKey: ['sharedWorkflow', variables],
    queryFn: fetcher<SharedWorkflowQuery, SharedWorkflowQueryVariables>(SharedWorkflowDocument, variables),
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

export const WorkflowTemplateDocument = `
    query workflowTemplate($id: String!, $sharedWorkflow: Boolean!) {
  workflowTemplate(id: $id, sharedWorkflow: $sharedWorkflow) {
    description
    projectVersion
    publicUrl
    workflow {
      label
    }
    components {
      connection {
        version
      }
      icon
      name
      title
      version
    }
  }
}
    `;

export const useWorkflowTemplateQuery = <
      TData = WorkflowTemplateQuery,
      TError = unknown
    >(
      variables: WorkflowTemplateQueryVariables,
      options?: Omit<UseQueryOptions<WorkflowTemplateQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkflowTemplateQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkflowTemplateQuery, TError, TData>(
      {
    queryKey: ['workflowTemplate', variables],
    queryFn: fetcher<WorkflowTemplateQuery, WorkflowTemplateQueryVariables>(WorkflowTemplateDocument, variables),
    ...options
  }
    )};

export const ConnectedUserProjectsDocument = `
    query connectedUserProjects($connectedUserId: ID, $environmentId: ID) {
  connectedUserProjects(
    connectedUserId: $connectedUserId
    environmentId: $environmentId
  ) {
    id
    connectedUser {
      id
      environmentId
      externalId
    }
    connectedUserProjectWorkflows {
      id
      connectedUserId
      enabled
      lastExecutionDate
      projectId
      workflowUuid
      workflowVersion
      workflow {
        id
        label
      }
    }
    environmentId
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

export const EnvironmentsDocument = `
    query environments {
  environments {
    id
    name
  }
}
    `;

export const useEnvironmentsQuery = <
      TData = EnvironmentsQuery,
      TError = unknown
    >(
      variables?: EnvironmentsQueryVariables,
      options?: Omit<UseQueryOptions<EnvironmentsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<EnvironmentsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<EnvironmentsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['environments'] : ['environments', variables],
    queryFn: fetcher<EnvironmentsQuery, EnvironmentsQueryVariables>(EnvironmentsDocument, variables),
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

export const McpServerUrlDocument = `
    query mcpServerUrl {
  mcpServerUrl
}
    `;

export const useMcpServerUrlQuery = <
      TData = McpServerUrlQuery,
      TError = unknown
    >(
      variables?: McpServerUrlQueryVariables,
      options?: Omit<UseQueryOptions<McpServerUrlQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpServerUrlQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpServerUrlQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['mcpServerUrl'] : ['mcpServerUrl', variables],
    queryFn: fetcher<McpServerUrlQuery, McpServerUrlQueryVariables>(McpServerUrlDocument, variables),
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
    environmentId
    enabled
    secretKey
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

export const UpdateMcpServerUrlDocument = `
    mutation updateMcpServerUrl {
  updateMcpServerUrl
}
    `;

export const useUpdateMcpServerUrlMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpServerUrlMutation, TError, UpdateMcpServerUrlMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpServerUrlMutation, TError, UpdateMcpServerUrlMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpServerUrl'],
    mutationFn: (variables?: UpdateMcpServerUrlMutationVariables) => fetcher<UpdateMcpServerUrlMutation, UpdateMcpServerUrlMutationVariables>(UpdateMcpServerUrlDocument, variables)(),
    ...options
  }
    )};
