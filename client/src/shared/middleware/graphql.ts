import { useQuery, useMutation, UseQueryOptions, UseMutationOptions } from '@tanstack/react-query';
import { fetcher } from './graphqlFetcher';
export class TypedDocumentString<TResult, TVariables> extends String {
  __apiType?: { result: TResult; variables: TVariables };

  constructor(private value: string) {
    super(value);
  }

  override toString(): string {
    return this.value;
  }
}
export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
export type MakeEmpty<T extends { [key: string]: unknown }, K extends keyof T> = { [_ in K]?: never };
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: { input: string; output: string; }
  String: { input: string; output: string; }
  Boolean: { input: boolean; output: boolean; }
  Int: { input: number; output: number; }
  Float: { input: number; output: number; }
  Any: { input: any; output: any; }
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
  resumePerformFunctionDefined?: Maybe<Scalars['Boolean']['output']>;
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

export type AgentEvalResult = {
  __typename?: 'AgentEvalResult';
  createdDate?: Maybe<Scalars['Long']['output']>;
  errorMessage?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  inputTokens?: Maybe<Scalars['Int']['output']>;
  outputTokens?: Maybe<Scalars['Int']['output']>;
  runIndex?: Maybe<Scalars['Int']['output']>;
  scenario: AgentEvalScenario;
  score?: Maybe<Scalars['Float']['output']>;
  status: AgentEvalResultStatus;
  transcriptFile?: Maybe<Scalars['String']['output']>;
  verdicts: Array<AgentJudgeVerdict>;
};

export enum AgentEvalResultStatus {
  Completed = 'COMPLETED',
  Failed = 'FAILED',
  Pending = 'PENDING',
  Running = 'RUNNING'
}

export type AgentEvalRun = {
  __typename?: 'AgentEvalRun';
  agentVersion?: Maybe<Scalars['String']['output']>;
  averageScore?: Maybe<Scalars['Float']['output']>;
  completedDate?: Maybe<Scalars['Long']['output']>;
  completedScenarios: Scalars['Int']['output'];
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  results: Array<AgentEvalResult>;
  startedDate?: Maybe<Scalars['Long']['output']>;
  status: AgentEvalRunStatus;
  totalInputTokens?: Maybe<Scalars['Int']['output']>;
  totalOutputTokens?: Maybe<Scalars['Int']['output']>;
  totalScenarios: Scalars['Int']['output'];
};

export enum AgentEvalRunStatus {
  Completed = 'COMPLETED',
  Failed = 'FAILED',
  Pending = 'PENDING',
  Running = 'RUNNING'
}

export type AgentEvalScenario = {
  __typename?: 'AgentEvalScenario';
  createdDate?: Maybe<Scalars['Long']['output']>;
  expectedOutput?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  judges: Array<AgentScenarioJudge>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  maxTurns?: Maybe<Scalars['Int']['output']>;
  name: Scalars['String']['output'];
  numberOfRuns?: Maybe<Scalars['Int']['output']>;
  personaPrompt?: Maybe<Scalars['String']['output']>;
  toolSimulations: Array<AgentScenarioToolSimulation>;
  type: AgentScenarioType;
  userMessage?: Maybe<Scalars['String']['output']>;
};

export type AgentEvalTest = {
  __typename?: 'AgentEvalTest';
  createdDate?: Maybe<Scalars['Long']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  scenarios: Array<AgentEvalScenario>;
};

export type AgentJudge = {
  __typename?: 'AgentJudge';
  configuration: Scalars['Map']['output'];
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  type: AgentJudgeType;
};

export enum AgentJudgeScope {
  Agent = 'AGENT',
  Scenario = 'SCENARIO'
}

export enum AgentJudgeType {
  ContainsText = 'CONTAINS_TEXT',
  JsonSchema = 'JSON_SCHEMA',
  LlmRule = 'LLM_RULE',
  RegexMatch = 'REGEX_MATCH',
  ResponseLength = 'RESPONSE_LENGTH',
  Similarity = 'SIMILARITY',
  StringEquals = 'STRING_EQUALS',
  ToolUsage = 'TOOL_USAGE'
}

export type AgentJudgeVerdict = {
  __typename?: 'AgentJudgeVerdict';
  explanation: Scalars['String']['output'];
  id: Scalars['ID']['output'];
  judgeName: Scalars['String']['output'];
  judgeScope: AgentJudgeScope;
  judgeType: AgentJudgeType;
  passed: Scalars['Boolean']['output'];
  score: Scalars['Float']['output'];
};

export type AgentScenarioJudge = {
  __typename?: 'AgentScenarioJudge';
  configuration: Scalars['Map']['output'];
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  type: AgentJudgeType;
};

export type AgentScenarioToolSimulation = {
  __typename?: 'AgentScenarioToolSimulation';
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  responsePrompt: Scalars['String']['output'];
  simulationModel?: Maybe<Scalars['String']['output']>;
  toolName: Scalars['String']['output'];
};

export enum AgentScenarioType {
  MultiTurn = 'MULTI_TURN',
  SingleTurn = 'SINGLE_TURN'
}

export type AgentSkill = {
  __typename?: 'AgentSkill';
  /** Epoch milliseconds (UTC) */
  createdDate?: Maybe<Scalars['Long']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  /** Epoch milliseconds (UTC) */
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
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

export type CreateEmbeddedMcpServerInput = {
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  environmentId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
};

export type CreateMcpIntegrationInstanceConfigurationInput = {
  integrationInstanceConfigurationId: Scalars['ID']['input'];
  mcpServerId: Scalars['ID']['input'];
  selectedWorkflowIds: Array<Scalars['String']['input']>;
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
  componentName: Scalars['String']['output'];
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
};

export type IntegrationInstanceConfigurationWorkflow = {
  __typename?: 'IntegrationInstanceConfigurationWorkflow';
  connections: Array<IntegrationInstanceConfigurationWorkflowConnection>;
  enabled: Scalars['Boolean']['output'];
  id: Scalars['ID']['output'];
  inputs?: Maybe<Scalars['Map']['output']>;
  integrationInstanceConfigurationId: Scalars['ID']['output'];
  version: Scalars['Int']['output'];
  workflowId: Scalars['String']['output'];
};

export type IntegrationInstanceConfigurationWorkflowConnection = {
  __typename?: 'IntegrationInstanceConfigurationWorkflowConnection';
  connectionId?: Maybe<Scalars['ID']['output']>;
  workflowConnectionKey: Scalars['String']['output'];
  workflowNodeName: Scalars['String']['output'];
};

export type IntegrationWorkflow = {
  __typename?: 'IntegrationWorkflow';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  integrationWorkflowId: Scalars['ID']['output'];
  label: Scalars['String']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  workflowTaskComponentNames: Array<Scalars['String']['output']>;
  workflowTriggerComponentNames: Array<Scalars['String']['output']>;
  workflowUuid?: Maybe<Scalars['String']['output']>;
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
  title?: Maybe<Scalars['String']['output']>;
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

export type McpIntegrationInstanceConfiguration = {
  __typename?: 'McpIntegrationInstanceConfiguration';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  integration?: Maybe<Integration>;
  integrationInstanceConfigurationId: Scalars['ID']['output'];
  integrationInstanceConfigurationName?: Maybe<Scalars['String']['output']>;
  integrationVersion?: Maybe<Scalars['Int']['output']>;
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  mcpIntegrationInstanceConfigurationWorkflows?: Maybe<Array<Maybe<McpIntegrationInstanceConfigurationWorkflow>>>;
  mcpServerId: Scalars['ID']['output'];
  version?: Maybe<Scalars['Int']['output']>;
};

export type McpIntegrationInstanceConfigurationWorkflow = {
  __typename?: 'McpIntegrationInstanceConfigurationWorkflow';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  integrationInstanceConfigurationWorkflow?: Maybe<IntegrationInstanceConfigurationWorkflow>;
  integrationInstanceConfigurationWorkflowId: Scalars['Long']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  mcpIntegrationInstanceConfigurationId: Scalars['Long']['output'];
  parameters?: Maybe<Scalars['Map']['output']>;
  version?: Maybe<Scalars['Int']['output']>;
  workflow?: Maybe<Workflow>;
};

export type McpIntegrationInstanceConfigurationWorkflowInput = {
  integrationInstanceConfigurationWorkflowId: Scalars['Long']['input'];
  mcpIntegrationInstanceConfigurationId: Scalars['Long']['input'];
};

export type McpIntegrationInstanceConfigurationWorkflowUpdateInput = {
  integrationInstanceConfigurationWorkflowId?: InputMaybe<Scalars['Long']['input']>;
  mcpIntegrationInstanceConfigurationId?: InputMaybe<Scalars['Long']['input']>;
  parameters?: InputMaybe<Scalars['Map']['input']>;
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
  parameters?: Maybe<Scalars['Map']['output']>;
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
  parameters?: InputMaybe<Scalars['Map']['input']>;
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
  title?: Maybe<Scalars['String']['output']>;
  version?: Maybe<Scalars['Int']['output']>;
};

export type McpToolInput = {
  mcpComponentId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  parameters?: InputMaybe<Scalars['Map']['input']>;
  version?: InputMaybe<Scalars['Int']['input']>;
};

export type McpToolInputForComponent = {
  name: Scalars['String']['input'];
  parameters?: InputMaybe<Scalars['Map']['input']>;
};

export type Mutation = {
  __typename?: 'Mutation';
  _placeholder?: Maybe<Scalars['Boolean']['output']>;
  addDataTableColumn: Scalars['Boolean']['output'];
  cancelAgentEvalRun: AgentEvalRun;
  cancelGenerationJob: Scalars['Boolean']['output'];
  createAgentEvalScenario: AgentEvalScenario;
  createAgentEvalTest: AgentEvalTest;
  createAgentJudge: AgentJudge;
  createAgentScenarioJudge: AgentScenarioJudge;
  createAgentScenarioToolSimulation: AgentScenarioToolSimulation;
  createAgentSkill: AgentSkill;
  createAgentSkillFromInstructions: AgentSkill;
  createApiConnector: ApiConnector;
  createApiKey: Scalars['String']['output'];
  createApprovalTask?: Maybe<ApprovalTask>;
  createDataTable: Scalars['Boolean']['output'];
  createEmbeddedMcpServer?: Maybe<McpServer>;
  createIdentityProvider: IdentityProviderType;
  createKnowledgeBase?: Maybe<KnowledgeBase>;
  createMcpComponent?: Maybe<McpComponent>;
  createMcpComponentWithTools?: Maybe<McpComponent>;
  createMcpIntegrationInstanceConfiguration?: Maybe<McpIntegrationInstanceConfiguration>;
  createMcpIntegrationInstanceConfigurationWorkflow?: Maybe<McpIntegrationInstanceConfigurationWorkflow>;
  createMcpProject?: Maybe<McpProject>;
  createMcpProjectWorkflow?: Maybe<McpProjectWorkflow>;
  createMcpServer?: Maybe<McpServer>;
  createMcpTool?: Maybe<McpTool>;
  createWorkspaceApiKey: Scalars['String']['output'];
  createWorkspaceMcpServer?: Maybe<McpServer>;
  deleteAgentEvalScenario: Scalars['Boolean']['output'];
  deleteAgentEvalTest: Scalars['Boolean']['output'];
  deleteAgentJudge: Scalars['Boolean']['output'];
  deleteAgentScenarioJudge: Scalars['Boolean']['output'];
  deleteAgentScenarioToolSimulation: Scalars['Boolean']['output'];
  deleteAgentSkill: Scalars['Boolean']['output'];
  deleteApiConnector: Scalars['Boolean']['output'];
  deleteApiKey: Scalars['Boolean']['output'];
  deleteApprovalTask?: Maybe<Scalars['Boolean']['output']>;
  deleteCustomComponent: Scalars['Boolean']['output'];
  deleteDataTableRow: Scalars['Boolean']['output'];
  deleteEmbeddedMcpServer?: Maybe<Scalars['Boolean']['output']>;
  deleteIdentityProvider: Scalars['Boolean']['output'];
  deleteJobFileLogs: Scalars['Boolean']['output'];
  deleteKnowledgeBase?: Maybe<Scalars['Boolean']['output']>;
  deleteKnowledgeBaseDocument?: Maybe<Scalars['Boolean']['output']>;
  deleteKnowledgeBaseDocumentChunk?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpComponent?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpIntegrationInstanceConfiguration?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpIntegrationInstanceConfigurationWorkflow?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpProject?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpProjectWorkflow?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpServer?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpTool?: Maybe<Scalars['Boolean']['output']>;
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
  saveClusterElementTestOutput?: Maybe<WorkflowNodeTestOutputResult>;
  saveWorkflowTestConfigurationConnection?: Maybe<Scalars['Boolean']['output']>;
  startAgentEvalRun: AgentEvalRun;
  startDiscoverEndpoints: EndpointDiscoveryResult;
  startGenerateForEndpoints: GenerationJobStatus;
  startGenerateFromDocumentationPreview: GenerationJobStatus;
  testClusterElementScript: ScriptTestExecution;
  testWorkflowNodeScript: ScriptTestExecution;
  updateAgentEvalScenario: AgentEvalScenario;
  updateAgentEvalTest: AgentEvalTest;
  updateAgentJudge: AgentJudge;
  updateAgentScenarioJudge: AgentScenarioJudge;
  updateAgentScenarioToolSimulation: AgentScenarioToolSimulation;
  updateAgentSkill: AgentSkill;
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
  updateMcpIntegrationInstanceConfiguration?: Maybe<McpIntegrationInstanceConfiguration>;
  updateMcpIntegrationInstanceConfigurationVersion?: Maybe<Scalars['Boolean']['output']>;
  updateMcpIntegrationInstanceConfigurationWorkflow?: Maybe<McpIntegrationInstanceConfigurationWorkflow>;
  updateMcpProject?: Maybe<McpProject>;
  updateMcpProjectWorkflow?: Maybe<McpProjectWorkflow>;
  updateMcpServer?: Maybe<McpServer>;
  updateMcpServerTags?: Maybe<Array<Maybe<Tag>>>;
  updateMcpServerUrl: Scalars['String']['output'];
  updateMcpTool?: Maybe<McpTool>;
  updateUser: AdminUser;
  updateWorkspaceApiKey: Scalars['Boolean']['output'];
};


export type MutationAddDataTableColumnArgs = {
  input: AddColumnInput;
};


export type MutationCancelAgentEvalRunArgs = {
  id: Scalars['ID']['input'];
};


export type MutationCancelGenerationJobArgs = {
  jobId: Scalars['String']['input'];
};


export type MutationCreateAgentEvalScenarioArgs = {
  agentEvalTestId: Scalars['ID']['input'];
  expectedOutput?: InputMaybe<Scalars['String']['input']>;
  maxTurns?: InputMaybe<Scalars['Int']['input']>;
  name: Scalars['String']['input'];
  numberOfRuns?: InputMaybe<Scalars['Int']['input']>;
  personaPrompt?: InputMaybe<Scalars['String']['input']>;
  type: AgentScenarioType;
  userMessage?: InputMaybe<Scalars['String']['input']>;
};


export type MutationCreateAgentEvalTestArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
};


export type MutationCreateAgentJudgeArgs = {
  configuration: Scalars['Map']['input'];
  name: Scalars['String']['input'];
  type: AgentJudgeType;
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
};


export type MutationCreateAgentScenarioJudgeArgs = {
  agentEvalScenarioId: Scalars['ID']['input'];
  configuration: Scalars['Map']['input'];
  name: Scalars['String']['input'];
  type: AgentJudgeType;
};


export type MutationCreateAgentScenarioToolSimulationArgs = {
  agentEvalScenarioId: Scalars['ID']['input'];
  responsePrompt: Scalars['String']['input'];
  simulationModel?: InputMaybe<Scalars['String']['input']>;
  toolName: Scalars['String']['input'];
};


export type MutationCreateAgentSkillArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  fileBytes: Scalars['String']['input'];
  filename: Scalars['String']['input'];
  name: Scalars['String']['input'];
};


export type MutationCreateAgentSkillFromInstructionsArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  instructions: Scalars['String']['input'];
  name: Scalars['String']['input'];
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


export type MutationCreateEmbeddedMcpServerArgs = {
  input: CreateEmbeddedMcpServerInput;
};


export type MutationCreateIdentityProviderArgs = {
  input: IdentityProviderInput;
};


export type MutationCreateKnowledgeBaseArgs = {
  environmentId: Scalars['ID']['input'];
  knowledgeBase: KnowledgeBaseInput;
  workspaceId: Scalars['ID']['input'];
};


export type MutationCreateMcpComponentArgs = {
  input: McpComponentInput;
};


export type MutationCreateMcpComponentWithToolsArgs = {
  input: McpComponentWithToolsInput;
};


export type MutationCreateMcpIntegrationInstanceConfigurationArgs = {
  input: CreateMcpIntegrationInstanceConfigurationInput;
};


export type MutationCreateMcpIntegrationInstanceConfigurationWorkflowArgs = {
  input: McpIntegrationInstanceConfigurationWorkflowInput;
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


export type MutationDeleteAgentEvalScenarioArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAgentEvalTestArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAgentJudgeArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAgentScenarioJudgeArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAgentScenarioToolSimulationArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAgentSkillArgs = {
  id: Scalars['ID']['input'];
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


export type MutationDeleteEmbeddedMcpServerArgs = {
  mcpServerId: Scalars['ID']['input'];
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


export type MutationDeleteMcpIntegrationInstanceConfigurationArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteMcpIntegrationInstanceConfigurationWorkflowArgs = {
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


export type MutationDeleteMcpToolArgs = {
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


export type MutationSaveClusterElementTestOutputArgs = {
  clusterElementType: Scalars['String']['input'];
  clusterElementWorkflowNodeName: Scalars['String']['input'];
  environmentId: Scalars['Long']['input'];
  inputParameters?: InputMaybe<Scalars['Map']['input']>;
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


export type MutationStartAgentEvalRunArgs = {
  agentEvalTestId: Scalars['ID']['input'];
  agentJudgeIds?: InputMaybe<Array<Scalars['ID']['input']>>;
  environmentId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  scenarioIds?: InputMaybe<Array<Scalars['ID']['input']>>;
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


export type MutationUpdateAgentEvalScenarioArgs = {
  expectedOutput?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  maxTurns?: InputMaybe<Scalars['Int']['input']>;
  name?: InputMaybe<Scalars['String']['input']>;
  numberOfRuns?: InputMaybe<Scalars['Int']['input']>;
  personaPrompt?: InputMaybe<Scalars['String']['input']>;
  userMessage?: InputMaybe<Scalars['String']['input']>;
};


export type MutationUpdateAgentEvalTestArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
};


export type MutationUpdateAgentJudgeArgs = {
  configuration?: InputMaybe<Scalars['Map']['input']>;
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
};


export type MutationUpdateAgentScenarioJudgeArgs = {
  configuration?: InputMaybe<Scalars['Map']['input']>;
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
};


export type MutationUpdateAgentScenarioToolSimulationArgs = {
  id: Scalars['ID']['input'];
  responsePrompt?: InputMaybe<Scalars['String']['input']>;
  simulationModel?: InputMaybe<Scalars['String']['input']>;
  toolName?: InputMaybe<Scalars['String']['input']>;
};


export type MutationUpdateAgentSkillArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  name: Scalars['String']['input'];
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


export type MutationUpdateMcpIntegrationInstanceConfigurationArgs = {
  id: Scalars['ID']['input'];
  input: UpdateMcpIntegrationInstanceConfigurationInput;
};


export type MutationUpdateMcpIntegrationInstanceConfigurationVersionArgs = {
  id: Scalars['ID']['input'];
  input: UpdateMcpIntegrationInstanceConfigurationVersionInput;
};


export type MutationUpdateMcpIntegrationInstanceConfigurationWorkflowArgs = {
  id: Scalars['ID']['input'];
  input: McpIntegrationInstanceConfigurationWorkflowUpdateInput;
};


export type MutationUpdateMcpProjectArgs = {
  id: Scalars['ID']['input'];
  input: UpdateMcpProjectInput;
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


export type MutationUpdateMcpToolArgs = {
  id: Scalars['ID']['input'];
  input: McpToolInput;
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
  value?: Maybe<Scalars['Any']['output']>;
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
  workflowConnectionKey: Scalars['String']['output'];
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
  agentEvalResult?: Maybe<AgentEvalResult>;
  agentEvalResultTranscript?: Maybe<Scalars['String']['output']>;
  agentEvalRun?: Maybe<AgentEvalRun>;
  agentEvalRuns: Array<AgentEvalRun>;
  agentEvalTest?: Maybe<AgentEvalTest>;
  agentEvalTests: Array<AgentEvalTest>;
  agentJudges: Array<AgentJudge>;
  agentSkill: AgentSkill;
  agentSkillFileContent: Scalars['String']['output'];
  agentSkillFilePaths: Array<Scalars['String']['output']>;
  agentSkills: Array<AgentSkill>;
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
  clusterElementDynamicProperties: Array<Property>;
  clusterElementMissingRequiredProperties: Array<Scalars['String']['output']>;
  clusterElementOptions: Array<Option>;
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
  embeddedMcpServers?: Maybe<Array<Maybe<McpServer>>>;
  endpointDiscoveryStatus?: Maybe<EndpointDiscoveryResult>;
  environments?: Maybe<Array<Maybe<Environment>>>;
  exportDataTableCsv: Scalars['String']['output'];
  generationJobStatus?: Maybe<GenerationJobStatus>;
  identityProvider?: Maybe<IdentityProviderType>;
  identityProviders: Array<Maybe<IdentityProviderType>>;
  integration?: Maybe<Integration>;
  integrationWorkflows: Array<IntegrationWorkflow>;
  integrationWorkflowsByIntegrationId: Array<IntegrationWorkflow>;
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
  mcpComponentDefinitions: Array<ComponentDefinition>;
  mcpComponents?: Maybe<Array<Maybe<McpComponent>>>;
  mcpComponentsByServerId?: Maybe<Array<Maybe<McpComponent>>>;
  mcpIntegrationInstanceConfiguration?: Maybe<McpIntegrationInstanceConfiguration>;
  mcpIntegrationInstanceConfigurationWorkflow?: Maybe<McpIntegrationInstanceConfigurationWorkflow>;
  mcpIntegrationInstanceConfigurationWorkflowProperties?: Maybe<Array<Maybe<Property>>>;
  mcpIntegrationInstanceConfigurationWorkflows?: Maybe<Array<Maybe<McpIntegrationInstanceConfigurationWorkflow>>>;
  mcpIntegrationInstanceConfigurationWorkflowsByMcpIntegrationInstanceConfigurationId?: Maybe<Array<Maybe<McpIntegrationInstanceConfigurationWorkflow>>>;
  mcpIntegrationInstanceConfigurations?: Maybe<Array<Maybe<McpIntegrationInstanceConfiguration>>>;
  mcpIntegrationInstanceConfigurationsByServerId?: Maybe<Array<Maybe<McpIntegrationInstanceConfiguration>>>;
  mcpProject?: Maybe<McpProject>;
  mcpProjectWorkflow?: Maybe<McpProjectWorkflow>;
  mcpProjectWorkflowProperties?: Maybe<Array<Maybe<Property>>>;
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
  toolEligibleIntegrationInstanceConfigurationWorkflows: Array<IntegrationWorkflow>;
  toolEligibleIntegrationVersionWorkflows: Array<IntegrationWorkflow>;
  toolEligibleProjectVersionWorkflows: Array<ProjectWorkflow>;
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


export type QueryAgentEvalResultArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAgentEvalResultTranscriptArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAgentEvalRunArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAgentEvalRunsArgs = {
  agentEvalTestId: Scalars['ID']['input'];
  limit?: InputMaybe<Scalars['Int']['input']>;
  offset?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryAgentEvalTestArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAgentEvalTestsArgs = {
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
};


export type QueryAgentJudgesArgs = {
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
};


export type QueryAgentSkillArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAgentSkillFileContentArgs = {
  id: Scalars['ID']['input'];
  path: Scalars['String']['input'];
};


export type QueryAgentSkillFilePathsArgs = {
  id: Scalars['ID']['input'];
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
  clusterElementType?: InputMaybe<Scalars['String']['input']>;
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
};


export type QueryClusterElementDefinitionsArgs = {
  clusterElementType: Scalars['String']['input'];
  rootComponentName: Scalars['String']['input'];
  rootComponentVersion: Scalars['Int']['input'];
};


export type QueryClusterElementDynamicPropertiesArgs = {
  clusterElementName: Scalars['String']['input'];
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
  connectionId?: InputMaybe<Scalars['Long']['input']>;
  inputParameters?: InputMaybe<Scalars['Map']['input']>;
  lookupDependsOnPaths?: InputMaybe<Array<Scalars['String']['input']>>;
  propertyName: Scalars['String']['input'];
};


export type QueryClusterElementMissingRequiredPropertiesArgs = {
  clusterElementType: Scalars['String']['input'];
  clusterElementWorkflowNodeName: Scalars['String']['input'];
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
};


export type QueryClusterElementOptionsArgs = {
  clusterElementName: Scalars['String']['input'];
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
  connectionId?: InputMaybe<Scalars['Long']['input']>;
  inputParameters?: InputMaybe<Scalars['Map']['input']>;
  lookupDependsOnPaths?: InputMaybe<Array<Scalars['String']['input']>>;
  propertyName: Scalars['String']['input'];
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
  clusterElementDefinitions?: InputMaybe<Scalars['Boolean']['input']>;
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


export type QueryIntegrationWorkflowsByIntegrationIdArgs = {
  integrationId: Scalars['ID']['input'];
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
  environmentId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryMcpComponentArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpComponentsByServerIdArgs = {
  mcpServerId?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpIntegrationInstanceConfigurationArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpIntegrationInstanceConfigurationWorkflowArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpIntegrationInstanceConfigurationWorkflowPropertiesArgs = {
  mcpIntegrationInstanceConfigurationWorkflowId: Scalars['ID']['input'];
};


export type QueryMcpIntegrationInstanceConfigurationWorkflowsByMcpIntegrationInstanceConfigurationIdArgs = {
  mcpIntegrationInstanceConfigurationId?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpIntegrationInstanceConfigurationsByServerIdArgs = {
  mcpServerId?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpProjectArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpProjectWorkflowArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpProjectWorkflowPropertiesArgs = {
  mcpProjectWorkflowId: Scalars['ID']['input'];
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


export type QueryToolEligibleIntegrationInstanceConfigurationWorkflowsArgs = {
  integrationInstanceConfigurationId: Scalars['ID']['input'];
};


export type QueryToolEligibleIntegrationVersionWorkflowsArgs = {
  integrationId: Scalars['ID']['input'];
  integrationVersion: Scalars['Int']['input'];
};


export type QueryToolEligibleProjectVersionWorkflowsArgs = {
  projectId: Scalars['ID']['input'];
  projectVersion: Scalars['Int']['input'];
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
  Callable = 'CALLABLE',
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

export type UpdateMcpIntegrationInstanceConfigurationInput = {
  selectedWorkflowIds: Array<Scalars['String']['input']>;
};

export type UpdateMcpIntegrationInstanceConfigurationVersionInput = {
  integrationVersion: Scalars['Int']['input'];
  workflowUuids: Array<Scalars['String']['input']>;
};

export type UpdateMcpProjectInput = {
  selectedWorkflowIds: Array<Scalars['String']['input']>;
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

export type WorkflowNodeTestOutputResult = {
  __typename?: 'WorkflowNodeTestOutputResult';
  id: Scalars['Long']['output'];
  workflowId: Scalars['String']['output'];
  workflowNodeName: Scalars['String']['output'];
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

export type AgentEvalResultQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AgentEvalResultQuery = { __typename?: 'Query', agentEvalResult?: { __typename?: 'AgentEvalResult', id: string, status: AgentEvalResultStatus, score?: number | null, errorMessage?: string | null, transcriptFile?: string | null, createdDate?: any | null, scenario: { __typename?: 'AgentEvalScenario', id: string, name: string, type: AgentScenarioType, userMessage?: string | null, expectedOutput?: string | null, personaPrompt?: string | null, maxTurns?: number | null, createdDate?: any | null, lastModifiedDate?: any | null, judges: Array<{ __typename?: 'AgentScenarioJudge', id: string, name: string, type: AgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null }> }, verdicts: Array<{ __typename?: 'AgentJudgeVerdict', id: string, judgeName: string, judgeType: AgentJudgeType, judgeScope: AgentJudgeScope, passed: boolean, score: number, explanation: string }> } | null };

export type AgentEvalResultTranscriptQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AgentEvalResultTranscriptQuery = { __typename?: 'Query', agentEvalResultTranscript?: string | null };

export type AgentEvalRunQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AgentEvalRunQuery = { __typename?: 'Query', agentEvalRun?: { __typename?: 'AgentEvalRun', id: string, name: string, status: AgentEvalRunStatus, averageScore?: number | null, totalScenarios: number, completedScenarios: number, agentVersion?: string | null, totalInputTokens?: number | null, totalOutputTokens?: number | null, startedDate?: any | null, completedDate?: any | null, createdDate?: any | null, results: Array<{ __typename?: 'AgentEvalResult', id: string, status: AgentEvalResultStatus, score?: number | null, errorMessage?: string | null, transcriptFile?: string | null, inputTokens?: number | null, outputTokens?: number | null, runIndex?: number | null, createdDate?: any | null, scenario: { __typename?: 'AgentEvalScenario', id: string, name: string, type: AgentScenarioType, userMessage?: string | null, expectedOutput?: string | null, personaPrompt?: string | null, maxTurns?: number | null, createdDate?: any | null, lastModifiedDate?: any | null, judges: Array<{ __typename?: 'AgentScenarioJudge', id: string, name: string, type: AgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null }> }, verdicts: Array<{ __typename?: 'AgentJudgeVerdict', id: string, judgeName: string, judgeType: AgentJudgeType, judgeScope: AgentJudgeScope, passed: boolean, score: number, explanation: string }> }> } | null };

export type AgentEvalRunsQueryVariables = Exact<{
  agentEvalTestId: Scalars['ID']['input'];
  limit?: InputMaybe<Scalars['Int']['input']>;
  offset?: InputMaybe<Scalars['Int']['input']>;
}>;


export type AgentEvalRunsQuery = { __typename?: 'Query', agentEvalRuns: Array<{ __typename?: 'AgentEvalRun', id: string, name: string, status: AgentEvalRunStatus, averageScore?: number | null, totalScenarios: number, completedScenarios: number, startedDate?: any | null, completedDate?: any | null, createdDate?: any | null }> };

export type AgentEvalTestQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AgentEvalTestQuery = { __typename?: 'Query', agentEvalTest?: { __typename?: 'AgentEvalTest', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null, scenarios: Array<{ __typename?: 'AgentEvalScenario', id: string, name: string, type: AgentScenarioType, userMessage?: string | null, expectedOutput?: string | null, personaPrompt?: string | null, maxTurns?: number | null, numberOfRuns?: number | null, createdDate?: any | null, lastModifiedDate?: any | null, judges: Array<{ __typename?: 'AgentScenarioJudge', id: string, name: string, type: AgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null }>, toolSimulations: Array<{ __typename?: 'AgentScenarioToolSimulation', id: string, responsePrompt: string, simulationModel?: string | null, toolName: string, createdDate?: any | null, lastModifiedDate?: any | null }> }> } | null };

export type AgentEvalTestsQueryVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
}>;


export type AgentEvalTestsQuery = { __typename?: 'Query', agentEvalTests: Array<{ __typename?: 'AgentEvalTest', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null, scenarios: Array<{ __typename?: 'AgentEvalScenario', id: string, name: string, type: AgentScenarioType, userMessage?: string | null, expectedOutput?: string | null, personaPrompt?: string | null, maxTurns?: number | null, numberOfRuns?: number | null, createdDate?: any | null, lastModifiedDate?: any | null, toolSimulations: Array<{ __typename?: 'AgentScenarioToolSimulation', id: string, toolName: string, responsePrompt: string, simulationModel?: string | null }>, judges: Array<{ __typename?: 'AgentScenarioJudge', id: string, name: string, type: AgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null }> }> }> };

export type AgentJudgesQueryVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
}>;


export type AgentJudgesQuery = { __typename?: 'Query', agentJudges: Array<{ __typename?: 'AgentJudge', id: string, name: string, type: AgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null }> };

export type CancelAgentEvalRunMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type CancelAgentEvalRunMutation = { __typename?: 'Mutation', cancelAgentEvalRun: { __typename?: 'AgentEvalRun', id: string, status: AgentEvalRunStatus } };

export type CreateAgentEvalScenarioMutationVariables = Exact<{
  agentEvalTestId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  type: AgentScenarioType;
  userMessage?: InputMaybe<Scalars['String']['input']>;
  expectedOutput?: InputMaybe<Scalars['String']['input']>;
  personaPrompt?: InputMaybe<Scalars['String']['input']>;
  maxTurns?: InputMaybe<Scalars['Int']['input']>;
  numberOfRuns?: InputMaybe<Scalars['Int']['input']>;
}>;


export type CreateAgentEvalScenarioMutation = { __typename?: 'Mutation', createAgentEvalScenario: { __typename?: 'AgentEvalScenario', id: string, name: string, type: AgentScenarioType, userMessage?: string | null, expectedOutput?: string | null, personaPrompt?: string | null, maxTurns?: number | null, numberOfRuns?: number | null, createdDate?: any | null, lastModifiedDate?: any | null, judges: Array<{ __typename?: 'AgentScenarioJudge', id: string, name: string, type: AgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null }> } };

export type CreateAgentEvalTestMutationVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
  name: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
}>;


export type CreateAgentEvalTestMutation = { __typename?: 'Mutation', createAgentEvalTest: { __typename?: 'AgentEvalTest', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null } };

export type CreateAgentJudgeMutationVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
  name: Scalars['String']['input'];
  type: AgentJudgeType;
  configuration: Scalars['Map']['input'];
}>;


export type CreateAgentJudgeMutation = { __typename?: 'Mutation', createAgentJudge: { __typename?: 'AgentJudge', id: string, name: string, type: AgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null } };

export type CreateAgentScenarioJudgeMutationVariables = Exact<{
  agentEvalScenarioId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  type: AgentJudgeType;
  configuration: Scalars['Map']['input'];
}>;


export type CreateAgentScenarioJudgeMutation = { __typename?: 'Mutation', createAgentScenarioJudge: { __typename?: 'AgentScenarioJudge', id: string, name: string, type: AgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null } };

export type CreateAgentScenarioToolSimulationMutationVariables = Exact<{
  agentEvalScenarioId: Scalars['ID']['input'];
  toolName: Scalars['String']['input'];
  responsePrompt: Scalars['String']['input'];
  simulationModel?: InputMaybe<Scalars['String']['input']>;
}>;


export type CreateAgentScenarioToolSimulationMutation = { __typename?: 'Mutation', createAgentScenarioToolSimulation: { __typename?: 'AgentScenarioToolSimulation', id: string, toolName: string, responsePrompt: string, simulationModel?: string | null } };

export type DeleteAgentEvalScenarioMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAgentEvalScenarioMutation = { __typename?: 'Mutation', deleteAgentEvalScenario: boolean };

export type DeleteAgentEvalTestMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAgentEvalTestMutation = { __typename?: 'Mutation', deleteAgentEvalTest: boolean };

export type DeleteAgentJudgeMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAgentJudgeMutation = { __typename?: 'Mutation', deleteAgentJudge: boolean };

export type DeleteAgentScenarioJudgeMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAgentScenarioJudgeMutation = { __typename?: 'Mutation', deleteAgentScenarioJudge: boolean };

export type DeleteAgentScenarioToolSimulationMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAgentScenarioToolSimulationMutation = { __typename?: 'Mutation', deleteAgentScenarioToolSimulation: boolean };

export type StartAgentEvalRunMutationVariables = Exact<{
  agentEvalTestId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  environmentId: Scalars['ID']['input'];
  scenarioIds?: InputMaybe<Array<Scalars['ID']['input']> | Scalars['ID']['input']>;
  agentJudgeIds?: InputMaybe<Array<Scalars['ID']['input']> | Scalars['ID']['input']>;
}>;


export type StartAgentEvalRunMutation = { __typename?: 'Mutation', startAgentEvalRun: { __typename?: 'AgentEvalRun', id: string, name: string, status: AgentEvalRunStatus, totalScenarios: number, completedScenarios: number, agentVersion?: string | null, createdDate?: any | null } };

export type UpdateAgentEvalScenarioMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
  userMessage?: InputMaybe<Scalars['String']['input']>;
  expectedOutput?: InputMaybe<Scalars['String']['input']>;
  personaPrompt?: InputMaybe<Scalars['String']['input']>;
  maxTurns?: InputMaybe<Scalars['Int']['input']>;
  numberOfRuns?: InputMaybe<Scalars['Int']['input']>;
}>;


export type UpdateAgentEvalScenarioMutation = { __typename?: 'Mutation', updateAgentEvalScenario: { __typename?: 'AgentEvalScenario', id: string, name: string, type: AgentScenarioType, userMessage?: string | null, expectedOutput?: string | null, personaPrompt?: string | null, maxTurns?: number | null, numberOfRuns?: number | null, createdDate?: any | null, lastModifiedDate?: any | null, judges: Array<{ __typename?: 'AgentScenarioJudge', id: string, name: string, type: AgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null }> } };

export type UpdateAgentEvalTestMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
  description?: InputMaybe<Scalars['String']['input']>;
}>;


export type UpdateAgentEvalTestMutation = { __typename?: 'Mutation', updateAgentEvalTest: { __typename?: 'AgentEvalTest', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null } };

export type UpdateAgentJudgeMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
  configuration?: InputMaybe<Scalars['Map']['input']>;
}>;


export type UpdateAgentJudgeMutation = { __typename?: 'Mutation', updateAgentJudge: { __typename?: 'AgentJudge', id: string, name: string, type: AgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null } };

export type UpdateAgentScenarioJudgeMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
  configuration?: InputMaybe<Scalars['Map']['input']>;
}>;


export type UpdateAgentScenarioJudgeMutation = { __typename?: 'Mutation', updateAgentScenarioJudge: { __typename?: 'AgentScenarioJudge', id: string, name: string, type: AgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null } };

export type UpdateAgentScenarioToolSimulationMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  toolName?: InputMaybe<Scalars['String']['input']>;
  responsePrompt?: InputMaybe<Scalars['String']['input']>;
  simulationModel?: InputMaybe<Scalars['String']['input']>;
}>;


export type UpdateAgentScenarioToolSimulationMutation = { __typename?: 'Mutation', updateAgentScenarioToolSimulation: { __typename?: 'AgentScenarioToolSimulation', id: string, toolName: string, responsePrompt: string, simulationModel?: string | null } };

export type AgentSkillQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AgentSkillQuery = { __typename?: 'Query', agentSkill: { __typename?: 'AgentSkill', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null } };

export type AgentSkillFileContentQueryVariables = Exact<{
  id: Scalars['ID']['input'];
  path: Scalars['String']['input'];
}>;


export type AgentSkillFileContentQuery = { __typename?: 'Query', agentSkillFileContent: string };

export type AgentSkillFilePathsQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AgentSkillFilePathsQuery = { __typename?: 'Query', agentSkillFilePaths: Array<string> };

export type AgentSkillsQueryVariables = Exact<{ [key: string]: never; }>;


export type AgentSkillsQuery = { __typename?: 'Query', agentSkills: Array<{ __typename?: 'AgentSkill', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null }> };

export type CreateAgentSkillMutationVariables = Exact<{
  name: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
  filename: Scalars['String']['input'];
  fileBytes: Scalars['String']['input'];
}>;


export type CreateAgentSkillMutation = { __typename?: 'Mutation', createAgentSkill: { __typename?: 'AgentSkill', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null } };

export type CreateAgentSkillFromInstructionsMutationVariables = Exact<{
  name: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
  instructions: Scalars['String']['input'];
}>;


export type CreateAgentSkillFromInstructionsMutation = { __typename?: 'Mutation', createAgentSkillFromInstructions: { __typename?: 'AgentSkill', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null } };

export type DeleteAgentSkillMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAgentSkillMutation = { __typename?: 'Mutation', deleteAgentSkill: boolean };

export type UpdateAgentSkillMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
}>;


export type UpdateAgentSkillMutation = { __typename?: 'Mutation', updateAgentSkill: { __typename?: 'AgentSkill', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null } };

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

export type DeleteMcpProjectWorkflowMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteMcpProjectWorkflowMutation = { __typename?: 'Mutation', deleteMcpProjectWorkflow?: boolean | null };

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

export type McpProjectWorkflowPropertiesQueryVariables = Exact<{
  mcpProjectWorkflowId: Scalars['ID']['input'];
}>;


export type McpProjectWorkflowPropertiesQuery = { __typename?: 'Query', mcpProjectWorkflowProperties?: Array<
    | { __typename?: 'ArrayProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, arrayDefaultValue?: Array<any | null> | null }
    | { __typename?: 'BooleanProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, booleanDefaultValue?: boolean | null }
    | { __typename?: 'DateProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'DateTimeProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'DynamicPropertiesProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'FileEntryProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'IntegerProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, integerDefaultValue?: any | null }
    | { __typename?: 'NullProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'NumberProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, numberDefaultValue?: number | null }
    | { __typename?: 'ObjectProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, objectDefaultValue?: any | null }
    | { __typename?: 'StringProperty', controlType: ControlType, defaultValue?: string | null, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'TimeProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
   | null> | null };

export type McpProjectsQueryVariables = Exact<{ [key: string]: never; }>;


export type McpProjectsQuery = { __typename?: 'Query', mcpProjects?: Array<{ __typename?: 'McpProject', id: string, mcpServerId: string, project?: { __typename?: 'Project', id: string, name: string } | null } | null> | null };

export type McpProjectsByServerIdQueryVariables = Exact<{
  mcpServerId: Scalars['ID']['input'];
}>;


export type McpProjectsByServerIdQuery = { __typename?: 'Query', mcpProjectsByServerId?: Array<{ __typename?: 'McpProject', id: string, projectDeploymentId: string, mcpServerId: string, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null, projectVersion?: number | null, project?: { __typename?: 'Project', id: string, name: string, category?: { __typename?: 'Category', id?: string | null, name?: string | null } | null, tags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null } | null, mcpProjectWorkflows?: Array<{ __typename?: 'McpProjectWorkflow', id: string, mcpProjectId: any, projectDeploymentWorkflowId: any, parameters?: any | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null, projectDeploymentWorkflow?: { __typename?: 'ProjectDeploymentWorkflow', id: string, enabled: boolean, inputs?: any | null, projectDeploymentId: string, version: number, workflowId: string, connections: Array<{ __typename?: 'ProjectDeploymentWorkflowConnection', connectionId?: string | null, workflowConnectionKey: string, workflowNodeName: string }> } | null, workflow?: { __typename?: 'Workflow', id: string, label: string } | null } | null> | null } | null> | null };

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

export type ToolEligibleProjectVersionWorkflowsQueryVariables = Exact<{
  projectId: Scalars['ID']['input'];
  projectVersion: Scalars['Int']['input'];
}>;


export type ToolEligibleProjectVersionWorkflowsQuery = { __typename?: 'Query', toolEligibleProjectVersionWorkflows: Array<{ __typename?: 'ProjectWorkflow', id: string, workflow: { __typename?: 'Workflow', id: string, label: string } }> };

export type UpdateMcpProjectMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: UpdateMcpProjectInput;
}>;


export type UpdateMcpProjectMutation = { __typename?: 'Mutation', updateMcpProject?: { __typename?: 'McpProject', id: string, mcpServerId: string, projectDeploymentId: string, projectVersion?: number | null } | null };

export type UpdateMcpProjectWorkflowMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: McpProjectWorkflowUpdateInput;
}>;


export type UpdateMcpProjectWorkflowMutation = { __typename?: 'Mutation', updateMcpProjectWorkflow?: { __typename?: 'McpProjectWorkflow', id: string, mcpProjectId: any, projectDeploymentWorkflowId: any, parameters?: any | null } | null };

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


export type WorkspaceMcpServersQuery = { __typename?: 'Query', workspaceMcpServers?: Array<{ __typename?: 'McpServer', id: string, name: string, type: PlatformType, environmentId: string, enabled: boolean, url: string, lastModifiedDate?: any | null, mcpComponents?: Array<{ __typename?: 'McpComponent', id: string, mcpServerId: string, componentName: string, componentVersion: number, title?: string | null } | null> | null, tags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null } | null> | null };

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
  environmentId: Scalars['ID']['input'];
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
  environmentId: Scalars['ID']['input'];
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

export type CreateEmbeddedMcpServerMutationVariables = Exact<{
  input: CreateEmbeddedMcpServerInput;
}>;


export type CreateEmbeddedMcpServerMutation = { __typename?: 'Mutation', createEmbeddedMcpServer?: { __typename?: 'McpServer', enabled: boolean, environmentId: string, id: string, name: string, type: PlatformType } | null };

export type CreateMcpIntegrationInstanceConfigurationMutationVariables = Exact<{
  input: CreateMcpIntegrationInstanceConfigurationInput;
}>;


export type CreateMcpIntegrationInstanceConfigurationMutation = { __typename?: 'Mutation', createMcpIntegrationInstanceConfiguration?: { __typename?: 'McpIntegrationInstanceConfiguration', id: string, integrationInstanceConfigurationId: string, mcpServerId: string } | null };

export type DeleteEmbeddedMcpServerMutationVariables = Exact<{
  mcpServerId: Scalars['ID']['input'];
}>;


export type DeleteEmbeddedMcpServerMutation = { __typename?: 'Mutation', deleteEmbeddedMcpServer?: boolean | null };

export type DeleteMcpIntegrationInstanceConfigurationMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteMcpIntegrationInstanceConfigurationMutation = { __typename?: 'Mutation', deleteMcpIntegrationInstanceConfiguration?: boolean | null };

export type DeleteMcpIntegrationInstanceConfigurationWorkflowMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteMcpIntegrationInstanceConfigurationWorkflowMutation = { __typename?: 'Mutation', deleteMcpIntegrationInstanceConfigurationWorkflow?: boolean | null };

export type EmbeddedMcpServersQueryVariables = Exact<{ [key: string]: never; }>;


export type EmbeddedMcpServersQuery = { __typename?: 'Query', embeddedMcpServers?: Array<{ __typename?: 'McpServer', id: string, enabled: boolean, environmentId: string, lastModifiedDate?: any | null, name: string, type: PlatformType, url: string, mcpComponents?: Array<{ __typename?: 'McpComponent', componentName: string, componentVersion: number, connectionId?: string | null, id: string, lastModifiedDate?: any | null, mcpServerId: string, title?: string | null, mcpTools?: Array<{ __typename?: 'McpTool', id: string, mcpComponentId: string, name: string, title?: string | null, parameters?: any | null } | null> | null } | null> | null, tags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null } | null> | null };

export type IntegrationByIdQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type IntegrationByIdQuery = { __typename?: 'Query', integration?: { __typename?: 'Integration', id: string, name: string } | null };

export type IntegrationWorkflowsQueryVariables = Exact<{ [key: string]: never; }>;


export type IntegrationWorkflowsQuery = { __typename?: 'Query', integrationWorkflows: Array<{ __typename?: 'IntegrationWorkflow', id: string, label: string, description?: string | null, integrationWorkflowId: string, workflowUuid?: string | null, workflowTaskComponentNames: Array<string>, workflowTriggerComponentNames: Array<string>, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null }> };

export type IntegrationWorkflowsByIntegrationIdQueryVariables = Exact<{
  integrationId: Scalars['ID']['input'];
}>;


export type IntegrationWorkflowsByIntegrationIdQuery = { __typename?: 'Query', integrationWorkflowsByIntegrationId: Array<{ __typename?: 'IntegrationWorkflow', id: string, label: string, description?: string | null, integrationWorkflowId: string, workflowUuid?: string | null, workflowTaskComponentNames: Array<string>, workflowTriggerComponentNames: Array<string>, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null }> };

export type McpComponentDefinitionsQueryVariables = Exact<{ [key: string]: never; }>;


export type McpComponentDefinitionsQuery = { __typename?: 'Query', mcpComponentDefinitions: Array<{ __typename?: 'ComponentDefinition', clusterElementsCount?: any | null, description?: string | null, icon?: string | null, name: string, title?: string | null, version?: number | null }> };

export type McpIntegrationInstanceConfigurationWorkflowPropertiesQueryVariables = Exact<{
  mcpIntegrationInstanceConfigurationWorkflowId: Scalars['ID']['input'];
}>;


export type McpIntegrationInstanceConfigurationWorkflowPropertiesQuery = { __typename?: 'Query', mcpIntegrationInstanceConfigurationWorkflowProperties?: Array<
    | { __typename?: 'ArrayProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, arrayDefaultValue?: Array<any | null> | null }
    | { __typename?: 'BooleanProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, booleanDefaultValue?: boolean | null }
    | { __typename?: 'DateProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'DateTimeProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'DynamicPropertiesProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'FileEntryProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'IntegerProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, integerDefaultValue?: any | null }
    | { __typename?: 'NullProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'NumberProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, numberDefaultValue?: number | null }
    | { __typename?: 'ObjectProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, objectDefaultValue?: any | null }
    | { __typename?: 'StringProperty', controlType: ControlType, defaultValue?: string | null, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'TimeProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
   | null> | null };

export type McpIntegrationInstanceConfigurationsQueryVariables = Exact<{ [key: string]: never; }>;


export type McpIntegrationInstanceConfigurationsQuery = { __typename?: 'Query', mcpIntegrationInstanceConfigurations?: Array<{ __typename?: 'McpIntegrationInstanceConfiguration', id: string, integrationInstanceConfigurationId: string, mcpServerId: string, integration?: { __typename?: 'Integration', id: string, name: string } | null, mcpIntegrationInstanceConfigurationWorkflows?: Array<{ __typename?: 'McpIntegrationInstanceConfigurationWorkflow', integrationInstanceConfigurationWorkflow?: { __typename?: 'IntegrationInstanceConfigurationWorkflow', workflowId: string } | null } | null> | null } | null> | null };

export type McpIntegrationInstanceConfigurationsByServerIdQueryVariables = Exact<{
  mcpServerId?: InputMaybe<Scalars['ID']['input']>;
}>;


export type McpIntegrationInstanceConfigurationsByServerIdQuery = { __typename?: 'Query', mcpIntegrationInstanceConfigurationsByServerId?: Array<{ __typename?: 'McpIntegrationInstanceConfiguration', id: string, integrationInstanceConfigurationId: string, integrationInstanceConfigurationName?: string | null, integrationVersion?: number | null, mcpServerId: string, integration?: { __typename?: 'Integration', componentName: string, id: string, name: string } | null, mcpIntegrationInstanceConfigurationWorkflows?: Array<{ __typename?: 'McpIntegrationInstanceConfigurationWorkflow', id: string, integrationInstanceConfigurationWorkflowId: any, mcpIntegrationInstanceConfigurationId: any, parameters?: any | null, integrationInstanceConfigurationWorkflow?: { __typename?: 'IntegrationInstanceConfigurationWorkflow', id: string, enabled: boolean, inputs?: any | null, integrationInstanceConfigurationId: string, version: number, workflowId: string, connections: Array<{ __typename?: 'IntegrationInstanceConfigurationWorkflowConnection', connectionId?: string | null, workflowConnectionKey: string, workflowNodeName: string }> } | null, workflow?: { __typename?: 'Workflow', id: string, label: string } | null } | null> | null } | null> | null };

export type ToolEligibleIntegrationInstanceConfigurationWorkflowsQueryVariables = Exact<{
  integrationInstanceConfigurationId: Scalars['ID']['input'];
}>;


export type ToolEligibleIntegrationInstanceConfigurationWorkflowsQuery = { __typename?: 'Query', toolEligibleIntegrationInstanceConfigurationWorkflows: Array<{ __typename?: 'IntegrationWorkflow', id: string, integrationWorkflowId: string, label: string }> };

export type ToolEligibleIntegrationVersionWorkflowsQueryVariables = Exact<{
  integrationId: Scalars['ID']['input'];
  integrationVersion: Scalars['Int']['input'];
}>;


export type ToolEligibleIntegrationVersionWorkflowsQuery = { __typename?: 'Query', toolEligibleIntegrationVersionWorkflows: Array<{ __typename?: 'IntegrationWorkflow', id: string, integrationWorkflowId: string, label: string }> };

export type UpdateMcpIntegrationInstanceConfigurationMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: UpdateMcpIntegrationInstanceConfigurationInput;
}>;


export type UpdateMcpIntegrationInstanceConfigurationMutation = { __typename?: 'Mutation', updateMcpIntegrationInstanceConfiguration?: { __typename?: 'McpIntegrationInstanceConfiguration', id: string, integrationInstanceConfigurationId: string, mcpServerId: string } | null };

export type UpdateMcpIntegrationInstanceConfigurationVersionMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: UpdateMcpIntegrationInstanceConfigurationVersionInput;
}>;


export type UpdateMcpIntegrationInstanceConfigurationVersionMutation = { __typename?: 'Mutation', updateMcpIntegrationInstanceConfigurationVersion?: boolean | null };

export type UpdateMcpIntegrationInstanceConfigurationWorkflowMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: McpIntegrationInstanceConfigurationWorkflowUpdateInput;
}>;


export type UpdateMcpIntegrationInstanceConfigurationWorkflowMutation = { __typename?: 'Mutation', updateMcpIntegrationInstanceConfigurationWorkflow?: { __typename?: 'McpIntegrationInstanceConfigurationWorkflow', id: string, mcpIntegrationInstanceConfigurationId: any, integrationInstanceConfigurationWorkflowId: any, parameters?: any | null } | null };

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

export type ClusterElementDefinitionQueryVariables = Exact<{
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
  clusterElementName: Scalars['String']['input'];
}>;


export type ClusterElementDefinitionQuery = { __typename?: 'Query', clusterElementDefinition: { __typename?: 'ClusterElementDefinition', componentName?: string | null, componentVersion?: number | null, description?: string | null, name: string, title?: string | null, properties: Array<
      | { __typename?: 'ArrayProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, arrayDefaultValue?: Array<any | null> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null, items?: Array<
          | { __typename?: 'ArrayProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, arrayDefaultValue?: Array<any | null> | null }
          | { __typename?: 'BooleanProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, booleanDefaultValue?: boolean | null }
          | { __typename?: 'DateProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, dateDefaultValue?: string | null }
          | { __typename?: 'DateTimeProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, dateTimeDefaultValue?: string | null }
          | { __typename?: 'DynamicPropertiesProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, propertiesDataSource?: { __typename?: 'PropertiesDataSource', propertiesLookupDependsOn?: Array<string> | null } | null }
          | { __typename?: 'FileEntryProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
          | { __typename?: 'IntegerProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, integerDefaultValue?: any | null, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
          | { __typename?: 'NullProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
          | { __typename?: 'NumberProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, numberDefaultValue?: number | null, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
          | { __typename?: 'ObjectProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, objectDefaultValue?: any | null }
          | { __typename?: 'StringProperty', controlType: ControlType, defaultValue?: string | null, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
          | { __typename?: 'TimeProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, timeDefaultValue?: string | null }
        > | null }
      | { __typename?: 'BooleanProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, booleanDefaultValue?: boolean | null }
      | { __typename?: 'DateProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, dateDefaultValue?: string | null }
      | { __typename?: 'DateTimeProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, dateTimeDefaultValue?: string | null }
      | { __typename?: 'DynamicPropertiesProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, propertiesDataSource?: { __typename?: 'PropertiesDataSource', propertiesLookupDependsOn?: Array<string> | null } | null }
      | { __typename?: 'FileEntryProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
      | { __typename?: 'IntegerProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, integerDefaultValue?: any | null, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
      | { __typename?: 'NullProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
      | { __typename?: 'NumberProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, numberDefaultValue?: number | null, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
      | { __typename?: 'ObjectProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, objectDefaultValue?: any | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null, properties?: Array<
          | { __typename?: 'ArrayProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, arrayDefaultValue?: Array<any | null> | null }
          | { __typename?: 'BooleanProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, booleanDefaultValue?: boolean | null }
          | { __typename?: 'DateProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, dateDefaultValue?: string | null }
          | { __typename?: 'DateTimeProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, dateTimeDefaultValue?: string | null }
          | { __typename?: 'DynamicPropertiesProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, propertiesDataSource?: { __typename?: 'PropertiesDataSource', propertiesLookupDependsOn?: Array<string> | null } | null }
          | { __typename?: 'FileEntryProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
          | { __typename?: 'IntegerProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, integerDefaultValue?: any | null, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
          | { __typename?: 'NullProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
          | { __typename?: 'NumberProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, numberDefaultValue?: number | null, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
          | { __typename?: 'ObjectProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, objectDefaultValue?: any | null }
          | { __typename?: 'StringProperty', controlType: ControlType, defaultValue?: string | null, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
          | { __typename?: 'TimeProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, timeDefaultValue?: string | null }
        > | null }
      | { __typename?: 'StringProperty', controlType: ControlType, defaultValue?: string | null, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
      | { __typename?: 'TimeProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, timeDefaultValue?: string | null }
    > } };

export type ClusterElementDynamicPropertiesQueryVariables = Exact<{
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
  clusterElementName: Scalars['String']['input'];
  propertyName: Scalars['String']['input'];
  connectionId?: InputMaybe<Scalars['Long']['input']>;
  inputParameters?: InputMaybe<Scalars['Map']['input']>;
  lookupDependsOnPaths?: InputMaybe<Array<Scalars['String']['input']> | Scalars['String']['input']>;
}>;


export type ClusterElementDynamicPropertiesQuery = { __typename?: 'Query', clusterElementDynamicProperties: Array<
    | { __typename?: 'ArrayProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null, items?: Array<
        | { __typename?: 'ArrayProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
        | { __typename?: 'BooleanProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
        | { __typename?: 'DateProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
        | { __typename?: 'DateTimeProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
        | { __typename?: 'DynamicPropertiesProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, propertiesDataSource?: { __typename?: 'PropertiesDataSource', propertiesLookupDependsOn?: Array<string> | null } | null }
        | { __typename?: 'FileEntryProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
        | { __typename?: 'IntegerProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
        | { __typename?: 'NullProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
        | { __typename?: 'NumberProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
        | { __typename?: 'ObjectProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
        | { __typename?: 'StringProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
        | { __typename?: 'TimeProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
      > | null }
    | { __typename?: 'BooleanProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'DateProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'DateTimeProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'DynamicPropertiesProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, propertiesDataSource?: { __typename?: 'PropertiesDataSource', propertiesLookupDependsOn?: Array<string> | null } | null }
    | { __typename?: 'FileEntryProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'IntegerProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
    | { __typename?: 'NullProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
    | { __typename?: 'NumberProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
    | { __typename?: 'ObjectProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null, properties?: Array<
        | { __typename?: 'ArrayProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
        | { __typename?: 'BooleanProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
        | { __typename?: 'DateProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
        | { __typename?: 'DateTimeProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
        | { __typename?: 'DynamicPropertiesProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, propertiesDataSource?: { __typename?: 'PropertiesDataSource', propertiesLookupDependsOn?: Array<string> | null } | null }
        | { __typename?: 'FileEntryProperty', advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
        | { __typename?: 'IntegerProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
        | { __typename?: 'NullProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
        | { __typename?: 'NumberProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
        | { __typename?: 'ObjectProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
        | { __typename?: 'StringProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
        | { __typename?: 'TimeProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
      > | null }
    | { __typename?: 'StringProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType, options?: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> | null, optionsDataSource?: { __typename?: 'OptionsDataSource', optionsLookupDependsOn?: Array<string> | null } | null }
    | { __typename?: 'TimeProperty', controlType: ControlType, label?: string | null, placeholder?: string | null, advancedOption?: boolean | null, description?: string | null, displayCondition?: string | null, expressionEnabled?: boolean | null, hidden?: boolean | null, name?: string | null, required?: boolean | null, type: PropertyType }
  > };

export type ClusterElementOptionsQueryVariables = Exact<{
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
  clusterElementName: Scalars['String']['input'];
  propertyName: Scalars['String']['input'];
  connectionId?: InputMaybe<Scalars['Long']['input']>;
  inputParameters?: InputMaybe<Scalars['Map']['input']>;
  lookupDependsOnPaths?: InputMaybe<Array<Scalars['String']['input']> | Scalars['String']['input']>;
}>;


export type ClusterElementOptionsQuery = { __typename?: 'Query', clusterElementOptions: Array<{ __typename?: 'Option', description?: string | null, label?: string | null, value?: any | null }> };

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


export type CreateMcpComponentMutation = { __typename?: 'Mutation', createMcpComponent?: { __typename?: 'McpComponent', id: string, componentName: string, componentVersion: number, title?: string | null, mcpServerId: string, connectionId?: string | null } | null };

export type CreateMcpComponentWithToolsMutationVariables = Exact<{
  input: McpComponentWithToolsInput;
}>;


export type CreateMcpComponentWithToolsMutation = { __typename?: 'Mutation', createMcpComponentWithTools?: { __typename?: 'McpComponent', id: string, componentName: string, componentVersion: number, title?: string | null, mcpServerId: string, connectionId?: string | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null } | null };

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

export type DeleteMcpToolMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteMcpToolMutation = { __typename?: 'Mutation', deleteMcpTool?: boolean | null };

export type EnvironmentsQueryVariables = Exact<{ [key: string]: never; }>;


export type EnvironmentsQuery = { __typename?: 'Query', environments?: Array<{ __typename?: 'Environment', id: string, name: string } | null> | null };

export type ManagementMcpServerUrlQueryVariables = Exact<{ [key: string]: never; }>;


export type ManagementMcpServerUrlQuery = { __typename?: 'Query', managementMcpServerUrl?: string | null };

export type McpComponentsByServerIdQueryVariables = Exact<{
  mcpServerId: Scalars['ID']['input'];
}>;


export type McpComponentsByServerIdQuery = { __typename?: 'Query', mcpComponentsByServerId?: Array<{ __typename?: 'McpComponent', id: string, componentName: string, componentVersion: number, title?: string | null, connectionId?: string | null, lastModifiedDate?: any | null, mcpServerId: string, version?: number | null, mcpTools?: Array<{ __typename?: 'McpTool', id: string, mcpComponentId: string, name: string, parameters?: any | null, title?: string | null, version?: number | null } | null> | null } | null> | null };

export type McpServerTagsQueryVariables = Exact<{
  type: PlatformType;
}>;


export type McpServerTagsQuery = { __typename?: 'Query', mcpServerTags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null };

export type McpServersQueryVariables = Exact<{
  type: PlatformType;
}>;


export type McpServersQuery = { __typename?: 'Query', mcpServers?: Array<{ __typename?: 'McpServer', id: string, name: string, type: PlatformType, environmentId: string, enabled: boolean, secretKey: string, lastModifiedDate?: any | null, mcpComponents?: Array<{ __typename?: 'McpComponent', id: string, mcpServerId: string, componentName: string, componentVersion: number, title?: string | null } | null> | null, tags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null } | null> | null };

export type McpToolsByComponentIdQueryVariables = Exact<{
  mcpComponentId: Scalars['ID']['input'];
}>;


export type McpToolsByComponentIdQuery = { __typename?: 'Query', mcpToolsByComponentId?: Array<{ __typename?: 'McpTool', id: string, name: string, title?: string | null, mcpComponentId: string, parameters?: any | null, version?: number | null } | null> | null };

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

export type SaveClusterElementTestOutputMutationVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
  clusterElementType: Scalars['String']['input'];
  clusterElementWorkflowNodeName: Scalars['String']['input'];
  environmentId: Scalars['Long']['input'];
  inputParameters?: InputMaybe<Scalars['Map']['input']>;
}>;


export type SaveClusterElementTestOutputMutation = { __typename?: 'Mutation', saveClusterElementTestOutput?: { __typename?: 'WorkflowNodeTestOutputResult', id: any, workflowId: string, workflowNodeName: string } | null };

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


export type UpdateMcpComponentWithToolsMutation = { __typename?: 'Mutation', updateMcpComponentWithTools?: { __typename?: 'McpComponent', id: string, componentName: string, componentVersion: number, title?: string | null, mcpServerId: string, connectionId?: string | null, createdBy?: string | null, createdDate?: any | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, version?: number | null } | null };

export type UpdateMcpServerUrlMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type UpdateMcpServerUrlMutation = { __typename?: 'Mutation', updateMcpServerUrl: string };

export type UpdateMcpToolMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: McpToolInput;
}>;


export type UpdateMcpToolMutation = { __typename?: 'Mutation', updateMcpTool?: { __typename?: 'McpTool', id: string, name: string, mcpComponentId: string, parameters?: any | null, version?: number | null } | null };

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


export type CreateIdentityProviderMutation = { __typename?: 'Mutation', createIdentityProvider: { __typename?: 'IdentityProviderType', autoProvision: boolean, clientId?: string | null, createdBy?: string | null, createdDate?: any | null, defaultAuthority: string, domains: Array<string>, enabled: boolean, enforced: boolean, id: string, issuerUri?: string | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, metadataUri?: string | null, mfaMethod?: string | null, mfaRequired: boolean, name: string, nameIdFormat?: string | null, scopes?: string | null, signingCertificate?: string | null, type: string } };

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


export type IdentityProviderQuery = { __typename?: 'Query', identityProvider?: { __typename?: 'IdentityProviderType', autoProvision: boolean, clientId?: string | null, createdBy?: string | null, createdDate?: any | null, defaultAuthority: string, domains: Array<string>, enabled: boolean, enforced: boolean, id: string, issuerUri?: string | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, metadataUri?: string | null, mfaMethod?: string | null, mfaRequired: boolean, name: string, nameIdFormat?: string | null, scopes?: string | null, signingCertificate?: string | null, type: string } | null };

export type IdentityProvidersQueryVariables = Exact<{ [key: string]: never; }>;


export type IdentityProvidersQuery = { __typename?: 'Query', identityProviders: Array<{ __typename?: 'IdentityProviderType', autoProvision: boolean, clientId?: string | null, createdBy?: string | null, createdDate?: any | null, defaultAuthority: string, domains: Array<string>, enabled: boolean, enforced: boolean, id: string, issuerUri?: string | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, metadataUri?: string | null, mfaMethod?: string | null, mfaRequired: boolean, name: string, nameIdFormat?: string | null, scopes?: string | null, signingCertificate?: string | null, type: string } | null> };

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


export type UpdateIdentityProviderMutation = { __typename?: 'Mutation', updateIdentityProvider: { __typename?: 'IdentityProviderType', autoProvision: boolean, clientId?: string | null, createdBy?: string | null, createdDate?: any | null, defaultAuthority: string, domains: Array<string>, enabled: boolean, enforced: boolean, id: string, issuerUri?: string | null, lastModifiedBy?: string | null, lastModifiedDate?: any | null, metadataUri?: string | null, mfaMethod?: string | null, mfaRequired: boolean, name: string, nameIdFormat?: string | null, scopes?: string | null, signingCertificate?: string | null, type: string } };

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



export const AgentEvalResultDocument = new TypedDocumentString(`
    query agentEvalResult($id: ID!) {
  agentEvalResult(id: $id) {
    id
    scenario {
      id
      name
      type
      userMessage
      expectedOutput
      personaPrompt
      maxTurns
      judges {
        id
        name
        type
        configuration
        createdDate
        lastModifiedDate
      }
      createdDate
      lastModifiedDate
    }
    status
    score
    errorMessage
    transcriptFile
    verdicts {
      id
      judgeName
      judgeType
      judgeScope
      passed
      score
      explanation
    }
    createdDate
  }
}
    `);

export const useAgentEvalResultQuery = <
      TData = AgentEvalResultQuery,
      TError = unknown
    >(
      variables: AgentEvalResultQueryVariables,
      options?: Omit<UseQueryOptions<AgentEvalResultQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AgentEvalResultQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AgentEvalResultQuery, TError, TData>(
      {
    queryKey: ['agentEvalResult', variables],
    queryFn: fetcher<AgentEvalResultQuery, AgentEvalResultQueryVariables>(AgentEvalResultDocument, variables),
    ...options
  }
    )};

export const AgentEvalResultTranscriptDocument = new TypedDocumentString(`
    query agentEvalResultTranscript($id: ID!) {
  agentEvalResultTranscript(id: $id)
}
    `);

export const useAgentEvalResultTranscriptQuery = <
      TData = AgentEvalResultTranscriptQuery,
      TError = unknown
    >(
      variables: AgentEvalResultTranscriptQueryVariables,
      options?: Omit<UseQueryOptions<AgentEvalResultTranscriptQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AgentEvalResultTranscriptQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AgentEvalResultTranscriptQuery, TError, TData>(
      {
    queryKey: ['agentEvalResultTranscript', variables],
    queryFn: fetcher<AgentEvalResultTranscriptQuery, AgentEvalResultTranscriptQueryVariables>(AgentEvalResultTranscriptDocument, variables),
    ...options
  }
    )};

export const AgentEvalRunDocument = new TypedDocumentString(`
    query agentEvalRun($id: ID!) {
  agentEvalRun(id: $id) {
    id
    name
    status
    averageScore
    totalScenarios
    completedScenarios
    agentVersion
    totalInputTokens
    totalOutputTokens
    startedDate
    completedDate
    results {
      id
      scenario {
        id
        name
        type
        userMessage
        expectedOutput
        personaPrompt
        maxTurns
        judges {
          id
          name
          type
          configuration
          createdDate
          lastModifiedDate
        }
        createdDate
        lastModifiedDate
      }
      status
      score
      errorMessage
      transcriptFile
      inputTokens
      outputTokens
      runIndex
      verdicts {
        id
        judgeName
        judgeType
        judgeScope
        passed
        score
        explanation
      }
      createdDate
    }
    createdDate
  }
}
    `);

export const useAgentEvalRunQuery = <
      TData = AgentEvalRunQuery,
      TError = unknown
    >(
      variables: AgentEvalRunQueryVariables,
      options?: Omit<UseQueryOptions<AgentEvalRunQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AgentEvalRunQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AgentEvalRunQuery, TError, TData>(
      {
    queryKey: ['agentEvalRun', variables],
    queryFn: fetcher<AgentEvalRunQuery, AgentEvalRunQueryVariables>(AgentEvalRunDocument, variables),
    ...options
  }
    )};

export const AgentEvalRunsDocument = new TypedDocumentString(`
    query agentEvalRuns($agentEvalTestId: ID!, $limit: Int, $offset: Int) {
  agentEvalRuns(agentEvalTestId: $agentEvalTestId, limit: $limit, offset: $offset) {
    id
    name
    status
    averageScore
    totalScenarios
    completedScenarios
    startedDate
    completedDate
    createdDate
  }
}
    `);

export const useAgentEvalRunsQuery = <
      TData = AgentEvalRunsQuery,
      TError = unknown
    >(
      variables: AgentEvalRunsQueryVariables,
      options?: Omit<UseQueryOptions<AgentEvalRunsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AgentEvalRunsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AgentEvalRunsQuery, TError, TData>(
      {
    queryKey: ['agentEvalRuns', variables],
    queryFn: fetcher<AgentEvalRunsQuery, AgentEvalRunsQueryVariables>(AgentEvalRunsDocument, variables),
    ...options
  }
    )};

export const AgentEvalTestDocument = new TypedDocumentString(`
    query agentEvalTest($id: ID!) {
  agentEvalTest(id: $id) {
    id
    name
    description
    scenarios {
      id
      name
      type
      userMessage
      expectedOutput
      personaPrompt
      maxTurns
      numberOfRuns
      judges {
        id
        name
        type
        configuration
        createdDate
        lastModifiedDate
      }
      toolSimulations {
        id
        responsePrompt
        simulationModel
        toolName
        createdDate
        lastModifiedDate
      }
      createdDate
      lastModifiedDate
    }
    createdDate
    lastModifiedDate
  }
}
    `);

export const useAgentEvalTestQuery = <
      TData = AgentEvalTestQuery,
      TError = unknown
    >(
      variables: AgentEvalTestQueryVariables,
      options?: Omit<UseQueryOptions<AgentEvalTestQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AgentEvalTestQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AgentEvalTestQuery, TError, TData>(
      {
    queryKey: ['agentEvalTest', variables],
    queryFn: fetcher<AgentEvalTestQuery, AgentEvalTestQueryVariables>(AgentEvalTestDocument, variables),
    ...options
  }
    )};

export const AgentEvalTestsDocument = new TypedDocumentString(`
    query agentEvalTests($workflowId: String!, $workflowNodeName: String!) {
  agentEvalTests(workflowId: $workflowId, workflowNodeName: $workflowNodeName) {
    id
    name
    description
    scenarios {
      id
      name
      type
      userMessage
      expectedOutput
      personaPrompt
      maxTurns
      numberOfRuns
      toolSimulations {
        id
        toolName
        responsePrompt
        simulationModel
      }
      judges {
        id
        name
        type
        configuration
        createdDate
        lastModifiedDate
      }
      createdDate
      lastModifiedDate
    }
    createdDate
    lastModifiedDate
  }
}
    `);

export const useAgentEvalTestsQuery = <
      TData = AgentEvalTestsQuery,
      TError = unknown
    >(
      variables: AgentEvalTestsQueryVariables,
      options?: Omit<UseQueryOptions<AgentEvalTestsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AgentEvalTestsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AgentEvalTestsQuery, TError, TData>(
      {
    queryKey: ['agentEvalTests', variables],
    queryFn: fetcher<AgentEvalTestsQuery, AgentEvalTestsQueryVariables>(AgentEvalTestsDocument, variables),
    ...options
  }
    )};

export const AgentJudgesDocument = new TypedDocumentString(`
    query agentJudges($workflowId: String!, $workflowNodeName: String!) {
  agentJudges(workflowId: $workflowId, workflowNodeName: $workflowNodeName) {
    id
    name
    type
    configuration
    createdDate
    lastModifiedDate
  }
}
    `);

export const useAgentJudgesQuery = <
      TData = AgentJudgesQuery,
      TError = unknown
    >(
      variables: AgentJudgesQueryVariables,
      options?: Omit<UseQueryOptions<AgentJudgesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AgentJudgesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AgentJudgesQuery, TError, TData>(
      {
    queryKey: ['agentJudges', variables],
    queryFn: fetcher<AgentJudgesQuery, AgentJudgesQueryVariables>(AgentJudgesDocument, variables),
    ...options
  }
    )};

export const CancelAgentEvalRunDocument = new TypedDocumentString(`
    mutation cancelAgentEvalRun($id: ID!) {
  cancelAgentEvalRun(id: $id) {
    id
    status
  }
}
    `);

export const useCancelAgentEvalRunMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CancelAgentEvalRunMutation, TError, CancelAgentEvalRunMutationVariables, TContext>) => {
    
    return useMutation<CancelAgentEvalRunMutation, TError, CancelAgentEvalRunMutationVariables, TContext>(
      {
    mutationKey: ['cancelAgentEvalRun'],
    mutationFn: (variables?: CancelAgentEvalRunMutationVariables) => fetcher<CancelAgentEvalRunMutation, CancelAgentEvalRunMutationVariables>(CancelAgentEvalRunDocument, variables)(),
    ...options
  }
    )};

export const CreateAgentEvalScenarioDocument = new TypedDocumentString(`
    mutation createAgentEvalScenario($agentEvalTestId: ID!, $name: String!, $type: AgentScenarioType!, $userMessage: String, $expectedOutput: String, $personaPrompt: String, $maxTurns: Int, $numberOfRuns: Int) {
  createAgentEvalScenario(
    agentEvalTestId: $agentEvalTestId
    name: $name
    type: $type
    userMessage: $userMessage
    expectedOutput: $expectedOutput
    personaPrompt: $personaPrompt
    maxTurns: $maxTurns
    numberOfRuns: $numberOfRuns
  ) {
    id
    name
    type
    userMessage
    expectedOutput
    personaPrompt
    maxTurns
    numberOfRuns
    judges {
      id
      name
      type
      configuration
      createdDate
      lastModifiedDate
    }
    createdDate
    lastModifiedDate
  }
}
    `);

export const useCreateAgentEvalScenarioMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAgentEvalScenarioMutation, TError, CreateAgentEvalScenarioMutationVariables, TContext>) => {
    
    return useMutation<CreateAgentEvalScenarioMutation, TError, CreateAgentEvalScenarioMutationVariables, TContext>(
      {
    mutationKey: ['createAgentEvalScenario'],
    mutationFn: (variables?: CreateAgentEvalScenarioMutationVariables) => fetcher<CreateAgentEvalScenarioMutation, CreateAgentEvalScenarioMutationVariables>(CreateAgentEvalScenarioDocument, variables)(),
    ...options
  }
    )};

export const CreateAgentEvalTestDocument = new TypedDocumentString(`
    mutation createAgentEvalTest($workflowId: String!, $workflowNodeName: String!, $name: String!, $description: String) {
  createAgentEvalTest(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    name: $name
    description: $description
  ) {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useCreateAgentEvalTestMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAgentEvalTestMutation, TError, CreateAgentEvalTestMutationVariables, TContext>) => {
    
    return useMutation<CreateAgentEvalTestMutation, TError, CreateAgentEvalTestMutationVariables, TContext>(
      {
    mutationKey: ['createAgentEvalTest'],
    mutationFn: (variables?: CreateAgentEvalTestMutationVariables) => fetcher<CreateAgentEvalTestMutation, CreateAgentEvalTestMutationVariables>(CreateAgentEvalTestDocument, variables)(),
    ...options
  }
    )};

export const CreateAgentJudgeDocument = new TypedDocumentString(`
    mutation createAgentJudge($workflowId: String!, $workflowNodeName: String!, $name: String!, $type: AgentJudgeType!, $configuration: Map!) {
  createAgentJudge(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    name: $name
    type: $type
    configuration: $configuration
  ) {
    id
    name
    type
    configuration
    createdDate
    lastModifiedDate
  }
}
    `);

export const useCreateAgentJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAgentJudgeMutation, TError, CreateAgentJudgeMutationVariables, TContext>) => {
    
    return useMutation<CreateAgentJudgeMutation, TError, CreateAgentJudgeMutationVariables, TContext>(
      {
    mutationKey: ['createAgentJudge'],
    mutationFn: (variables?: CreateAgentJudgeMutationVariables) => fetcher<CreateAgentJudgeMutation, CreateAgentJudgeMutationVariables>(CreateAgentJudgeDocument, variables)(),
    ...options
  }
    )};

export const CreateAgentScenarioJudgeDocument = new TypedDocumentString(`
    mutation createAgentScenarioJudge($agentEvalScenarioId: ID!, $name: String!, $type: AgentJudgeType!, $configuration: Map!) {
  createAgentScenarioJudge(
    agentEvalScenarioId: $agentEvalScenarioId
    name: $name
    type: $type
    configuration: $configuration
  ) {
    id
    name
    type
    configuration
    createdDate
    lastModifiedDate
  }
}
    `);

export const useCreateAgentScenarioJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAgentScenarioJudgeMutation, TError, CreateAgentScenarioJudgeMutationVariables, TContext>) => {
    
    return useMutation<CreateAgentScenarioJudgeMutation, TError, CreateAgentScenarioJudgeMutationVariables, TContext>(
      {
    mutationKey: ['createAgentScenarioJudge'],
    mutationFn: (variables?: CreateAgentScenarioJudgeMutationVariables) => fetcher<CreateAgentScenarioJudgeMutation, CreateAgentScenarioJudgeMutationVariables>(CreateAgentScenarioJudgeDocument, variables)(),
    ...options
  }
    )};

export const CreateAgentScenarioToolSimulationDocument = new TypedDocumentString(`
    mutation createAgentScenarioToolSimulation($agentEvalScenarioId: ID!, $toolName: String!, $responsePrompt: String!, $simulationModel: String) {
  createAgentScenarioToolSimulation(
    agentEvalScenarioId: $agentEvalScenarioId
    toolName: $toolName
    responsePrompt: $responsePrompt
    simulationModel: $simulationModel
  ) {
    id
    toolName
    responsePrompt
    simulationModel
  }
}
    `);

export const useCreateAgentScenarioToolSimulationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAgentScenarioToolSimulationMutation, TError, CreateAgentScenarioToolSimulationMutationVariables, TContext>) => {
    
    return useMutation<CreateAgentScenarioToolSimulationMutation, TError, CreateAgentScenarioToolSimulationMutationVariables, TContext>(
      {
    mutationKey: ['createAgentScenarioToolSimulation'],
    mutationFn: (variables?: CreateAgentScenarioToolSimulationMutationVariables) => fetcher<CreateAgentScenarioToolSimulationMutation, CreateAgentScenarioToolSimulationMutationVariables>(CreateAgentScenarioToolSimulationDocument, variables)(),
    ...options
  }
    )};

export const DeleteAgentEvalScenarioDocument = new TypedDocumentString(`
    mutation deleteAgentEvalScenario($id: ID!) {
  deleteAgentEvalScenario(id: $id)
}
    `);

export const useDeleteAgentEvalScenarioMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAgentEvalScenarioMutation, TError, DeleteAgentEvalScenarioMutationVariables, TContext>) => {
    
    return useMutation<DeleteAgentEvalScenarioMutation, TError, DeleteAgentEvalScenarioMutationVariables, TContext>(
      {
    mutationKey: ['deleteAgentEvalScenario'],
    mutationFn: (variables?: DeleteAgentEvalScenarioMutationVariables) => fetcher<DeleteAgentEvalScenarioMutation, DeleteAgentEvalScenarioMutationVariables>(DeleteAgentEvalScenarioDocument, variables)(),
    ...options
  }
    )};

export const DeleteAgentEvalTestDocument = new TypedDocumentString(`
    mutation deleteAgentEvalTest($id: ID!) {
  deleteAgentEvalTest(id: $id)
}
    `);

export const useDeleteAgentEvalTestMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAgentEvalTestMutation, TError, DeleteAgentEvalTestMutationVariables, TContext>) => {
    
    return useMutation<DeleteAgentEvalTestMutation, TError, DeleteAgentEvalTestMutationVariables, TContext>(
      {
    mutationKey: ['deleteAgentEvalTest'],
    mutationFn: (variables?: DeleteAgentEvalTestMutationVariables) => fetcher<DeleteAgentEvalTestMutation, DeleteAgentEvalTestMutationVariables>(DeleteAgentEvalTestDocument, variables)(),
    ...options
  }
    )};

export const DeleteAgentJudgeDocument = new TypedDocumentString(`
    mutation deleteAgentJudge($id: ID!) {
  deleteAgentJudge(id: $id)
}
    `);

export const useDeleteAgentJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAgentJudgeMutation, TError, DeleteAgentJudgeMutationVariables, TContext>) => {
    
    return useMutation<DeleteAgentJudgeMutation, TError, DeleteAgentJudgeMutationVariables, TContext>(
      {
    mutationKey: ['deleteAgentJudge'],
    mutationFn: (variables?: DeleteAgentJudgeMutationVariables) => fetcher<DeleteAgentJudgeMutation, DeleteAgentJudgeMutationVariables>(DeleteAgentJudgeDocument, variables)(),
    ...options
  }
    )};

export const DeleteAgentScenarioJudgeDocument = new TypedDocumentString(`
    mutation deleteAgentScenarioJudge($id: ID!) {
  deleteAgentScenarioJudge(id: $id)
}
    `);

export const useDeleteAgentScenarioJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAgentScenarioJudgeMutation, TError, DeleteAgentScenarioJudgeMutationVariables, TContext>) => {
    
    return useMutation<DeleteAgentScenarioJudgeMutation, TError, DeleteAgentScenarioJudgeMutationVariables, TContext>(
      {
    mutationKey: ['deleteAgentScenarioJudge'],
    mutationFn: (variables?: DeleteAgentScenarioJudgeMutationVariables) => fetcher<DeleteAgentScenarioJudgeMutation, DeleteAgentScenarioJudgeMutationVariables>(DeleteAgentScenarioJudgeDocument, variables)(),
    ...options
  }
    )};

export const DeleteAgentScenarioToolSimulationDocument = new TypedDocumentString(`
    mutation deleteAgentScenarioToolSimulation($id: ID!) {
  deleteAgentScenarioToolSimulation(id: $id)
}
    `);

export const useDeleteAgentScenarioToolSimulationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAgentScenarioToolSimulationMutation, TError, DeleteAgentScenarioToolSimulationMutationVariables, TContext>) => {
    
    return useMutation<DeleteAgentScenarioToolSimulationMutation, TError, DeleteAgentScenarioToolSimulationMutationVariables, TContext>(
      {
    mutationKey: ['deleteAgentScenarioToolSimulation'],
    mutationFn: (variables?: DeleteAgentScenarioToolSimulationMutationVariables) => fetcher<DeleteAgentScenarioToolSimulationMutation, DeleteAgentScenarioToolSimulationMutationVariables>(DeleteAgentScenarioToolSimulationDocument, variables)(),
    ...options
  }
    )};

export const StartAgentEvalRunDocument = new TypedDocumentString(`
    mutation startAgentEvalRun($agentEvalTestId: ID!, $name: String!, $environmentId: ID!, $scenarioIds: [ID!], $agentJudgeIds: [ID!]) {
  startAgentEvalRun(
    agentEvalTestId: $agentEvalTestId
    name: $name
    environmentId: $environmentId
    scenarioIds: $scenarioIds
    agentJudgeIds: $agentJudgeIds
  ) {
    id
    name
    status
    totalScenarios
    completedScenarios
    agentVersion
    createdDate
  }
}
    `);

export const useStartAgentEvalRunMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<StartAgentEvalRunMutation, TError, StartAgentEvalRunMutationVariables, TContext>) => {
    
    return useMutation<StartAgentEvalRunMutation, TError, StartAgentEvalRunMutationVariables, TContext>(
      {
    mutationKey: ['startAgentEvalRun'],
    mutationFn: (variables?: StartAgentEvalRunMutationVariables) => fetcher<StartAgentEvalRunMutation, StartAgentEvalRunMutationVariables>(StartAgentEvalRunDocument, variables)(),
    ...options
  }
    )};

export const UpdateAgentEvalScenarioDocument = new TypedDocumentString(`
    mutation updateAgentEvalScenario($id: ID!, $name: String, $userMessage: String, $expectedOutput: String, $personaPrompt: String, $maxTurns: Int, $numberOfRuns: Int) {
  updateAgentEvalScenario(
    id: $id
    name: $name
    userMessage: $userMessage
    expectedOutput: $expectedOutput
    personaPrompt: $personaPrompt
    maxTurns: $maxTurns
    numberOfRuns: $numberOfRuns
  ) {
    id
    name
    type
    userMessage
    expectedOutput
    personaPrompt
    maxTurns
    numberOfRuns
    judges {
      id
      name
      type
      configuration
      createdDate
      lastModifiedDate
    }
    createdDate
    lastModifiedDate
  }
}
    `);

export const useUpdateAgentEvalScenarioMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAgentEvalScenarioMutation, TError, UpdateAgentEvalScenarioMutationVariables, TContext>) => {
    
    return useMutation<UpdateAgentEvalScenarioMutation, TError, UpdateAgentEvalScenarioMutationVariables, TContext>(
      {
    mutationKey: ['updateAgentEvalScenario'],
    mutationFn: (variables?: UpdateAgentEvalScenarioMutationVariables) => fetcher<UpdateAgentEvalScenarioMutation, UpdateAgentEvalScenarioMutationVariables>(UpdateAgentEvalScenarioDocument, variables)(),
    ...options
  }
    )};

export const UpdateAgentEvalTestDocument = new TypedDocumentString(`
    mutation updateAgentEvalTest($id: ID!, $name: String, $description: String) {
  updateAgentEvalTest(id: $id, name: $name, description: $description) {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useUpdateAgentEvalTestMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAgentEvalTestMutation, TError, UpdateAgentEvalTestMutationVariables, TContext>) => {
    
    return useMutation<UpdateAgentEvalTestMutation, TError, UpdateAgentEvalTestMutationVariables, TContext>(
      {
    mutationKey: ['updateAgentEvalTest'],
    mutationFn: (variables?: UpdateAgentEvalTestMutationVariables) => fetcher<UpdateAgentEvalTestMutation, UpdateAgentEvalTestMutationVariables>(UpdateAgentEvalTestDocument, variables)(),
    ...options
  }
    )};

export const UpdateAgentJudgeDocument = new TypedDocumentString(`
    mutation updateAgentJudge($id: ID!, $name: String, $configuration: Map) {
  updateAgentJudge(id: $id, name: $name, configuration: $configuration) {
    id
    name
    type
    configuration
    createdDate
    lastModifiedDate
  }
}
    `);

export const useUpdateAgentJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAgentJudgeMutation, TError, UpdateAgentJudgeMutationVariables, TContext>) => {
    
    return useMutation<UpdateAgentJudgeMutation, TError, UpdateAgentJudgeMutationVariables, TContext>(
      {
    mutationKey: ['updateAgentJudge'],
    mutationFn: (variables?: UpdateAgentJudgeMutationVariables) => fetcher<UpdateAgentJudgeMutation, UpdateAgentJudgeMutationVariables>(UpdateAgentJudgeDocument, variables)(),
    ...options
  }
    )};

export const UpdateAgentScenarioJudgeDocument = new TypedDocumentString(`
    mutation updateAgentScenarioJudge($id: ID!, $name: String, $configuration: Map) {
  updateAgentScenarioJudge(id: $id, name: $name, configuration: $configuration) {
    id
    name
    type
    configuration
    createdDate
    lastModifiedDate
  }
}
    `);

export const useUpdateAgentScenarioJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAgentScenarioJudgeMutation, TError, UpdateAgentScenarioJudgeMutationVariables, TContext>) => {
    
    return useMutation<UpdateAgentScenarioJudgeMutation, TError, UpdateAgentScenarioJudgeMutationVariables, TContext>(
      {
    mutationKey: ['updateAgentScenarioJudge'],
    mutationFn: (variables?: UpdateAgentScenarioJudgeMutationVariables) => fetcher<UpdateAgentScenarioJudgeMutation, UpdateAgentScenarioJudgeMutationVariables>(UpdateAgentScenarioJudgeDocument, variables)(),
    ...options
  }
    )};

export const UpdateAgentScenarioToolSimulationDocument = new TypedDocumentString(`
    mutation updateAgentScenarioToolSimulation($id: ID!, $toolName: String, $responsePrompt: String, $simulationModel: String) {
  updateAgentScenarioToolSimulation(
    id: $id
    toolName: $toolName
    responsePrompt: $responsePrompt
    simulationModel: $simulationModel
  ) {
    id
    toolName
    responsePrompt
    simulationModel
  }
}
    `);

export const useUpdateAgentScenarioToolSimulationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAgentScenarioToolSimulationMutation, TError, UpdateAgentScenarioToolSimulationMutationVariables, TContext>) => {
    
    return useMutation<UpdateAgentScenarioToolSimulationMutation, TError, UpdateAgentScenarioToolSimulationMutationVariables, TContext>(
      {
    mutationKey: ['updateAgentScenarioToolSimulation'],
    mutationFn: (variables?: UpdateAgentScenarioToolSimulationMutationVariables) => fetcher<UpdateAgentScenarioToolSimulationMutation, UpdateAgentScenarioToolSimulationMutationVariables>(UpdateAgentScenarioToolSimulationDocument, variables)(),
    ...options
  }
    )};

export const AgentSkillDocument = new TypedDocumentString(`
    query agentSkill($id: ID!) {
  agentSkill(id: $id) {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useAgentSkillQuery = <
      TData = AgentSkillQuery,
      TError = unknown
    >(
      variables: AgentSkillQueryVariables,
      options?: Omit<UseQueryOptions<AgentSkillQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AgentSkillQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AgentSkillQuery, TError, TData>(
      {
    queryKey: ['agentSkill', variables],
    queryFn: fetcher<AgentSkillQuery, AgentSkillQueryVariables>(AgentSkillDocument, variables),
    ...options
  }
    )};

export const AgentSkillFileContentDocument = new TypedDocumentString(`
    query agentSkillFileContent($id: ID!, $path: String!) {
  agentSkillFileContent(id: $id, path: $path)
}
    `);

export const useAgentSkillFileContentQuery = <
      TData = AgentSkillFileContentQuery,
      TError = unknown
    >(
      variables: AgentSkillFileContentQueryVariables,
      options?: Omit<UseQueryOptions<AgentSkillFileContentQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AgentSkillFileContentQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AgentSkillFileContentQuery, TError, TData>(
      {
    queryKey: ['agentSkillFileContent', variables],
    queryFn: fetcher<AgentSkillFileContentQuery, AgentSkillFileContentQueryVariables>(AgentSkillFileContentDocument, variables),
    ...options
  }
    )};

export const AgentSkillFilePathsDocument = new TypedDocumentString(`
    query agentSkillFilePaths($id: ID!) {
  agentSkillFilePaths(id: $id)
}
    `);

export const useAgentSkillFilePathsQuery = <
      TData = AgentSkillFilePathsQuery,
      TError = unknown
    >(
      variables: AgentSkillFilePathsQueryVariables,
      options?: Omit<UseQueryOptions<AgentSkillFilePathsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AgentSkillFilePathsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AgentSkillFilePathsQuery, TError, TData>(
      {
    queryKey: ['agentSkillFilePaths', variables],
    queryFn: fetcher<AgentSkillFilePathsQuery, AgentSkillFilePathsQueryVariables>(AgentSkillFilePathsDocument, variables),
    ...options
  }
    )};

export const AgentSkillsDocument = new TypedDocumentString(`
    query agentSkills {
  agentSkills {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useAgentSkillsQuery = <
      TData = AgentSkillsQuery,
      TError = unknown
    >(
      variables?: AgentSkillsQueryVariables,
      options?: Omit<UseQueryOptions<AgentSkillsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AgentSkillsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AgentSkillsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['agentSkills'] : ['agentSkills', variables],
    queryFn: fetcher<AgentSkillsQuery, AgentSkillsQueryVariables>(AgentSkillsDocument, variables),
    ...options
  }
    )};

export const CreateAgentSkillDocument = new TypedDocumentString(`
    mutation createAgentSkill($name: String!, $description: String, $filename: String!, $fileBytes: String!) {
  createAgentSkill(
    name: $name
    description: $description
    filename: $filename
    fileBytes: $fileBytes
  ) {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useCreateAgentSkillMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAgentSkillMutation, TError, CreateAgentSkillMutationVariables, TContext>) => {
    
    return useMutation<CreateAgentSkillMutation, TError, CreateAgentSkillMutationVariables, TContext>(
      {
    mutationKey: ['createAgentSkill'],
    mutationFn: (variables?: CreateAgentSkillMutationVariables) => fetcher<CreateAgentSkillMutation, CreateAgentSkillMutationVariables>(CreateAgentSkillDocument, variables)(),
    ...options
  }
    )};

export const CreateAgentSkillFromInstructionsDocument = new TypedDocumentString(`
    mutation createAgentSkillFromInstructions($name: String!, $description: String, $instructions: String!) {
  createAgentSkillFromInstructions(
    name: $name
    description: $description
    instructions: $instructions
  ) {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useCreateAgentSkillFromInstructionsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAgentSkillFromInstructionsMutation, TError, CreateAgentSkillFromInstructionsMutationVariables, TContext>) => {
    
    return useMutation<CreateAgentSkillFromInstructionsMutation, TError, CreateAgentSkillFromInstructionsMutationVariables, TContext>(
      {
    mutationKey: ['createAgentSkillFromInstructions'],
    mutationFn: (variables?: CreateAgentSkillFromInstructionsMutationVariables) => fetcher<CreateAgentSkillFromInstructionsMutation, CreateAgentSkillFromInstructionsMutationVariables>(CreateAgentSkillFromInstructionsDocument, variables)(),
    ...options
  }
    )};

export const DeleteAgentSkillDocument = new TypedDocumentString(`
    mutation deleteAgentSkill($id: ID!) {
  deleteAgentSkill(id: $id)
}
    `);

export const useDeleteAgentSkillMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAgentSkillMutation, TError, DeleteAgentSkillMutationVariables, TContext>) => {
    
    return useMutation<DeleteAgentSkillMutation, TError, DeleteAgentSkillMutationVariables, TContext>(
      {
    mutationKey: ['deleteAgentSkill'],
    mutationFn: (variables?: DeleteAgentSkillMutationVariables) => fetcher<DeleteAgentSkillMutation, DeleteAgentSkillMutationVariables>(DeleteAgentSkillDocument, variables)(),
    ...options
  }
    )};

export const UpdateAgentSkillDocument = new TypedDocumentString(`
    mutation updateAgentSkill($id: ID!, $name: String!, $description: String) {
  updateAgentSkill(id: $id, name: $name, description: $description) {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useUpdateAgentSkillMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAgentSkillMutation, TError, UpdateAgentSkillMutationVariables, TContext>) => {
    
    return useMutation<UpdateAgentSkillMutation, TError, UpdateAgentSkillMutationVariables, TContext>(
      {
    mutationKey: ['updateAgentSkill'],
    mutationFn: (variables?: UpdateAgentSkillMutationVariables) => fetcher<UpdateAgentSkillMutation, UpdateAgentSkillMutationVariables>(UpdateAgentSkillDocument, variables)(),
    ...options
  }
    )};

export const ApprovalTaskDocument = new TypedDocumentString(`
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
    `);

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

export const ApprovalTasksDocument = new TypedDocumentString(`
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
    `);

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

export const CreateApprovalTaskDocument = new TypedDocumentString(`
    mutation createApprovalTask($approvalTask: ApprovalTaskInput!) {
  createApprovalTask(approvalTask: $approvalTask) {
    description
    id
    name
  }
}
    `);

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

export const DeleteApprovalTaskDocument = new TypedDocumentString(`
    mutation deleteApprovalTask($id: ID!) {
  deleteApprovalTask(id: $id)
}
    `);

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

export const UpdateApprovalTaskDocument = new TypedDocumentString(`
    mutation updateApprovalTask($approvalTask: ApprovalTaskInput!) {
  updateApprovalTask(approvalTask: $approvalTask) {
    description
    id
    name
    version
  }
}
    `);

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

export const CreateMcpProjectDocument = new TypedDocumentString(`
    mutation createMcpProject($input: CreateMcpProjectInput!) {
  createMcpProject(input: $input) {
    id
    mcpServerId
    projectDeploymentId
    projectVersion
  }
}
    `);

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

export const CreateWorkspaceApiKeyDocument = new TypedDocumentString(`
    mutation createWorkspaceApiKey($workspaceId: ID!, $name: String!, $environmentId: ID!) {
  createWorkspaceApiKey(
    workspaceId: $workspaceId
    name: $name
    environmentId: $environmentId
  )
}
    `);

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

export const CreateMcpServerDocument = new TypedDocumentString(`
    mutation createMcpServer($input: CreateWorkspaceMcpServerInput!) {
  createWorkspaceMcpServer(input: $input) {
    id
    name
    type
    environmentId
    enabled
  }
}
    `);

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

export const DeleteMcpProjectDocument = new TypedDocumentString(`
    mutation deleteMcpProject($id: ID!) {
  deleteMcpProject(id: $id)
}
    `);

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

export const DeleteMcpProjectWorkflowDocument = new TypedDocumentString(`
    mutation deleteMcpProjectWorkflow($id: ID!) {
  deleteMcpProjectWorkflow(id: $id)
}
    `);

export const useDeleteMcpProjectWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteMcpProjectWorkflowMutation, TError, DeleteMcpProjectWorkflowMutationVariables, TContext>) => {
    
    return useMutation<DeleteMcpProjectWorkflowMutation, TError, DeleteMcpProjectWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['deleteMcpProjectWorkflow'],
    mutationFn: (variables?: DeleteMcpProjectWorkflowMutationVariables) => fetcher<DeleteMcpProjectWorkflowMutation, DeleteMcpProjectWorkflowMutationVariables>(DeleteMcpProjectWorkflowDocument, variables)(),
    ...options
  }
    )};

export const DeleteSharedProjectDocument = new TypedDocumentString(`
    mutation deleteSharedProject($id: ID!) {
  deleteSharedProject(id: $id)
}
    `);

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

export const DeleteSharedWorkflowDocument = new TypedDocumentString(`
    mutation deleteSharedWorkflow($workflowId: String!) {
  deleteSharedWorkflow(workflowId: $workflowId)
}
    `);

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

export const DeleteWorkspaceApiKeyDocument = new TypedDocumentString(`
    mutation deleteWorkspaceApiKey($apiKeyId: ID!) {
  deleteWorkspaceApiKey(apiKeyId: $apiKeyId)
}
    `);

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

export const DeleteWorkspaceMcpServerDocument = new TypedDocumentString(`
    mutation deleteWorkspaceMcpServer($id: ID!) {
  deleteWorkspaceMcpServer(mcpServerId: $id)
}
    `);

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

export const DisconnectConnectionDocument = new TypedDocumentString(`
    mutation DisconnectConnection($connectionId: ID!) {
  disconnectConnection(connectionId: $connectionId)
}
    `);

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

export const ExportSharedProjectDocument = new TypedDocumentString(`
    mutation exportSharedProject($id: ID!, $description: String) {
  exportSharedProject(id: $id, description: $description)
}
    `);

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

export const ExportSharedWorkflowDocument = new TypedDocumentString(`
    mutation exportSharedWorkflow($workflowId: String!, $description: String) {
  exportSharedWorkflow(workflowId: $workflowId, description: $description)
}
    `);

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

export const ImportProjectTemplateDocument = new TypedDocumentString(`
    mutation importProjectTemplate($id: String!, $workspaceId: ID!, $sharedProject: Boolean!) {
  importProjectTemplate(
    id: $id
    workspaceId: $workspaceId
    sharedProject: $sharedProject
  )
}
    `);

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

export const ImportWorkflowTemplateDocument = new TypedDocumentString(`
    mutation importWorkflowTemplate($workflowUuid: String!, $projectId: ID!, $sharedWorkflow: Boolean!) {
  importWorkflowTemplate(
    id: $workflowUuid
    projectId: $projectId
    sharedWorkflow: $sharedWorkflow
  )
}
    `);

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

export const McpProjectWorkflowPropertiesDocument = new TypedDocumentString(`
    query mcpProjectWorkflowProperties($mcpProjectWorkflowId: ID!) {
  mcpProjectWorkflowProperties(mcpProjectWorkflowId: $mcpProjectWorkflowId) {
    advancedOption
    description
    displayCondition
    expressionEnabled
    hidden
    name
    required
    type
    ... on StringProperty {
      controlType
      defaultValue
      label
      placeholder
    }
    ... on IntegerProperty {
      controlType
      integerDefaultValue: defaultValue
      label
      placeholder
    }
    ... on NumberProperty {
      controlType
      label
      numberDefaultValue: defaultValue
      placeholder
    }
    ... on BooleanProperty {
      booleanDefaultValue: defaultValue
      controlType
      label
      placeholder
    }
    ... on ArrayProperty {
      arrayDefaultValue: defaultValue
      controlType
      label
      placeholder
    }
    ... on ObjectProperty {
      controlType
      label
      objectDefaultValue: defaultValue
      placeholder
    }
  }
}
    `);

export const useMcpProjectWorkflowPropertiesQuery = <
      TData = McpProjectWorkflowPropertiesQuery,
      TError = unknown
    >(
      variables: McpProjectWorkflowPropertiesQueryVariables,
      options?: Omit<UseQueryOptions<McpProjectWorkflowPropertiesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpProjectWorkflowPropertiesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpProjectWorkflowPropertiesQuery, TError, TData>(
      {
    queryKey: ['mcpProjectWorkflowProperties', variables],
    queryFn: fetcher<McpProjectWorkflowPropertiesQuery, McpProjectWorkflowPropertiesQueryVariables>(McpProjectWorkflowPropertiesDocument, variables),
    ...options
  }
    )};

export const McpProjectsDocument = new TypedDocumentString(`
    query mcpProjects {
  mcpProjects {
    id
    mcpServerId
    project {
      id
      name
    }
  }
}
    `);

export const useMcpProjectsQuery = <
      TData = McpProjectsQuery,
      TError = unknown
    >(
      variables?: McpProjectsQueryVariables,
      options?: Omit<UseQueryOptions<McpProjectsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpProjectsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpProjectsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['mcpProjects'] : ['mcpProjects', variables],
    queryFn: fetcher<McpProjectsQuery, McpProjectsQueryVariables>(McpProjectsDocument, variables),
    ...options
  }
    )};

export const McpProjectsByServerIdDocument = new TypedDocumentString(`
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
      parameters
      projectDeploymentWorkflow {
        id
        connections {
          connectionId
          workflowConnectionKey
          workflowNodeName
        }
        enabled
        inputs
        projectDeploymentId
        version
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
    `);

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

export const PreBuiltProjectTemplatesDocument = new TypedDocumentString(`
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
    `);

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

export const PreBuiltWorkflowTemplatesDocument = new TypedDocumentString(`
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
    `);

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

export const ProjectByIdDocument = new TypedDocumentString(`
    query projectById($id: ID!) {
  project(id: $id) {
    id
    name
  }
}
    `);

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

export const ProjectTemplateDocument = new TypedDocumentString(`
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
    `);

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

export const SharedProjectDocument = new TypedDocumentString(`
    query sharedProject($projectUuid: String!) {
  sharedProject(projectUuid: $projectUuid) {
    description
    exported
    projectVersion
    publicUrl
  }
}
    `);

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

export const SharedWorkflowDocument = new TypedDocumentString(`
    query sharedWorkflow($workflowUuid: String!) {
  sharedWorkflow(workflowUuid: $workflowUuid) {
    description
    exported
    projectVersion
    publicUrl
  }
}
    `);

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

export const ToolEligibleProjectVersionWorkflowsDocument = new TypedDocumentString(`
    query toolEligibleProjectVersionWorkflows($projectId: ID!, $projectVersion: Int!) {
  toolEligibleProjectVersionWorkflows(
    projectId: $projectId
    projectVersion: $projectVersion
  ) {
    id
    workflow {
      id
      label
    }
  }
}
    `);

export const useToolEligibleProjectVersionWorkflowsQuery = <
      TData = ToolEligibleProjectVersionWorkflowsQuery,
      TError = unknown
    >(
      variables: ToolEligibleProjectVersionWorkflowsQueryVariables,
      options?: Omit<UseQueryOptions<ToolEligibleProjectVersionWorkflowsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ToolEligibleProjectVersionWorkflowsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ToolEligibleProjectVersionWorkflowsQuery, TError, TData>(
      {
    queryKey: ['toolEligibleProjectVersionWorkflows', variables],
    queryFn: fetcher<ToolEligibleProjectVersionWorkflowsQuery, ToolEligibleProjectVersionWorkflowsQueryVariables>(ToolEligibleProjectVersionWorkflowsDocument, variables),
    ...options
  }
    )};

export const UpdateMcpProjectDocument = new TypedDocumentString(`
    mutation updateMcpProject($id: ID!, $input: UpdateMcpProjectInput!) {
  updateMcpProject(id: $id, input: $input) {
    id
    mcpServerId
    projectDeploymentId
    projectVersion
  }
}
    `);

export const useUpdateMcpProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpProjectMutation, TError, UpdateMcpProjectMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpProjectMutation, TError, UpdateMcpProjectMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpProject'],
    mutationFn: (variables?: UpdateMcpProjectMutationVariables) => fetcher<UpdateMcpProjectMutation, UpdateMcpProjectMutationVariables>(UpdateMcpProjectDocument, variables)(),
    ...options
  }
    )};

export const UpdateMcpProjectWorkflowDocument = new TypedDocumentString(`
    mutation updateMcpProjectWorkflow($id: ID!, $input: McpProjectWorkflowUpdateInput!) {
  updateMcpProjectWorkflow(id: $id, input: $input) {
    id
    mcpProjectId
    projectDeploymentWorkflowId
    parameters
  }
}
    `);

export const useUpdateMcpProjectWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpProjectWorkflowMutation, TError, UpdateMcpProjectWorkflowMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpProjectWorkflowMutation, TError, UpdateMcpProjectWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpProjectWorkflow'],
    mutationFn: (variables?: UpdateMcpProjectWorkflowMutationVariables) => fetcher<UpdateMcpProjectWorkflowMutation, UpdateMcpProjectWorkflowMutationVariables>(UpdateMcpProjectWorkflowDocument, variables)(),
    ...options
  }
    )};

export const UpdateMcpServerDocument = new TypedDocumentString(`
    mutation updateMcpServer($id: ID!, $input: McpServerUpdateInput!) {
  updateMcpServer(id: $id, input: $input) {
    id
    name
    enabled
  }
}
    `);

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

export const UpdateMcpServerTagsDocument = new TypedDocumentString(`
    mutation updateMcpServerTags($id: ID!, $tags: [TagInput!]!) {
  updateMcpServerTags(id: $id, tags: $tags) {
    id
  }
}
    `);

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

export const UpdateWorkspaceApiKeyDocument = new TypedDocumentString(`
    mutation updateWorkspaceApiKey($apiKeyId: ID!, $name: String!) {
  updateWorkspaceApiKey(apiKeyId: $apiKeyId, name: $name)
}
    `);

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

export const WorkflowChatProjectDeploymentWorkflowDocument = new TypedDocumentString(`
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
    `);

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

export const WorkflowChatWorkspaceProjectDeploymentsDocument = new TypedDocumentString(`
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
    `);

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

export const WorkflowTemplateDocument = new TypedDocumentString(`
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
    `);

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

export const WorkspaceApiKeysDocument = new TypedDocumentString(`
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
    `);

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

export const WorkspaceMcpServersDocument = new TypedDocumentString(`
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
      title
    }
    tags {
      id
      name
    }
    lastModifiedDate
  }
}
    `);

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

export const AddDataTableColumnDocument = new TypedDocumentString(`
    mutation addDataTableColumn($input: AddColumnInput!) {
  addDataTableColumn(input: $input)
}
    `);

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

export const CreateDataTableDocument = new TypedDocumentString(`
    mutation createDataTable($input: CreateDataTableInput!) {
  createDataTable(input: $input)
}
    `);

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

export const DataTableRowsDocument = new TypedDocumentString(`
    query dataTableRows($environmentId: ID!, $tableId: ID!) {
  dataTableRows(environmentId: $environmentId, tableId: $tableId) {
    id
    values
  }
}
    `);

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

export const DataTableRowsPageDocument = new TypedDocumentString(`
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
    `);

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

export const DataTableTagsDocument = new TypedDocumentString(`
    query dataTableTags {
  dataTableTags {
    id
    name
  }
}
    `);

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

export const DataTableTagsByTableDocument = new TypedDocumentString(`
    query dataTableTagsByTable {
  dataTableTagsByTable {
    tableId
    tags {
      id
      name
    }
  }
}
    `);

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

export const DataTablesDocument = new TypedDocumentString(`
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
    `);

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

export const DeleteDataTableRowDocument = new TypedDocumentString(`
    mutation deleteDataTableRow($input: DeleteRowInput!) {
  deleteDataTableRow(input: $input)
}
    `);

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

export const DropDataTableDocument = new TypedDocumentString(`
    mutation dropDataTable($input: RemoveTableInput!) {
  dropDataTable(input: $input)
}
    `);

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

export const DuplicateDataTableDocument = new TypedDocumentString(`
    mutation duplicateDataTable($input: DuplicateDataTableInput!) {
  duplicateDataTable(input: $input)
}
    `);

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

export const ExportDataTableCsvDocument = new TypedDocumentString(`
    query exportDataTableCsv($environmentId: ID!, $tableId: ID!) {
  exportDataTableCsv(environmentId: $environmentId, tableId: $tableId)
}
    `);

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

export const ImportDataTableCsvDocument = new TypedDocumentString(`
    mutation importDataTableCsv($input: ImportCsvInput!) {
  importDataTableCsv(input: $input)
}
    `);

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

export const InsertDataTableRowDocument = new TypedDocumentString(`
    mutation insertDataTableRow($input: InsertRowInput!) {
  insertDataTableRow(input: $input) {
    id
    values
  }
}
    `);

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

export const RemoveDataTableColumnDocument = new TypedDocumentString(`
    mutation removeDataTableColumn($input: RemoveColumnInput!) {
  removeDataTableColumn(input: $input)
}
    `);

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

export const RenameDataTableDocument = new TypedDocumentString(`
    mutation renameDataTable($input: RenameDataTableInput!) {
  renameDataTable(input: $input)
}
    `);

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

export const RenameDataTableColumnDocument = new TypedDocumentString(`
    mutation renameDataTableColumn($input: RenameColumnInput!) {
  renameDataTableColumn(input: $input)
}
    `);

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

export const UpdateDataTableRowDocument = new TypedDocumentString(`
    mutation updateDataTableRow($input: UpdateRowInput!) {
  updateDataTableRow(input: $input) {
    id
    values
  }
}
    `);

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

export const UpdateDataTableTagsDocument = new TypedDocumentString(`
    mutation updateDataTableTags($input: UpdateDataTableTagsInput!) {
  updateDataTableTags(input: $input)
}
    `);

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

export const CreateKnowledgeBaseDocument = new TypedDocumentString(`
    mutation createKnowledgeBase($knowledgeBase: KnowledgeBaseInput!, $environmentId: ID!, $workspaceId: ID!) {
  createKnowledgeBase(
    knowledgeBase: $knowledgeBase
    environmentId: $environmentId
    workspaceId: $workspaceId
  ) {
    id
    name
  }
}
    `);

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

export const DeleteKnowledgeBaseDocument = new TypedDocumentString(`
    mutation deleteKnowledgeBase($id: ID!) {
  deleteKnowledgeBase(id: $id)
}
    `);

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

export const DeleteKnowledgeBaseDocumentDocument = new TypedDocumentString(`
    mutation deleteKnowledgeBaseDocument($id: ID!) {
  deleteKnowledgeBaseDocument(id: $id)
}
    `);

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

export const DeleteKnowledgeBaseDocumentChunkDocument = new TypedDocumentString(`
    mutation deleteKnowledgeBaseDocumentChunk($id: ID!) {
  deleteKnowledgeBaseDocumentChunk(id: $id)
}
    `);

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

export const KnowledgeBaseDocument = new TypedDocumentString(`
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
    `);

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

export const KnowledgeBaseDocumentStatusDocument = new TypedDocumentString(`
    query knowledgeBaseDocumentStatus($id: ID!) {
  knowledgeBaseDocumentStatus(id: $id) {
    documentId
    status
    timestamp
    message
  }
}
    `);

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

export const KnowledgeBaseDocumentTagsDocument = new TypedDocumentString(`
    query knowledgeBaseDocumentTags {
  knowledgeBaseDocumentTags {
    id
    name
  }
}
    `);

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

export const KnowledgeBaseDocumentTagsByDocumentDocument = new TypedDocumentString(`
    query knowledgeBaseDocumentTagsByDocument {
  knowledgeBaseDocumentTagsByDocument {
    knowledgeBaseDocumentId
    tags {
      id
      name
    }
  }
}
    `);

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

export const KnowledgeBaseTagsDocument = new TypedDocumentString(`
    query knowledgeBaseTags {
  knowledgeBaseTags {
    id
    name
  }
}
    `);

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

export const KnowledgeBaseTagsByKnowledgeBaseDocument = new TypedDocumentString(`
    query knowledgeBaseTagsByKnowledgeBase {
  knowledgeBaseTagsByKnowledgeBase {
    knowledgeBaseId
    tags {
      id
      name
    }
  }
}
    `);

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

export const KnowledgeBasesDocument = new TypedDocumentString(`
    query knowledgeBases($environmentId: ID!, $workspaceId: ID!) {
  knowledgeBases(environmentId: $environmentId, workspaceId: $workspaceId) {
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
    `);

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

export const SearchKnowledgeBaseDocument = new TypedDocumentString(`
    query searchKnowledgeBase($id: ID!, $query: String!, $metadataFilters: String) {
  searchKnowledgeBase(id: $id, query: $query, metadataFilters: $metadataFilters) {
    id
    knowledgeBaseDocumentId
    content
    metadata
    score
  }
}
    `);

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

export const UpdateKnowledgeBaseDocument = new TypedDocumentString(`
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
    `);

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

export const UpdateKnowledgeBaseDocumentChunkDocument = new TypedDocumentString(`
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
    `);

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

export const UpdateKnowledgeBaseDocumentTagsDocument = new TypedDocumentString(`
    mutation updateKnowledgeBaseDocumentTags($input: UpdateKnowledgeBaseDocumentTagsInput!) {
  updateKnowledgeBaseDocumentTags(input: $input)
}
    `);

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

export const UpdateKnowledgeBaseTagsDocument = new TypedDocumentString(`
    mutation updateKnowledgeBaseTags($input: UpdateKnowledgeBaseTagsInput!) {
  updateKnowledgeBaseTags(input: $input)
}
    `);

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

export const AutomationSearchDocument = new TypedDocumentString(`
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
    `);

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

export const ConnectedUserProjectsDocument = new TypedDocumentString(`
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
    `);

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

export const CreateEmbeddedMcpServerDocument = new TypedDocumentString(`
    mutation createEmbeddedMcpServer($input: CreateEmbeddedMcpServerInput!) {
  createEmbeddedMcpServer(input: $input) {
    enabled
    environmentId
    id
    name
    type
  }
}
    `);

export const useCreateEmbeddedMcpServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateEmbeddedMcpServerMutation, TError, CreateEmbeddedMcpServerMutationVariables, TContext>) => {
    
    return useMutation<CreateEmbeddedMcpServerMutation, TError, CreateEmbeddedMcpServerMutationVariables, TContext>(
      {
    mutationKey: ['createEmbeddedMcpServer'],
    mutationFn: (variables?: CreateEmbeddedMcpServerMutationVariables) => fetcher<CreateEmbeddedMcpServerMutation, CreateEmbeddedMcpServerMutationVariables>(CreateEmbeddedMcpServerDocument, variables)(),
    ...options
  }
    )};

export const CreateMcpIntegrationInstanceConfigurationDocument = new TypedDocumentString(`
    mutation createMcpIntegrationInstanceConfiguration($input: CreateMcpIntegrationInstanceConfigurationInput!) {
  createMcpIntegrationInstanceConfiguration(input: $input) {
    id
    integrationInstanceConfigurationId
    mcpServerId
  }
}
    `);

export const useCreateMcpIntegrationInstanceConfigurationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateMcpIntegrationInstanceConfigurationMutation, TError, CreateMcpIntegrationInstanceConfigurationMutationVariables, TContext>) => {
    
    return useMutation<CreateMcpIntegrationInstanceConfigurationMutation, TError, CreateMcpIntegrationInstanceConfigurationMutationVariables, TContext>(
      {
    mutationKey: ['createMcpIntegrationInstanceConfiguration'],
    mutationFn: (variables?: CreateMcpIntegrationInstanceConfigurationMutationVariables) => fetcher<CreateMcpIntegrationInstanceConfigurationMutation, CreateMcpIntegrationInstanceConfigurationMutationVariables>(CreateMcpIntegrationInstanceConfigurationDocument, variables)(),
    ...options
  }
    )};

export const DeleteEmbeddedMcpServerDocument = new TypedDocumentString(`
    mutation deleteEmbeddedMcpServer($mcpServerId: ID!) {
  deleteEmbeddedMcpServer(mcpServerId: $mcpServerId)
}
    `);

export const useDeleteEmbeddedMcpServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteEmbeddedMcpServerMutation, TError, DeleteEmbeddedMcpServerMutationVariables, TContext>) => {
    
    return useMutation<DeleteEmbeddedMcpServerMutation, TError, DeleteEmbeddedMcpServerMutationVariables, TContext>(
      {
    mutationKey: ['deleteEmbeddedMcpServer'],
    mutationFn: (variables?: DeleteEmbeddedMcpServerMutationVariables) => fetcher<DeleteEmbeddedMcpServerMutation, DeleteEmbeddedMcpServerMutationVariables>(DeleteEmbeddedMcpServerDocument, variables)(),
    ...options
  }
    )};

export const DeleteMcpIntegrationInstanceConfigurationDocument = new TypedDocumentString(`
    mutation deleteMcpIntegrationInstanceConfiguration($id: ID!) {
  deleteMcpIntegrationInstanceConfiguration(id: $id)
}
    `);

export const useDeleteMcpIntegrationInstanceConfigurationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteMcpIntegrationInstanceConfigurationMutation, TError, DeleteMcpIntegrationInstanceConfigurationMutationVariables, TContext>) => {
    
    return useMutation<DeleteMcpIntegrationInstanceConfigurationMutation, TError, DeleteMcpIntegrationInstanceConfigurationMutationVariables, TContext>(
      {
    mutationKey: ['deleteMcpIntegrationInstanceConfiguration'],
    mutationFn: (variables?: DeleteMcpIntegrationInstanceConfigurationMutationVariables) => fetcher<DeleteMcpIntegrationInstanceConfigurationMutation, DeleteMcpIntegrationInstanceConfigurationMutationVariables>(DeleteMcpIntegrationInstanceConfigurationDocument, variables)(),
    ...options
  }
    )};

export const DeleteMcpIntegrationInstanceConfigurationWorkflowDocument = new TypedDocumentString(`
    mutation deleteMcpIntegrationInstanceConfigurationWorkflow($id: ID!) {
  deleteMcpIntegrationInstanceConfigurationWorkflow(id: $id)
}
    `);

export const useDeleteMcpIntegrationInstanceConfigurationWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteMcpIntegrationInstanceConfigurationWorkflowMutation, TError, DeleteMcpIntegrationInstanceConfigurationWorkflowMutationVariables, TContext>) => {
    
    return useMutation<DeleteMcpIntegrationInstanceConfigurationWorkflowMutation, TError, DeleteMcpIntegrationInstanceConfigurationWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['deleteMcpIntegrationInstanceConfigurationWorkflow'],
    mutationFn: (variables?: DeleteMcpIntegrationInstanceConfigurationWorkflowMutationVariables) => fetcher<DeleteMcpIntegrationInstanceConfigurationWorkflowMutation, DeleteMcpIntegrationInstanceConfigurationWorkflowMutationVariables>(DeleteMcpIntegrationInstanceConfigurationWorkflowDocument, variables)(),
    ...options
  }
    )};

export const EmbeddedMcpServersDocument = new TypedDocumentString(`
    query embeddedMcpServers {
  embeddedMcpServers {
    id
    enabled
    environmentId
    lastModifiedDate
    mcpComponents {
      componentName
      componentVersion
      connectionId
      id
      lastModifiedDate
      mcpServerId
      title
      mcpTools {
        id
        mcpComponentId
        name
        title
        parameters
      }
    }
    name
    tags {
      id
      name
    }
    type
    url
  }
}
    `);

export const useEmbeddedMcpServersQuery = <
      TData = EmbeddedMcpServersQuery,
      TError = unknown
    >(
      variables?: EmbeddedMcpServersQueryVariables,
      options?: Omit<UseQueryOptions<EmbeddedMcpServersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<EmbeddedMcpServersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<EmbeddedMcpServersQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['embeddedMcpServers'] : ['embeddedMcpServers', variables],
    queryFn: fetcher<EmbeddedMcpServersQuery, EmbeddedMcpServersQueryVariables>(EmbeddedMcpServersDocument, variables),
    ...options
  }
    )};

export const IntegrationByIdDocument = new TypedDocumentString(`
    query integrationById($id: ID!) {
  integration(id: $id) {
    id
    name
  }
}
    `);

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

export const IntegrationWorkflowsDocument = new TypedDocumentString(`
    query integrationWorkflows {
  integrationWorkflows {
    id
    label
    description
    integrationWorkflowId
    workflowUuid
    workflowTaskComponentNames
    workflowTriggerComponentNames
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
  }
}
    `);

export const useIntegrationWorkflowsQuery = <
      TData = IntegrationWorkflowsQuery,
      TError = unknown
    >(
      variables?: IntegrationWorkflowsQueryVariables,
      options?: Omit<UseQueryOptions<IntegrationWorkflowsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<IntegrationWorkflowsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<IntegrationWorkflowsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['integrationWorkflows'] : ['integrationWorkflows', variables],
    queryFn: fetcher<IntegrationWorkflowsQuery, IntegrationWorkflowsQueryVariables>(IntegrationWorkflowsDocument, variables),
    ...options
  }
    )};

export const IntegrationWorkflowsByIntegrationIdDocument = new TypedDocumentString(`
    query integrationWorkflowsByIntegrationId($integrationId: ID!) {
  integrationWorkflowsByIntegrationId(integrationId: $integrationId) {
    id
    label
    description
    integrationWorkflowId
    workflowUuid
    workflowTaskComponentNames
    workflowTriggerComponentNames
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
  }
}
    `);

export const useIntegrationWorkflowsByIntegrationIdQuery = <
      TData = IntegrationWorkflowsByIntegrationIdQuery,
      TError = unknown
    >(
      variables: IntegrationWorkflowsByIntegrationIdQueryVariables,
      options?: Omit<UseQueryOptions<IntegrationWorkflowsByIntegrationIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<IntegrationWorkflowsByIntegrationIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<IntegrationWorkflowsByIntegrationIdQuery, TError, TData>(
      {
    queryKey: ['integrationWorkflowsByIntegrationId', variables],
    queryFn: fetcher<IntegrationWorkflowsByIntegrationIdQuery, IntegrationWorkflowsByIntegrationIdQueryVariables>(IntegrationWorkflowsByIntegrationIdDocument, variables),
    ...options
  }
    )};

export const McpComponentDefinitionsDocument = new TypedDocumentString(`
    query mcpComponentDefinitions {
  mcpComponentDefinitions {
    clusterElementsCount
    description
    icon
    name
    title
    version
  }
}
    `);

export const useMcpComponentDefinitionsQuery = <
      TData = McpComponentDefinitionsQuery,
      TError = unknown
    >(
      variables?: McpComponentDefinitionsQueryVariables,
      options?: Omit<UseQueryOptions<McpComponentDefinitionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpComponentDefinitionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpComponentDefinitionsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['mcpComponentDefinitions'] : ['mcpComponentDefinitions', variables],
    queryFn: fetcher<McpComponentDefinitionsQuery, McpComponentDefinitionsQueryVariables>(McpComponentDefinitionsDocument, variables),
    ...options
  }
    )};

export const McpIntegrationInstanceConfigurationWorkflowPropertiesDocument = new TypedDocumentString(`
    query mcpIntegrationInstanceConfigurationWorkflowProperties($mcpIntegrationInstanceConfigurationWorkflowId: ID!) {
  mcpIntegrationInstanceConfigurationWorkflowProperties(
    mcpIntegrationInstanceConfigurationWorkflowId: $mcpIntegrationInstanceConfigurationWorkflowId
  ) {
    advancedOption
    description
    displayCondition
    expressionEnabled
    hidden
    name
    required
    type
    ... on StringProperty {
      controlType
      defaultValue
      label
      placeholder
    }
    ... on IntegerProperty {
      controlType
      integerDefaultValue: defaultValue
      label
      placeholder
    }
    ... on NumberProperty {
      controlType
      label
      numberDefaultValue: defaultValue
      placeholder
    }
    ... on BooleanProperty {
      booleanDefaultValue: defaultValue
      controlType
      label
      placeholder
    }
    ... on ArrayProperty {
      arrayDefaultValue: defaultValue
      controlType
      label
      placeholder
    }
    ... on ObjectProperty {
      controlType
      label
      objectDefaultValue: defaultValue
      placeholder
    }
  }
}
    `);

export const useMcpIntegrationInstanceConfigurationWorkflowPropertiesQuery = <
      TData = McpIntegrationInstanceConfigurationWorkflowPropertiesQuery,
      TError = unknown
    >(
      variables: McpIntegrationInstanceConfigurationWorkflowPropertiesQueryVariables,
      options?: Omit<UseQueryOptions<McpIntegrationInstanceConfigurationWorkflowPropertiesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpIntegrationInstanceConfigurationWorkflowPropertiesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpIntegrationInstanceConfigurationWorkflowPropertiesQuery, TError, TData>(
      {
    queryKey: ['mcpIntegrationInstanceConfigurationWorkflowProperties', variables],
    queryFn: fetcher<McpIntegrationInstanceConfigurationWorkflowPropertiesQuery, McpIntegrationInstanceConfigurationWorkflowPropertiesQueryVariables>(McpIntegrationInstanceConfigurationWorkflowPropertiesDocument, variables),
    ...options
  }
    )};

export const McpIntegrationInstanceConfigurationsDocument = new TypedDocumentString(`
    query mcpIntegrationInstanceConfigurations {
  mcpIntegrationInstanceConfigurations {
    id
    integrationInstanceConfigurationId
    mcpServerId
    integration {
      id
      name
    }
    mcpIntegrationInstanceConfigurationWorkflows {
      integrationInstanceConfigurationWorkflow {
        workflowId
      }
    }
  }
}
    `);

export const useMcpIntegrationInstanceConfigurationsQuery = <
      TData = McpIntegrationInstanceConfigurationsQuery,
      TError = unknown
    >(
      variables?: McpIntegrationInstanceConfigurationsQueryVariables,
      options?: Omit<UseQueryOptions<McpIntegrationInstanceConfigurationsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpIntegrationInstanceConfigurationsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpIntegrationInstanceConfigurationsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['mcpIntegrationInstanceConfigurations'] : ['mcpIntegrationInstanceConfigurations', variables],
    queryFn: fetcher<McpIntegrationInstanceConfigurationsQuery, McpIntegrationInstanceConfigurationsQueryVariables>(McpIntegrationInstanceConfigurationsDocument, variables),
    ...options
  }
    )};

export const McpIntegrationInstanceConfigurationsByServerIdDocument = new TypedDocumentString(`
    query mcpIntegrationInstanceConfigurationsByServerId($mcpServerId: ID) {
  mcpIntegrationInstanceConfigurationsByServerId(mcpServerId: $mcpServerId) {
    id
    integration {
      componentName
      id
      name
    }
    integrationInstanceConfigurationId
    integrationInstanceConfigurationName
    integrationVersion
    mcpIntegrationInstanceConfigurationWorkflows {
      id
      integrationInstanceConfigurationWorkflowId
      integrationInstanceConfigurationWorkflow {
        id
        connections {
          connectionId
          workflowConnectionKey
          workflowNodeName
        }
        enabled
        inputs
        integrationInstanceConfigurationId
        version
        workflowId
      }
      mcpIntegrationInstanceConfigurationId
      parameters
      workflow {
        id
        label
      }
    }
    mcpServerId
  }
}
    `);

export const useMcpIntegrationInstanceConfigurationsByServerIdQuery = <
      TData = McpIntegrationInstanceConfigurationsByServerIdQuery,
      TError = unknown
    >(
      variables?: McpIntegrationInstanceConfigurationsByServerIdQueryVariables,
      options?: Omit<UseQueryOptions<McpIntegrationInstanceConfigurationsByServerIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpIntegrationInstanceConfigurationsByServerIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpIntegrationInstanceConfigurationsByServerIdQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['mcpIntegrationInstanceConfigurationsByServerId'] : ['mcpIntegrationInstanceConfigurationsByServerId', variables],
    queryFn: fetcher<McpIntegrationInstanceConfigurationsByServerIdQuery, McpIntegrationInstanceConfigurationsByServerIdQueryVariables>(McpIntegrationInstanceConfigurationsByServerIdDocument, variables),
    ...options
  }
    )};

export const ToolEligibleIntegrationInstanceConfigurationWorkflowsDocument = new TypedDocumentString(`
    query toolEligibleIntegrationInstanceConfigurationWorkflows($integrationInstanceConfigurationId: ID!) {
  toolEligibleIntegrationInstanceConfigurationWorkflows(
    integrationInstanceConfigurationId: $integrationInstanceConfigurationId
  ) {
    id
    integrationWorkflowId
    label
  }
}
    `);

export const useToolEligibleIntegrationInstanceConfigurationWorkflowsQuery = <
      TData = ToolEligibleIntegrationInstanceConfigurationWorkflowsQuery,
      TError = unknown
    >(
      variables: ToolEligibleIntegrationInstanceConfigurationWorkflowsQueryVariables,
      options?: Omit<UseQueryOptions<ToolEligibleIntegrationInstanceConfigurationWorkflowsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ToolEligibleIntegrationInstanceConfigurationWorkflowsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ToolEligibleIntegrationInstanceConfigurationWorkflowsQuery, TError, TData>(
      {
    queryKey: ['toolEligibleIntegrationInstanceConfigurationWorkflows', variables],
    queryFn: fetcher<ToolEligibleIntegrationInstanceConfigurationWorkflowsQuery, ToolEligibleIntegrationInstanceConfigurationWorkflowsQueryVariables>(ToolEligibleIntegrationInstanceConfigurationWorkflowsDocument, variables),
    ...options
  }
    )};

export const ToolEligibleIntegrationVersionWorkflowsDocument = new TypedDocumentString(`
    query toolEligibleIntegrationVersionWorkflows($integrationId: ID!, $integrationVersion: Int!) {
  toolEligibleIntegrationVersionWorkflows(
    integrationId: $integrationId
    integrationVersion: $integrationVersion
  ) {
    id
    integrationWorkflowId
    label
  }
}
    `);

export const useToolEligibleIntegrationVersionWorkflowsQuery = <
      TData = ToolEligibleIntegrationVersionWorkflowsQuery,
      TError = unknown
    >(
      variables: ToolEligibleIntegrationVersionWorkflowsQueryVariables,
      options?: Omit<UseQueryOptions<ToolEligibleIntegrationVersionWorkflowsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ToolEligibleIntegrationVersionWorkflowsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ToolEligibleIntegrationVersionWorkflowsQuery, TError, TData>(
      {
    queryKey: ['toolEligibleIntegrationVersionWorkflows', variables],
    queryFn: fetcher<ToolEligibleIntegrationVersionWorkflowsQuery, ToolEligibleIntegrationVersionWorkflowsQueryVariables>(ToolEligibleIntegrationVersionWorkflowsDocument, variables),
    ...options
  }
    )};

export const UpdateMcpIntegrationInstanceConfigurationDocument = new TypedDocumentString(`
    mutation updateMcpIntegrationInstanceConfiguration($id: ID!, $input: UpdateMcpIntegrationInstanceConfigurationInput!) {
  updateMcpIntegrationInstanceConfiguration(id: $id, input: $input) {
    id
    integrationInstanceConfigurationId
    mcpServerId
  }
}
    `);

export const useUpdateMcpIntegrationInstanceConfigurationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpIntegrationInstanceConfigurationMutation, TError, UpdateMcpIntegrationInstanceConfigurationMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpIntegrationInstanceConfigurationMutation, TError, UpdateMcpIntegrationInstanceConfigurationMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpIntegrationInstanceConfiguration'],
    mutationFn: (variables?: UpdateMcpIntegrationInstanceConfigurationMutationVariables) => fetcher<UpdateMcpIntegrationInstanceConfigurationMutation, UpdateMcpIntegrationInstanceConfigurationMutationVariables>(UpdateMcpIntegrationInstanceConfigurationDocument, variables)(),
    ...options
  }
    )};

export const UpdateMcpIntegrationInstanceConfigurationVersionDocument = new TypedDocumentString(`
    mutation updateMcpIntegrationInstanceConfigurationVersion($id: ID!, $input: UpdateMcpIntegrationInstanceConfigurationVersionInput!) {
  updateMcpIntegrationInstanceConfigurationVersion(id: $id, input: $input)
}
    `);

export const useUpdateMcpIntegrationInstanceConfigurationVersionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpIntegrationInstanceConfigurationVersionMutation, TError, UpdateMcpIntegrationInstanceConfigurationVersionMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpIntegrationInstanceConfigurationVersionMutation, TError, UpdateMcpIntegrationInstanceConfigurationVersionMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpIntegrationInstanceConfigurationVersion'],
    mutationFn: (variables?: UpdateMcpIntegrationInstanceConfigurationVersionMutationVariables) => fetcher<UpdateMcpIntegrationInstanceConfigurationVersionMutation, UpdateMcpIntegrationInstanceConfigurationVersionMutationVariables>(UpdateMcpIntegrationInstanceConfigurationVersionDocument, variables)(),
    ...options
  }
    )};

export const UpdateMcpIntegrationInstanceConfigurationWorkflowDocument = new TypedDocumentString(`
    mutation updateMcpIntegrationInstanceConfigurationWorkflow($id: ID!, $input: McpIntegrationInstanceConfigurationWorkflowUpdateInput!) {
  updateMcpIntegrationInstanceConfigurationWorkflow(id: $id, input: $input) {
    id
    mcpIntegrationInstanceConfigurationId
    integrationInstanceConfigurationWorkflowId
    parameters
  }
}
    `);

export const useUpdateMcpIntegrationInstanceConfigurationWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpIntegrationInstanceConfigurationWorkflowMutation, TError, UpdateMcpIntegrationInstanceConfigurationWorkflowMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpIntegrationInstanceConfigurationWorkflowMutation, TError, UpdateMcpIntegrationInstanceConfigurationWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpIntegrationInstanceConfigurationWorkflow'],
    mutationFn: (variables?: UpdateMcpIntegrationInstanceConfigurationWorkflowMutationVariables) => fetcher<UpdateMcpIntegrationInstanceConfigurationWorkflowMutation, UpdateMcpIntegrationInstanceConfigurationWorkflowMutationVariables>(UpdateMcpIntegrationInstanceConfigurationWorkflowDocument, variables)(),
    ...options
  }
    )};

export const ApiConnectorDocument = new TypedDocumentString(`
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
    `);

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

export const ApiConnectorsDocument = new TypedDocumentString(`
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
    `);

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

export const CancelGenerationJobDocument = new TypedDocumentString(`
    mutation cancelGenerationJob($jobId: String!) {
  cancelGenerationJob(jobId: $jobId)
}
    `);

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

export const CreateApiConnectorDocument = new TypedDocumentString(`
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
    `);

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

export const DeleteApiConnectorDocument = new TypedDocumentString(`
    mutation deleteApiConnector($id: ID!) {
  deleteApiConnector(id: $id)
}
    `);

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

export const EnableApiConnectorDocument = new TypedDocumentString(`
    mutation enableApiConnector($id: ID!, $enable: Boolean!) {
  enableApiConnector(id: $id, enable: $enable)
}
    `);

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

export const GenerateSpecificationDocument = new TypedDocumentString(`
    mutation generateSpecification($input: GenerateSpecificationInput!) {
  generateSpecification(input: $input) {
    specification
  }
}
    `);

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

export const GenerationJobStatusDocument = new TypedDocumentString(`
    query generationJobStatus($jobId: String!) {
  generationJobStatus(jobId: $jobId) {
    jobId
    status
    specification
    errorMessage
  }
}
    `);

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

export const ImportOpenApiSpecificationDocument = new TypedDocumentString(`
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
    `);

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

export const StartGenerateFromDocumentationPreviewDocument = new TypedDocumentString(`
    mutation startGenerateFromDocumentationPreview($input: GenerateFromDocumentationInput!) {
  startGenerateFromDocumentationPreview(input: $input) {
    jobId
    status
    specification
    errorMessage
  }
}
    `);

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

export const UpdateApiConnectorDocument = new TypedDocumentString(`
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
    `);

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

export const EditorJobFileLogsDocument = new TypedDocumentString(`
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
    `);

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

export const EditorJobFileLogsExistDocument = new TypedDocumentString(`
    query editorJobFileLogsExist($jobId: ID!) {
  editorJobFileLogsExist(jobId: $jobId)
}
    `);

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

export const EditorTaskExecutionFileLogsDocument = new TypedDocumentString(`
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
    `);

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

export const JobFileLogsDocument = new TypedDocumentString(`
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
    `);

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

export const JobFileLogsExistDocument = new TypedDocumentString(`
    query jobFileLogsExist($jobId: ID!) {
  jobFileLogsExist(jobId: $jobId)
}
    `);

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

export const TaskExecutionFileLogsDocument = new TypedDocumentString(`
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
    `);

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

export const AdminApiKeysDocument = new TypedDocumentString(`
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
    `);

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

export const ApiKeysDocument = new TypedDocumentString(`
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
    `);

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

export const ClusterElementComponentConnectionsDocument = new TypedDocumentString(`
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
    `);

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

export const ClusterElementDefinitionDocument = new TypedDocumentString(`
    query clusterElementDefinition($componentName: String!, $componentVersion: Int!, $clusterElementName: String!) {
  clusterElementDefinition(
    componentName: $componentName
    componentVersion: $componentVersion
    clusterElementName: $clusterElementName
  ) {
    componentName
    componentVersion
    description
    name
    title
    properties {
      advancedOption
      description
      displayCondition
      expressionEnabled
      hidden
      name
      required
      type
      ... on StringProperty {
        controlType
        defaultValue
        label
        placeholder
        options {
          description
          label
          value
        }
        optionsDataSource {
          optionsLookupDependsOn
        }
      }
      ... on IntegerProperty {
        controlType
        integerDefaultValue: defaultValue
        label
        placeholder
        options {
          description
          label
          value
        }
        optionsDataSource {
          optionsLookupDependsOn
        }
      }
      ... on NumberProperty {
        controlType
        label
        numberDefaultValue: defaultValue
        placeholder
        options {
          description
          label
          value
        }
        optionsDataSource {
          optionsLookupDependsOn
        }
      }
      ... on BooleanProperty {
        booleanDefaultValue: defaultValue
        controlType
        label
        placeholder
      }
      ... on ArrayProperty {
        arrayDefaultValue: defaultValue
        controlType
        label
        placeholder
        optionsDataSource {
          optionsLookupDependsOn
        }
        items {
          advancedOption
          description
          displayCondition
          expressionEnabled
          hidden
          name
          required
          type
          ... on StringProperty {
            controlType
            defaultValue
            label
            placeholder
            options {
              description
              label
              value
            }
            optionsDataSource {
              optionsLookupDependsOn
            }
          }
          ... on IntegerProperty {
            controlType
            integerDefaultValue: defaultValue
            label
            placeholder
            options {
              description
              label
              value
            }
            optionsDataSource {
              optionsLookupDependsOn
            }
          }
          ... on NumberProperty {
            controlType
            label
            numberDefaultValue: defaultValue
            placeholder
            options {
              description
              label
              value
            }
            optionsDataSource {
              optionsLookupDependsOn
            }
          }
          ... on BooleanProperty {
            booleanDefaultValue: defaultValue
            controlType
            label
            placeholder
          }
          ... on ArrayProperty {
            arrayDefaultValue: defaultValue
            controlType
            label
            placeholder
          }
          ... on ObjectProperty {
            controlType
            label
            objectDefaultValue: defaultValue
            placeholder
          }
          ... on DateProperty {
            controlType
            dateDefaultValue: defaultValue
            label
            placeholder
          }
          ... on DateTimeProperty {
            controlType
            dateTimeDefaultValue: defaultValue
            label
            placeholder
          }
          ... on TimeProperty {
            controlType
            label
            placeholder
            timeDefaultValue: defaultValue
          }
          ... on NullProperty {
            controlType
            label
            placeholder
          }
          ... on DynamicPropertiesProperty {
            propertiesDataSource {
              propertiesLookupDependsOn
            }
          }
        }
      }
      ... on ObjectProperty {
        controlType
        label
        objectDefaultValue: defaultValue
        placeholder
        optionsDataSource {
          optionsLookupDependsOn
        }
        properties {
          advancedOption
          description
          displayCondition
          expressionEnabled
          hidden
          name
          required
          type
          ... on StringProperty {
            controlType
            defaultValue
            label
            placeholder
            options {
              description
              label
              value
            }
            optionsDataSource {
              optionsLookupDependsOn
            }
          }
          ... on IntegerProperty {
            controlType
            integerDefaultValue: defaultValue
            label
            placeholder
            options {
              description
              label
              value
            }
            optionsDataSource {
              optionsLookupDependsOn
            }
          }
          ... on NumberProperty {
            controlType
            label
            numberDefaultValue: defaultValue
            placeholder
            options {
              description
              label
              value
            }
            optionsDataSource {
              optionsLookupDependsOn
            }
          }
          ... on BooleanProperty {
            booleanDefaultValue: defaultValue
            controlType
            label
            placeholder
          }
          ... on ArrayProperty {
            arrayDefaultValue: defaultValue
            controlType
            label
            placeholder
          }
          ... on ObjectProperty {
            controlType
            label
            objectDefaultValue: defaultValue
            placeholder
          }
          ... on DateProperty {
            controlType
            dateDefaultValue: defaultValue
            label
            placeholder
          }
          ... on DateTimeProperty {
            controlType
            dateTimeDefaultValue: defaultValue
            label
            placeholder
          }
          ... on TimeProperty {
            controlType
            label
            placeholder
            timeDefaultValue: defaultValue
          }
          ... on NullProperty {
            controlType
            label
            placeholder
          }
          ... on DynamicPropertiesProperty {
            propertiesDataSource {
              propertiesLookupDependsOn
            }
          }
        }
      }
      ... on DateProperty {
        controlType
        dateDefaultValue: defaultValue
        label
        placeholder
      }
      ... on DateTimeProperty {
        controlType
        dateTimeDefaultValue: defaultValue
        label
        placeholder
      }
      ... on TimeProperty {
        controlType
        label
        placeholder
        timeDefaultValue: defaultValue
      }
      ... on NullProperty {
        controlType
        label
        placeholder
      }
      ... on DynamicPropertiesProperty {
        propertiesDataSource {
          propertiesLookupDependsOn
        }
      }
    }
  }
}
    `);

export const useClusterElementDefinitionQuery = <
      TData = ClusterElementDefinitionQuery,
      TError = unknown
    >(
      variables: ClusterElementDefinitionQueryVariables,
      options?: Omit<UseQueryOptions<ClusterElementDefinitionQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ClusterElementDefinitionQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ClusterElementDefinitionQuery, TError, TData>(
      {
    queryKey: ['clusterElementDefinition', variables],
    queryFn: fetcher<ClusterElementDefinitionQuery, ClusterElementDefinitionQueryVariables>(ClusterElementDefinitionDocument, variables),
    ...options
  }
    )};

export const ClusterElementDynamicPropertiesDocument = new TypedDocumentString(`
    query clusterElementDynamicProperties($componentName: String!, $componentVersion: Int!, $clusterElementName: String!, $propertyName: String!, $connectionId: Long, $inputParameters: Map, $lookupDependsOnPaths: [String!]) {
  clusterElementDynamicProperties(
    componentName: $componentName
    componentVersion: $componentVersion
    clusterElementName: $clusterElementName
    propertyName: $propertyName
    connectionId: $connectionId
    inputParameters: $inputParameters
    lookupDependsOnPaths: $lookupDependsOnPaths
  ) {
    advancedOption
    description
    displayCondition
    expressionEnabled
    hidden
    name
    required
    type
    ... on StringProperty {
      controlType
      label
      placeholder
      options {
        description
        label
        value
      }
      optionsDataSource {
        optionsLookupDependsOn
      }
    }
    ... on IntegerProperty {
      controlType
      label
      placeholder
      options {
        description
        label
        value
      }
      optionsDataSource {
        optionsLookupDependsOn
      }
    }
    ... on NumberProperty {
      controlType
      label
      placeholder
      options {
        description
        label
        value
      }
      optionsDataSource {
        optionsLookupDependsOn
      }
    }
    ... on BooleanProperty {
      controlType
      label
      placeholder
    }
    ... on ArrayProperty {
      controlType
      label
      placeholder
      optionsDataSource {
        optionsLookupDependsOn
      }
      items {
        advancedOption
        description
        displayCondition
        expressionEnabled
        hidden
        name
        required
        type
        ... on StringProperty {
          controlType
          label
          placeholder
          options {
            description
            label
            value
          }
          optionsDataSource {
            optionsLookupDependsOn
          }
        }
        ... on IntegerProperty {
          controlType
          label
          placeholder
          options {
            description
            label
            value
          }
          optionsDataSource {
            optionsLookupDependsOn
          }
        }
        ... on NumberProperty {
          controlType
          label
          placeholder
          options {
            description
            label
            value
          }
          optionsDataSource {
            optionsLookupDependsOn
          }
        }
        ... on BooleanProperty {
          controlType
          label
          placeholder
        }
        ... on ArrayProperty {
          controlType
          label
          placeholder
        }
        ... on ObjectProperty {
          controlType
          label
          placeholder
        }
        ... on DateProperty {
          controlType
          label
          placeholder
        }
        ... on DateTimeProperty {
          controlType
          label
          placeholder
        }
        ... on TimeProperty {
          controlType
          label
          placeholder
        }
        ... on NullProperty {
          controlType
          label
          placeholder
        }
        ... on DynamicPropertiesProperty {
          propertiesDataSource {
            propertiesLookupDependsOn
          }
        }
      }
    }
    ... on ObjectProperty {
      controlType
      label
      placeholder
      optionsDataSource {
        optionsLookupDependsOn
      }
      properties {
        advancedOption
        description
        displayCondition
        expressionEnabled
        hidden
        name
        required
        type
        ... on StringProperty {
          controlType
          label
          placeholder
          options {
            description
            label
            value
          }
          optionsDataSource {
            optionsLookupDependsOn
          }
        }
        ... on IntegerProperty {
          controlType
          label
          placeholder
          options {
            description
            label
            value
          }
          optionsDataSource {
            optionsLookupDependsOn
          }
        }
        ... on NumberProperty {
          controlType
          label
          placeholder
          options {
            description
            label
            value
          }
          optionsDataSource {
            optionsLookupDependsOn
          }
        }
        ... on BooleanProperty {
          controlType
          label
          placeholder
        }
        ... on ArrayProperty {
          controlType
          label
          placeholder
        }
        ... on ObjectProperty {
          controlType
          label
          placeholder
        }
        ... on DateProperty {
          controlType
          label
          placeholder
        }
        ... on DateTimeProperty {
          controlType
          label
          placeholder
        }
        ... on TimeProperty {
          controlType
          label
          placeholder
        }
        ... on NullProperty {
          controlType
          label
          placeholder
        }
        ... on DynamicPropertiesProperty {
          propertiesDataSource {
            propertiesLookupDependsOn
          }
        }
      }
    }
    ... on DateProperty {
      controlType
      label
      placeholder
    }
    ... on DateTimeProperty {
      controlType
      label
      placeholder
    }
    ... on TimeProperty {
      controlType
      label
      placeholder
    }
    ... on NullProperty {
      controlType
      label
      placeholder
    }
    ... on DynamicPropertiesProperty {
      propertiesDataSource {
        propertiesLookupDependsOn
      }
    }
  }
}
    `);

export const useClusterElementDynamicPropertiesQuery = <
      TData = ClusterElementDynamicPropertiesQuery,
      TError = unknown
    >(
      variables: ClusterElementDynamicPropertiesQueryVariables,
      options?: Omit<UseQueryOptions<ClusterElementDynamicPropertiesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ClusterElementDynamicPropertiesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ClusterElementDynamicPropertiesQuery, TError, TData>(
      {
    queryKey: ['clusterElementDynamicProperties', variables],
    queryFn: fetcher<ClusterElementDynamicPropertiesQuery, ClusterElementDynamicPropertiesQueryVariables>(ClusterElementDynamicPropertiesDocument, variables),
    ...options
  }
    )};

export const ClusterElementOptionsDocument = new TypedDocumentString(`
    query clusterElementOptions($componentName: String!, $componentVersion: Int!, $clusterElementName: String!, $propertyName: String!, $connectionId: Long, $inputParameters: Map, $lookupDependsOnPaths: [String!]) {
  clusterElementOptions(
    componentName: $componentName
    componentVersion: $componentVersion
    clusterElementName: $clusterElementName
    propertyName: $propertyName
    connectionId: $connectionId
    inputParameters: $inputParameters
    lookupDependsOnPaths: $lookupDependsOnPaths
  ) {
    description
    label
    value
  }
}
    `);

export const useClusterElementOptionsQuery = <
      TData = ClusterElementOptionsQuery,
      TError = unknown
    >(
      variables: ClusterElementOptionsQueryVariables,
      options?: Omit<UseQueryOptions<ClusterElementOptionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ClusterElementOptionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ClusterElementOptionsQuery, TError, TData>(
      {
    queryKey: ['clusterElementOptions', variables],
    queryFn: fetcher<ClusterElementOptionsQuery, ClusterElementOptionsQueryVariables>(ClusterElementOptionsDocument, variables),
    ...options
  }
    )};

export const ClusterElementScriptInputDocument = new TypedDocumentString(`
    query clusterElementScriptInput($workflowId: String!, $workflowNodeName: String!, $clusterElementType: String!, $clusterElementWorkflowNodeName: String!, $environmentId: Long!) {
  clusterElementScriptInput(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    clusterElementType: $clusterElementType
    clusterElementWorkflowNodeName: $clusterElementWorkflowNodeName
    environmentId: $environmentId
  )
}
    `);

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

export const ComponentDefinitionSearchDocument = new TypedDocumentString(`
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
    `);

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

export const CreateApiKeyDocument = new TypedDocumentString(`
    mutation createApiKey($name: String!, $environmentId: ID!, $type: PlatformType) {
  createApiKey(name: $name, environmentId: $environmentId, type: $type)
}
    `);

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

export const CreateMcpComponentDocument = new TypedDocumentString(`
    mutation createMcpComponent($input: McpComponentInput!) {
  createMcpComponent(input: $input) {
    id
    componentName
    componentVersion
    title
    mcpServerId
    connectionId
  }
}
    `);

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

export const CreateMcpComponentWithToolsDocument = new TypedDocumentString(`
    mutation createMcpComponentWithTools($input: McpComponentWithToolsInput!) {
  createMcpComponentWithTools(input: $input) {
    id
    componentName
    componentVersion
    title
    mcpServerId
    connectionId
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `);

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

export const CreateMcpToolDocument = new TypedDocumentString(`
    mutation createMcpTool($input: McpToolInput!) {
  createMcpTool(input: $input) {
    id
    name
    mcpComponentId
    parameters
  }
}
    `);

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

export const DeleteApiKeyDocument = new TypedDocumentString(`
    mutation deleteApiKey($id: ID!) {
  deleteApiKey(id: $id)
}
    `);

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

export const DeleteMcpComponentDocument = new TypedDocumentString(`
    mutation deleteMcpComponent($id: ID!) {
  deleteMcpComponent(id: $id)
}
    `);

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

export const DeleteMcpToolDocument = new TypedDocumentString(`
    mutation deleteMcpTool($id: ID!) {
  deleteMcpTool(id: $id)
}
    `);

export const useDeleteMcpToolMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteMcpToolMutation, TError, DeleteMcpToolMutationVariables, TContext>) => {
    
    return useMutation<DeleteMcpToolMutation, TError, DeleteMcpToolMutationVariables, TContext>(
      {
    mutationKey: ['deleteMcpTool'],
    mutationFn: (variables?: DeleteMcpToolMutationVariables) => fetcher<DeleteMcpToolMutation, DeleteMcpToolMutationVariables>(DeleteMcpToolDocument, variables)(),
    ...options
  }
    )};

export const EnvironmentsDocument = new TypedDocumentString(`
    query environments {
  environments {
    id
    name
  }
}
    `);

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

export const ManagementMcpServerUrlDocument = new TypedDocumentString(`
    query managementMcpServerUrl {
  managementMcpServerUrl
}
    `);

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

export const McpComponentsByServerIdDocument = new TypedDocumentString(`
    query mcpComponentsByServerId($mcpServerId: ID!) {
  mcpComponentsByServerId(mcpServerId: $mcpServerId) {
    id
    componentName
    componentVersion
    title
    connectionId
    lastModifiedDate
    mcpServerId
    mcpTools {
      id
      mcpComponentId
      name
      parameters
      title
      version
    }
    version
  }
}
    `);

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

export const McpServerTagsDocument = new TypedDocumentString(`
    query mcpServerTags($type: PlatformType!) {
  mcpServerTags(type: $type) {
    id
    name
  }
}
    `);

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

export const McpServersDocument = new TypedDocumentString(`
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
      title
    }
    tags {
      id
      name
    }
    lastModifiedDate
  }
}
    `);

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

export const McpToolsByComponentIdDocument = new TypedDocumentString(`
    query mcpToolsByComponentId($mcpComponentId: ID!) {
  mcpToolsByComponentId(mcpComponentId: $mcpComponentId) {
    id
    name
    title
    mcpComponentId
    parameters
    version
  }
}
    `);

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

export const SaveClusterElementTestConfigurationConnectionDocument = new TypedDocumentString(`
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
    `);

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

export const SaveClusterElementTestOutputDocument = new TypedDocumentString(`
    mutation saveClusterElementTestOutput($workflowId: String!, $workflowNodeName: String!, $clusterElementType: String!, $clusterElementWorkflowNodeName: String!, $environmentId: Long!, $inputParameters: Map) {
  saveClusterElementTestOutput(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    clusterElementType: $clusterElementType
    clusterElementWorkflowNodeName: $clusterElementWorkflowNodeName
    environmentId: $environmentId
    inputParameters: $inputParameters
  ) {
    id
    workflowId
    workflowNodeName
  }
}
    `);

export const useSaveClusterElementTestOutputMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SaveClusterElementTestOutputMutation, TError, SaveClusterElementTestOutputMutationVariables, TContext>) => {
    
    return useMutation<SaveClusterElementTestOutputMutation, TError, SaveClusterElementTestOutputMutationVariables, TContext>(
      {
    mutationKey: ['saveClusterElementTestOutput'],
    mutationFn: (variables?: SaveClusterElementTestOutputMutationVariables) => fetcher<SaveClusterElementTestOutputMutation, SaveClusterElementTestOutputMutationVariables>(SaveClusterElementTestOutputDocument, variables)(),
    ...options
  }
    )};

export const SaveWorkflowTestConfigurationConnectionDocument = new TypedDocumentString(`
    mutation saveWorkflowTestConfigurationConnection($workflowId: String!, $workflowNodeName: String!, $workflowConnectionKey: String!, $connectionId: Long!, $environmentId: Long!) {
  saveWorkflowTestConfigurationConnection(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    workflowConnectionKey: $workflowConnectionKey
    connectionId: $connectionId
    environmentId: $environmentId
  )
}
    `);

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

export const TestClusterElementScriptDocument = new TypedDocumentString(`
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
    `);

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

export const TestWorkflowNodeScriptDocument = new TypedDocumentString(`
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
    `);

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

export const UpdateApiKeyDocument = new TypedDocumentString(`
    mutation updateApiKey($id: ID!, $name: String!) {
  updateApiKey(id: $id, name: $name)
}
    `);

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

export const UpdateManagementMcpServerUrlDocument = new TypedDocumentString(`
    mutation updateManagementMcpServerUrl {
  updateManagementMcpServerUrl
}
    `);

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

export const UpdateMcpComponentWithToolsDocument = new TypedDocumentString(`
    mutation updateMcpComponentWithTools($id: ID!, $input: McpComponentWithToolsInput!) {
  updateMcpComponentWithTools(id: $id, input: $input) {
    id
    componentName
    componentVersion
    title
    mcpServerId
    connectionId
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `);

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

export const UpdateMcpServerUrlDocument = new TypedDocumentString(`
    mutation updateMcpServerUrl($id: ID!) {
  updateMcpServerUrl(id: $id)
}
    `);

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

export const UpdateMcpToolDocument = new TypedDocumentString(`
    mutation updateMcpTool($id: ID!, $input: McpToolInput!) {
  updateMcpTool(id: $id, input: $input) {
    id
    name
    mcpComponentId
    parameters
    version
  }
}
    `);

export const useUpdateMcpToolMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpToolMutation, TError, UpdateMcpToolMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpToolMutation, TError, UpdateMcpToolMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpTool'],
    mutationFn: (variables?: UpdateMcpToolMutationVariables) => fetcher<UpdateMcpToolMutation, UpdateMcpToolMutationVariables>(UpdateMcpToolDocument, variables)(),
    ...options
  }
    )};

export const WorkflowNodeComponentConnectionsDocument = new TypedDocumentString(`
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
    `);

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

export const WorkflowNodeScriptInputDocument = new TypedDocumentString(`
    query workflowNodeScriptInput($workflowId: String!, $workflowNodeName: String!, $environmentId: Long!) {
  workflowNodeScriptInput(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    environmentId: $environmentId
  )
}
    `);

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

export const CustomComponentDocument = new TypedDocumentString(`
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
    `);

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

export const CustomComponentDefinitionDocument = new TypedDocumentString(`
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
    `);

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

export const CustomComponentsDocument = new TypedDocumentString(`
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
    `);

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

export const DeleteCustomComponentDocument = new TypedDocumentString(`
    mutation deleteCustomComponent($id: ID!) {
  deleteCustomComponent(id: $id)
}
    `);

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

export const EnableCustomComponentDocument = new TypedDocumentString(`
    mutation enableCustomComponent($id: ID!, $enable: Boolean!) {
  enableCustomComponent(id: $id, enable: $enable)
}
    `);

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

export const AuthoritiesDocument = new TypedDocumentString(`
    query authorities {
  authorities
}
    `);

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

export const CreateIdentityProviderDocument = new TypedDocumentString(`
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
    `);

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

export const DeleteIdentityProviderDocument = new TypedDocumentString(`
    mutation deleteIdentityProvider($id: ID!) {
  deleteIdentityProvider(id: $id)
}
    `);

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

export const DeleteUserDocument = new TypedDocumentString(`
    mutation deleteUser($login: String!) {
  deleteUser(login: $login)
}
    `);

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

export const IdentityProviderDocument = new TypedDocumentString(`
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
    `);

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

export const IdentityProvidersDocument = new TypedDocumentString(`
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
    `);

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

export const InviteUserDocument = new TypedDocumentString(`
    mutation inviteUser($email: String!, $password: String!, $role: String!) {
  inviteUser(email: $email, password: $password, role: $role)
}
    `);

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

export const UpdateIdentityProviderDocument = new TypedDocumentString(`
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
    `);

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

export const UpdateUserDocument = new TypedDocumentString(`
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
    `);

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

export const UsersDocument = new TypedDocumentString(`
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
    `);

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
