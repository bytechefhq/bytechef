export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
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

export type AiAgentEvalResult = {
  __typename?: 'AiAgentEvalResult';
  createdDate?: Maybe<Scalars['Long']['output']>;
  errorMessage?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  inputTokens?: Maybe<Scalars['Int']['output']>;
  outputTokens?: Maybe<Scalars['Int']['output']>;
  runIndex?: Maybe<Scalars['Int']['output']>;
  scenario: AiAgentEvalScenario;
  score?: Maybe<Scalars['Float']['output']>;
  status: AiAgentEvalResultStatus;
  transcriptFile?: Maybe<Scalars['String']['output']>;
  verdicts: Array<AiAgentJudgeVerdict>;
};

export enum AiAgentEvalResultStatus {
  Completed = 'COMPLETED',
  Failed = 'FAILED',
  Pending = 'PENDING',
  Running = 'RUNNING'
}

export type AiAgentEvalRun = {
  __typename?: 'AiAgentEvalRun';
  agentVersion?: Maybe<Scalars['String']['output']>;
  averageScore?: Maybe<Scalars['Float']['output']>;
  completedDate?: Maybe<Scalars['Long']['output']>;
  completedScenarios: Scalars['Int']['output'];
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  results: Array<AiAgentEvalResult>;
  startedDate?: Maybe<Scalars['Long']['output']>;
  status: AiAgentEvalRunStatus;
  totalInputTokens?: Maybe<Scalars['Int']['output']>;
  totalOutputTokens?: Maybe<Scalars['Int']['output']>;
  totalScenarios: Scalars['Int']['output'];
};

export enum AiAgentEvalRunStatus {
  Completed = 'COMPLETED',
  Failed = 'FAILED',
  Pending = 'PENDING',
  Running = 'RUNNING'
}

export type AiAgentEvalScenario = {
  __typename?: 'AiAgentEvalScenario';
  createdDate?: Maybe<Scalars['Long']['output']>;
  expectedOutput?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  judges: Array<AiAgentScenarioJudge>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  maxTurns?: Maybe<Scalars['Int']['output']>;
  name: Scalars['String']['output'];
  numberOfRuns?: Maybe<Scalars['Int']['output']>;
  personaPrompt?: Maybe<Scalars['String']['output']>;
  toolSimulations: Array<AiAgentScenarioToolSimulation>;
  type: AiAgentScenarioType;
  userMessage?: Maybe<Scalars['String']['output']>;
};

export type AiAgentEvalTest = {
  __typename?: 'AiAgentEvalTest';
  createdDate?: Maybe<Scalars['Long']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  scenarios: Array<AiAgentEvalScenario>;
};

export type AiAgentJudge = {
  __typename?: 'AiAgentJudge';
  configuration: Scalars['Map']['output'];
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  type: AiAgentJudgeType;
};

export enum AiAgentJudgeScope {
  Agent = 'AGENT',
  Scenario = 'SCENARIO'
}

export enum AiAgentJudgeType {
  ContainsText = 'CONTAINS_TEXT',
  JsonSchema = 'JSON_SCHEMA',
  LlmRule = 'LLM_RULE',
  RegexMatch = 'REGEX_MATCH',
  ResponseLength = 'RESPONSE_LENGTH',
  Similarity = 'SIMILARITY',
  StringEquals = 'STRING_EQUALS',
  ToolUsage = 'TOOL_USAGE'
}

export type AiAgentJudgeVerdict = {
  __typename?: 'AiAgentJudgeVerdict';
  explanation: Scalars['String']['output'];
  id: Scalars['ID']['output'];
  judgeName: Scalars['String']['output'];
  judgeScope: AiAgentJudgeScope;
  judgeType: AiAgentJudgeType;
  passed: Scalars['Boolean']['output'];
  score: Scalars['Float']['output'];
};

export type AiAgentScenarioJudge = {
  __typename?: 'AiAgentScenarioJudge';
  configuration: Scalars['Map']['output'];
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  type: AiAgentJudgeType;
};

export type AiAgentScenarioToolSimulation = {
  __typename?: 'AiAgentScenarioToolSimulation';
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  responsePrompt: Scalars['String']['output'];
  simulationModel?: Maybe<Scalars['String']['output']>;
  toolName: Scalars['String']['output'];
};

export enum AiAgentScenarioType {
  MultiTurn = 'MULTI_TURN',
  SingleTurn = 'SINGLE_TURN'
}

export type AiSkill = {
  __typename?: 'AiSkill';
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
  assigneeId?: Maybe<Scalars['ID']['output']>;
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['String']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  dueDate?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  jobResumeId?: Maybe<Scalars['String']['output']>;
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  priority: ApprovalTaskPriority;
  status: ApprovalTaskStatus;
  version: Scalars['Int']['output'];
};

export type ApprovalTaskInput = {
  assigneeId?: InputMaybe<Scalars['ID']['input']>;
  description?: InputMaybe<Scalars['String']['input']>;
  dueDate?: InputMaybe<Scalars['String']['input']>;
  id?: InputMaybe<Scalars['ID']['input']>;
  name: Scalars['String']['input'];
  priority?: InputMaybe<ApprovalTaskPriority>;
  status?: InputMaybe<ApprovalTaskStatus>;
  version?: InputMaybe<Scalars['Int']['input']>;
};

export enum ApprovalTaskPriority {
  High = 'HIGH',
  Low = 'LOW',
  Medium = 'MEDIUM'
}

export enum ApprovalTaskStatus {
  Completed = 'COMPLETED',
  InProgress = 'IN_PROGRESS',
  Open = 'OPEN'
}

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

export type AutomationWorkflowProject = {
  __typename?: 'AutomationWorkflowProject';
  categoryId?: Maybe<Scalars['ID']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  lastPublishedVersion?: Maybe<Scalars['Int']['output']>;
  name: Scalars['String']['output'];
  published: Scalars['Boolean']['output'];
  tagIds: Array<Scalars['ID']['output']>;
  version: Scalars['Int']['output'];
  workflowTemplates: Array<AutomationWorkflowProjectWorkflowTemplate>;
};

export type AutomationWorkflowProjectCategory = {
  __typename?: 'AutomationWorkflowProjectCategory';
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
};

export type AutomationWorkflowProjectComponent = {
  __typename?: 'AutomationWorkflowProjectComponent';
  icon?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  title?: Maybe<Scalars['String']['output']>;
};

export type AutomationWorkflowProjectTag = {
  __typename?: 'AutomationWorkflowProjectTag';
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
};

export type AutomationWorkflowProjectVersion = {
  __typename?: 'AutomationWorkflowProjectVersion';
  publishedDate?: Maybe<Scalars['String']['output']>;
  status: Scalars['String']['output'];
  version: Scalars['Int']['output'];
};

export type AutomationWorkflowProjectWorkflowTemplate = {
  __typename?: 'AutomationWorkflowProjectWorkflowTemplate';
  components: Array<AutomationWorkflowProjectComponent>;
  description?: Maybe<Scalars['String']['output']>;
  label?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['String']['output']>;
  triggers: Array<AutomationWorkflowProjectComponent>;
  workflowUuid: Scalars['ID']['output'];
};

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

export type ChatWorkflow = {
  __typename?: 'ChatWorkflow';
  projectDeploymentId: Scalars['ID']['output'];
  projectId: Scalars['ID']['output'];
  projectName: Scalars['String']['output'];
  workflowExecutionId: Scalars['String']['output'];
  workflowLabel: Scalars['String']['output'];
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

export type ConnectedUserMcpServer = {
  __typename?: 'ConnectedUserMcpServer';
  enabled: Scalars['Boolean']['output'];
  environmentId: Scalars['ID']['output'];
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  tools: Array<ConnectedUserMcpServerTool>;
};

export type ConnectedUserMcpServerTool = {
  __typename?: 'ConnectedUserMcpServerTool';
  componentName: Scalars['String']['output'];
  componentVersion: Scalars['Int']['output'];
  enabled: Scalars['Boolean']['output'];
  id: Scalars['ID']['output'];
  integrationInstanceId: Scalars['ID']['output'];
  name: Scalars['String']['output'];
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
  workflowVersion?: Maybe<Scalars['Int']['output']>;
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

export enum EvaluatorFunctionCategory {
  Collection = 'COLLECTION',
  DateTime = 'DATE_TIME',
  Map = 'MAP',
  String = 'STRING',
  Type = 'TYPE',
  Utility = 'UTILITY'
}

export type EvaluatorFunctionDefinition = {
  __typename?: 'EvaluatorFunctionDefinition';
  category: EvaluatorFunctionCategory;
  description: Scalars['String']['output'];
  example: Scalars['String']['output'];
  name: Scalars['String']['output'];
  parameters: Array<EvaluatorFunctionParameter>;
  returnType: EvaluatorFunctionType;
  title: Scalars['String']['output'];
};

export type EvaluatorFunctionParameter = {
  __typename?: 'EvaluatorFunctionParameter';
  description: Scalars['String']['output'];
  name: Scalars['String']['output'];
  required: Scalars['Boolean']['output'];
  type: EvaluatorFunctionType;
};

export enum EvaluatorFunctionType {
  Boolean = 'BOOLEAN',
  Byte = 'BYTE',
  Character = 'CHARACTER',
  Datetime = 'DATETIME',
  Double = 'DOUBLE',
  Float = 'FLOAT',
  Integer = 'INTEGER',
  List = 'LIST',
  Long = 'LONG',
  Map = 'MAP',
  Number = 'NUMBER',
  Short = 'SHORT',
  String = 'STRING'
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
  tags?: Maybe<Array<Scalars['String']['output']>>;
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
  tags: Array<Scalars['String']['output']>;
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
  cancelAiAgentEvalRun: AiAgentEvalRun;
  cancelGenerationJob: Scalars['Boolean']['output'];
  createAiAgentEvalScenario: AiAgentEvalScenario;
  createAiAgentEvalTest: AiAgentEvalTest;
  createAiAgentJudge: AiAgentJudge;
  createAiAgentScenarioJudge: AiAgentScenarioJudge;
  createAiAgentScenarioToolSimulation: AiAgentScenarioToolSimulation;
  createAiSkill: AiSkill;
  createAiSkillFromInstructions: AiSkill;
  createApiConnector: ApiConnector;
  createApiKey: Scalars['String']['output'];
  createApprovalTask?: Maybe<ApprovalTask>;
  createAutomationWorkflowProject: Scalars['ID']['output'];
  createAutomationWorkflowProjectWorkflow: Scalars['ID']['output'];
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
  deleteAiAgentEvalScenario: Scalars['Boolean']['output'];
  deleteAiAgentEvalTest: Scalars['Boolean']['output'];
  deleteAiAgentJudge: Scalars['Boolean']['output'];
  deleteAiAgentScenarioJudge: Scalars['Boolean']['output'];
  deleteAiAgentScenarioToolSimulation: Scalars['Boolean']['output'];
  deleteAiSkill: Scalars['Boolean']['output'];
  deleteApiConnector: Scalars['Boolean']['output'];
  deleteApiKey: Scalars['Boolean']['output'];
  deleteApprovalTask?: Maybe<Scalars['Boolean']['output']>;
  deleteAutomationWorkflowProject: Scalars['Boolean']['output'];
  deleteAutomationWorkflowProjectWorkflow: Scalars['Boolean']['output'];
  deleteConnectedUserMcpServer?: Maybe<Scalars['Boolean']['output']>;
  deleteConnectedUserProjectWorkflow?: Maybe<Scalars['Boolean']['output']>;
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
  duplicateAutomationWorkflowProject: Scalars['ID']['output'];
  duplicateAutomationWorkflowProjectWorkflow: Scalars['ID']['output'];
  duplicateDataTable: Scalars['Boolean']['output'];
  enableApiConnector: Scalars['Boolean']['output'];
  enableConnectedUserMcpServer?: Maybe<Scalars['Boolean']['output']>;
  enableConnectedUserMcpTool?: Maybe<Scalars['Boolean']['output']>;
  enableConnectedUserProjectWorkflow?: Maybe<Scalars['Boolean']['output']>;
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
  publishAutomationWorkflowProject: Scalars['Boolean']['output'];
  removeDataTableColumn: Scalars['Boolean']['output'];
  renameDataTable: Scalars['Boolean']['output'];
  renameDataTableColumn: Scalars['Boolean']['output'];
  saveClusterElementTestConfigurationConnection?: Maybe<Scalars['Boolean']['output']>;
  saveClusterElementTestOutput?: Maybe<WorkflowNodeTestOutputResult>;
  saveWorkflowTestConfigurationConnection?: Maybe<Scalars['Boolean']['output']>;
  startAiAgentEvalRun: AiAgentEvalRun;
  startDiscoverEndpoints: EndpointDiscoveryResult;
  startGenerateForEndpoints: GenerationJobStatus;
  startGenerateFromDocumentationPreview: GenerationJobStatus;
  testClusterElementScript: ScriptTestExecution;
  testWorkflowNodeScript: ScriptTestExecution;
  updateAiAgentEvalScenario: AiAgentEvalScenario;
  updateAiAgentEvalTest: AiAgentEvalTest;
  updateAiAgentJudge: AiAgentJudge;
  updateAiAgentScenarioJudge: AiAgentScenarioJudge;
  updateAiAgentScenarioToolSimulation: AiAgentScenarioToolSimulation;
  updateAiSkill: AiSkill;
  updateAiSkillContent: AiSkill;
  updateApiConnector: ApiConnector;
  updateApiKey: Scalars['Boolean']['output'];
  updateApprovalTask?: Maybe<ApprovalTask>;
  updateAutomationWorkflowProject: Scalars['Boolean']['output'];
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


export type MutationCancelAiAgentEvalRunArgs = {
  id: Scalars['ID']['input'];
};


export type MutationCancelGenerationJobArgs = {
  jobId: Scalars['String']['input'];
};


export type MutationCreateAiAgentEvalScenarioArgs = {
  agentEvalTestId: Scalars['ID']['input'];
  expectedOutput?: InputMaybe<Scalars['String']['input']>;
  maxTurns?: InputMaybe<Scalars['Int']['input']>;
  name: Scalars['String']['input'];
  numberOfRuns?: InputMaybe<Scalars['Int']['input']>;
  personaPrompt?: InputMaybe<Scalars['String']['input']>;
  type: AiAgentScenarioType;
  userMessage?: InputMaybe<Scalars['String']['input']>;
};


export type MutationCreateAiAgentEvalTestArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
};


export type MutationCreateAiAgentJudgeArgs = {
  configuration: Scalars['Map']['input'];
  name: Scalars['String']['input'];
  type: AiAgentJudgeType;
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
};


export type MutationCreateAiAgentScenarioJudgeArgs = {
  agentEvalScenarioId: Scalars['ID']['input'];
  configuration: Scalars['Map']['input'];
  name: Scalars['String']['input'];
  type: AiAgentJudgeType;
};


export type MutationCreateAiAgentScenarioToolSimulationArgs = {
  agentEvalScenarioId: Scalars['ID']['input'];
  responsePrompt: Scalars['String']['input'];
  simulationModel?: InputMaybe<Scalars['String']['input']>;
  toolName: Scalars['String']['input'];
};


export type MutationCreateAiSkillArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  fileBytes: Scalars['String']['input'];
  filename: Scalars['String']['input'];
  name: Scalars['String']['input'];
};


export type MutationCreateAiSkillFromInstructionsArgs = {
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


export type MutationCreateAutomationWorkflowProjectArgs = {
  category?: InputMaybe<Scalars['String']['input']>;
  description?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  tags?: InputMaybe<Array<Scalars['String']['input']>>;
};


export type MutationCreateAutomationWorkflowProjectWorkflowArgs = {
  definition?: InputMaybe<Scalars['String']['input']>;
  projectId: Scalars['ID']['input'];
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


export type MutationDeleteAiAgentEvalScenarioArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiAgentEvalTestArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiAgentJudgeArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiAgentScenarioJudgeArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiAgentScenarioToolSimulationArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiSkillArgs = {
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


export type MutationDeleteAutomationWorkflowProjectArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAutomationWorkflowProjectWorkflowArgs = {
  workflowUuid: Scalars['ID']['input'];
};


export type MutationDeleteConnectedUserMcpServerArgs = {
  connectedUserId: Scalars['ID']['input'];
  mcpServerId: Scalars['ID']['input'];
};


export type MutationDeleteConnectedUserProjectWorkflowArgs = {
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


export type MutationDuplicateAutomationWorkflowProjectArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDuplicateAutomationWorkflowProjectWorkflowArgs = {
  workflowUuid: Scalars['ID']['input'];
};


export type MutationDuplicateDataTableArgs = {
  input: DuplicateDataTableInput;
};


export type MutationEnableApiConnectorArgs = {
  enable: Scalars['Boolean']['input'];
  id: Scalars['ID']['input'];
};


export type MutationEnableConnectedUserMcpServerArgs = {
  connectedUserId: Scalars['ID']['input'];
  enable: Scalars['Boolean']['input'];
  mcpServerId: Scalars['ID']['input'];
};


export type MutationEnableConnectedUserMcpToolArgs = {
  enable: Scalars['Boolean']['input'];
  id: Scalars['ID']['input'];
};


export type MutationEnableConnectedUserProjectWorkflowArgs = {
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


export type MutationPublishAutomationWorkflowProjectArgs = {
  id: Scalars['ID']['input'];
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


export type MutationStartAiAgentEvalRunArgs = {
  agentEvalTestId: Scalars['ID']['input'];
  aiAgentJudgeIds?: InputMaybe<Array<Scalars['ID']['input']>>;
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


export type MutationUpdateAiAgentEvalScenarioArgs = {
  expectedOutput?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  maxTurns?: InputMaybe<Scalars['Int']['input']>;
  name?: InputMaybe<Scalars['String']['input']>;
  numberOfRuns?: InputMaybe<Scalars['Int']['input']>;
  personaPrompt?: InputMaybe<Scalars['String']['input']>;
  userMessage?: InputMaybe<Scalars['String']['input']>;
};


export type MutationUpdateAiAgentEvalTestArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
};


export type MutationUpdateAiAgentJudgeArgs = {
  configuration?: InputMaybe<Scalars['Map']['input']>;
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
};


export type MutationUpdateAiAgentScenarioJudgeArgs = {
  configuration?: InputMaybe<Scalars['Map']['input']>;
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
};


export type MutationUpdateAiAgentScenarioToolSimulationArgs = {
  id: Scalars['ID']['input'];
  responsePrompt?: InputMaybe<Scalars['String']['input']>;
  simulationModel?: InputMaybe<Scalars['String']['input']>;
  toolName?: InputMaybe<Scalars['String']['input']>;
};


export type MutationUpdateAiSkillArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  name: Scalars['String']['input'];
};


export type MutationUpdateAiSkillContentArgs = {
  content: Scalars['String']['input'];
  id: Scalars['ID']['input'];
  path?: InputMaybe<Scalars['String']['input']>;
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


export type MutationUpdateAutomationWorkflowProjectArgs = {
  category?: InputMaybe<Scalars['String']['input']>;
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  tags?: InputMaybe<Array<Scalars['String']['input']>>;
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
  aiAgentEvalResult?: Maybe<AiAgentEvalResult>;
  aiAgentEvalResultTranscript?: Maybe<Scalars['String']['output']>;
  aiAgentEvalRun?: Maybe<AiAgentEvalRun>;
  aiAgentEvalRuns: Array<AiAgentEvalRun>;
  aiAgentEvalTest?: Maybe<AiAgentEvalTest>;
  aiAgentEvalTests: Array<AiAgentEvalTest>;
  aiAgentJudges: Array<AiAgentJudge>;
  aiSkill: AiSkill;
  aiSkillFileContent: Scalars['String']['output'];
  aiSkillFilePaths: Array<Scalars['String']['output']>;
  aiSkills: Array<AiSkill>;
  apiConnector?: Maybe<ApiConnector>;
  apiConnectors: Array<ApiConnector>;
  apiKey?: Maybe<ApiKey>;
  apiKeys?: Maybe<Array<Maybe<ApiKey>>>;
  approvalTask?: Maybe<ApprovalTask>;
  approvalTasks?: Maybe<Array<Maybe<ApprovalTask>>>;
  approvalTasksByIds?: Maybe<Array<Maybe<ApprovalTask>>>;
  authorities: Array<Scalars['String']['output']>;
  automationSearch: Array<SearchResult>;
  automationWorkflowProjectCategories: Array<AutomationWorkflowProjectCategory>;
  automationWorkflowProjectTags: Array<AutomationWorkflowProjectTag>;
  automationWorkflowProjectVersions: Array<AutomationWorkflowProjectVersion>;
  automationWorkflowProjects: Array<AutomationWorkflowProject>;
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
  connectedUserMcpServers: Array<ConnectedUserMcpServer>;
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
  evaluatorFunctionDefinition: EvaluatorFunctionDefinition;
  evaluatorFunctionDefinitions: Array<EvaluatorFunctionDefinition>;
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
  knowledgeBaseDocumentTags?: Maybe<Array<Scalars['String']['output']>>;
  knowledgeBaseDocumentTagsByDocument?: Maybe<Array<KnowledgeBaseDocumentTagsEntry>>;
  knowledgeBaseEmbeddingActive: Scalars['Boolean']['output'];
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
  validateWorkflow: WorkflowValidationResult;
  validateWorkflowById: WorkflowValidationResult;
  workflowNodeComponentConnections: Array<ComponentConnection>;
  workflowNodeMissingRequiredProperties: Array<Scalars['String']['output']>;
  workflowNodeScriptInput?: Maybe<Scalars['Map']['output']>;
  workflowTemplate?: Maybe<WorkflowTemplate>;
  workspaceApiKeys: Array<ApiKey>;
  workspaceChatWorkflows: Array<ChatWorkflow>;
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


export type QueryAiAgentEvalResultArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiAgentEvalResultTranscriptArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiAgentEvalRunArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiAgentEvalRunsArgs = {
  agentEvalTestId: Scalars['ID']['input'];
  limit?: InputMaybe<Scalars['Int']['input']>;
  offset?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryAiAgentEvalTestArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiAgentEvalTestsArgs = {
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
};


export type QueryAiAgentJudgesArgs = {
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
};


export type QueryAiSkillArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiSkillFileContentArgs = {
  id: Scalars['ID']['input'];
  path: Scalars['String']['input'];
};


export type QueryAiSkillFilePathsArgs = {
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


export type QueryApprovalTasksArgs = {
  environmentId?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryApprovalTasksByIdsArgs = {
  ids: Array<Scalars['ID']['input']>;
};


export type QueryAutomationSearchArgs = {
  limit?: InputMaybe<Scalars['Int']['input']>;
  query: Scalars['String']['input'];
};


export type QueryAutomationWorkflowProjectVersionsArgs = {
  id: Scalars['ID']['input'];
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


export type QueryConnectedUserMcpServersArgs = {
  connectedUserId: Scalars['ID']['input'];
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


export type QueryEvaluatorFunctionDefinitionArgs = {
  name: Scalars['String']['input'];
};


export type QueryEvaluatorFunctionDefinitionsArgs = {
  name?: InputMaybe<Scalars['String']['input']>;
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


export type QueryKnowledgeBaseEmbeddingActiveArgs = {
  environment: Scalars['Int']['input'];
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


export type QueryValidateWorkflowArgs = {
  workflow: Scalars['String']['input'];
};


export type QueryValidateWorkflowByIdArgs = {
  workflowId: Scalars['String']['input'];
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


export type QueryWorkspaceChatWorkflowsArgs = {
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
  tags?: InputMaybe<Array<Scalars['String']['input']>>;
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

export type WorkflowValidationResult = {
  __typename?: 'WorkflowValidationResult';
  errors: Array<Scalars['String']['output']>;
  warnings: Array<Scalars['String']['output']>;
};
