import { endpointUrl, fetchParams } from './config';
import { useQuery, useMutation, UseQueryOptions, UseMutationOptions } from '@tanstack/react-query';
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

export type ApiCollectionSearchResult = SearchResult & {
  __typename?: 'ApiCollectionSearchResult';
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  type: SearchAssetType;
};

export type ApiConnector = {
  __typename?: 'ApiConnector';
  connectorVersion: Scalars['Int']['output'];
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  definition?: Maybe<Scalars['String']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  enabled?: Maybe<Scalars['Boolean']['output']>;
  endpoints?: Maybe<Array<ApiConnectorEndpoint>>;
  icon?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  specification?: Maybe<Scalars['String']['output']>;
  title?: Maybe<Scalars['String']['output']>;
  version?: Maybe<Scalars['Int']['output']>;
};

export type ApiConnectorEndpoint = {
  __typename?: 'ApiConnectorEndpoint';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  httpMethod?: Maybe<HttpMethod>;
  id: Scalars['ID']['output'];
  lastExecutionDate?: Maybe<Scalars['Long']['output']>;
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  path?: Maybe<Scalars['String']['output']>;
  version?: Maybe<Scalars['Int']['output']>;
};

export type ApiEndpointSearchResult = SearchResult & {
  __typename?: 'ApiEndpointSearchResult';
  collectionId: Scalars['ID']['output'];
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  path?: Maybe<Scalars['String']['output']>;
  type: SearchAssetType;
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

export type ApprovalTask = {
  __typename?: 'ApprovalTask';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['String']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  version: Scalars['Int']['output'];
};

export type ApprovalTaskInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  id?: InputMaybe<Scalars['ID']['input']>;
  name: Scalars['String']['input'];
  version?: InputMaybe<Scalars['Int']['input']>;
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

export type ComponentConnection = {
  __typename?: 'ComponentConnection';
  componentName: Scalars['String']['output'];
  componentVersion: Scalars['Int']['output'];
  key: Scalars['String']['output'];
  required: Scalars['Boolean']['output'];
  workflowNodeName: Scalars['String']['output'];
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

export type ConnectionSearchResult = SearchResult & {
  __typename?: 'ConnectionSearchResult';
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  type: SearchAssetType;
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

export type CreateApiConnectorInput = {
  connectorVersion: Scalars['Int']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
  icon?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  title?: InputMaybe<Scalars['String']['input']>;
};

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

export type CustomComponent = {
  __typename?: 'CustomComponent';
  componentVersion?: Maybe<Scalars['Int']['output']>;
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  enabled?: Maybe<Scalars['Boolean']['output']>;
  icon?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  language?: Maybe<CustomComponentLanguage>;
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  title?: Maybe<Scalars['String']['output']>;
  version?: Maybe<Scalars['Int']['output']>;
};

export type CustomComponentActionDefinition = {
  __typename?: 'CustomComponentActionDefinition';
  description?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  title?: Maybe<Scalars['String']['output']>;
};

export type CustomComponentDefinition = {
  __typename?: 'CustomComponentDefinition';
  actions: Array<CustomComponentActionDefinition>;
  triggers: Array<CustomComponentTriggerDefinition>;
};

export enum CustomComponentLanguage {
  Java = 'JAVA',
  Javascript = 'JAVASCRIPT',
  Python = 'PYTHON',
  Ruby = 'RUBY'
}

export type CustomComponentTriggerDefinition = {
  __typename?: 'CustomComponentTriggerDefinition';
  description?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  title?: Maybe<Scalars['String']['output']>;
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

export type DataTableSearchResult = SearchResult & {
  __typename?: 'DataTableSearchResult';
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  type: SearchAssetType;
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

export type DiscoverEndpointsInput = {
  documentationUrl: Scalars['String']['input'];
  name: Scalars['String']['input'];
  userPrompt?: InputMaybe<Scalars['String']['input']>;
};

export type DiscoveredEndpoint = {
  __typename?: 'DiscoveredEndpoint';
  id: Scalars['ID']['output'];
  method: Scalars['String']['output'];
  path: Scalars['String']['output'];
  resource?: Maybe<Scalars['String']['output']>;
  summary?: Maybe<Scalars['String']['output']>;
};

export type DocumentStatusUpdate = {
  __typename?: 'DocumentStatusUpdate';
  documentId: Scalars['ID']['output'];
  message?: Maybe<Scalars['String']['output']>;
  status: Scalars['Int']['output'];
  timestamp: Scalars['Long']['output'];
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

export type EndpointDefinitionInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  httpMethod: HttpMethod;
  operationId: Scalars['String']['input'];
  parameters?: InputMaybe<Array<ParameterDefinitionInput>>;
  path: Scalars['String']['input'];
  requestBody?: InputMaybe<RequestBodyDefinitionInput>;
  responses?: InputMaybe<Array<ResponseDefinitionInput>>;
  summary?: InputMaybe<Scalars['String']['input']>;
};

export type EndpointDiscoveryResult = {
  __typename?: 'EndpointDiscoveryResult';
  endpoints?: Maybe<Array<DiscoveredEndpoint>>;
  errorMessage?: Maybe<Scalars['String']['output']>;
  jobId: Scalars['String']['output'];
  status: GenerationJobStatusEnum;
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

export type ExecutionError = {
  __typename?: 'ExecutionError';
  message?: Maybe<Scalars['String']['output']>;
  stackTrace?: Maybe<Array<Maybe<Scalars['String']['output']>>>;
};

export type FileEntry = {
  __typename?: 'FileEntry';
  extension?: Maybe<Scalars['String']['output']>;
  mimeType?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  url: Scalars['String']['output'];
};

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

export type GenerateForEndpointsInput = {
  documentationUrl: Scalars['String']['input'];
  icon?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  selectedEndpoints: Array<SelectedEndpointInput>;
};

export type GenerateFromDocumentationInput = {
  documentationUrl: Scalars['String']['input'];
  icon?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  userPrompt?: InputMaybe<Scalars['String']['input']>;
};

export type GenerateSpecificationInput = {
  baseUrl?: InputMaybe<Scalars['String']['input']>;
  endpoints: Array<EndpointDefinitionInput>;
  name: Scalars['String']['input'];
};

export type GenerateSpecificationResponse = {
  __typename?: 'GenerateSpecificationResponse';
  specification?: Maybe<Scalars['String']['output']>;
};

export type GenerationJobStatus = {
  __typename?: 'GenerationJobStatus';
  errorMessage?: Maybe<Scalars['String']['output']>;
  jobId: Scalars['String']['output'];
  specification?: Maybe<Scalars['String']['output']>;
  status: GenerationJobStatusEnum;
};

export enum GenerationJobStatusEnum {
  Cancelled = 'CANCELLED',
  Completed = 'COMPLETED',
  Failed = 'FAILED',
  Pending = 'PENDING',
  Processing = 'PROCESSING'
}

export type Help = {
  __typename?: 'Help';
  description?: Maybe<Scalars['String']['output']>;
  documentationUrl?: Maybe<Scalars['String']['output']>;
};

export enum HttpMethod {
  Delete = 'DELETE',
  Get = 'GET',
  Patch = 'PATCH',
  Post = 'POST',
  Put = 'PUT'
}

export type IdentityProviderInput = {
  autoProvision?: InputMaybe<Scalars['Boolean']['input']>;
  clientId?: InputMaybe<Scalars['String']['input']>;
  clientSecret?: InputMaybe<Scalars['String']['input']>;
  defaultAuthority?: InputMaybe<Scalars['String']['input']>;
  domains: Array<Scalars['String']['input']>;
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  enforced?: InputMaybe<Scalars['Boolean']['input']>;
  issuerUri?: InputMaybe<Scalars['String']['input']>;
  metadataUri?: InputMaybe<Scalars['String']['input']>;
  mfaMethod?: InputMaybe<Scalars['String']['input']>;
  mfaRequired?: InputMaybe<Scalars['Boolean']['input']>;
  name: Scalars['String']['input'];
  nameIdFormat?: InputMaybe<Scalars['String']['input']>;
  scopes?: InputMaybe<Scalars['String']['input']>;
  signingCertificate?: InputMaybe<Scalars['String']['input']>;
  type?: InputMaybe<Scalars['String']['input']>;
};

export type IdentityProviderType = {
  __typename?: 'IdentityProviderType';
  autoProvision: Scalars['Boolean']['output'];
  clientId?: Maybe<Scalars['String']['output']>;
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  defaultAuthority: Scalars['String']['output'];
  domains: Array<Scalars['String']['output']>;
  enabled: Scalars['Boolean']['output'];
  enforced: Scalars['Boolean']['output'];
  id: Scalars['ID']['output'];
  issuerUri?: Maybe<Scalars['String']['output']>;
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  metadataUri?: Maybe<Scalars['String']['output']>;
  mfaMethod?: Maybe<Scalars['String']['output']>;
  mfaRequired: Scalars['Boolean']['output'];
  name: Scalars['String']['output'];
  nameIdFormat?: Maybe<Scalars['String']['output']>;
  scopes?: Maybe<Scalars['String']['output']>;
  signingCertificate?: Maybe<Scalars['String']['output']>;
  type: Scalars['String']['output'];
};

export type ImportCsvInput = {
  csv: Scalars['String']['input'];
  environmentId: Scalars['ID']['input'];
  tableId: Scalars['ID']['input'];
};

export type ImportOpenApiSpecificationInput = {
  icon?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  specification: Scalars['String']['input'];
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

export type KnowledgeBase = {
  __typename?: 'KnowledgeBase';
  createdDate?: Maybe<Scalars['Long']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  documents?: Maybe<Array<Maybe<KnowledgeBaseDocument>>>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  maxChunkSize?: Maybe<Scalars['Int']['output']>;
  minChunkSizeChars?: Maybe<Scalars['Int']['output']>;
  name: Scalars['String']['output'];
  overlap?: Maybe<Scalars['Int']['output']>;
};

export type KnowledgeBaseDocument = {
  __typename?: 'KnowledgeBaseDocument';
  chunks?: Maybe<Array<Maybe<KnowledgeBaseDocumentChunk>>>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  document?: Maybe<FileEntry>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  status: Scalars['Int']['output'];
  tags?: Maybe<Array<Tag>>;
};

export type KnowledgeBaseDocumentChunk = {
  __typename?: 'KnowledgeBaseDocumentChunk';
  content?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  knowledgeBaseDocumentId: Scalars['ID']['output'];
  metadata?: Maybe<Scalars['Map']['output']>;
  score?: Maybe<Scalars['Float']['output']>;
};

export type KnowledgeBaseDocumentChunkInput = {
  content: Scalars['String']['input'];
};

export type KnowledgeBaseDocumentSearchResult = SearchResult & {
  __typename?: 'KnowledgeBaseDocumentSearchResult';
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  knowledgeBaseId: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  type: SearchAssetType;
};

export type KnowledgeBaseDocumentTagsEntry = {
  __typename?: 'KnowledgeBaseDocumentTagsEntry';
  knowledgeBaseDocumentId: Scalars['ID']['output'];
  tags: Array<Tag>;
};

export type KnowledgeBaseInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  maxChunkSize?: InputMaybe<Scalars['Int']['input']>;
  minChunkSizeChars?: InputMaybe<Scalars['Int']['input']>;
  name: Scalars['String']['input'];
  overlap?: InputMaybe<Scalars['Int']['input']>;
};

export type KnowledgeBaseSearchResult = SearchResult & {
  __typename?: 'KnowledgeBaseSearchResult';
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  type: SearchAssetType;
};

export type KnowledgeBaseTagsEntry = {
  __typename?: 'KnowledgeBaseTagsEntry';
  knowledgeBaseId: Scalars['ID']['output'];
  tags: Array<Tag>;
};

export type LogEntry = {
  __typename?: 'LogEntry';
  componentName: Scalars['String']['output'];
  componentOperationName?: Maybe<Scalars['String']['output']>;
  exceptionMessage?: Maybe<Scalars['String']['output']>;
  exceptionType?: Maybe<Scalars['String']['output']>;
  level: LogLevel;
  message: Scalars['String']['output'];
  stackTrace?: Maybe<Scalars['String']['output']>;
  taskExecutionId: Scalars['ID']['output'];
  timestamp: Scalars['String']['output'];
};

export type LogFilterInput = {
  componentName?: InputMaybe<Scalars['String']['input']>;
  fromTimestamp?: InputMaybe<Scalars['String']['input']>;
  minLevel?: InputMaybe<LogLevel>;
  searchText?: InputMaybe<Scalars['String']['input']>;
  taskExecutionId?: InputMaybe<Scalars['ID']['input']>;
  toTimestamp?: InputMaybe<Scalars['String']['input']>;
};

export enum LogLevel {
  Debug = 'DEBUG',
  Error = 'ERROR',
  Info = 'INFO',
  Trace = 'TRACE',
  Warn = 'WARN'
}

export type LogPage = {
  __typename?: 'LogPage';
  content: Array<LogEntry>;
  hasNext: Scalars['Boolean']['output'];
  hasPrevious: Scalars['Boolean']['output'];
  pageNumber: Scalars['Int']['output'];
  pageSize: Scalars['Int']['output'];
  totalElements: Scalars['Int']['output'];
  totalPages: Scalars['Int']['output'];
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
  cancelGenerationJob: Scalars['Boolean']['output'];
  createApiConnector: ApiConnector;
  createApiKey: Scalars['String']['output'];
  createApprovalTask?: Maybe<ApprovalTask>;
  createDataTable: Scalars['Boolean']['output'];
  createIdentityProvider: IdentityProviderType;
  createKnowledgeBase?: Maybe<KnowledgeBase>;
  createMcpComponent?: Maybe<McpComponent>;
  createMcpComponentWithTools?: Maybe<McpComponent>;
  createMcpProject?: Maybe<McpProject>;
  createMcpProjectWorkflow?: Maybe<McpProjectWorkflow>;
  createMcpServer?: Maybe<McpServer>;
  createMcpTool?: Maybe<McpTool>;
  createWorkspaceApiKey: Scalars['String']['output'];
  createWorkspaceMcpServer?: Maybe<McpServer>;
  deleteApiConnector: Scalars['Boolean']['output'];
  deleteApiKey: Scalars['Boolean']['output'];
  deleteApprovalTask?: Maybe<Scalars['Boolean']['output']>;
  deleteCustomComponent: Scalars['Boolean']['output'];
  deleteDataTableRow: Scalars['Boolean']['output'];
  deleteIdentityProvider: Scalars['Boolean']['output'];
  deleteJobFileLogs: Scalars['Boolean']['output'];
  deleteKnowledgeBase?: Maybe<Scalars['Boolean']['output']>;
  deleteKnowledgeBaseDocument?: Maybe<Scalars['Boolean']['output']>;
  deleteKnowledgeBaseDocumentChunk?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpComponent?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpProject?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpProjectWorkflow?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpServer?: Maybe<Scalars['Boolean']['output']>;
  deleteSharedProject: Scalars['Boolean']['output'];
  deleteSharedWorkflow: Scalars['Boolean']['output'];
  deleteUser: Scalars['Boolean']['output'];
  deleteWorkspaceApiKey: Scalars['Boolean']['output'];
  deleteWorkspaceMcpServer?: Maybe<Scalars['Boolean']['output']>;
  disconnectConnection: Scalars['Boolean']['output'];
  dropDataTable: Scalars['Boolean']['output'];
  duplicateDataTable: Scalars['Boolean']['output'];
  enableApiConnector: Scalars['Boolean']['output'];
  enableCustomComponent: Scalars['Boolean']['output'];
  exportSharedProject?: Maybe<Scalars['Boolean']['output']>;
  exportSharedWorkflow: Scalars['Boolean']['output'];
  generateFromDocumentation: ApiConnector;
  generateSpecification: GenerateSpecificationResponse;
  importDataTableCsv: Scalars['Boolean']['output'];
  importOpenApiSpecification: ApiConnector;
  importProjectTemplate: Scalars['ID']['output'];
  importWorkflowTemplate: Scalars['ID']['output'];
  insertDataTableRow: DataTableRow;
  inviteUser: Scalars['Boolean']['output'];
  removeDataTableColumn: Scalars['Boolean']['output'];
  renameDataTable: Scalars['Boolean']['output'];
  renameDataTableColumn: Scalars['Boolean']['output'];
  saveClusterElementTestConfigurationConnection?: Maybe<Scalars['Boolean']['output']>;
  saveWorkflowTestConfigurationConnection?: Maybe<Scalars['Boolean']['output']>;
  startDiscoverEndpoints: EndpointDiscoveryResult;
  startGenerateForEndpoints: GenerationJobStatus;
  startGenerateFromDocumentationPreview: GenerationJobStatus;
  testClusterElementScript: ScriptTestExecution;
  testWorkflowNodeScript: ScriptTestExecution;
  updateApiConnector: ApiConnector;
  updateApiKey: Scalars['Boolean']['output'];
  updateApprovalTask?: Maybe<ApprovalTask>;
  updateDataTableRow: DataTableRow;
  updateDataTableTags: Scalars['Boolean']['output'];
  updateIdentityProvider: IdentityProviderType;
  updateKnowledgeBase?: Maybe<KnowledgeBase>;
  updateKnowledgeBaseDocumentChunk?: Maybe<KnowledgeBaseDocumentChunk>;
  updateKnowledgeBaseDocumentTags: Scalars['Boolean']['output'];
  updateKnowledgeBaseTags: Scalars['Boolean']['output'];
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


export type MutationCancelGenerationJobArgs = {
  jobId: Scalars['String']['input'];
};


export type MutationCreateApiConnectorArgs = {
  input: CreateApiConnectorInput;
};


export type MutationCreateApiKeyArgs = {
  environmentId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  type?: InputMaybe<PlatformType>;
};


export type MutationCreateApprovalTaskArgs = {
  approvalTask: ApprovalTaskInput;
};


export type MutationCreateDataTableArgs = {
  input: CreateDataTableInput;
};


export type MutationCreateIdentityProviderArgs = {
  input: IdentityProviderInput;
};


export type MutationCreateKnowledgeBaseArgs = {
  knowledgeBase: KnowledgeBaseInput;
  workspaceId: Scalars['ID']['input'];
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


export type MutationDeleteApiConnectorArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteApiKeyArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteApprovalTaskArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteCustomComponentArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteDataTableRowArgs = {
  input: DeleteRowInput;
};


export type MutationDeleteIdentityProviderArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteJobFileLogsArgs = {
  jobId: Scalars['ID']['input'];
};


export type MutationDeleteKnowledgeBaseArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteKnowledgeBaseDocumentArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteKnowledgeBaseDocumentChunkArgs = {
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


export type MutationDisconnectConnectionArgs = {
  connectionId: Scalars['ID']['input'];
};


export type MutationDropDataTableArgs = {
  input: RemoveTableInput;
};


export type MutationDuplicateDataTableArgs = {
  input: DuplicateDataTableInput;
};


export type MutationEnableApiConnectorArgs = {
  enable: Scalars['Boolean']['input'];
  id: Scalars['ID']['input'];
};


export type MutationEnableCustomComponentArgs = {
  enable: Scalars['Boolean']['input'];
  id: Scalars['ID']['input'];
};


export type MutationExportSharedProjectArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
};


export type MutationExportSharedWorkflowArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  workflowId: Scalars['String']['input'];
};


export type MutationGenerateFromDocumentationArgs = {
  input: GenerateFromDocumentationInput;
};


export type MutationGenerateSpecificationArgs = {
  input: GenerateSpecificationInput;
};


export type MutationImportDataTableCsvArgs = {
  input: ImportCsvInput;
};


export type MutationImportOpenApiSpecificationArgs = {
  input: ImportOpenApiSpecificationInput;
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


export type MutationSaveClusterElementTestConfigurationConnectionArgs = {
  clusterElementType: Scalars['String']['input'];
  clusterElementWorkflowNodeName: Scalars['String']['input'];
  connectionId: Scalars['Long']['input'];
  environmentId: Scalars['Long']['input'];
  workflowConnectionKey: Scalars['String']['input'];
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
};


export type MutationSaveWorkflowTestConfigurationConnectionArgs = {
  connectionId: Scalars['Long']['input'];
  environmentId: Scalars['Long']['input'];
  workflowConnectionKey: Scalars['String']['input'];
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
};


export type MutationStartDiscoverEndpointsArgs = {
  input: DiscoverEndpointsInput;
};


export type MutationStartGenerateForEndpointsArgs = {
  input: GenerateForEndpointsInput;
};


export type MutationStartGenerateFromDocumentationPreviewArgs = {
  input: GenerateFromDocumentationInput;
};


export type MutationTestClusterElementScriptArgs = {
  clusterElementType: Scalars['String']['input'];
  clusterElementWorkflowNodeName: Scalars['String']['input'];
  environmentId: Scalars['Long']['input'];
  inputParameters?: InputMaybe<Scalars['Map']['input']>;
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
};


export type MutationTestWorkflowNodeScriptArgs = {
  environmentId: Scalars['Long']['input'];
  inputParameters?: InputMaybe<Scalars['Map']['input']>;
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
};


export type MutationUpdateApiConnectorArgs = {
  id: Scalars['ID']['input'];
  input: UpdateApiConnectorInput;
};


export type MutationUpdateApiKeyArgs = {
  id: Scalars['ID']['input'];
  name: Scalars['String']['input'];
};


export type MutationUpdateApprovalTaskArgs = {
  approvalTask: ApprovalTaskInput;
};


export type MutationUpdateDataTableRowArgs = {
  input: UpdateRowInput;
};


export type MutationUpdateDataTableTagsArgs = {
  input: UpdateDataTableTagsInput;
};


export type MutationUpdateIdentityProviderArgs = {
  id: Scalars['ID']['input'];
  input: IdentityProviderInput;
};


export type MutationUpdateKnowledgeBaseArgs = {
  id: Scalars['ID']['input'];
  knowledgeBase: KnowledgeBaseInput;
};


export type MutationUpdateKnowledgeBaseDocumentChunkArgs = {
  id: Scalars['ID']['input'];
  knowledgeBaseDocumentChunk: KnowledgeBaseDocumentChunkInput;
};


export type MutationUpdateKnowledgeBaseDocumentTagsArgs = {
  input: UpdateKnowledgeBaseDocumentTagsInput;
};


export type MutationUpdateKnowledgeBaseTagsArgs = {
  input: UpdateKnowledgeBaseTagsInput;
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

export type ParameterDefinitionInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  example?: InputMaybe<Scalars['String']['input']>;
  location: ParameterLocation;
  name: Scalars['String']['input'];
  required?: InputMaybe<Scalars['Boolean']['input']>;
  type: ParameterType;
};

export enum ParameterLocation {
  Header = 'HEADER',
  Path = 'PATH',
  Query = 'QUERY'
}

export enum ParameterType {
  Array = 'ARRAY',
  Boolean = 'BOOLEAN',
  Integer = 'INTEGER',
  Number = 'NUMBER',
  Object = 'OBJECT',
  String = 'STRING'
}

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

export type ProjectDeploymentSearchResult = SearchResult & {
  __typename?: 'ProjectDeploymentSearchResult';
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  projectName: Scalars['String']['output'];
  type: SearchAssetType;
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

export type ProjectSearchResult = SearchResult & {
  __typename?: 'ProjectSearchResult';
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  type: SearchAssetType;
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
  apiConnector?: Maybe<ApiConnector>;
  apiConnectors: Array<ApiConnector>;
  apiKey?: Maybe<ApiKey>;
  apiKeys?: Maybe<Array<Maybe<ApiKey>>>;
  approvalTask?: Maybe<ApprovalTask>;
  approvalTasks?: Maybe<Array<Maybe<ApprovalTask>>>;
  approvalTasksByIds?: Maybe<Array<Maybe<ApprovalTask>>>;
  authorities: Array<Scalars['String']['output']>;
  automationSearch: Array<SearchResult>;
  clusterElementComponentConnections: Array<ComponentConnection>;
  clusterElementDefinition: ClusterElementDefinition;
  clusterElementDefinitions: Array<ClusterElementDefinition>;
  clusterElementMissingRequiredProperties: Array<Scalars['String']['output']>;
  clusterElementScriptInput?: Maybe<Scalars['Map']['output']>;
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
  customComponent?: Maybe<CustomComponent>;
  customComponentDefinition?: Maybe<CustomComponentDefinition>;
  customComponents: Array<CustomComponent>;
  dataTableRows: Array<DataTableRow>;
  dataTableRowsPage: DataTableRowPage;
  dataTableTags: Array<Tag>;
  dataTableTagsByTable: Array<DataTableTagsEntry>;
  dataTableWebhooks: Array<DataTableWebhook>;
  dataTables: Array<DataTable>;
  editorJobFileLogs: LogPage;
  editorJobFileLogsExist: Scalars['Boolean']['output'];
  editorTaskExecutionFileLogs: Array<LogEntry>;
  endpointDiscoveryStatus?: Maybe<EndpointDiscoveryResult>;
  environments?: Maybe<Array<Maybe<Environment>>>;
  exportDataTableCsv: Scalars['String']['output'];
  generationJobStatus?: Maybe<GenerationJobStatus>;
  identityProvider?: Maybe<IdentityProviderType>;
  identityProviders: Array<Maybe<IdentityProviderType>>;
  integration?: Maybe<Integration>;
  jobFileLogs: LogPage;
  jobFileLogsExist: Scalars['Boolean']['output'];
  knowledgeBase?: Maybe<KnowledgeBase>;
  knowledgeBaseDocument?: Maybe<KnowledgeBaseDocument>;
  knowledgeBaseDocumentStatus?: Maybe<DocumentStatusUpdate>;
  knowledgeBaseDocumentTags?: Maybe<Array<Tag>>;
  knowledgeBaseDocumentTagsByDocument?: Maybe<Array<KnowledgeBaseDocumentTagsEntry>>;
  knowledgeBaseTags?: Maybe<Array<Tag>>;
  knowledgeBaseTagsByKnowledgeBase?: Maybe<Array<KnowledgeBaseTagsEntry>>;
  knowledgeBases?: Maybe<Array<Maybe<KnowledgeBase>>>;
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
  searchKnowledgeBase?: Maybe<Array<Maybe<KnowledgeBaseDocumentChunk>>>;
  sharedProject?: Maybe<SharedProject>;
  sharedWorkflow?: Maybe<SharedWorkflow>;
  taskDispatcherDefinition: TaskDispatcherDefinition;
  taskDispatcherDefinitionVersions: Array<TaskDispatcherDefinition>;
  taskDispatcherDefinitions: Array<TaskDispatcherDefinition>;
  taskExecutionFileLogs: Array<LogEntry>;
  triggerDefinition: TriggerDefinition;
  triggerDefinitions: Array<TriggerDefinition>;
  unifiedApiComponentDefinitions: Array<ComponentDefinition>;
  user?: Maybe<AdminUser>;
  users?: Maybe<AdminUserPage>;
  workflowNodeComponentConnections: Array<ComponentConnection>;
  workflowNodeMissingRequiredProperties: Array<Scalars['String']['output']>;
  workflowNodeScriptInput?: Maybe<Scalars['Map']['output']>;
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


export type QueryApiConnectorArgs = {
  id: Scalars['ID']['input'];
};


export type QueryApiKeyArgs = {
  id: Scalars['ID']['input'];
};


export type QueryApiKeysArgs = {
  environmentId: Scalars['ID']['input'];
  type: PlatformType;
};


export type QueryApprovalTaskArgs = {
  id: Scalars['ID']['input'];
};


export type QueryApprovalTasksByIdsArgs = {
  ids: Array<Scalars['ID']['input']>;
};


export type QueryAutomationSearchArgs = {
  limit?: InputMaybe<Scalars['Int']['input']>;
  query: Scalars['String']['input'];
};


export type QueryClusterElementComponentConnectionsArgs = {
  clusterElementType: Scalars['String']['input'];
  clusterElementWorkflowNodeName: Scalars['String']['input'];
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
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


export type QueryClusterElementMissingRequiredPropertiesArgs = {
  clusterElementType: Scalars['String']['input'];
  clusterElementWorkflowNodeName: Scalars['String']['input'];
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
};


export type QueryClusterElementScriptInputArgs = {
  clusterElementType: Scalars['String']['input'];
  clusterElementWorkflowNodeName: Scalars['String']['input'];
  environmentId: Scalars['Long']['input'];
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
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


export type QueryCustomComponentArgs = {
  id: Scalars['ID']['input'];
};


export type QueryCustomComponentDefinitionArgs = {
  id: Scalars['ID']['input'];
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


export type QueryEditorJobFileLogsArgs = {
  filter?: InputMaybe<LogFilterInput>;
  jobId: Scalars['ID']['input'];
  page?: InputMaybe<Scalars['Int']['input']>;
  size?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryEditorJobFileLogsExistArgs = {
  jobId: Scalars['ID']['input'];
};


export type QueryEditorTaskExecutionFileLogsArgs = {
  jobId: Scalars['ID']['input'];
  taskExecutionId: Scalars['ID']['input'];
};


export type QueryEndpointDiscoveryStatusArgs = {
  jobId: Scalars['String']['input'];
};


export type QueryExportDataTableCsvArgs = {
  environmentId: Scalars['ID']['input'];
  tableId: Scalars['ID']['input'];
};


export type QueryGenerationJobStatusArgs = {
  jobId: Scalars['String']['input'];
};


export type QueryIdentityProviderArgs = {
  id: Scalars['ID']['input'];
};


export type QueryIntegrationArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryJobFileLogsArgs = {
  filter?: InputMaybe<LogFilterInput>;
  jobId: Scalars['ID']['input'];
  page?: InputMaybe<Scalars['Int']['input']>;
  size?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryJobFileLogsExistArgs = {
  jobId: Scalars['ID']['input'];
};


export type QueryKnowledgeBaseArgs = {
  id: Scalars['ID']['input'];
};


export type QueryKnowledgeBaseDocumentArgs = {
  id: Scalars['ID']['input'];
};


export type QueryKnowledgeBaseDocumentStatusArgs = {
  id: Scalars['ID']['input'];
};


export type QueryKnowledgeBasesArgs = {
  workspaceId: Scalars['ID']['input'];
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


export type QuerySearchKnowledgeBaseArgs = {
  id: Scalars['ID']['input'];
  metadataFilters?: InputMaybe<Scalars['String']['input']>;
  query: Scalars['String']['input'];
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


export type QueryTaskExecutionFileLogsArgs = {
  jobId: Scalars['ID']['input'];
  taskExecutionId: Scalars['ID']['input'];
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


export type QueryWorkflowNodeComponentConnectionsArgs = {
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
};


export type QueryWorkflowNodeMissingRequiredPropertiesArgs = {
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
};


export type QueryWorkflowNodeScriptInputArgs = {
  environmentId: Scalars['Long']['input'];
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
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

export type RequestBodyDefinitionInput = {
  contentType: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
  required?: InputMaybe<Scalars['Boolean']['input']>;
  schema: Scalars['String']['input'];
};

export type Resources = {
  __typename?: 'Resources';
  documentationUrl?: Maybe<Scalars['String']['output']>;
};

export type ResponseDefinitionInput = {
  contentType?: InputMaybe<Scalars['String']['input']>;
  description: Scalars['String']['input'];
  schema?: InputMaybe<Scalars['String']['input']>;
  statusCode: Scalars['String']['input'];
};

export type ScriptTestExecution = {
  __typename?: 'ScriptTestExecution';
  error?: Maybe<ExecutionError>;
  output?: Maybe<Scalars['Map']['output']>;
};

export enum SearchAssetType {
  ApiCollection = 'API_COLLECTION',
  ApiEndpoint = 'API_ENDPOINT',
  Connection = 'CONNECTION',
  DataTable = 'DATA_TABLE',
  Deployment = 'DEPLOYMENT',
  KnowledgeBase = 'KNOWLEDGE_BASE',
  KnowledgeBaseDocument = 'KNOWLEDGE_BASE_DOCUMENT',
  Project = 'PROJECT',
  Workflow = 'WORKFLOW'
}

export type SearchResult = {
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  type: SearchAssetType;
};

export type SelectedEndpointInput = {
  method: Scalars['String']['input'];
  path: Scalars['String']['input'];
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

export type UpdateApiConnectorInput = {
  connectorVersion?: InputMaybe<Scalars['Int']['input']>;
  description?: InputMaybe<Scalars['String']['input']>;
  icon?: InputMaybe<Scalars['String']['input']>;
  name?: InputMaybe<Scalars['String']['input']>;
  title?: InputMaybe<Scalars['String']['input']>;
};

export type UpdateDataTableTagsInput = {
  tableId: Scalars['ID']['input'];
  tags?: InputMaybe<Array<TagInput>>;
};

export type UpdateKnowledgeBaseDocumentTagsInput = {
  knowledgeBaseDocumentId: Scalars['ID']['input'];
  tags?: InputMaybe<Array<TagInput>>;
};

export type UpdateKnowledgeBaseTagsInput = {
  knowledgeBaseId: Scalars['ID']['input'];
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

export type WorkflowSearchResult = SearchResult & {
  __typename?: 'WorkflowSearchResult';
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  label: Scalars['String']['output'];
  name: Scalars['String']['output'];
  projectId: Scalars['ID']['output'];
  type: SearchAssetType;
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

export type ApprovalTaskQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type ApprovalTaskQuery = { __typename?: 'Query', approvalTask?: { __typename?: 'ApprovalTask', createdBy?: string | null, createdDate?: string | null, description?: string | null, id: string, lastModifiedBy?: string | null, lastModifiedDate?: string | null, name: string, version: number } | null };

export type ApprovalTasksQueryVariables = Exact<{ [key: string]: never; }>;


export type ApprovalTasksQuery = { __typename?: 'Query', approvalTasks?: Array<{ __typename?: 'ApprovalTask', createdBy?: string | null, createdDate?: string | null, description?: string | null, id: string, lastModifiedBy?: string | null, lastModifiedDate?: string | null, name: string, version: number } | null> | null };

export type CreateApprovalTaskMutationVariables = Exact<{
  approvalTask: ApprovalTaskInput;
}>;


export type CreateApprovalTaskMutation = { __typename?: 'Mutation', createApprovalTask?: { __typename?: 'ApprovalTask', description?: string | null, id: string, name: string } | null };

export type DeleteApprovalTaskMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteApprovalTaskMutation = { __typename?: 'Mutation', deleteApprovalTask?: boolean | null };

export type UpdateApprovalTaskMutationVariables = Exact<{
  approvalTask: ApprovalTaskInput;
}>;


export type UpdateApprovalTaskMutation = { __typename?: 'Mutation', updateApprovalTask?: { __typename?: 'ApprovalTask', description?: string | null, id: string, name: string, version: number } | null };

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

export type DisconnectConnectionMutationVariables = Exact<{
  connectionId: Scalars['ID']['input'];
}>;


export type DisconnectConnectionMutation = { __typename?: 'Mutation', disconnectConnection: boolean };

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

export type CreateKnowledgeBaseMutationVariables = Exact<{
  knowledgeBase: KnowledgeBaseInput;
  workspaceId: Scalars['ID']['input'];
}>;


export type CreateKnowledgeBaseMutation = { __typename?: 'Mutation', createKnowledgeBase?: { __typename?: 'KnowledgeBase', id: string, name: string } | null };

export type DeleteKnowledgeBaseMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteKnowledgeBaseMutation = { __typename?: 'Mutation', deleteKnowledgeBase?: boolean | null };

export type DeleteKnowledgeBaseDocumentMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteKnowledgeBaseDocumentMutation = { __typename?: 'Mutation', deleteKnowledgeBaseDocument?: boolean | null };

export type DeleteKnowledgeBaseDocumentChunkMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteKnowledgeBaseDocumentChunkMutation = { __typename?: 'Mutation', deleteKnowledgeBaseDocumentChunk?: boolean | null };

export type KnowledgeBaseQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type KnowledgeBaseQuery = { __typename?: 'Query', knowledgeBase?: { __typename?: 'KnowledgeBase', id: string, name: string, description?: string | null, maxChunkSize?: number | null, minChunkSizeChars?: number | null, overlap?: number | null, createdDate?: any | null, lastModifiedDate?: any | null, documents?: Array<{ __typename?: 'KnowledgeBaseDocument', id: string, name: string, status: number, createdDate?: any | null, document?: { __typename?: 'FileEntry', name: string, extension?: string | null, mimeType?: string | null, url: string } | null, tags?: Array<{ __typename?: 'Tag', id: string, name: string }> | null, chunks?: Array<{ __typename?: 'KnowledgeBaseDocumentChunk', id: string, knowledgeBaseDocumentId: string, content?: string | null, metadata?: any | null } | null> | null } | null> | null } | null };

export type KnowledgeBaseDocumentStatusQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type KnowledgeBaseDocumentStatusQuery = { __typename?: 'Query', knowledgeBaseDocumentStatus?: { __typename?: 'DocumentStatusUpdate', documentId: string, status: number, timestamp: any, message?: string | null } | null };

export type KnowledgeBaseDocumentTagsQueryVariables = Exact<{ [key: string]: never; }>;


export type KnowledgeBaseDocumentTagsQuery = { __typename?: 'Query', knowledgeBaseDocumentTags?: Array<{ __typename?: 'Tag', id: string, name: string }> | null };

export type KnowledgeBaseDocumentTagsByDocumentQueryVariables = Exact<{ [key: string]: never; }>;


export type KnowledgeBaseDocumentTagsByDocumentQuery = { __typename?: 'Query', knowledgeBaseDocumentTagsByDocument?: Array<{ __typename?: 'KnowledgeBaseDocumentTagsEntry', knowledgeBaseDocumentId: string, tags: Array<{ __typename?: 'Tag', id: string, name: string }> }> | null };

export type KnowledgeBaseTagsQueryVariables = Exact<{ [key: string]: never; }>;


export type KnowledgeBaseTagsQuery = { __typename?: 'Query', knowledgeBaseTags?: Array<{ __typename?: 'Tag', id: string, name: string }> | null };

export type KnowledgeBaseTagsByKnowledgeBaseQueryVariables = Exact<{ [key: string]: never; }>;


export type KnowledgeBaseTagsByKnowledgeBaseQuery = { __typename?: 'Query', knowledgeBaseTagsByKnowledgeBase?: Array<{ __typename?: 'KnowledgeBaseTagsEntry', knowledgeBaseId: string, tags: Array<{ __typename?: 'Tag', id: string, name: string }> }> | null };

export type KnowledgeBasesQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type KnowledgeBasesQuery = { __typename?: 'Query', knowledgeBases?: Array<{ __typename?: 'KnowledgeBase', id: string, name: string, description?: string | null, maxChunkSize?: number | null, minChunkSizeChars?: number | null, overlap?: number | null, createdDate?: any | null, lastModifiedDate?: any | null } | null> | null };

export type SearchKnowledgeBaseQueryVariables = Exact<{
  id: Scalars['ID']['input'];
  query: Scalars['String']['input'];
  metadataFilters?: InputMaybe<Scalars['String']['input']>;
}>;


export type SearchKnowledgeBaseQuery = { __typename?: 'Query', searchKnowledgeBase?: Array<{ __typename?: 'KnowledgeBaseDocumentChunk', id: string, knowledgeBaseDocumentId: string, content?: string | null, metadata?: any | null, score?: number | null } | null> | null };

export type UpdateKnowledgeBaseMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  knowledgeBase: KnowledgeBaseInput;
}>;


export type UpdateKnowledgeBaseMutation = { __typename?: 'Mutation', updateKnowledgeBase?: { __typename?: 'KnowledgeBase', id: string, name: string, description?: string | null, maxChunkSize?: number | null, minChunkSizeChars?: number | null, overlap?: number | null } | null };

export type UpdateKnowledgeBaseDocumentChunkMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  knowledgeBaseDocumentChunk: KnowledgeBaseDocumentChunkInput;
}>;


export type UpdateKnowledgeBaseDocumentChunkMutation = { __typename?: 'Mutation', updateKnowledgeBaseDocumentChunk?: { __typename?: 'KnowledgeBaseDocumentChunk', id: string, knowledgeBaseDocumentId: string, content?: string | null, metadata?: any | null } | null };

export type UpdateKnowledgeBaseDocumentTagsMutationVariables = Exact<{
  input: UpdateKnowledgeBaseDocumentTagsInput;
}>;


export type UpdateKnowledgeBaseDocumentTagsMutation = { __typename?: 'Mutation', updateKnowledgeBaseDocumentTags: boolean };

export type UpdateKnowledgeBaseTagsMutationVariables = Exact<{
  input: UpdateKnowledgeBaseTagsInput;
}>;


export type UpdateKnowledgeBaseTagsMutation = { __typename?: 'Mutation', updateKnowledgeBaseTags: boolean };

export type AutomationSearchQueryVariables = Exact<{
  query: Scalars['String']['input'];
  limit?: InputMaybe<Scalars['Int']['input']>;
}>;


export type AutomationSearchQuery = { __typename?: 'Query', automationSearch: Array<
    | { __typename?: 'ApiCollectionSearchResult', id: string, name: string, description?: string | null, type: SearchAssetType }
    | { __typename?: 'ApiEndpointSearchResult', collectionId: string, path?: string | null, id: string, name: string, description?: string | null, type: SearchAssetType }
    | { __typename?: 'ConnectionSearchResult', id: string, name: string, description?: string | null, type: SearchAssetType }
    | { __typename?: 'DataTableSearchResult', id: string, name: string, description?: string | null, type: SearchAssetType }
    | { __typename?: 'KnowledgeBaseDocumentSearchResult', knowledgeBaseId: string, id: string, name: string, description?: string | null, type: SearchAssetType }
    | { __typename?: 'KnowledgeBaseSearchResult', id: string, name: string, description?: string | null, type: SearchAssetType }
    | { __typename?: 'ProjectDeploymentSearchResult', projectName: string, id: string, name: string, description?: string | null, type: SearchAssetType }
    | { __typename?: 'ProjectSearchResult', id: string, name: string, description?: string | null, type: SearchAssetType }
    | { __typename?: 'WorkflowSearchResult', projectId: string, label: string, id: string, name: string, description?: string | null, type: SearchAssetType }
  > };

export type ConnectedUserProjectsQueryVariables = Exact<{
  connectedUserId?: InputMaybe<Scalars['ID']['input']>;
  environmentId?: InputMaybe<Scalars['ID']['input']>;
}>;


export type ConnectedUserProjectsQuery = { __typename?: 'Query', connectedUserProjects: Array<{ __typename?: 'ConnectedUserProject', id: string, environmentId: string, lastExecutionDate?: string | null, projectId: string, projectVersion?: number | null, connectedUser: { __typename?: 'ConnectedUser', id: string, environmentId: string, externalId: string }, connectedUserProjectWorkflows: Array<{ __typename?: 'ConnectedUserProjectWorkflow', id: string, connectedUserId: string, enabled: boolean, lastExecutionDate?: string | null, projectId: string, workflowUuid: string, workflowVersion: number, workflow: { __typename?: 'Workflow', id: string, label: string, triggers: Array<{ __typename?: 'WorkflowTrigger', name: string, type: string, parameters?: any | null }> } }> }> };

export type IntegrationByIdQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type IntegrationByIdQuery = { __typename?: 'Query', integration?: { __typename?: 'Integration', id: string, name: string } | null };

export type ApiConnectorQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type ApiConnectorQuery = { __typename?: 'Query', apiConnector?: { __typename?: 'ApiConnector', id: string, name: string, title?: string | null, description?: string | null, icon?: string | null, connectorVersion: number, enabled?: boolean | null, specification?: string | null, definition?: string | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null, endpoints?: Array<{ __typename?: 'ApiConnectorEndpoint', id: string, name: string, description?: string | null, path?: string | null, httpMethod?: HttpMethod | null }> | null } | null };

export type ApiConnectorsQueryVariables = Exact<{ [key: string]: never; }>;


export type ApiConnectorsQuery = { __typename?: 'Query', apiConnectors: Array<{ __typename?: 'ApiConnector', id: string, name: string, title?: string | null, description?: string | null, icon?: string | null, connectorVersion: number, enabled?: boolean | null, specification?: string | null, definition?: string | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null, endpoints?: Array<{ __typename?: 'ApiConnectorEndpoint', id: string, name: string, description?: string | null, path?: string | null, httpMethod?: HttpMethod | null }> | null }> };

export type CancelGenerationJobMutationVariables = Exact<{
  jobId: Scalars['String']['input'];
}>;


export type CancelGenerationJobMutation = { __typename?: 'Mutation', cancelGenerationJob: boolean };

export type CreateApiConnectorMutationVariables = Exact<{
  input: CreateApiConnectorInput;
}>;


export type CreateApiConnectorMutation = { __typename?: 'Mutation', createApiConnector: { __typename?: 'ApiConnector', id: string, name: string, title?: string | null, description?: string | null, icon?: string | null, connectorVersion: number, enabled?: boolean | null, specification?: string | null, definition?: string | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null, endpoints?: Array<{ __typename?: 'ApiConnectorEndpoint', id: string, name: string, description?: string | null, path?: string | null, httpMethod?: HttpMethod | null }> | null } };

export type DeleteApiConnectorMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteApiConnectorMutation = { __typename?: 'Mutation', deleteApiConnector: boolean };

export type EnableApiConnectorMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  enable: Scalars['Boolean']['input'];
}>;


export type EnableApiConnectorMutation = { __typename?: 'Mutation', enableApiConnector: boolean };

export type GenerateSpecificationMutationVariables = Exact<{
  input: GenerateSpecificationInput;
}>;


export type GenerateSpecificationMutation = { __typename?: 'Mutation', generateSpecification: { __typename?: 'GenerateSpecificationResponse', specification?: string | null } };

export type GenerationJobStatusQueryVariables = Exact<{
  jobId: Scalars['String']['input'];
}>;


export type GenerationJobStatusQuery = { __typename?: 'Query', generationJobStatus?: { __typename?: 'GenerationJobStatus', jobId: string, status: GenerationJobStatusEnum, specification?: string | null, errorMessage?: string | null } | null };

export type ImportOpenApiSpecificationMutationVariables = Exact<{
  input: ImportOpenApiSpecificationInput;
}>;


export type ImportOpenApiSpecificationMutation = { __typename?: 'Mutation', importOpenApiSpecification: { __typename?: 'ApiConnector', id: string, name: string, title?: string | null, description?: string | null, icon?: string | null, connectorVersion: number, enabled?: boolean | null, specification?: string | null, definition?: string | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null, endpoints?: Array<{ __typename?: 'ApiConnectorEndpoint', id: string, name: string, description?: string | null, path?: string | null, httpMethod?: HttpMethod | null }> | null } };

export type StartGenerateFromDocumentationPreviewMutationVariables = Exact<{
  input: GenerateFromDocumentationInput;
}>;


export type StartGenerateFromDocumentationPreviewMutation = { __typename?: 'Mutation', startGenerateFromDocumentationPreview: { __typename?: 'GenerationJobStatus', jobId: string, status: GenerationJobStatusEnum, specification?: string | null, errorMessage?: string | null } };

export type UpdateApiConnectorMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: UpdateApiConnectorInput;
}>;


export type UpdateApiConnectorMutation = { __typename?: 'Mutation', updateApiConnector: { __typename?: 'ApiConnector', id: string, name: string, title?: string | null, description?: string | null, icon?: string | null, connectorVersion: number, enabled?: boolean | null, specification?: string | null, definition?: string | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null, endpoints?: Array<{ __typename?: 'ApiConnectorEndpoint', id: string, name: string, description?: string | null, path?: string | null, httpMethod?: HttpMethod | null }> | null } };

export type EditorJobFileLogsQueryVariables = Exact<{
  jobId: Scalars['ID']['input'];
  filter?: InputMaybe<LogFilterInput>;
  page?: InputMaybe<Scalars['Int']['input']>;
  size?: InputMaybe<Scalars['Int']['input']>;
}>;


export type EditorJobFileLogsQuery = { __typename?: 'Query', editorJobFileLogs: { __typename?: 'LogPage', totalElements: number, totalPages: number, pageNumber: number, pageSize: number, hasNext: boolean, hasPrevious: boolean, content: Array<{ __typename?: 'LogEntry', timestamp: string, level: LogLevel, componentName: string, componentOperationName?: string | null, taskExecutionId: string, message: string, exceptionType?: string | null, exceptionMessage?: string | null, stackTrace?: string | null }> } };

export type EditorJobFileLogsExistQueryVariables = Exact<{
  jobId: Scalars['ID']['input'];
}>;


export type EditorJobFileLogsExistQuery = { __typename?: 'Query', editorJobFileLogsExist: boolean };

export type EditorTaskExecutionFileLogsQueryVariables = Exact<{
  jobId: Scalars['ID']['input'];
  taskExecutionId: Scalars['ID']['input'];
}>;


export type EditorTaskExecutionFileLogsQuery = { __typename?: 'Query', editorTaskExecutionFileLogs: Array<{ __typename?: 'LogEntry', timestamp: string, level: LogLevel, componentName: string, componentOperationName?: string | null, taskExecutionId: string, message: string, exceptionType?: string | null, exceptionMessage?: string | null, stackTrace?: string | null }> };

export type JobFileLogsQueryVariables = Exact<{
  jobId: Scalars['ID']['input'];
  filter?: InputMaybe<LogFilterInput>;
  page?: InputMaybe<Scalars['Int']['input']>;
  size?: InputMaybe<Scalars['Int']['input']>;
}>;


export type JobFileLogsQuery = { __typename?: 'Query', jobFileLogs: { __typename?: 'LogPage', totalElements: number, totalPages: number, pageNumber: number, pageSize: number, hasNext: boolean, hasPrevious: boolean, content: Array<{ __typename?: 'LogEntry', timestamp: string, level: LogLevel, componentName: string, componentOperationName?: string | null, taskExecutionId: string, message: string, exceptionType?: string | null, exceptionMessage?: string | null, stackTrace?: string | null }> } };

export type JobFileLogsExistQueryVariables = Exact<{
  jobId: Scalars['ID']['input'];
}>;


export type JobFileLogsExistQuery = { __typename?: 'Query', jobFileLogsExist: boolean };

export type TaskExecutionFileLogsQueryVariables = Exact<{
  jobId: Scalars['ID']['input'];
  taskExecutionId: Scalars['ID']['input'];
}>;


export type TaskExecutionFileLogsQuery = { __typename?: 'Query', taskExecutionFileLogs: Array<{ __typename?: 'LogEntry', timestamp: string, level: LogLevel, componentName: string, componentOperationName?: string | null, taskExecutionId: string, message: string, exceptionType?: string | null, exceptionMessage?: string | null, stackTrace?: string | null }> };

export type AdminApiKeysQueryVariables = Exact<{
  environmentId: Scalars['ID']['input'];
}>;


export type AdminApiKeysQuery = { __typename?: 'Query', adminApiKeys?: Array<{ __typename?: 'ApiKey', id?: string | null, name?: string | null, secretKey?: string | null, lastUsedDate?: any | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null } | null> | null };

export type ApiKeysQueryVariables = Exact<{
  environmentId: Scalars['ID']['input'];
  type: PlatformType;
}>;


export type ApiKeysQuery = { __typename?: 'Query', apiKeys?: Array<{ __typename?: 'ApiKey', id?: string | null, name?: string | null, secretKey?: string | null, lastUsedDate?: any | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null } | null> | null };

export type ClusterElementComponentConnectionsQueryVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
  clusterElementType: Scalars['String']['input'];
  clusterElementWorkflowNodeName: Scalars['String']['input'];
}>;


export type ClusterElementComponentConnectionsQuery = { __typename?: 'Query', clusterElementComponentConnections: Array<{ __typename?: 'ComponentConnection', componentName: string, componentVersion: number, key: string, required: boolean, workflowNodeName: string }> };

export type ClusterElementScriptInputQueryVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
  clusterElementType: Scalars['String']['input'];
  clusterElementWorkflowNodeName: Scalars['String']['input'];
  environmentId: Scalars['Long']['input'];
}>;


export type ClusterElementScriptInputQuery = { __typename?: 'Query', clusterElementScriptInput?: any | null };

export type ComponentDefinitionSearchQueryVariables = Exact<{
  query: Scalars['String']['input'];
}>;


export type ComponentDefinitionSearchQuery = { __typename?: 'Query', componentDefinitionSearch: Array<{ __typename?: 'ComponentDefinition', name: string, title?: string | null, icon?: string | null, description?: string | null, version?: number | null, actionsCount?: number | null, triggersCount?: number | null, clusterElementsCount?: any | null, componentCategories?: Array<{ __typename?: 'ComponentCategory', name: string, label?: string | null }> | null, actions?: Array<{ __typename?: 'ActionDefinition', name: string, title?: string | null, description?: string | null }> | null, triggers?: Array<{ __typename?: 'TriggerDefinition', name: string, title?: string | null, description?: string | null }> | null, clusterElements?: Array<{ __typename?: 'ClusterElementDefinition', type?: { __typename?: 'ClusterElementType', name?: string | null, label?: string | null } | null }> | null }> };

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

export type SaveClusterElementTestConfigurationConnectionMutationVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
  clusterElementType: Scalars['String']['input'];
  clusterElementWorkflowNodeName: Scalars['String']['input'];
  workflowConnectionKey: Scalars['String']['input'];
  connectionId: Scalars['Long']['input'];
  environmentId: Scalars['Long']['input'];
}>;


export type SaveClusterElementTestConfigurationConnectionMutation = { __typename?: 'Mutation', saveClusterElementTestConfigurationConnection?: boolean | null };

export type SaveWorkflowTestConfigurationConnectionMutationVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
  workflowConnectionKey: Scalars['String']['input'];
  connectionId: Scalars['Long']['input'];
  environmentId: Scalars['Long']['input'];
}>;


export type SaveWorkflowTestConfigurationConnectionMutation = { __typename?: 'Mutation', saveWorkflowTestConfigurationConnection?: boolean | null };

export type TestClusterElementScriptMutationVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
  clusterElementType: Scalars['String']['input'];
  clusterElementWorkflowNodeName: Scalars['String']['input'];
  environmentId: Scalars['Long']['input'];
  inputParameters?: InputMaybe<Scalars['Map']['input']>;
}>;


export type TestClusterElementScriptMutation = { __typename?: 'Mutation', testClusterElementScript: { __typename?: 'ScriptTestExecution', output?: any | null, error?: { __typename?: 'ExecutionError', message?: string | null, stackTrace?: Array<string | null> | null } | null } };

export type TestWorkflowNodeScriptMutationVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
  environmentId: Scalars['Long']['input'];
  inputParameters?: InputMaybe<Scalars['Map']['input']>;
}>;


export type TestWorkflowNodeScriptMutation = { __typename?: 'Mutation', testWorkflowNodeScript: { __typename?: 'ScriptTestExecution', output?: any | null, error?: { __typename?: 'ExecutionError', message?: string | null, stackTrace?: Array<string | null> | null } | null } };

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

export type WorkflowNodeComponentConnectionsQueryVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
}>;


export type WorkflowNodeComponentConnectionsQuery = { __typename?: 'Query', workflowNodeComponentConnections: Array<{ __typename?: 'ComponentConnection', componentName: string, componentVersion: number, key: string, required: boolean, workflowNodeName: string }> };

export type WorkflowNodeScriptInputQueryVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
  environmentId: Scalars['Long']['input'];
}>;


export type WorkflowNodeScriptInputQuery = { __typename?: 'Query', workflowNodeScriptInput?: any | null };

export type CustomComponentQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type CustomComponentQuery = { __typename?: 'Query', customComponent?: { __typename?: 'CustomComponent', id: string, name: string, title?: string | null, description?: string | null, icon?: string | null, componentVersion?: number | null, enabled?: boolean | null, language?: CustomComponentLanguage | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null } | null };

export type CustomComponentDefinitionQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type CustomComponentDefinitionQuery = { __typename?: 'Query', customComponentDefinition?: { __typename?: 'CustomComponentDefinition', actions: Array<{ __typename?: 'CustomComponentActionDefinition', name: string, title?: string | null, description?: string | null }>, triggers: Array<{ __typename?: 'CustomComponentTriggerDefinition', name: string, title?: string | null, description?: string | null }> } | null };

export type CustomComponentsQueryVariables = Exact<{ [key: string]: never; }>;


export type CustomComponentsQuery = { __typename?: 'Query', customComponents: Array<{ __typename?: 'CustomComponent', id: string, name: string, title?: string | null, description?: string | null, icon?: string | null, componentVersion?: number | null, enabled?: boolean | null, language?: CustomComponentLanguage | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null }> };

export type DeleteCustomComponentMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteCustomComponentMutation = { __typename?: 'Mutation', deleteCustomComponent: boolean };

export type EnableCustomComponentMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  enable: Scalars['Boolean']['input'];
}>;


export type EnableCustomComponentMutation = { __typename?: 'Mutation', enableCustomComponent: boolean };

export type AuthoritiesQueryVariables = Exact<{ [key: string]: never; }>;


export type AuthoritiesQuery = { __typename?: 'Query', authorities: Array<string> };

export type CreateIdentityProviderMutationVariables = Exact<{
  input: IdentityProviderInput;
}>;


export type CreateIdentityProviderMutation = { __typename?: 'Mutation', createIdentityProvider: { __typename?: 'IdentityProviderType', autoProvision: boolean, clientId: string, createdBy?: string | null, createdDate?: any | null, defaultAuthority: string, domains: Array<string>, enabled: boolean, enforced: boolean, id: string, issuerUri: string, lastModifiedBy?: string | null, lastModifiedDate?: any | null, metadataUri?: string | null, mfaMethod?: string | null, mfaRequired: boolean, name: string, nameIdFormat?: string | null, scopes: string, signingCertificate?: string | null, type: string } };

export type DeleteIdentityProviderMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteIdentityProviderMutation = { __typename?: 'Mutation', deleteIdentityProvider: boolean };

export type DeleteUserMutationVariables = Exact<{
  login: Scalars['String']['input'];
}>;


export type DeleteUserMutation = { __typename?: 'Mutation', deleteUser: boolean };

export type IdentityProviderQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type IdentityProviderQuery = { __typename?: 'Query', identityProvider?: { __typename?: 'IdentityProviderType', autoProvision: boolean, clientId: string, createdBy?: string | null, createdDate?: any | null, defaultAuthority: string, domains: Array<string>, enabled: boolean, enforced: boolean, id: string, issuerUri: string, lastModifiedBy?: string | null, lastModifiedDate?: any | null, metadataUri?: string | null, mfaMethod?: string | null, mfaRequired: boolean, name: string, nameIdFormat?: string | null, scopes: string, signingCertificate?: string | null, type: string } | null };

export type IdentityProvidersQueryVariables = Exact<{ [key: string]: never; }>;


export type IdentityProvidersQuery = { __typename?: 'Query', identityProviders: Array<{ __typename?: 'IdentityProviderType', autoProvision: boolean, clientId: string, createdBy?: string | null, createdDate?: any | null, defaultAuthority: string, domains: Array<string>, enabled: boolean, enforced: boolean, id: string, issuerUri: string, lastModifiedBy?: string | null, lastModifiedDate?: any | null, metadataUri?: string | null, mfaMethod?: string | null, mfaRequired: boolean, name: string, nameIdFormat?: string | null, scopes: string, signingCertificate?: string | null, type: string } | null> };

export type InviteUserMutationVariables = Exact<{
  email: Scalars['String']['input'];
  password: Scalars['String']['input'];
  role: Scalars['String']['input'];
}>;


export type InviteUserMutation = { __typename?: 'Mutation', inviteUser: boolean };

export type UpdateIdentityProviderMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: IdentityProviderInput;
}>;


export type UpdateIdentityProviderMutation = { __typename?: 'Mutation', updateIdentityProvider: { __typename?: 'IdentityProviderType', autoProvision: boolean, clientId: string, createdBy?: string | null, createdDate?: any | null, defaultAuthority: string, domains: Array<string>, enabled: boolean, enforced: boolean, id: string, issuerUri: string, lastModifiedBy?: string | null, lastModifiedDate?: any | null, metadataUri?: string | null, mfaMethod?: string | null, mfaRequired: boolean, name: string, nameIdFormat?: string | null, scopes: string, signingCertificate?: string | null, type: string } };

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



export const ApprovalTaskDocument = `
    query approvalTask($id: ID!) {
  approvalTask(id: $id) {
    createdBy
    createdDate
    description
    id
    lastModifiedBy
    lastModifiedDate
    name
    version
  }
}
    `;

export const useApprovalTaskQuery = <
      TData = ApprovalTaskQuery,
      TError = unknown
    >(
      variables: ApprovalTaskQueryVariables,
      options?: Omit<UseQueryOptions<ApprovalTaskQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ApprovalTaskQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ApprovalTaskQuery, TError, TData>(
      {
    queryKey: ['approvalTask', variables],
    queryFn: fetcher<ApprovalTaskQuery, ApprovalTaskQueryVariables>(ApprovalTaskDocument, variables),
    ...options
  }
    )};

export const ApprovalTasksDocument = `
    query approvalTasks {
  approvalTasks {
    createdBy
    createdDate
    description
    id
    lastModifiedBy
    lastModifiedDate
    name
    version
  }
}
    `;

export const useApprovalTasksQuery = <
      TData = ApprovalTasksQuery,
      TError = unknown
    >(
      variables?: ApprovalTasksQueryVariables,
      options?: Omit<UseQueryOptions<ApprovalTasksQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ApprovalTasksQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ApprovalTasksQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['approvalTasks'] : ['approvalTasks', variables],
    queryFn: fetcher<ApprovalTasksQuery, ApprovalTasksQueryVariables>(ApprovalTasksDocument, variables),
    ...options
  }
    )};

export const CreateApprovalTaskDocument = `
    mutation createApprovalTask($approvalTask: ApprovalTaskInput!) {
  createApprovalTask(approvalTask: $approvalTask) {
    description
    id
    name
  }
}
    `;

export const useCreateApprovalTaskMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateApprovalTaskMutation, TError, CreateApprovalTaskMutationVariables, TContext>) => {
    
    return useMutation<CreateApprovalTaskMutation, TError, CreateApprovalTaskMutationVariables, TContext>(
      {
    mutationKey: ['createApprovalTask'],
    mutationFn: (variables?: CreateApprovalTaskMutationVariables) => fetcher<CreateApprovalTaskMutation, CreateApprovalTaskMutationVariables>(CreateApprovalTaskDocument, variables)(),
    ...options
  }
    )};

export const DeleteApprovalTaskDocument = `
    mutation deleteApprovalTask($id: ID!) {
  deleteApprovalTask(id: $id)
}
    `;

export const useDeleteApprovalTaskMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteApprovalTaskMutation, TError, DeleteApprovalTaskMutationVariables, TContext>) => {
    
    return useMutation<DeleteApprovalTaskMutation, TError, DeleteApprovalTaskMutationVariables, TContext>(
      {
    mutationKey: ['deleteApprovalTask'],
    mutationFn: (variables?: DeleteApprovalTaskMutationVariables) => fetcher<DeleteApprovalTaskMutation, DeleteApprovalTaskMutationVariables>(DeleteApprovalTaskDocument, variables)(),
    ...options
  }
    )};

export const UpdateApprovalTaskDocument = `
    mutation updateApprovalTask($approvalTask: ApprovalTaskInput!) {
  updateApprovalTask(approvalTask: $approvalTask) {
    description
    id
    name
    version
  }
}
    `;

export const useUpdateApprovalTaskMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateApprovalTaskMutation, TError, UpdateApprovalTaskMutationVariables, TContext>) => {
    
    return useMutation<UpdateApprovalTaskMutation, TError, UpdateApprovalTaskMutationVariables, TContext>(
      {
    mutationKey: ['updateApprovalTask'],
    mutationFn: (variables?: UpdateApprovalTaskMutationVariables) => fetcher<UpdateApprovalTaskMutation, UpdateApprovalTaskMutationVariables>(UpdateApprovalTaskDocument, variables)(),
    ...options
  }
    )};

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

export const DisconnectConnectionDocument = `
    mutation DisconnectConnection($connectionId: ID!) {
  disconnectConnection(connectionId: $connectionId)
}
    `;

export const useDisconnectConnectionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DisconnectConnectionMutation, TError, DisconnectConnectionMutationVariables, TContext>) => {
    
    return useMutation<DisconnectConnectionMutation, TError, DisconnectConnectionMutationVariables, TContext>(
      {
    mutationKey: ['DisconnectConnection'],
    mutationFn: (variables?: DisconnectConnectionMutationVariables) => fetcher<DisconnectConnectionMutation, DisconnectConnectionMutationVariables>(DisconnectConnectionDocument, variables)(),
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

export const CreateKnowledgeBaseDocument = `
    mutation createKnowledgeBase($knowledgeBase: KnowledgeBaseInput!, $workspaceId: ID!) {
  createKnowledgeBase(knowledgeBase: $knowledgeBase, workspaceId: $workspaceId) {
    id
    name
  }
}
    `;

export const useCreateKnowledgeBaseMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateKnowledgeBaseMutation, TError, CreateKnowledgeBaseMutationVariables, TContext>) => {
    
    return useMutation<CreateKnowledgeBaseMutation, TError, CreateKnowledgeBaseMutationVariables, TContext>(
      {
    mutationKey: ['createKnowledgeBase'],
    mutationFn: (variables?: CreateKnowledgeBaseMutationVariables) => fetcher<CreateKnowledgeBaseMutation, CreateKnowledgeBaseMutationVariables>(CreateKnowledgeBaseDocument, variables)(),
    ...options
  }
    )};

export const DeleteKnowledgeBaseDocument = `
    mutation deleteKnowledgeBase($id: ID!) {
  deleteKnowledgeBase(id: $id)
}
    `;

export const useDeleteKnowledgeBaseMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteKnowledgeBaseMutation, TError, DeleteKnowledgeBaseMutationVariables, TContext>) => {
    
    return useMutation<DeleteKnowledgeBaseMutation, TError, DeleteKnowledgeBaseMutationVariables, TContext>(
      {
    mutationKey: ['deleteKnowledgeBase'],
    mutationFn: (variables?: DeleteKnowledgeBaseMutationVariables) => fetcher<DeleteKnowledgeBaseMutation, DeleteKnowledgeBaseMutationVariables>(DeleteKnowledgeBaseDocument, variables)(),
    ...options
  }
    )};

export const DeleteKnowledgeBaseDocumentDocument = `
    mutation deleteKnowledgeBaseDocument($id: ID!) {
  deleteKnowledgeBaseDocument(id: $id)
}
    `;

export const useDeleteKnowledgeBaseDocumentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteKnowledgeBaseDocumentMutation, TError, DeleteKnowledgeBaseDocumentMutationVariables, TContext>) => {
    
    return useMutation<DeleteKnowledgeBaseDocumentMutation, TError, DeleteKnowledgeBaseDocumentMutationVariables, TContext>(
      {
    mutationKey: ['deleteKnowledgeBaseDocument'],
    mutationFn: (variables?: DeleteKnowledgeBaseDocumentMutationVariables) => fetcher<DeleteKnowledgeBaseDocumentMutation, DeleteKnowledgeBaseDocumentMutationVariables>(DeleteKnowledgeBaseDocumentDocument, variables)(),
    ...options
  }
    )};

export const DeleteKnowledgeBaseDocumentChunkDocument = `
    mutation deleteKnowledgeBaseDocumentChunk($id: ID!) {
  deleteKnowledgeBaseDocumentChunk(id: $id)
}
    `;

export const useDeleteKnowledgeBaseDocumentChunkMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteKnowledgeBaseDocumentChunkMutation, TError, DeleteKnowledgeBaseDocumentChunkMutationVariables, TContext>) => {
    
    return useMutation<DeleteKnowledgeBaseDocumentChunkMutation, TError, DeleteKnowledgeBaseDocumentChunkMutationVariables, TContext>(
      {
    mutationKey: ['deleteKnowledgeBaseDocumentChunk'],
    mutationFn: (variables?: DeleteKnowledgeBaseDocumentChunkMutationVariables) => fetcher<DeleteKnowledgeBaseDocumentChunkMutation, DeleteKnowledgeBaseDocumentChunkMutationVariables>(DeleteKnowledgeBaseDocumentChunkDocument, variables)(),
    ...options
  }
    )};

export const KnowledgeBaseDocument = `
    query knowledgeBase($id: ID!) {
  knowledgeBase(id: $id) {
    id
    name
    description
    maxChunkSize
    minChunkSizeChars
    overlap
    documents {
      id
      name
      document {
        name
        extension
        mimeType
        url
      }
      status
      tags {
        id
        name
      }
      createdDate
      chunks {
        id
        knowledgeBaseDocumentId
        content
        metadata
      }
    }
    createdDate
    lastModifiedDate
  }
}
    `;

export const useKnowledgeBaseQuery = <
      TData = KnowledgeBaseQuery,
      TError = unknown
    >(
      variables: KnowledgeBaseQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseQuery, TError, TData>(
      {
    queryKey: ['knowledgeBase', variables],
    queryFn: fetcher<KnowledgeBaseQuery, KnowledgeBaseQueryVariables>(KnowledgeBaseDocument, variables),
    ...options
  }
    )};

export const KnowledgeBaseDocumentStatusDocument = `
    query knowledgeBaseDocumentStatus($id: ID!) {
  knowledgeBaseDocumentStatus(id: $id) {
    documentId
    status
    timestamp
    message
  }
}
    `;

export const useKnowledgeBaseDocumentStatusQuery = <
      TData = KnowledgeBaseDocumentStatusQuery,
      TError = unknown
    >(
      variables: KnowledgeBaseDocumentStatusQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseDocumentStatusQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseDocumentStatusQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseDocumentStatusQuery, TError, TData>(
      {
    queryKey: ['knowledgeBaseDocumentStatus', variables],
    queryFn: fetcher<KnowledgeBaseDocumentStatusQuery, KnowledgeBaseDocumentStatusQueryVariables>(KnowledgeBaseDocumentStatusDocument, variables),
    ...options
  }
    )};

export const KnowledgeBaseDocumentTagsDocument = `
    query knowledgeBaseDocumentTags {
  knowledgeBaseDocumentTags {
    id
    name
  }
}
    `;

export const useKnowledgeBaseDocumentTagsQuery = <
      TData = KnowledgeBaseDocumentTagsQuery,
      TError = unknown
    >(
      variables?: KnowledgeBaseDocumentTagsQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseDocumentTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseDocumentTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseDocumentTagsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['knowledgeBaseDocumentTags'] : ['knowledgeBaseDocumentTags', variables],
    queryFn: fetcher<KnowledgeBaseDocumentTagsQuery, KnowledgeBaseDocumentTagsQueryVariables>(KnowledgeBaseDocumentTagsDocument, variables),
    ...options
  }
    )};

export const KnowledgeBaseDocumentTagsByDocumentDocument = `
    query knowledgeBaseDocumentTagsByDocument {
  knowledgeBaseDocumentTagsByDocument {
    knowledgeBaseDocumentId
    tags {
      id
      name
    }
  }
}
    `;

export const useKnowledgeBaseDocumentTagsByDocumentQuery = <
      TData = KnowledgeBaseDocumentTagsByDocumentQuery,
      TError = unknown
    >(
      variables?: KnowledgeBaseDocumentTagsByDocumentQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseDocumentTagsByDocumentQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseDocumentTagsByDocumentQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseDocumentTagsByDocumentQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['knowledgeBaseDocumentTagsByDocument'] : ['knowledgeBaseDocumentTagsByDocument', variables],
    queryFn: fetcher<KnowledgeBaseDocumentTagsByDocumentQuery, KnowledgeBaseDocumentTagsByDocumentQueryVariables>(KnowledgeBaseDocumentTagsByDocumentDocument, variables),
    ...options
  }
    )};

export const KnowledgeBaseTagsDocument = `
    query knowledgeBaseTags {
  knowledgeBaseTags {
    id
    name
  }
}
    `;

export const useKnowledgeBaseTagsQuery = <
      TData = KnowledgeBaseTagsQuery,
      TError = unknown
    >(
      variables?: KnowledgeBaseTagsQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseTagsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['knowledgeBaseTags'] : ['knowledgeBaseTags', variables],
    queryFn: fetcher<KnowledgeBaseTagsQuery, KnowledgeBaseTagsQueryVariables>(KnowledgeBaseTagsDocument, variables),
    ...options
  }
    )};

export const KnowledgeBaseTagsByKnowledgeBaseDocument = `
    query knowledgeBaseTagsByKnowledgeBase {
  knowledgeBaseTagsByKnowledgeBase {
    knowledgeBaseId
    tags {
      id
      name
    }
  }
}
    `;

export const useKnowledgeBaseTagsByKnowledgeBaseQuery = <
      TData = KnowledgeBaseTagsByKnowledgeBaseQuery,
      TError = unknown
    >(
      variables?: KnowledgeBaseTagsByKnowledgeBaseQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseTagsByKnowledgeBaseQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseTagsByKnowledgeBaseQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseTagsByKnowledgeBaseQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['knowledgeBaseTagsByKnowledgeBase'] : ['knowledgeBaseTagsByKnowledgeBase', variables],
    queryFn: fetcher<KnowledgeBaseTagsByKnowledgeBaseQuery, KnowledgeBaseTagsByKnowledgeBaseQueryVariables>(KnowledgeBaseTagsByKnowledgeBaseDocument, variables),
    ...options
  }
    )};

export const KnowledgeBasesDocument = `
    query knowledgeBases($workspaceId: ID!) {
  knowledgeBases(workspaceId: $workspaceId) {
    id
    name
    description
    maxChunkSize
    minChunkSizeChars
    overlap
    createdDate
    lastModifiedDate
  }
}
    `;

export const useKnowledgeBasesQuery = <
      TData = KnowledgeBasesQuery,
      TError = unknown
    >(
      variables: KnowledgeBasesQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBasesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBasesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBasesQuery, TError, TData>(
      {
    queryKey: ['knowledgeBases', variables],
    queryFn: fetcher<KnowledgeBasesQuery, KnowledgeBasesQueryVariables>(KnowledgeBasesDocument, variables),
    ...options
  }
    )};

export const SearchKnowledgeBaseDocument = `
    query searchKnowledgeBase($id: ID!, $query: String!, $metadataFilters: String) {
  searchKnowledgeBase(id: $id, query: $query, metadataFilters: $metadataFilters) {
    id
    knowledgeBaseDocumentId
    content
    metadata
    score
  }
}
    `;

export const useSearchKnowledgeBaseQuery = <
      TData = SearchKnowledgeBaseQuery,
      TError = unknown
    >(
      variables: SearchKnowledgeBaseQueryVariables,
      options?: Omit<UseQueryOptions<SearchKnowledgeBaseQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<SearchKnowledgeBaseQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<SearchKnowledgeBaseQuery, TError, TData>(
      {
    queryKey: ['searchKnowledgeBase', variables],
    queryFn: fetcher<SearchKnowledgeBaseQuery, SearchKnowledgeBaseQueryVariables>(SearchKnowledgeBaseDocument, variables),
    ...options
  }
    )};

export const UpdateKnowledgeBaseDocument = `
    mutation updateKnowledgeBase($id: ID!, $knowledgeBase: KnowledgeBaseInput!) {
  updateKnowledgeBase(id: $id, knowledgeBase: $knowledgeBase) {
    id
    name
    description
    maxChunkSize
    minChunkSizeChars
    overlap
  }
}
    `;

export const useUpdateKnowledgeBaseMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateKnowledgeBaseMutation, TError, UpdateKnowledgeBaseMutationVariables, TContext>) => {
    
    return useMutation<UpdateKnowledgeBaseMutation, TError, UpdateKnowledgeBaseMutationVariables, TContext>(
      {
    mutationKey: ['updateKnowledgeBase'],
    mutationFn: (variables?: UpdateKnowledgeBaseMutationVariables) => fetcher<UpdateKnowledgeBaseMutation, UpdateKnowledgeBaseMutationVariables>(UpdateKnowledgeBaseDocument, variables)(),
    ...options
  }
    )};

export const UpdateKnowledgeBaseDocumentChunkDocument = `
    mutation updateKnowledgeBaseDocumentChunk($id: ID!, $knowledgeBaseDocumentChunk: KnowledgeBaseDocumentChunkInput!) {
  updateKnowledgeBaseDocumentChunk(
    id: $id
    knowledgeBaseDocumentChunk: $knowledgeBaseDocumentChunk
  ) {
    id
    knowledgeBaseDocumentId
    content
    metadata
  }
}
    `;

export const useUpdateKnowledgeBaseDocumentChunkMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateKnowledgeBaseDocumentChunkMutation, TError, UpdateKnowledgeBaseDocumentChunkMutationVariables, TContext>) => {
    
    return useMutation<UpdateKnowledgeBaseDocumentChunkMutation, TError, UpdateKnowledgeBaseDocumentChunkMutationVariables, TContext>(
      {
    mutationKey: ['updateKnowledgeBaseDocumentChunk'],
    mutationFn: (variables?: UpdateKnowledgeBaseDocumentChunkMutationVariables) => fetcher<UpdateKnowledgeBaseDocumentChunkMutation, UpdateKnowledgeBaseDocumentChunkMutationVariables>(UpdateKnowledgeBaseDocumentChunkDocument, variables)(),
    ...options
  }
    )};

export const UpdateKnowledgeBaseDocumentTagsDocument = `
    mutation updateKnowledgeBaseDocumentTags($input: UpdateKnowledgeBaseDocumentTagsInput!) {
  updateKnowledgeBaseDocumentTags(input: $input)
}
    `;

export const useUpdateKnowledgeBaseDocumentTagsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateKnowledgeBaseDocumentTagsMutation, TError, UpdateKnowledgeBaseDocumentTagsMutationVariables, TContext>) => {
    
    return useMutation<UpdateKnowledgeBaseDocumentTagsMutation, TError, UpdateKnowledgeBaseDocumentTagsMutationVariables, TContext>(
      {
    mutationKey: ['updateKnowledgeBaseDocumentTags'],
    mutationFn: (variables?: UpdateKnowledgeBaseDocumentTagsMutationVariables) => fetcher<UpdateKnowledgeBaseDocumentTagsMutation, UpdateKnowledgeBaseDocumentTagsMutationVariables>(UpdateKnowledgeBaseDocumentTagsDocument, variables)(),
    ...options
  }
    )};

export const UpdateKnowledgeBaseTagsDocument = `
    mutation updateKnowledgeBaseTags($input: UpdateKnowledgeBaseTagsInput!) {
  updateKnowledgeBaseTags(input: $input)
}
    `;

export const useUpdateKnowledgeBaseTagsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateKnowledgeBaseTagsMutation, TError, UpdateKnowledgeBaseTagsMutationVariables, TContext>) => {
    
    return useMutation<UpdateKnowledgeBaseTagsMutation, TError, UpdateKnowledgeBaseTagsMutationVariables, TContext>(
      {
    mutationKey: ['updateKnowledgeBaseTags'],
    mutationFn: (variables?: UpdateKnowledgeBaseTagsMutationVariables) => fetcher<UpdateKnowledgeBaseTagsMutation, UpdateKnowledgeBaseTagsMutationVariables>(UpdateKnowledgeBaseTagsDocument, variables)(),
    ...options
  }
    )};

export const AutomationSearchDocument = `
    query automationSearch($query: String!, $limit: Int) {
  automationSearch(query: $query, limit: $limit) {
    id
    name
    description
    type
    ... on WorkflowSearchResult {
      projectId
      label
    }
    ... on ProjectDeploymentSearchResult {
      projectName
    }
    ... on ApiEndpointSearchResult {
      collectionId
      path
    }
    ... on KnowledgeBaseDocumentSearchResult {
      knowledgeBaseId
    }
  }
}
    `;

export const useAutomationSearchQuery = <
      TData = AutomationSearchQuery,
      TError = unknown
    >(
      variables: AutomationSearchQueryVariables,
      options?: Omit<UseQueryOptions<AutomationSearchQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AutomationSearchQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AutomationSearchQuery, TError, TData>(
      {
    queryKey: ['automationSearch', variables],
    queryFn: fetcher<AutomationSearchQuery, AutomationSearchQueryVariables>(AutomationSearchDocument, variables),
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

export const ApiConnectorDocument = `
    query apiConnector($id: ID!) {
  apiConnector(id: $id) {
    id
    name
    title
    description
    icon
    connectorVersion
    enabled
    specification
    definition
    endpoints {
      id
      name
      description
      path
      httpMethod
    }
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `;

export const useApiConnectorQuery = <
      TData = ApiConnectorQuery,
      TError = unknown
    >(
      variables: ApiConnectorQueryVariables,
      options?: Omit<UseQueryOptions<ApiConnectorQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ApiConnectorQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ApiConnectorQuery, TError, TData>(
      {
    queryKey: ['apiConnector', variables],
    queryFn: fetcher<ApiConnectorQuery, ApiConnectorQueryVariables>(ApiConnectorDocument, variables),
    ...options
  }
    )};

export const ApiConnectorsDocument = `
    query apiConnectors {
  apiConnectors {
    id
    name
    title
    description
    icon
    connectorVersion
    enabled
    specification
    definition
    endpoints {
      id
      name
      description
      path
      httpMethod
    }
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `;

export const useApiConnectorsQuery = <
      TData = ApiConnectorsQuery,
      TError = unknown
    >(
      variables?: ApiConnectorsQueryVariables,
      options?: Omit<UseQueryOptions<ApiConnectorsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ApiConnectorsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ApiConnectorsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['apiConnectors'] : ['apiConnectors', variables],
    queryFn: fetcher<ApiConnectorsQuery, ApiConnectorsQueryVariables>(ApiConnectorsDocument, variables),
    ...options
  }
    )};

export const CancelGenerationJobDocument = `
    mutation cancelGenerationJob($jobId: String!) {
  cancelGenerationJob(jobId: $jobId)
}
    `;

export const useCancelGenerationJobMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CancelGenerationJobMutation, TError, CancelGenerationJobMutationVariables, TContext>) => {
    
    return useMutation<CancelGenerationJobMutation, TError, CancelGenerationJobMutationVariables, TContext>(
      {
    mutationKey: ['cancelGenerationJob'],
    mutationFn: (variables?: CancelGenerationJobMutationVariables) => fetcher<CancelGenerationJobMutation, CancelGenerationJobMutationVariables>(CancelGenerationJobDocument, variables)(),
    ...options
  }
    )};

export const CreateApiConnectorDocument = `
    mutation createApiConnector($input: CreateApiConnectorInput!) {
  createApiConnector(input: $input) {
    id
    name
    title
    description
    icon
    connectorVersion
    enabled
    specification
    definition
    endpoints {
      id
      name
      description
      path
      httpMethod
    }
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `;

export const useCreateApiConnectorMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateApiConnectorMutation, TError, CreateApiConnectorMutationVariables, TContext>) => {
    
    return useMutation<CreateApiConnectorMutation, TError, CreateApiConnectorMutationVariables, TContext>(
      {
    mutationKey: ['createApiConnector'],
    mutationFn: (variables?: CreateApiConnectorMutationVariables) => fetcher<CreateApiConnectorMutation, CreateApiConnectorMutationVariables>(CreateApiConnectorDocument, variables)(),
    ...options
  }
    )};

export const DeleteApiConnectorDocument = `
    mutation deleteApiConnector($id: ID!) {
  deleteApiConnector(id: $id)
}
    `;

export const useDeleteApiConnectorMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteApiConnectorMutation, TError, DeleteApiConnectorMutationVariables, TContext>) => {
    
    return useMutation<DeleteApiConnectorMutation, TError, DeleteApiConnectorMutationVariables, TContext>(
      {
    mutationKey: ['deleteApiConnector'],
    mutationFn: (variables?: DeleteApiConnectorMutationVariables) => fetcher<DeleteApiConnectorMutation, DeleteApiConnectorMutationVariables>(DeleteApiConnectorDocument, variables)(),
    ...options
  }
    )};

export const EnableApiConnectorDocument = `
    mutation enableApiConnector($id: ID!, $enable: Boolean!) {
  enableApiConnector(id: $id, enable: $enable)
}
    `;

export const useEnableApiConnectorMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<EnableApiConnectorMutation, TError, EnableApiConnectorMutationVariables, TContext>) => {
    
    return useMutation<EnableApiConnectorMutation, TError, EnableApiConnectorMutationVariables, TContext>(
      {
    mutationKey: ['enableApiConnector'],
    mutationFn: (variables?: EnableApiConnectorMutationVariables) => fetcher<EnableApiConnectorMutation, EnableApiConnectorMutationVariables>(EnableApiConnectorDocument, variables)(),
    ...options
  }
    )};

export const GenerateSpecificationDocument = `
    mutation generateSpecification($input: GenerateSpecificationInput!) {
  generateSpecification(input: $input) {
    specification
  }
}
    `;

export const useGenerateSpecificationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<GenerateSpecificationMutation, TError, GenerateSpecificationMutationVariables, TContext>) => {
    
    return useMutation<GenerateSpecificationMutation, TError, GenerateSpecificationMutationVariables, TContext>(
      {
    mutationKey: ['generateSpecification'],
    mutationFn: (variables?: GenerateSpecificationMutationVariables) => fetcher<GenerateSpecificationMutation, GenerateSpecificationMutationVariables>(GenerateSpecificationDocument, variables)(),
    ...options
  }
    )};

export const GenerationJobStatusDocument = `
    query generationJobStatus($jobId: String!) {
  generationJobStatus(jobId: $jobId) {
    jobId
    status
    specification
    errorMessage
  }
}
    `;

export const useGenerationJobStatusQuery = <
      TData = GenerationJobStatusQuery,
      TError = unknown
    >(
      variables: GenerationJobStatusQueryVariables,
      options?: Omit<UseQueryOptions<GenerationJobStatusQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GenerationJobStatusQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GenerationJobStatusQuery, TError, TData>(
      {
    queryKey: ['generationJobStatus', variables],
    queryFn: fetcher<GenerationJobStatusQuery, GenerationJobStatusQueryVariables>(GenerationJobStatusDocument, variables),
    ...options
  }
    )};

export const ImportOpenApiSpecificationDocument = `
    mutation importOpenApiSpecification($input: ImportOpenApiSpecificationInput!) {
  importOpenApiSpecification(input: $input) {
    id
    name
    title
    description
    icon
    connectorVersion
    enabled
    specification
    definition
    endpoints {
      id
      name
      description
      path
      httpMethod
    }
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `;

export const useImportOpenApiSpecificationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ImportOpenApiSpecificationMutation, TError, ImportOpenApiSpecificationMutationVariables, TContext>) => {
    
    return useMutation<ImportOpenApiSpecificationMutation, TError, ImportOpenApiSpecificationMutationVariables, TContext>(
      {
    mutationKey: ['importOpenApiSpecification'],
    mutationFn: (variables?: ImportOpenApiSpecificationMutationVariables) => fetcher<ImportOpenApiSpecificationMutation, ImportOpenApiSpecificationMutationVariables>(ImportOpenApiSpecificationDocument, variables)(),
    ...options
  }
    )};

export const StartGenerateFromDocumentationPreviewDocument = `
    mutation startGenerateFromDocumentationPreview($input: GenerateFromDocumentationInput!) {
  startGenerateFromDocumentationPreview(input: $input) {
    jobId
    status
    specification
    errorMessage
  }
}
    `;

export const useStartGenerateFromDocumentationPreviewMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<StartGenerateFromDocumentationPreviewMutation, TError, StartGenerateFromDocumentationPreviewMutationVariables, TContext>) => {
    
    return useMutation<StartGenerateFromDocumentationPreviewMutation, TError, StartGenerateFromDocumentationPreviewMutationVariables, TContext>(
      {
    mutationKey: ['startGenerateFromDocumentationPreview'],
    mutationFn: (variables?: StartGenerateFromDocumentationPreviewMutationVariables) => fetcher<StartGenerateFromDocumentationPreviewMutation, StartGenerateFromDocumentationPreviewMutationVariables>(StartGenerateFromDocumentationPreviewDocument, variables)(),
    ...options
  }
    )};

export const UpdateApiConnectorDocument = `
    mutation updateApiConnector($id: ID!, $input: UpdateApiConnectorInput!) {
  updateApiConnector(id: $id, input: $input) {
    id
    name
    title
    description
    icon
    connectorVersion
    enabled
    specification
    definition
    endpoints {
      id
      name
      description
      path
      httpMethod
    }
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `;

export const useUpdateApiConnectorMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateApiConnectorMutation, TError, UpdateApiConnectorMutationVariables, TContext>) => {
    
    return useMutation<UpdateApiConnectorMutation, TError, UpdateApiConnectorMutationVariables, TContext>(
      {
    mutationKey: ['updateApiConnector'],
    mutationFn: (variables?: UpdateApiConnectorMutationVariables) => fetcher<UpdateApiConnectorMutation, UpdateApiConnectorMutationVariables>(UpdateApiConnectorDocument, variables)(),
    ...options
  }
    )};

export const EditorJobFileLogsDocument = `
    query editorJobFileLogs($jobId: ID!, $filter: LogFilterInput, $page: Int, $size: Int) {
  editorJobFileLogs(jobId: $jobId, filter: $filter, page: $page, size: $size) {
    content {
      timestamp
      level
      componentName
      componentOperationName
      taskExecutionId
      message
      exceptionType
      exceptionMessage
      stackTrace
    }
    totalElements
    totalPages
    pageNumber
    pageSize
    hasNext
    hasPrevious
  }
}
    `;

export const useEditorJobFileLogsQuery = <
      TData = EditorJobFileLogsQuery,
      TError = unknown
    >(
      variables: EditorJobFileLogsQueryVariables,
      options?: Omit<UseQueryOptions<EditorJobFileLogsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<EditorJobFileLogsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<EditorJobFileLogsQuery, TError, TData>(
      {
    queryKey: ['editorJobFileLogs', variables],
    queryFn: fetcher<EditorJobFileLogsQuery, EditorJobFileLogsQueryVariables>(EditorJobFileLogsDocument, variables),
    ...options
  }
    )};

export const EditorJobFileLogsExistDocument = `
    query editorJobFileLogsExist($jobId: ID!) {
  editorJobFileLogsExist(jobId: $jobId)
}
    `;

export const useEditorJobFileLogsExistQuery = <
      TData = EditorJobFileLogsExistQuery,
      TError = unknown
    >(
      variables: EditorJobFileLogsExistQueryVariables,
      options?: Omit<UseQueryOptions<EditorJobFileLogsExistQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<EditorJobFileLogsExistQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<EditorJobFileLogsExistQuery, TError, TData>(
      {
    queryKey: ['editorJobFileLogsExist', variables],
    queryFn: fetcher<EditorJobFileLogsExistQuery, EditorJobFileLogsExistQueryVariables>(EditorJobFileLogsExistDocument, variables),
    ...options
  }
    )};

export const EditorTaskExecutionFileLogsDocument = `
    query editorTaskExecutionFileLogs($jobId: ID!, $taskExecutionId: ID!) {
  editorTaskExecutionFileLogs(jobId: $jobId, taskExecutionId: $taskExecutionId) {
    timestamp
    level
    componentName
    componentOperationName
    taskExecutionId
    message
    exceptionType
    exceptionMessage
    stackTrace
  }
}
    `;

export const useEditorTaskExecutionFileLogsQuery = <
      TData = EditorTaskExecutionFileLogsQuery,
      TError = unknown
    >(
      variables: EditorTaskExecutionFileLogsQueryVariables,
      options?: Omit<UseQueryOptions<EditorTaskExecutionFileLogsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<EditorTaskExecutionFileLogsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<EditorTaskExecutionFileLogsQuery, TError, TData>(
      {
    queryKey: ['editorTaskExecutionFileLogs', variables],
    queryFn: fetcher<EditorTaskExecutionFileLogsQuery, EditorTaskExecutionFileLogsQueryVariables>(EditorTaskExecutionFileLogsDocument, variables),
    ...options
  }
    )};

export const JobFileLogsDocument = `
    query jobFileLogs($jobId: ID!, $filter: LogFilterInput, $page: Int, $size: Int) {
  jobFileLogs(jobId: $jobId, filter: $filter, page: $page, size: $size) {
    content {
      timestamp
      level
      componentName
      componentOperationName
      taskExecutionId
      message
      exceptionType
      exceptionMessage
      stackTrace
    }
    totalElements
    totalPages
    pageNumber
    pageSize
    hasNext
    hasPrevious
  }
}
    `;

export const useJobFileLogsQuery = <
      TData = JobFileLogsQuery,
      TError = unknown
    >(
      variables: JobFileLogsQueryVariables,
      options?: Omit<UseQueryOptions<JobFileLogsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<JobFileLogsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<JobFileLogsQuery, TError, TData>(
      {
    queryKey: ['jobFileLogs', variables],
    queryFn: fetcher<JobFileLogsQuery, JobFileLogsQueryVariables>(JobFileLogsDocument, variables),
    ...options
  }
    )};

export const JobFileLogsExistDocument = `
    query jobFileLogsExist($jobId: ID!) {
  jobFileLogsExist(jobId: $jobId)
}
    `;

export const useJobFileLogsExistQuery = <
      TData = JobFileLogsExistQuery,
      TError = unknown
    >(
      variables: JobFileLogsExistQueryVariables,
      options?: Omit<UseQueryOptions<JobFileLogsExistQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<JobFileLogsExistQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<JobFileLogsExistQuery, TError, TData>(
      {
    queryKey: ['jobFileLogsExist', variables],
    queryFn: fetcher<JobFileLogsExistQuery, JobFileLogsExistQueryVariables>(JobFileLogsExistDocument, variables),
    ...options
  }
    )};

export const TaskExecutionFileLogsDocument = `
    query taskExecutionFileLogs($jobId: ID!, $taskExecutionId: ID!) {
  taskExecutionFileLogs(jobId: $jobId, taskExecutionId: $taskExecutionId) {
    timestamp
    level
    componentName
    componentOperationName
    taskExecutionId
    message
    exceptionType
    exceptionMessage
    stackTrace
  }
}
    `;

export const useTaskExecutionFileLogsQuery = <
      TData = TaskExecutionFileLogsQuery,
      TError = unknown
    >(
      variables: TaskExecutionFileLogsQueryVariables,
      options?: Omit<UseQueryOptions<TaskExecutionFileLogsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<TaskExecutionFileLogsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<TaskExecutionFileLogsQuery, TError, TData>(
      {
    queryKey: ['taskExecutionFileLogs', variables],
    queryFn: fetcher<TaskExecutionFileLogsQuery, TaskExecutionFileLogsQueryVariables>(TaskExecutionFileLogsDocument, variables),
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

export const ClusterElementComponentConnectionsDocument = `
    query clusterElementComponentConnections($workflowId: String!, $workflowNodeName: String!, $clusterElementType: String!, $clusterElementWorkflowNodeName: String!) {
  clusterElementComponentConnections(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    clusterElementType: $clusterElementType
    clusterElementWorkflowNodeName: $clusterElementWorkflowNodeName
  ) {
    componentName
    componentVersion
    key
    required
    workflowNodeName
  }
}
    `;

export const useClusterElementComponentConnectionsQuery = <
      TData = ClusterElementComponentConnectionsQuery,
      TError = unknown
    >(
      variables: ClusterElementComponentConnectionsQueryVariables,
      options?: Omit<UseQueryOptions<ClusterElementComponentConnectionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ClusterElementComponentConnectionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ClusterElementComponentConnectionsQuery, TError, TData>(
      {
    queryKey: ['clusterElementComponentConnections', variables],
    queryFn: fetcher<ClusterElementComponentConnectionsQuery, ClusterElementComponentConnectionsQueryVariables>(ClusterElementComponentConnectionsDocument, variables),
    ...options
  }
    )};

export const ClusterElementScriptInputDocument = `
    query clusterElementScriptInput($workflowId: String!, $workflowNodeName: String!, $clusterElementType: String!, $clusterElementWorkflowNodeName: String!, $environmentId: Long!) {
  clusterElementScriptInput(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    clusterElementType: $clusterElementType
    clusterElementWorkflowNodeName: $clusterElementWorkflowNodeName
    environmentId: $environmentId
  )
}
    `;

export const useClusterElementScriptInputQuery = <
      TData = ClusterElementScriptInputQuery,
      TError = unknown
    >(
      variables: ClusterElementScriptInputQueryVariables,
      options?: Omit<UseQueryOptions<ClusterElementScriptInputQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ClusterElementScriptInputQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ClusterElementScriptInputQuery, TError, TData>(
      {
    queryKey: ['clusterElementScriptInput', variables],
    queryFn: fetcher<ClusterElementScriptInputQuery, ClusterElementScriptInputQueryVariables>(ClusterElementScriptInputDocument, variables),
    ...options
  }
    )};

export const ComponentDefinitionSearchDocument = `
    query ComponentDefinitionSearch($query: String!) {
  componentDefinitionSearch(query: $query) {
    name
    title
    icon
    description
    version
    actionsCount
    triggersCount
    clusterElementsCount
    componentCategories {
      name
      label
    }
    actions {
      name
      title
      description
    }
    triggers {
      name
      title
      description
    }
    clusterElements {
      type {
        name
        label
      }
    }
  }
}
    `;

export const useComponentDefinitionSearchQuery = <
      TData = ComponentDefinitionSearchQuery,
      TError = unknown
    >(
      variables: ComponentDefinitionSearchQueryVariables,
      options?: Omit<UseQueryOptions<ComponentDefinitionSearchQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ComponentDefinitionSearchQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ComponentDefinitionSearchQuery, TError, TData>(
      {
    queryKey: ['ComponentDefinitionSearch', variables],
    queryFn: fetcher<ComponentDefinitionSearchQuery, ComponentDefinitionSearchQueryVariables>(ComponentDefinitionSearchDocument, variables),
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

export const SaveClusterElementTestConfigurationConnectionDocument = `
    mutation saveClusterElementTestConfigurationConnection($workflowId: String!, $workflowNodeName: String!, $clusterElementType: String!, $clusterElementWorkflowNodeName: String!, $workflowConnectionKey: String!, $connectionId: Long!, $environmentId: Long!) {
  saveClusterElementTestConfigurationConnection(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    clusterElementType: $clusterElementType
    clusterElementWorkflowNodeName: $clusterElementWorkflowNodeName
    workflowConnectionKey: $workflowConnectionKey
    connectionId: $connectionId
    environmentId: $environmentId
  )
}
    `;

export const useSaveClusterElementTestConfigurationConnectionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SaveClusterElementTestConfigurationConnectionMutation, TError, SaveClusterElementTestConfigurationConnectionMutationVariables, TContext>) => {
    
    return useMutation<SaveClusterElementTestConfigurationConnectionMutation, TError, SaveClusterElementTestConfigurationConnectionMutationVariables, TContext>(
      {
    mutationKey: ['saveClusterElementTestConfigurationConnection'],
    mutationFn: (variables?: SaveClusterElementTestConfigurationConnectionMutationVariables) => fetcher<SaveClusterElementTestConfigurationConnectionMutation, SaveClusterElementTestConfigurationConnectionMutationVariables>(SaveClusterElementTestConfigurationConnectionDocument, variables)(),
    ...options
  }
    )};

export const SaveWorkflowTestConfigurationConnectionDocument = `
    mutation saveWorkflowTestConfigurationConnection($workflowId: String!, $workflowNodeName: String!, $workflowConnectionKey: String!, $connectionId: Long!, $environmentId: Long!) {
  saveWorkflowTestConfigurationConnection(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    workflowConnectionKey: $workflowConnectionKey
    connectionId: $connectionId
    environmentId: $environmentId
  )
}
    `;

export const useSaveWorkflowTestConfigurationConnectionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SaveWorkflowTestConfigurationConnectionMutation, TError, SaveWorkflowTestConfigurationConnectionMutationVariables, TContext>) => {
    
    return useMutation<SaveWorkflowTestConfigurationConnectionMutation, TError, SaveWorkflowTestConfigurationConnectionMutationVariables, TContext>(
      {
    mutationKey: ['saveWorkflowTestConfigurationConnection'],
    mutationFn: (variables?: SaveWorkflowTestConfigurationConnectionMutationVariables) => fetcher<SaveWorkflowTestConfigurationConnectionMutation, SaveWorkflowTestConfigurationConnectionMutationVariables>(SaveWorkflowTestConfigurationConnectionDocument, variables)(),
    ...options
  }
    )};

export const TestClusterElementScriptDocument = `
    mutation testClusterElementScript($workflowId: String!, $workflowNodeName: String!, $clusterElementType: String!, $clusterElementWorkflowNodeName: String!, $environmentId: Long!, $inputParameters: Map) {
  testClusterElementScript(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    clusterElementType: $clusterElementType
    clusterElementWorkflowNodeName: $clusterElementWorkflowNodeName
    environmentId: $environmentId
    inputParameters: $inputParameters
  ) {
    error {
      message
      stackTrace
    }
    output
  }
}
    `;

export const useTestClusterElementScriptMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<TestClusterElementScriptMutation, TError, TestClusterElementScriptMutationVariables, TContext>) => {
    
    return useMutation<TestClusterElementScriptMutation, TError, TestClusterElementScriptMutationVariables, TContext>(
      {
    mutationKey: ['testClusterElementScript'],
    mutationFn: (variables?: TestClusterElementScriptMutationVariables) => fetcher<TestClusterElementScriptMutation, TestClusterElementScriptMutationVariables>(TestClusterElementScriptDocument, variables)(),
    ...options
  }
    )};

export const TestWorkflowNodeScriptDocument = `
    mutation testWorkflowNodeScript($workflowId: String!, $workflowNodeName: String!, $environmentId: Long!, $inputParameters: Map) {
  testWorkflowNodeScript(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    environmentId: $environmentId
    inputParameters: $inputParameters
  ) {
    error {
      message
      stackTrace
    }
    output
  }
}
    `;

export const useTestWorkflowNodeScriptMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<TestWorkflowNodeScriptMutation, TError, TestWorkflowNodeScriptMutationVariables, TContext>) => {
    
    return useMutation<TestWorkflowNodeScriptMutation, TError, TestWorkflowNodeScriptMutationVariables, TContext>(
      {
    mutationKey: ['testWorkflowNodeScript'],
    mutationFn: (variables?: TestWorkflowNodeScriptMutationVariables) => fetcher<TestWorkflowNodeScriptMutation, TestWorkflowNodeScriptMutationVariables>(TestWorkflowNodeScriptDocument, variables)(),
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

export const WorkflowNodeComponentConnectionsDocument = `
    query workflowNodeComponentConnections($workflowId: String!, $workflowNodeName: String!) {
  workflowNodeComponentConnections(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
  ) {
    componentName
    componentVersion
    key
    required
    workflowNodeName
  }
}
    `;

export const useWorkflowNodeComponentConnectionsQuery = <
      TData = WorkflowNodeComponentConnectionsQuery,
      TError = unknown
    >(
      variables: WorkflowNodeComponentConnectionsQueryVariables,
      options?: Omit<UseQueryOptions<WorkflowNodeComponentConnectionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkflowNodeComponentConnectionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkflowNodeComponentConnectionsQuery, TError, TData>(
      {
    queryKey: ['workflowNodeComponentConnections', variables],
    queryFn: fetcher<WorkflowNodeComponentConnectionsQuery, WorkflowNodeComponentConnectionsQueryVariables>(WorkflowNodeComponentConnectionsDocument, variables),
    ...options
  }
    )};

export const WorkflowNodeScriptInputDocument = `
    query workflowNodeScriptInput($workflowId: String!, $workflowNodeName: String!, $environmentId: Long!) {
  workflowNodeScriptInput(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    environmentId: $environmentId
  )
}
    `;

export const useWorkflowNodeScriptInputQuery = <
      TData = WorkflowNodeScriptInputQuery,
      TError = unknown
    >(
      variables: WorkflowNodeScriptInputQueryVariables,
      options?: Omit<UseQueryOptions<WorkflowNodeScriptInputQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkflowNodeScriptInputQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkflowNodeScriptInputQuery, TError, TData>(
      {
    queryKey: ['workflowNodeScriptInput', variables],
    queryFn: fetcher<WorkflowNodeScriptInputQuery, WorkflowNodeScriptInputQueryVariables>(WorkflowNodeScriptInputDocument, variables),
    ...options
  }
    )};

export const CustomComponentDocument = `
    query customComponent($id: ID!) {
  customComponent(id: $id) {
    id
    name
    title
    description
    icon
    componentVersion
    enabled
    language
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `;

export const useCustomComponentQuery = <
      TData = CustomComponentQuery,
      TError = unknown
    >(
      variables: CustomComponentQueryVariables,
      options?: Omit<UseQueryOptions<CustomComponentQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<CustomComponentQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<CustomComponentQuery, TError, TData>(
      {
    queryKey: ['customComponent', variables],
    queryFn: fetcher<CustomComponentQuery, CustomComponentQueryVariables>(CustomComponentDocument, variables),
    ...options
  }
    )};

export const CustomComponentDefinitionDocument = `
    query customComponentDefinition($id: ID!) {
  customComponentDefinition(id: $id) {
    actions {
      name
      title
      description
    }
    triggers {
      name
      title
      description
    }
  }
}
    `;

export const useCustomComponentDefinitionQuery = <
      TData = CustomComponentDefinitionQuery,
      TError = unknown
    >(
      variables: CustomComponentDefinitionQueryVariables,
      options?: Omit<UseQueryOptions<CustomComponentDefinitionQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<CustomComponentDefinitionQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<CustomComponentDefinitionQuery, TError, TData>(
      {
    queryKey: ['customComponentDefinition', variables],
    queryFn: fetcher<CustomComponentDefinitionQuery, CustomComponentDefinitionQueryVariables>(CustomComponentDefinitionDocument, variables),
    ...options
  }
    )};

export const CustomComponentsDocument = `
    query customComponents {
  customComponents {
    id
    name
    title
    description
    icon
    componentVersion
    enabled
    language
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `;

export const useCustomComponentsQuery = <
      TData = CustomComponentsQuery,
      TError = unknown
    >(
      variables?: CustomComponentsQueryVariables,
      options?: Omit<UseQueryOptions<CustomComponentsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<CustomComponentsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<CustomComponentsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['customComponents'] : ['customComponents', variables],
    queryFn: fetcher<CustomComponentsQuery, CustomComponentsQueryVariables>(CustomComponentsDocument, variables),
    ...options
  }
    )};

export const DeleteCustomComponentDocument = `
    mutation deleteCustomComponent($id: ID!) {
  deleteCustomComponent(id: $id)
}
    `;

export const useDeleteCustomComponentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteCustomComponentMutation, TError, DeleteCustomComponentMutationVariables, TContext>) => {
    
    return useMutation<DeleteCustomComponentMutation, TError, DeleteCustomComponentMutationVariables, TContext>(
      {
    mutationKey: ['deleteCustomComponent'],
    mutationFn: (variables?: DeleteCustomComponentMutationVariables) => fetcher<DeleteCustomComponentMutation, DeleteCustomComponentMutationVariables>(DeleteCustomComponentDocument, variables)(),
    ...options
  }
    )};

export const EnableCustomComponentDocument = `
    mutation enableCustomComponent($id: ID!, $enable: Boolean!) {
  enableCustomComponent(id: $id, enable: $enable)
}
    `;

export const useEnableCustomComponentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<EnableCustomComponentMutation, TError, EnableCustomComponentMutationVariables, TContext>) => {
    
    return useMutation<EnableCustomComponentMutation, TError, EnableCustomComponentMutationVariables, TContext>(
      {
    mutationKey: ['enableCustomComponent'],
    mutationFn: (variables?: EnableCustomComponentMutationVariables) => fetcher<EnableCustomComponentMutation, EnableCustomComponentMutationVariables>(EnableCustomComponentDocument, variables)(),
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

export const CreateIdentityProviderDocument = `
    mutation createIdentityProvider($input: IdentityProviderInput!) {
  createIdentityProvider(input: $input) {
    autoProvision
    clientId
    createdBy
    createdDate
    defaultAuthority
    domains
    enabled
    enforced
    id
    issuerUri
    lastModifiedBy
    lastModifiedDate
    metadataUri
    mfaMethod
    mfaRequired
    name
    nameIdFormat
    scopes
    signingCertificate
    type
  }
}
    `;

export const useCreateIdentityProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateIdentityProviderMutation, TError, CreateIdentityProviderMutationVariables, TContext>) => {
    
    return useMutation<CreateIdentityProviderMutation, TError, CreateIdentityProviderMutationVariables, TContext>(
      {
    mutationKey: ['createIdentityProvider'],
    mutationFn: (variables?: CreateIdentityProviderMutationVariables) => fetcher<CreateIdentityProviderMutation, CreateIdentityProviderMutationVariables>(CreateIdentityProviderDocument, variables)(),
    ...options
  }
    )};

export const DeleteIdentityProviderDocument = `
    mutation deleteIdentityProvider($id: ID!) {
  deleteIdentityProvider(id: $id)
}
    `;

export const useDeleteIdentityProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteIdentityProviderMutation, TError, DeleteIdentityProviderMutationVariables, TContext>) => {
    
    return useMutation<DeleteIdentityProviderMutation, TError, DeleteIdentityProviderMutationVariables, TContext>(
      {
    mutationKey: ['deleteIdentityProvider'],
    mutationFn: (variables?: DeleteIdentityProviderMutationVariables) => fetcher<DeleteIdentityProviderMutation, DeleteIdentityProviderMutationVariables>(DeleteIdentityProviderDocument, variables)(),
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

export const IdentityProviderDocument = `
    query identityProvider($id: ID!) {
  identityProvider(id: $id) {
    autoProvision
    clientId
    createdBy
    createdDate
    defaultAuthority
    domains
    enabled
    enforced
    id
    issuerUri
    lastModifiedBy
    lastModifiedDate
    metadataUri
    mfaMethod
    mfaRequired
    name
    nameIdFormat
    scopes
    signingCertificate
    type
  }
}
    `;

export const useIdentityProviderQuery = <
      TData = IdentityProviderQuery,
      TError = unknown
    >(
      variables: IdentityProviderQueryVariables,
      options?: Omit<UseQueryOptions<IdentityProviderQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<IdentityProviderQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<IdentityProviderQuery, TError, TData>(
      {
    queryKey: ['identityProvider', variables],
    queryFn: fetcher<IdentityProviderQuery, IdentityProviderQueryVariables>(IdentityProviderDocument, variables),
    ...options
  }
    )};

export const IdentityProvidersDocument = `
    query identityProviders {
  identityProviders {
    autoProvision
    clientId
    createdBy
    createdDate
    defaultAuthority
    domains
    enabled
    enforced
    id
    issuerUri
    lastModifiedBy
    lastModifiedDate
    metadataUri
    mfaMethod
    mfaRequired
    name
    nameIdFormat
    scopes
    signingCertificate
    type
  }
}
    `;

export const useIdentityProvidersQuery = <
      TData = IdentityProvidersQuery,
      TError = unknown
    >(
      variables?: IdentityProvidersQueryVariables,
      options?: Omit<UseQueryOptions<IdentityProvidersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<IdentityProvidersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<IdentityProvidersQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['identityProviders'] : ['identityProviders', variables],
    queryFn: fetcher<IdentityProvidersQuery, IdentityProvidersQueryVariables>(IdentityProvidersDocument, variables),
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

export const UpdateIdentityProviderDocument = `
    mutation updateIdentityProvider($id: ID!, $input: IdentityProviderInput!) {
  updateIdentityProvider(id: $id, input: $input) {
    autoProvision
    clientId
    createdBy
    createdDate
    defaultAuthority
    domains
    enabled
    enforced
    id
    issuerUri
    lastModifiedBy
    lastModifiedDate
    metadataUri
    mfaMethod
    mfaRequired
    name
    nameIdFormat
    scopes
    signingCertificate
    type
  }
}
    `;

export const useUpdateIdentityProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateIdentityProviderMutation, TError, UpdateIdentityProviderMutationVariables, TContext>) => {
    
    return useMutation<UpdateIdentityProviderMutation, TError, UpdateIdentityProviderMutationVariables, TContext>(
      {
    mutationKey: ['updateIdentityProvider'],
    mutationFn: (variables?: UpdateIdentityProviderMutationVariables) => fetcher<UpdateIdentityProviderMutation, UpdateIdentityProviderMutationVariables>(UpdateIdentityProviderDocument, variables)(),
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
