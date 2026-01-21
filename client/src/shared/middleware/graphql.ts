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

export type ActionDefinition = {
  __typename?: 'ActionDefinition';
  componentName: Scalars['String']['output'];
  componentVersion: Scalars['Int']['output'];
  description?: Maybe<Scalars['String']['output']>;
  help?: Maybe<Help>;
  name: Scalars['String']['output'];
  outputDefined: Scalars['Boolean']['output'];
  outputFunctionDefined: Scalars['Boolean']['output'];
  outputSchemaDefined?: Maybe<Scalars['Boolean']['output']>;
  properties: Array<Property>;
  title?: Maybe<Scalars['String']['output']>;
  workflowNodeDescriptionDefined?: Maybe<Scalars['Boolean']['output']>;
};

export type AddColumnInput = {
  column: ColumnInput;
  environmentId: Scalars['ID']['input'];
  tableId: Scalars['ID']['input'];
};

export type AdminUser = {
  __typename?: 'AdminUser';
  activated?: Maybe<Scalars['Boolean']['output']>;
  authorities?: Maybe<Array<Maybe<Scalars['String']['output']>>>;
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  email?: Maybe<Scalars['String']['output']>;
  firstName?: Maybe<Scalars['String']['output']>;
  id?: Maybe<Scalars['ID']['output']>;
  imageUrl?: Maybe<Scalars['String']['output']>;
  langKey?: Maybe<Scalars['String']['output']>;
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  lastName?: Maybe<Scalars['String']['output']>;
  login?: Maybe<Scalars['String']['output']>;
  uuid?: Maybe<Scalars['String']['output']>;
};

export type AdminUserPage = {
  __typename?: 'AdminUserPage';
  content: Array<Maybe<AdminUser>>;
  number: Scalars['Int']['output'];
  size: Scalars['Int']['output'];
  totalElements: Scalars['Int']['output'];
  totalPages: Scalars['Int']['output'];
};

export type ApiKey = {
  __typename?: 'ApiKey';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  id?: Maybe<Scalars['ID']['output']>;
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  lastUsedDate?: Maybe<Scalars['Long']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  secretKey?: Maybe<Scalars['String']['output']>;
};

export type ArrayProperty = Property & {
  __typename?: 'ArrayProperty';
  advancedOption?: Maybe<Scalars['Boolean']['output']>;
  controlType: ControlType;
  defaultValue?: Maybe<Array<Maybe<Scalars['Map']['output']>>>;
  description?: Maybe<Scalars['String']['output']>;
  displayCondition?: Maybe<Scalars['String']['output']>;
  exampleValue?: Maybe<Array<Maybe<Scalars['Map']['output']>>>;
  expressionEnabled?: Maybe<Scalars['Boolean']['output']>;
  hidden?: Maybe<Scalars['Boolean']['output']>;
  items?: Maybe<Array<Property>>;
  label?: Maybe<Scalars['String']['output']>;
  maxItems?: Maybe<Scalars['Long']['output']>;
  minItems?: Maybe<Scalars['Long']['output']>;
  multipleValues?: Maybe<Scalars['Boolean']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  options?: Maybe<Array<Option>>;
  optionsDataSource?: Maybe<OptionsDataSource>;
  placeholder?: Maybe<Scalars['String']['output']>;
  required?: Maybe<Scalars['Boolean']['output']>;
  type: PropertyType;
};

export type Authorization = {
  __typename?: 'Authorization';
  description?: Maybe<Scalars['String']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  properties?: Maybe<Array<Property>>;
  title?: Maybe<Scalars['String']['output']>;
  type?: Maybe<AuthorizationType>;
};

export enum AuthorizationType {
  ApiKey = 'API_KEY',
  BasicAuth = 'BASIC_AUTH',
  BearerToken = 'BEARER_TOKEN',
  Custom = 'CUSTOM',
  DigestAuth = 'DIGEST_AUTH',
  Oauth2AuthorizationCode = 'OAUTH2_AUTHORIZATION_CODE',
  Oauth2AuthorizationCodePkce = 'OAUTH2_AUTHORIZATION_CODE_PKCE',
  Oauth2ClientCredentials = 'OAUTH2_CLIENT_CREDENTIALS',
  Oauth2ImplicitCode = 'OAUTH2_IMPLICIT_CODE',
  Oauth2ResourceOwnerPassword = 'OAUTH2_RESOURCE_OWNER_PASSWORD'
}

export type BooleanProperty = Property & {
  __typename?: 'BooleanProperty';
  advancedOption?: Maybe<Scalars['Boolean']['output']>;
  controlType: ControlType;
  defaultValue?: Maybe<Scalars['Boolean']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  displayCondition?: Maybe<Scalars['String']['output']>;
  exampleValue?: Maybe<Scalars['Boolean']['output']>;
  expressionEnabled?: Maybe<Scalars['Boolean']['output']>;
  hidden?: Maybe<Scalars['Boolean']['output']>;
  label?: Maybe<Scalars['String']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  placeholder?: Maybe<Scalars['String']['output']>;
  required?: Maybe<Scalars['Boolean']['output']>;
  type: PropertyType;
};

export type Category = {
  __typename?: 'Category';
  id?: Maybe<Scalars['ID']['output']>;
  name?: Maybe<Scalars['String']['output']>;
};

export type ClusterElementDefinition = {
  __typename?: 'ClusterElementDefinition';
  componentName?: Maybe<Scalars['String']['output']>;
  componentVersion?: Maybe<Scalars['Int']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  help?: Maybe<Help>;
  icon?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  outputDefined: Scalars['Boolean']['output'];
  outputFunctionDefined: Scalars['Boolean']['output'];
  outputSchemaDefined?: Maybe<Scalars['Boolean']['output']>;
  properties: Array<Property>;
  title?: Maybe<Scalars['String']['output']>;
  type?: Maybe<ClusterElementType>;
};

export type ClusterElementType = {
  __typename?: 'ClusterElementType';
  key?: Maybe<Scalars['String']['output']>;
  label?: Maybe<Scalars['String']['output']>;
  multipleElements?: Maybe<Scalars['Boolean']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  required?: Maybe<Scalars['Boolean']['output']>;
};

export type ColumnInput = {
  name: Scalars['String']['input'];
  type: ColumnType;
};

export enum ColumnType {
  Boolean = 'BOOLEAN',
  Date = 'DATE',
  DateTime = 'DATE_TIME',
  Integer = 'INTEGER',
  Number = 'NUMBER',
  String = 'STRING'
}

export type ComponentCategory = {
  __typename?: 'ComponentCategory';
  label?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
};

export type ComponentDefinition = {
  __typename?: 'ComponentDefinition';
  actionClusterElementTypes?: Maybe<Scalars['Map']['output']>;
  actions?: Maybe<Array<ActionDefinition>>;
  actionsCount?: Maybe<Scalars['Int']['output']>;
  clusterElement?: Maybe<Scalars['Boolean']['output']>;
  clusterElementClusterElementTypes?: Maybe<Scalars['Map']['output']>;
  clusterElementTypes?: Maybe<Array<ClusterElementType>>;
  clusterElements?: Maybe<Array<ClusterElementDefinition>>;
  clusterElementsCount?: Maybe<Scalars['Map']['output']>;
  clusterRoot?: Maybe<Scalars['Boolean']['output']>;
  componentCategories?: Maybe<Array<ComponentCategory>>;
  connection?: Maybe<ConnectionDefinition>;
  connectionRequired?: Maybe<Scalars['Boolean']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  icon?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  resources?: Maybe<Resources>;
  tags?: Maybe<Array<Scalars['String']['output']>>;
  title?: Maybe<Scalars['String']['output']>;
  triggers?: Maybe<Array<TriggerDefinition>>;
  triggersCount?: Maybe<Scalars['Int']['output']>;
  unifiedApiCategory?: Maybe<UnifiedApiCategory>;
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
  authorizations?: Maybe<Array<Authorization>>;
  baseUri?: Maybe<Scalars['String']['output']>;
  componentDescription?: Maybe<Scalars['String']['output']>;
  componentName: Scalars['String']['output'];
  componentTitle?: Maybe<Scalars['String']['output']>;
  properties?: Maybe<Array<Property>>;
  version: Scalars['Int']['output'];
};

export enum ControlType {
  ArrayBuilder = 'ARRAY_BUILDER',
  CodeEditor = 'CODE_EDITOR',
  Date = 'DATE',
  DateTime = 'DATE_TIME',
  Email = 'EMAIL',
  FileEntry = 'FILE_ENTRY',
  Integer = 'INTEGER',
  JsonSchemaBuilder = 'JSON_SCHEMA_BUILDER',
  MultiSelect = 'MULTI_SELECT',
  Null = 'NULL',
  Number = 'NUMBER',
  ObjectBuilder = 'OBJECT_BUILDER',
  Password = 'PASSWORD',
  Phone = 'PHONE',
  RichText = 'RICH_TEXT',
  Select = 'SELECT',
  Text = 'TEXT',
  TextArea = 'TEXT_AREA',
  Time = 'TIME',
  Url = 'URL'
}

export type CreateDataTableInput = {
  baseName: Scalars['String']['input'];
  columns: Array<ColumnInput>;
  description?: InputMaybe<Scalars['String']['input']>;
  environmentId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};

export type CreateMcpProjectInput = {
  mcpServerId: Scalars['ID']['input'];
  projectId: Scalars['ID']['input'];
  projectVersion: Scalars['Int']['input'];
  selectedWorkflowIds: Array<Scalars['String']['input']>;
};

export type CreateWorkspaceMcpServerInput = {
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  environmentId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  type: PlatformType;
  workspaceId: Scalars['ID']['input'];
};

export type DataTable = {
  __typename?: 'DataTable';
  baseName: Scalars['String']['output'];
  columns: Array<DataTableColumn>;
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
};

export type DataTableColumn = {
  __typename?: 'DataTableColumn';
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  type: ColumnType;
};

export type DataTableRow = {
  __typename?: 'DataTableRow';
  id: Scalars['ID']['output'];
  values: Scalars['Map']['output'];
};

export type DataTableRowPage = {
  __typename?: 'DataTableRowPage';
  hasMore: Scalars['Boolean']['output'];
  items: Array<DataTableRow>;
  nextOffset?: Maybe<Scalars['Int']['output']>;
};

export type DataTableTagsEntry = {
  __typename?: 'DataTableTagsEntry';
  tableId: Scalars['ID']['output'];
  tags: Array<Tag>;
};

export type DataTableWebhook = {
  __typename?: 'DataTableWebhook';
  environmentId: Scalars['Long']['output'];
  id: Scalars['ID']['output'];
  type: DataTableWebhookType;
  url: Scalars['String']['output'];
};

export enum DataTableWebhookType {
  RecordCreated = 'RECORD_CREATED',
  RecordDeleted = 'RECORD_DELETED',
  RecordUpdated = 'RECORD_UPDATED'
}

export type DateProperty = Property & {
  __typename?: 'DateProperty';
  advancedOption?: Maybe<Scalars['Boolean']['output']>;
  controlType: ControlType;
  defaultValue?: Maybe<Scalars['String']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  displayCondition?: Maybe<Scalars['String']['output']>;
  exampleValue?: Maybe<Scalars['String']['output']>;
  expressionEnabled?: Maybe<Scalars['Boolean']['output']>;
  hidden?: Maybe<Scalars['Boolean']['output']>;
  label?: Maybe<Scalars['String']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  placeholder?: Maybe<Scalars['String']['output']>;
  required?: Maybe<Scalars['Boolean']['output']>;
  type: PropertyType;
};

export type DateTimeProperty = Property & {
  __typename?: 'DateTimeProperty';
  advancedOption?: Maybe<Scalars['Boolean']['output']>;
  controlType: ControlType;
  defaultValue?: Maybe<Scalars['String']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  displayCondition?: Maybe<Scalars['String']['output']>;
  exampleValue?: Maybe<Scalars['String']['output']>;
  expressionEnabled?: Maybe<Scalars['Boolean']['output']>;
  hidden?: Maybe<Scalars['Boolean']['output']>;
  label?: Maybe<Scalars['String']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  placeholder?: Maybe<Scalars['String']['output']>;
  required?: Maybe<Scalars['Boolean']['output']>;
  type: PropertyType;
};

export type DeleteRowInput = {
  environmentId: Scalars['ID']['input'];
  id: Scalars['ID']['input'];
  tableId: Scalars['ID']['input'];
};

export type DuplicateDataTableInput = {
  environmentId: Scalars['ID']['input'];
  newBaseName: Scalars['String']['input'];
  tableId: Scalars['ID']['input'];
};

export type DynamicPropertiesProperty = Property & {
  __typename?: 'DynamicPropertiesProperty';
  advancedOption?: Maybe<Scalars['Boolean']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  displayCondition?: Maybe<Scalars['String']['output']>;
  expressionEnabled?: Maybe<Scalars['Boolean']['output']>;
  header?: Maybe<Scalars['String']['output']>;
  hidden?: Maybe<Scalars['Boolean']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  propertiesDataSource?: Maybe<PropertiesDataSource>;
  required?: Maybe<Scalars['Boolean']['output']>;
  type: PropertyType;
};

export type Environment = {
  __typename?: 'Environment';
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
};

export enum EnvironmentEnum {
  Development = 'DEVELOPMENT',
  Production = 'PRODUCTION',
  Staging = 'STAGING'
}

export type FileEntryProperty = Property & {
  __typename?: 'FileEntryProperty';
  advancedOption?: Maybe<Scalars['Boolean']['output']>;
  controlType: ControlType;
  defaultValue?: Maybe<Scalars['Map']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  displayCondition?: Maybe<Scalars['String']['output']>;
  exampleValue?: Maybe<Scalars['Map']['output']>;
  expressionEnabled?: Maybe<Scalars['Boolean']['output']>;
  hidden?: Maybe<Scalars['Boolean']['output']>;
  label?: Maybe<Scalars['String']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  placeholder?: Maybe<Scalars['String']['output']>;
  required?: Maybe<Scalars['Boolean']['output']>;
  type: PropertyType;
};

export type Help = {
  __typename?: 'Help';
  description?: Maybe<Scalars['String']['output']>;
  documentationUrl?: Maybe<Scalars['String']['output']>;
};

export type ImportCsvInput = {
  csv: Scalars['String']['input'];
  environmentId: Scalars['ID']['input'];
  tableId: Scalars['ID']['input'];
};

export type InsertRowInput = {
  environmentId: Scalars['ID']['input'];
  tableId: Scalars['ID']['input'];
  values: Scalars['Map']['input'];
};

export type IntegerProperty = Property & {
  __typename?: 'IntegerProperty';
  advancedOption?: Maybe<Scalars['Boolean']['output']>;
  controlType: ControlType;
  defaultValue?: Maybe<Scalars['Long']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  displayCondition?: Maybe<Scalars['String']['output']>;
  exampleValue?: Maybe<Scalars['Long']['output']>;
  expressionEnabled?: Maybe<Scalars['Boolean']['output']>;
  hidden?: Maybe<Scalars['Boolean']['output']>;
  label?: Maybe<Scalars['String']['output']>;
  maxValue?: Maybe<Scalars['Long']['output']>;
  minValue?: Maybe<Scalars['Long']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  options?: Maybe<Array<Option>>;
  optionsDataSource?: Maybe<OptionsDataSource>;
  placeholder?: Maybe<Scalars['String']['output']>;
  required?: Maybe<Scalars['Boolean']['output']>;
  type: PropertyType;
};

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
  type: PlatformType;
  url: Scalars['String']['output'];
  version?: Maybe<Scalars['Int']['output']>;
};

export type McpServerInput = {
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  environmentId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  type: PlatformType;
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

export type Mutation = {
  __typename?: 'Mutation';
  _placeholder?: Maybe<Scalars['Boolean']['output']>;
  addDataTableColumn: Scalars['Boolean']['output'];
  createApiKey: Scalars['String']['output'];
  createDataTable: Scalars['Boolean']['output'];
  createMcpComponent?: Maybe<McpComponent>;
  createMcpComponentWithTools?: Maybe<McpComponent>;
  createMcpProject?: Maybe<McpProject>;
  createMcpProjectWorkflow?: Maybe<McpProjectWorkflow>;
  createMcpServer?: Maybe<McpServer>;
  createMcpTool?: Maybe<McpTool>;
  createWorkspaceApiKey: Scalars['String']['output'];
  createWorkspaceMcpServer?: Maybe<McpServer>;
  deleteApiKey: Scalars['Boolean']['output'];
  deleteDataTableRow: Scalars['Boolean']['output'];
  deleteMcpComponent?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpProject?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpProjectWorkflow?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpServer?: Maybe<Scalars['Boolean']['output']>;
  deleteSharedProject: Scalars['Boolean']['output'];
  deleteSharedWorkflow: Scalars['Boolean']['output'];
  deleteUser: Scalars['Boolean']['output'];
  deleteWorkspaceApiKey: Scalars['Boolean']['output'];
  deleteWorkspaceMcpServer?: Maybe<Scalars['Boolean']['output']>;
  dropDataTable: Scalars['Boolean']['output'];
  duplicateDataTable: Scalars['Boolean']['output'];
  exportSharedProject?: Maybe<Scalars['Boolean']['output']>;
  exportSharedWorkflow: Scalars['Boolean']['output'];
  importDataTableCsv: Scalars['Boolean']['output'];
  importProjectTemplate: Scalars['ID']['output'];
  importWorkflowTemplate: Scalars['ID']['output'];
  insertDataTableRow: DataTableRow;
  inviteUser: Scalars['Boolean']['output'];
  removeDataTableColumn: Scalars['Boolean']['output'];
  renameDataTable: Scalars['Boolean']['output'];
  renameDataTableColumn: Scalars['Boolean']['output'];
  updateApiKey: Scalars['Boolean']['output'];
  updateDataTableRow: DataTableRow;
  updateDataTableTags: Scalars['Boolean']['output'];
  updateManagementMcpServerUrl: Scalars['String']['output'];
  updateMcpComponentWithTools?: Maybe<McpComponent>;
  updateMcpProjectWorkflow?: Maybe<McpProjectWorkflow>;
  updateMcpServer?: Maybe<McpServer>;
  updateMcpServerTags?: Maybe<Array<Maybe<Tag>>>;
  updateMcpServerUrl: Scalars['String']['output'];
  updateUser: AdminUser;
  updateWorkspaceApiKey: Scalars['Boolean']['output'];
};


export type MutationAddDataTableColumnArgs = {
  input: AddColumnInput;
};


export type MutationCreateApiKeyArgs = {
  environmentId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  type?: InputMaybe<PlatformType>;
};


export type MutationCreateDataTableArgs = {
  input: CreateDataTableInput;
};


export type MutationCreateMcpComponentArgs = {
  input: McpComponentInput;
};


export type MutationCreateMcpComponentWithToolsArgs = {
  input: McpComponentWithToolsInput;
};


export type MutationCreateMcpProjectArgs = {
  input: CreateMcpProjectInput;
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


export type MutationCreateWorkspaceApiKeyArgs = {
  environmentId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationCreateWorkspaceMcpServerArgs = {
  input: CreateWorkspaceMcpServerInput;
};


export type MutationDeleteApiKeyArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteDataTableRowArgs = {
  input: DeleteRowInput;
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


export type MutationDeleteSharedProjectArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteSharedWorkflowArgs = {
  workflowId: Scalars['String']['input'];
};


export type MutationDeleteUserArgs = {
  login: Scalars['String']['input'];
};


export type MutationDeleteWorkspaceApiKeyArgs = {
  apiKeyId: Scalars['ID']['input'];
};


export type MutationDeleteWorkspaceMcpServerArgs = {
  mcpServerId: Scalars['ID']['input'];
};


export type MutationDropDataTableArgs = {
  input: RemoveTableInput;
};


export type MutationDuplicateDataTableArgs = {
  input: DuplicateDataTableInput;
};


export type MutationExportSharedProjectArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
};


export type MutationExportSharedWorkflowArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  workflowId: Scalars['String']['input'];
};


export type MutationImportDataTableCsvArgs = {
  input: ImportCsvInput;
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


export type MutationInsertDataTableRowArgs = {
  input: InsertRowInput;
};


export type MutationInviteUserArgs = {
  email: Scalars['String']['input'];
  password: Scalars['String']['input'];
  role: Scalars['String']['input'];
};


export type MutationRemoveDataTableColumnArgs = {
  input: RemoveColumnInput;
};


export type MutationRenameDataTableArgs = {
  input: RenameDataTableInput;
};


export type MutationRenameDataTableColumnArgs = {
  input: RenameColumnInput;
};


export type MutationUpdateApiKeyArgs = {
  id: Scalars['ID']['input'];
  name: Scalars['String']['input'];
};


export type MutationUpdateDataTableRowArgs = {
  input: UpdateRowInput;
};


export type MutationUpdateDataTableTagsArgs = {
  input: UpdateDataTableTagsInput;
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


export type MutationUpdateMcpServerUrlArgs = {
  id: Scalars['ID']['input'];
};


export type MutationUpdateUserArgs = {
  login: Scalars['String']['input'];
  role: Scalars['String']['input'];
};


export type MutationUpdateWorkspaceApiKeyArgs = {
  apiKeyId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
};

export type NullProperty = Property & {
  __typename?: 'NullProperty';
  advancedOption?: Maybe<Scalars['Boolean']['output']>;
  controlType: ControlType;
  description?: Maybe<Scalars['String']['output']>;
  displayCondition?: Maybe<Scalars['String']['output']>;
  expressionEnabled?: Maybe<Scalars['Boolean']['output']>;
  hidden?: Maybe<Scalars['Boolean']['output']>;
  label?: Maybe<Scalars['String']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  placeholder?: Maybe<Scalars['String']['output']>;
  required?: Maybe<Scalars['Boolean']['output']>;
  type: PropertyType;
};

export type NumberProperty = Property & {
  __typename?: 'NumberProperty';
  advancedOption?: Maybe<Scalars['Boolean']['output']>;
  controlType: ControlType;
  defaultValue?: Maybe<Scalars['Float']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  displayCondition?: Maybe<Scalars['String']['output']>;
  exampleValue?: Maybe<Scalars['Float']['output']>;
  expressionEnabled?: Maybe<Scalars['Boolean']['output']>;
  hidden?: Maybe<Scalars['Boolean']['output']>;
  label?: Maybe<Scalars['String']['output']>;
  maxValue?: Maybe<Scalars['Float']['output']>;
  minValue?: Maybe<Scalars['Float']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  options?: Maybe<Array<Option>>;
  optionsDataSource?: Maybe<OptionsDataSource>;
  placeholder?: Maybe<Scalars['String']['output']>;
  required?: Maybe<Scalars['Boolean']['output']>;
  type: PropertyType;
};

export type ObjectProperty = Property & {
  __typename?: 'ObjectProperty';
  additionalProperties?: Maybe<Array<Property>>;
  advancedOption?: Maybe<Scalars['Boolean']['output']>;
  controlType: ControlType;
  defaultValue?: Maybe<Scalars['Map']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  displayCondition?: Maybe<Scalars['String']['output']>;
  exampleValue?: Maybe<Scalars['Map']['output']>;
  expressionEnabled?: Maybe<Scalars['Boolean']['output']>;
  hidden?: Maybe<Scalars['Boolean']['output']>;
  label?: Maybe<Scalars['String']['output']>;
  multipleValues?: Maybe<Scalars['Boolean']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  options?: Maybe<Array<Option>>;
  optionsDataSource?: Maybe<OptionsDataSource>;
  placeholder?: Maybe<Scalars['String']['output']>;
  properties?: Maybe<Array<Property>>;
  required?: Maybe<Scalars['Boolean']['output']>;
  type: PropertyType;
};

export type Option = {
  __typename?: 'Option';
  description?: Maybe<Scalars['String']['output']>;
  label?: Maybe<Scalars['String']['output']>;
  value?: Maybe<Scalars['Map']['output']>;
};

export type OptionsDataSource = {
  __typename?: 'OptionsDataSource';
  optionsLookupDependsOn?: Maybe<Array<Scalars['String']['output']>>;
};

export enum PlatformType {
  Automation = 'AUTOMATION',
  Embedded = 'EMBEDDED'
}

export type Project = {
  __typename?: 'Project';
  category?: Maybe<Category>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  tags?: Maybe<Array<Maybe<Tag>>>;
};

export type ProjectDeployment = {
  __typename?: 'ProjectDeployment';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['String']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  enabled: Scalars['Boolean']['output'];
  environment: Environment;
  id: Scalars['ID']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  project: Project;
  projectDeploymentWorkflows: Array<ProjectDeploymentWorkflow>;
  projectId: Scalars['ID']['output'];
  projectVersion: Scalars['Int']['output'];
  tags?: Maybe<Array<Maybe<Tag>>>;
  version: Scalars['Int']['output'];
};

export type ProjectDeploymentWorkflow = {
  __typename?: 'ProjectDeploymentWorkflow';
  connections: Array<ProjectDeploymentWorkflowConnection>;
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['String']['output']>;
  enabled: Scalars['Boolean']['output'];
  id: Scalars['ID']['output'];
  inputs?: Maybe<Scalars['Map']['output']>;
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['String']['output']>;
  projectDeploymentId: Scalars['ID']['output'];
  projectWorkflow: ProjectWorkflow;
  staticWebhookUrl?: Maybe<Scalars['String']['output']>;
  version: Scalars['Int']['output'];
  workflowExecutionId?: Maybe<Scalars['String']['output']>;
  workflowId: Scalars['String']['output'];
};

export type ProjectDeploymentWorkflowConnection = {
  __typename?: 'ProjectDeploymentWorkflowConnection';
  connectionId?: Maybe<Scalars['ID']['output']>;
  key: Scalars['String']['output'];
  workflowNodeName: Scalars['String']['output'];
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

export type ProjectWorkflow = {
  __typename?: 'ProjectWorkflow';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['String']['output']>;
  projectId: Scalars['ID']['output'];
  projectVersion: Scalars['Int']['output'];
  sseStreamResponse: Scalars['Boolean']['output'];
  uuid?: Maybe<Scalars['String']['output']>;
  version: Scalars['Int']['output'];
  workflow: Workflow;
  workflowId: Scalars['String']['output'];
};

export type PropertiesDataSource = {
  __typename?: 'PropertiesDataSource';
  propertiesLookupDependsOn?: Maybe<Array<Scalars['String']['output']>>;
};

export type Property = {
  advancedOption?: Maybe<Scalars['Boolean']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  displayCondition?: Maybe<Scalars['String']['output']>;
  expressionEnabled?: Maybe<Scalars['Boolean']['output']>;
  hidden?: Maybe<Scalars['Boolean']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  required?: Maybe<Scalars['Boolean']['output']>;
  type: PropertyType;
};

export enum PropertyType {
  Array = 'ARRAY',
  Boolean = 'BOOLEAN',
  Date = 'DATE',
  DateTime = 'DATE_TIME',
  DynamicProperties = 'DYNAMIC_PROPERTIES',
  FileEntry = 'FILE_ENTRY',
  Integer = 'INTEGER',
  Null = 'NULL',
  Number = 'NUMBER',
  Object = 'OBJECT',
  String = 'STRING',
  Task = 'TASK',
  Time = 'TIME'
}

export type Query = {
  __typename?: 'Query';
  _placeholder?: Maybe<Scalars['Boolean']['output']>;
  actionDefinition: ActionDefinition;
  actionDefinitions: Array<ActionDefinition>;
  adminApiKeys?: Maybe<Array<Maybe<ApiKey>>>;
  apiKey?: Maybe<ApiKey>;
  apiKeys?: Maybe<Array<Maybe<ApiKey>>>;
  authorities: Array<Scalars['String']['output']>;
  clusterElementDefinition: ClusterElementDefinition;
  clusterElementDefinitions: Array<ClusterElementDefinition>;
  componentDefinition: ComponentDefinition;
  componentDefinitionSearch: Array<ComponentDefinition>;
  componentDefinitionVersions: Array<ComponentDefinition>;
  componentDefinitions: Array<ComponentDefinition>;
  connectedUser?: Maybe<ConnectedUser>;
  connectedUserProjects: Array<ConnectedUserProject>;
  connectedUsers?: Maybe<ConnectedUserPage>;
  connectionComponentDefinition: ComponentDefinition;
  connectionDefinition: ConnectionDefinition;
  connectionDefinitions: Array<ConnectionDefinition>;
  dataTableRows: Array<DataTableRow>;
  dataTableRowsPage: DataTableRowPage;
  dataTableTags: Array<Tag>;
  dataTableTagsByTable: Array<DataTableTagsEntry>;
  dataTableWebhooks: Array<DataTableWebhook>;
  dataTables: Array<DataTable>;
  environments?: Maybe<Array<Maybe<Environment>>>;
  exportDataTableCsv: Scalars['String']['output'];
  integration?: Maybe<Integration>;
  managementMcpServerUrl?: Maybe<Scalars['String']['output']>;
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
  preBuiltProjectTemplates: Array<ProjectTemplate>;
  preBuiltWorkflowTemplates: Array<WorkflowTemplate>;
  project?: Maybe<Project>;
  projectDeploymentWorkflow?: Maybe<ProjectDeploymentWorkflow>;
  projectTemplate?: Maybe<ProjectTemplate>;
  projects?: Maybe<Array<Maybe<Project>>>;
  sharedProject?: Maybe<SharedProject>;
  sharedWorkflow?: Maybe<SharedWorkflow>;
  taskDispatcherDefinition: TaskDispatcherDefinition;
  taskDispatcherDefinitionVersions: Array<TaskDispatcherDefinition>;
  taskDispatcherDefinitions: Array<TaskDispatcherDefinition>;
  triggerDefinition: TriggerDefinition;
  triggerDefinitions: Array<TriggerDefinition>;
  unifiedApiComponentDefinitions: Array<ComponentDefinition>;
  user?: Maybe<AdminUser>;
  users?: Maybe<AdminUserPage>;
  workflowTemplate?: Maybe<WorkflowTemplate>;
  workspaceApiKeys: Array<ApiKey>;
  workspaceMcpServers?: Maybe<Array<Maybe<McpServer>>>;
  workspaceProjectDeployments: Array<ProjectDeployment>;
};


export type QueryActionDefinitionArgs = {
  actionName: Scalars['String']['input'];
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
};


export type QueryActionDefinitionsArgs = {
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
};


export type QueryAdminApiKeysArgs = {
  environmentId: Scalars['ID']['input'];
};


export type QueryApiKeyArgs = {
  id: Scalars['ID']['input'];
};


export type QueryApiKeysArgs = {
  environmentId: Scalars['ID']['input'];
  type: PlatformType;
};


export type QueryClusterElementDefinitionArgs = {
  clusterElementName: Scalars['String']['input'];
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
};


export type QueryClusterElementDefinitionsArgs = {
  clusterElementType: Scalars['String']['input'];
  rootComponentName: Scalars['String']['input'];
  rootComponentVersion: Scalars['Int']['input'];
};


export type QueryComponentDefinitionArgs = {
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
};


export type QueryComponentDefinitionSearchArgs = {
  query: Scalars['String']['input'];
};


export type QueryComponentDefinitionVersionsArgs = {
  componentName: Scalars['String']['input'];
};


export type QueryComponentDefinitionsArgs = {
  actionDefinitions?: InputMaybe<Scalars['Boolean']['input']>;
  connectionDefinitions?: InputMaybe<Scalars['Boolean']['input']>;
  include?: InputMaybe<Array<InputMaybe<Scalars['String']['input']>>>;
  triggerDefinitions?: InputMaybe<Scalars['Boolean']['input']>;
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


export type QueryConnectionComponentDefinitionArgs = {
  componentName: Scalars['String']['input'];
  connectionVersion: Scalars['Int']['input'];
};


export type QueryConnectionDefinitionArgs = {
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
};


export type QueryConnectionDefinitionsArgs = {
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
};


export type QueryDataTableRowsArgs = {
  environmentId: Scalars['ID']['input'];
  tableId: Scalars['ID']['input'];
};


export type QueryDataTableRowsPageArgs = {
  environmentId: Scalars['ID']['input'];
  limit?: InputMaybe<Scalars['Int']['input']>;
  offset?: InputMaybe<Scalars['Int']['input']>;
  tableId: Scalars['ID']['input'];
};


export type QueryDataTableWebhooksArgs = {
  environmentId: Scalars['ID']['input'];
  tableId: Scalars['ID']['input'];
};


export type QueryDataTablesArgs = {
  environmentId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryExportDataTableCsvArgs = {
  environmentId: Scalars['ID']['input'];
  tableId: Scalars['ID']['input'];
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
  type: PlatformType;
};


export type QueryMcpServersArgs = {
  orderBy?: InputMaybe<McpServerOrderBy>;
  type: PlatformType;
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


export type QueryProjectDeploymentWorkflowArgs = {
  id: Scalars['String']['input'];
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


export type QueryTaskDispatcherDefinitionArgs = {
  name: Scalars['String']['input'];
  version: Scalars['Int']['input'];
};


export type QueryTaskDispatcherDefinitionVersionsArgs = {
  name: Scalars['String']['input'];
};


export type QueryTriggerDefinitionArgs = {
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
  triggerName: Scalars['String']['input'];
};


export type QueryTriggerDefinitionsArgs = {
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
};


export type QueryUnifiedApiComponentDefinitionsArgs = {
  category: UnifiedApiCategory;
};


export type QueryUserArgs = {
  login: Scalars['String']['input'];
};


export type QueryUsersArgs = {
  pageNumber?: InputMaybe<Scalars['Int']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryWorkflowTemplateArgs = {
  id: Scalars['String']['input'];
  sharedWorkflow: Scalars['Boolean']['input'];
};


export type QueryWorkspaceApiKeysArgs = {
  environmentId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryWorkspaceMcpServersArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryWorkspaceProjectDeploymentsArgs = {
  environmentId: Scalars['ID']['input'];
  projectId?: InputMaybe<Scalars['ID']['input']>;
  tagId?: InputMaybe<Scalars['ID']['input']>;
  workspaceId: Scalars['ID']['input'];
};

export type RemoveColumnInput = {
  columnId: Scalars['ID']['input'];
  environmentId: Scalars['ID']['input'];
  tableId: Scalars['ID']['input'];
};

export type RemoveTableInput = {
  environmentId: Scalars['ID']['input'];
  tableId: Scalars['ID']['input'];
};

export type RenameColumnInput = {
  columnId: Scalars['ID']['input'];
  environmentId: Scalars['ID']['input'];
  newName: Scalars['String']['input'];
  tableId: Scalars['ID']['input'];
};

export type RenameDataTableInput = {
  environmentId: Scalars['ID']['input'];
  newBaseName: Scalars['String']['input'];
  tableId: Scalars['ID']['input'];
};

export type Resources = {
  __typename?: 'Resources';
  documentationUrl?: Maybe<Scalars['String']['output']>;
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

export type StringProperty = Property & {
  __typename?: 'StringProperty';
  advancedOption?: Maybe<Scalars['Boolean']['output']>;
  controlType: ControlType;
  defaultValue?: Maybe<Scalars['String']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  displayCondition?: Maybe<Scalars['String']['output']>;
  exampleValue?: Maybe<Scalars['String']['output']>;
  expressionEnabled?: Maybe<Scalars['Boolean']['output']>;
  hidden?: Maybe<Scalars['Boolean']['output']>;
  label?: Maybe<Scalars['String']['output']>;
  languageId?: Maybe<Scalars['String']['output']>;
  maxLength?: Maybe<Scalars['Int']['output']>;
  minLength?: Maybe<Scalars['Int']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  options?: Maybe<Array<Option>>;
  optionsDataSource?: Maybe<OptionsDataSource>;
  optionsLoadedDynamically?: Maybe<Scalars['Boolean']['output']>;
  placeholder?: Maybe<Scalars['String']['output']>;
  regex?: Maybe<Scalars['String']['output']>;
  required?: Maybe<Scalars['Boolean']['output']>;
  type: PropertyType;
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

export type TaskDispatcherDefinition = {
  __typename?: 'TaskDispatcherDefinition';
  description?: Maybe<Scalars['String']['output']>;
  icon?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  outputDefined: Scalars['Boolean']['output'];
  outputFunctionDefined?: Maybe<Scalars['Boolean']['output']>;
  outputSchemaDefined?: Maybe<Scalars['Boolean']['output']>;
  properties: Array<Property>;
  resources?: Maybe<Resources>;
  taskProperties: Array<Property>;
  title?: Maybe<Scalars['String']['output']>;
  variablePropertiesDefined?: Maybe<Scalars['Boolean']['output']>;
  version: Scalars['Int']['output'];
};

export type TimeProperty = Property & {
  __typename?: 'TimeProperty';
  advancedOption?: Maybe<Scalars['Boolean']['output']>;
  controlType: ControlType;
  defaultValue?: Maybe<Scalars['String']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  displayCondition?: Maybe<Scalars['String']['output']>;
  exampleValue?: Maybe<Scalars['String']['output']>;
  expressionEnabled?: Maybe<Scalars['Boolean']['output']>;
  hidden?: Maybe<Scalars['Boolean']['output']>;
  label?: Maybe<Scalars['String']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  placeholder?: Maybe<Scalars['String']['output']>;
  required?: Maybe<Scalars['Boolean']['output']>;
  type: PropertyType;
};

export type TriggerDefinition = {
  __typename?: 'TriggerDefinition';
  componentName?: Maybe<Scalars['String']['output']>;
  componentVersion?: Maybe<Scalars['Int']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  help?: Maybe<Help>;
  name: Scalars['String']['output'];
  outputDefined: Scalars['Boolean']['output'];
  outputFunctionDefined: Scalars['Boolean']['output'];
  outputSchemaDefined?: Maybe<Scalars['Boolean']['output']>;
  properties: Array<Property>;
  title?: Maybe<Scalars['String']['output']>;
  type: TriggerType;
  workflowNodeDescriptionDefined?: Maybe<Scalars['Boolean']['output']>;
};

export enum TriggerType {
  DynamicWebhook = 'DYNAMIC_WEBHOOK',
  Hybrid = 'HYBRID',
  Listener = 'LISTENER',
  Polling = 'POLLING',
  StaticWebhook = 'STATIC_WEBHOOK'
}

export enum UnifiedApiCategory {
  Accounting = 'ACCOUNTING',
  Ats = 'ATS',
  Crm = 'CRM',
  ECommerce = 'E_COMMERCE',
  FileStorage = 'FILE_STORAGE',
  Hris = 'HRIS',
  MarketingAutomation = 'MARKETING_AUTOMATION',
  Ticketing = 'TICKETING'
}

export type UpdateDataTableTagsInput = {
  tableId: Scalars['ID']['input'];
  tags?: InputMaybe<Array<TagInput>>;
};

export type UpdateRowInput = {
  environmentId: Scalars['ID']['input'];
  id: Scalars['ID']['input'];
  tableId: Scalars['ID']['input'];
  values: Scalars['Map']['input'];
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
  triggers: Array<WorkflowTrigger>;
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

export type WorkflowTrigger = {
  __typename?: 'WorkflowTrigger';
  description?: Maybe<Scalars['String']['output']>;
  label?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  parameters?: Maybe<Scalars['Map']['output']>;
  type: Scalars['String']['output'];
};

export type CreateMcpProjectMutationVariables = Exact<{
  input: CreateMcpProjectInput;
}>;


export type CreateMcpProjectMutation = { __typename?: 'Mutation', createMcpProject?: { __typename?: 'McpProject', id: string, mcpServerId: string, projectDeploymentId: string, projectVersion?: number | null } | null };

export type CreateWorkspaceApiKeyMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  environmentId: Scalars['ID']['input'];
}>;


export type CreateWorkspaceApiKeyMutation = { __typename?: 'Mutation', createWorkspaceApiKey: string };

export type CreateMcpServerMutationVariables = Exact<{
  input: CreateWorkspaceMcpServerInput;
}>;


export type CreateMcpServerMutation = { __typename?: 'Mutation', createWorkspaceMcpServer?: { __typename?: 'McpServer', id: string, name: string, type: PlatformType, environmentId: string, enabled: boolean } | null };

export type DeleteMcpProjectMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteMcpProjectMutation = { __typename?: 'Mutation', deleteMcpProject?: boolean | null };

export type DeleteSharedProjectMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteSharedProjectMutation = { __typename?: 'Mutation', deleteSharedProject: boolean };

export type DeleteSharedWorkflowMutationVariables = Exact<{
  workflowId: Scalars['String']['input'];
}>;


export type DeleteSharedWorkflowMutation = { __typename?: 'Mutation', deleteSharedWorkflow: boolean };

export type DeleteWorkspaceApiKeyMutationVariables = Exact<{
  apiKeyId: Scalars['ID']['input'];
}>;


export type DeleteWorkspaceApiKeyMutation = { __typename?: 'Mutation', deleteWorkspaceApiKey: boolean };

export type DeleteWorkspaceMcpServerMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteWorkspaceMcpServerMutation = { __typename?: 'Mutation', deleteWorkspaceMcpServer?: boolean | null };

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


export type McpProjectsByServerIdQuery = { __typename?: 'Query', mcpProjectsByServerId?: Array<{ __typename?: 'McpProject', id: string, projectDeploymentId: string, mcpServerId: string, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null, projectVersion?: number | null, project?: { __typename?: 'Project', id: string, name: string, category?: { __typename?: 'Category', id?: string | null, name?: string | null } | null, tags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null } | null, mcpProjectWorkflows?: Array<{ __typename?: 'McpProjectWorkflow', id: string, mcpProjectId: any, projectDeploymentWorkflowId: any, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null, projectDeploymentWorkflow?: { __typename?: 'ProjectDeploymentWorkflow', id: string, projectDeploymentId: string, inputs?: any | null, workflowId: string } | null, workflow?: { __typename?: 'Workflow', id: string, label: string } | null } | null> | null } | null> | null };

export type PreBuiltProjectTemplatesQueryVariables = Exact<{
  query?: InputMaybe<Scalars['String']['input']>;
  category?: InputMaybe<Scalars['String']['input']>;
}>;


export type PreBuiltProjectTemplatesQuery = { __typename?: 'Query', preBuiltProjectTemplates: Array<{ __typename?: 'ProjectTemplate', authorName?: string | null, categories: Array<string>, description?: string | null, id?: string | null, projectVersion?: number | null, publicUrl?: string | null, components: Array<{ __typename?: 'ComponentDefinitionTuple', key?: string | null, value: Array<{ __typename?: 'ComponentDefinition', icon?: string | null, name: string, title?: string | null, version?: number | null, connection?: { __typename?: 'ConnectionDefinition', version: number } | null } | null> }>, project?: { __typename?: 'ProjectInfo', name: string, description?: string | null } | null, workflows: Array<{ __typename?: 'WorkflowInfo', id: string, label: string }> }> };

export type PreBuiltWorkflowTemplatesQueryVariables = Exact<{
  query?: InputMaybe<Scalars['String']['input']>;
  category?: InputMaybe<Scalars['String']['input']>;
}>;


export type PreBuiltWorkflowTemplatesQuery = { __typename?: 'Query', preBuiltWorkflowTemplates: Array<{ __typename?: 'WorkflowTemplate', authorName?: string | null, categories: Array<string>, description?: string | null, id?: string | null, projectVersion?: number | null, publicUrl?: string | null, components: Array<{ __typename?: 'ComponentDefinition', icon?: string | null, name: string, title?: string | null, version?: number | null, connection?: { __typename?: 'ConnectionDefinition', version: number } | null }>, workflow: { __typename?: 'SharedWorkflowInfo', label: string, description?: string | null } }> };

export type ProjectByIdQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type ProjectByIdQuery = { __typename?: 'Query', project?: { __typename?: 'Project', id: string, name: string } | null };

export type ProjectTemplateQueryVariables = Exact<{
  id: Scalars['String']['input'];
  sharedProject: Scalars['Boolean']['input'];
}>;


export type ProjectTemplateQuery = { __typename?: 'Query', projectTemplate?: { __typename?: 'ProjectTemplate', description?: string | null, projectVersion?: number | null, publicUrl?: string | null, components: Array<{ __typename?: 'ComponentDefinitionTuple', key?: string | null, value: Array<{ __typename?: 'ComponentDefinition', icon?: string | null, name: string, title?: string | null, version?: number | null, connection?: { __typename?: 'ConnectionDefinition', componentName: string, version: number } | null } | null> }>, project?: { __typename?: 'ProjectInfo', name: string } | null, workflows: Array<{ __typename?: 'WorkflowInfo', id: string, label: string }> } | null };

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

export type UpdateWorkspaceApiKeyMutationVariables = Exact<{
  apiKeyId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
}>;


export type UpdateWorkspaceApiKeyMutation = { __typename?: 'Mutation', updateWorkspaceApiKey: boolean };

export type WorkflowChatProjectDeploymentWorkflowQueryVariables = Exact<{
  id: Scalars['String']['input'];
}>;


export type WorkflowChatProjectDeploymentWorkflowQuery = { __typename?: 'Query', projectDeploymentWorkflow?: { __typename?: 'ProjectDeploymentWorkflow', projectWorkflow: { __typename?: 'ProjectWorkflow', sseStreamResponse: boolean, workflow: { __typename?: 'Workflow', label: string } } } | null };

export type WorkflowChatWorkspaceProjectDeploymentsQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  environmentId: Scalars['ID']['input'];
  projectId?: InputMaybe<Scalars['ID']['input']>;
  tagId?: InputMaybe<Scalars['ID']['input']>;
}>;


export type WorkflowChatWorkspaceProjectDeploymentsQuery = { __typename?: 'Query', workspaceProjectDeployments: Array<{ __typename?: 'ProjectDeployment', id: string, enabled: boolean, project: { __typename?: 'Project', id: string, name: string }, projectDeploymentWorkflows: Array<{ __typename?: 'ProjectDeploymentWorkflow', id: string, enabled: boolean, staticWebhookUrl?: string | null, workflowExecutionId?: string | null, projectWorkflow: { __typename?: 'ProjectWorkflow', workflow: { __typename?: 'Workflow', id: string, label: string, triggers: Array<{ __typename?: 'WorkflowTrigger', parameters?: any | null, type: string }> } } }> }> };

export type WorkflowTemplateQueryVariables = Exact<{
  id: Scalars['String']['input'];
  sharedWorkflow: Scalars['Boolean']['input'];
}>;


export type WorkflowTemplateQuery = { __typename?: 'Query', workflowTemplate?: { __typename?: 'WorkflowTemplate', description?: string | null, projectVersion?: number | null, publicUrl?: string | null, workflow: { __typename?: 'SharedWorkflowInfo', label: string }, components: Array<{ __typename?: 'ComponentDefinition', icon?: string | null, name: string, title?: string | null, version?: number | null, connection?: { __typename?: 'ConnectionDefinition', componentName: string, version: number } | null }> } | null };

export type WorkspaceApiKeysQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  environmentId: Scalars['ID']['input'];
}>;


export type WorkspaceApiKeysQuery = { __typename?: 'Query', workspaceApiKeys: Array<{ __typename?: 'ApiKey', id?: string | null, name?: string | null, secretKey?: string | null, lastUsedDate?: any | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null }> };

export type WorkspaceMcpServersQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type WorkspaceMcpServersQuery = { __typename?: 'Query', workspaceMcpServers?: Array<{ __typename?: 'McpServer', id: string, name: string, type: PlatformType, environmentId: string, enabled: boolean, url: string, lastModifiedDate?: any | null, mcpComponents?: Array<{ __typename?: 'McpComponent', id: string, mcpServerId: string, componentName: string, componentVersion: number } | null> | null, tags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null } | null> | null };

export type AddDataTableColumnMutationVariables = Exact<{
  input: AddColumnInput;
}>;


export type AddDataTableColumnMutation = { __typename?: 'Mutation', addDataTableColumn: boolean };

export type CreateDataTableMutationVariables = Exact<{
  input: CreateDataTableInput;
}>;


export type CreateDataTableMutation = { __typename?: 'Mutation', createDataTable: boolean };

export type DataTableRowsQueryVariables = Exact<{
  environmentId: Scalars['ID']['input'];
  tableId: Scalars['ID']['input'];
}>;


export type DataTableRowsQuery = { __typename?: 'Query', dataTableRows: Array<{ __typename?: 'DataTableRow', id: string, values: any }> };

export type DataTableRowsPageQueryVariables = Exact<{
  environmentId: Scalars['ID']['input'];
  tableId: Scalars['ID']['input'];
  limit?: InputMaybe<Scalars['Int']['input']>;
  offset?: InputMaybe<Scalars['Int']['input']>;
}>;


export type DataTableRowsPageQuery = { __typename?: 'Query', dataTableRowsPage: { __typename?: 'DataTableRowPage', hasMore: boolean, nextOffset?: number | null, items: Array<{ __typename?: 'DataTableRow', id: string, values: any }> } };

export type DataTableTagsQueryVariables = Exact<{ [key: string]: never; }>;


export type DataTableTagsQuery = { __typename?: 'Query', dataTableTags: Array<{ __typename?: 'Tag', id: string, name: string }> };

export type DataTableTagsByTableQueryVariables = Exact<{ [key: string]: never; }>;


export type DataTableTagsByTableQuery = { __typename?: 'Query', dataTableTagsByTable: Array<{ __typename?: 'DataTableTagsEntry', tableId: string, tags: Array<{ __typename?: 'Tag', id: string, name: string }> }> };

export type DataTablesQueryVariables = Exact<{
  environmentId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
}>;


export type DataTablesQuery = { __typename?: 'Query', dataTables: Array<{ __typename?: 'DataTable', id: string, baseName: string, lastModifiedDate?: any | null, columns: Array<{ __typename?: 'DataTableColumn', id: string, name: string, type: ColumnType }> }> };

export type DeleteDataTableRowMutationVariables = Exact<{
  input: DeleteRowInput;
}>;


export type DeleteDataTableRowMutation = { __typename?: 'Mutation', deleteDataTableRow: boolean };

export type DropDataTableMutationVariables = Exact<{
  input: RemoveTableInput;
}>;


export type DropDataTableMutation = { __typename?: 'Mutation', dropDataTable: boolean };

export type DuplicateDataTableMutationVariables = Exact<{
  input: DuplicateDataTableInput;
}>;


export type DuplicateDataTableMutation = { __typename?: 'Mutation', duplicateDataTable: boolean };

export type ExportDataTableCsvQueryVariables = Exact<{
  environmentId: Scalars['ID']['input'];
  tableId: Scalars['ID']['input'];
}>;


export type ExportDataTableCsvQuery = { __typename?: 'Query', exportDataTableCsv: string };

export type ImportDataTableCsvMutationVariables = Exact<{
  input: ImportCsvInput;
}>;


export type ImportDataTableCsvMutation = { __typename?: 'Mutation', importDataTableCsv: boolean };

export type InsertDataTableRowMutationVariables = Exact<{
  input: InsertRowInput;
}>;


export type InsertDataTableRowMutation = { __typename?: 'Mutation', insertDataTableRow: { __typename?: 'DataTableRow', id: string, values: any } };

export type RemoveDataTableColumnMutationVariables = Exact<{
  input: RemoveColumnInput;
}>;


export type RemoveDataTableColumnMutation = { __typename?: 'Mutation', removeDataTableColumn: boolean };

export type RenameDataTableMutationVariables = Exact<{
  input: RenameDataTableInput;
}>;


export type RenameDataTableMutation = { __typename?: 'Mutation', renameDataTable: boolean };

export type RenameDataTableColumnMutationVariables = Exact<{
  input: RenameColumnInput;
}>;


export type RenameDataTableColumnMutation = { __typename?: 'Mutation', renameDataTableColumn: boolean };

export type UpdateDataTableRowMutationVariables = Exact<{
  input: UpdateRowInput;
}>;


export type UpdateDataTableRowMutation = { __typename?: 'Mutation', updateDataTableRow: { __typename?: 'DataTableRow', id: string, values: any } };

export type UpdateDataTableTagsMutationVariables = Exact<{
  input: UpdateDataTableTagsInput;
}>;


export type UpdateDataTableTagsMutation = { __typename?: 'Mutation', updateDataTableTags: boolean };

export type ConnectedUserProjectsQueryVariables = Exact<{
  connectedUserId?: InputMaybe<Scalars['ID']['input']>;
  environmentId?: InputMaybe<Scalars['ID']['input']>;
}>;


export type ConnectedUserProjectsQuery = { __typename?: 'Query', connectedUserProjects: Array<{ __typename?: 'ConnectedUserProject', id: string, environmentId: string, lastExecutionDate?: string | null, projectId: string, projectVersion?: number | null, connectedUser: { __typename?: 'ConnectedUser', id: string, environmentId: string, externalId: string }, connectedUserProjectWorkflows: Array<{ __typename?: 'ConnectedUserProjectWorkflow', id: string, connectedUserId: string, enabled: boolean, lastExecutionDate?: string | null, projectId: string, workflowUuid: string, workflowVersion: number, workflow: { __typename?: 'Workflow', id: string, label: string, triggers: Array<{ __typename?: 'WorkflowTrigger', name: string, type: string, parameters?: any | null }> } }> }> };

export type IntegrationByIdQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type IntegrationByIdQuery = { __typename?: 'Query', integration?: { __typename?: 'Integration', id: string, name: string } | null };

export type AdminApiKeysQueryVariables = Exact<{
  environmentId: Scalars['ID']['input'];
}>;


export type AdminApiKeysQuery = { __typename?: 'Query', adminApiKeys?: Array<{ __typename?: 'ApiKey', id?: string | null, name?: string | null, secretKey?: string | null, lastUsedDate?: any | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null } | null> | null };

export type ApiKeysQueryVariables = Exact<{
  environmentId: Scalars['ID']['input'];
  type: PlatformType;
}>;


export type ApiKeysQuery = { __typename?: 'Query', apiKeys?: Array<{ __typename?: 'ApiKey', id?: string | null, name?: string | null, secretKey?: string | null, lastUsedDate?: any | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null } | null> | null };

export type CreateApiKeyMutationVariables = Exact<{
  name: Scalars['String']['input'];
  environmentId: Scalars['ID']['input'];
  type?: InputMaybe<PlatformType>;
}>;


export type CreateApiKeyMutation = { __typename?: 'Mutation', createApiKey: string };

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

export type DeleteApiKeyMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteApiKeyMutation = { __typename?: 'Mutation', deleteApiKey: boolean };

export type DeleteMcpComponentMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteMcpComponentMutation = { __typename?: 'Mutation', deleteMcpComponent?: boolean | null };

export type EnvironmentsQueryVariables = Exact<{ [key: string]: never; }>;


export type EnvironmentsQuery = { __typename?: 'Query', environments?: Array<{ __typename?: 'Environment', id: string, name: string } | null> | null };

export type ManagementMcpServerUrlQueryVariables = Exact<{ [key: string]: never; }>;


export type ManagementMcpServerUrlQuery = { __typename?: 'Query', managementMcpServerUrl?: string | null };

export type McpComponentsByServerIdQueryVariables = Exact<{
  mcpServerId: Scalars['ID']['input'];
}>;


export type McpComponentsByServerIdQuery = { __typename?: 'Query', mcpComponentsByServerId?: Array<{ __typename?: 'McpComponent', id: string, componentName: string, componentVersion: number, connectionId?: string | null, mcpServerId: string, version?: number | null, mcpTools?: Array<{ __typename?: 'McpTool', id: string, mcpComponentId: string, name: string } | null> | null } | null> | null };

export type McpServerTagsQueryVariables = Exact<{
  type: PlatformType;
}>;


export type McpServerTagsQuery = { __typename?: 'Query', mcpServerTags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null };

export type McpServersQueryVariables = Exact<{
  type: PlatformType;
}>;


export type McpServersQuery = { __typename?: 'Query', mcpServers?: Array<{ __typename?: 'McpServer', id: string, name: string, type: PlatformType, environmentId: string, enabled: boolean, secretKey: string, lastModifiedDate?: any | null, mcpComponents?: Array<{ __typename?: 'McpComponent', id: string, mcpServerId: string, componentName: string, componentVersion: number } | null> | null, tags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null } | null> | null };

export type McpToolsByComponentIdQueryVariables = Exact<{
  mcpComponentId: Scalars['ID']['input'];
}>;


export type McpToolsByComponentIdQuery = { __typename?: 'Query', mcpToolsByComponentId?: Array<{ __typename?: 'McpTool', id: string, name: string, mcpComponentId: string, parameters?: any | null } | null> | null };

export type UpdateApiKeyMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  name: Scalars['String']['input'];
}>;


export type UpdateApiKeyMutation = { __typename?: 'Mutation', updateApiKey: boolean };

export type UpdateManagementMcpServerUrlMutationVariables = Exact<{ [key: string]: never; }>;


export type UpdateManagementMcpServerUrlMutation = { __typename?: 'Mutation', updateManagementMcpServerUrl: string };

export type UpdateMcpComponentWithToolsMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: McpComponentWithToolsInput;
}>;


export type UpdateMcpComponentWithToolsMutation = { __typename?: 'Mutation', updateMcpComponentWithTools?: { __typename?: 'McpComponent', id: string, componentName: string, componentVersion: number, mcpServerId: string, connectionId?: string | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null } | null };

export type UpdateMcpServerUrlMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type UpdateMcpServerUrlMutation = { __typename?: 'Mutation', updateMcpServerUrl: string };

export type AuthoritiesQueryVariables = Exact<{ [key: string]: never; }>;


export type AuthoritiesQuery = { __typename?: 'Query', authorities: Array<string> };

export type DeleteUserMutationVariables = Exact<{
  login: Scalars['String']['input'];
}>;


export type DeleteUserMutation = { __typename?: 'Mutation', deleteUser: boolean };

export type InviteUserMutationVariables = Exact<{
  email: Scalars['String']['input'];
  password: Scalars['String']['input'];
  role: Scalars['String']['input'];
}>;


export type InviteUserMutation = { __typename?: 'Mutation', inviteUser: boolean };

export type UpdateUserMutationVariables = Exact<{
  login: Scalars['String']['input'];
  role: Scalars['String']['input'];
}>;


export type UpdateUserMutation = { __typename?: 'Mutation', updateUser: { __typename?: 'AdminUser', id?: string | null, login?: string | null, email?: string | null, firstName?: string | null, lastName?: string | null, activated?: boolean | null, authorities?: Array<string | null> | null } };

export type UsersQueryVariables = Exact<{
  pageNumber?: InputMaybe<Scalars['Int']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
}>;


export type UsersQuery = { __typename?: 'Query', users?: { __typename?: 'AdminUserPage', number: number, size: number, totalElements: number, totalPages: number, content: Array<{ __typename?: 'AdminUser', id?: string | null, login?: string | null, email?: string | null, firstName?: string | null, lastName?: string | null, activated?: boolean | null, authorities?: Array<string | null> | null } | null> } | null };



export const CreateMcpProjectDocument = `
    mutation createMcpProject($input: CreateMcpProjectInput!) {
  createMcpProject(input: $input) {
    id
    mcpServerId
    projectDeploymentId
    projectVersion
  }
}
    `;

export const useCreateMcpProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateMcpProjectMutation, TError, CreateMcpProjectMutationVariables, TContext>) => {
    
    return useMutation<CreateMcpProjectMutation, TError, CreateMcpProjectMutationVariables, TContext>(
      {
    mutationKey: ['createMcpProject'],
    mutationFn: (variables?: CreateMcpProjectMutationVariables) => fetcher<CreateMcpProjectMutation, CreateMcpProjectMutationVariables>(CreateMcpProjectDocument, variables)(),
    ...options
  }
    )};

export const CreateWorkspaceApiKeyDocument = `
    mutation createWorkspaceApiKey($workspaceId: ID!, $name: String!, $environmentId: ID!) {
  createWorkspaceApiKey(
    workspaceId: $workspaceId
    name: $name
    environmentId: $environmentId
  )
}
    `;

export const useCreateWorkspaceApiKeyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateWorkspaceApiKeyMutation, TError, CreateWorkspaceApiKeyMutationVariables, TContext>) => {
    
    return useMutation<CreateWorkspaceApiKeyMutation, TError, CreateWorkspaceApiKeyMutationVariables, TContext>(
      {
    mutationKey: ['createWorkspaceApiKey'],
    mutationFn: (variables?: CreateWorkspaceApiKeyMutationVariables) => fetcher<CreateWorkspaceApiKeyMutation, CreateWorkspaceApiKeyMutationVariables>(CreateWorkspaceApiKeyDocument, variables)(),
    ...options
  }
    )};

export const CreateMcpServerDocument = `
    mutation createMcpServer($input: CreateWorkspaceMcpServerInput!) {
  createWorkspaceMcpServer(input: $input) {
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

export const DeleteWorkspaceApiKeyDocument = `
    mutation deleteWorkspaceApiKey($apiKeyId: ID!) {
  deleteWorkspaceApiKey(apiKeyId: $apiKeyId)
}
    `;

export const useDeleteWorkspaceApiKeyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteWorkspaceApiKeyMutation, TError, DeleteWorkspaceApiKeyMutationVariables, TContext>) => {
    
    return useMutation<DeleteWorkspaceApiKeyMutation, TError, DeleteWorkspaceApiKeyMutationVariables, TContext>(
      {
    mutationKey: ['deleteWorkspaceApiKey'],
    mutationFn: (variables?: DeleteWorkspaceApiKeyMutationVariables) => fetcher<DeleteWorkspaceApiKeyMutation, DeleteWorkspaceApiKeyMutationVariables>(DeleteWorkspaceApiKeyDocument, variables)(),
    ...options
  }
    )};

export const DeleteWorkspaceMcpServerDocument = `
    mutation deleteWorkspaceMcpServer($id: ID!) {
  deleteWorkspaceMcpServer(mcpServerId: $id)
}
    `;

export const useDeleteWorkspaceMcpServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteWorkspaceMcpServerMutation, TError, DeleteWorkspaceMcpServerMutationVariables, TContext>) => {
    
    return useMutation<DeleteWorkspaceMcpServerMutation, TError, DeleteWorkspaceMcpServerMutationVariables, TContext>(
      {
    mutationKey: ['deleteWorkspaceMcpServer'],
    mutationFn: (variables?: DeleteWorkspaceMcpServerMutationVariables) => fetcher<DeleteWorkspaceMcpServerMutation, DeleteWorkspaceMcpServerMutationVariables>(DeleteWorkspaceMcpServerDocument, variables)(),
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
          componentName
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

export const UpdateWorkspaceApiKeyDocument = `
    mutation updateWorkspaceApiKey($apiKeyId: ID!, $name: String!) {
  updateWorkspaceApiKey(apiKeyId: $apiKeyId, name: $name)
}
    `;

export const useUpdateWorkspaceApiKeyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateWorkspaceApiKeyMutation, TError, UpdateWorkspaceApiKeyMutationVariables, TContext>) => {
    
    return useMutation<UpdateWorkspaceApiKeyMutation, TError, UpdateWorkspaceApiKeyMutationVariables, TContext>(
      {
    mutationKey: ['updateWorkspaceApiKey'],
    mutationFn: (variables?: UpdateWorkspaceApiKeyMutationVariables) => fetcher<UpdateWorkspaceApiKeyMutation, UpdateWorkspaceApiKeyMutationVariables>(UpdateWorkspaceApiKeyDocument, variables)(),
    ...options
  }
    )};

export const WorkflowChatProjectDeploymentWorkflowDocument = `
    query workflowChatProjectDeploymentWorkflow($id: String!) {
  projectDeploymentWorkflow(id: $id) {
    projectWorkflow {
      sseStreamResponse
      workflow {
        label
      }
    }
  }
}
    `;

export const useWorkflowChatProjectDeploymentWorkflowQuery = <
      TData = WorkflowChatProjectDeploymentWorkflowQuery,
      TError = unknown
    >(
      variables: WorkflowChatProjectDeploymentWorkflowQueryVariables,
      options?: Omit<UseQueryOptions<WorkflowChatProjectDeploymentWorkflowQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkflowChatProjectDeploymentWorkflowQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkflowChatProjectDeploymentWorkflowQuery, TError, TData>(
      {
    queryKey: ['workflowChatProjectDeploymentWorkflow', variables],
    queryFn: fetcher<WorkflowChatProjectDeploymentWorkflowQuery, WorkflowChatProjectDeploymentWorkflowQueryVariables>(WorkflowChatProjectDeploymentWorkflowDocument, variables),
    ...options
  }
    )};

export const WorkflowChatWorkspaceProjectDeploymentsDocument = `
    query workflowChatWorkspaceProjectDeployments($workspaceId: ID!, $environmentId: ID!, $projectId: ID, $tagId: ID) {
  workspaceProjectDeployments(
    workspaceId: $workspaceId
    environmentId: $environmentId
    projectId: $projectId
    tagId: $tagId
  ) {
    id
    enabled
    project {
      id
      name
    }
    projectDeploymentWorkflows {
      id
      enabled
      staticWebhookUrl
      workflowExecutionId
      projectWorkflow {
        workflow {
          id
          label
          triggers {
            parameters
            type
          }
        }
      }
    }
  }
}
    `;

export const useWorkflowChatWorkspaceProjectDeploymentsQuery = <
      TData = WorkflowChatWorkspaceProjectDeploymentsQuery,
      TError = unknown
    >(
      variables: WorkflowChatWorkspaceProjectDeploymentsQueryVariables,
      options?: Omit<UseQueryOptions<WorkflowChatWorkspaceProjectDeploymentsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkflowChatWorkspaceProjectDeploymentsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkflowChatWorkspaceProjectDeploymentsQuery, TError, TData>(
      {
    queryKey: ['workflowChatWorkspaceProjectDeployments', variables],
    queryFn: fetcher<WorkflowChatWorkspaceProjectDeploymentsQuery, WorkflowChatWorkspaceProjectDeploymentsQueryVariables>(WorkflowChatWorkspaceProjectDeploymentsDocument, variables),
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
        componentName
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

export const WorkspaceApiKeysDocument = `
    query workspaceApiKeys($workspaceId: ID!, $environmentId: ID!) {
  workspaceApiKeys(workspaceId: $workspaceId, environmentId: $environmentId) {
    id
    name
    secretKey
    lastUsedDate
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
  }
}
    `;

export const useWorkspaceApiKeysQuery = <
      TData = WorkspaceApiKeysQuery,
      TError = unknown
    >(
      variables: WorkspaceApiKeysQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceApiKeysQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceApiKeysQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceApiKeysQuery, TError, TData>(
      {
    queryKey: ['workspaceApiKeys', variables],
    queryFn: fetcher<WorkspaceApiKeysQuery, WorkspaceApiKeysQueryVariables>(WorkspaceApiKeysDocument, variables),
    ...options
  }
    )};

export const WorkspaceMcpServersDocument = `
    query workspaceMcpServers($workspaceId: ID!) {
  workspaceMcpServers(workspaceId: $workspaceId) {
    id
    name
    type
    environmentId
    enabled
    url
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

export const useWorkspaceMcpServersQuery = <
      TData = WorkspaceMcpServersQuery,
      TError = unknown
    >(
      variables: WorkspaceMcpServersQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceMcpServersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceMcpServersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceMcpServersQuery, TError, TData>(
      {
    queryKey: ['workspaceMcpServers', variables],
    queryFn: fetcher<WorkspaceMcpServersQuery, WorkspaceMcpServersQueryVariables>(WorkspaceMcpServersDocument, variables),
    ...options
  }
    )};

export const AddDataTableColumnDocument = `
    mutation addDataTableColumn($input: AddColumnInput!) {
  addDataTableColumn(input: $input)
}
    `;

export const useAddDataTableColumnMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<AddDataTableColumnMutation, TError, AddDataTableColumnMutationVariables, TContext>) => {
    
    return useMutation<AddDataTableColumnMutation, TError, AddDataTableColumnMutationVariables, TContext>(
      {
    mutationKey: ['addDataTableColumn'],
    mutationFn: (variables?: AddDataTableColumnMutationVariables) => fetcher<AddDataTableColumnMutation, AddDataTableColumnMutationVariables>(AddDataTableColumnDocument, variables)(),
    ...options
  }
    )};

export const CreateDataTableDocument = `
    mutation createDataTable($input: CreateDataTableInput!) {
  createDataTable(input: $input)
}
    `;

export const useCreateDataTableMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateDataTableMutation, TError, CreateDataTableMutationVariables, TContext>) => {
    
    return useMutation<CreateDataTableMutation, TError, CreateDataTableMutationVariables, TContext>(
      {
    mutationKey: ['createDataTable'],
    mutationFn: (variables?: CreateDataTableMutationVariables) => fetcher<CreateDataTableMutation, CreateDataTableMutationVariables>(CreateDataTableDocument, variables)(),
    ...options
  }
    )};

export const DataTableRowsDocument = `
    query dataTableRows($environmentId: ID!, $tableId: ID!) {
  dataTableRows(environmentId: $environmentId, tableId: $tableId) {
    id
    values
  }
}
    `;

export const useDataTableRowsQuery = <
      TData = DataTableRowsQuery,
      TError = unknown
    >(
      variables: DataTableRowsQueryVariables,
      options?: Omit<UseQueryOptions<DataTableRowsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<DataTableRowsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<DataTableRowsQuery, TError, TData>(
      {
    queryKey: ['dataTableRows', variables],
    queryFn: fetcher<DataTableRowsQuery, DataTableRowsQueryVariables>(DataTableRowsDocument, variables),
    ...options
  }
    )};

export const DataTableRowsPageDocument = `
    query dataTableRowsPage($environmentId: ID!, $tableId: ID!, $limit: Int, $offset: Int) {
  dataTableRowsPage(
    environmentId: $environmentId
    tableId: $tableId
    limit: $limit
    offset: $offset
  ) {
    items {
      id
      values
    }
    hasMore
    nextOffset
  }
}
    `;

export const useDataTableRowsPageQuery = <
      TData = DataTableRowsPageQuery,
      TError = unknown
    >(
      variables: DataTableRowsPageQueryVariables,
      options?: Omit<UseQueryOptions<DataTableRowsPageQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<DataTableRowsPageQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<DataTableRowsPageQuery, TError, TData>(
      {
    queryKey: ['dataTableRowsPage', variables],
    queryFn: fetcher<DataTableRowsPageQuery, DataTableRowsPageQueryVariables>(DataTableRowsPageDocument, variables),
    ...options
  }
    )};

export const DataTableTagsDocument = `
    query dataTableTags {
  dataTableTags {
    id
    name
  }
}
    `;

export const useDataTableTagsQuery = <
      TData = DataTableTagsQuery,
      TError = unknown
    >(
      variables?: DataTableTagsQueryVariables,
      options?: Omit<UseQueryOptions<DataTableTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<DataTableTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<DataTableTagsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['dataTableTags'] : ['dataTableTags', variables],
    queryFn: fetcher<DataTableTagsQuery, DataTableTagsQueryVariables>(DataTableTagsDocument, variables),
    ...options
  }
    )};

export const DataTableTagsByTableDocument = `
    query dataTableTagsByTable {
  dataTableTagsByTable {
    tableId
    tags {
      id
      name
    }
  }
}
    `;

export const useDataTableTagsByTableQuery = <
      TData = DataTableTagsByTableQuery,
      TError = unknown
    >(
      variables?: DataTableTagsByTableQueryVariables,
      options?: Omit<UseQueryOptions<DataTableTagsByTableQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<DataTableTagsByTableQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<DataTableTagsByTableQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['dataTableTagsByTable'] : ['dataTableTagsByTable', variables],
    queryFn: fetcher<DataTableTagsByTableQuery, DataTableTagsByTableQueryVariables>(DataTableTagsByTableDocument, variables),
    ...options
  }
    )};

export const DataTablesDocument = `
    query dataTables($environmentId: ID!, $workspaceId: ID!) {
  dataTables(environmentId: $environmentId, workspaceId: $workspaceId) {
    id
    baseName
    lastModifiedDate
    columns {
      id
      name
      type
    }
  }
}
    `;

export const useDataTablesQuery = <
      TData = DataTablesQuery,
      TError = unknown
    >(
      variables: DataTablesQueryVariables,
      options?: Omit<UseQueryOptions<DataTablesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<DataTablesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<DataTablesQuery, TError, TData>(
      {
    queryKey: ['dataTables', variables],
    queryFn: fetcher<DataTablesQuery, DataTablesQueryVariables>(DataTablesDocument, variables),
    ...options
  }
    )};

export const DeleteDataTableRowDocument = `
    mutation deleteDataTableRow($input: DeleteRowInput!) {
  deleteDataTableRow(input: $input)
}
    `;

export const useDeleteDataTableRowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteDataTableRowMutation, TError, DeleteDataTableRowMutationVariables, TContext>) => {
    
    return useMutation<DeleteDataTableRowMutation, TError, DeleteDataTableRowMutationVariables, TContext>(
      {
    mutationKey: ['deleteDataTableRow'],
    mutationFn: (variables?: DeleteDataTableRowMutationVariables) => fetcher<DeleteDataTableRowMutation, DeleteDataTableRowMutationVariables>(DeleteDataTableRowDocument, variables)(),
    ...options
  }
    )};

export const DropDataTableDocument = `
    mutation dropDataTable($input: RemoveTableInput!) {
  dropDataTable(input: $input)
}
    `;

export const useDropDataTableMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DropDataTableMutation, TError, DropDataTableMutationVariables, TContext>) => {
    
    return useMutation<DropDataTableMutation, TError, DropDataTableMutationVariables, TContext>(
      {
    mutationKey: ['dropDataTable'],
    mutationFn: (variables?: DropDataTableMutationVariables) => fetcher<DropDataTableMutation, DropDataTableMutationVariables>(DropDataTableDocument, variables)(),
    ...options
  }
    )};

export const DuplicateDataTableDocument = `
    mutation duplicateDataTable($input: DuplicateDataTableInput!) {
  duplicateDataTable(input: $input)
}
    `;

export const useDuplicateDataTableMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DuplicateDataTableMutation, TError, DuplicateDataTableMutationVariables, TContext>) => {
    
    return useMutation<DuplicateDataTableMutation, TError, DuplicateDataTableMutationVariables, TContext>(
      {
    mutationKey: ['duplicateDataTable'],
    mutationFn: (variables?: DuplicateDataTableMutationVariables) => fetcher<DuplicateDataTableMutation, DuplicateDataTableMutationVariables>(DuplicateDataTableDocument, variables)(),
    ...options
  }
    )};

export const ExportDataTableCsvDocument = `
    query exportDataTableCsv($environmentId: ID!, $tableId: ID!) {
  exportDataTableCsv(environmentId: $environmentId, tableId: $tableId)
}
    `;

export const useExportDataTableCsvQuery = <
      TData = ExportDataTableCsvQuery,
      TError = unknown
    >(
      variables: ExportDataTableCsvQueryVariables,
      options?: Omit<UseQueryOptions<ExportDataTableCsvQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ExportDataTableCsvQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ExportDataTableCsvQuery, TError, TData>(
      {
    queryKey: ['exportDataTableCsv', variables],
    queryFn: fetcher<ExportDataTableCsvQuery, ExportDataTableCsvQueryVariables>(ExportDataTableCsvDocument, variables),
    ...options
  }
    )};

export const ImportDataTableCsvDocument = `
    mutation importDataTableCsv($input: ImportCsvInput!) {
  importDataTableCsv(input: $input)
}
    `;

export const useImportDataTableCsvMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ImportDataTableCsvMutation, TError, ImportDataTableCsvMutationVariables, TContext>) => {
    
    return useMutation<ImportDataTableCsvMutation, TError, ImportDataTableCsvMutationVariables, TContext>(
      {
    mutationKey: ['importDataTableCsv'],
    mutationFn: (variables?: ImportDataTableCsvMutationVariables) => fetcher<ImportDataTableCsvMutation, ImportDataTableCsvMutationVariables>(ImportDataTableCsvDocument, variables)(),
    ...options
  }
    )};

export const InsertDataTableRowDocument = `
    mutation insertDataTableRow($input: InsertRowInput!) {
  insertDataTableRow(input: $input) {
    id
    values
  }
}
    `;

export const useInsertDataTableRowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<InsertDataTableRowMutation, TError, InsertDataTableRowMutationVariables, TContext>) => {
    
    return useMutation<InsertDataTableRowMutation, TError, InsertDataTableRowMutationVariables, TContext>(
      {
    mutationKey: ['insertDataTableRow'],
    mutationFn: (variables?: InsertDataTableRowMutationVariables) => fetcher<InsertDataTableRowMutation, InsertDataTableRowMutationVariables>(InsertDataTableRowDocument, variables)(),
    ...options
  }
    )};

export const RemoveDataTableColumnDocument = `
    mutation removeDataTableColumn($input: RemoveColumnInput!) {
  removeDataTableColumn(input: $input)
}
    `;

export const useRemoveDataTableColumnMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RemoveDataTableColumnMutation, TError, RemoveDataTableColumnMutationVariables, TContext>) => {
    
    return useMutation<RemoveDataTableColumnMutation, TError, RemoveDataTableColumnMutationVariables, TContext>(
      {
    mutationKey: ['removeDataTableColumn'],
    mutationFn: (variables?: RemoveDataTableColumnMutationVariables) => fetcher<RemoveDataTableColumnMutation, RemoveDataTableColumnMutationVariables>(RemoveDataTableColumnDocument, variables)(),
    ...options
  }
    )};

export const RenameDataTableDocument = `
    mutation renameDataTable($input: RenameDataTableInput!) {
  renameDataTable(input: $input)
}
    `;

export const useRenameDataTableMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RenameDataTableMutation, TError, RenameDataTableMutationVariables, TContext>) => {
    
    return useMutation<RenameDataTableMutation, TError, RenameDataTableMutationVariables, TContext>(
      {
    mutationKey: ['renameDataTable'],
    mutationFn: (variables?: RenameDataTableMutationVariables) => fetcher<RenameDataTableMutation, RenameDataTableMutationVariables>(RenameDataTableDocument, variables)(),
    ...options
  }
    )};

export const RenameDataTableColumnDocument = `
    mutation renameDataTableColumn($input: RenameColumnInput!) {
  renameDataTableColumn(input: $input)
}
    `;

export const useRenameDataTableColumnMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RenameDataTableColumnMutation, TError, RenameDataTableColumnMutationVariables, TContext>) => {
    
    return useMutation<RenameDataTableColumnMutation, TError, RenameDataTableColumnMutationVariables, TContext>(
      {
    mutationKey: ['renameDataTableColumn'],
    mutationFn: (variables?: RenameDataTableColumnMutationVariables) => fetcher<RenameDataTableColumnMutation, RenameDataTableColumnMutationVariables>(RenameDataTableColumnDocument, variables)(),
    ...options
  }
    )};

export const UpdateDataTableRowDocument = `
    mutation updateDataTableRow($input: UpdateRowInput!) {
  updateDataTableRow(input: $input) {
    id
    values
  }
}
    `;

export const useUpdateDataTableRowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateDataTableRowMutation, TError, UpdateDataTableRowMutationVariables, TContext>) => {
    
    return useMutation<UpdateDataTableRowMutation, TError, UpdateDataTableRowMutationVariables, TContext>(
      {
    mutationKey: ['updateDataTableRow'],
    mutationFn: (variables?: UpdateDataTableRowMutationVariables) => fetcher<UpdateDataTableRowMutation, UpdateDataTableRowMutationVariables>(UpdateDataTableRowDocument, variables)(),
    ...options
  }
    )};

export const UpdateDataTableTagsDocument = `
    mutation updateDataTableTags($input: UpdateDataTableTagsInput!) {
  updateDataTableTags(input: $input)
}
    `;

export const useUpdateDataTableTagsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateDataTableTagsMutation, TError, UpdateDataTableTagsMutationVariables, TContext>) => {
    
    return useMutation<UpdateDataTableTagsMutation, TError, UpdateDataTableTagsMutationVariables, TContext>(
      {
    mutationKey: ['updateDataTableTags'],
    mutationFn: (variables?: UpdateDataTableTagsMutationVariables) => fetcher<UpdateDataTableTagsMutation, UpdateDataTableTagsMutationVariables>(UpdateDataTableTagsDocument, variables)(),
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
        triggers {
          name
          type
          parameters
        }
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

export const AdminApiKeysDocument = `
    query adminApiKeys($environmentId: ID!) {
  adminApiKeys(environmentId: $environmentId) {
    id
    name
    secretKey
    lastUsedDate
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
  }
}
    `;

export const useAdminApiKeysQuery = <
      TData = AdminApiKeysQuery,
      TError = unknown
    >(
      variables: AdminApiKeysQueryVariables,
      options?: Omit<UseQueryOptions<AdminApiKeysQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AdminApiKeysQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AdminApiKeysQuery, TError, TData>(
      {
    queryKey: ['adminApiKeys', variables],
    queryFn: fetcher<AdminApiKeysQuery, AdminApiKeysQueryVariables>(AdminApiKeysDocument, variables),
    ...options
  }
    )};

export const ApiKeysDocument = `
    query apiKeys($environmentId: ID!, $type: PlatformType!) {
  apiKeys(environmentId: $environmentId, type: $type) {
    id
    name
    secretKey
    lastUsedDate
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
  }
}
    `;

export const useApiKeysQuery = <
      TData = ApiKeysQuery,
      TError = unknown
    >(
      variables: ApiKeysQueryVariables,
      options?: Omit<UseQueryOptions<ApiKeysQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ApiKeysQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ApiKeysQuery, TError, TData>(
      {
    queryKey: ['apiKeys', variables],
    queryFn: fetcher<ApiKeysQuery, ApiKeysQueryVariables>(ApiKeysDocument, variables),
    ...options
  }
    )};

export const CreateApiKeyDocument = `
    mutation createApiKey($name: String!, $environmentId: ID!, $type: PlatformType) {
  createApiKey(name: $name, environmentId: $environmentId, type: $type)
}
    `;

export const useCreateApiKeyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateApiKeyMutation, TError, CreateApiKeyMutationVariables, TContext>) => {
    
    return useMutation<CreateApiKeyMutation, TError, CreateApiKeyMutationVariables, TContext>(
      {
    mutationKey: ['createApiKey'],
    mutationFn: (variables?: CreateApiKeyMutationVariables) => fetcher<CreateApiKeyMutation, CreateApiKeyMutationVariables>(CreateApiKeyDocument, variables)(),
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

export const DeleteApiKeyDocument = `
    mutation deleteApiKey($id: ID!) {
  deleteApiKey(id: $id)
}
    `;

export const useDeleteApiKeyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteApiKeyMutation, TError, DeleteApiKeyMutationVariables, TContext>) => {
    
    return useMutation<DeleteApiKeyMutation, TError, DeleteApiKeyMutationVariables, TContext>(
      {
    mutationKey: ['deleteApiKey'],
    mutationFn: (variables?: DeleteApiKeyMutationVariables) => fetcher<DeleteApiKeyMutation, DeleteApiKeyMutationVariables>(DeleteApiKeyDocument, variables)(),
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

export const ManagementMcpServerUrlDocument = `
    query managementMcpServerUrl {
  managementMcpServerUrl
}
    `;

export const useManagementMcpServerUrlQuery = <
      TData = ManagementMcpServerUrlQuery,
      TError = unknown
    >(
      variables?: ManagementMcpServerUrlQueryVariables,
      options?: Omit<UseQueryOptions<ManagementMcpServerUrlQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ManagementMcpServerUrlQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ManagementMcpServerUrlQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['managementMcpServerUrl'] : ['managementMcpServerUrl', variables],
    queryFn: fetcher<ManagementMcpServerUrlQuery, ManagementMcpServerUrlQueryVariables>(ManagementMcpServerUrlDocument, variables),
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
    query mcpServerTags($type: PlatformType!) {
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
    query mcpServers($type: PlatformType!) {
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

export const UpdateApiKeyDocument = `
    mutation updateApiKey($id: ID!, $name: String!) {
  updateApiKey(id: $id, name: $name)
}
    `;

export const useUpdateApiKeyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateApiKeyMutation, TError, UpdateApiKeyMutationVariables, TContext>) => {
    
    return useMutation<UpdateApiKeyMutation, TError, UpdateApiKeyMutationVariables, TContext>(
      {
    mutationKey: ['updateApiKey'],
    mutationFn: (variables?: UpdateApiKeyMutationVariables) => fetcher<UpdateApiKeyMutation, UpdateApiKeyMutationVariables>(UpdateApiKeyDocument, variables)(),
    ...options
  }
    )};

export const UpdateManagementMcpServerUrlDocument = `
    mutation updateManagementMcpServerUrl {
  updateManagementMcpServerUrl
}
    `;

export const useUpdateManagementMcpServerUrlMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateManagementMcpServerUrlMutation, TError, UpdateManagementMcpServerUrlMutationVariables, TContext>) => {
    
    return useMutation<UpdateManagementMcpServerUrlMutation, TError, UpdateManagementMcpServerUrlMutationVariables, TContext>(
      {
    mutationKey: ['updateManagementMcpServerUrl'],
    mutationFn: (variables?: UpdateManagementMcpServerUrlMutationVariables) => fetcher<UpdateManagementMcpServerUrlMutation, UpdateManagementMcpServerUrlMutationVariables>(UpdateManagementMcpServerUrlDocument, variables)(),
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
    mutation updateMcpServerUrl($id: ID!) {
  updateMcpServerUrl(id: $id)
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

export const AuthoritiesDocument = `
    query authorities {
  authorities
}
    `;

export const useAuthoritiesQuery = <
      TData = AuthoritiesQuery,
      TError = unknown
    >(
      variables?: AuthoritiesQueryVariables,
      options?: Omit<UseQueryOptions<AuthoritiesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AuthoritiesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AuthoritiesQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['authorities'] : ['authorities', variables],
    queryFn: fetcher<AuthoritiesQuery, AuthoritiesQueryVariables>(AuthoritiesDocument, variables),
    ...options
  }
    )};

export const DeleteUserDocument = `
    mutation deleteUser($login: String!) {
  deleteUser(login: $login)
}
    `;

export const useDeleteUserMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteUserMutation, TError, DeleteUserMutationVariables, TContext>) => {
    
    return useMutation<DeleteUserMutation, TError, DeleteUserMutationVariables, TContext>(
      {
    mutationKey: ['deleteUser'],
    mutationFn: (variables?: DeleteUserMutationVariables) => fetcher<DeleteUserMutation, DeleteUserMutationVariables>(DeleteUserDocument, variables)(),
    ...options
  }
    )};

export const InviteUserDocument = `
    mutation inviteUser($email: String!, $password: String!, $role: String!) {
  inviteUser(email: $email, password: $password, role: $role)
}
    `;

export const useInviteUserMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<InviteUserMutation, TError, InviteUserMutationVariables, TContext>) => {
    
    return useMutation<InviteUserMutation, TError, InviteUserMutationVariables, TContext>(
      {
    mutationKey: ['inviteUser'],
    mutationFn: (variables?: InviteUserMutationVariables) => fetcher<InviteUserMutation, InviteUserMutationVariables>(InviteUserDocument, variables)(),
    ...options
  }
    )};

export const UpdateUserDocument = `
    mutation updateUser($login: String!, $role: String!) {
  updateUser(login: $login, role: $role) {
    id
    login
    email
    firstName
    lastName
    activated
    authorities
  }
}
    `;

export const useUpdateUserMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateUserMutation, TError, UpdateUserMutationVariables, TContext>) => {
    
    return useMutation<UpdateUserMutation, TError, UpdateUserMutationVariables, TContext>(
      {
    mutationKey: ['updateUser'],
    mutationFn: (variables?: UpdateUserMutationVariables) => fetcher<UpdateUserMutation, UpdateUserMutationVariables>(UpdateUserDocument, variables)(),
    ...options
  }
    )};

export const UsersDocument = `
    query users($pageNumber: Int, $pageSize: Int) {
  users(pageNumber: $pageNumber, pageSize: $pageSize) {
    content {
      id
      login
      email
      firstName
      lastName
      activated
      authorities
    }
    number
    size
    totalElements
    totalPages
  }
}
    `;

export const useUsersQuery = <
      TData = UsersQuery,
      TError = unknown
    >(
      variables?: UsersQueryVariables,
      options?: Omit<UseQueryOptions<UsersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<UsersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<UsersQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['users'] : ['users', variables],
    queryFn: fetcher<UsersQuery, UsersQueryVariables>(UsersDocument, variables),
    ...options
  }
    )};
