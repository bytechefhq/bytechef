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

export type AddAiHubPersonalAgentResourceInput = {
  aiHubPersonalAgentId: Scalars['ID']['input'];
  kind: AiHubPersonalAgentResourceKind;
  /** The referenced entity's id (a workflow id, file id, …) — NOT the personal-agent-resource row id. */
  resourceId: Scalars['String']['input'];
  resourceName: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
};

export type AddAiHubPersonalAgentToolInput = {
  aiHubPersonalAgentId: Scalars['ID']['input'];
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
  operationName: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
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

/** A workflow affected by connection reassignment. */
export type AffectedWorkflow = {
  __typename?: 'AffectedWorkflow';
  connectionIds: Array<Scalars['ID']['output']>;
  workflowId: Scalars['String']['output'];
  workflowName: Scalars['String']['output'];
};

export type AggregateScoreDelta = {
  __typename?: 'AggregateScoreDelta';
  deltas: Array<ExperimentScoreAverage>;
  scoreName: Scalars['String']['output'];
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

export type AiAutoMemory = {
  __typename?: 'AiAutoMemory';
  content: Scalars['String']['output'];
  createdAt?: Maybe<Scalars['Long']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  environmentId: Scalars['Long']['output'];
  id: Scalars['ID']['output'];
  memoryType: AiAutoMemoryType;
  name: Scalars['String']['output'];
  title: Scalars['String']['output'];
  updatedAt?: Maybe<Scalars['Long']['output']>;
  userId: Scalars['Long']['output'];
  workspaceId: Scalars['Long']['output'];
};

export enum AiAutoMemoryType {
  Feedback = 'FEEDBACK',
  Project = 'PROJECT',
  Reference = 'REFERENCE',
  User = 'USER'
}

export type AiDefaultModel = {
  __typename?: 'AiDefaultModel';
  model: Scalars['String']['output'];
  provider: Scalars['String']['output'];
};

export type AiEvalDatasetItemView = {
  __typename?: 'AiEvalDatasetItemView';
  createdDate?: Maybe<Scalars['Long']['output']>;
  datasetVersionId: Scalars['ID']['output'];
  expectedOutput?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  input: Scalars['String']['output'];
  metadata?: Maybe<Scalars['String']['output']>;
  sourceTraceId?: Maybe<Scalars['ID']['output']>;
};

export type AiEvalDatasetVersionView = {
  __typename?: 'AiEvalDatasetVersionView';
  createdDate?: Maybe<Scalars['Long']['output']>;
  datasetId: Scalars['ID']['output'];
  frozen: Scalars['Boolean']['output'];
  id: Scalars['ID']['output'];
  itemCount: Scalars['Int']['output'];
  label?: Maybe<Scalars['String']['output']>;
};

export type AiEvalDatasetView = {
  __typename?: 'AiEvalDatasetView';
  archivedDate?: Maybe<Scalars['Long']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  tags?: Maybe<Scalars['String']['output']>;
  workspaceId: Scalars['ID']['output'];
};

export type AiEvalExecution = {
  __typename?: 'AiEvalExecution';
  createdDate?: Maybe<Scalars['Long']['output']>;
  errorMessage?: Maybe<Scalars['String']['output']>;
  evalRuleId: Scalars['ID']['output'];
  id: Scalars['ID']['output'];
  scoreId?: Maybe<Scalars['ID']['output']>;
  status: AiEvalExecutionStatus;
  traceId: Scalars['ID']['output'];
};

export enum AiEvalExecutionStatus {
  Completed = 'COMPLETED',
  Error = 'ERROR',
  Pending = 'PENDING'
}

export type AiEvalExperimentRunView = {
  __typename?: 'AiEvalExperimentRunView';
  cost?: Maybe<Scalars['Float']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  datasetItemId: Scalars['ID']['output'];
  errorMessage?: Maybe<Scalars['String']['output']>;
  experimentId: Scalars['ID']['output'];
  id: Scalars['ID']['output'];
  latencyMs?: Maybe<Scalars['Int']['output']>;
  status: Scalars['String']['output'];
  traceId?: Maybe<Scalars['ID']['output']>;
};

export type AiEvalExperimentView = {
  __typename?: 'AiEvalExperimentView';
  completedDate?: Maybe<Scalars['Long']['output']>;
  completedRuns: Scalars['Int']['output'];
  createdDate?: Maybe<Scalars['Long']['output']>;
  datasetVersionId: Scalars['ID']['output'];
  failedRuns: Scalars['Int']['output'];
  id: Scalars['ID']['output'];
  metadata?: Maybe<Scalars['String']['output']>;
  model?: Maybe<Scalars['String']['output']>;
  promptVersionId?: Maybe<Scalars['ID']['output']>;
  startedDate?: Maybe<Scalars['Long']['output']>;
  status: Scalars['String']['output'];
  stopRequested: Scalars['Boolean']['output'];
  totalRuns: Scalars['Int']['output'];
};

export type AiEvalRule = {
  __typename?: 'AiEvalRule';
  createdDate?: Maybe<Scalars['Long']['output']>;
  delaySeconds?: Maybe<Scalars['Int']['output']>;
  enabled: Scalars['Boolean']['output'];
  filters?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  model: Scalars['String']['output'];
  name: Scalars['String']['output'];
  projectId?: Maybe<Scalars['ID']['output']>;
  promptTemplate: Scalars['String']['output'];
  samplingRate: Scalars['Float']['output'];
  scoreConfigId: Scalars['ID']['output'];
  version?: Maybe<Scalars['Int']['output']>;
  workspaceId: Scalars['ID']['output'];
};

export type AiEvalScore = {
  __typename?: 'AiEvalScore';
  comment?: Maybe<Scalars['String']['output']>;
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  dataType?: Maybe<AiEvalScoreDataType>;
  evalRuleId?: Maybe<Scalars['ID']['output']>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  source: AiEvalScoreSource;
  spanId?: Maybe<Scalars['ID']['output']>;
  stringValue?: Maybe<Scalars['String']['output']>;
  traceId: Scalars['ID']['output'];
  value?: Maybe<Scalars['Float']['output']>;
  workspaceId: Scalars['ID']['output'];
};

export type AiEvalScoreAnalytics = {
  __typename?: 'AiEvalScoreAnalytics';
  average?: Maybe<Scalars['Float']['output']>;
  count?: Maybe<Scalars['Int']['output']>;
  dataType?: Maybe<AiEvalScoreDataType>;
  distribution?: Maybe<Array<Maybe<AiEvalScoreDistributionEntry>>>;
  max?: Maybe<Scalars['Float']['output']>;
  min?: Maybe<Scalars['Float']['output']>;
  name?: Maybe<Scalars['String']['output']>;
};

export type AiEvalScoreConfig = {
  __typename?: 'AiEvalScoreConfig';
  categories?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  dataType?: Maybe<AiEvalScoreDataType>;
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  maxValue?: Maybe<Scalars['Float']['output']>;
  minValue?: Maybe<Scalars['Float']['output']>;
  name: Scalars['String']['output'];
  version?: Maybe<Scalars['Int']['output']>;
  workspaceId: Scalars['ID']['output'];
};

export enum AiEvalScoreDataType {
  Boolean = 'BOOLEAN',
  Categorical = 'CATEGORICAL',
  Numeric = 'NUMERIC'
}

export type AiEvalScoreDistributionEntry = {
  __typename?: 'AiEvalScoreDistributionEntry';
  count?: Maybe<Scalars['Int']['output']>;
  value?: Maybe<Scalars['String']['output']>;
};

export enum AiEvalScoreSource {
  Api = 'API',
  LlmJudge = 'LLM_JUDGE',
  Manual = 'MANUAL'
}

export type AiEvalScoreTrendPoint = {
  __typename?: 'AiEvalScoreTrendPoint';
  average?: Maybe<Scalars['Float']['output']>;
  count: Scalars['Int']['output'];
  day: Scalars['Long']['output'];
};

export type AiEvalTemplate = {
  __typename?: 'AiEvalTemplate';
  categories?: Maybe<Array<Maybe<Scalars['String']['output']>>>;
  dataType?: Maybe<AiEvalScoreDataType>;
  description?: Maybe<Scalars['String']['output']>;
  key: Scalars['String']['output'];
  maxValue?: Maybe<Scalars['Float']['output']>;
  minValue?: Maybe<Scalars['Float']['output']>;
  promptTemplate: Scalars['String']['output'];
  title: Scalars['String']['output'];
};

export type AiGatewayBudget = {
  __typename?: 'AiGatewayBudget';
  alertThreshold: Scalars['Int']['output'];
  amount: Scalars['String']['output'];
  createdDate?: Maybe<Scalars['Long']['output']>;
  enabled: Scalars['Boolean']['output'];
  enforcementMode: AiGatewayBudgetEnforcementMode;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  period: AiGatewayBudgetPeriod;
  version?: Maybe<Scalars['Int']['output']>;
};

export enum AiGatewayBudgetEnforcementMode {
  Hard = 'HARD',
  Soft = 'SOFT'
}

export enum AiGatewayBudgetPeriod {
  Daily = 'DAILY',
  Monthly = 'MONTHLY',
  Quarterly = 'QUARTERLY',
  Weekly = 'WEEKLY',
  Yearly = 'YEARLY'
}

export type AiGatewayModel = {
  __typename?: 'AiGatewayModel';
  alias?: Maybe<Scalars['String']['output']>;
  capabilities?: Maybe<Scalars['String']['output']>;
  contextWindow?: Maybe<Scalars['Int']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  defaultRoutingPolicyId?: Maybe<Scalars['ID']['output']>;
  enabled: Scalars['Boolean']['output'];
  id: Scalars['ID']['output'];
  inputCostPerMTokens?: Maybe<Scalars['Float']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  outputCostPerMTokens?: Maybe<Scalars['Float']['output']>;
  providerId: Scalars['ID']['output'];
  version?: Maybe<Scalars['Int']['output']>;
};

export type AiGatewayModelDeployment = {
  __typename?: 'AiGatewayModelDeployment';
  enabled: Scalars['Boolean']['output'];
  id: Scalars['ID']['output'];
  maxRpm?: Maybe<Scalars['Int']['output']>;
  maxTpm?: Maybe<Scalars['Int']['output']>;
  modelId: Scalars['ID']['output'];
  priorityOrder: Scalars['Int']['output'];
  routingPolicyId: Scalars['ID']['output'];
  weight: Scalars['Int']['output'];
};

export type AiGatewayProject = {
  __typename?: 'AiGatewayProject';
  cacheTtlMinutes?: Maybe<Scalars['Int']['output']>;
  cachingEnabled?: Maybe<Scalars['Boolean']['output']>;
  compressionEnabled?: Maybe<Scalars['Boolean']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  logRetentionDays?: Maybe<Scalars['Int']['output']>;
  name: Scalars['String']['output'];
  retryMaxAttempts?: Maybe<Scalars['Int']['output']>;
  routingPolicyId?: Maybe<Scalars['ID']['output']>;
  slug: Scalars['String']['output'];
  timeoutSeconds?: Maybe<Scalars['Int']['output']>;
  version?: Maybe<Scalars['Int']['output']>;
};

export type AiGatewayProvider = {
  __typename?: 'AiGatewayProvider';
  baseUrl?: Maybe<Scalars['String']['output']>;
  config?: Maybe<Scalars['String']['output']>;
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  enabled: Scalars['Boolean']['output'];
  id: Scalars['ID']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  type: AiGatewayProviderType;
  version?: Maybe<Scalars['Int']['output']>;
};

export enum AiGatewayProviderType {
  Anthropic = 'ANTHROPIC',
  AzureOpenai = 'AZURE_OPENAI',
  Cohere = 'COHERE',
  Deepseek = 'DEEPSEEK',
  GoogleGemini = 'GOOGLE_GEMINI',
  Groq = 'GROQ',
  Mistral = 'MISTRAL',
  Openai = 'OPENAI'
}

export type AiGatewayRateLimit = {
  __typename?: 'AiGatewayRateLimit';
  createdDate?: Maybe<Scalars['Long']['output']>;
  enabled: Scalars['Boolean']['output'];
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  limitType: AiGatewayRateLimitType;
  limitValue: Scalars['Int']['output'];
  name: Scalars['String']['output'];
  projectId?: Maybe<Scalars['ID']['output']>;
  propertyKey?: Maybe<Scalars['String']['output']>;
  scope: AiGatewayRateLimitScope;
  version?: Maybe<Scalars['Int']['output']>;
  windowSeconds: Scalars['Int']['output'];
};

export enum AiGatewayRateLimitScope {
  Global = 'GLOBAL',
  PerProperty = 'PER_PROPERTY',
  PerUser = 'PER_USER'
}

export enum AiGatewayRateLimitType {
  Cost = 'COST',
  Requests = 'REQUESTS',
  Tokens = 'TOKENS'
}

export type AiGatewayRequestLog = {
  __typename?: 'AiGatewayRequestLog';
  apiKeyId?: Maybe<Scalars['ID']['output']>;
  cacheHit?: Maybe<Scalars['Boolean']['output']>;
  cost?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  errorMessage?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  inputTokens?: Maybe<Scalars['Int']['output']>;
  latencyMs?: Maybe<Scalars['Int']['output']>;
  outputTokens?: Maybe<Scalars['Int']['output']>;
  requestId: Scalars['String']['output'];
  requestedModel?: Maybe<Scalars['String']['output']>;
  routedModel?: Maybe<Scalars['String']['output']>;
  routedProvider?: Maybe<Scalars['String']['output']>;
  routingPolicyId?: Maybe<Scalars['ID']['output']>;
  routingStrategy?: Maybe<Scalars['String']['output']>;
  status?: Maybe<Scalars['Int']['output']>;
};

export type AiGatewayRoutingPolicy = {
  __typename?: 'AiGatewayRoutingPolicy';
  config?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  deployments?: Maybe<Array<Maybe<AiGatewayModelDeployment>>>;
  enabled: Scalars['Boolean']['output'];
  fallbackModel?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  strategy: AiGatewayRoutingStrategyType;
  tagIds?: Maybe<Array<Scalars['ID']['output']>>;
  version?: Maybe<Scalars['Int']['output']>;
};

export enum AiGatewayRoutingStrategyType {
  CostOptimized = 'COST_OPTIMIZED',
  IntelligentBalanced = 'INTELLIGENT_BALANCED',
  IntelligentCost = 'INTELLIGENT_COST',
  IntelligentQuality = 'INTELLIGENT_QUALITY',
  LatencyOptimized = 'LATENCY_OPTIMIZED',
  PriorityFallback = 'PRIORITY_FALLBACK',
  Simple = 'SIMPLE',
  TagBased = 'TAG_BASED',
  WeightedRandom = 'WEIGHTED_RANDOM'
}

export type AiGatewaySpendSummary = {
  __typename?: 'AiGatewaySpendSummary';
  apiKeyId?: Maybe<Scalars['ID']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  model?: Maybe<Scalars['String']['output']>;
  periodEnd?: Maybe<Scalars['Long']['output']>;
  periodStart?: Maybe<Scalars['Long']['output']>;
  provider?: Maybe<Scalars['String']['output']>;
  requestCount?: Maybe<Scalars['Int']['output']>;
  totalCost?: Maybe<Scalars['String']['output']>;
  totalInputTokens?: Maybe<Scalars['Long']['output']>;
  totalOutputTokens?: Maybe<Scalars['Long']['output']>;
};

export type AiGatewayWorkspaceSettings = {
  __typename?: 'AiGatewayWorkspaceSettings';
  cacheEnabled?: Maybe<Scalars['Boolean']['output']>;
  cacheTtlSeconds?: Maybe<Scalars['Int']['output']>;
  defaultRoutingPolicyId?: Maybe<Scalars['ID']['output']>;
  logRetentionDays?: Maybe<Scalars['Int']['output']>;
  redactPii?: Maybe<Scalars['Boolean']['output']>;
  retryCount?: Maybe<Scalars['Int']['output']>;
  softBudgetWarningPct?: Maybe<Scalars['Int']['output']>;
  timeoutMs?: Maybe<Scalars['Int']['output']>;
  workspaceId: Scalars['ID']['output'];
};

export type AiGatewayWorkspaceSettingsInput = {
  cacheEnabled?: InputMaybe<Scalars['Boolean']['input']>;
  cacheTtlSeconds?: InputMaybe<Scalars['Int']['input']>;
  defaultRoutingPolicyId?: InputMaybe<Scalars['ID']['input']>;
  logRetentionDays?: InputMaybe<Scalars['Int']['input']>;
  redactPii?: InputMaybe<Scalars['Boolean']['input']>;
  retryCount?: InputMaybe<Scalars['Int']['input']>;
  softBudgetWarningPct?: InputMaybe<Scalars['Int']['input']>;
  timeoutMs?: InputMaybe<Scalars['Int']['input']>;
  workspaceId: Scalars['ID']['input'];
};

export type AiHubMcpServer = {
  __typename?: 'AiHubMcpServer';
  enabled: Scalars['Boolean']['output'];
  hasAuthToken: Scalars['Boolean']['output'];
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  url: Scalars['String']['output'];
};

export type AiHubMcpServerTool = {
  __typename?: 'AiHubMcpServerTool';
  description?: Maybe<Scalars['String']['output']>;
  enabled: Scalars['Boolean']['output'];
  name: Scalars['String']['output'];
};

export type AiHubPersonalAgent = {
  __typename?: 'AiHubPersonalAgent';
  createdAt?: Maybe<Scalars['Long']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  environmentId: Scalars['Long']['output'];
  id: Scalars['ID']['output'];
  instructions?: Maybe<Scalars['String']['output']>;
  /**
   * Optional per-agent LLM model id (provider-specific, e.g. "gpt-4o-mini", "claude-3-5-sonnet-20241022"). Paired
   * with llmProvider. The (provider, model) pair must resolve to a workspace-enabled provider at run time; otherwise
   * the agent falls back to the workspace default with a warn log.
   */
  llmModel?: Maybe<Scalars['String']['output']>;
  /**
   * Optional per-agent LLM provider override (e.g. "openai", "anthropic"). Both llmProvider and llmModel must be
   * set together or both null. Null/null means "use workspace default LLM." Service validation rejects half-set.
   */
  llmProvider?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  /**
   * The agent's resource template. Each entry is a (kind, resourceId, resourceName) reference. When a task is
   * created against this agent, these are copied into ai_hub_task_artifact as *_REFERENCED artifacts.
   */
  resources: Array<AiHubPersonalAgentResource>;
  /**
   * Optional one-to-one schedule. Null when the user has not enabled scheduling for this agent.
   * Read-only here — mutate via setAiHubPersonalAgentSchedule.
   */
  schedule?: Maybe<AiHubPersonalAgentSchedule>;
  title?: Maybe<Scalars['String']['output']>;
  /**
   * The agent's tool template. Each entry is a (componentName, componentVersion, operationName) triple the
   * agent declares as available. When a task is created against this agent, these tools are auto-attached
   * to the ai_hub_task_tool table so the LLM sees them on the very first turn — same UX as AI Agent simple mode.
   */
  tools: Array<AiHubPersonalAgentTool>;
  updatedAt?: Maybe<Scalars['Long']['output']>;
  userId: Scalars['Long']['output'];
  workspaceId: Scalars['Long']['output'];
};

export type AiHubPersonalAgentResource = {
  __typename?: 'AiHubPersonalAgentResource';
  aiHubPersonalAgentId: Scalars['Long']['output'];
  createdAt?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  kind: AiHubPersonalAgentResourceKind;
  /**
   * The referenced entity's id (a workflow id, file id, …). String because composer resource ids are strings and
   * workflow ids are non-numeric.
   */
  resourceId: Scalars['String']['output'];
  resourceName: Scalars['String']['output'];
};

export enum AiHubPersonalAgentResourceKind {
  ApiCollection = 'API_COLLECTION',
  DataTable = 'DATA_TABLE',
  File = 'FILE',
  KnowledgeBase = 'KNOWLEDGE_BASE',
  McpServer = 'MCP_SERVER',
  Task = 'TASK',
  Workflow = 'WORKFLOW',
  WorkflowExecution = 'WORKFLOW_EXECUTION'
}

/** Scheduled run configuration for a personal agent. At most one row per agent. */
export type AiHubPersonalAgentSchedule = {
  __typename?: 'AiHubPersonalAgentSchedule';
  aiHubPersonalAgentId: Scalars['ID']['output'];
  cronExpression?: Maybe<Scalars['String']['output']>;
  dayOfMonth?: Maybe<Scalars['Int']['output']>;
  dayOfWeek?: Maybe<Scalars['Int']['output']>;
  effectiveCronExpression: Scalars['String']['output'];
  enabled: Scalars['Boolean']['output'];
  frequencyKind: ScheduleFrequencyKind;
  id: Scalars['ID']['output'];
  intervalMinutes?: Maybe<Scalars['Int']['output']>;
  lastRunAt?: Maybe<Scalars['String']['output']>;
  lifecycleKind: ScheduleLifecycleKind;
  maxRuns?: Maybe<Scalars['Int']['output']>;
  minuteOfHour?: Maybe<Scalars['Int']['output']>;
  nextRunAt?: Maybe<Scalars['String']['output']>;
  prompt: Scalars['String']['output'];
  remainingRuns?: Maybe<Scalars['Int']['output']>;
  startDate?: Maybe<Scalars['String']['output']>;
  timeOfDay?: Maybe<Scalars['String']['output']>;
  title: Scalars['String']['output'];
  zoneId: Scalars['String']['output'];
};

export type AiHubPersonalAgentScheduleInput = {
  cronExpression?: InputMaybe<Scalars['String']['input']>;
  dayOfMonth?: InputMaybe<Scalars['Int']['input']>;
  dayOfWeek?: InputMaybe<Scalars['Int']['input']>;
  enabled: Scalars['Boolean']['input'];
  frequencyKind: ScheduleFrequencyKind;
  intervalMinutes?: InputMaybe<Scalars['Int']['input']>;
  lifecycleKind: ScheduleLifecycleKind;
  maxRuns?: InputMaybe<Scalars['Int']['input']>;
  minuteOfHour?: InputMaybe<Scalars['Int']['input']>;
  prompt: Scalars['String']['input'];
  startDate?: InputMaybe<Scalars['String']['input']>;
  timeOfDay?: InputMaybe<Scalars['String']['input']>;
  title: Scalars['String']['input'];
  zoneId: Scalars['String']['input'];
};

export type AiHubPersonalAgentTool = {
  __typename?: 'AiHubPersonalAgentTool';
  aiHubPersonalAgentId: Scalars['Long']['output'];
  componentName: Scalars['String']['output'];
  componentVersion: Scalars['Int']['output'];
  /**
   * Optional pinned connection. When set, aiHubTasks spawned from this agent attach the tool's component to
   * this specific connection so the LLM doesn't have to re-pick at first invocation. Null when the agent's
   * template leaves connection picking to the user (matches pre-config behaviour).
   */
  connectionId?: Maybe<Scalars['Long']['output']>;
  createdAt?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  operationName: Scalars['String']['output'];
  /**
   * Pre-set invocation parameters. Merged with LLM-supplied call arguments at dispatch time (LLM args win on
   * conflict). Empty map when the agent's template doesn't pre-bind any defaults.
   */
  parameters?: Maybe<Scalars['Any']['output']>;
};

/**
 * AI Hub task metadata. {@code threadId} is client-generated and keys Spring AI's chat-memory
 * table; the (workspace, user, environment, threadId) tuple uniquely identifies the chat thread.
 */
export type AiHubTask = {
  __typename?: 'AiHubTask';
  /**
   * Owning personal-agent id. Non-null when {@code kind = PERSONAL_AGENT}; null for STANDARD and WORKFLOW_CHAT.
   * The client uses this id to resolve the agent's display title for sidebar grouping and to navigate to the
   * agent's detail view.
   */
  aiHubPersonalAgentId?: Maybe<Scalars['Long']['output']>;
  /**
   * Whether the title was set automatically (creation-time auto-label or LLM regeneration) and remains eligible
   * for further LLM regeneration. Flips to false when the LLM regenerates a title or the user renames the
   * task, so the regen loop fires once and then stops. The client uses this flag to decide whether to
   * invoke generateAiHubTaskTitle on each turn.
   */
  autoTitled: Scalars['Boolean']['output'];
  createdAt?: Maybe<Scalars['Long']['output']>;
  environmentId: Scalars['Long']['output'];
  id: Scalars['ID']['output'];
  /**
   * Discriminator for the task flavour. STANDARD (default) talks to the LLM agent; WORKFLOW_CHAT
   * binds the task to a specific workflow execution and the client picks ChatRuntimeProvider
   * instead of AiHubRuntimeProvider for those rows.
   */
  kind: AiHubTaskKind;
  lastPreview?: Maybe<Scalars['String']['output']>;
  messageCount: Scalars['Int']['output'];
  /**
   * Parent project-deployment id for the workflow execution. Used by the sidebar to group workflow chats by project.
   * Non-null when {@code kind = WORKFLOW_CHAT}.
   */
  projectDeploymentId?: Maybe<Scalars['Long']['output']>;
  status: AiHubTaskStatus;
  threadId: Scalars['String']['output'];
  title?: Maybe<Scalars['String']['output']>;
  updatedAt?: Maybe<Scalars['Long']['output']>;
  userId: Scalars['Long']['output'];
  /** Composite WorkflowExecutionId string the task chats with. Non-null when {@code kind = WORKFLOW_CHAT}. */
  workflowExecutionId?: Maybe<Scalars['String']['output']>;
  workspaceId: Scalars['Long']['output'];
};

/**
 * Audit/undo log row for a AI Hub mutation. Mirrors {@link AiHubTaskArtifact}: id is the primary
 * key, taskId pins the row to a task, kind/status are ordinal-pinned enums, environmentId is
 * denormalised from the parent task at write time so analytics queries do not need a join.
 */
export type AiHubTaskArtifact = {
  __typename?: 'AiHubTaskArtifact';
  artifactId: Scalars['String']['output'];
  artifactName: Scalars['String']['output'];
  createdAt?: Maybe<Scalars['Long']['output']>;
  environmentId: Scalars['Long']['output'];
  id: Scalars['ID']['output'];
  kind: AiHubTaskArtifactKind;
  metadataJson?: Maybe<Scalars['String']['output']>;
  status: AiHubTaskArtifactStatus;
  statusChangedAt?: Maybe<Scalars['Long']['output']>;
  taskId: Scalars['ID']['output'];
};

export enum AiHubTaskArtifactKind {
  BinaryFileCreated = 'BINARY_FILE_CREATED',
  DataTableColumnAdded = 'DATA_TABLE_COLUMN_ADDED',
  DataTableReferenced = 'DATA_TABLE_REFERENCED',
  DataTableRowAdded = 'DATA_TABLE_ROW_ADDED',
  DataTableRowDeleted = 'DATA_TABLE_ROW_DELETED',
  DataTableRowUpdated = 'DATA_TABLE_ROW_UPDATED',
  FileCreated = 'FILE_CREATED',
  FileReferenced = 'FILE_REFERENCED',
  KbDocumentAdded = 'KB_DOCUMENT_ADDED',
  KbDocumentDeleted = 'KB_DOCUMENT_DELETED',
  KbReferenced = 'KB_REFERENCED',
  MemoryCreated = 'MEMORY_CREATED',
  MemoryDeleted = 'MEMORY_DELETED',
  MemoryRenamed = 'MEMORY_RENAMED',
  MemoryUpdated = 'MEMORY_UPDATED',
  WorkflowCreated = 'WORKFLOW_CREATED',
  WorkflowExecutionStarted = 'WORKFLOW_EXECUTION_STARTED',
  WorkflowReferenced = 'WORKFLOW_REFERENCED',
  WorkflowUpdated = 'WORKFLOW_UPDATED'
}

/**
 * Paginated artifact listing. {@code totalCount} is the total across all pages so the UI can render an
 * accurate "showing N of M" hint; {@code hasMore} is a derived convenience for pagination controls so the
 * client does not have to compute it from page+size.
 */
export type AiHubTaskArtifactPage = {
  __typename?: 'AiHubTaskArtifactPage';
  hasMore: Scalars['Boolean']['output'];
  items: Array<AiHubTaskArtifact>;
  pageClamped: Scalars['Boolean']['output'];
  sizeClamped: Scalars['Boolean']['output'];
  totalCount: Scalars['Long']['output'];
};

export enum AiHubTaskArtifactStatus {
  Applied = 'APPLIED',
  Expired = 'EXPIRED',
  Irreversible = 'IRREVERSIBLE'
}

export enum AiHubTaskKind {
  PersonalAgent = 'PERSONAL_AGENT',
  Standard = 'STANDARD',
  WorkflowChat = 'WORKFLOW_CHAT'
}

/**
 * Single chat message in a task. {@code role} is one of {@code user}/{@code assistant}/{@code system};
 * {@code timestamp} is the message's epoch-milli arrival time.
 */
export type AiHubTaskMessage = {
  __typename?: 'AiHubTaskMessage';
  content: Scalars['String']['output'];
  role: Scalars['String']['output'];
  timestamp: Scalars['Long']['output'];
};

export type AiHubTaskPatchInput = {
  id: Scalars['ID']['input'];
  lastPreview?: InputMaybe<Scalars['String']['input']>;
  messageCount?: InputMaybe<Scalars['Int']['input']>;
  status?: InputMaybe<AiHubTaskStatus>;
  title?: InputMaybe<Scalars['String']['input']>;
  workspaceId: Scalars['ID']['input'];
};

export enum AiHubTaskStatus {
  Active = 'ACTIVE',
  Archived = 'ARCHIVED',
  Deleted = 'DELETED'
}

export type AiHubTaskToolBinding = {
  __typename?: 'AiHubTaskToolBinding';
  clusterElementName: Scalars['String']['output'];
  componentName: Scalars['String']['output'];
  componentVersion: Scalars['Int']['output'];
  connectionId?: Maybe<Scalars['ID']['output']>;
  environment: Scalars['Int']['output'];
  parameters: Scalars['Any']['output'];
  taskComponentId: Scalars['ID']['output'];
  taskId: Scalars['ID']['output'];
  taskToolId: Scalars['ID']['output'];
};

export type AiHubUserConnector = {
  __typename?: 'AiHubUserConnector';
  componentName: Scalars['String']['output'];
  componentVersion: Scalars['Int']['output'];
  connectionId?: Maybe<Scalars['ID']['output']>;
  connectionRequired: Scalars['Boolean']['output'];
  description?: Maybe<Scalars['String']['output']>;
  enabled: Scalars['Boolean']['output'];
  icon?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  title?: Maybe<Scalars['String']['output']>;
  tools: Array<AiHubUserConnectorTool>;
};

export type AiHubUserConnectorTool = {
  __typename?: 'AiHubUserConnectorTool';
  description?: Maybe<Scalars['String']['output']>;
  enabled: Scalars['Boolean']['output'];
  name: Scalars['String']['output'];
  parameters?: Maybe<Scalars['Any']['output']>;
  title?: Maybe<Scalars['String']['output']>;
};

export type AiHubWorkspaceSettings = {
  __typename?: 'AiHubWorkspaceSettings';
  voiceWebhookUrl?: Maybe<Scalars['String']['output']>;
  workspaceId: Scalars['ID']['output'];
};

export enum AiObservabilityAlertCondition {
  Equals = 'EQUALS',
  GreaterThan = 'GREATER_THAN',
  LessThan = 'LESS_THAN'
}

export type AiObservabilityAlertEvent = {
  __typename?: 'AiObservabilityAlertEvent';
  alertRuleId: Scalars['ID']['output'];
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  message?: Maybe<Scalars['String']['output']>;
  status: AiObservabilityAlertEventStatus;
  triggeredValue?: Maybe<Scalars['Float']['output']>;
};

export enum AiObservabilityAlertEventStatus {
  Acknowledged = 'ACKNOWLEDGED',
  Resolved = 'RESOLVED',
  Triggered = 'TRIGGERED'
}

export enum AiObservabilityAlertMetric {
  Cost = 'COST',
  ErrorRate = 'ERROR_RATE',
  LatencyP95 = 'LATENCY_P95',
  RequestVolume = 'REQUEST_VOLUME',
  TokenUsage = 'TOKEN_USAGE'
}

export type AiObservabilityAlertRule = {
  __typename?: 'AiObservabilityAlertRule';
  channelIds?: Maybe<Array<Maybe<Scalars['ID']['output']>>>;
  condition: AiObservabilityAlertCondition;
  cooldownMinutes: Scalars['Int']['output'];
  createdDate?: Maybe<Scalars['Long']['output']>;
  enabled: Scalars['Boolean']['output'];
  filters?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  metric: AiObservabilityAlertMetric;
  name: Scalars['String']['output'];
  projectId?: Maybe<Scalars['ID']['output']>;
  snoozedUntil?: Maybe<Scalars['Long']['output']>;
  threshold: Scalars['Float']['output'];
  version?: Maybe<Scalars['Int']['output']>;
  windowMinutes: Scalars['Int']['output'];
};

export type AiObservabilityAlertRuleInput = {
  channelIds?: InputMaybe<Array<InputMaybe<Scalars['ID']['input']>>>;
  condition: AiObservabilityAlertCondition;
  cooldownMinutes: Scalars['Int']['input'];
  enabled: Scalars['Boolean']['input'];
  filters?: InputMaybe<Scalars['String']['input']>;
  metric: AiObservabilityAlertMetric;
  name: Scalars['String']['input'];
  projectId?: InputMaybe<Scalars['ID']['input']>;
  threshold: Scalars['Float']['input'];
  windowMinutes: Scalars['Int']['input'];
  workspaceId: Scalars['ID']['input'];
};

export enum AiObservabilityExportFormat {
  Csv = 'CSV',
  Json = 'JSON',
  Jsonl = 'JSONL'
}

export type AiObservabilityExportJob = {
  __typename?: 'AiObservabilityExportJob';
  createdBy: Scalars['String']['output'];
  createdDate?: Maybe<Scalars['Long']['output']>;
  cronExpression?: Maybe<Scalars['String']['output']>;
  errorMessage?: Maybe<Scalars['String']['output']>;
  filePath?: Maybe<Scalars['String']['output']>;
  filters?: Maybe<Scalars['String']['output']>;
  format: AiObservabilityExportFormat;
  id: Scalars['ID']['output'];
  projectId?: Maybe<Scalars['ID']['output']>;
  recordCount?: Maybe<Scalars['Int']['output']>;
  scope: AiObservabilityExportScope;
  status: AiObservabilityExportJobStatus;
  type: AiObservabilityExportJobType;
};

export enum AiObservabilityExportJobStatus {
  Cancelled = 'CANCELLED',
  Completed = 'COMPLETED',
  Failed = 'FAILED',
  Pending = 'PENDING',
  Processing = 'PROCESSING'
}

export enum AiObservabilityExportJobType {
  OnDemand = 'ON_DEMAND',
  Scheduled = 'SCHEDULED'
}

export enum AiObservabilityExportScope {
  Prompts = 'PROMPTS',
  RequestLogs = 'REQUEST_LOGS',
  Sessions = 'SESSIONS',
  Traces = 'TRACES'
}

export type AiObservabilityNotificationChannel = {
  __typename?: 'AiObservabilityNotificationChannel';
  config: Scalars['String']['output'];
  createdDate?: Maybe<Scalars['Long']['output']>;
  enabled: Scalars['Boolean']['output'];
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  type: AiObservabilityNotificationChannelType;
  version?: Maybe<Scalars['Int']['output']>;
};

export type AiObservabilityNotificationChannelInput = {
  config: Scalars['String']['input'];
  enabled: Scalars['Boolean']['input'];
  name: Scalars['String']['input'];
  type: AiObservabilityNotificationChannelType;
  workspaceId: Scalars['ID']['input'];
};

export enum AiObservabilityNotificationChannelType {
  Email = 'EMAIL',
  Slack = 'SLACK',
  Webhook = 'WEBHOOK'
}

export type AiObservabilitySession = {
  __typename?: 'AiObservabilitySession';
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  projectId?: Maybe<Scalars['ID']['output']>;
  traceCount?: Maybe<Scalars['Int']['output']>;
  traces?: Maybe<Array<Maybe<AiObservabilityTrace>>>;
  userId?: Maybe<Scalars['String']['output']>;
  version?: Maybe<Scalars['Int']['output']>;
  workspaceId: Scalars['ID']['output'];
};

export type AiObservabilitySpan = {
  __typename?: 'AiObservabilitySpan';
  cost?: Maybe<Scalars['Float']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  endTime?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  input?: Maybe<Scalars['String']['output']>;
  inputTokens?: Maybe<Scalars['Int']['output']>;
  latencyMs?: Maybe<Scalars['Int']['output']>;
  level: AiObservabilitySpanLevel;
  metadata?: Maybe<Scalars['String']['output']>;
  model?: Maybe<Scalars['String']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  output?: Maybe<Scalars['String']['output']>;
  outputTokens?: Maybe<Scalars['Int']['output']>;
  parentSpanId?: Maybe<Scalars['ID']['output']>;
  promptId?: Maybe<Scalars['ID']['output']>;
  promptVersionId?: Maybe<Scalars['ID']['output']>;
  provider?: Maybe<Scalars['String']['output']>;
  startTime?: Maybe<Scalars['Long']['output']>;
  status: AiObservabilitySpanStatus;
  traceId: Scalars['ID']['output'];
  type: AiObservabilitySpanType;
  version?: Maybe<Scalars['Int']['output']>;
};

export enum AiObservabilitySpanLevel {
  Debug = 'DEBUG',
  Default = 'DEFAULT',
  Error = 'ERROR',
  Warning = 'WARNING'
}

export enum AiObservabilitySpanStatus {
  Active = 'ACTIVE',
  Completed = 'COMPLETED',
  Error = 'ERROR'
}

export enum AiObservabilitySpanType {
  Event = 'EVENT',
  Generation = 'GENERATION',
  Span = 'SPAN',
  ToolCall = 'TOOL_CALL'
}

export type AiObservabilityTrace = {
  __typename?: 'AiObservabilityTrace';
  apiKeyId?: Maybe<Scalars['ID']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  input?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  metadata?: Maybe<Scalars['String']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  output?: Maybe<Scalars['String']['output']>;
  projectId?: Maybe<Scalars['ID']['output']>;
  sessionId?: Maybe<Scalars['ID']['output']>;
  source: AiObservabilityTraceSource;
  spans?: Maybe<Array<Maybe<AiObservabilitySpan>>>;
  status: AiObservabilityTraceStatus;
  tagIds?: Maybe<Array<Scalars['ID']['output']>>;
  totalCost?: Maybe<Scalars['Float']['output']>;
  totalInputTokens?: Maybe<Scalars['Int']['output']>;
  totalLatencyMs?: Maybe<Scalars['Int']['output']>;
  totalOutputTokens?: Maybe<Scalars['Int']['output']>;
  userId?: Maybe<Scalars['String']['output']>;
  version?: Maybe<Scalars['Int']['output']>;
  workspaceId: Scalars['ID']['output'];
};

export enum AiObservabilityTraceSource {
  Api = 'API',
  Experiment = 'EXPERIMENT',
  Otlp = 'OTLP',
  Playground = 'PLAYGROUND'
}

export enum AiObservabilityTraceStatus {
  Active = 'ACTIVE',
  Completed = 'COMPLETED',
  Error = 'ERROR'
}

export type AiObservabilityWebhookDelivery = {
  __typename?: 'AiObservabilityWebhookDelivery';
  attemptCount: Scalars['Int']['output'];
  createdDate?: Maybe<Scalars['Long']['output']>;
  deliveredDate?: Maybe<Scalars['Long']['output']>;
  errorMessage?: Maybe<Scalars['String']['output']>;
  eventType?: Maybe<Scalars['String']['output']>;
  httpStatus?: Maybe<Scalars['Int']['output']>;
  id: Scalars['ID']['output'];
  payload?: Maybe<Scalars['String']['output']>;
  status: AiObservabilityWebhookDeliveryStatus;
  subscriptionId: Scalars['ID']['output'];
};

export enum AiObservabilityWebhookDeliveryStatus {
  Failed = 'FAILED',
  Pending = 'PENDING',
  Retrying = 'RETRYING',
  Success = 'SUCCESS'
}

export type AiObservabilityWebhookSubscription = {
  __typename?: 'AiObservabilityWebhookSubscription';
  createdDate?: Maybe<Scalars['Long']['output']>;
  enabled: Scalars['Boolean']['output'];
  events: Scalars['String']['output'];
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  lastTriggeredDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  projectId?: Maybe<Scalars['ID']['output']>;
  url: Scalars['String']['output'];
  version?: Maybe<Scalars['Int']['output']>;
};

export type AiPrompt = {
  __typename?: 'AiPrompt';
  createdDate?: Maybe<Scalars['Long']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  projectId?: Maybe<Scalars['ID']['output']>;
  version?: Maybe<Scalars['Int']['output']>;
  versions?: Maybe<Array<Maybe<AiPromptVersion>>>;
};

export type AiPromptVersion = {
  __typename?: 'AiPromptVersion';
  active: Scalars['Boolean']['output'];
  commitMessage?: Maybe<Scalars['String']['output']>;
  content: Scalars['String']['output'];
  createdBy: Scalars['String']['output'];
  createdDate?: Maybe<Scalars['Long']['output']>;
  environment?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  metrics?: Maybe<AiPromptVersionMetrics>;
  promptId: Scalars['ID']['output'];
  type: AiPromptVersionType;
  variables?: Maybe<Scalars['String']['output']>;
  versionNumber: Scalars['Int']['output'];
};

export type AiPromptVersionMetrics = {
  __typename?: 'AiPromptVersionMetrics';
  avgCostUsd?: Maybe<Scalars['Float']['output']>;
  avgLatencyMs?: Maybe<Scalars['Float']['output']>;
  errorRate?: Maybe<Scalars['Float']['output']>;
  invocationCount: Scalars['Int']['output'];
};

export enum AiPromptVersionType {
  Chat = 'CHAT',
  Text = 'TEXT'
}

export type AiProviderCatalogItem = {
  __typename?: 'AiProviderCatalogItem';
  enabled: Scalars['Boolean']['output'];
  icon?: Maybe<Scalars['String']['output']>;
  key: Scalars['String']['output'];
  models: Array<AiProviderModel>;
  name: Scalars['String']['output'];
  supportsModelById: Scalars['Boolean']['output'];
};

export type AiProviderModel = {
  __typename?: 'AiProviderModel';
  label: Scalars['String']['output'];
  name: Scalars['String']['output'];
};

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

export type AssetFile = {
  __typename?: 'AssetFile';
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  downloadUrl: Scalars['String']['output'];
  environmentId: Scalars['Long']['output'];
  format?: Maybe<Scalars['String']['output']>;
  generatedByAgentSource?: Maybe<Scalars['Int']['output']>;
  generatedFromPrompt?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedBy?: Maybe<Scalars['String']['output']>;
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  metadataJson?: Maybe<Scalars['String']['output']>;
  mimeType: Scalars['String']['output'];
  name: Scalars['String']['output'];
  sizeBytes: Scalars['Long']['output'];
  source: AssetFileSource;
  tags: Array<Tag>;
};

export enum AssetFileSource {
  AiGenerated = 'AI_GENERATED',
  UserUpload = 'USER_UPLOAD'
}

export type AttachAiHubTaskToolInput = {
  clusterElementName: Scalars['String']['input'];
  componentName: Scalars['String']['input'];
  componentVersion?: InputMaybe<Scalars['Int']['input']>;
  connectionId?: InputMaybe<Scalars['ID']['input']>;
  environment: Scalars['Int']['input'];
  parameters?: InputMaybe<Scalars['Any']['input']>;
  taskId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};

export type AuditEventDataEntryType = {
  __typename?: 'AuditEventDataEntryType';
  key: Scalars['String']['output'];
  value: Scalars['String']['output'];
};

export type AuditEventPageType = {
  __typename?: 'AuditEventPageType';
  content: Array<AuditEventType>;
  number: Scalars['Int']['output'];
  size: Scalars['Int']['output'];
  totalElements: Scalars['Int']['output'];
  totalPages: Scalars['Int']['output'];
};

export type AuditEventType = {
  __typename?: 'AuditEventType';
  data: Array<AuditEventDataEntryType>;
  eventDate: Scalars['Long']['output'];
  eventType: Scalars['String']['output'];
  id: Scalars['ID']['output'];
  principal?: Maybe<Scalars['String']['output']>;
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
  permissionExpression?: Maybe<Scalars['String']['output']>;
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
  permissionExpression?: Maybe<Scalars['String']['output']>;
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

export type BulkPromoteFailure = {
  __typename?: 'BulkPromoteFailure';
  connectionId: Scalars['ID']['output'];
  /** Stable classifier the client can key on for localized rendering — either a ConnectionErrorType key or UNEXPECTED. */
  errorCode: Scalars['String']['output'];
  /** Human-readable fallback. Sanitized server-side so SQL/JDBC detail never reaches the admin UI. */
  message: Scalars['String']['output'];
};

/** Outcome of a bulk visibility-change operation. Invariant: promoted + skipped + failed == attempted. (EE only) */
export type BulkPromoteResult = {
  __typename?: 'BulkPromoteResult';
  /** Candidate rows considered in this call (pre-filter size). */
  attempted: Scalars['Int']['output'];
  failed: Scalars['Int']['output'];
  failures: Array<BulkPromoteFailure>;
  promoted: Scalars['Int']['output'];
  /** Rows that were already at the target visibility at promote time (benign concurrent races). */
  skipped: Scalars['Int']['output'];
};

export type BulkReassignFailure = {
  __typename?: 'BulkReassignFailure';
  connectionId: Scalars['ID']['output'];
  /** Stable classifier the client can key on for localized rendering — either a ConnectionErrorType key or UNEXPECTED. */
  errorCode: Scalars['String']['output'];
  /** Human-readable fallback. Sanitized server-side so SQL/JDBC detail never reaches the admin UI. */
  message: Scalars['String']['output'];
};

/** Outcome of a bulk connection reassignment / mark-pending operation. Mirrors BulkPromoteResult. */
export type BulkReassignResult = {
  __typename?: 'BulkReassignResult';
  failed: Scalars['Int']['output'];
  failures: Array<BulkReassignFailure>;
  /** Rows in a terminal state (e.g. REVOKED) that could not legally transition — counted separately from failed so a silent no-op does not look like an error. */
  skipped: Scalars['Int']['output'];
  /** Rows considered by the operation (pre-filter size of the candidate set). */
  total: Scalars['Int']['output'];
  /** Rows whose state was successfully advanced in this call. */
  updated: Scalars['Int']['output'];
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
  projectWorkflowId: Scalars['ID']['output'];
  workflowExecutionId: Scalars['String']['output'];
  workflowId: Scalars['ID']['output'];
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

export type ComponentPolicy = {
  __typename?: 'ComponentPolicy';
  description?: Maybe<Scalars['String']['output']>;
  enabled: Scalars['Boolean']['output'];
  icon?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  title?: Maybe<Scalars['String']['output']>;
  version: Scalars['Int']['output'];
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

export type ConnectionCredentialStoreInfo = {
  __typename?: 'ConnectionCredentialStoreInfo';
  readOnly: Scalars['Boolean']['output'];
  type: ConnectionCredentialStoreType;
};

export enum ConnectionCredentialStoreType {
  AwsSecretsManager = 'AWS_SECRETS_MANAGER',
  Database = 'DATABASE',
  HashicorpVault = 'HASHICORP_VAULT'
}

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

/** A connection that needs reassignment, with metadata about its usage. */
export type ConnectionReassignmentItem = {
  __typename?: 'ConnectionReassignmentItem';
  connectionId: Scalars['ID']['output'];
  connectionName: Scalars['String']['output'];
  dependentWorkflowCount: Scalars['Int']['output'];
  environmentId: Scalars['Int']['output'];
  visibility: ConnectionVisibility;
};

export type ConnectionSearchResult = SearchResult & {
  __typename?: 'ConnectionSearchResult';
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  type: SearchAssetType;
};

/** Connection status indicating the lifecycle state of a connection. */
export enum ConnectionStatus {
  Active = 'ACTIVE',
  PendingReassignment = 'PENDING_REASSIGNMENT',
  Revoked = 'REVOKED'
}

/** Visibility scope controlling which users can see and use a connection. */
export enum ConnectionVisibility {
  Organization = 'ORGANIZATION',
  Private = 'PRIVATE',
  Workspace = 'WORKSPACE'
}

/** Parent Context Store entity. Env-stamped at creation; sources hang off this via contextStoreId. */
export type ContextStore = {
  __typename?: 'ContextStore';
  description?: Maybe<Scalars['String']['output']>;
  environment: Scalars['String']['output'];
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  tagIds: Array<Scalars['ID']['output']>;
  /**
   * Full Tag objects (id + name) resolved from the store's tagIds. Use {@code tags} when rendering the
   * TagList picker; use {@code tagIds} when you just need the ID list (e.g. for filter set membership).
   */
  tags: Array<Tag>;
  version: Scalars['Int']['output'];
};

/**
 * A Context Store source binds a workspace, a source component (with its ItemReader cluster element and
 * optional connection), and a record-shape definition to a periodic sync cadence. Source is 1:1 with its
 * record shape — {@code entityName}, {@code idField}, {@code indexedFields}, etc. live directly on the source
 * row.
 */
export type ContextStoreSource = {
  __typename?: 'ContextStoreSource';
  cadence: Scalars['String']['output'];
  connectionId?: Maybe<Scalars['ID']['output']>;
  /**
   * FK to the parent ContextStore. Environment is inherited via this relationship — there is no
   * environment column on the source itself.
   */
  contextStoreId: Scalars['ID']['output'];
  description?: Maybe<Scalars['String']['output']>;
  enabled: Scalars['Boolean']['output'];
  /**
   * Stable wire identifier for the source's records (independent of the display {@code name}). Used by
   * the destination component handler and ClickHouse table naming.
   */
  entityName: Scalars['String']['output'];
  /**
   * Phase 17b: optional rare full-replace cadence paired with the regular incremental cadence.
   * Null = single-trigger MVP behavior.
   */
  fullReplaceCadence?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  idField: Scalars['String']['output'];
  indexedFields: Scalars['Map']['output'];
  lastSyncJobExecutionId?: Maybe<Scalars['ID']['output']>;
  lastSyncRunAt?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  parameters?: Maybe<Scalars['Map']['output']>;
  semanticIndexFields?: Maybe<Scalars['Map']['output']>;
  sourceClusterElementName?: Maybe<Scalars['String']['output']>;
  sourceComponentName: Scalars['String']['output'];
  sourceComponentVersion: Scalars['Int']['output'];
  status: ContextStoreSourceStatus;
  storedFields?: Maybe<Scalars['Map']['output']>;
  /** Phase 17b: tombstone-derivation strategy. */
  tombstoneStrategy: ContextStoreTombstoneStrategy;
  workflowId?: Maybe<Scalars['ID']['output']>;
  workspaceId?: Maybe<Scalars['ID']['output']>;
};

export type ContextStoreSourceFilter = {
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
};

export enum ContextStoreSourceStatus {
  BuildingPreview = 'BUILDING_PREVIEW',
  Disabled = 'DISABLED',
  Failed = 'FAILED',
  Preview = 'PREVIEW',
  Ready = 'READY'
}

export enum ContextStoreTombstoneStrategy {
  None = 'NONE',
  PeriodicFullReplace = 'PERIODIC_FULL_REPLACE',
  UpstreamChangeFeed = 'UPSTREAM_CHANGE_FEED'
}

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

export type CreateAiGatewayBudgetInput = {
  alertThreshold?: InputMaybe<Scalars['Int']['input']>;
  amount: Scalars['String']['input'];
  enforcementMode: AiGatewayBudgetEnforcementMode;
  period: AiGatewayBudgetPeriod;
  workspaceId: Scalars['ID']['input'];
};

export type CreateAiGatewayModelInput = {
  alias?: InputMaybe<Scalars['String']['input']>;
  capabilities?: InputMaybe<Scalars['String']['input']>;
  contextWindow?: InputMaybe<Scalars['Int']['input']>;
  defaultRoutingPolicyId?: InputMaybe<Scalars['ID']['input']>;
  inputCostPerMTokens?: InputMaybe<Scalars['Float']['input']>;
  name: Scalars['String']['input'];
  outputCostPerMTokens?: InputMaybe<Scalars['Float']['input']>;
  providerId: Scalars['ID']['input'];
};

export type CreateAiGatewayProjectInput = {
  cacheTtlMinutes?: InputMaybe<Scalars['Int']['input']>;
  cachingEnabled?: InputMaybe<Scalars['Boolean']['input']>;
  compressionEnabled?: InputMaybe<Scalars['Boolean']['input']>;
  description?: InputMaybe<Scalars['String']['input']>;
  logRetentionDays?: InputMaybe<Scalars['Int']['input']>;
  name: Scalars['String']['input'];
  retryMaxAttempts?: InputMaybe<Scalars['Int']['input']>;
  routingPolicyId?: InputMaybe<Scalars['ID']['input']>;
  slug: Scalars['String']['input'];
  timeoutSeconds?: InputMaybe<Scalars['Int']['input']>;
  workspaceId: Scalars['ID']['input'];
};

export type CreateAiGatewayProviderInput = {
  apiKey: Scalars['String']['input'];
  baseUrl?: InputMaybe<Scalars['String']['input']>;
  config?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  type: AiGatewayProviderType;
};

export type CreateAiGatewayRateLimitInput = {
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  limitType: AiGatewayRateLimitType;
  limitValue: Scalars['Int']['input'];
  name: Scalars['String']['input'];
  projectId?: InputMaybe<Scalars['ID']['input']>;
  propertyKey?: InputMaybe<Scalars['String']['input']>;
  scope: AiGatewayRateLimitScope;
  windowSeconds: Scalars['Int']['input'];
  workspaceId: Scalars['ID']['input'];
};

export type CreateAiGatewayRoutingPolicyInput = {
  config?: InputMaybe<Scalars['String']['input']>;
  fallbackModel?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  strategy: AiGatewayRoutingStrategyType;
  tagIds?: InputMaybe<Array<Scalars['ID']['input']>>;
};

export type CreateAiHubPersonalAgentInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  environment: Scalars['Int']['input'];
  instructions?: InputMaybe<Scalars['String']['input']>;
  /** Optional per-agent LLM model id. See llmProvider docs. */
  llmModel?: InputMaybe<Scalars['String']['input']>;
  /**
   * Optional per-agent LLM provider override. Must be paired with llmModel; setting only one throws a typed
   * validation error. Null/null = use workspace default LLM.
   */
  llmProvider?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  title?: InputMaybe<Scalars['String']['input']>;
  workspaceId: Scalars['ID']['input'];
};

export type CreateAiHubPersonalAgentTaskInput = {
  aiHubPersonalAgentId: Scalars['ID']['input'];
  environment: Scalars['Int']['input'];
  title?: InputMaybe<Scalars['String']['input']>;
  workspaceId: Scalars['ID']['input'];
};

export type CreateAiPromptInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  projectId?: InputMaybe<Scalars['ID']['input']>;
  workspaceId: Scalars['ID']['input'];
};

export type CreateAiPromptVersionInput = {
  active?: InputMaybe<Scalars['Boolean']['input']>;
  commitMessage?: InputMaybe<Scalars['String']['input']>;
  content: Scalars['String']['input'];
  environment?: InputMaybe<Scalars['String']['input']>;
  promptId: Scalars['ID']['input'];
  type: AiPromptVersionType;
  variables?: InputMaybe<Scalars['String']['input']>;
};

export type CreateApiConnectorInput = {
  connectorVersion: Scalars['Int']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
  icon?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  title?: InputMaybe<Scalars['String']['input']>;
};

export type CreateContextStoreInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  tagIds?: InputMaybe<Array<Scalars['ID']['input']>>;
};

export type CreateContextStoreSourceInput = {
  cadence: Scalars['String']['input'];
  connectionId?: InputMaybe<Scalars['ID']['input']>;
  /**
   * Parent Context Store ID. Required — sources must hang off an existing ContextStore in the
   * same workspace. Defense in depth: the facade verifies the store belongs to workspaceId.
   */
  contextStoreId: Scalars['ID']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
  /**
   * Stable wire identifier for the source's records. Used by the destination component handler and
   * ClickHouse table naming; independent of the display {@code name} (which can be renamed without
   * invalidating already-synced records).
   */
  entityName: Scalars['String']['input'];
  environmentId?: InputMaybe<Scalars['ID']['input']>;
  /**
   * Phase 17b: optional rare full-replace cadence paired with the regular incremental cadence
   * (e.g., '@daily' alongside an '@hourly' cadence). Null = single-trigger MVP.
   */
  fullReplaceCadence?: InputMaybe<Scalars['String']['input']>;
  idField: Scalars['String']['input'];
  indexedFields: Scalars['Map']['input'];
  name: Scalars['String']['input'];
  parameters?: InputMaybe<Scalars['Map']['input']>;
  semanticIndexFields?: InputMaybe<Scalars['Map']['input']>;
  /**
   * Optional. When omitted, the server picks the first ItemReader cluster element on the
   * source component. Supply a name only when the component defines more than one ItemReader.
   */
  sourceClusterElementName?: InputMaybe<Scalars['String']['input']>;
  sourceComponentName: Scalars['String']['input'];
  sourceComponentVersion: Scalars['Int']['input'];
  storedFields?: InputMaybe<Scalars['Map']['input']>;
  /** Phase 17b: tombstone-derivation strategy. Defaults to PERIODIC_FULL_REPLACE when omitted. */
  tombstoneStrategy?: InputMaybe<ContextStoreTombstoneStrategy>;
  workspaceId: Scalars['ID']['input'];
};

export type CreateCustomRoleInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  /** Permission scope names to grant (must be names registered by the server's PermissionScopeProvider SPI) */
  scopes: Array<Scalars['String']['input']>;
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

export type CreateKnowledgeBaseSourceInput = {
  cadence: Scalars['String']['input'];
  connectionId?: InputMaybe<Scalars['ID']['input']>;
  environmentId?: InputMaybe<Scalars['ID']['input']>;
  /**
   * Phase 17b: optional rare full-replace cadence paired with the regular incremental cadence
   * (e.g., '@daily' alongside an '@hourly' cadence). Null = single-trigger MVP.
   */
  fullReplaceCadence?: InputMaybe<Scalars['String']['input']>;
  knowledgeBaseId: Scalars['ID']['input'];
  /**
   * Optional metadata-tag whitelist shaped as {fields: ["fieldA", "fieldB"]}. Narrows which
   * incoming metadata keys become KB document tags at sync time. Omit (or null) to keep MVP
   * behavior — every field becomes a tag.
   */
  metadataFields?: InputMaybe<Scalars['Map']['input']>;
  name: Scalars['String']['input'];
  /**
   * Optional. Input parameters for the SOURCE cluster element captured by the create wizard
   * (e.g., Airtable BASE_ID/TABLE_ID, HubSpot OBJECT_TYPE). Threaded through to the auto-generated
   * workflow's SOURCE cluster element. Omit for readers with no input properties.
   */
  parameters?: InputMaybe<Scalars['Map']['input']>;
  /**
   * Optional. When omitted, the server picks the first ItemReader cluster element on the
   * source component. Supply a name only when the component defines more than one ItemReader.
   */
  sourceClusterElementName?: InputMaybe<Scalars['String']['input']>;
  sourceComponentName: Scalars['String']['input'];
  sourceComponentVersion: Scalars['Int']['input'];
  /** Phase 17b: tombstone-derivation strategy. Defaults to PERIODIC_FULL_REPLACE when omitted. */
  tombstoneStrategy?: InputMaybe<TombstoneStrategy>;
  workspaceId: Scalars['ID']['input'];
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

/** Input for creating a new organization connection. */
export type CreateOrganizationConnectionInput = {
  componentName: Scalars['String']['input'];
  connectionVersion: Scalars['Int']['input'];
  environmentId: Scalars['Int']['input'];
  name: Scalars['String']['input'];
  parameters: Scalars['Map']['input'];
};

export type CreateWorkspaceAiGatewayModelInput = {
  alias?: InputMaybe<Scalars['String']['input']>;
  capabilities?: InputMaybe<Scalars['String']['input']>;
  contextWindow?: InputMaybe<Scalars['Int']['input']>;
  defaultRoutingPolicyId?: InputMaybe<Scalars['ID']['input']>;
  inputCostPerMTokens?: InputMaybe<Scalars['Float']['input']>;
  name: Scalars['String']['input'];
  outputCostPerMTokens?: InputMaybe<Scalars['Float']['input']>;
  providerId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};

export type CreateWorkspaceAiGatewayProviderInput = {
  apiKey: Scalars['String']['input'];
  baseUrl?: InputMaybe<Scalars['String']['input']>;
  config?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  type: AiGatewayProviderType;
  workspaceId: Scalars['ID']['input'];
};

export type CreateWorkspaceAiGatewayRoutingPolicyInput = {
  config?: InputMaybe<Scalars['String']['input']>;
  fallbackModel?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  strategy: AiGatewayRoutingStrategyType;
  workspaceId: Scalars['ID']['input'];
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

/** A custom permission role (EE) with a user-defined set of permission scopes */
export type CustomRole = {
  __typename?: 'CustomRole';
  createdDate?: Maybe<Scalars['String']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  /** Permission scope names granted by this role (e.g., WORKFLOW_VIEW, EXECUTION_VIEW) */
  scopes: Array<Scalars['String']['output']>;
};

/**
 * Slim DTO returned by dataStreamCompatibleConnections so the connection picker can render
 * without coupling to the full Connection type.
 */
export type DataStreamCompatibleConnection = {
  __typename?: 'DataStreamCompatibleConnection';
  /**
   * Component name, resolved via connectionDefinition.componentName (NOT
   * componentConnection.componentVersion — the two often differ; use the connection definition
   * as the authoritative source).
   */
  componentName: Scalars['String']['output'];
  componentVersion: Scalars['Int']['output'];
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
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

export type DeleteAiHubTaskArtifactInput = {
  artifactId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
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

export type ExperimentComparisonRow = {
  __typename?: 'ExperimentComparisonRow';
  datasetItemId: Scalars['ID']['output'];
  runsByExperiment: Array<ExperimentRunPoint>;
};

export type ExperimentComparisonView = {
  __typename?: 'ExperimentComparisonView';
  aggregateScoreDeltas: Array<AggregateScoreDelta>;
  experiments: Array<ExperimentSummary>;
  rows: Array<ExperimentComparisonRow>;
};

export type ExperimentRunPoint = {
  __typename?: 'ExperimentRunPoint';
  cost?: Maybe<Scalars['Float']['output']>;
  experimentId: Scalars['ID']['output'];
  latencyMs?: Maybe<Scalars['Int']['output']>;
  runId: Scalars['ID']['output'];
  scores: Array<ScorePoint>;
  status: Scalars['String']['output'];
  traceId?: Maybe<Scalars['ID']['output']>;
};

export type ExperimentScoreAverage = {
  __typename?: 'ExperimentScoreAverage';
  average?: Maybe<Scalars['Float']['output']>;
  count: Scalars['Int']['output'];
  experimentId: Scalars['ID']['output'];
};

export type ExperimentSummary = {
  __typename?: 'ExperimentSummary';
  averageLatencyMs?: Maybe<Scalars['Int']['output']>;
  completedRuns: Scalars['Int']['output'];
  failedRuns: Scalars['Int']['output'];
  id: Scalars['ID']['output'];
  model?: Maybe<Scalars['String']['output']>;
  totalCost?: Maybe<Scalars['Float']['output']>;
  totalRuns: Scalars['Int']['output'];
};

export type Field = {
  __typename?: 'Field';
  label?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  /**
   * Java class simple name of the field's type, e.g. String, Long, Instant. Used by the wizard to
   * auto-prefill the indexed-field-type selector (String → TEXT, Long/Integer/Double/Float → NUMERIC,
   * Instant/LocalDateTime/LocalDate → TIMESTAMP, anything else → TEXT). User can override.
   */
  type?: Maybe<Scalars['String']['output']>;
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
  maxPages?: InputMaybe<Scalars['Int']['input']>;
  name: Scalars['String']['input'];
  userPrompt?: InputMaybe<Scalars['String']['input']>;
};

export type GeneratePropertyValueInput = {
  dynamic: Scalars['Boolean']['input'];
  environmentId: Scalars['Int']['input'];
  mode: PropertyCopilotMode;
  prompt: Scalars['String']['input'];
  propertyPath: Scalars['String']['input'];
  propertyType?: InputMaybe<Scalars['String']['input']>;
  workflowId: Scalars['ID']['input'];
  workflowNodeName: Scalars['String']['input'];
};

export type GeneratePropertyValuePayload = {
  __typename?: 'GeneratePropertyValuePayload';
  message?: Maybe<Scalars['String']['output']>;
  valid: Scalars['Boolean']['output'];
  value: Scalars['String']['output'];
};

export type GenerateSampleOutputInput = {
  environmentId: Scalars['Int']['input'];
  prompt: Scalars['String']['input'];
  workflowId: Scalars['ID']['input'];
};

export type GenerateSampleOutputPayload = {
  __typename?: 'GenerateSampleOutputPayload';
  message?: Maybe<Scalars['String']['output']>;
  valid: Scalars['Boolean']['output'];
  value: Scalars['String']['output'];
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

export type GenerateWorkflowDescriptionInput = {
  environmentId: Scalars['Int']['input'];
  workflowId: Scalars['ID']['input'];
  workflowNodeName?: InputMaybe<Scalars['String']['input']>;
};

export type GenerateWorkflowDescriptionPayload = {
  __typename?: 'GenerateWorkflowDescriptionPayload';
  value: Scalars['String']['output'];
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
  permissionExpression?: Maybe<Scalars['String']['output']>;
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
  permissionExpression?: Maybe<Scalars['String']['output']>;
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
  deletedAt?: Maybe<Scalars['Long']['output']>;
  document?: Maybe<FileEntry>;
  id: Scalars['ID']['output'];
  lastSeenAt?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  sourceId?: Maybe<Scalars['ID']['output']>;
  sourceRecordId?: Maybe<Scalars['String']['output']>;
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

export type KnowledgeBaseSource = {
  __typename?: 'KnowledgeBaseSource';
  cadence: Scalars['String']['output'];
  connectionId?: Maybe<Scalars['ID']['output']>;
  enabled: Scalars['Boolean']['output'];
  /**
   * Phase 17b: optional rare full-replace cadence paired with the regular incremental cadence.
   * Null = single-trigger MVP behavior.
   */
  fullReplaceCadence?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  knowledgeBaseId: Scalars['ID']['output'];
  lastSyncJobExecutionId?: Maybe<Scalars['ID']['output']>;
  lastSyncRunAt?: Maybe<Scalars['Long']['output']>;
  /**
   * Optional metadata-tag whitelist controlling which incoming metadata fields are flattened
   * into KB document tags. {fields: [...]} narrows; null preserves MVP behavior (every field
   * becomes a tag). Mirrors ContextStoreEntity.storedFields.
   */
  metadataFields?: Maybe<Scalars['Map']['output']>;
  name: Scalars['String']['output'];
  sourceClusterElementName?: Maybe<Scalars['String']['output']>;
  sourceComponentName: Scalars['String']['output'];
  sourceComponentVersion: Scalars['Int']['output'];
  status: KnowledgeBaseSourceStatus;
  /**
   * Phase 17b: tombstone-derivation strategy. PERIODIC_FULL_REPLACE pairs with fullReplaceCadence,
   * UPSTREAM_CHANGE_FEED is reserved for components that emit deletion events, NONE makes the source
   * append-only.
   */
  tombstoneStrategy: TombstoneStrategy;
  workflowId?: Maybe<Scalars['String']['output']>;
  workspaceId?: Maybe<Scalars['ID']['output']>;
};

export type KnowledgeBaseSourceFilter = {
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
};

export enum KnowledgeBaseSourceStatus {
  BuildingPreview = 'BUILDING_PREVIEW',
  Disabled = 'DISABLED',
  Failed = 'FAILED',
  Preview = 'PREVIEW',
  Ready = 'READY'
}

export type KnowledgeBaseTagsEntry = {
  __typename?: 'KnowledgeBaseTagsEntry';
  knowledgeBaseId: Scalars['ID']['output'];
  tags: Array<Tag>;
};

export type LicenceType = {
  __typename?: 'LicenceType';
  allowedJobs: Scalars['Long']['output'];
  currentMonthJobUsage: Scalars['Long']['output'];
  expiresAt?: Maybe<Scalars['String']['output']>;
  features: Array<Scalars['String']['output']>;
  holderEmail?: Maybe<Scalars['String']['output']>;
  holderName?: Maybe<Scalars['String']['output']>;
  id?: Maybe<Scalars['String']['output']>;
  issuedAt?: Maybe<Scalars['String']['output']>;
  maxUsers?: Maybe<Scalars['Int']['output']>;
  status: Scalars['String']['output'];
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
  acknowledgeAiObservabilityAlertEvent?: Maybe<AiObservabilityAlertEvent>;
  /**
   * Register a new MCP server for the current user. authToken is optional (sent as a bearer token); it is
   * encrypted at rest. Returns the new server id.
   */
  addAiHubMcpServer: Scalars['ID']['output'];
  /**
   * Adds a (kind, resourceId) reference to the agent's resource template. Idempotent — adding the same resource
   * twice returns the existing row. Future tasks spawned from this agent start with this resource attached.
   */
  addAiHubPersonalAgentResource: AiHubPersonalAgentResource;
  /**
   * Adds a (componentName, componentVersion, operationName) triple to the agent's tool template. Idempotent — adding
   * the same tool twice returns the existing row rather than producing a duplicate. Future aiHubTasks spawned
   * from this agent will start with this tool attached.
   */
  addAiHubPersonalAgentTool: AiHubPersonalAgentTool;
  /**
   * Add a pre-built connector to the current user's AI Hub (a user-global "added connector"). Idempotent —
   * re-adding refreshes the connection in place. Returns the connector id.
   */
  addAiHubUserConnector: Scalars['ID']['output'];
  addDataTableColumn: Scalars['Boolean']['output'];
  /** Add a user to a workspace. Requires ADMIN workspace role. */
  addWorkspaceUser: WorkspaceUser;
  /**
   * Attach a tool to a task. Idempotent — re-attaching the same (component, action, connection)
   * upserts the tool's parameters in place. Returns the persisted ids so the client can immediately
   * address the row in subsequent updates / removes.
   */
  attachAiHubTaskTool: AiHubTaskToolBinding;
  /**
   * Bulk-archives workflow-chat aiHubTasks for the given workspace. Returns the number of rows that flipped
   * to ARCHIVED. Tasks already ARCHIVED are skipped (idempotent); aiHubTasks the caller doesn't own
   * are silently filtered (the service-layer ownership check rejects them per row, but we don't surface
   * individual failures — the caller asked to archive everything, partial successes are acceptable).
   *
   * Designed for the "I have 30 workflow chats clogging my sidebar" cleanup case. The caller passes the kind
   * discriminator so this can later be extended to bulk-archive standard aiHubTasks too without overloading
   * the same mutation; for now only WORKFLOW_CHAT is supported.
   */
  bulkArchiveWorkflowChatAiHubTasks: Scalars['Int']['output'];
  cancelAiAgentEvalRun: AiAgentEvalRun;
  /**
   * Cancels an in-flight LLM agent run for a STANDARD or PERSONAL_AGENT task. Companion to
   * {@code cancelWorkflowChatTurn}, which targets workflow-chat tasks bound to a workflow execution.
   * The server marks the run terminated in the in-flight registry and emits a complete signal to any
   * SSE subscribers; a subsequent mount-time probe sees the task as not-in-flight so the client stops
   * showing the streaming UI.
   *
   * Returns {@code true} when a non-terminated run was cancelled, {@code false} when none was in flight
   * (idempotent — the user may click stop after the run finished, and we want the client to disambiguate
   * that from a successful cancel without complex error handling). Throws Forbidden when the caller does
   * not own the task.
   *
   * The optional runId is the AG-UI runId of the turn being stopped; when supplied the server
   * tombstones it so a Stop that reaches the server before the agent run registers still sticks.
   */
  cancelAiHubRun: Scalars['Boolean']['output'];
  cancelAiObservabilityExportJob?: Maybe<AiObservabilityExportJob>;
  cancelGenerationJob: Scalars['Boolean']['output'];
  /**
   * Cancels the in-flight workflow-chat turn for the given task. Returns {@code true} if a job was
   * cancelled, {@code false} when the task has no running turn (idempotent — the user may click stop
   * after the workflow already completed, and we want the client to be able to disambiguate that from a
   * successful cancel without complex error handling).
   *
   * Resolves through the per-task jobId registry that AgUiStreamBridge populates from the executor's
   * start event. Throws Forbidden when the caller doesn't own the task.
   */
  cancelWorkflowChatTurn: Scalars['Boolean']['output'];
  createAiAgentEvalScenario: AiAgentEvalScenario;
  createAiAgentEvalTest: AiAgentEvalTest;
  createAiAgentJudge: AiAgentJudge;
  createAiAgentScenarioJudge: AiAgentScenarioJudge;
  createAiAgentScenarioToolSimulation: AiAgentScenarioToolSimulation;
  createAiEvalRule?: Maybe<AiEvalRule>;
  createAiEvalScore?: Maybe<AiEvalScore>;
  createAiEvalScoreConfig?: Maybe<AiEvalScoreConfig>;
  createAiGatewayBudget?: Maybe<AiGatewayBudget>;
  createAiGatewayModel?: Maybe<AiGatewayModel>;
  createAiGatewayProject?: Maybe<AiGatewayProject>;
  createAiGatewayProvider?: Maybe<AiGatewayProvider>;
  createAiGatewayRateLimit?: Maybe<AiGatewayRateLimit>;
  createAiGatewayRoutingPolicy?: Maybe<AiGatewayRoutingPolicy>;
  /**
   * Creates a new personal agent. The name field is auto-slugified server-side; pass the user's free text and
   * let the service produce the canonical slug. Conflict on a duplicate slug throws a typed error so the client
   * can prompt the user for a different name.
   */
  createAiHubPersonalAgent: AiHubPersonalAgent;
  /**
   * Creates a fresh personal-agent task row for the current user (always-new semantics, May 2026). Past
   * aiHubTasks bound to the same agent remain reachable through the aiHubTasks list. Returns the new row
   * along with any tool template rows auto-attached from the agent's `tools` list.
   */
  createAiHubPersonalAgentTask: AiHubTask;
  /**
   * Creates a new task, or returns the existing one if the same {@code threadId} is reused. Idempotent
   * on (workspace, user, environment, threadId).
   */
  createAiHubTask: AiHubTask;
  createAiObservabilityAlertRule?: Maybe<AiObservabilityAlertRule>;
  createAiObservabilityExportJob?: Maybe<AiObservabilityExportJob>;
  createAiObservabilityNotificationChannel?: Maybe<AiObservabilityNotificationChannel>;
  createAiObservabilityWebhookSubscription?: Maybe<AiObservabilityWebhookSubscription>;
  createAiPrompt?: Maybe<AiPrompt>;
  createAiPromptVersion?: Maybe<AiPromptVersion>;
  createAiSkill: AiSkill;
  createAiSkillFromInstructions: AiSkill;
  createApiConnector: ApiConnector;
  createApiKey: Scalars['String']['output'];
  createApprovalTask?: Maybe<ApprovalTask>;
  createAutomationWorkflowProject: Scalars['ID']['output'];
  createAutomationWorkflowProjectWorkflow: Scalars['ID']['output'];
  createContextStore: ContextStore;
  createContextStoreSource: ContextStoreSource;
  /** Create a new custom role with the given scopes. Requires tenant admin. */
  createCustomRole: CustomRole;
  createDataTable: Scalars['Boolean']['output'];
  createEmbeddedMcpServer?: Maybe<McpServer>;
  createIdentityProvider: IdentityProviderType;
  createKnowledgeBase?: Maybe<KnowledgeBase>;
  createKnowledgeBaseSource: KnowledgeBaseSource;
  createMcpComponent?: Maybe<McpComponent>;
  createMcpComponentWithTools?: Maybe<McpComponent>;
  createMcpIntegrationInstanceConfiguration?: Maybe<McpIntegrationInstanceConfiguration>;
  createMcpIntegrationInstanceConfigurationWorkflow?: Maybe<McpIntegrationInstanceConfigurationWorkflow>;
  createMcpProject?: Maybe<McpProject>;
  createMcpProjectWorkflow?: Maybe<McpProjectWorkflow>;
  createMcpServer?: Maybe<McpServer>;
  createMcpTool?: Maybe<McpTool>;
  /** Create a new connection with ORGANIZATION visibility. (admin only, EE only) */
  createOrganizationConnection: Scalars['ID']['output'];
  /**
   * Creates (or returns the existing) workflow-chat task bound to the given workflow execution. Idempotent
   * on (workspace, user, environment, workflowExecutionId): re-clicking the same workflow-chat sidebar row restores
   * the existing thread instead of creating a duplicate. Returns a row with {@code kind = WORKFLOW_CHAT}.
   *
   * Optional {@code title} is persisted on first creation (when the row doesn't yet exist) so workflow-chat
   * aiHubTasks get a meaningful sidebar label without waiting for the LLM-driven title generation that the
   * bridge bypasses. Pass e.g. "{projectName} — {workflowLabel}" from the client. The title is NOT overwritten on
   * the idempotency path — once a row has been named, that name sticks.
   */
  createWorkflowChatAiHubTask: AiHubTask;
  createWorkspaceAiGatewayModel?: Maybe<AiGatewayModel>;
  createWorkspaceAiGatewayProvider?: Maybe<AiGatewayProvider>;
  createWorkspaceAiGatewayRoutingPolicy?: Maybe<AiGatewayRoutingPolicy>;
  createWorkspaceApiKey: Scalars['String']['output'];
  createWorkspaceMcpServer?: Maybe<McpServer>;
  deleteAiAgentEvalScenario: Scalars['Boolean']['output'];
  deleteAiAgentEvalTest: Scalars['Boolean']['output'];
  deleteAiAgentJudge: Scalars['Boolean']['output'];
  deleteAiAgentScenarioJudge: Scalars['Boolean']['output'];
  deleteAiAgentScenarioToolSimulation: Scalars['Boolean']['output'];
  /**
   * Deletes a memory by primary key. Returns true on success; throws NotFound when the row does not exist or
   * Forbidden when the row belongs to another user.
   */
  deleteAiAutoMemory: Scalars['Boolean']['output'];
  deleteAiEvalRule?: Maybe<Scalars['Boolean']['output']>;
  deleteAiEvalScore?: Maybe<Scalars['Boolean']['output']>;
  deleteAiEvalScoreConfig?: Maybe<Scalars['Boolean']['output']>;
  deleteAiGatewayBudget?: Maybe<Scalars['Boolean']['output']>;
  deleteAiGatewayModel?: Maybe<Scalars['Boolean']['output']>;
  deleteAiGatewayProject?: Maybe<Scalars['Boolean']['output']>;
  deleteAiGatewayProvider?: Maybe<Scalars['Boolean']['output']>;
  deleteAiGatewayRateLimit?: Maybe<Scalars['Boolean']['output']>;
  deleteAiGatewayRoutingPolicy?: Maybe<Scalars['Boolean']['output']>;
  /**
   * Deletes a personal agent. Tasks bound to the agent are NOT cascaded — past chat history remains
   * accessible, but new turns degrade to plain ai-hub behaviour. Returns true on success; throws when the
   * agent does not exist or is owned by another user.
   */
  deleteAiHubPersonalAgent: Scalars['Boolean']['output'];
  /**
   * Hard-deletes a task and removes its messages from the chat-memory table. Associated artifacts
   * cascade via the database FK. Returns true on success; throws Forbidden when the caller is not the
   * task's owner.
   */
  deleteAiHubTask: Scalars['Boolean']['output'];
  /**
   * Removes a user-attached reference artifact (FILE_REFERENCED / WORKFLOW_REFERENCED /
   * DATA_TABLE_REFERENCED / KB_REFERENCED) from a task. Returns `true` on success or when the row was
   * already gone (idempotent). Throws on ownership / workspace mismatch and on any non-reference kind —
   * those are agent-driven audit rows that the user cannot delete via this surface.
   */
  deleteAiHubTaskArtifact: Scalars['Boolean']['output'];
  deleteAiObservabilityAlertRule?: Maybe<Scalars['Boolean']['output']>;
  deleteAiObservabilityNotificationChannel?: Maybe<Scalars['Boolean']['output']>;
  deleteAiObservabilityWebhookSubscription?: Maybe<Scalars['Boolean']['output']>;
  deleteAiPrompt?: Maybe<Scalars['Boolean']['output']>;
  deleteAiSkill: Scalars['Boolean']['output'];
  deleteApiConnector: Scalars['Boolean']['output'];
  deleteApiKey: Scalars['Boolean']['output'];
  deleteApprovalTask?: Maybe<Scalars['Boolean']['output']>;
  deleteAssetFile: Scalars['Boolean']['output'];
  deleteAutomationWorkflowProject: Scalars['Boolean']['output'];
  deleteAutomationWorkflowProjectWorkflow: Scalars['Boolean']['output'];
  deleteConnectedUserMcpServer?: Maybe<Scalars['Boolean']['output']>;
  deleteConnectedUserProjectWorkflow?: Maybe<Scalars['Boolean']['output']>;
  deleteContextStore: Scalars['Boolean']['output'];
  deleteContextStoreSource: Scalars['Boolean']['output'];
  deleteCustomComponent: Scalars['Boolean']['output'];
  /** Delete a custom role. Fails if the role is in use by any project member. Requires tenant admin. */
  deleteCustomRole: Scalars['Boolean']['output'];
  deleteDataTableRow: Scalars['Boolean']['output'];
  deleteEmbeddedMcpServer?: Maybe<Scalars['Boolean']['output']>;
  deleteIdentityProvider: Scalars['Boolean']['output'];
  deleteJobFileLogs: Scalars['Boolean']['output'];
  deleteKnowledgeBase?: Maybe<Scalars['Boolean']['output']>;
  deleteKnowledgeBaseDocument?: Maybe<Scalars['Boolean']['output']>;
  deleteKnowledgeBaseDocumentChunk?: Maybe<Scalars['Boolean']['output']>;
  deleteKnowledgeBaseSource: Scalars['Boolean']['output'];
  deleteLicence: Scalars['Boolean']['output'];
  deleteMcpComponent?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpIntegrationInstanceConfiguration?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpIntegrationInstanceConfigurationWorkflow?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpProject?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpProjectWorkflow?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpServer?: Maybe<Scalars['Boolean']['output']>;
  deleteMcpTool?: Maybe<Scalars['Boolean']['output']>;
  /** Delete an organization connection. Fails if the connection is not ORGANIZATION-scoped. (admin only, EE only) */
  deleteOrganizationConnection: Scalars['Boolean']['output'];
  deleteSharedProject: Scalars['Boolean']['output'];
  deleteSharedWorkflow: Scalars['Boolean']['output'];
  deleteUser: Scalars['Boolean']['output'];
  deleteWorkspaceAiGatewayModel?: Maybe<Scalars['Boolean']['output']>;
  deleteWorkspaceAiGatewayProvider?: Maybe<Scalars['Boolean']['output']>;
  deleteWorkspaceAiGatewayRoutingPolicy?: Maybe<Scalars['Boolean']['output']>;
  deleteWorkspaceApiKey: Scalars['Boolean']['output'];
  deleteWorkspaceMcpServer?: Maybe<Scalars['Boolean']['output']>;
  /** Demote a connection to PRIVATE visibility. Authorized for workspace administrators OR the connection creator (orphan-recovery path when no admins remain). Fails if the connection is used by active deployments. (EE only) */
  demoteConnectionToPrivate: Scalars['Boolean']['output'];
  /**
   * Detach a whole component binding (cascades to all its tools via the FK). Use when the user wants to
   * remove all of e.g. Slack's tools at once instead of one at a time.
   */
  detachAiHubTaskComponent: Scalars['Boolean']['output'];
  /** Unlink a connection from all deployed workflows and test configurations, without deleting the connection itself. */
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
  /**
   * Generates a title for a task using the AI title-generation service, then saves it. Idempotent —
   * if the task already has a non-blank title, returns the current state without calling the LLM.
   * Throws when the upstream model is unavailable so the client can surface a retryable error.
   */
  generateAiHubTaskTitle: AiHubTask;
  generateFromDocumentation: ApiConnector;
  generatePropertyValue: GeneratePropertyValuePayload;
  generateSampleOutput: GenerateSampleOutputPayload;
  generateSpecification: GenerateSpecificationResponse;
  generateWorkflowDescription: GenerateWorkflowDescriptionPayload;
  importDataTableCsv: Scalars['Boolean']['output'];
  importOpenApiSpecification: ApiConnector;
  importProjectTemplate: Scalars['ID']['output'];
  importWorkflowTemplate: Scalars['ID']['output'];
  insertDataTableRow: DataTableRow;
  instantiateAiEvalTemplate?: Maybe<AiEvalRule>;
  inviteUser: Scalars['Boolean']['output'];
  /** Mark all of a user's connections as pending reassignment. Returns per-row outcome so partial failures surface; a silent no-op batch does not look like an error. (admin only) */
  markConnectionsPendingReassignment: BulkReassignResult;
  playgroundChatCompletion?: Maybe<PlaygroundChatCompletionResponse>;
  /** Promote every PRIVATE connection in the workspace to WORKSPACE visibility. Returns per-row outcome so partial failures can surface. Intended for CE→EE migration. (admin only, EE only) */
  promoteAllPrivateConnectionsToWorkspace: BulkPromoteResult;
  /** Promote a connection to WORKSPACE visibility, making it visible to all workspace members. (admin only, EE only) */
  promoteConnectionToWorkspace: Scalars['Boolean']['output'];
  publishAutomationWorkflowProject: Scalars['Boolean']['output'];
  /** Reassign all of a user's unresolved connections to a new owner. (admin only) */
  reassignAllConnections: Scalars['Boolean']['output'];
  /** Reassign a single connection to a new owner. Resets status to ACTIVE if pending. (admin only) */
  reassignConnection: Scalars['Boolean']['output'];
  /**
   * Records a user-attached reference (file / workflow / data table / knowledge base) as a task
   * artifact so it appears in the sidebar artifact list. Idempotent — re-attaching the same resource hits
   * the existing row instead of creating a duplicate. Used by the composer plus-button menu when the user
   * references an artifact in a task.
   */
  recordReferencedAiHubTaskArtifact: AiHubTaskArtifact;
  refreshContextStoreSource: Scalars['ID']['output'];
  refreshKnowledgeBaseSource: Scalars['ID']['output'];
  /** Register an existing connection backed by an externally-provisioned credential (e.g. AWS Secrets Manager). */
  registerExistingConnection: Scalars['Long']['output'];
  /** Remove an MCP server. Idempotent — returns false when not found/owned. */
  removeAiHubMcpServer: Scalars['Boolean']['output'];
  /**
   * Removes a resource from the agent's template by row id. Already-spawned tasks keep their copied artifact rows;
   * only future tasks are affected. Idempotent — removing a non-existent id is a no-op.
   */
  removeAiHubPersonalAgentResource: Scalars['Boolean']['output'];
  /**
   * Removes a tool from the agent's template by id. Already-attached aiHubTasks keep their copied tool rows;
   * only future aiHubTasks are affected. Idempotent — removing a non-existent id is a no-op.
   */
  removeAiHubPersonalAgentTool: Scalars['Boolean']['output'];
  /** Detach a single tool. Returns true on success; false when the id was not found (idempotent). */
  removeAiHubTaskTool: Scalars['Boolean']['output'];
  /** Remove a user connector (cascades its tool config). Idempotent — returns false when not found/owned. */
  removeAiHubUserConnector: Scalars['Boolean']['output'];
  removeDataTableColumn: Scalars['Boolean']['output'];
  /** Remove a user from a workspace. Requires ADMIN workspace role. */
  removeWorkspaceUser: Scalars['Boolean']['output'];
  renameDataTable: Scalars['Boolean']['output'];
  renameDataTableColumn: Scalars['Boolean']['output'];
  runAiEvalRuleOnHistoricalTraces?: Maybe<Scalars['Int']['output']>;
  saveClusterElementTestConfigurationConnection?: Maybe<Scalars['Boolean']['output']>;
  saveClusterElementTestOutput?: Maybe<WorkflowNodeTestOutputResult>;
  saveWorkflowTestConfigurationConnection?: Maybe<Scalars['Boolean']['output']>;
  setActiveAiPromptVersion?: Maybe<Scalars['Boolean']['output']>;
  /** Toggle an MCP server on/off. */
  setAiHubMcpServerEnabled: Scalars['Boolean']['output'];
  /** Toggle a single tool of an MCP server on/off. */
  setAiHubMcpServerToolEnabled: Scalars['Boolean']['output'];
  /**
   * Upserts or deletes the agent's single schedule.
   * - input.schedule != null  → upsert (insert if absent, update if present).
   * - input.schedule == null  → delete any existing schedule + cancel Quartz.
   * Returns the agent with its (possibly null) schedule field populated.
   */
  setAiHubPersonalAgentSchedule: AiHubPersonalAgent;
  /** Toggle a user connector on/off (the component-level enabled flag). */
  setAiHubUserConnectorEnabled: Scalars['Boolean']['output'];
  /** Toggle a single tool within a user connector on/off. */
  setAiHubUserConnectorToolEnabled: Scalars['Boolean']['output'];
  /** Set the pre-configured parameter values for a single tool within a user connector. */
  setAiHubUserConnectorToolParameters: Scalars['Boolean']['output'];
  setAiObservabilityTraceTags?: Maybe<AiObservabilityTrace>;
  setContextStoreSourceEnabled: ContextStoreSource;
  setKnowledgeBaseSourceEnabled: KnowledgeBaseSource;
  snoozeAiObservabilityAlertRule?: Maybe<AiObservabilityAlertRule>;
  startAiAgentEvalRun: AiAgentEvalRun;
  startDiscoverEndpoints: EndpointDiscoveryResult;
  startGenerateForEndpoints: GenerationJobStatus;
  startGenerateFromDocumentationPreview: GenerationJobStatus;
  testAiObservabilityAlertRule?: Maybe<Scalars['Float']['output']>;
  testAiObservabilityNotificationChannel?: Maybe<Scalars['Boolean']['output']>;
  testAiObservabilityWebhookSubscription?: Maybe<Scalars['Boolean']['output']>;
  testClusterElementScript: ScriptTestExecution;
  testWorkflowNodeScript: ScriptTestExecution;
  testWorkspaceAiGatewayProviderConnection?: Maybe<ProviderConnectionResult>;
  /**
   * Truncates the chat-memory history for a task, deleting the message at {@code fromMessageIndex} and
   * every message after it. Used by the edit-and-resend UX: the user clicks edit on a previous user message,
   * the client truncates here, and the next runAgent call re-runs from the edited message.
   *
   * Returns the number of messages deleted. Idempotent — calling with an index past the end deletes zero.
   * Throws Forbidden when the caller does not own the task.
   */
  truncateAiHubTaskMessages: Scalars['Int']['output'];
  unsnoozeAiObservabilityAlertRule?: Maybe<AiObservabilityAlertRule>;
  updateAiAgentEvalScenario: AiAgentEvalScenario;
  updateAiAgentEvalTest: AiAgentEvalTest;
  updateAiAgentJudge: AiAgentJudge;
  updateAiAgentScenarioJudge: AiAgentScenarioJudge;
  updateAiAgentScenarioToolSimulation: AiAgentScenarioToolSimulation;
  /**
   * Partial update of a memory by primary key, scoped to (workspaceId, currentUserId). The memory's environment
   * is immutable post-create so it does not appear in the patch input — environments do not move.
   */
  updateAiAutoMemory: AiAutoMemory;
  updateAiEvalRule?: Maybe<AiEvalRule>;
  updateAiEvalScoreConfig?: Maybe<AiEvalScoreConfig>;
  updateAiGatewayBudget?: Maybe<AiGatewayBudget>;
  updateAiGatewayModel?: Maybe<AiGatewayModel>;
  updateAiGatewayProject?: Maybe<AiGatewayProject>;
  updateAiGatewayProvider?: Maybe<AiGatewayProvider>;
  updateAiGatewayRateLimit?: Maybe<AiGatewayRateLimit>;
  updateAiGatewayRoutingPolicy?: Maybe<AiGatewayRoutingPolicy>;
  updateAiGatewayWorkspaceSettings?: Maybe<AiGatewayWorkspaceSettings>;
  /**
   * Partial update of an existing personal agent's editable fields (title, description, instructions). The
   * name is intentionally NOT mutable — chat URLs and tool references key off it; renames need a dedicated
   * operation that doesn't yet exist.
   */
  updateAiHubPersonalAgent: AiHubPersonalAgent;
  /**
   * Updates the per-tool config (pinned connection + pre-set parameters) on an existing tool template row.
   * Tasks already spawned from this agent keep their copied config — only future aiHubTasks pick up
   * the new values. Both fields are independently nullable in the input: omit `connectionId` to clear the pin;
   * omit `parameters` to preserve the existing map (pass an explicit empty map to reset).
   */
  updateAiHubPersonalAgentToolConfig: AiHubPersonalAgentTool;
  /**
   * Partial update of a task by primary key. At least one of {@code title}, {@code lastPreview},
   * {@code messageCount}, {@code status} must be non-null — an all-null patch is rejected with an error.
   */
  updateAiHubTask: AiHubTask;
  /**
   * Update only the parameters of a previously attached tool (e.g. user adjusts the default channel
   * after attach). Other fields on the binding (component, action, connection) are immutable post-attach;
   * if the user wants to switch connection they should detach + reattach instead.
   */
  updateAiHubTaskToolParameters: AiHubTaskToolBinding;
  updateAiHubVoiceWebhookUrl?: Maybe<AiHubWorkspaceSettings>;
  updateAiObservabilityAlertRule?: Maybe<AiObservabilityAlertRule>;
  updateAiObservabilityNotificationChannel?: Maybe<AiObservabilityNotificationChannel>;
  updateAiObservabilityWebhookSubscription?: Maybe<AiObservabilityWebhookSubscription>;
  updateAiPrompt?: Maybe<AiPrompt>;
  updateAiSkill: AiSkill;
  updateAiSkillContent: AiSkill;
  updateApiConnector: ApiConnector;
  updateApiKey: Scalars['Boolean']['output'];
  updateApprovalTask?: Maybe<ApprovalTask>;
  updateAssetFile: AssetFile;
  updateAssetFileTags: AssetFile;
  updateAssetFileTextContent: AssetFile;
  updateAutomationWorkflowProject: Scalars['Boolean']['output'];
  updateAutomationWorkflowProjectWorkflow: Scalars['Boolean']['output'];
  updateAutomationWorkflowProjectWorkflowPermissionExpression: Scalars['Boolean']['output'];
  /** Enables or disables a component tenant-wide. Admin-only. */
  updateComponentPolicy: ComponentPolicy;
  updateContextStore: ContextStore;
  updateContextStoreSource: ContextStoreSource;
  /**
   * Replace the tag list on a Context Store. Existing tags whose name matches an entry in {@code tags} are
   * re-used; names not yet in the tag table are created on the fly via TagService.save. Returns the updated tags
   * so the client can refresh its remainingTags cache.
   */
  updateContextStoreTags: Array<Tag>;
  /** Update an existing custom role. Requires tenant admin. */
  updateCustomRole: CustomRole;
  updateDataTableRow: DataTableRow;
  updateDataTableTags: Scalars['Boolean']['output'];
  updateIdentityProvider: IdentityProviderType;
  updateIntegrationWorkflowPermissionExpression?: Maybe<IntegrationWorkflow>;
  updateKnowledgeBase?: Maybe<KnowledgeBase>;
  updateKnowledgeBaseDocumentChunk?: Maybe<KnowledgeBaseDocumentChunk>;
  updateKnowledgeBaseDocumentTags: Scalars['Boolean']['output'];
  updateKnowledgeBaseSource: KnowledgeBaseSource;
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
  /** Update an organization connection's name and tags. (admin only, EE only) */
  updateOrganizationConnection: Scalars['Boolean']['output'];
  updateUser: AdminUser;
  updateWorkspaceAiGatewayModel?: Maybe<AiGatewayModel>;
  updateWorkspaceAiGatewayProvider?: Maybe<AiGatewayProvider>;
  updateWorkspaceAiGatewayRoutingPolicy?: Maybe<AiGatewayRoutingPolicy>;
  updateWorkspaceApiKey: Scalars['Boolean']['output'];
  /** Update a workspace user's role. Requires ADMIN workspace role. */
  updateWorkspaceUserRole: WorkspaceUser;
  uploadLicence: LicenceType;
};


export type MutationAcknowledgeAiObservabilityAlertEventArgs = {
  id: Scalars['ID']['input'];
};


export type MutationAddAiHubMcpServerArgs = {
  authToken?: InputMaybe<Scalars['String']['input']>;
  environment: Scalars['Int']['input'];
  name: Scalars['String']['input'];
  url: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationAddAiHubPersonalAgentResourceArgs = {
  input: AddAiHubPersonalAgentResourceInput;
};


export type MutationAddAiHubPersonalAgentToolArgs = {
  input: AddAiHubPersonalAgentToolInput;
};


export type MutationAddAiHubUserConnectorArgs = {
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
  connectionId?: InputMaybe<Scalars['ID']['input']>;
  environment: Scalars['Int']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationAddDataTableColumnArgs = {
  input: AddColumnInput;
};


export type MutationAddWorkspaceUserArgs = {
  role: WorkspaceRole;
  userId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationAttachAiHubTaskToolArgs = {
  input: AttachAiHubTaskToolInput;
};


export type MutationBulkArchiveWorkflowChatAiHubTasksArgs = {
  environment: Scalars['Int']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationCancelAiAgentEvalRunArgs = {
  id: Scalars['ID']['input'];
};


export type MutationCancelAiHubRunArgs = {
  id: Scalars['ID']['input'];
  runId?: InputMaybe<Scalars['String']['input']>;
  workspaceId: Scalars['ID']['input'];
};


export type MutationCancelAiObservabilityExportJobArgs = {
  id: Scalars['ID']['input'];
};


export type MutationCancelGenerationJobArgs = {
  jobId: Scalars['String']['input'];
};


export type MutationCancelWorkflowChatTurnArgs = {
  id: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
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


export type MutationCreateAiEvalRuleArgs = {
  delaySeconds?: InputMaybe<Scalars['Int']['input']>;
  enabled: Scalars['Boolean']['input'];
  filters?: InputMaybe<Scalars['String']['input']>;
  model: Scalars['String']['input'];
  name: Scalars['String']['input'];
  projectId?: InputMaybe<Scalars['ID']['input']>;
  promptTemplate: Scalars['String']['input'];
  samplingRate: Scalars['Float']['input'];
  scoreConfigId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationCreateAiEvalScoreArgs = {
  comment?: InputMaybe<Scalars['String']['input']>;
  dataType: AiEvalScoreDataType;
  name: Scalars['String']['input'];
  source: AiEvalScoreSource;
  spanId?: InputMaybe<Scalars['ID']['input']>;
  stringValue?: InputMaybe<Scalars['String']['input']>;
  traceId: Scalars['ID']['input'];
  value?: InputMaybe<Scalars['Float']['input']>;
  workspaceId: Scalars['ID']['input'];
};


export type MutationCreateAiEvalScoreConfigArgs = {
  categories?: InputMaybe<Scalars['String']['input']>;
  dataType?: InputMaybe<AiEvalScoreDataType>;
  description?: InputMaybe<Scalars['String']['input']>;
  maxValue?: InputMaybe<Scalars['Float']['input']>;
  minValue?: InputMaybe<Scalars['Float']['input']>;
  name: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationCreateAiGatewayBudgetArgs = {
  input: CreateAiGatewayBudgetInput;
};


export type MutationCreateAiGatewayModelArgs = {
  input: CreateAiGatewayModelInput;
};


export type MutationCreateAiGatewayProjectArgs = {
  input: CreateAiGatewayProjectInput;
};


export type MutationCreateAiGatewayProviderArgs = {
  input: CreateAiGatewayProviderInput;
};


export type MutationCreateAiGatewayRateLimitArgs = {
  input: CreateAiGatewayRateLimitInput;
};


export type MutationCreateAiGatewayRoutingPolicyArgs = {
  input: CreateAiGatewayRoutingPolicyInput;
};


export type MutationCreateAiHubPersonalAgentArgs = {
  input: CreateAiHubPersonalAgentInput;
};


export type MutationCreateAiHubPersonalAgentTaskArgs = {
  input: CreateAiHubPersonalAgentTaskInput;
};


export type MutationCreateAiHubTaskArgs = {
  environment: Scalars['Int']['input'];
  threadId: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationCreateAiObservabilityAlertRuleArgs = {
  input: AiObservabilityAlertRuleInput;
};


export type MutationCreateAiObservabilityExportJobArgs = {
  cronExpression?: InputMaybe<Scalars['String']['input']>;
  filters?: InputMaybe<Scalars['String']['input']>;
  format: AiObservabilityExportFormat;
  projectId?: InputMaybe<Scalars['ID']['input']>;
  scope: AiObservabilityExportScope;
  type?: InputMaybe<AiObservabilityExportJobType>;
  workspaceId: Scalars['ID']['input'];
};


export type MutationCreateAiObservabilityNotificationChannelArgs = {
  input: AiObservabilityNotificationChannelInput;
};


export type MutationCreateAiObservabilityWebhookSubscriptionArgs = {
  enabled: Scalars['Boolean']['input'];
  events: Scalars['String']['input'];
  name: Scalars['String']['input'];
  projectId?: InputMaybe<Scalars['ID']['input']>;
  secret?: InputMaybe<Scalars['String']['input']>;
  url: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationCreateAiPromptArgs = {
  input: CreateAiPromptInput;
};


export type MutationCreateAiPromptVersionArgs = {
  input: CreateAiPromptVersionInput;
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
  permissionExpression?: InputMaybe<Scalars['String']['input']>;
  tags?: InputMaybe<Array<Scalars['String']['input']>>;
};


export type MutationCreateAutomationWorkflowProjectWorkflowArgs = {
  definition?: InputMaybe<Scalars['String']['input']>;
  permissionExpression?: InputMaybe<Scalars['String']['input']>;
  projectId: Scalars['ID']['input'];
};


export type MutationCreateContextStoreArgs = {
  environmentId: Scalars['ID']['input'];
  input: CreateContextStoreInput;
  workspaceId: Scalars['ID']['input'];
};


export type MutationCreateContextStoreSourceArgs = {
  input: CreateContextStoreSourceInput;
};


export type MutationCreateCustomRoleArgs = {
  input: CreateCustomRoleInput;
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


export type MutationCreateKnowledgeBaseSourceArgs = {
  input: CreateKnowledgeBaseSourceInput;
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


export type MutationCreateOrganizationConnectionArgs = {
  input: CreateOrganizationConnectionInput;
};


export type MutationCreateWorkflowChatAiHubTaskArgs = {
  environment: Scalars['Int']['input'];
  projectDeploymentId: Scalars['ID']['input'];
  title?: InputMaybe<Scalars['String']['input']>;
  workflowExecutionId: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationCreateWorkspaceAiGatewayModelArgs = {
  input: CreateWorkspaceAiGatewayModelInput;
};


export type MutationCreateWorkspaceAiGatewayProviderArgs = {
  input: CreateWorkspaceAiGatewayProviderInput;
};


export type MutationCreateWorkspaceAiGatewayRoutingPolicyArgs = {
  input: CreateWorkspaceAiGatewayRoutingPolicyInput;
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


export type MutationDeleteAiAutoMemoryArgs = {
  id: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationDeleteAiEvalRuleArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiEvalScoreArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiEvalScoreConfigArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiGatewayBudgetArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiGatewayModelArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiGatewayProjectArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiGatewayProviderArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiGatewayRateLimitArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiGatewayRoutingPolicyArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiHubPersonalAgentArgs = {
  id: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationDeleteAiHubTaskArgs = {
  id: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationDeleteAiHubTaskArtifactArgs = {
  input: DeleteAiHubTaskArtifactInput;
};


export type MutationDeleteAiObservabilityAlertRuleArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiObservabilityNotificationChannelArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiObservabilityWebhookSubscriptionArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteAiPromptArgs = {
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


export type MutationDeleteAssetFileArgs = {
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


export type MutationDeleteContextStoreArgs = {
  id: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationDeleteContextStoreSourceArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteCustomComponentArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteCustomRoleArgs = {
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


export type MutationDeleteKnowledgeBaseSourceArgs = {
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


export type MutationDeleteOrganizationConnectionArgs = {
  connectionId: Scalars['ID']['input'];
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


export type MutationDeleteWorkspaceAiGatewayModelArgs = {
  modelId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationDeleteWorkspaceAiGatewayProviderArgs = {
  providerId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationDeleteWorkspaceAiGatewayRoutingPolicyArgs = {
  routingPolicyId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationDeleteWorkspaceApiKeyArgs = {
  apiKeyId: Scalars['ID']['input'];
};


export type MutationDeleteWorkspaceMcpServerArgs = {
  mcpServerId: Scalars['ID']['input'];
};


export type MutationDemoteConnectionToPrivateArgs = {
  connectionId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationDetachAiHubTaskComponentArgs = {
  taskComponentId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
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


export type MutationGenerateAiHubTaskTitleArgs = {
  id: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationGenerateFromDocumentationArgs = {
  input: GenerateFromDocumentationInput;
};


export type MutationGeneratePropertyValueArgs = {
  input: GeneratePropertyValueInput;
};


export type MutationGenerateSampleOutputArgs = {
  input: GenerateSampleOutputInput;
};


export type MutationGenerateSpecificationArgs = {
  input: GenerateSpecificationInput;
};


export type MutationGenerateWorkflowDescriptionArgs = {
  input: GenerateWorkflowDescriptionInput;
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


export type MutationInstantiateAiEvalTemplateArgs = {
  model: Scalars['String']['input'];
  projectId?: InputMaybe<Scalars['ID']['input']>;
  samplingRate: Scalars['Float']['input'];
  templateKey: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationInviteUserArgs = {
  email: Scalars['String']['input'];
  password: Scalars['String']['input'];
  role: Scalars['String']['input'];
};


export type MutationMarkConnectionsPendingReassignmentArgs = {
  userLogin: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationPlaygroundChatCompletionArgs = {
  input: PlaygroundChatCompletionInput;
};


export type MutationPromoteAllPrivateConnectionsToWorkspaceArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type MutationPromoteConnectionToWorkspaceArgs = {
  connectionId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationPublishAutomationWorkflowProjectArgs = {
  id: Scalars['ID']['input'];
};


export type MutationReassignAllConnectionsArgs = {
  newOwnerLogin: Scalars['String']['input'];
  userLogin: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationReassignConnectionArgs = {
  connectionId: Scalars['ID']['input'];
  newOwnerLogin: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationRecordReferencedAiHubTaskArtifactArgs = {
  input: RecordReferencedAiHubTaskArtifactInput;
};


export type MutationRefreshContextStoreSourceArgs = {
  id: Scalars['ID']['input'];
};


export type MutationRefreshKnowledgeBaseSourceArgs = {
  id: Scalars['ID']['input'];
};


export type MutationRegisterExistingConnectionArgs = {
  input: RegisterExistingConnectionInput;
};


export type MutationRemoveAiHubMcpServerArgs = {
  mcpServerId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationRemoveAiHubPersonalAgentResourceArgs = {
  id: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationRemoveAiHubPersonalAgentToolArgs = {
  toolId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationRemoveAiHubTaskToolArgs = {
  taskToolId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationRemoveAiHubUserConnectorArgs = {
  connectorId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationRemoveDataTableColumnArgs = {
  input: RemoveColumnInput;
};


export type MutationRemoveWorkspaceUserArgs = {
  userId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationRenameDataTableArgs = {
  input: RenameDataTableInput;
};


export type MutationRenameDataTableColumnArgs = {
  input: RenameColumnInput;
};


export type MutationRunAiEvalRuleOnHistoricalTracesArgs = {
  endDate: Scalars['Long']['input'];
  ruleId: Scalars['ID']['input'];
  startDate: Scalars['Long']['input'];
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


export type MutationSetActiveAiPromptVersionArgs = {
  environment: Scalars['String']['input'];
  promptVersionId: Scalars['ID']['input'];
};


export type MutationSetAiHubMcpServerEnabledArgs = {
  enabled: Scalars['Boolean']['input'];
  mcpServerId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationSetAiHubMcpServerToolEnabledArgs = {
  enabled: Scalars['Boolean']['input'];
  mcpServerId: Scalars['ID']['input'];
  toolName: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationSetAiHubPersonalAgentScheduleArgs = {
  input: SetAiHubPersonalAgentScheduleInput;
};


export type MutationSetAiHubUserConnectorEnabledArgs = {
  connectorId: Scalars['ID']['input'];
  enabled: Scalars['Boolean']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationSetAiHubUserConnectorToolEnabledArgs = {
  connectorId: Scalars['ID']['input'];
  enabled: Scalars['Boolean']['input'];
  toolName: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationSetAiHubUserConnectorToolParametersArgs = {
  connectorId: Scalars['ID']['input'];
  parameters: Scalars['Any']['input'];
  toolName: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationSetAiObservabilityTraceTagsArgs = {
  tagIds: Array<Scalars['ID']['input']>;
  traceId: Scalars['ID']['input'];
};


export type MutationSetContextStoreSourceEnabledArgs = {
  enabled: Scalars['Boolean']['input'];
  id: Scalars['ID']['input'];
};


export type MutationSetKnowledgeBaseSourceEnabledArgs = {
  enabled: Scalars['Boolean']['input'];
  id: Scalars['ID']['input'];
};


export type MutationSnoozeAiObservabilityAlertRuleArgs = {
  id: Scalars['ID']['input'];
  until: Scalars['Long']['input'];
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


export type MutationTestAiObservabilityAlertRuleArgs = {
  id: Scalars['ID']['input'];
};


export type MutationTestAiObservabilityNotificationChannelArgs = {
  id: Scalars['ID']['input'];
};


export type MutationTestAiObservabilityWebhookSubscriptionArgs = {
  id: Scalars['ID']['input'];
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


export type MutationTestWorkspaceAiGatewayProviderConnectionArgs = {
  providerId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationTruncateAiHubTaskMessagesArgs = {
  fromMessageIndex: Scalars['Int']['input'];
  id: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationUnsnoozeAiObservabilityAlertRuleArgs = {
  id: Scalars['ID']['input'];
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


export type MutationUpdateAiAutoMemoryArgs = {
  input: UpdateAiAutoMemoryInput;
};


export type MutationUpdateAiEvalRuleArgs = {
  delaySeconds?: InputMaybe<Scalars['Int']['input']>;
  enabled: Scalars['Boolean']['input'];
  filters?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  model: Scalars['String']['input'];
  name: Scalars['String']['input'];
  promptTemplate: Scalars['String']['input'];
  samplingRate: Scalars['Float']['input'];
  scoreConfigId: Scalars['ID']['input'];
};


export type MutationUpdateAiEvalScoreConfigArgs = {
  categories?: InputMaybe<Scalars['String']['input']>;
  dataType?: InputMaybe<AiEvalScoreDataType>;
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  maxValue?: InputMaybe<Scalars['Float']['input']>;
  minValue?: InputMaybe<Scalars['Float']['input']>;
  name: Scalars['String']['input'];
};


export type MutationUpdateAiGatewayBudgetArgs = {
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayBudgetInput;
};


export type MutationUpdateAiGatewayModelArgs = {
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayModelInput;
};


export type MutationUpdateAiGatewayProjectArgs = {
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayProjectInput;
};


export type MutationUpdateAiGatewayProviderArgs = {
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayProviderInput;
};


export type MutationUpdateAiGatewayRateLimitArgs = {
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayRateLimitInput;
};


export type MutationUpdateAiGatewayRoutingPolicyArgs = {
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayRoutingPolicyInput;
};


export type MutationUpdateAiGatewayWorkspaceSettingsArgs = {
  input: AiGatewayWorkspaceSettingsInput;
};


export type MutationUpdateAiHubPersonalAgentArgs = {
  input: UpdateAiHubPersonalAgentInput;
};


export type MutationUpdateAiHubPersonalAgentToolConfigArgs = {
  input: UpdateAiHubPersonalAgentToolConfigInput;
};


export type MutationUpdateAiHubTaskArgs = {
  input: AiHubTaskPatchInput;
};


export type MutationUpdateAiHubTaskToolParametersArgs = {
  parameters: Scalars['Any']['input'];
  taskToolId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationUpdateAiHubVoiceWebhookUrlArgs = {
  input: UpdateAiHubVoiceWebhookUrlInput;
};


export type MutationUpdateAiObservabilityAlertRuleArgs = {
  id: Scalars['ID']['input'];
  input: AiObservabilityAlertRuleInput;
};


export type MutationUpdateAiObservabilityNotificationChannelArgs = {
  id: Scalars['ID']['input'];
  input: AiObservabilityNotificationChannelInput;
};


export type MutationUpdateAiObservabilityWebhookSubscriptionArgs = {
  enabled: Scalars['Boolean']['input'];
  events: Scalars['String']['input'];
  id: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  secret?: InputMaybe<Scalars['String']['input']>;
  url: Scalars['String']['input'];
};


export type MutationUpdateAiPromptArgs = {
  id: Scalars['ID']['input'];
  input: UpdateAiPromptInput;
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


export type MutationUpdateAssetFileArgs = {
  input: UpdateAssetFileInput;
};


export type MutationUpdateAssetFileTagsArgs = {
  input: UpdateAssetFileTagsInput;
};


export type MutationUpdateAssetFileTextContentArgs = {
  content: Scalars['String']['input'];
  id: Scalars['ID']['input'];
};


export type MutationUpdateAutomationWorkflowProjectArgs = {
  category?: InputMaybe<Scalars['String']['input']>;
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  permissionExpression?: InputMaybe<Scalars['String']['input']>;
  tags?: InputMaybe<Array<Scalars['String']['input']>>;
};


export type MutationUpdateAutomationWorkflowProjectWorkflowArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  label: Scalars['String']['input'];
  workflowUuid: Scalars['ID']['input'];
};


export type MutationUpdateAutomationWorkflowProjectWorkflowPermissionExpressionArgs = {
  permissionExpression?: InputMaybe<Scalars['String']['input']>;
  workflowUuid: Scalars['ID']['input'];
};


export type MutationUpdateComponentPolicyArgs = {
  enabled: Scalars['Boolean']['input'];
  name: Scalars['String']['input'];
};


export type MutationUpdateContextStoreArgs = {
  id: Scalars['ID']['input'];
  input: UpdateContextStoreInput;
  workspaceId: Scalars['ID']['input'];
};


export type MutationUpdateContextStoreSourceArgs = {
  id: Scalars['ID']['input'];
  input: UpdateContextStoreSourceInput;
};


export type MutationUpdateContextStoreTagsArgs = {
  id: Scalars['ID']['input'];
  tags: Array<TagInput>;
  workspaceId: Scalars['ID']['input'];
};


export type MutationUpdateCustomRoleArgs = {
  id: Scalars['ID']['input'];
  input: UpdateCustomRoleInput;
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


export type MutationUpdateIntegrationWorkflowPermissionExpressionArgs = {
  integrationWorkflowId: Scalars['ID']['input'];
  permissionExpression?: InputMaybe<Scalars['String']['input']>;
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


export type MutationUpdateKnowledgeBaseSourceArgs = {
  id: Scalars['ID']['input'];
  input: UpdateKnowledgeBaseSourceInput;
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


export type MutationUpdateOrganizationConnectionArgs = {
  connectionId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  tagIds?: InputMaybe<Array<Scalars['ID']['input']>>;
  version: Scalars['Int']['input'];
};


export type MutationUpdateUserArgs = {
  login: Scalars['String']['input'];
  role: Scalars['String']['input'];
};


export type MutationUpdateWorkspaceAiGatewayModelArgs = {
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayModelInput;
};


export type MutationUpdateWorkspaceAiGatewayProviderArgs = {
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayProviderInput;
  workspaceId: Scalars['ID']['input'];
};


export type MutationUpdateWorkspaceAiGatewayRoutingPolicyArgs = {
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayRoutingPolicyInput;
  workspaceId: Scalars['ID']['input'];
};


export type MutationUpdateWorkspaceApiKeyArgs = {
  apiKeyId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
};


export type MutationUpdateWorkspaceUserRoleArgs = {
  role: WorkspaceRole;
  userId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationUploadLicenceArgs = {
  contents: Scalars['String']['input'];
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

/** An organization-scoped connection visible to all members across all workspaces. */
export type OrganizationConnection = {
  __typename?: 'OrganizationConnection';
  componentName: Scalars['String']['output'];
  createdBy?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['String']['output']>;
  environmentId: Scalars['Int']['output'];
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  visibility: ConnectionVisibility;
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

export type PlaygroundChatCompletionInput = {
  maxTokens?: InputMaybe<Scalars['Int']['input']>;
  messages: Array<PlaygroundChatMessageInput>;
  model: Scalars['String']['input'];
  promptId?: InputMaybe<Scalars['ID']['input']>;
  promptVariables?: InputMaybe<Scalars['String']['input']>;
  temperature?: InputMaybe<Scalars['Float']['input']>;
  topP?: InputMaybe<Scalars['Float']['input']>;
};

export type PlaygroundChatCompletionResponse = {
  __typename?: 'PlaygroundChatCompletionResponse';
  completionTokens?: Maybe<Scalars['Int']['output']>;
  content?: Maybe<Scalars['String']['output']>;
  cost?: Maybe<Scalars['Float']['output']>;
  finishReason?: Maybe<Scalars['String']['output']>;
  latencyMs?: Maybe<Scalars['Int']['output']>;
  model?: Maybe<Scalars['String']['output']>;
  promptTokens?: Maybe<Scalars['Int']['output']>;
  totalTokens?: Maybe<Scalars['Int']['output']>;
  traceId?: Maybe<Scalars['ID']['output']>;
};

export type PlaygroundChatMessageInput = {
  content: Scalars['String']['input'];
  role: PlaygroundChatRole;
};

export enum PlaygroundChatRole {
  Assistant = 'ASSISTANT',
  System = 'SYSTEM',
  User = 'USER'
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

export enum PropertyCopilotMode {
  Formula = 'FORMULA',
  Text = 'TEXT'
}

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

export type ProviderConnectionResult = {
  __typename?: 'ProviderConnectionResult';
  errorMessage?: Maybe<Scalars['String']['output']>;
  latencyMs?: Maybe<Scalars['Int']['output']>;
  ok: Scalars['Boolean']['output'];
};

export type Query = {
  __typename?: 'Query';
  _placeholder?: Maybe<Scalars['Boolean']['output']>;
  actionDefinition: ActionDefinition;
  actionDefinitions: Array<ActionDefinition>;
  adminApiKeys?: Maybe<Array<Maybe<ApiKey>>>;
  /** Get workflows that would be affected by reassigning a user's connections. (admin only) */
  affectedWorkflows: Array<AffectedWorkflow>;
  aiAgentEvalResult?: Maybe<AiAgentEvalResult>;
  aiAgentEvalResultTranscript?: Maybe<Scalars['String']['output']>;
  aiAgentEvalRun?: Maybe<AiAgentEvalRun>;
  aiAgentEvalRuns: Array<AiAgentEvalRun>;
  aiAgentEvalTest?: Maybe<AiAgentEvalTest>;
  aiAgentEvalTests: Array<AiAgentEvalTest>;
  aiAgentJudges: Array<AiAgentJudge>;
  /**
   * Lists the current user's memories in the workspace, scoped to the supplied environment so DEVELOPMENT
   * preferences do not bleed into PRODUCTION sessions and vice versa. The optional memoryType filter narrows
   * results to a single category. Ordered by updatedAt DESC.
   */
  aiAutoMemories: Array<AiAutoMemory>;
  /**
   * Returns a single memory by id, verifying ownership against (workspaceId, currentUserId). Returns null when
   * missing or owned by another user — the same shape on both errors so a probe cannot enumerate ids across
   * workspaces.
   */
  aiAutoMemory?: Maybe<AiAutoMemory>;
  aiDefaultModel?: Maybe<AiDefaultModel>;
  aiEvalDatasetItems: Array<AiEvalDatasetItemView>;
  aiEvalDatasetVersions: Array<AiEvalDatasetVersionView>;
  aiEvalDatasets: Array<AiEvalDatasetView>;
  aiEvalExecutions?: Maybe<Array<Maybe<AiEvalExecution>>>;
  aiEvalExecutionsByTrace?: Maybe<Array<Maybe<AiEvalExecution>>>;
  aiEvalExperimentRunByTraceId?: Maybe<AiEvalExperimentRunView>;
  aiEvalExperimentRuns: Array<AiEvalExperimentRunView>;
  aiEvalExperiments: Array<AiEvalExperimentView>;
  aiEvalRule?: Maybe<AiEvalRule>;
  aiEvalRules?: Maybe<Array<Maybe<AiEvalRule>>>;
  aiEvalScoreAnalytics?: Maybe<Array<Maybe<AiEvalScoreAnalytics>>>;
  aiEvalScoreConfig?: Maybe<AiEvalScoreConfig>;
  aiEvalScoreConfigs?: Maybe<Array<Maybe<AiEvalScoreConfig>>>;
  aiEvalScoreTrend?: Maybe<Array<Maybe<AiEvalScoreTrendPoint>>>;
  aiEvalScores?: Maybe<Array<Maybe<AiEvalScore>>>;
  aiEvalScoresByTrace?: Maybe<Array<Maybe<AiEvalScore>>>;
  aiEvalTemplates?: Maybe<Array<Maybe<AiEvalTemplate>>>;
  aiGatewayBudget?: Maybe<AiGatewayBudget>;
  aiGatewayModel?: Maybe<AiGatewayModel>;
  aiGatewayModels?: Maybe<Array<Maybe<AiGatewayModel>>>;
  aiGatewayModelsByProvider?: Maybe<Array<Maybe<AiGatewayModel>>>;
  aiGatewayProject?: Maybe<AiGatewayProject>;
  aiGatewayProjects: Array<AiGatewayProject>;
  aiGatewayProvider?: Maybe<AiGatewayProvider>;
  aiGatewayProviders?: Maybe<Array<Maybe<AiGatewayProvider>>>;
  aiGatewayRateLimits: Array<AiGatewayRateLimit>;
  aiGatewayRequestLogs?: Maybe<Array<Maybe<AiGatewayRequestLog>>>;
  aiGatewayRoutingPolicies?: Maybe<Array<Maybe<AiGatewayRoutingPolicy>>>;
  aiGatewayRoutingPolicy?: Maybe<AiGatewayRoutingPolicy>;
  aiGatewaySpendSummaries?: Maybe<Array<Maybe<AiGatewaySpendSummary>>>;
  aiGatewayWorkspaceSettings?: Maybe<AiGatewayWorkspaceSettings>;
  /**
   * The tools of one MCP server, discovered by connecting to it, each joined with its persisted enabled state.
   * Errors if the server is unreachable.
   */
  aiHubMcpServerTools: Array<AiHubMcpServerTool>;
  /** The current user's registered MCP servers for the workspace. */
  aiHubMcpServers: Array<AiHubMcpServer>;
  /**
   * Returns a single personal agent by id, verifying ownership against (workspaceId, currentUserId). Returns
   * null when missing or owned by another user — uniform shape so probing cannot enumerate ids.
   */
  aiHubPersonalAgent?: Maybe<AiHubPersonalAgent>;
  /**
   * Lists the current user's personal agents in the workspace, scoped to the supplied environment so
   * DEVELOPMENT-only agents do not appear in PRODUCTION sessions. Ordered by updatedAt DESC.
   */
  aiHubPersonalAgents: Array<AiHubPersonalAgent>;
  /**
   * Paginated, filtered audit listing across all aiHubTasks in the workspace. Admin-only — gated by
   * ADMIN authority on AiHubTaskArtifactFacade. {@code page} and {@code size} are silently
   * clamped to internal bounds (10_000 and 500 respectively); {@code pageClamped}/{@code sizeClamped} on the
   * response surface the silent truncation so dashboards/tests can detect a mismatch between requested and
   * served values. Negative {@code page} or {@code size < 1} are hard errors — those are malformed, not just
   * out-of-range.
   */
  aiHubTaskArtifacts: AiHubTaskArtifactPage;
  /**
   * Returns the artifact log for a task, ordered newest-first. Ownership is verified by the service
   * layer. Distinct from the workspace-wide {@code aiHubTaskArtifacts} admin query — this is the
   * per-task read used by the sidebar.
   */
  aiHubTaskArtifactsByAiHubTask: Array<AiHubTaskArtifact>;
  /**
   * Returns the message history for a task. Ownership is verified at the service layer — a caller who
   * is not the task's owner gets a 403-equivalent error.
   */
  aiHubTaskMessages: Array<AiHubTaskMessage>;
  /**
   * Lists components that publish at least one tool-typed cluster element, with each component's tool
   * catalog. Powers the composer plus-button menu's Tools section so users can browse the catalog
   * without going through chat.
   */
  aiHubTaskToolableComponents: Array<ToolableComponent>;
  /**
   * Lists every tool attached to the supplied task, joined with its parent component-binding
   * context. Used by the task attached-tools chip list in the composer.
   */
  aiHubTaskTools: Array<AiHubTaskToolBinding>;
  /**
   * Lists aiHubTasks for the current user in the given workspace, scoped to the supplied environment so
   * DEVELOPMENT chat history does not bleed into a PRODUCTION session view. Filtered by lifecycle status
   * (default {@code ACTIVE}).
   */
  aiHubTasks: Array<AiHubTask>;
  /**
   * The current user's globally-added connectors (the Connectors page Pre-built list). Each carries the
   * component metadata, its connection, the component-level enabled flag, and its tools with per-tool
   * enabled flags.
   */
  aiHubUserConnectors: Array<AiHubUserConnector>;
  aiHubWorkspaceSettings?: Maybe<AiHubWorkspaceSettings>;
  aiObservabilityAlertEvents?: Maybe<Array<Maybe<AiObservabilityAlertEvent>>>;
  aiObservabilityAlertRule?: Maybe<AiObservabilityAlertRule>;
  aiObservabilityAlertRules?: Maybe<Array<Maybe<AiObservabilityAlertRule>>>;
  aiObservabilityExportJob?: Maybe<AiObservabilityExportJob>;
  aiObservabilityExportJobs?: Maybe<Array<Maybe<AiObservabilityExportJob>>>;
  aiObservabilityNotificationChannel?: Maybe<AiObservabilityNotificationChannel>;
  aiObservabilityNotificationChannels?: Maybe<Array<Maybe<AiObservabilityNotificationChannel>>>;
  aiObservabilitySession?: Maybe<AiObservabilitySession>;
  aiObservabilitySessions?: Maybe<Array<Maybe<AiObservabilitySession>>>;
  aiObservabilityTrace?: Maybe<AiObservabilityTrace>;
  aiObservabilityTraces?: Maybe<Array<Maybe<AiObservabilityTrace>>>;
  aiObservabilityWebhookDeliveries?: Maybe<Array<Maybe<AiObservabilityWebhookDelivery>>>;
  aiObservabilityWebhookSubscription?: Maybe<AiObservabilityWebhookSubscription>;
  aiObservabilityWebhookSubscriptions?: Maybe<Array<Maybe<AiObservabilityWebhookSubscription>>>;
  aiPrompt?: Maybe<AiPrompt>;
  aiPromptVersions?: Maybe<Array<Maybe<AiPromptVersion>>>;
  aiPrompts?: Maybe<Array<Maybe<AiPrompt>>>;
  aiProviderCatalog: Array<AiProviderCatalogItem>;
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
  assetFile?: Maybe<AssetFile>;
  assetFileTags: Array<Tag>;
  assetFileTextContent?: Maybe<Scalars['String']['output']>;
  assetFiles: Array<AssetFile>;
  auditEventTypes: Array<Scalars['String']['output']>;
  auditEvents: AuditEventPageType;
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
  /**
   * Workflow-less variant of FieldsProvider.getFields() — used by the Add Context Source wizard to
   * populate ID Field and Indexed Fields name dropdowns. Returns an empty list when the cluster element
   * does not implement FieldsProvider (the wizard then falls back to free-text input).
   */
  clusterElementFields: Array<Field>;
  clusterElementMissingRequiredProperties: Array<Scalars['String']['output']>;
  clusterElementOptions: Array<Option>;
  clusterElementScriptInput?: Maybe<Scalars['Map']['output']>;
  componentDefinition: ComponentDefinition;
  componentDefinitionSearch: Array<ComponentDefinition>;
  componentDefinitionVersions: Array<ComponentDefinition>;
  componentDefinitions: Array<ComponentDefinition>;
  /**
   * Lists every registry component with its tenant-wide visibility flag. Components with no policy row are reported
   * enabled. Admin-only.
   */
  componentPolicies: Array<ComponentPolicy>;
  connectedUser?: Maybe<ConnectedUser>;
  connectedUserMcpServers: Array<ConnectedUserMcpServer>;
  connectedUserProjects: Array<ConnectedUserProject>;
  connectedUsers?: Maybe<ConnectedUserPage>;
  connectionComponentDefinition: ComponentDefinition;
  connectionCredentialStores: Array<ConnectionCredentialStoreInfo>;
  connectionDefinition: ConnectionDefinition;
  connectionDefinitions: Array<ConnectionDefinition>;
  contextStore?: Maybe<ContextStore>;
  /**
   * Resolve a (workspace, name, environment) triple to a Context Store id. Building block for env-aware
   * workflows that target a store by stable name across DEVELOPMENT / STAGING / PRODUCTION instead of a
   * hardcoded id. Returns null when no store matches in the given environment.
   */
  contextStoreIdByName?: Maybe<Scalars['ID']['output']>;
  contextStoreSource?: Maybe<ContextStoreSource>;
  contextStoreSources: Array<ContextStoreSource>;
  /**
   * All tags used across Context Stores in this workspace. Drives the TagList autocomplete (remainingTags)
   * on the management page.
   */
  contextStoreTags: Array<Tag>;
  /**
   * Returns the parent Context Stores in the workspace, filtered by environment. Each Context Store is
   * env-stamped at creation; sources, records, and ClickHouse projection tables hang off context_store_id and
   * inherit env transitively.
   */
  contextStores: Array<ContextStore>;
  customComponent?: Maybe<CustomComponent>;
  customComponentDefinition?: Maybe<CustomComponentDefinition>;
  customComponents: Array<CustomComponent>;
  /** Get a custom role by ID. Requires tenant admin. */
  customRole: CustomRole;
  /** List all custom roles. Requires tenant admin. */
  customRoles: Array<CustomRole>;
  /**
   * Workspace connections whose component exposes at least one ItemReader cluster element.
   * Used by Context Store and Knowledge Base Source create-source dialogs to filter the
   * connection picker.
   */
  dataStreamCompatibleConnections: Array<DataStreamCompatibleConnection>;
  dataTableRows: Array<DataTableRow>;
  dataTableRowsPage: DataTableRowPage;
  dataTableTags: Array<Tag>;
  dataTableTagsByTable: Array<DataTableTagsEntry>;
  dataTableWebhooks: Array<DataTableWebhook>;
  dataTables: Array<DataTable>;
  editorJobFileLogs: LogPage;
  editorJobFileLogsExist: Scalars['Boolean']['output'];
  editorTaskExecutionFileLogs: Array<LogEntry>;
  embeddedMcpServerTags?: Maybe<Array<Maybe<Tag>>>;
  embeddedMcpServers?: Maybe<Array<Maybe<McpServer>>>;
  endpointDiscoveryStatus?: Maybe<EndpointDiscoveryResult>;
  environments?: Maybe<Array<Maybe<Environment>>>;
  evaluatorFunctionDefinition: EvaluatorFunctionDefinition;
  evaluatorFunctionDefinitions: Array<EvaluatorFunctionDefinition>;
  experimentComparison?: Maybe<ExperimentComparisonView>;
  exportDataTableCsv: Scalars['String']['output'];
  generationJobStatus?: Maybe<GenerationJobStatus>;
  identityProvider?: Maybe<IdentityProviderType>;
  identityProviders: Array<Maybe<IdentityProviderType>>;
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
  knowledgeBaseSource?: Maybe<KnowledgeBaseSource>;
  knowledgeBaseSources: Array<KnowledgeBaseSource>;
  knowledgeBaseTags?: Maybe<Array<Tag>>;
  knowledgeBaseTagsByKnowledgeBase?: Maybe<Array<KnowledgeBaseTagsEntry>>;
  knowledgeBases?: Maybe<Array<Maybe<KnowledgeBase>>>;
  licence?: Maybe<LicenceType>;
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
  mcpServers?: Maybe<Array<Maybe<McpServer>>>;
  mcpTool?: Maybe<McpTool>;
  mcpTools?: Maybe<Array<Maybe<McpTool>>>;
  mcpToolsByComponentId?: Maybe<Array<Maybe<McpTool>>>;
  /** Returns the workspace role name for the current user in the given workspace */
  myWorkspaceRole?: Maybe<Scalars['String']['output']>;
  /** Returns the permission scope names the current user has in the given workspace */
  myWorkspaceScopes: Array<Scalars['String']['output']>;
  /** Get all organization-level connections, optionally filtered by environment. (admin only, EE only) */
  organizationConnections: Array<OrganizationConnection>;
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
  /** Get all connections owned by a user within a workspace, with metadata about how many workflows depend on each. (admin only) */
  unresolvedConnections: Array<ConnectionReassignmentItem>;
  user?: Maybe<AdminUser>;
  users?: Maybe<AdminUserPage>;
  validateWorkflow: WorkflowValidationResult;
  validateWorkflowById: WorkflowValidationResult;
  workflowNodeComponentConnections: Array<ComponentConnection>;
  workflowNodeMissingRequiredProperties: Array<Scalars['String']['output']>;
  workflowNodeScriptInput?: Maybe<Scalars['Map']['output']>;
  workflowTemplate?: Maybe<WorkflowTemplate>;
  workspaceAiGatewayModels?: Maybe<Array<Maybe<AiGatewayModel>>>;
  workspaceAiGatewayProviders?: Maybe<Array<Maybe<AiGatewayProvider>>>;
  workspaceAiGatewayRequestLogs?: Maybe<Array<Maybe<AiGatewayRequestLog>>>;
  workspaceAiGatewayRoutingPolicies?: Maybe<Array<Maybe<AiGatewayRoutingPolicy>>>;
  workspaceApiKeys: Array<ApiKey>;
  workspaceChatWorkflows: Array<ChatWorkflow>;
  workspaceMcpServerTags?: Maybe<Array<Maybe<Tag>>>;
  workspaceMcpServers?: Maybe<Array<Maybe<McpServer>>>;
  workspaceProjectDeployments: Array<ProjectDeployment>;
  /** List all users of a workspace. Requires at least VIEWER workspace role. */
  workspaceUsers: Array<WorkspaceUser>;
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


export type QueryAffectedWorkflowsArgs = {
  userLogin: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
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


export type QueryAiAutoMemoriesArgs = {
  environment: Scalars['Int']['input'];
  memoryType?: InputMaybe<AiAutoMemoryType>;
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiAutoMemoryArgs = {
  id: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiDefaultModelArgs = {
  environment: Scalars['ID']['input'];
};


export type QueryAiEvalDatasetItemsArgs = {
  versionId: Scalars['ID']['input'];
};


export type QueryAiEvalDatasetVersionsArgs = {
  datasetId: Scalars['ID']['input'];
};


export type QueryAiEvalDatasetsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiEvalExecutionsArgs = {
  evalRuleId: Scalars['ID']['input'];
};


export type QueryAiEvalExecutionsByTraceArgs = {
  traceId: Scalars['ID']['input'];
};


export type QueryAiEvalExperimentRunByTraceIdArgs = {
  traceId: Scalars['ID']['input'];
};


export type QueryAiEvalExperimentRunsArgs = {
  experimentId: Scalars['ID']['input'];
};


export type QueryAiEvalExperimentsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiEvalRuleArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiEvalRulesArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiEvalScoreAnalyticsArgs = {
  endDate: Scalars['Long']['input'];
  startDate: Scalars['Long']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiEvalScoreConfigArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiEvalScoreConfigsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiEvalScoreTrendArgs = {
  endDate: Scalars['Long']['input'];
  name: Scalars['String']['input'];
  startDate: Scalars['Long']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiEvalScoresArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiEvalScoresByTraceArgs = {
  traceId: Scalars['ID']['input'];
};


export type QueryAiGatewayBudgetArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiGatewayModelArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiGatewayModelsByProviderArgs = {
  providerId: Scalars['ID']['input'];
};


export type QueryAiGatewayProjectArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiGatewayProjectsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiGatewayProviderArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiGatewayRateLimitsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiGatewayRequestLogsArgs = {
  endDate: Scalars['Long']['input'];
  startDate: Scalars['Long']['input'];
};


export type QueryAiGatewayRoutingPolicyArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiGatewaySpendSummariesArgs = {
  endDate: Scalars['Long']['input'];
  startDate: Scalars['Long']['input'];
};


export type QueryAiGatewayWorkspaceSettingsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiHubMcpServerToolsArgs = {
  mcpServerId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiHubMcpServersArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiHubPersonalAgentArgs = {
  id: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiHubPersonalAgentsArgs = {
  environment: Scalars['Int']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiHubTaskArtifactsArgs = {
  environment?: InputMaybe<Scalars['Int']['input']>;
  from?: InputMaybe<Scalars['Long']['input']>;
  kind?: InputMaybe<AiHubTaskArtifactKind>;
  page?: InputMaybe<Scalars['Int']['input']>;
  size?: InputMaybe<Scalars['Int']['input']>;
  to?: InputMaybe<Scalars['Long']['input']>;
  userId?: InputMaybe<Scalars['ID']['input']>;
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiHubTaskArtifactsByAiHubTaskArgs = {
  id: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiHubTaskMessagesArgs = {
  id: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiHubTaskToolableComponentsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiHubTaskToolsArgs = {
  taskId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiHubTasksArgs = {
  environment: Scalars['Int']['input'];
  status?: InputMaybe<AiHubTaskStatus>;
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiHubUserConnectorsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiHubWorkspaceSettingsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiObservabilityAlertEventsArgs = {
  alertRuleId: Scalars['ID']['input'];
};


export type QueryAiObservabilityAlertRuleArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiObservabilityAlertRulesArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiObservabilityExportJobArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiObservabilityExportJobsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiObservabilityNotificationChannelArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiObservabilityNotificationChannelsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiObservabilitySessionArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiObservabilitySessionsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiObservabilityTraceArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiObservabilityTracesArgs = {
  endDate: Scalars['Long']['input'];
  model?: InputMaybe<Scalars['String']['input']>;
  source?: InputMaybe<AiObservabilityTraceSource>;
  startDate: Scalars['Long']['input'];
  status?: InputMaybe<AiObservabilityTraceStatus>;
  tagId?: InputMaybe<Scalars['ID']['input']>;
  userId?: InputMaybe<Scalars['String']['input']>;
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiObservabilityWebhookDeliveriesArgs = {
  subscriptionId: Scalars['ID']['input'];
};


export type QueryAiObservabilityWebhookSubscriptionArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiObservabilityWebhookSubscriptionsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiPromptArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiPromptVersionsArgs = {
  promptId: Scalars['ID']['input'];
};


export type QueryAiPromptsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiProviderCatalogArgs = {
  environment: Scalars['ID']['input'];
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


export type QueryAssetFileArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAssetFileTagsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAssetFileTextContentArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAssetFilesArgs = {
  environment?: InputMaybe<Scalars['Int']['input']>;
  mimeTypePrefix?: InputMaybe<Scalars['String']['input']>;
  tagIds?: InputMaybe<Array<Scalars['ID']['input']>>;
  workspaceId: Scalars['ID']['input'];
};


export type QueryAuditEventsArgs = {
  dataSearch?: InputMaybe<Scalars['String']['input']>;
  eventType?: InputMaybe<Scalars['String']['input']>;
  fromDate?: InputMaybe<Scalars['Long']['input']>;
  page?: InputMaybe<Scalars['Int']['input']>;
  principal?: InputMaybe<Scalars['String']['input']>;
  size?: InputMaybe<Scalars['Int']['input']>;
  toDate?: InputMaybe<Scalars['Long']['input']>;
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


export type QueryClusterElementFieldsArgs = {
  clusterElementName: Scalars['String']['input'];
  componentName: Scalars['String']['input'];
  componentVersion: Scalars['Int']['input'];
  connectionId?: InputMaybe<Scalars['Long']['input']>;
  inputParameters?: InputMaybe<Scalars['Map']['input']>;
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


export type QueryContextStoreArgs = {
  id: Scalars['ID']['input'];
};


export type QueryContextStoreIdByNameArgs = {
  environmentId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryContextStoreSourceArgs = {
  id: Scalars['ID']['input'];
};


export type QueryContextStoreSourcesArgs = {
  environmentId: Scalars['ID']['input'];
  filter?: InputMaybe<ContextStoreSourceFilter>;
  workspaceId: Scalars['ID']['input'];
};


export type QueryContextStoreTagsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryContextStoresArgs = {
  environmentId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryCustomComponentArgs = {
  id: Scalars['ID']['input'];
};


export type QueryCustomComponentDefinitionArgs = {
  id: Scalars['ID']['input'];
};


export type QueryCustomRoleArgs = {
  id: Scalars['ID']['input'];
};


export type QueryDataStreamCompatibleConnectionsArgs = {
  environmentId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
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


export type QueryDataTableTagsArgs = {
  workspaceId: Scalars['ID']['input'];
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


export type QueryExperimentComparisonArgs = {
  experimentIds: Array<Scalars['ID']['input']>;
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


export type QueryKnowledgeBaseSourceArgs = {
  id: Scalars['ID']['input'];
};


export type QueryKnowledgeBaseSourcesArgs = {
  environmentId: Scalars['ID']['input'];
  filter?: InputMaybe<KnowledgeBaseSourceFilter>;
  workspaceId: Scalars['ID']['input'];
};


export type QueryKnowledgeBaseTagsArgs = {
  workspaceId: Scalars['ID']['input'];
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


export type QueryMcpProjectsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryMcpProjectsByServerIdArgs = {
  mcpServerId?: InputMaybe<Scalars['ID']['input']>;
};


export type QueryMcpServerArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
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


export type QueryMyWorkspaceRoleArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryMyWorkspaceScopesArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryOrganizationConnectionsArgs = {
  environmentId?: InputMaybe<Scalars['ID']['input']>;
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


export type QueryUnresolvedConnectionsArgs = {
  userLogin: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
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


export type QueryWorkspaceAiGatewayModelsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryWorkspaceAiGatewayProvidersArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryWorkspaceAiGatewayRequestLogsArgs = {
  endDate: Scalars['Long']['input'];
  startDate: Scalars['Long']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryWorkspaceAiGatewayRoutingPoliciesArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryWorkspaceApiKeysArgs = {
  environmentId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryWorkspaceChatWorkflowsArgs = {
  environmentId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type QueryWorkspaceMcpServerTagsArgs = {
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


export type QueryWorkspaceUsersArgs = {
  workspaceId: Scalars['ID']['input'];
};

export type RecordReferencedAiHubTaskArtifactInput = {
  artifactId: Scalars['String']['input'];
  artifactName: Scalars['String']['input'];
  kind: AiHubTaskArtifactKind;
  /**
   * Optional JSON map of side-channel context the artifact needs to be quick-openable from the sidebar.
   * For WORKFLOW_REFERENCED this carries `projectId` and `projectWorkflowId` so the sidebar row can open
   * the workflow tab (which routes by parent project); for other kinds it's currently unused but
   * schema-symmetric with the agent-driven `record(...)` path. Persisted as-is in `metadata_json`.
   */
  metadataJson?: InputMaybe<Scalars['String']['input']>;
  taskId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};

export type RegisterExistingConnectionInput = {
  componentName: Scalars['String']['input'];
  connectionVersion: Scalars['Int']['input'];
  credentialRef: Scalars['String']['input'];
  credentialStoreType: ConnectionCredentialStoreType;
  environmentId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
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

export enum ScheduleFrequencyKind {
  CustomCron = 'CUSTOM_CRON',
  Daily = 'DAILY',
  EveryXMinutes = 'EVERY_X_MINUTES',
  Hourly = 'HOURLY',
  Monthly = 'MONTHLY',
  Weekly = 'WEEKLY'
}

export enum ScheduleLifecycleKind {
  NumberOfRuns = 'NUMBER_OF_RUNS',
  Recurring = 'RECURRING'
}

export type ScorePoint = {
  __typename?: 'ScorePoint';
  dataType: Scalars['String']['output'];
  name: Scalars['String']['output'];
  stringValue?: Maybe<Scalars['String']['output']>;
  value?: Maybe<Scalars['Float']['output']>;
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

export type SetAiHubPersonalAgentScheduleInput = {
  aiHubPersonalAgentId: Scalars['ID']['input'];
  /** Null clears the schedule. Non-null upserts. */
  schedule?: InputMaybe<AiHubPersonalAgentScheduleInput>;
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

export enum TombstoneStrategy {
  None = 'NONE',
  PeriodicFullReplace = 'PERIODIC_FULL_REPLACE',
  UpstreamChangeFeed = 'UPSTREAM_CHANGE_FEED'
}

export type ToolableClusterElement = {
  __typename?: 'ToolableClusterElement';
  description?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  title?: Maybe<Scalars['String']['output']>;
};

export type ToolableComponent = {
  __typename?: 'ToolableComponent';
  componentName: Scalars['String']['output'];
  componentVersion: Scalars['Int']['output'];
  /** Whether this component requires a connection to be used (drives the composer's Connect vs toggle affordance). */
  connectionRequired: Scalars['Boolean']['output'];
  description?: Maybe<Scalars['String']['output']>;
  icon?: Maybe<Scalars['String']['output']>;
  title?: Maybe<Scalars['String']['output']>;
  tools: Array<ToolableClusterElement>;
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

export type UpdateAiAutoMemoryInput = {
  content?: InputMaybe<Scalars['String']['input']>;
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  memoryType?: InputMaybe<AiAutoMemoryType>;
  title?: InputMaybe<Scalars['String']['input']>;
  workspaceId: Scalars['ID']['input'];
};

export type UpdateAiGatewayBudgetInput = {
  alertThreshold?: InputMaybe<Scalars['Int']['input']>;
  amount?: InputMaybe<Scalars['String']['input']>;
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  enforcementMode?: InputMaybe<AiGatewayBudgetEnforcementMode>;
  period?: InputMaybe<AiGatewayBudgetPeriod>;
};

export type UpdateAiGatewayModelInput = {
  alias?: InputMaybe<Scalars['String']['input']>;
  capabilities?: InputMaybe<Scalars['String']['input']>;
  contextWindow?: InputMaybe<Scalars['Int']['input']>;
  defaultRoutingPolicyId?: InputMaybe<Scalars['ID']['input']>;
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  inputCostPerMTokens?: InputMaybe<Scalars['Float']['input']>;
  name?: InputMaybe<Scalars['String']['input']>;
  outputCostPerMTokens?: InputMaybe<Scalars['Float']['input']>;
};

export type UpdateAiGatewayProjectInput = {
  cacheTtlMinutes?: InputMaybe<Scalars['Int']['input']>;
  cachingEnabled?: InputMaybe<Scalars['Boolean']['input']>;
  compressionEnabled?: InputMaybe<Scalars['Boolean']['input']>;
  description?: InputMaybe<Scalars['String']['input']>;
  logRetentionDays?: InputMaybe<Scalars['Int']['input']>;
  name?: InputMaybe<Scalars['String']['input']>;
  retryMaxAttempts?: InputMaybe<Scalars['Int']['input']>;
  routingPolicyId?: InputMaybe<Scalars['ID']['input']>;
  slug?: InputMaybe<Scalars['String']['input']>;
  timeoutSeconds?: InputMaybe<Scalars['Int']['input']>;
};

export type UpdateAiGatewayProviderInput = {
  apiKey?: InputMaybe<Scalars['String']['input']>;
  baseUrl?: InputMaybe<Scalars['String']['input']>;
  config?: InputMaybe<Scalars['String']['input']>;
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  name: Scalars['String']['input'];
  type: AiGatewayProviderType;
};

export type UpdateAiGatewayRateLimitInput = {
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  limitType?: InputMaybe<AiGatewayRateLimitType>;
  limitValue?: InputMaybe<Scalars['Int']['input']>;
  name?: InputMaybe<Scalars['String']['input']>;
  projectId?: InputMaybe<Scalars['ID']['input']>;
  propertyKey?: InputMaybe<Scalars['String']['input']>;
  scope?: InputMaybe<AiGatewayRateLimitScope>;
  windowSeconds?: InputMaybe<Scalars['Int']['input']>;
};

export type UpdateAiGatewayRoutingPolicyInput = {
  config?: InputMaybe<Scalars['String']['input']>;
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  fallbackModel?: InputMaybe<Scalars['String']['input']>;
  name?: InputMaybe<Scalars['String']['input']>;
  strategy?: InputMaybe<AiGatewayRoutingStrategyType>;
  tagIds?: InputMaybe<Array<Scalars['ID']['input']>>;
};

export type UpdateAiHubPersonalAgentInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  instructions?: InputMaybe<Scalars['String']['input']>;
  llmModel?: InputMaybe<Scalars['String']['input']>;
  /**
   * Tri-state on the (llmProvider, llmModel) pair:
   * - both null on the input: leave the existing override untouched.
   * - both empty strings: clear the override (revert to workspace default).
   * - both non-empty: apply the new override after the both-set-or-both-null guard.
   * - only one set: typed validation error.
   */
  llmProvider?: InputMaybe<Scalars['String']['input']>;
  title?: InputMaybe<Scalars['String']['input']>;
  workspaceId: Scalars['ID']['input'];
};

export type UpdateAiHubPersonalAgentToolConfigInput = {
  /**
   * Pinned connection id. Null clears the existing pin so subsequent aiHubTasks let the user pick at
   * first invocation.
   */
  connectionId?: InputMaybe<Scalars['Long']['input']>;
  /**
   * Pre-set parameters map. Null preserves the existing map; pass an empty object to reset. Stored verbatim
   * — no shape validation against the action's input schema, since LLM-supplied invocation args still merge
   * on top at dispatch.
   */
  parameters?: InputMaybe<Scalars['Any']['input']>;
  toolId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};

export type UpdateAiHubVoiceWebhookUrlInput = {
  voiceWebhookUrl?: InputMaybe<Scalars['String']['input']>;
  workspaceId: Scalars['ID']['input'];
};

export type UpdateAiPromptInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  name?: InputMaybe<Scalars['String']['input']>;
};

export type UpdateApiConnectorInput = {
  connectorVersion?: InputMaybe<Scalars['Int']['input']>;
  description?: InputMaybe<Scalars['String']['input']>;
  icon?: InputMaybe<Scalars['String']['input']>;
  name?: InputMaybe<Scalars['String']['input']>;
  title?: InputMaybe<Scalars['String']['input']>;
};

export type UpdateAssetFileInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
};

export type UpdateAssetFileTagsInput = {
  id: Scalars['ID']['input'];
  tags: Array<TagInput>;
};

export type UpdateContextStoreInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  tagIds?: InputMaybe<Array<Scalars['ID']['input']>>;
  version: Scalars['Int']['input'];
};

export type UpdateContextStoreSourceInput = {
  cadence?: InputMaybe<Scalars['String']['input']>;
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  /**
   * Phase 17b: optional full-replace cadence update. Null leaves the current value unchanged.
   * Empty string \"\" clears it (drops back to single-trigger). Any other value replaces it.
   */
  fullReplaceCadence?: InputMaybe<Scalars['String']['input']>;
  name?: InputMaybe<Scalars['String']['input']>;
  /** Phase 17b: optional tombstone strategy update. Null leaves the current value unchanged. */
  tombstoneStrategy?: InputMaybe<ContextStoreTombstoneStrategy>;
};

export type UpdateCustomRoleInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  /** Permission scope names to grant (must be names registered by the server's PermissionScopeProvider SPI) */
  scopes: Array<Scalars['String']['input']>;
};

export type UpdateDataTableTagsInput = {
  tableId: Scalars['ID']['input'];
  tags?: InputMaybe<Array<TagInput>>;
};

export type UpdateKnowledgeBaseDocumentTagsInput = {
  knowledgeBaseDocumentId: Scalars['ID']['input'];
  tags?: InputMaybe<Array<Scalars['String']['input']>>;
};

export type UpdateKnowledgeBaseSourceInput = {
  cadence?: InputMaybe<Scalars['String']['input']>;
  enabled?: InputMaybe<Scalars['Boolean']['input']>;
  /**
   * Phase 17b: optional full-replace cadence update. Null leaves the current value unchanged.
   * Empty string \"\" clears it (drops back to single-trigger). Any other value replaces it.
   */
  fullReplaceCadence?: InputMaybe<Scalars['String']['input']>;
  /**
   * Optional metadata-tag whitelist update. Null leaves the current whitelist unchanged;
   * {} clears it (revert to full-flatten); {fields: [...]} replaces it.
   */
  metadataFields?: InputMaybe<Scalars['Map']['input']>;
  name?: InputMaybe<Scalars['String']['input']>;
  /** Phase 17b: optional tombstone strategy update. Null leaves the current value unchanged. */
  tombstoneStrategy?: InputMaybe<TombstoneStrategy>;
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

export enum WorkspaceRole {
  Admin = 'ADMIN',
  Editor = 'EDITOR',
  Viewer = 'VIEWER'
}

export type WorkspaceUser = {
  __typename?: 'WorkspaceUser';
  createdDate?: Maybe<Scalars['String']['output']>;
  /** Custom role ID (EE only), null if using a built-in role */
  customRoleId?: Maybe<Scalars['ID']['output']>;
  id: Scalars['ID']['output'];
  user?: Maybe<WorkspaceUserInfo>;
  userId: Scalars['ID']['output'];
  workspaceId: Scalars['ID']['output'];
  /** Built-in workspace role, null if using a custom role */
  workspaceRole?: Maybe<WorkspaceRole>;
};

export type WorkspaceUserInfo = {
  __typename?: 'WorkspaceUserInfo';
  email: Scalars['String']['output'];
  firstName?: Maybe<Scalars['String']['output']>;
  lastName?: Maybe<Scalars['String']['output']>;
};
