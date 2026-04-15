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

/** A workflow affected by connection reassignment. */
export type AffectedWorkflow = {
  __typename?: 'AffectedWorkflow';
  connectionIds: Array<Scalars['ID']['output']>;
  workflowId: Scalars['String']['output'];
  workflowName: Scalars['String']['output'];
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

export type AiAgentSkill = {
  __typename?: 'AiAgentSkill';
  /** Epoch milliseconds (UTC) */
  createdDate?: Maybe<Scalars['Long']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  /** Epoch milliseconds (UTC) */
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
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
  workspaceId: Scalars['ID']['output'];
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
  workspaceId: Scalars['ID']['output'];
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
  workspaceId: Scalars['ID']['output'];
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

export type AiGatewayTag = {
  __typename?: 'AiGatewayTag';
  color?: Maybe<Scalars['String']['output']>;
  createdDate?: Maybe<Scalars['Long']['output']>;
  id: Scalars['ID']['output'];
  lastModifiedDate?: Maybe<Scalars['Long']['output']>;
  name: Scalars['String']['output'];
  version?: Maybe<Scalars['Int']['output']>;
  workspaceId: Scalars['ID']['output'];
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
  workspaceId: Scalars['ID']['output'];
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
  workspaceId: Scalars['ID']['output'];
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
  workspaceId: Scalars['ID']['output'];
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
  workspaceId: Scalars['ID']['output'];
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
  workspaceId: Scalars['ID']['output'];
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

/** Outcome of a bulk visibility-change operation. Invariant: promoted + skipped + failed == attempted. */
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
  Project = 'PROJECT',
  Workspace = 'WORKSPACE'
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

export type CreateAiGatewayTagInput = {
  color?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
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

export type CreateCustomRoleInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  /** Permission scope names to grant (must be valid PermissionScope enum values) */
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
  /** Permission scope names granted by this role (e.g., WORKFLOW_VIEW, EXECUTION_DATA) */
  scopes: Array<Scalars['String']['output']>;
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
  acknowledgeAiObservabilityAlertEvent?: Maybe<AiObservabilityAlertEvent>;
  addDataTableColumn: Scalars['Boolean']['output'];
  /** Add a user to a project. Requires PROJECT_MANAGE_USERS scope. */
  addProjectUser: ProjectUser;
  /** Add a user to a workspace. Requires ADMIN workspace role. */
  addWorkspaceUser: WorkspaceUser;
  cancelAiAgentEvalRun: AiAgentEvalRun;
  cancelAiObservabilityExportJob?: Maybe<AiObservabilityExportJob>;
  cancelGenerationJob: Scalars['Boolean']['output'];
  createAiAgentEvalScenario: AiAgentEvalScenario;
  createAiAgentEvalTest: AiAgentEvalTest;
  createAiAgentJudge: AiAgentJudge;
  createAiAgentScenarioJudge: AiAgentScenarioJudge;
  createAiAgentScenarioToolSimulation: AiAgentScenarioToolSimulation;
  createAiAgentSkill: AiAgentSkill;
  createAiAgentSkillFromInstructions: AiAgentSkill;
  createAiEvalRule?: Maybe<AiEvalRule>;
  createAiEvalScore?: Maybe<AiEvalScore>;
  createAiEvalScoreConfig?: Maybe<AiEvalScoreConfig>;
  createAiGatewayBudget?: Maybe<AiGatewayBudget>;
  createAiGatewayModel?: Maybe<AiGatewayModel>;
  createAiGatewayProject?: Maybe<AiGatewayProject>;
  createAiGatewayProvider?: Maybe<AiGatewayProvider>;
  createAiGatewayRateLimit?: Maybe<AiGatewayRateLimit>;
  createAiGatewayRoutingPolicy?: Maybe<AiGatewayRoutingPolicy>;
  createAiGatewayTag?: Maybe<AiGatewayTag>;
  createAiObservabilityAlertRule?: Maybe<AiObservabilityAlertRule>;
  createAiObservabilityExportJob?: Maybe<AiObservabilityExportJob>;
  createAiObservabilityNotificationChannel?: Maybe<AiObservabilityNotificationChannel>;
  createAiObservabilityWebhookSubscription?: Maybe<AiObservabilityWebhookSubscription>;
  createAiPrompt?: Maybe<AiPrompt>;
  createAiPromptVersion?: Maybe<AiPromptVersion>;
  createApiConnector: ApiConnector;
  createApiKey: Scalars['String']['output'];
  createApprovalTask?: Maybe<ApprovalTask>;
  /** Create a new custom role with the given scopes. Requires tenant admin. */
  createCustomRole: CustomRole;
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
  /** Create a new connection with ORGANIZATION visibility. (admin only, EE only) */
  createOrganizationConnection: Scalars['ID']['output'];
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
  deleteAiAgentSkill: Scalars['Boolean']['output'];
  deleteAiEvalRule?: Maybe<Scalars['Boolean']['output']>;
  deleteAiEvalScore?: Maybe<Scalars['Boolean']['output']>;
  deleteAiEvalScoreConfig?: Maybe<Scalars['Boolean']['output']>;
  deleteAiGatewayBudget?: Maybe<Scalars['Boolean']['output']>;
  deleteAiGatewayModel?: Maybe<Scalars['Boolean']['output']>;
  deleteAiGatewayProject?: Maybe<Scalars['Boolean']['output']>;
  deleteAiGatewayProvider?: Maybe<Scalars['Boolean']['output']>;
  deleteAiGatewayRateLimit?: Maybe<Scalars['Boolean']['output']>;
  deleteAiGatewayRoutingPolicy?: Maybe<Scalars['Boolean']['output']>;
  deleteAiGatewayTag?: Maybe<Scalars['Boolean']['output']>;
  deleteAiObservabilityAlertRule?: Maybe<Scalars['Boolean']['output']>;
  deleteAiObservabilityNotificationChannel?: Maybe<Scalars['Boolean']['output']>;
  deleteAiObservabilityWebhookSubscription?: Maybe<Scalars['Boolean']['output']>;
  deleteAiPrompt?: Maybe<Scalars['Boolean']['output']>;
  deleteApiConnector: Scalars['Boolean']['output'];
  deleteApiKey: Scalars['Boolean']['output'];
  deleteApprovalTask?: Maybe<Scalars['Boolean']['output']>;
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
  /** Demote a connection to PRIVATE visibility, removing all project associations. Authorized for workspace administrators OR the connection creator (orphan-recovery path when no admins remain). Fails if the connection is used by active deployments. */
  demoteConnectionToPrivate: Scalars['Boolean']['output'];
  /** Unlink a connection from all deployed workflows and test configurations, without deleting the connection itself. */
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
  /** Mark all of a user's connections as pending reassignment. Returns per-row outcome so partial failures surface; a silent no-op batch does not look like an error. (admin only) */
  markConnectionsPendingReassignment: BulkReassignResult;
  playgroundChatCompletion?: Maybe<PlaygroundChatCompletionResponse>;
  /** Promote every PRIVATE connection in the workspace to WORKSPACE visibility. Returns per-row outcome so partial failures can surface. Intended for CE→EE migration. */
  promoteAllPrivateConnectionsToWorkspace: BulkPromoteResult;
  /** Promote a connection to WORKSPACE visibility, making it visible to all workspace members. */
  promoteConnectionToWorkspace: Scalars['Boolean']['output'];
  /** Reassign all of a user's unresolved connections to a new owner. (admin only) */
  reassignAllConnections: Scalars['Boolean']['output'];
  /** Reassign a single connection to a new owner. Resets status to ACTIVE if pending. (admin only) */
  reassignConnection: Scalars['Boolean']['output'];
  removeDataTableColumn: Scalars['Boolean']['output'];
  /** Remove a user from a project. Requires PROJECT_MANAGE_USERS scope. */
  removeProjectUser: Scalars['Boolean']['output'];
  /** Remove a user from a workspace. Requires ADMIN workspace role. */
  removeWorkspaceUser: Scalars['Boolean']['output'];
  renameDataTable: Scalars['Boolean']['output'];
  renameDataTableColumn: Scalars['Boolean']['output'];
  /** Revoke a connection from a project. Auto-demotes to PRIVATE when no projects remain. */
  revokeConnectionFromProject: Scalars['Boolean']['output'];
  runAiEvalRuleOnHistoricalTraces?: Maybe<Scalars['Int']['output']>;
  saveClusterElementTestConfigurationConnection?: Maybe<Scalars['Boolean']['output']>;
  saveClusterElementTestOutput?: Maybe<WorkflowNodeTestOutputResult>;
  saveWorkflowTestConfigurationConnection?: Maybe<Scalars['Boolean']['output']>;
  setActiveAiPromptVersion?: Maybe<Scalars['Boolean']['output']>;
  setAiObservabilityTraceTags?: Maybe<AiObservabilityTrace>;
  /** Replace the set of projects a connection is shared with. Server diffs against current shares and applies share/revoke as needed in one round-trip. */
  setConnectionProjects: Scalars['Boolean']['output'];
  /** Share a connection with a specific project, setting visibility to PROJECT. */
  shareConnectionToProject: Scalars['Boolean']['output'];
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
  unsnoozeAiObservabilityAlertRule?: Maybe<AiObservabilityAlertRule>;
  updateAiAgentEvalScenario: AiAgentEvalScenario;
  updateAiAgentEvalTest: AiAgentEvalTest;
  updateAiAgentJudge: AiAgentJudge;
  updateAiAgentScenarioJudge: AiAgentScenarioJudge;
  updateAiAgentScenarioToolSimulation: AiAgentScenarioToolSimulation;
  updateAiAgentSkill: AiAgentSkill;
  updateAiEvalRule?: Maybe<AiEvalRule>;
  updateAiEvalScoreConfig?: Maybe<AiEvalScoreConfig>;
  updateAiGatewayBudget?: Maybe<AiGatewayBudget>;
  updateAiGatewayModel?: Maybe<AiGatewayModel>;
  updateAiGatewayProject?: Maybe<AiGatewayProject>;
  updateAiGatewayProvider?: Maybe<AiGatewayProvider>;
  updateAiGatewayRateLimit?: Maybe<AiGatewayRateLimit>;
  updateAiGatewayRoutingPolicy?: Maybe<AiGatewayRoutingPolicy>;
  updateAiGatewayTag?: Maybe<AiGatewayTag>;
  updateAiGatewayWorkspaceSettings?: Maybe<AiGatewayWorkspaceSettings>;
  updateAiObservabilityAlertRule?: Maybe<AiObservabilityAlertRule>;
  updateAiObservabilityNotificationChannel?: Maybe<AiObservabilityNotificationChannel>;
  updateAiObservabilityWebhookSubscription?: Maybe<AiObservabilityWebhookSubscription>;
  updateAiPrompt?: Maybe<AiPrompt>;
  updateApiConnector: ApiConnector;
  updateApiKey: Scalars['Boolean']['output'];
  updateApprovalTask?: Maybe<ApprovalTask>;
  /** Update an existing custom role. Requires tenant admin. */
  updateCustomRole: CustomRole;
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
  /** Update an organization connection's name and tags. (admin only, EE only) */
  updateOrganizationConnection: Scalars['Boolean']['output'];
  /** Update a project user's role. Requires PROJECT_MANAGE_USERS scope. */
  updateProjectUserRole: ProjectUser;
  updateUser: AdminUser;
  updateWorkspaceAiGatewayModel?: Maybe<AiGatewayModel>;
  updateWorkspaceAiGatewayProvider?: Maybe<AiGatewayProvider>;
  updateWorkspaceAiGatewayRoutingPolicy?: Maybe<AiGatewayRoutingPolicy>;
  updateWorkspaceApiKey: Scalars['Boolean']['output'];
  /** Update a workspace user's role. Requires ADMIN workspace role. */
  updateWorkspaceUserRole: WorkspaceUser;
};


export type MutationAcknowledgeAiObservabilityAlertEventArgs = {
  id: Scalars['ID']['input'];
};


export type MutationAddDataTableColumnArgs = {
  input: AddColumnInput;
};


export type MutationAddProjectUserArgs = {
  projectId: Scalars['ID']['input'];
  role: ProjectRole;
  userId: Scalars['ID']['input'];
};


export type MutationAddWorkspaceUserArgs = {
  role: WorkspaceRole;
  userId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
};


export type MutationCancelAiAgentEvalRunArgs = {
  id: Scalars['ID']['input'];
};


export type MutationCancelAiObservabilityExportJobArgs = {
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


export type MutationCreateAiAgentSkillArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  fileBytes: Scalars['String']['input'];
  filename: Scalars['String']['input'];
  name: Scalars['String']['input'];
};


export type MutationCreateAiAgentSkillFromInstructionsArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  instructions: Scalars['String']['input'];
  name: Scalars['String']['input'];
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


export type MutationCreateAiGatewayTagArgs = {
  input: CreateAiGatewayTagInput;
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


export type MutationDeleteAiAgentSkillArgs = {
  id: Scalars['ID']['input'];
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


export type MutationDeleteAiGatewayTagArgs = {
  id: Scalars['ID']['input'];
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


export type MutationRemoveDataTableColumnArgs = {
  input: RemoveColumnInput;
};


export type MutationRemoveProjectUserArgs = {
  projectId: Scalars['ID']['input'];
  userId: Scalars['ID']['input'];
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


export type MutationRevokeConnectionFromProjectArgs = {
  connectionId: Scalars['ID']['input'];
  projectId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
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


export type MutationSetAiObservabilityTraceTagsArgs = {
  tagIds: Array<Scalars['ID']['input']>;
  traceId: Scalars['ID']['input'];
};


export type MutationSetConnectionProjectsArgs = {
  connectionId: Scalars['ID']['input'];
  projectIds: Array<Scalars['ID']['input']>;
  workspaceId: Scalars['ID']['input'];
};


export type MutationShareConnectionToProjectArgs = {
  connectionId: Scalars['ID']['input'];
  projectId: Scalars['ID']['input'];
  workspaceId: Scalars['ID']['input'];
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


export type MutationUpdateAiAgentSkillArgs = {
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  name: Scalars['String']['input'];
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


export type MutationUpdateAiGatewayTagArgs = {
  color?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
};


export type MutationUpdateAiGatewayWorkspaceSettingsArgs = {
  input: AiGatewayWorkspaceSettingsInput;
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


export type MutationUpdateOrganizationConnectionArgs = {
  connectionId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  tagIds?: InputMaybe<Array<Scalars['ID']['input']>>;
  version: Scalars['Int']['input'];
};


export type MutationUpdateProjectUserRoleArgs = {
  projectId: Scalars['ID']['input'];
  role: ProjectRole;
  userId: Scalars['ID']['input'];
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

/** Project-level roles ordered from most to least privileged */
export enum ProjectRole {
  Admin = 'ADMIN',
  Editor = 'EDITOR',
  Operator = 'OPERATOR',
  Viewer = 'VIEWER'
}

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

/** A project user with an assigned role controlling their permissions within the project */
export type ProjectUser = {
  __typename?: 'ProjectUser';
  createdDate?: Maybe<Scalars['String']['output']>;
  /** Custom role ID (EE only), null if using a built-in role */
  customRoleId?: Maybe<Scalars['ID']['output']>;
  id: Scalars['ID']['output'];
  projectId: Scalars['ID']['output'];
  /** Built-in project role, null if using a custom role */
  projectRole?: Maybe<ProjectRole>;
  user?: Maybe<ProjectUserInfo>;
  userId: Scalars['ID']['output'];
};

export type ProjectUserInfo = {
  __typename?: 'ProjectUserInfo';
  email: Scalars['String']['output'];
  firstName?: Maybe<Scalars['String']['output']>;
  lastName?: Maybe<Scalars['String']['output']>;
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
  aiAgentSkill: AiAgentSkill;
  aiAgentSkillFileContent: Scalars['String']['output'];
  aiAgentSkillFilePaths: Array<Scalars['String']['output']>;
  aiAgentSkills: Array<AiAgentSkill>;
  aiEvalExecutions?: Maybe<Array<Maybe<AiEvalExecution>>>;
  aiEvalExecutionsByTrace?: Maybe<Array<Maybe<AiEvalExecution>>>;
  aiEvalRule?: Maybe<AiEvalRule>;
  aiEvalRules?: Maybe<Array<Maybe<AiEvalRule>>>;
  aiEvalScoreAnalytics?: Maybe<Array<Maybe<AiEvalScoreAnalytics>>>;
  aiEvalScoreConfig?: Maybe<AiEvalScoreConfig>;
  aiEvalScoreConfigs?: Maybe<Array<Maybe<AiEvalScoreConfig>>>;
  aiEvalScoreTrend?: Maybe<Array<Maybe<AiEvalScoreTrendPoint>>>;
  aiEvalScores?: Maybe<Array<Maybe<AiEvalScore>>>;
  aiEvalScoresByTrace?: Maybe<Array<Maybe<AiEvalScore>>>;
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
  aiGatewayTag?: Maybe<AiGatewayTag>;
  aiGatewayTags?: Maybe<Array<Maybe<AiGatewayTag>>>;
  aiGatewayWorkspaceSettings?: Maybe<AiGatewayWorkspaceSettings>;
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
  apiConnector?: Maybe<ApiConnector>;
  apiConnectors: Array<ApiConnector>;
  apiKey?: Maybe<ApiKey>;
  apiKeys?: Maybe<Array<Maybe<ApiKey>>>;
  approvalTask?: Maybe<ApprovalTask>;
  approvalTasks?: Maybe<Array<Maybe<ApprovalTask>>>;
  approvalTasksByIds?: Maybe<Array<Maybe<ApprovalTask>>>;
  auditEventTypes: Array<Scalars['String']['output']>;
  auditEvents: AuditEventPageType;
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
  /** Get a custom role by ID. Requires tenant admin. */
  customRole: CustomRole;
  /** List all custom roles. Requires tenant admin. */
  customRoles: Array<CustomRole>;
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
  /** Returns the permission scope names the current user has for the given project */
  myProjectScopes: Array<Scalars['String']['output']>;
  /** Returns the workspace role name for the current user in the given workspace */
  myWorkspaceRole?: Maybe<Scalars['String']['output']>;
  /** Get all organization-level connections, optionally filtered by environment. (admin only, EE only) */
  organizationConnections: Array<OrganizationConnection>;
  preBuiltProjectTemplates: Array<ProjectTemplate>;
  preBuiltWorkflowTemplates: Array<WorkflowTemplate>;
  project?: Maybe<Project>;
  projectDeploymentWorkflow?: Maybe<ProjectDeploymentWorkflow>;
  projectTemplate?: Maybe<ProjectTemplate>;
  /** List all users of a project. Requires PROJECT_VIEW_USERS scope. */
  projectUsers: Array<ProjectUser>;
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


export type QueryAiAgentSkillArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiAgentSkillFileContentArgs = {
  id: Scalars['ID']['input'];
  path: Scalars['String']['input'];
};


export type QueryAiAgentSkillFilePathsArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiEvalExecutionsArgs = {
  evalRuleId: Scalars['ID']['input'];
};


export type QueryAiEvalExecutionsByTraceArgs = {
  traceId: Scalars['ID']['input'];
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


export type QueryAiGatewayTagArgs = {
  id: Scalars['ID']['input'];
};


export type QueryAiGatewayTagsArgs = {
  workspaceId: Scalars['ID']['input'];
};


export type QueryAiGatewayWorkspaceSettingsArgs = {
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


export type QueryCustomRoleArgs = {
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


export type QueryMyProjectScopesArgs = {
  projectId: Scalars['ID']['input'];
};


export type QueryMyWorkspaceRoleArgs = {
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


export type QueryProjectUsersArgs = {
  projectId: Scalars['ID']['input'];
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
  propertyKey?: InputMaybe<Scalars['String']['input']>;
  propertyValue?: InputMaybe<Scalars['String']['input']>;
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

export type UpdateCustomRoleInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  /** Permission scope names to grant (must be valid PermissionScope enum values) */
  scopes: Array<Scalars['String']['input']>;
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

export enum WorkspaceRole {
  Admin = 'ADMIN',
  Editor = 'EDITOR',
  Viewer = 'VIEWER'
}

export type WorkspaceUser = {
  __typename?: 'WorkspaceUser';
  createdDate?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  user?: Maybe<WorkspaceUserInfo>;
  userId: Scalars['ID']['output'];
  workspaceId: Scalars['ID']['output'];
  workspaceRole?: Maybe<WorkspaceRole>;
};

export type WorkspaceUserInfo = {
  __typename?: 'WorkspaceUserInfo';
  email: Scalars['String']['output'];
  firstName?: Maybe<Scalars['String']['output']>;
  lastName?: Maybe<Scalars['String']['output']>;
};

export type AiAgentEvalResultQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AiAgentEvalResultQuery = { __typename?: 'Query', aiAgentEvalResult?: { __typename?: 'AiAgentEvalResult', id: string, status: AiAgentEvalResultStatus, score?: number | null, errorMessage?: string | null, transcriptFile?: string | null, createdDate?: any | null, scenario: { __typename?: 'AiAgentEvalScenario', id: string, name: string, type: AiAgentScenarioType, userMessage?: string | null, expectedOutput?: string | null, personaPrompt?: string | null, maxTurns?: number | null, createdDate?: any | null, lastModifiedDate?: any | null, judges: Array<{ __typename?: 'AiAgentScenarioJudge', id: string, name: string, type: AiAgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null }> }, verdicts: Array<{ __typename?: 'AiAgentJudgeVerdict', id: string, judgeName: string, judgeType: AiAgentJudgeType, judgeScope: AiAgentJudgeScope, passed: boolean, score: number, explanation: string }> } | null };

export type AiAgentEvalResultTranscriptQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AiAgentEvalResultTranscriptQuery = { __typename?: 'Query', aiAgentEvalResultTranscript?: string | null };

export type AiAgentEvalRunQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AiAgentEvalRunQuery = { __typename?: 'Query', aiAgentEvalRun?: { __typename?: 'AiAgentEvalRun', id: string, name: string, status: AiAgentEvalRunStatus, averageScore?: number | null, totalScenarios: number, completedScenarios: number, agentVersion?: string | null, totalInputTokens?: number | null, totalOutputTokens?: number | null, startedDate?: any | null, completedDate?: any | null, createdDate?: any | null, results: Array<{ __typename?: 'AiAgentEvalResult', id: string, status: AiAgentEvalResultStatus, score?: number | null, errorMessage?: string | null, transcriptFile?: string | null, inputTokens?: number | null, outputTokens?: number | null, runIndex?: number | null, createdDate?: any | null, scenario: { __typename?: 'AiAgentEvalScenario', id: string, name: string, type: AiAgentScenarioType, userMessage?: string | null, expectedOutput?: string | null, personaPrompt?: string | null, maxTurns?: number | null, createdDate?: any | null, lastModifiedDate?: any | null, judges: Array<{ __typename?: 'AiAgentScenarioJudge', id: string, name: string, type: AiAgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null }> }, verdicts: Array<{ __typename?: 'AiAgentJudgeVerdict', id: string, judgeName: string, judgeType: AiAgentJudgeType, judgeScope: AiAgentJudgeScope, passed: boolean, score: number, explanation: string }> }> } | null };

export type AiAgentEvalRunsQueryVariables = Exact<{
  agentEvalTestId: Scalars['ID']['input'];
  limit?: InputMaybe<Scalars['Int']['input']>;
  offset?: InputMaybe<Scalars['Int']['input']>;
}>;


export type AiAgentEvalRunsQuery = { __typename?: 'Query', aiAgentEvalRuns: Array<{ __typename?: 'AiAgentEvalRun', id: string, name: string, status: AiAgentEvalRunStatus, averageScore?: number | null, totalScenarios: number, completedScenarios: number, startedDate?: any | null, completedDate?: any | null, createdDate?: any | null }> };

export type AiAgentEvalTestQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AiAgentEvalTestQuery = { __typename?: 'Query', aiAgentEvalTest?: { __typename?: 'AiAgentEvalTest', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null, scenarios: Array<{ __typename?: 'AiAgentEvalScenario', id: string, name: string, type: AiAgentScenarioType, userMessage?: string | null, expectedOutput?: string | null, personaPrompt?: string | null, maxTurns?: number | null, numberOfRuns?: number | null, createdDate?: any | null, lastModifiedDate?: any | null, judges: Array<{ __typename?: 'AiAgentScenarioJudge', id: string, name: string, type: AiAgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null }>, toolSimulations: Array<{ __typename?: 'AiAgentScenarioToolSimulation', id: string, responsePrompt: string, simulationModel?: string | null, toolName: string, createdDate?: any | null, lastModifiedDate?: any | null }> }> } | null };

export type AiAgentEvalTestsQueryVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
}>;


export type AiAgentEvalTestsQuery = { __typename?: 'Query', aiAgentEvalTests: Array<{ __typename?: 'AiAgentEvalTest', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null, scenarios: Array<{ __typename?: 'AiAgentEvalScenario', id: string, name: string, type: AiAgentScenarioType, userMessage?: string | null, expectedOutput?: string | null, personaPrompt?: string | null, maxTurns?: number | null, numberOfRuns?: number | null, createdDate?: any | null, lastModifiedDate?: any | null, toolSimulations: Array<{ __typename?: 'AiAgentScenarioToolSimulation', id: string, toolName: string, responsePrompt: string, simulationModel?: string | null }>, judges: Array<{ __typename?: 'AiAgentScenarioJudge', id: string, name: string, type: AiAgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null }> }> }> };

export type AiAgentJudgesQueryVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
}>;


export type AiAgentJudgesQuery = { __typename?: 'Query', aiAgentJudges: Array<{ __typename?: 'AiAgentJudge', id: string, name: string, type: AiAgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null }> };

export type CancelAiAgentEvalRunMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type CancelAiAgentEvalRunMutation = { __typename?: 'Mutation', cancelAiAgentEvalRun: { __typename?: 'AiAgentEvalRun', id: string, status: AiAgentEvalRunStatus } };

export type CreateAiAgentEvalScenarioMutationVariables = Exact<{
  agentEvalTestId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  type: AiAgentScenarioType;
  userMessage?: InputMaybe<Scalars['String']['input']>;
  expectedOutput?: InputMaybe<Scalars['String']['input']>;
  personaPrompt?: InputMaybe<Scalars['String']['input']>;
  maxTurns?: InputMaybe<Scalars['Int']['input']>;
  numberOfRuns?: InputMaybe<Scalars['Int']['input']>;
}>;


export type CreateAiAgentEvalScenarioMutation = { __typename?: 'Mutation', createAiAgentEvalScenario: { __typename?: 'AiAgentEvalScenario', id: string, name: string, type: AiAgentScenarioType, userMessage?: string | null, expectedOutput?: string | null, personaPrompt?: string | null, maxTurns?: number | null, numberOfRuns?: number | null, createdDate?: any | null, lastModifiedDate?: any | null, judges: Array<{ __typename?: 'AiAgentScenarioJudge', id: string, name: string, type: AiAgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null }> } };

export type CreateAiAgentEvalTestMutationVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
  name: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
}>;


export type CreateAiAgentEvalTestMutation = { __typename?: 'Mutation', createAiAgentEvalTest: { __typename?: 'AiAgentEvalTest', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null } };

export type CreateAiAgentJudgeMutationVariables = Exact<{
  workflowId: Scalars['String']['input'];
  workflowNodeName: Scalars['String']['input'];
  name: Scalars['String']['input'];
  type: AiAgentJudgeType;
  configuration: Scalars['Map']['input'];
}>;


export type CreateAiAgentJudgeMutation = { __typename?: 'Mutation', createAiAgentJudge: { __typename?: 'AiAgentJudge', id: string, name: string, type: AiAgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null } };

export type CreateAiAgentScenarioJudgeMutationVariables = Exact<{
  agentEvalScenarioId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  type: AiAgentJudgeType;
  configuration: Scalars['Map']['input'];
}>;


export type CreateAiAgentScenarioJudgeMutation = { __typename?: 'Mutation', createAiAgentScenarioJudge: { __typename?: 'AiAgentScenarioJudge', id: string, name: string, type: AiAgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null } };

export type CreateAiAgentScenarioToolSimulationMutationVariables = Exact<{
  agentEvalScenarioId: Scalars['ID']['input'];
  toolName: Scalars['String']['input'];
  responsePrompt: Scalars['String']['input'];
  simulationModel?: InputMaybe<Scalars['String']['input']>;
}>;


export type CreateAiAgentScenarioToolSimulationMutation = { __typename?: 'Mutation', createAiAgentScenarioToolSimulation: { __typename?: 'AiAgentScenarioToolSimulation', id: string, toolName: string, responsePrompt: string, simulationModel?: string | null } };

export type DeleteAiAgentEvalScenarioMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiAgentEvalScenarioMutation = { __typename?: 'Mutation', deleteAiAgentEvalScenario: boolean };

export type DeleteAiAgentEvalTestMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiAgentEvalTestMutation = { __typename?: 'Mutation', deleteAiAgentEvalTest: boolean };

export type DeleteAiAgentJudgeMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiAgentJudgeMutation = { __typename?: 'Mutation', deleteAiAgentJudge: boolean };

export type DeleteAiAgentScenarioJudgeMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiAgentScenarioJudgeMutation = { __typename?: 'Mutation', deleteAiAgentScenarioJudge: boolean };

export type DeleteAiAgentScenarioToolSimulationMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiAgentScenarioToolSimulationMutation = { __typename?: 'Mutation', deleteAiAgentScenarioToolSimulation: boolean };

export type StartAiAgentEvalRunMutationVariables = Exact<{
  agentEvalTestId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  environmentId: Scalars['ID']['input'];
  scenarioIds?: InputMaybe<Array<Scalars['ID']['input']> | Scalars['ID']['input']>;
  aiAgentJudgeIds?: InputMaybe<Array<Scalars['ID']['input']> | Scalars['ID']['input']>;
}>;


export type StartAiAgentEvalRunMutation = { __typename?: 'Mutation', startAiAgentEvalRun: { __typename?: 'AiAgentEvalRun', id: string, name: string, status: AiAgentEvalRunStatus, totalScenarios: number, completedScenarios: number, agentVersion?: string | null, createdDate?: any | null } };

export type UpdateAiAgentEvalScenarioMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
  userMessage?: InputMaybe<Scalars['String']['input']>;
  expectedOutput?: InputMaybe<Scalars['String']['input']>;
  personaPrompt?: InputMaybe<Scalars['String']['input']>;
  maxTurns?: InputMaybe<Scalars['Int']['input']>;
  numberOfRuns?: InputMaybe<Scalars['Int']['input']>;
}>;


export type UpdateAiAgentEvalScenarioMutation = { __typename?: 'Mutation', updateAiAgentEvalScenario: { __typename?: 'AiAgentEvalScenario', id: string, name: string, type: AiAgentScenarioType, userMessage?: string | null, expectedOutput?: string | null, personaPrompt?: string | null, maxTurns?: number | null, numberOfRuns?: number | null, createdDate?: any | null, lastModifiedDate?: any | null, judges: Array<{ __typename?: 'AiAgentScenarioJudge', id: string, name: string, type: AiAgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null }> } };

export type UpdateAiAgentEvalTestMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
  description?: InputMaybe<Scalars['String']['input']>;
}>;


export type UpdateAiAgentEvalTestMutation = { __typename?: 'Mutation', updateAiAgentEvalTest: { __typename?: 'AiAgentEvalTest', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null } };

export type UpdateAiAgentJudgeMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
  configuration?: InputMaybe<Scalars['Map']['input']>;
}>;


export type UpdateAiAgentJudgeMutation = { __typename?: 'Mutation', updateAiAgentJudge: { __typename?: 'AiAgentJudge', id: string, name: string, type: AiAgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null } };

export type UpdateAiAgentScenarioJudgeMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
  configuration?: InputMaybe<Scalars['Map']['input']>;
}>;


export type UpdateAiAgentScenarioJudgeMutation = { __typename?: 'Mutation', updateAiAgentScenarioJudge: { __typename?: 'AiAgentScenarioJudge', id: string, name: string, type: AiAgentJudgeType, configuration: any, createdDate?: any | null, lastModifiedDate?: any | null } };

export type UpdateAiAgentScenarioToolSimulationMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  toolName?: InputMaybe<Scalars['String']['input']>;
  responsePrompt?: InputMaybe<Scalars['String']['input']>;
  simulationModel?: InputMaybe<Scalars['String']['input']>;
}>;


export type UpdateAiAgentScenarioToolSimulationMutation = { __typename?: 'Mutation', updateAiAgentScenarioToolSimulation: { __typename?: 'AiAgentScenarioToolSimulation', id: string, toolName: string, responsePrompt: string, simulationModel?: string | null } };

export type AiAgentSkillQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AiAgentSkillQuery = { __typename?: 'Query', aiAgentSkill: { __typename?: 'AiAgentSkill', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null } };

export type AiAgentSkillFileContentQueryVariables = Exact<{
  id: Scalars['ID']['input'];
  path: Scalars['String']['input'];
}>;


export type AiAgentSkillFileContentQuery = { __typename?: 'Query', aiAgentSkillFileContent: string };

export type AiAgentSkillFilePathsQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AiAgentSkillFilePathsQuery = { __typename?: 'Query', aiAgentSkillFilePaths: Array<string> };

export type AiAgentSkillsQueryVariables = Exact<{ [key: string]: never; }>;


export type AiAgentSkillsQuery = { __typename?: 'Query', aiAgentSkills: Array<{ __typename?: 'AiAgentSkill', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null }> };

export type CreateAiAgentSkillMutationVariables = Exact<{
  name: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
  filename: Scalars['String']['input'];
  fileBytes: Scalars['String']['input'];
}>;


export type CreateAiAgentSkillMutation = { __typename?: 'Mutation', createAiAgentSkill: { __typename?: 'AiAgentSkill', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null } };

export type CreateAiAgentSkillFromInstructionsMutationVariables = Exact<{
  name: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
  instructions: Scalars['String']['input'];
}>;


export type CreateAiAgentSkillFromInstructionsMutation = { __typename?: 'Mutation', createAiAgentSkillFromInstructions: { __typename?: 'AiAgentSkill', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null } };

export type DeleteAiAgentSkillMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiAgentSkillMutation = { __typename?: 'Mutation', deleteAiAgentSkill: boolean };

export type UpdateAiAgentSkillMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
}>;


export type UpdateAiAgentSkillMutation = { __typename?: 'Mutation', updateAiAgentSkill: { __typename?: 'AiAgentSkill', id: string, name: string, description?: string | null, createdDate?: any | null, lastModifiedDate?: any | null } };

export type AuditEventsQueryVariables = Exact<{
  principal?: InputMaybe<Scalars['String']['input']>;
  eventType?: InputMaybe<Scalars['String']['input']>;
  fromDate?: InputMaybe<Scalars['Long']['input']>;
  toDate?: InputMaybe<Scalars['Long']['input']>;
  dataSearch?: InputMaybe<Scalars['String']['input']>;
  page?: InputMaybe<Scalars['Int']['input']>;
  size?: InputMaybe<Scalars['Int']['input']>;
}>;


export type AuditEventsQuery = { __typename?: 'Query', auditEvents: { __typename?: 'AuditEventPageType', number: number, size: number, totalElements: number, totalPages: number, content: Array<{ __typename?: 'AuditEventType', eventDate: any, eventType: string, id: string, principal?: string | null, data: Array<{ __typename?: 'AuditEventDataEntryType', key: string, value: string }> }> } };

export type AuditEventTypesQueryVariables = Exact<{ [key: string]: never; }>;


export type AuditEventTypesQuery = { __typename?: 'Query', auditEventTypes: Array<string> };

export type AiEvalRulesQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type AiEvalRulesQuery = { __typename?: 'Query', aiEvalRules?: Array<{ __typename?: 'AiEvalRule', createdDate?: any | null, delaySeconds?: number | null, enabled: boolean, filters?: string | null, id: string, lastModifiedDate?: any | null, model: string, name: string, projectId?: string | null, promptTemplate: string, samplingRate: number, scoreConfigId: string, version?: number | null, workspaceId: string } | null> | null };

export type AiEvalRuleQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AiEvalRuleQuery = { __typename?: 'Query', aiEvalRule?: { __typename?: 'AiEvalRule', createdDate?: any | null, delaySeconds?: number | null, enabled: boolean, filters?: string | null, id: string, lastModifiedDate?: any | null, model: string, name: string, projectId?: string | null, promptTemplate: string, samplingRate: number, scoreConfigId: string, version?: number | null, workspaceId: string } | null };

export type AiEvalExecutionsQueryVariables = Exact<{
  evalRuleId: Scalars['ID']['input'];
}>;


export type AiEvalExecutionsQuery = { __typename?: 'Query', aiEvalExecutions?: Array<{ __typename?: 'AiEvalExecution', createdDate?: any | null, errorMessage?: string | null, evalRuleId: string, id: string, scoreId?: string | null, status: AiEvalExecutionStatus, traceId: string } | null> | null };

export type CreateAiEvalRuleMutationVariables = Exact<{
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
}>;


export type CreateAiEvalRuleMutation = { __typename?: 'Mutation', createAiEvalRule?: { __typename?: 'AiEvalRule', id: string, name: string } | null };

export type DeleteAiEvalRuleMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiEvalRuleMutation = { __typename?: 'Mutation', deleteAiEvalRule?: boolean | null };

export type RunAiEvalRuleOnHistoricalTracesMutationVariables = Exact<{
  ruleId: Scalars['ID']['input'];
  startDate: Scalars['Long']['input'];
  endDate: Scalars['Long']['input'];
}>;


export type RunAiEvalRuleOnHistoricalTracesMutation = { __typename?: 'Mutation', runAiEvalRuleOnHistoricalTraces?: number | null };

export type UpdateAiEvalRuleMutationVariables = Exact<{
  delaySeconds?: InputMaybe<Scalars['Int']['input']>;
  enabled: Scalars['Boolean']['input'];
  filters?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  model: Scalars['String']['input'];
  name: Scalars['String']['input'];
  promptTemplate: Scalars['String']['input'];
  samplingRate: Scalars['Float']['input'];
  scoreConfigId: Scalars['ID']['input'];
}>;


export type UpdateAiEvalRuleMutation = { __typename?: 'Mutation', updateAiEvalRule?: { __typename?: 'AiEvalRule', id: string, name: string } | null };

export type AiEvalScoreConfigsQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type AiEvalScoreConfigsQuery = { __typename?: 'Query', aiEvalScoreConfigs?: Array<{ __typename?: 'AiEvalScoreConfig', categories?: string | null, createdDate?: any | null, dataType?: AiEvalScoreDataType | null, description?: string | null, id: string, lastModifiedDate?: any | null, maxValue?: number | null, minValue?: number | null, name: string, version?: number | null, workspaceId: string } | null> | null };

export type AiEvalScoreConfigQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AiEvalScoreConfigQuery = { __typename?: 'Query', aiEvalScoreConfig?: { __typename?: 'AiEvalScoreConfig', categories?: string | null, createdDate?: any | null, dataType?: AiEvalScoreDataType | null, description?: string | null, id: string, lastModifiedDate?: any | null, maxValue?: number | null, minValue?: number | null, name: string, version?: number | null, workspaceId: string } | null };

export type CreateAiEvalScoreConfigMutationVariables = Exact<{
  categories?: InputMaybe<Scalars['String']['input']>;
  dataType?: InputMaybe<AiEvalScoreDataType>;
  description?: InputMaybe<Scalars['String']['input']>;
  maxValue?: InputMaybe<Scalars['Float']['input']>;
  minValue?: InputMaybe<Scalars['Float']['input']>;
  name: Scalars['String']['input'];
  workspaceId: Scalars['ID']['input'];
}>;


export type CreateAiEvalScoreConfigMutation = { __typename?: 'Mutation', createAiEvalScoreConfig?: { __typename?: 'AiEvalScoreConfig', id: string, name: string } | null };

export type DeleteAiEvalScoreConfigMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiEvalScoreConfigMutation = { __typename?: 'Mutation', deleteAiEvalScoreConfig?: boolean | null };

export type UpdateAiEvalScoreConfigMutationVariables = Exact<{
  categories?: InputMaybe<Scalars['String']['input']>;
  dataType?: InputMaybe<AiEvalScoreDataType>;
  description?: InputMaybe<Scalars['String']['input']>;
  id: Scalars['ID']['input'];
  maxValue?: InputMaybe<Scalars['Float']['input']>;
  minValue?: InputMaybe<Scalars['Float']['input']>;
  name: Scalars['String']['input'];
}>;


export type UpdateAiEvalScoreConfigMutation = { __typename?: 'Mutation', updateAiEvalScoreConfig?: { __typename?: 'AiEvalScoreConfig', id: string, name: string } | null };

export type AiEvalScoresQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type AiEvalScoresQuery = { __typename?: 'Query', aiEvalScores?: Array<{ __typename?: 'AiEvalScore', comment?: string | null, createdBy?: string | null, createdDate?: any | null, dataType?: AiEvalScoreDataType | null, evalRuleId?: string | null, id: string, name: string, source: AiEvalScoreSource, spanId?: string | null, stringValue?: string | null, traceId: string, value?: number | null, workspaceId: string } | null> | null };

export type AiEvalScoresByTraceQueryVariables = Exact<{
  traceId: Scalars['ID']['input'];
}>;


export type AiEvalScoresByTraceQuery = { __typename?: 'Query', aiEvalScoresByTrace?: Array<{ __typename?: 'AiEvalScore', comment?: string | null, createdBy?: string | null, createdDate?: any | null, dataType?: AiEvalScoreDataType | null, evalRuleId?: string | null, id: string, name: string, source: AiEvalScoreSource, spanId?: string | null, stringValue?: string | null, traceId: string, value?: number | null, workspaceId: string } | null> | null };

export type CreateAiEvalScoreMutationVariables = Exact<{
  comment?: InputMaybe<Scalars['String']['input']>;
  dataType: AiEvalScoreDataType;
  name: Scalars['String']['input'];
  source: AiEvalScoreSource;
  spanId?: InputMaybe<Scalars['ID']['input']>;
  stringValue?: InputMaybe<Scalars['String']['input']>;
  traceId: Scalars['ID']['input'];
  value?: InputMaybe<Scalars['Float']['input']>;
  workspaceId: Scalars['ID']['input'];
}>;


export type CreateAiEvalScoreMutation = { __typename?: 'Mutation', createAiEvalScore?: { __typename?: 'AiEvalScore', id: string, name: string } | null };

export type DeleteAiEvalScoreMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiEvalScoreMutation = { __typename?: 'Mutation', deleteAiEvalScore?: boolean | null };

export type AiEvalScoreAnalyticsQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  startDate: Scalars['Long']['input'];
  endDate: Scalars['Long']['input'];
}>;


export type AiEvalScoreAnalyticsQuery = { __typename?: 'Query', aiEvalScoreAnalytics?: Array<{ __typename?: 'AiEvalScoreAnalytics', average?: number | null, count?: number | null, dataType?: AiEvalScoreDataType | null, max?: number | null, min?: number | null, name?: string | null, distribution?: Array<{ __typename?: 'AiEvalScoreDistributionEntry', count?: number | null, value?: string | null } | null> | null } | null> | null };

export type AiEvalScoreTrendQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  startDate: Scalars['Long']['input'];
  endDate: Scalars['Long']['input'];
}>;


export type AiEvalScoreTrendQuery = { __typename?: 'Query', aiEvalScoreTrend?: Array<{ __typename?: 'AiEvalScoreTrendPoint', average?: number | null, count: number, day: any } | null> | null };

export type AiGatewayBudgetQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type AiGatewayBudgetQuery = { __typename?: 'Query', aiGatewayBudget?: { __typename?: 'AiGatewayBudget', alertThreshold: number, amount: string, createdDate?: any | null, enabled: boolean, enforcementMode: AiGatewayBudgetEnforcementMode, id: string, lastModifiedDate?: any | null, period: AiGatewayBudgetPeriod, version?: number | null, workspaceId: string } | null };

export type CreateAiGatewayBudgetMutationVariables = Exact<{
  input: CreateAiGatewayBudgetInput;
}>;


export type CreateAiGatewayBudgetMutation = { __typename?: 'Mutation', createAiGatewayBudget?: { __typename?: 'AiGatewayBudget', alertThreshold: number, amount: string, createdDate?: any | null, enabled: boolean, enforcementMode: AiGatewayBudgetEnforcementMode, id: string, lastModifiedDate?: any | null, period: AiGatewayBudgetPeriod, version?: number | null, workspaceId: string } | null };

export type UpdateAiGatewayBudgetMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayBudgetInput;
}>;


export type UpdateAiGatewayBudgetMutation = { __typename?: 'Mutation', updateAiGatewayBudget?: { __typename?: 'AiGatewayBudget', alertThreshold: number, amount: string, createdDate?: any | null, enabled: boolean, enforcementMode: AiGatewayBudgetEnforcementMode, id: string, lastModifiedDate?: any | null, period: AiGatewayBudgetPeriod, version?: number | null, workspaceId: string } | null };

export type DeleteAiGatewayBudgetMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiGatewayBudgetMutation = { __typename?: 'Mutation', deleteAiGatewayBudget?: boolean | null };

export type AiGatewayModelsQueryVariables = Exact<{ [key: string]: never; }>;


export type AiGatewayModelsQuery = { __typename?: 'Query', aiGatewayModels?: Array<{ __typename?: 'AiGatewayModel', alias?: string | null, capabilities?: string | null, contextWindow?: number | null, createdDate?: any | null, enabled: boolean, id: string, inputCostPerMTokens?: number | null, lastModifiedDate?: any | null, name: string, outputCostPerMTokens?: number | null, providerId: string, version?: number | null } | null> | null };

export type AiGatewayModelsByProviderQueryVariables = Exact<{
  providerId: Scalars['ID']['input'];
}>;


export type AiGatewayModelsByProviderQuery = { __typename?: 'Query', aiGatewayModelsByProvider?: Array<{ __typename?: 'AiGatewayModel', alias?: string | null, capabilities?: string | null, contextWindow?: number | null, createdDate?: any | null, enabled: boolean, id: string, inputCostPerMTokens?: number | null, lastModifiedDate?: any | null, name: string, outputCostPerMTokens?: number | null, providerId: string, version?: number | null } | null> | null };

export type CreateAiGatewayModelMutationVariables = Exact<{
  input: CreateAiGatewayModelInput;
}>;


export type CreateAiGatewayModelMutation = { __typename?: 'Mutation', createAiGatewayModel?: { __typename?: 'AiGatewayModel', alias?: string | null, capabilities?: string | null, contextWindow?: number | null, createdDate?: any | null, enabled: boolean, id: string, inputCostPerMTokens?: number | null, lastModifiedDate?: any | null, name: string, outputCostPerMTokens?: number | null, providerId: string, version?: number | null } | null };

export type UpdateAiGatewayModelMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayModelInput;
}>;


export type UpdateAiGatewayModelMutation = { __typename?: 'Mutation', updateAiGatewayModel?: { __typename?: 'AiGatewayModel', alias?: string | null, capabilities?: string | null, contextWindow?: number | null, createdDate?: any | null, enabled: boolean, id: string, inputCostPerMTokens?: number | null, lastModifiedDate?: any | null, name: string, outputCostPerMTokens?: number | null, providerId: string, version?: number | null } | null };

export type DeleteAiGatewayModelMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiGatewayModelMutation = { __typename?: 'Mutation', deleteAiGatewayModel?: boolean | null };

export type PlaygroundChatCompletionMutationVariables = Exact<{
  input: PlaygroundChatCompletionInput;
}>;


export type PlaygroundChatCompletionMutation = { __typename?: 'Mutation', playgroundChatCompletion?: { __typename?: 'PlaygroundChatCompletionResponse', completionTokens?: number | null, content?: string | null, cost?: number | null, finishReason?: string | null, latencyMs?: number | null, model?: string | null, promptTokens?: number | null, totalTokens?: number | null, traceId?: string | null } | null };

export type AiGatewayProjectsQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type AiGatewayProjectsQuery = { __typename?: 'Query', aiGatewayProjects: Array<{ __typename?: 'AiGatewayProject', cachingEnabled?: boolean | null, cacheTtlMinutes?: number | null, compressionEnabled?: boolean | null, createdDate?: any | null, description?: string | null, id: string, lastModifiedDate?: any | null, logRetentionDays?: number | null, name: string, retryMaxAttempts?: number | null, routingPolicyId?: string | null, slug: string, timeoutSeconds?: number | null, version?: number | null, workspaceId: string }> };

export type CreateAiGatewayProjectMutationVariables = Exact<{
  input: CreateAiGatewayProjectInput;
}>;


export type CreateAiGatewayProjectMutation = { __typename?: 'Mutation', createAiGatewayProject?: { __typename?: 'AiGatewayProject', id: string, name: string, slug: string } | null };

export type UpdateAiGatewayProjectMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayProjectInput;
}>;


export type UpdateAiGatewayProjectMutation = { __typename?: 'Mutation', updateAiGatewayProject?: { __typename?: 'AiGatewayProject', id: string, name: string, slug: string } | null };

export type DeleteAiGatewayProjectMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiGatewayProjectMutation = { __typename?: 'Mutation', deleteAiGatewayProject?: boolean | null };

export type AiGatewayProvidersQueryVariables = Exact<{ [key: string]: never; }>;


export type AiGatewayProvidersQuery = { __typename?: 'Query', aiGatewayProviders?: Array<{ __typename?: 'AiGatewayProvider', baseUrl?: string | null, config?: string | null, createdBy?: string | null, createdDate?: any | null, enabled: boolean, id: string, lastModifiedBy?: string | null, lastModifiedDate?: any | null, name: string, type: AiGatewayProviderType, version?: number | null } | null> | null };

export type AiGatewayProviderQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AiGatewayProviderQuery = { __typename?: 'Query', aiGatewayProvider?: { __typename?: 'AiGatewayProvider', baseUrl?: string | null, config?: string | null, createdBy?: string | null, createdDate?: any | null, enabled: boolean, id: string, lastModifiedBy?: string | null, lastModifiedDate?: any | null, name: string, type: AiGatewayProviderType, version?: number | null } | null };

export type CreateAiGatewayProviderMutationVariables = Exact<{
  input: CreateAiGatewayProviderInput;
}>;


export type CreateAiGatewayProviderMutation = { __typename?: 'Mutation', createAiGatewayProvider?: { __typename?: 'AiGatewayProvider', baseUrl?: string | null, config?: string | null, createdBy?: string | null, createdDate?: any | null, enabled: boolean, id: string, lastModifiedBy?: string | null, lastModifiedDate?: any | null, name: string, type: AiGatewayProviderType, version?: number | null } | null };

export type UpdateAiGatewayProviderMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayProviderInput;
}>;


export type UpdateAiGatewayProviderMutation = { __typename?: 'Mutation', updateAiGatewayProvider?: { __typename?: 'AiGatewayProvider', baseUrl?: string | null, config?: string | null, createdBy?: string | null, createdDate?: any | null, enabled: boolean, id: string, lastModifiedBy?: string | null, lastModifiedDate?: any | null, name: string, type: AiGatewayProviderType, version?: number | null } | null };

export type DeleteAiGatewayProviderMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiGatewayProviderMutation = { __typename?: 'Mutation', deleteAiGatewayProvider?: boolean | null };

export type AiGatewayRateLimitsQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type AiGatewayRateLimitsQuery = { __typename?: 'Query', aiGatewayRateLimits: Array<{ __typename?: 'AiGatewayRateLimit', createdDate?: any | null, enabled: boolean, id: string, lastModifiedDate?: any | null, limitType: AiGatewayRateLimitType, limitValue: number, name: string, projectId?: string | null, propertyKey?: string | null, scope: AiGatewayRateLimitScope, version?: number | null, windowSeconds: number, workspaceId: string }> };

export type CreateAiGatewayRateLimitMutationVariables = Exact<{
  input: CreateAiGatewayRateLimitInput;
}>;


export type CreateAiGatewayRateLimitMutation = { __typename?: 'Mutation', createAiGatewayRateLimit?: { __typename?: 'AiGatewayRateLimit', createdDate?: any | null, enabled: boolean, id: string, lastModifiedDate?: any | null, limitType: AiGatewayRateLimitType, limitValue: number, name: string, projectId?: string | null, propertyKey?: string | null, scope: AiGatewayRateLimitScope, version?: number | null, windowSeconds: number, workspaceId: string } | null };

export type UpdateAiGatewayRateLimitMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayRateLimitInput;
}>;


export type UpdateAiGatewayRateLimitMutation = { __typename?: 'Mutation', updateAiGatewayRateLimit?: { __typename?: 'AiGatewayRateLimit', createdDate?: any | null, enabled: boolean, id: string, lastModifiedDate?: any | null, limitType: AiGatewayRateLimitType, limitValue: number, name: string, projectId?: string | null, propertyKey?: string | null, scope: AiGatewayRateLimitScope, version?: number | null, windowSeconds: number, workspaceId: string } | null };

export type DeleteAiGatewayRateLimitMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiGatewayRateLimitMutation = { __typename?: 'Mutation', deleteAiGatewayRateLimit?: boolean | null };

export type AiGatewayRequestLogsQueryVariables = Exact<{
  startDate: Scalars['Long']['input'];
  endDate: Scalars['Long']['input'];
}>;


export type AiGatewayRequestLogsQuery = { __typename?: 'Query', aiGatewayRequestLogs?: Array<{ __typename?: 'AiGatewayRequestLog', apiKeyId?: string | null, cacheHit?: boolean | null, cost?: string | null, createdDate?: any | null, errorMessage?: string | null, id: string, inputTokens?: number | null, latencyMs?: number | null, outputTokens?: number | null, requestId: string, requestedModel?: string | null, routedModel?: string | null, routedProvider?: string | null, routingPolicyId?: string | null, routingStrategy?: string | null, status?: number | null } | null> | null };

export type AiGatewayRoutingPoliciesQueryVariables = Exact<{ [key: string]: never; }>;


export type AiGatewayRoutingPoliciesQuery = { __typename?: 'Query', aiGatewayRoutingPolicies?: Array<{ __typename?: 'AiGatewayRoutingPolicy', config?: string | null, createdDate?: any | null, enabled: boolean, fallbackModel?: string | null, id: string, lastModifiedDate?: any | null, name: string, strategy: AiGatewayRoutingStrategyType, version?: number | null, deployments?: Array<{ __typename?: 'AiGatewayModelDeployment', enabled: boolean, id: string, maxRpm?: number | null, maxTpm?: number | null, modelId: string, priorityOrder: number, routingPolicyId: string, weight: number } | null> | null } | null> | null };

export type CreateAiGatewayRoutingPolicyMutationVariables = Exact<{
  input: CreateAiGatewayRoutingPolicyInput;
}>;


export type CreateAiGatewayRoutingPolicyMutation = { __typename?: 'Mutation', createAiGatewayRoutingPolicy?: { __typename?: 'AiGatewayRoutingPolicy', config?: string | null, createdDate?: any | null, enabled: boolean, fallbackModel?: string | null, id: string, lastModifiedDate?: any | null, name: string, strategy: AiGatewayRoutingStrategyType, version?: number | null } | null };

export type UpdateAiGatewayRoutingPolicyMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayRoutingPolicyInput;
}>;


export type UpdateAiGatewayRoutingPolicyMutation = { __typename?: 'Mutation', updateAiGatewayRoutingPolicy?: { __typename?: 'AiGatewayRoutingPolicy', config?: string | null, createdDate?: any | null, enabled: boolean, fallbackModel?: string | null, id: string, lastModifiedDate?: any | null, name: string, strategy: AiGatewayRoutingStrategyType, version?: number | null } | null };

export type DeleteAiGatewayRoutingPolicyMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiGatewayRoutingPolicyMutation = { __typename?: 'Mutation', deleteAiGatewayRoutingPolicy?: boolean | null };

export type AiGatewaySpendSummariesQueryVariables = Exact<{
  startDate: Scalars['Long']['input'];
  endDate: Scalars['Long']['input'];
}>;


export type AiGatewaySpendSummariesQuery = { __typename?: 'Query', aiGatewaySpendSummaries?: Array<{ __typename?: 'AiGatewaySpendSummary', apiKeyId?: string | null, createdDate?: any | null, id: string, model?: string | null, periodEnd?: any | null, periodStart?: any | null, provider?: string | null, requestCount?: number | null, totalCost?: string | null, totalInputTokens?: any | null, totalOutputTokens?: any | null } | null> | null };

export type AiGatewayTagsQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type AiGatewayTagsQuery = { __typename?: 'Query', aiGatewayTags?: Array<{ __typename?: 'AiGatewayTag', color?: string | null, createdDate?: any | null, id: string, lastModifiedDate?: any | null, name: string, version?: number | null, workspaceId: string } | null> | null };

export type CreateAiGatewayTagMutationVariables = Exact<{
  input: CreateAiGatewayTagInput;
}>;


export type CreateAiGatewayTagMutation = { __typename?: 'Mutation', createAiGatewayTag?: { __typename?: 'AiGatewayTag', color?: string | null, id: string, name: string } | null };

export type UpdateAiGatewayTagMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  name?: InputMaybe<Scalars['String']['input']>;
  color?: InputMaybe<Scalars['String']['input']>;
}>;


export type UpdateAiGatewayTagMutation = { __typename?: 'Mutation', updateAiGatewayTag?: { __typename?: 'AiGatewayTag', color?: string | null, id: string, name: string } | null };

export type DeleteAiGatewayTagMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiGatewayTagMutation = { __typename?: 'Mutation', deleteAiGatewayTag?: boolean | null };

export type AiGatewayWorkspaceSettingsQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type AiGatewayWorkspaceSettingsQuery = { __typename?: 'Query', aiGatewayWorkspaceSettings?: { __typename?: 'AiGatewayWorkspaceSettings', cacheEnabled?: boolean | null, cacheTtlSeconds?: number | null, defaultRoutingPolicyId?: string | null, logRetentionDays?: number | null, redactPii?: boolean | null, retryCount?: number | null, softBudgetWarningPct?: number | null, timeoutMs?: number | null, workspaceId: string } | null };

export type UpdateAiGatewayWorkspaceSettingsMutationVariables = Exact<{
  input: AiGatewayWorkspaceSettingsInput;
}>;


export type UpdateAiGatewayWorkspaceSettingsMutation = { __typename?: 'Mutation', updateAiGatewayWorkspaceSettings?: { __typename?: 'AiGatewayWorkspaceSettings', cacheEnabled?: boolean | null, cacheTtlSeconds?: number | null, defaultRoutingPolicyId?: string | null, logRetentionDays?: number | null, redactPii?: boolean | null, retryCount?: number | null, softBudgetWarningPct?: number | null, timeoutMs?: number | null, workspaceId: string } | null };

export type AiObservabilityAlertEventsQueryVariables = Exact<{
  alertRuleId: Scalars['ID']['input'];
}>;


export type AiObservabilityAlertEventsQuery = { __typename?: 'Query', aiObservabilityAlertEvents?: Array<{ __typename?: 'AiObservabilityAlertEvent', alertRuleId: string, createdDate?: any | null, id: string, message?: string | null, status: AiObservabilityAlertEventStatus, triggeredValue?: number | null } | null> | null };

export type AcknowledgeAiObservabilityAlertEventMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AcknowledgeAiObservabilityAlertEventMutation = { __typename?: 'Mutation', acknowledgeAiObservabilityAlertEvent?: { __typename?: 'AiObservabilityAlertEvent', alertRuleId: string, createdDate?: any | null, id: string, message?: string | null, status: AiObservabilityAlertEventStatus, triggeredValue?: number | null } | null };

export type AiObservabilityAlertRulesQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type AiObservabilityAlertRulesQuery = { __typename?: 'Query', aiObservabilityAlertRules?: Array<{ __typename?: 'AiObservabilityAlertRule', channelIds?: Array<string | null> | null, condition: AiObservabilityAlertCondition, cooldownMinutes: number, createdDate?: any | null, enabled: boolean, filters?: string | null, id: string, lastModifiedDate?: any | null, metric: AiObservabilityAlertMetric, name: string, projectId?: string | null, snoozedUntil?: any | null, threshold: number, version?: number | null, windowMinutes: number, workspaceId: string } | null> | null };

export type AiObservabilityAlertRuleQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AiObservabilityAlertRuleQuery = { __typename?: 'Query', aiObservabilityAlertRule?: { __typename?: 'AiObservabilityAlertRule', channelIds?: Array<string | null> | null, condition: AiObservabilityAlertCondition, cooldownMinutes: number, createdDate?: any | null, enabled: boolean, filters?: string | null, id: string, lastModifiedDate?: any | null, metric: AiObservabilityAlertMetric, name: string, projectId?: string | null, snoozedUntil?: any | null, threshold: number, version?: number | null, windowMinutes: number, workspaceId: string } | null };

export type CreateAiObservabilityAlertRuleMutationVariables = Exact<{
  input: AiObservabilityAlertRuleInput;
}>;


export type CreateAiObservabilityAlertRuleMutation = { __typename?: 'Mutation', createAiObservabilityAlertRule?: { __typename?: 'AiObservabilityAlertRule', channelIds?: Array<string | null> | null, condition: AiObservabilityAlertCondition, cooldownMinutes: number, createdDate?: any | null, enabled: boolean, filters?: string | null, id: string, lastModifiedDate?: any | null, metric: AiObservabilityAlertMetric, name: string, projectId?: string | null, threshold: number, version?: number | null, windowMinutes: number, workspaceId: string } | null };

export type UpdateAiObservabilityAlertRuleMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: AiObservabilityAlertRuleInput;
}>;


export type UpdateAiObservabilityAlertRuleMutation = { __typename?: 'Mutation', updateAiObservabilityAlertRule?: { __typename?: 'AiObservabilityAlertRule', channelIds?: Array<string | null> | null, condition: AiObservabilityAlertCondition, cooldownMinutes: number, createdDate?: any | null, enabled: boolean, filters?: string | null, id: string, lastModifiedDate?: any | null, metric: AiObservabilityAlertMetric, name: string, projectId?: string | null, threshold: number, version?: number | null, windowMinutes: number, workspaceId: string } | null };

export type DeleteAiObservabilityAlertRuleMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiObservabilityAlertRuleMutation = { __typename?: 'Mutation', deleteAiObservabilityAlertRule?: boolean | null };

export type TestAiObservabilityAlertRuleMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type TestAiObservabilityAlertRuleMutation = { __typename?: 'Mutation', testAiObservabilityAlertRule?: number | null };

export type SnoozeAiObservabilityAlertRuleMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  until: Scalars['Long']['input'];
}>;


export type SnoozeAiObservabilityAlertRuleMutation = { __typename?: 'Mutation', snoozeAiObservabilityAlertRule?: { __typename?: 'AiObservabilityAlertRule', id: string, snoozedUntil?: any | null } | null };

export type UnsnoozeAiObservabilityAlertRuleMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type UnsnoozeAiObservabilityAlertRuleMutation = { __typename?: 'Mutation', unsnoozeAiObservabilityAlertRule?: { __typename?: 'AiObservabilityAlertRule', id: string, snoozedUntil?: any | null } | null };

export type AiObservabilityExportJobsQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type AiObservabilityExportJobsQuery = { __typename?: 'Query', aiObservabilityExportJobs?: Array<{ __typename?: 'AiObservabilityExportJob', createdBy: string, createdDate?: any | null, errorMessage?: string | null, filePath?: string | null, filters?: string | null, format: AiObservabilityExportFormat, id: string, projectId?: string | null, recordCount?: number | null, scope: AiObservabilityExportScope, status: AiObservabilityExportJobStatus, type: AiObservabilityExportJobType, workspaceId: string } | null> | null };

export type AiObservabilityExportJobQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AiObservabilityExportJobQuery = { __typename?: 'Query', aiObservabilityExportJob?: { __typename?: 'AiObservabilityExportJob', createdBy: string, createdDate?: any | null, errorMessage?: string | null, filePath?: string | null, filters?: string | null, format: AiObservabilityExportFormat, id: string, projectId?: string | null, recordCount?: number | null, scope: AiObservabilityExportScope, status: AiObservabilityExportJobStatus, type: AiObservabilityExportJobType, workspaceId: string } | null };

export type CreateAiObservabilityExportJobMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  projectId?: InputMaybe<Scalars['ID']['input']>;
  format: AiObservabilityExportFormat;
  scope: AiObservabilityExportScope;
  filters?: InputMaybe<Scalars['String']['input']>;
}>;


export type CreateAiObservabilityExportJobMutation = { __typename?: 'Mutation', createAiObservabilityExportJob?: { __typename?: 'AiObservabilityExportJob', createdBy: string, createdDate?: any | null, format: AiObservabilityExportFormat, id: string, scope: AiObservabilityExportScope, status: AiObservabilityExportJobStatus, type: AiObservabilityExportJobType, workspaceId: string } | null };

export type CancelAiObservabilityExportJobMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type CancelAiObservabilityExportJobMutation = { __typename?: 'Mutation', cancelAiObservabilityExportJob?: { __typename?: 'AiObservabilityExportJob', id: string, status: AiObservabilityExportJobStatus } | null };

export type AiObservabilityNotificationChannelsQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type AiObservabilityNotificationChannelsQuery = { __typename?: 'Query', aiObservabilityNotificationChannels?: Array<{ __typename?: 'AiObservabilityNotificationChannel', config: string, createdDate?: any | null, enabled: boolean, id: string, lastModifiedDate?: any | null, name: string, type: AiObservabilityNotificationChannelType, version?: number | null, workspaceId: string } | null> | null };

export type CreateAiObservabilityNotificationChannelMutationVariables = Exact<{
  input: AiObservabilityNotificationChannelInput;
}>;


export type CreateAiObservabilityNotificationChannelMutation = { __typename?: 'Mutation', createAiObservabilityNotificationChannel?: { __typename?: 'AiObservabilityNotificationChannel', config: string, createdDate?: any | null, enabled: boolean, id: string, lastModifiedDate?: any | null, name: string, type: AiObservabilityNotificationChannelType, version?: number | null, workspaceId: string } | null };

export type UpdateAiObservabilityNotificationChannelMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: AiObservabilityNotificationChannelInput;
}>;


export type UpdateAiObservabilityNotificationChannelMutation = { __typename?: 'Mutation', updateAiObservabilityNotificationChannel?: { __typename?: 'AiObservabilityNotificationChannel', config: string, createdDate?: any | null, enabled: boolean, id: string, lastModifiedDate?: any | null, name: string, type: AiObservabilityNotificationChannelType, version?: number | null, workspaceId: string } | null };

export type DeleteAiObservabilityNotificationChannelMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiObservabilityNotificationChannelMutation = { __typename?: 'Mutation', deleteAiObservabilityNotificationChannel?: boolean | null };

export type TestAiObservabilityNotificationChannelMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type TestAiObservabilityNotificationChannelMutation = { __typename?: 'Mutation', testAiObservabilityNotificationChannel?: boolean | null };

export type AiObservabilitySessionsQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type AiObservabilitySessionsQuery = { __typename?: 'Query', aiObservabilitySessions?: Array<{ __typename?: 'AiObservabilitySession', createdDate?: any | null, id: string, lastModifiedDate?: any | null, name?: string | null, projectId?: string | null, traceCount?: number | null, userId?: string | null, version?: number | null, workspaceId: string } | null> | null };

export type AiObservabilitySessionQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AiObservabilitySessionQuery = { __typename?: 'Query', aiObservabilitySession?: { __typename?: 'AiObservabilitySession', createdDate?: any | null, id: string, lastModifiedDate?: any | null, name?: string | null, projectId?: string | null, userId?: string | null, version?: number | null, workspaceId: string, traces?: Array<{ __typename?: 'AiObservabilityTrace', createdDate?: any | null, id: string, name?: string | null, source: AiObservabilityTraceSource, status: AiObservabilityTraceStatus, totalCost?: number | null, totalInputTokens?: number | null, totalLatencyMs?: number | null, totalOutputTokens?: number | null, userId?: string | null } | null> | null } | null };

export type AiObservabilityTracesQueryVariables = Exact<{
  endDate: Scalars['Long']['input'];
  model?: InputMaybe<Scalars['String']['input']>;
  source?: InputMaybe<AiObservabilityTraceSource>;
  startDate: Scalars['Long']['input'];
  status?: InputMaybe<AiObservabilityTraceStatus>;
  tagId?: InputMaybe<Scalars['ID']['input']>;
  userId?: InputMaybe<Scalars['String']['input']>;
  workspaceId: Scalars['ID']['input'];
}>;


export type AiObservabilityTracesQuery = { __typename?: 'Query', aiObservabilityTraces?: Array<{ __typename?: 'AiObservabilityTrace', createdDate?: any | null, id: string, input?: string | null, lastModifiedDate?: any | null, metadata?: string | null, name?: string | null, output?: string | null, projectId?: string | null, sessionId?: string | null, source: AiObservabilityTraceSource, status: AiObservabilityTraceStatus, totalCost?: number | null, totalInputTokens?: number | null, totalLatencyMs?: number | null, totalOutputTokens?: number | null, userId?: string | null, version?: number | null, workspaceId: string } | null> | null };

export type AiObservabilityTraceQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AiObservabilityTraceQuery = { __typename?: 'Query', aiObservabilityTrace?: { __typename?: 'AiObservabilityTrace', createdDate?: any | null, id: string, input?: string | null, lastModifiedDate?: any | null, metadata?: string | null, name?: string | null, output?: string | null, projectId?: string | null, sessionId?: string | null, source: AiObservabilityTraceSource, status: AiObservabilityTraceStatus, tagIds?: Array<string> | null, totalCost?: number | null, totalInputTokens?: number | null, totalLatencyMs?: number | null, totalOutputTokens?: number | null, userId?: string | null, version?: number | null, workspaceId: string, spans?: Array<{ __typename?: 'AiObservabilitySpan', cost?: number | null, createdDate?: any | null, endTime?: any | null, id: string, input?: string | null, inputTokens?: number | null, latencyMs?: number | null, level: AiObservabilitySpanLevel, metadata?: string | null, model?: string | null, name?: string | null, output?: string | null, outputTokens?: number | null, parentSpanId?: string | null, provider?: string | null, startTime?: any | null, status: AiObservabilitySpanStatus, traceId: string, type: AiObservabilitySpanType, version?: number | null } | null> | null } | null };

export type SetAiObservabilityTraceTagsMutationVariables = Exact<{
  traceId: Scalars['ID']['input'];
  tagIds: Array<Scalars['ID']['input']> | Scalars['ID']['input'];
}>;


export type SetAiObservabilityTraceTagsMutation = { __typename?: 'Mutation', setAiObservabilityTraceTags?: { __typename?: 'AiObservabilityTrace', id: string, tagIds?: Array<string> | null } | null };

export type AiObservabilityWebhookDeliveriesQueryVariables = Exact<{
  subscriptionId: Scalars['ID']['input'];
}>;


export type AiObservabilityWebhookDeliveriesQuery = { __typename?: 'Query', aiObservabilityWebhookDeliveries?: Array<{ __typename?: 'AiObservabilityWebhookDelivery', attemptCount: number, createdDate?: any | null, deliveredDate?: any | null, errorMessage?: string | null, eventType?: string | null, httpStatus?: number | null, id: string, status: AiObservabilityWebhookDeliveryStatus, subscriptionId: string } | null> | null };

export type AiObservabilityWebhookSubscriptionsQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type AiObservabilityWebhookSubscriptionsQuery = { __typename?: 'Query', aiObservabilityWebhookSubscriptions?: Array<{ __typename?: 'AiObservabilityWebhookSubscription', createdDate?: any | null, enabled: boolean, events: string, id: string, lastModifiedDate?: any | null, lastTriggeredDate?: any | null, name: string, projectId?: string | null, url: string, version?: number | null, workspaceId: string } | null> | null };

export type AiObservabilityWebhookSubscriptionQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AiObservabilityWebhookSubscriptionQuery = { __typename?: 'Query', aiObservabilityWebhookSubscription?: { __typename?: 'AiObservabilityWebhookSubscription', createdDate?: any | null, enabled: boolean, events: string, id: string, lastModifiedDate?: any | null, lastTriggeredDate?: any | null, name: string, projectId?: string | null, url: string, version?: number | null, workspaceId: string } | null };

export type CreateAiObservabilityWebhookSubscriptionMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  projectId?: InputMaybe<Scalars['ID']['input']>;
  name: Scalars['String']['input'];
  url: Scalars['String']['input'];
  secret?: InputMaybe<Scalars['String']['input']>;
  events: Scalars['String']['input'];
  enabled: Scalars['Boolean']['input'];
}>;


export type CreateAiObservabilityWebhookSubscriptionMutation = { __typename?: 'Mutation', createAiObservabilityWebhookSubscription?: { __typename?: 'AiObservabilityWebhookSubscription', createdDate?: any | null, enabled: boolean, events: string, id: string, name: string, url: string, version?: number | null, workspaceId: string } | null };

export type UpdateAiObservabilityWebhookSubscriptionMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  url: Scalars['String']['input'];
  secret?: InputMaybe<Scalars['String']['input']>;
  events: Scalars['String']['input'];
  enabled: Scalars['Boolean']['input'];
}>;


export type UpdateAiObservabilityWebhookSubscriptionMutation = { __typename?: 'Mutation', updateAiObservabilityWebhookSubscription?: { __typename?: 'AiObservabilityWebhookSubscription', createdDate?: any | null, enabled: boolean, events: string, id: string, name: string, url: string, version?: number | null, workspaceId: string } | null };

export type DeleteAiObservabilityWebhookSubscriptionMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiObservabilityWebhookSubscriptionMutation = { __typename?: 'Mutation', deleteAiObservabilityWebhookSubscription?: boolean | null };

export type TestAiObservabilityWebhookSubscriptionMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type TestAiObservabilityWebhookSubscriptionMutation = { __typename?: 'Mutation', testAiObservabilityWebhookSubscription?: boolean | null };

export type AiPromptsQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type AiPromptsQuery = { __typename?: 'Query', aiPrompts?: Array<{ __typename?: 'AiPrompt', createdDate?: any | null, description?: string | null, id: string, lastModifiedDate?: any | null, name: string, projectId?: string | null, version?: number | null, workspaceId: string, versions?: Array<{ __typename?: 'AiPromptVersion', active: boolean, commitMessage?: string | null, content: string, createdBy: string, createdDate?: any | null, environment?: string | null, id: string, promptId: string, type: AiPromptVersionType, variables?: string | null, versionNumber: number, metrics?: { __typename?: 'AiPromptVersionMetrics', avgCostUsd?: number | null, avgLatencyMs?: number | null, errorRate?: number | null, invocationCount: number } | null } | null> | null } | null> | null };

export type AiPromptQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type AiPromptQuery = { __typename?: 'Query', aiPrompt?: { __typename?: 'AiPrompt', createdDate?: any | null, description?: string | null, id: string, lastModifiedDate?: any | null, name: string, projectId?: string | null, version?: number | null, workspaceId: string, versions?: Array<{ __typename?: 'AiPromptVersion', active: boolean, commitMessage?: string | null, content: string, createdBy: string, createdDate?: any | null, environment?: string | null, id: string, promptId: string, type: AiPromptVersionType, variables?: string | null, versionNumber: number, metrics?: { __typename?: 'AiPromptVersionMetrics', avgCostUsd?: number | null, avgLatencyMs?: number | null, errorRate?: number | null, invocationCount: number } | null } | null> | null } | null };

export type CreateAiPromptMutationVariables = Exact<{
  input: CreateAiPromptInput;
}>;


export type CreateAiPromptMutation = { __typename?: 'Mutation', createAiPrompt?: { __typename?: 'AiPrompt', createdDate?: any | null, description?: string | null, id: string, lastModifiedDate?: any | null, name: string, projectId?: string | null, version?: number | null, workspaceId: string } | null };

export type UpdateAiPromptMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: UpdateAiPromptInput;
}>;


export type UpdateAiPromptMutation = { __typename?: 'Mutation', updateAiPrompt?: { __typename?: 'AiPrompt', createdDate?: any | null, description?: string | null, id: string, lastModifiedDate?: any | null, name: string, projectId?: string | null, version?: number | null, workspaceId: string } | null };

export type DeleteAiPromptMutationVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type DeleteAiPromptMutation = { __typename?: 'Mutation', deleteAiPrompt?: boolean | null };

export type CreateAiPromptVersionMutationVariables = Exact<{
  input: CreateAiPromptVersionInput;
}>;


export type CreateAiPromptVersionMutation = { __typename?: 'Mutation', createAiPromptVersion?: { __typename?: 'AiPromptVersion', active: boolean, commitMessage?: string | null, content: string, createdBy: string, createdDate?: any | null, environment?: string | null, id: string, promptId: string, type: AiPromptVersionType, variables?: string | null, versionNumber: number } | null };

export type SetActiveAiPromptVersionMutationVariables = Exact<{
  promptVersionId: Scalars['ID']['input'];
  environment: Scalars['String']['input'];
}>;


export type SetActiveAiPromptVersionMutation = { __typename?: 'Mutation', setActiveAiPromptVersion?: boolean | null };

export type WorkspaceAiGatewayModelsQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type WorkspaceAiGatewayModelsQuery = { __typename?: 'Query', workspaceAiGatewayModels?: Array<{ __typename?: 'AiGatewayModel', alias?: string | null, capabilities?: string | null, contextWindow?: number | null, defaultRoutingPolicyId?: string | null, createdDate?: any | null, enabled: boolean, id: string, inputCostPerMTokens?: number | null, lastModifiedDate?: any | null, name: string, outputCostPerMTokens?: number | null, providerId: string, version?: number | null } | null> | null };

export type CreateWorkspaceAiGatewayModelMutationVariables = Exact<{
  input: CreateWorkspaceAiGatewayModelInput;
}>;


export type CreateWorkspaceAiGatewayModelMutation = { __typename?: 'Mutation', createWorkspaceAiGatewayModel?: { __typename?: 'AiGatewayModel', alias?: string | null, capabilities?: string | null, contextWindow?: number | null, defaultRoutingPolicyId?: string | null, createdDate?: any | null, enabled: boolean, id: string, inputCostPerMTokens?: number | null, lastModifiedDate?: any | null, name: string, outputCostPerMTokens?: number | null, providerId: string, version?: number | null } | null };

export type DeleteWorkspaceAiGatewayModelMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  modelId: Scalars['ID']['input'];
}>;


export type DeleteWorkspaceAiGatewayModelMutation = { __typename?: 'Mutation', deleteWorkspaceAiGatewayModel?: boolean | null };

export type UpdateWorkspaceAiGatewayModelMutationVariables = Exact<{
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayModelInput;
}>;


export type UpdateWorkspaceAiGatewayModelMutation = { __typename?: 'Mutation', updateWorkspaceAiGatewayModel?: { __typename?: 'AiGatewayModel', alias?: string | null, capabilities?: string | null, contextWindow?: number | null, defaultRoutingPolicyId?: string | null, createdDate?: any | null, enabled: boolean, id: string, inputCostPerMTokens?: number | null, lastModifiedDate?: any | null, name: string, outputCostPerMTokens?: number | null, providerId: string, version?: number | null } | null };

export type WorkspaceAiGatewayProvidersQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type WorkspaceAiGatewayProvidersQuery = { __typename?: 'Query', workspaceAiGatewayProviders?: Array<{ __typename?: 'AiGatewayProvider', baseUrl?: string | null, config?: string | null, createdBy?: string | null, createdDate?: any | null, enabled: boolean, id: string, lastModifiedBy?: string | null, lastModifiedDate?: any | null, name: string, type: AiGatewayProviderType, version?: number | null } | null> | null };

export type CreateWorkspaceAiGatewayProviderMutationVariables = Exact<{
  input: CreateWorkspaceAiGatewayProviderInput;
}>;


export type CreateWorkspaceAiGatewayProviderMutation = { __typename?: 'Mutation', createWorkspaceAiGatewayProvider?: { __typename?: 'AiGatewayProvider', baseUrl?: string | null, config?: string | null, createdBy?: string | null, createdDate?: any | null, enabled: boolean, id: string, lastModifiedBy?: string | null, lastModifiedDate?: any | null, name: string, type: AiGatewayProviderType, version?: number | null } | null };

export type DeleteWorkspaceAiGatewayProviderMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  providerId: Scalars['ID']['input'];
}>;


export type DeleteWorkspaceAiGatewayProviderMutation = { __typename?: 'Mutation', deleteWorkspaceAiGatewayProvider?: boolean | null };

export type UpdateWorkspaceAiGatewayProviderMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayProviderInput;
}>;


export type UpdateWorkspaceAiGatewayProviderMutation = { __typename?: 'Mutation', updateWorkspaceAiGatewayProvider?: { __typename?: 'AiGatewayProvider', baseUrl?: string | null, config?: string | null, createdBy?: string | null, createdDate?: any | null, enabled: boolean, id: string, lastModifiedBy?: string | null, lastModifiedDate?: any | null, name: string, type: AiGatewayProviderType, version?: number | null } | null };

export type TestWorkspaceAiGatewayProviderConnectionMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  providerId: Scalars['ID']['input'];
}>;


export type TestWorkspaceAiGatewayProviderConnectionMutation = { __typename?: 'Mutation', testWorkspaceAiGatewayProviderConnection?: { __typename?: 'ProviderConnectionResult', errorMessage?: string | null, latencyMs?: number | null, ok: boolean } | null };

export type WorkspaceAiGatewayRequestLogsQueryVariables = Exact<{
  endDate: Scalars['Long']['input'];
  propertyKey?: InputMaybe<Scalars['String']['input']>;
  propertyValue?: InputMaybe<Scalars['String']['input']>;
  startDate: Scalars['Long']['input'];
  workspaceId: Scalars['ID']['input'];
}>;


export type WorkspaceAiGatewayRequestLogsQuery = { __typename?: 'Query', workspaceAiGatewayRequestLogs?: Array<{ __typename?: 'AiGatewayRequestLog', apiKeyId?: string | null, cacheHit?: boolean | null, cost?: string | null, createdDate?: any | null, errorMessage?: string | null, id: string, inputTokens?: number | null, latencyMs?: number | null, outputTokens?: number | null, requestId: string, requestedModel?: string | null, routedModel?: string | null, routedProvider?: string | null, routingPolicyId?: string | null, routingStrategy?: string | null, status?: number | null } | null> | null };

export type WorkspaceAiGatewayRoutingPoliciesQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type WorkspaceAiGatewayRoutingPoliciesQuery = { __typename?: 'Query', workspaceAiGatewayRoutingPolicies?: Array<{ __typename?: 'AiGatewayRoutingPolicy', config?: string | null, createdDate?: any | null, enabled: boolean, fallbackModel?: string | null, id: string, lastModifiedDate?: any | null, name: string, strategy: AiGatewayRoutingStrategyType, version?: number | null, deployments?: Array<{ __typename?: 'AiGatewayModelDeployment', enabled: boolean, id: string, maxRpm?: number | null, maxTpm?: number | null, modelId: string, priorityOrder: number, routingPolicyId: string, weight: number } | null> | null } | null> | null };

export type CreateWorkspaceAiGatewayRoutingPolicyMutationVariables = Exact<{
  input: CreateWorkspaceAiGatewayRoutingPolicyInput;
}>;


export type CreateWorkspaceAiGatewayRoutingPolicyMutation = { __typename?: 'Mutation', createWorkspaceAiGatewayRoutingPolicy?: { __typename?: 'AiGatewayRoutingPolicy', config?: string | null, createdDate?: any | null, enabled: boolean, fallbackModel?: string | null, id: string, lastModifiedDate?: any | null, name: string, strategy: AiGatewayRoutingStrategyType, version?: number | null } | null };

export type DeleteWorkspaceAiGatewayRoutingPolicyMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  routingPolicyId: Scalars['ID']['input'];
}>;


export type DeleteWorkspaceAiGatewayRoutingPolicyMutation = { __typename?: 'Mutation', deleteWorkspaceAiGatewayRoutingPolicy?: boolean | null };

export type UpdateWorkspaceAiGatewayRoutingPolicyMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  id: Scalars['ID']['input'];
  input: UpdateAiGatewayRoutingPolicyInput;
}>;


export type UpdateWorkspaceAiGatewayRoutingPolicyMutation = { __typename?: 'Mutation', updateWorkspaceAiGatewayRoutingPolicy?: { __typename?: 'AiGatewayRoutingPolicy', config?: string | null, createdDate?: any | null, enabled: boolean, fallbackModel?: string | null, id: string, lastModifiedDate?: any | null, name: string, strategy: AiGatewayRoutingStrategyType, version?: number | null } | null };

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

export type AddProjectUserMutationVariables = Exact<{
  projectId: Scalars['ID']['input'];
  userId: Scalars['ID']['input'];
  role: ProjectRole;
}>;


export type AddProjectUserMutation = { __typename?: 'Mutation', addProjectUser: { __typename?: 'ProjectUser', id: string, projectId: string, userId: string, projectRole?: ProjectRole | null, user?: { __typename?: 'ProjectUserInfo', email: string, firstName?: string | null, lastName?: string | null } | null } };

export type AddWorkspaceUserMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  userId: Scalars['ID']['input'];
  role: WorkspaceRole;
}>;


export type AddWorkspaceUserMutation = { __typename?: 'Mutation', addWorkspaceUser: { __typename?: 'WorkspaceUser', id: string, workspaceId: string, userId: string, workspaceRole?: WorkspaceRole | null, user?: { __typename?: 'WorkspaceUserInfo', email: string, firstName?: string | null, lastName?: string | null } | null } };

export type AffectedWorkflowsQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  userLogin: Scalars['String']['input'];
}>;


export type AffectedWorkflowsQuery = { __typename?: 'Query', affectedWorkflows: Array<{ __typename?: 'AffectedWorkflow', workflowId: string, workflowName: string, connectionIds: Array<string> }> };

export type CreateMcpProjectMutationVariables = Exact<{
  input: CreateMcpProjectInput;
}>;


export type CreateMcpProjectMutation = { __typename?: 'Mutation', createMcpProject?: { __typename?: 'McpProject', id: string, mcpServerId: string, projectDeploymentId: string, projectVersion?: number | null } | null };

export type CreateOrganizationConnectionMutationVariables = Exact<{
  input: CreateOrganizationConnectionInput;
}>;


export type CreateOrganizationConnectionMutation = { __typename?: 'Mutation', createOrganizationConnection: string };

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

export type DeleteOrganizationConnectionMutationVariables = Exact<{
  connectionId: Scalars['ID']['input'];
}>;


export type DeleteOrganizationConnectionMutation = { __typename?: 'Mutation', deleteOrganizationConnection: boolean };

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

export type DemoteConnectionToPrivateMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  connectionId: Scalars['ID']['input'];
}>;


export type DemoteConnectionToPrivateMutation = { __typename?: 'Mutation', demoteConnectionToPrivate: boolean };

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

export type MyProjectScopesQueryVariables = Exact<{
  projectId: Scalars['ID']['input'];
}>;


export type MyProjectScopesQuery = { __typename?: 'Query', myProjectScopes: Array<string> };

export type MyWorkspaceRoleQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type MyWorkspaceRoleQuery = { __typename?: 'Query', myWorkspaceRole?: string | null };

export type OrganizationConnectionsQueryVariables = Exact<{
  environmentId?: InputMaybe<Scalars['ID']['input']>;
}>;


export type OrganizationConnectionsQuery = { __typename?: 'Query', organizationConnections: Array<{ __typename?: 'OrganizationConnection', id: string, name: string, componentName: string, environmentId: number, visibility: ConnectionVisibility, createdBy?: string | null, createdDate?: string | null, lastModifiedDate?: string | null }> };

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

export type ProjectUsersQueryVariables = Exact<{
  projectId: Scalars['ID']['input'];
}>;


export type ProjectUsersQuery = { __typename?: 'Query', projectUsers: Array<{ __typename?: 'ProjectUser', id: string, projectId: string, userId: string, projectRole?: ProjectRole | null, createdDate?: string | null, user?: { __typename?: 'ProjectUserInfo', email: string, firstName?: string | null, lastName?: string | null } | null }> };

export type PromoteAllPrivateConnectionsToWorkspaceMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type PromoteAllPrivateConnectionsToWorkspaceMutation = { __typename?: 'Mutation', promoteAllPrivateConnectionsToWorkspace: { __typename?: 'BulkPromoteResult', attempted: number, promoted: number, skipped: number, failed: number, failures: Array<{ __typename?: 'BulkPromoteFailure', connectionId: string, errorCode: string, message: string }> } };

export type PromoteConnectionToWorkspaceMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  connectionId: Scalars['ID']['input'];
}>;


export type PromoteConnectionToWorkspaceMutation = { __typename?: 'Mutation', promoteConnectionToWorkspace: boolean };

export type ReassignAllConnectionsMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  userLogin: Scalars['String']['input'];
  newOwnerLogin: Scalars['String']['input'];
}>;


export type ReassignAllConnectionsMutation = { __typename?: 'Mutation', reassignAllConnections: boolean };

export type RemoveProjectUserMutationVariables = Exact<{
  projectId: Scalars['ID']['input'];
  userId: Scalars['ID']['input'];
}>;


export type RemoveProjectUserMutation = { __typename?: 'Mutation', removeProjectUser: boolean };

export type RemoveWorkspaceUserMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  userId: Scalars['ID']['input'];
}>;


export type RemoveWorkspaceUserMutation = { __typename?: 'Mutation', removeWorkspaceUser: boolean };

export type RevokeConnectionFromProjectMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  connectionId: Scalars['ID']['input'];
  projectId: Scalars['ID']['input'];
}>;


export type RevokeConnectionFromProjectMutation = { __typename?: 'Mutation', revokeConnectionFromProject: boolean };

export type SetConnectionProjectsMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  connectionId: Scalars['ID']['input'];
  projectIds: Array<Scalars['ID']['input']> | Scalars['ID']['input'];
}>;


export type SetConnectionProjectsMutation = { __typename?: 'Mutation', setConnectionProjects: boolean };

export type ShareConnectionToProjectMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  connectionId: Scalars['ID']['input'];
  projectId: Scalars['ID']['input'];
}>;


export type ShareConnectionToProjectMutation = { __typename?: 'Mutation', shareConnectionToProject: boolean };

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

export type UnresolvedConnectionsQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  userLogin: Scalars['String']['input'];
}>;


export type UnresolvedConnectionsQuery = { __typename?: 'Query', unresolvedConnections: Array<{ __typename?: 'ConnectionReassignmentItem', connectionId: string, connectionName: string, visibility: ConnectionVisibility, environmentId: number, dependentWorkflowCount: number }> };

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

export type UpdateOrganizationConnectionMutationVariables = Exact<{
  connectionId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
  tagIds?: InputMaybe<Array<Scalars['ID']['input']> | Scalars['ID']['input']>;
  version: Scalars['Int']['input'];
}>;


export type UpdateOrganizationConnectionMutation = { __typename?: 'Mutation', updateOrganizationConnection: boolean };

export type UpdateProjectUserRoleMutationVariables = Exact<{
  projectId: Scalars['ID']['input'];
  userId: Scalars['ID']['input'];
  role: ProjectRole;
}>;


export type UpdateProjectUserRoleMutation = { __typename?: 'Mutation', updateProjectUserRole: { __typename?: 'ProjectUser', id: string, projectRole?: ProjectRole | null } };

export type UpdateWorkspaceApiKeyMutationVariables = Exact<{
  apiKeyId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
}>;


export type UpdateWorkspaceApiKeyMutation = { __typename?: 'Mutation', updateWorkspaceApiKey: boolean };

export type UpdateWorkspaceUserRoleMutationVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  userId: Scalars['ID']['input'];
  role: WorkspaceRole;
}>;


export type UpdateWorkspaceUserRoleMutation = { __typename?: 'Mutation', updateWorkspaceUserRole: { __typename?: 'WorkspaceUser', id: string, workspaceRole?: WorkspaceRole | null } };

export type WorkflowChatProjectDeploymentWorkflowQueryVariables = Exact<{
  id: Scalars['String']['input'];
}>;


export type WorkflowChatProjectDeploymentWorkflowQuery = { __typename?: 'Query', projectDeploymentWorkflow?: { __typename?: 'ProjectDeploymentWorkflow', projectWorkflow: { __typename?: 'ProjectWorkflow', sseStreamResponse: boolean, workflow: { __typename?: 'Workflow', label: string } } } | null };

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

export type WorkspaceChatWorkflowsQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
  environmentId: Scalars['ID']['input'];
}>;


export type WorkspaceChatWorkflowsQuery = { __typename?: 'Query', workspaceChatWorkflows: Array<{ __typename?: 'ChatWorkflow', projectDeploymentId: string, projectId: string, projectName: string, workflowExecutionId: string, workflowLabel: string }> };

export type WorkspaceMcpServersQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type WorkspaceMcpServersQuery = { __typename?: 'Query', workspaceMcpServers?: Array<{ __typename?: 'McpServer', id: string, name: string, type: PlatformType, environmentId: string, enabled: boolean, url: string, lastModifiedDate?: any | null, mcpComponents?: Array<{ __typename?: 'McpComponent', id: string, mcpServerId: string, componentName: string, componentVersion: number, title?: string | null } | null> | null, tags?: Array<{ __typename?: 'Tag', id: string, name: string } | null> | null } | null> | null };

export type WorkspaceUsersQueryVariables = Exact<{
  workspaceId: Scalars['ID']['input'];
}>;


export type WorkspaceUsersQuery = { __typename?: 'Query', workspaceUsers: Array<{ __typename?: 'WorkspaceUser', id: string, workspaceId: string, userId: string, workspaceRole?: WorkspaceRole | null, createdDate?: string | null, user?: { __typename?: 'WorkspaceUserInfo', email: string, firstName?: string | null, lastName?: string | null } | null }> };

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



export const AiAgentEvalResultDocument = new TypedDocumentString(`
    query aiAgentEvalResult($id: ID!) {
  aiAgentEvalResult(id: $id) {
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

export const useAiAgentEvalResultQuery = <
      TData = AiAgentEvalResultQuery,
      TError = unknown
    >(
      variables: AiAgentEvalResultQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentEvalResultQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentEvalResultQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentEvalResultQuery, TError, TData>(
      {
    queryKey: ['aiAgentEvalResult', variables],
    queryFn: fetcher<AiAgentEvalResultQuery, AiAgentEvalResultQueryVariables>(AiAgentEvalResultDocument, variables),
    ...options
  }
    )};

export const AiAgentEvalResultTranscriptDocument = new TypedDocumentString(`
    query aiAgentEvalResultTranscript($id: ID!) {
  aiAgentEvalResultTranscript(id: $id)
}
    `);

export const useAiAgentEvalResultTranscriptQuery = <
      TData = AiAgentEvalResultTranscriptQuery,
      TError = unknown
    >(
      variables: AiAgentEvalResultTranscriptQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentEvalResultTranscriptQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentEvalResultTranscriptQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentEvalResultTranscriptQuery, TError, TData>(
      {
    queryKey: ['aiAgentEvalResultTranscript', variables],
    queryFn: fetcher<AiAgentEvalResultTranscriptQuery, AiAgentEvalResultTranscriptQueryVariables>(AiAgentEvalResultTranscriptDocument, variables),
    ...options
  }
    )};

export const AiAgentEvalRunDocument = new TypedDocumentString(`
    query aiAgentEvalRun($id: ID!) {
  aiAgentEvalRun(id: $id) {
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

export const useAiAgentEvalRunQuery = <
      TData = AiAgentEvalRunQuery,
      TError = unknown
    >(
      variables: AiAgentEvalRunQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentEvalRunQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentEvalRunQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentEvalRunQuery, TError, TData>(
      {
    queryKey: ['aiAgentEvalRun', variables],
    queryFn: fetcher<AiAgentEvalRunQuery, AiAgentEvalRunQueryVariables>(AiAgentEvalRunDocument, variables),
    ...options
  }
    )};

export const AiAgentEvalRunsDocument = new TypedDocumentString(`
    query aiAgentEvalRuns($agentEvalTestId: ID!, $limit: Int, $offset: Int) {
  aiAgentEvalRuns(
    agentEvalTestId: $agentEvalTestId
    limit: $limit
    offset: $offset
  ) {
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

export const useAiAgentEvalRunsQuery = <
      TData = AiAgentEvalRunsQuery,
      TError = unknown
    >(
      variables: AiAgentEvalRunsQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentEvalRunsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentEvalRunsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentEvalRunsQuery, TError, TData>(
      {
    queryKey: ['aiAgentEvalRuns', variables],
    queryFn: fetcher<AiAgentEvalRunsQuery, AiAgentEvalRunsQueryVariables>(AiAgentEvalRunsDocument, variables),
    ...options
  }
    )};

export const AiAgentEvalTestDocument = new TypedDocumentString(`
    query aiAgentEvalTest($id: ID!) {
  aiAgentEvalTest(id: $id) {
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

export const useAiAgentEvalTestQuery = <
      TData = AiAgentEvalTestQuery,
      TError = unknown
    >(
      variables: AiAgentEvalTestQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentEvalTestQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentEvalTestQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentEvalTestQuery, TError, TData>(
      {
    queryKey: ['aiAgentEvalTest', variables],
    queryFn: fetcher<AiAgentEvalTestQuery, AiAgentEvalTestQueryVariables>(AiAgentEvalTestDocument, variables),
    ...options
  }
    )};

export const AiAgentEvalTestsDocument = new TypedDocumentString(`
    query aiAgentEvalTests($workflowId: String!, $workflowNodeName: String!) {
  aiAgentEvalTests(workflowId: $workflowId, workflowNodeName: $workflowNodeName) {
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

export const useAiAgentEvalTestsQuery = <
      TData = AiAgentEvalTestsQuery,
      TError = unknown
    >(
      variables: AiAgentEvalTestsQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentEvalTestsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentEvalTestsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentEvalTestsQuery, TError, TData>(
      {
    queryKey: ['aiAgentEvalTests', variables],
    queryFn: fetcher<AiAgentEvalTestsQuery, AiAgentEvalTestsQueryVariables>(AiAgentEvalTestsDocument, variables),
    ...options
  }
    )};

export const AiAgentJudgesDocument = new TypedDocumentString(`
    query aiAgentJudges($workflowId: String!, $workflowNodeName: String!) {
  aiAgentJudges(workflowId: $workflowId, workflowNodeName: $workflowNodeName) {
    id
    name
    type
    configuration
    createdDate
    lastModifiedDate
  }
}
    `);

export const useAiAgentJudgesQuery = <
      TData = AiAgentJudgesQuery,
      TError = unknown
    >(
      variables: AiAgentJudgesQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentJudgesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentJudgesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentJudgesQuery, TError, TData>(
      {
    queryKey: ['aiAgentJudges', variables],
    queryFn: fetcher<AiAgentJudgesQuery, AiAgentJudgesQueryVariables>(AiAgentJudgesDocument, variables),
    ...options
  }
    )};

export const CancelAiAgentEvalRunDocument = new TypedDocumentString(`
    mutation cancelAiAgentEvalRun($id: ID!) {
  cancelAiAgentEvalRun(id: $id) {
    id
    status
  }
}
    `);

export const useCancelAiAgentEvalRunMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CancelAiAgentEvalRunMutation, TError, CancelAiAgentEvalRunMutationVariables, TContext>) => {
    
    return useMutation<CancelAiAgentEvalRunMutation, TError, CancelAiAgentEvalRunMutationVariables, TContext>(
      {
    mutationKey: ['cancelAiAgentEvalRun'],
    mutationFn: (variables?: CancelAiAgentEvalRunMutationVariables) => fetcher<CancelAiAgentEvalRunMutation, CancelAiAgentEvalRunMutationVariables>(CancelAiAgentEvalRunDocument, variables)(),
    ...options
  }
    )};

export const CreateAiAgentEvalScenarioDocument = new TypedDocumentString(`
    mutation createAiAgentEvalScenario($agentEvalTestId: ID!, $name: String!, $type: AiAgentScenarioType!, $userMessage: String, $expectedOutput: String, $personaPrompt: String, $maxTurns: Int, $numberOfRuns: Int) {
  createAiAgentEvalScenario(
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

export const useCreateAiAgentEvalScenarioMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiAgentEvalScenarioMutation, TError, CreateAiAgentEvalScenarioMutationVariables, TContext>) => {
    
    return useMutation<CreateAiAgentEvalScenarioMutation, TError, CreateAiAgentEvalScenarioMutationVariables, TContext>(
      {
    mutationKey: ['createAiAgentEvalScenario'],
    mutationFn: (variables?: CreateAiAgentEvalScenarioMutationVariables) => fetcher<CreateAiAgentEvalScenarioMutation, CreateAiAgentEvalScenarioMutationVariables>(CreateAiAgentEvalScenarioDocument, variables)(),
    ...options
  }
    )};

export const CreateAiAgentEvalTestDocument = new TypedDocumentString(`
    mutation createAiAgentEvalTest($workflowId: String!, $workflowNodeName: String!, $name: String!, $description: String) {
  createAiAgentEvalTest(
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

export const useCreateAiAgentEvalTestMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiAgentEvalTestMutation, TError, CreateAiAgentEvalTestMutationVariables, TContext>) => {
    
    return useMutation<CreateAiAgentEvalTestMutation, TError, CreateAiAgentEvalTestMutationVariables, TContext>(
      {
    mutationKey: ['createAiAgentEvalTest'],
    mutationFn: (variables?: CreateAiAgentEvalTestMutationVariables) => fetcher<CreateAiAgentEvalTestMutation, CreateAiAgentEvalTestMutationVariables>(CreateAiAgentEvalTestDocument, variables)(),
    ...options
  }
    )};

export const CreateAiAgentJudgeDocument = new TypedDocumentString(`
    mutation createAiAgentJudge($workflowId: String!, $workflowNodeName: String!, $name: String!, $type: AiAgentJudgeType!, $configuration: Map!) {
  createAiAgentJudge(
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

export const useCreateAiAgentJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiAgentJudgeMutation, TError, CreateAiAgentJudgeMutationVariables, TContext>) => {
    
    return useMutation<CreateAiAgentJudgeMutation, TError, CreateAiAgentJudgeMutationVariables, TContext>(
      {
    mutationKey: ['createAiAgentJudge'],
    mutationFn: (variables?: CreateAiAgentJudgeMutationVariables) => fetcher<CreateAiAgentJudgeMutation, CreateAiAgentJudgeMutationVariables>(CreateAiAgentJudgeDocument, variables)(),
    ...options
  }
    )};

export const CreateAiAgentScenarioJudgeDocument = new TypedDocumentString(`
    mutation createAiAgentScenarioJudge($agentEvalScenarioId: ID!, $name: String!, $type: AiAgentJudgeType!, $configuration: Map!) {
  createAiAgentScenarioJudge(
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

export const useCreateAiAgentScenarioJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiAgentScenarioJudgeMutation, TError, CreateAiAgentScenarioJudgeMutationVariables, TContext>) => {
    
    return useMutation<CreateAiAgentScenarioJudgeMutation, TError, CreateAiAgentScenarioJudgeMutationVariables, TContext>(
      {
    mutationKey: ['createAiAgentScenarioJudge'],
    mutationFn: (variables?: CreateAiAgentScenarioJudgeMutationVariables) => fetcher<CreateAiAgentScenarioJudgeMutation, CreateAiAgentScenarioJudgeMutationVariables>(CreateAiAgentScenarioJudgeDocument, variables)(),
    ...options
  }
    )};

export const CreateAiAgentScenarioToolSimulationDocument = new TypedDocumentString(`
    mutation createAiAgentScenarioToolSimulation($agentEvalScenarioId: ID!, $toolName: String!, $responsePrompt: String!, $simulationModel: String) {
  createAiAgentScenarioToolSimulation(
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

export const useCreateAiAgentScenarioToolSimulationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiAgentScenarioToolSimulationMutation, TError, CreateAiAgentScenarioToolSimulationMutationVariables, TContext>) => {
    
    return useMutation<CreateAiAgentScenarioToolSimulationMutation, TError, CreateAiAgentScenarioToolSimulationMutationVariables, TContext>(
      {
    mutationKey: ['createAiAgentScenarioToolSimulation'],
    mutationFn: (variables?: CreateAiAgentScenarioToolSimulationMutationVariables) => fetcher<CreateAiAgentScenarioToolSimulationMutation, CreateAiAgentScenarioToolSimulationMutationVariables>(CreateAiAgentScenarioToolSimulationDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiAgentEvalScenarioDocument = new TypedDocumentString(`
    mutation deleteAiAgentEvalScenario($id: ID!) {
  deleteAiAgentEvalScenario(id: $id)
}
    `);

export const useDeleteAiAgentEvalScenarioMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiAgentEvalScenarioMutation, TError, DeleteAiAgentEvalScenarioMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiAgentEvalScenarioMutation, TError, DeleteAiAgentEvalScenarioMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiAgentEvalScenario'],
    mutationFn: (variables?: DeleteAiAgentEvalScenarioMutationVariables) => fetcher<DeleteAiAgentEvalScenarioMutation, DeleteAiAgentEvalScenarioMutationVariables>(DeleteAiAgentEvalScenarioDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiAgentEvalTestDocument = new TypedDocumentString(`
    mutation deleteAiAgentEvalTest($id: ID!) {
  deleteAiAgentEvalTest(id: $id)
}
    `);

export const useDeleteAiAgentEvalTestMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiAgentEvalTestMutation, TError, DeleteAiAgentEvalTestMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiAgentEvalTestMutation, TError, DeleteAiAgentEvalTestMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiAgentEvalTest'],
    mutationFn: (variables?: DeleteAiAgentEvalTestMutationVariables) => fetcher<DeleteAiAgentEvalTestMutation, DeleteAiAgentEvalTestMutationVariables>(DeleteAiAgentEvalTestDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiAgentJudgeDocument = new TypedDocumentString(`
    mutation deleteAiAgentJudge($id: ID!) {
  deleteAiAgentJudge(id: $id)
}
    `);

export const useDeleteAiAgentJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiAgentJudgeMutation, TError, DeleteAiAgentJudgeMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiAgentJudgeMutation, TError, DeleteAiAgentJudgeMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiAgentJudge'],
    mutationFn: (variables?: DeleteAiAgentJudgeMutationVariables) => fetcher<DeleteAiAgentJudgeMutation, DeleteAiAgentJudgeMutationVariables>(DeleteAiAgentJudgeDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiAgentScenarioJudgeDocument = new TypedDocumentString(`
    mutation deleteAiAgentScenarioJudge($id: ID!) {
  deleteAiAgentScenarioJudge(id: $id)
}
    `);

export const useDeleteAiAgentScenarioJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiAgentScenarioJudgeMutation, TError, DeleteAiAgentScenarioJudgeMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiAgentScenarioJudgeMutation, TError, DeleteAiAgentScenarioJudgeMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiAgentScenarioJudge'],
    mutationFn: (variables?: DeleteAiAgentScenarioJudgeMutationVariables) => fetcher<DeleteAiAgentScenarioJudgeMutation, DeleteAiAgentScenarioJudgeMutationVariables>(DeleteAiAgentScenarioJudgeDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiAgentScenarioToolSimulationDocument = new TypedDocumentString(`
    mutation deleteAiAgentScenarioToolSimulation($id: ID!) {
  deleteAiAgentScenarioToolSimulation(id: $id)
}
    `);

export const useDeleteAiAgentScenarioToolSimulationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiAgentScenarioToolSimulationMutation, TError, DeleteAiAgentScenarioToolSimulationMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiAgentScenarioToolSimulationMutation, TError, DeleteAiAgentScenarioToolSimulationMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiAgentScenarioToolSimulation'],
    mutationFn: (variables?: DeleteAiAgentScenarioToolSimulationMutationVariables) => fetcher<DeleteAiAgentScenarioToolSimulationMutation, DeleteAiAgentScenarioToolSimulationMutationVariables>(DeleteAiAgentScenarioToolSimulationDocument, variables)(),
    ...options
  }
    )};

export const StartAiAgentEvalRunDocument = new TypedDocumentString(`
    mutation startAiAgentEvalRun($agentEvalTestId: ID!, $name: String!, $environmentId: ID!, $scenarioIds: [ID!], $aiAgentJudgeIds: [ID!]) {
  startAiAgentEvalRun(
    agentEvalTestId: $agentEvalTestId
    name: $name
    environmentId: $environmentId
    scenarioIds: $scenarioIds
    aiAgentJudgeIds: $aiAgentJudgeIds
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

export const useStartAiAgentEvalRunMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<StartAiAgentEvalRunMutation, TError, StartAiAgentEvalRunMutationVariables, TContext>) => {
    
    return useMutation<StartAiAgentEvalRunMutation, TError, StartAiAgentEvalRunMutationVariables, TContext>(
      {
    mutationKey: ['startAiAgentEvalRun'],
    mutationFn: (variables?: StartAiAgentEvalRunMutationVariables) => fetcher<StartAiAgentEvalRunMutation, StartAiAgentEvalRunMutationVariables>(StartAiAgentEvalRunDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentEvalScenarioDocument = new TypedDocumentString(`
    mutation updateAiAgentEvalScenario($id: ID!, $name: String, $userMessage: String, $expectedOutput: String, $personaPrompt: String, $maxTurns: Int, $numberOfRuns: Int) {
  updateAiAgentEvalScenario(
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

export const useUpdateAiAgentEvalScenarioMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentEvalScenarioMutation, TError, UpdateAiAgentEvalScenarioMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentEvalScenarioMutation, TError, UpdateAiAgentEvalScenarioMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgentEvalScenario'],
    mutationFn: (variables?: UpdateAiAgentEvalScenarioMutationVariables) => fetcher<UpdateAiAgentEvalScenarioMutation, UpdateAiAgentEvalScenarioMutationVariables>(UpdateAiAgentEvalScenarioDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentEvalTestDocument = new TypedDocumentString(`
    mutation updateAiAgentEvalTest($id: ID!, $name: String, $description: String) {
  updateAiAgentEvalTest(id: $id, name: $name, description: $description) {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useUpdateAiAgentEvalTestMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentEvalTestMutation, TError, UpdateAiAgentEvalTestMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentEvalTestMutation, TError, UpdateAiAgentEvalTestMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgentEvalTest'],
    mutationFn: (variables?: UpdateAiAgentEvalTestMutationVariables) => fetcher<UpdateAiAgentEvalTestMutation, UpdateAiAgentEvalTestMutationVariables>(UpdateAiAgentEvalTestDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentJudgeDocument = new TypedDocumentString(`
    mutation updateAiAgentJudge($id: ID!, $name: String, $configuration: Map) {
  updateAiAgentJudge(id: $id, name: $name, configuration: $configuration) {
    id
    name
    type
    configuration
    createdDate
    lastModifiedDate
  }
}
    `);

export const useUpdateAiAgentJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentJudgeMutation, TError, UpdateAiAgentJudgeMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentJudgeMutation, TError, UpdateAiAgentJudgeMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgentJudge'],
    mutationFn: (variables?: UpdateAiAgentJudgeMutationVariables) => fetcher<UpdateAiAgentJudgeMutation, UpdateAiAgentJudgeMutationVariables>(UpdateAiAgentJudgeDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentScenarioJudgeDocument = new TypedDocumentString(`
    mutation updateAiAgentScenarioJudge($id: ID!, $name: String, $configuration: Map) {
  updateAiAgentScenarioJudge(id: $id, name: $name, configuration: $configuration) {
    id
    name
    type
    configuration
    createdDate
    lastModifiedDate
  }
}
    `);

export const useUpdateAiAgentScenarioJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentScenarioJudgeMutation, TError, UpdateAiAgentScenarioJudgeMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentScenarioJudgeMutation, TError, UpdateAiAgentScenarioJudgeMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgentScenarioJudge'],
    mutationFn: (variables?: UpdateAiAgentScenarioJudgeMutationVariables) => fetcher<UpdateAiAgentScenarioJudgeMutation, UpdateAiAgentScenarioJudgeMutationVariables>(UpdateAiAgentScenarioJudgeDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentScenarioToolSimulationDocument = new TypedDocumentString(`
    mutation updateAiAgentScenarioToolSimulation($id: ID!, $toolName: String, $responsePrompt: String, $simulationModel: String) {
  updateAiAgentScenarioToolSimulation(
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

export const useUpdateAiAgentScenarioToolSimulationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentScenarioToolSimulationMutation, TError, UpdateAiAgentScenarioToolSimulationMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentScenarioToolSimulationMutation, TError, UpdateAiAgentScenarioToolSimulationMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgentScenarioToolSimulation'],
    mutationFn: (variables?: UpdateAiAgentScenarioToolSimulationMutationVariables) => fetcher<UpdateAiAgentScenarioToolSimulationMutation, UpdateAiAgentScenarioToolSimulationMutationVariables>(UpdateAiAgentScenarioToolSimulationDocument, variables)(),
    ...options
  }
    )};

export const AiAgentSkillDocument = new TypedDocumentString(`
    query aiAgentSkill($id: ID!) {
  aiAgentSkill(id: $id) {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useAiAgentSkillQuery = <
      TData = AiAgentSkillQuery,
      TError = unknown
    >(
      variables: AiAgentSkillQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentSkillQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentSkillQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentSkillQuery, TError, TData>(
      {
    queryKey: ['aiAgentSkill', variables],
    queryFn: fetcher<AiAgentSkillQuery, AiAgentSkillQueryVariables>(AiAgentSkillDocument, variables),
    ...options
  }
    )};

export const AiAgentSkillFileContentDocument = new TypedDocumentString(`
    query aiAgentSkillFileContent($id: ID!, $path: String!) {
  aiAgentSkillFileContent(id: $id, path: $path)
}
    `);

export const useAiAgentSkillFileContentQuery = <
      TData = AiAgentSkillFileContentQuery,
      TError = unknown
    >(
      variables: AiAgentSkillFileContentQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentSkillFileContentQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentSkillFileContentQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentSkillFileContentQuery, TError, TData>(
      {
    queryKey: ['aiAgentSkillFileContent', variables],
    queryFn: fetcher<AiAgentSkillFileContentQuery, AiAgentSkillFileContentQueryVariables>(AiAgentSkillFileContentDocument, variables),
    ...options
  }
    )};

export const AiAgentSkillFilePathsDocument = new TypedDocumentString(`
    query aiAgentSkillFilePaths($id: ID!) {
  aiAgentSkillFilePaths(id: $id)
}
    `);

export const useAiAgentSkillFilePathsQuery = <
      TData = AiAgentSkillFilePathsQuery,
      TError = unknown
    >(
      variables: AiAgentSkillFilePathsQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentSkillFilePathsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentSkillFilePathsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentSkillFilePathsQuery, TError, TData>(
      {
    queryKey: ['aiAgentSkillFilePaths', variables],
    queryFn: fetcher<AiAgentSkillFilePathsQuery, AiAgentSkillFilePathsQueryVariables>(AiAgentSkillFilePathsDocument, variables),
    ...options
  }
    )};

export const AiAgentSkillsDocument = new TypedDocumentString(`
    query aiAgentSkills {
  aiAgentSkills {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useAiAgentSkillsQuery = <
      TData = AiAgentSkillsQuery,
      TError = unknown
    >(
      variables?: AiAgentSkillsQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentSkillsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentSkillsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentSkillsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['aiAgentSkills'] : ['aiAgentSkills', variables],
    queryFn: fetcher<AiAgentSkillsQuery, AiAgentSkillsQueryVariables>(AiAgentSkillsDocument, variables),
    ...options
  }
    )};

export const CreateAiAgentSkillDocument = new TypedDocumentString(`
    mutation createAiAgentSkill($name: String!, $description: String, $filename: String!, $fileBytes: String!) {
  createAiAgentSkill(
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

export const useCreateAiAgentSkillMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiAgentSkillMutation, TError, CreateAiAgentSkillMutationVariables, TContext>) => {
    
    return useMutation<CreateAiAgentSkillMutation, TError, CreateAiAgentSkillMutationVariables, TContext>(
      {
    mutationKey: ['createAiAgentSkill'],
    mutationFn: (variables?: CreateAiAgentSkillMutationVariables) => fetcher<CreateAiAgentSkillMutation, CreateAiAgentSkillMutationVariables>(CreateAiAgentSkillDocument, variables)(),
    ...options
  }
    )};

export const CreateAiAgentSkillFromInstructionsDocument = new TypedDocumentString(`
    mutation createAiAgentSkillFromInstructions($name: String!, $description: String, $instructions: String!) {
  createAiAgentSkillFromInstructions(
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

export const useCreateAiAgentSkillFromInstructionsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiAgentSkillFromInstructionsMutation, TError, CreateAiAgentSkillFromInstructionsMutationVariables, TContext>) => {
    
    return useMutation<CreateAiAgentSkillFromInstructionsMutation, TError, CreateAiAgentSkillFromInstructionsMutationVariables, TContext>(
      {
    mutationKey: ['createAiAgentSkillFromInstructions'],
    mutationFn: (variables?: CreateAiAgentSkillFromInstructionsMutationVariables) => fetcher<CreateAiAgentSkillFromInstructionsMutation, CreateAiAgentSkillFromInstructionsMutationVariables>(CreateAiAgentSkillFromInstructionsDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiAgentSkillDocument = new TypedDocumentString(`
    mutation deleteAiAgentSkill($id: ID!) {
  deleteAiAgentSkill(id: $id)
}
    `);

export const useDeleteAiAgentSkillMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiAgentSkillMutation, TError, DeleteAiAgentSkillMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiAgentSkillMutation, TError, DeleteAiAgentSkillMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiAgentSkill'],
    mutationFn: (variables?: DeleteAiAgentSkillMutationVariables) => fetcher<DeleteAiAgentSkillMutation, DeleteAiAgentSkillMutationVariables>(DeleteAiAgentSkillDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentSkillDocument = new TypedDocumentString(`
    mutation updateAiAgentSkill($id: ID!, $name: String!, $description: String) {
  updateAiAgentSkill(id: $id, name: $name, description: $description) {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useUpdateAiAgentSkillMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentSkillMutation, TError, UpdateAiAgentSkillMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentSkillMutation, TError, UpdateAiAgentSkillMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgentSkill'],
    mutationFn: (variables?: UpdateAiAgentSkillMutationVariables) => fetcher<UpdateAiAgentSkillMutation, UpdateAiAgentSkillMutationVariables>(UpdateAiAgentSkillDocument, variables)(),
    ...options
  }
    )};

export const AuditEventsDocument = new TypedDocumentString(`
    query AuditEvents($principal: String, $eventType: String, $fromDate: Long, $toDate: Long, $dataSearch: String, $page: Int, $size: Int) {
  auditEvents(
    principal: $principal
    eventType: $eventType
    fromDate: $fromDate
    toDate: $toDate
    dataSearch: $dataSearch
    page: $page
    size: $size
  ) {
    content {
      data {
        key
        value
      }
      eventDate
      eventType
      id
      principal
    }
    number
    size
    totalElements
    totalPages
  }
}
    `);

export const useAuditEventsQuery = <
      TData = AuditEventsQuery,
      TError = unknown
    >(
      variables?: AuditEventsQueryVariables,
      options?: Omit<UseQueryOptions<AuditEventsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AuditEventsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AuditEventsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['AuditEvents'] : ['AuditEvents', variables],
    queryFn: fetcher<AuditEventsQuery, AuditEventsQueryVariables>(AuditEventsDocument, variables),
    ...options
  }
    )};

export const AuditEventTypesDocument = new TypedDocumentString(`
    query AuditEventTypes {
  auditEventTypes
}
    `);

export const useAuditEventTypesQuery = <
      TData = AuditEventTypesQuery,
      TError = unknown
    >(
      variables?: AuditEventTypesQueryVariables,
      options?: Omit<UseQueryOptions<AuditEventTypesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AuditEventTypesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AuditEventTypesQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['AuditEventTypes'] : ['AuditEventTypes', variables],
    queryFn: fetcher<AuditEventTypesQuery, AuditEventTypesQueryVariables>(AuditEventTypesDocument, variables),
    ...options
  }
    )};

export const AiEvalRulesDocument = new TypedDocumentString(`
    query aiEvalRules($workspaceId: ID!) {
  aiEvalRules(workspaceId: $workspaceId) {
    createdDate
    delaySeconds
    enabled
    filters
    id
    lastModifiedDate
    model
    name
    projectId
    promptTemplate
    samplingRate
    scoreConfigId
    version
    workspaceId
  }
}
    `);

export const useAiEvalRulesQuery = <
      TData = AiEvalRulesQuery,
      TError = unknown
    >(
      variables: AiEvalRulesQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalRulesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalRulesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalRulesQuery, TError, TData>(
      {
    queryKey: ['aiEvalRules', variables],
    queryFn: fetcher<AiEvalRulesQuery, AiEvalRulesQueryVariables>(AiEvalRulesDocument, variables),
    ...options
  }
    )};

export const AiEvalRuleDocument = new TypedDocumentString(`
    query aiEvalRule($id: ID!) {
  aiEvalRule(id: $id) {
    createdDate
    delaySeconds
    enabled
    filters
    id
    lastModifiedDate
    model
    name
    projectId
    promptTemplate
    samplingRate
    scoreConfigId
    version
    workspaceId
  }
}
    `);

export const useAiEvalRuleQuery = <
      TData = AiEvalRuleQuery,
      TError = unknown
    >(
      variables: AiEvalRuleQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalRuleQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalRuleQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalRuleQuery, TError, TData>(
      {
    queryKey: ['aiEvalRule', variables],
    queryFn: fetcher<AiEvalRuleQuery, AiEvalRuleQueryVariables>(AiEvalRuleDocument, variables),
    ...options
  }
    )};

export const AiEvalExecutionsDocument = new TypedDocumentString(`
    query aiEvalExecutions($evalRuleId: ID!) {
  aiEvalExecutions(evalRuleId: $evalRuleId) {
    createdDate
    errorMessage
    evalRuleId
    id
    scoreId
    status
    traceId
  }
}
    `);

export const useAiEvalExecutionsQuery = <
      TData = AiEvalExecutionsQuery,
      TError = unknown
    >(
      variables: AiEvalExecutionsQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalExecutionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalExecutionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalExecutionsQuery, TError, TData>(
      {
    queryKey: ['aiEvalExecutions', variables],
    queryFn: fetcher<AiEvalExecutionsQuery, AiEvalExecutionsQueryVariables>(AiEvalExecutionsDocument, variables),
    ...options
  }
    )};

export const CreateAiEvalRuleDocument = new TypedDocumentString(`
    mutation createAiEvalRule($delaySeconds: Int, $enabled: Boolean!, $filters: String, $model: String!, $name: String!, $projectId: ID, $promptTemplate: String!, $samplingRate: Float!, $scoreConfigId: ID!, $workspaceId: ID!) {
  createAiEvalRule(
    delaySeconds: $delaySeconds
    enabled: $enabled
    filters: $filters
    model: $model
    name: $name
    projectId: $projectId
    promptTemplate: $promptTemplate
    samplingRate: $samplingRate
    scoreConfigId: $scoreConfigId
    workspaceId: $workspaceId
  ) {
    id
    name
  }
}
    `);

export const useCreateAiEvalRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiEvalRuleMutation, TError, CreateAiEvalRuleMutationVariables, TContext>) => {
    
    return useMutation<CreateAiEvalRuleMutation, TError, CreateAiEvalRuleMutationVariables, TContext>(
      {
    mutationKey: ['createAiEvalRule'],
    mutationFn: (variables?: CreateAiEvalRuleMutationVariables) => fetcher<CreateAiEvalRuleMutation, CreateAiEvalRuleMutationVariables>(CreateAiEvalRuleDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiEvalRuleDocument = new TypedDocumentString(`
    mutation deleteAiEvalRule($id: ID!) {
  deleteAiEvalRule(id: $id)
}
    `);

export const useDeleteAiEvalRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiEvalRuleMutation, TError, DeleteAiEvalRuleMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiEvalRuleMutation, TError, DeleteAiEvalRuleMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiEvalRule'],
    mutationFn: (variables?: DeleteAiEvalRuleMutationVariables) => fetcher<DeleteAiEvalRuleMutation, DeleteAiEvalRuleMutationVariables>(DeleteAiEvalRuleDocument, variables)(),
    ...options
  }
    )};

export const RunAiEvalRuleOnHistoricalTracesDocument = new TypedDocumentString(`
    mutation runAiEvalRuleOnHistoricalTraces($ruleId: ID!, $startDate: Long!, $endDate: Long!) {
  runAiEvalRuleOnHistoricalTraces(
    ruleId: $ruleId
    startDate: $startDate
    endDate: $endDate
  )
}
    `);

export const useRunAiEvalRuleOnHistoricalTracesMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RunAiEvalRuleOnHistoricalTracesMutation, TError, RunAiEvalRuleOnHistoricalTracesMutationVariables, TContext>) => {
    
    return useMutation<RunAiEvalRuleOnHistoricalTracesMutation, TError, RunAiEvalRuleOnHistoricalTracesMutationVariables, TContext>(
      {
    mutationKey: ['runAiEvalRuleOnHistoricalTraces'],
    mutationFn: (variables?: RunAiEvalRuleOnHistoricalTracesMutationVariables) => fetcher<RunAiEvalRuleOnHistoricalTracesMutation, RunAiEvalRuleOnHistoricalTracesMutationVariables>(RunAiEvalRuleOnHistoricalTracesDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiEvalRuleDocument = new TypedDocumentString(`
    mutation updateAiEvalRule($delaySeconds: Int, $enabled: Boolean!, $filters: String, $id: ID!, $model: String!, $name: String!, $promptTemplate: String!, $samplingRate: Float!, $scoreConfigId: ID!) {
  updateAiEvalRule(
    delaySeconds: $delaySeconds
    enabled: $enabled
    filters: $filters
    id: $id
    model: $model
    name: $name
    promptTemplate: $promptTemplate
    samplingRate: $samplingRate
    scoreConfigId: $scoreConfigId
  ) {
    id
    name
  }
}
    `);

export const useUpdateAiEvalRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiEvalRuleMutation, TError, UpdateAiEvalRuleMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiEvalRuleMutation, TError, UpdateAiEvalRuleMutationVariables, TContext>(
      {
    mutationKey: ['updateAiEvalRule'],
    mutationFn: (variables?: UpdateAiEvalRuleMutationVariables) => fetcher<UpdateAiEvalRuleMutation, UpdateAiEvalRuleMutationVariables>(UpdateAiEvalRuleDocument, variables)(),
    ...options
  }
    )};

export const AiEvalScoreConfigsDocument = new TypedDocumentString(`
    query aiEvalScoreConfigs($workspaceId: ID!) {
  aiEvalScoreConfigs(workspaceId: $workspaceId) {
    categories
    createdDate
    dataType
    description
    id
    lastModifiedDate
    maxValue
    minValue
    name
    version
    workspaceId
  }
}
    `);

export const useAiEvalScoreConfigsQuery = <
      TData = AiEvalScoreConfigsQuery,
      TError = unknown
    >(
      variables: AiEvalScoreConfigsQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalScoreConfigsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalScoreConfigsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalScoreConfigsQuery, TError, TData>(
      {
    queryKey: ['aiEvalScoreConfigs', variables],
    queryFn: fetcher<AiEvalScoreConfigsQuery, AiEvalScoreConfigsQueryVariables>(AiEvalScoreConfigsDocument, variables),
    ...options
  }
    )};

export const AiEvalScoreConfigDocument = new TypedDocumentString(`
    query aiEvalScoreConfig($id: ID!) {
  aiEvalScoreConfig(id: $id) {
    categories
    createdDate
    dataType
    description
    id
    lastModifiedDate
    maxValue
    minValue
    name
    version
    workspaceId
  }
}
    `);

export const useAiEvalScoreConfigQuery = <
      TData = AiEvalScoreConfigQuery,
      TError = unknown
    >(
      variables: AiEvalScoreConfigQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalScoreConfigQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalScoreConfigQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalScoreConfigQuery, TError, TData>(
      {
    queryKey: ['aiEvalScoreConfig', variables],
    queryFn: fetcher<AiEvalScoreConfigQuery, AiEvalScoreConfigQueryVariables>(AiEvalScoreConfigDocument, variables),
    ...options
  }
    )};

export const CreateAiEvalScoreConfigDocument = new TypedDocumentString(`
    mutation createAiEvalScoreConfig($categories: String, $dataType: AiEvalScoreDataType, $description: String, $maxValue: Float, $minValue: Float, $name: String!, $workspaceId: ID!) {
  createAiEvalScoreConfig(
    categories: $categories
    dataType: $dataType
    description: $description
    maxValue: $maxValue
    minValue: $minValue
    name: $name
    workspaceId: $workspaceId
  ) {
    id
    name
  }
}
    `);

export const useCreateAiEvalScoreConfigMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiEvalScoreConfigMutation, TError, CreateAiEvalScoreConfigMutationVariables, TContext>) => {
    
    return useMutation<CreateAiEvalScoreConfigMutation, TError, CreateAiEvalScoreConfigMutationVariables, TContext>(
      {
    mutationKey: ['createAiEvalScoreConfig'],
    mutationFn: (variables?: CreateAiEvalScoreConfigMutationVariables) => fetcher<CreateAiEvalScoreConfigMutation, CreateAiEvalScoreConfigMutationVariables>(CreateAiEvalScoreConfigDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiEvalScoreConfigDocument = new TypedDocumentString(`
    mutation deleteAiEvalScoreConfig($id: ID!) {
  deleteAiEvalScoreConfig(id: $id)
}
    `);

export const useDeleteAiEvalScoreConfigMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiEvalScoreConfigMutation, TError, DeleteAiEvalScoreConfigMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiEvalScoreConfigMutation, TError, DeleteAiEvalScoreConfigMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiEvalScoreConfig'],
    mutationFn: (variables?: DeleteAiEvalScoreConfigMutationVariables) => fetcher<DeleteAiEvalScoreConfigMutation, DeleteAiEvalScoreConfigMutationVariables>(DeleteAiEvalScoreConfigDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiEvalScoreConfigDocument = new TypedDocumentString(`
    mutation updateAiEvalScoreConfig($categories: String, $dataType: AiEvalScoreDataType, $description: String, $id: ID!, $maxValue: Float, $minValue: Float, $name: String!) {
  updateAiEvalScoreConfig(
    categories: $categories
    dataType: $dataType
    description: $description
    id: $id
    maxValue: $maxValue
    minValue: $minValue
    name: $name
  ) {
    id
    name
  }
}
    `);

export const useUpdateAiEvalScoreConfigMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiEvalScoreConfigMutation, TError, UpdateAiEvalScoreConfigMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiEvalScoreConfigMutation, TError, UpdateAiEvalScoreConfigMutationVariables, TContext>(
      {
    mutationKey: ['updateAiEvalScoreConfig'],
    mutationFn: (variables?: UpdateAiEvalScoreConfigMutationVariables) => fetcher<UpdateAiEvalScoreConfigMutation, UpdateAiEvalScoreConfigMutationVariables>(UpdateAiEvalScoreConfigDocument, variables)(),
    ...options
  }
    )};

export const AiEvalScoresDocument = new TypedDocumentString(`
    query aiEvalScores($workspaceId: ID!) {
  aiEvalScores(workspaceId: $workspaceId) {
    comment
    createdBy
    createdDate
    dataType
    evalRuleId
    id
    name
    source
    spanId
    stringValue
    traceId
    value
    workspaceId
  }
}
    `);

export const useAiEvalScoresQuery = <
      TData = AiEvalScoresQuery,
      TError = unknown
    >(
      variables: AiEvalScoresQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalScoresQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalScoresQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalScoresQuery, TError, TData>(
      {
    queryKey: ['aiEvalScores', variables],
    queryFn: fetcher<AiEvalScoresQuery, AiEvalScoresQueryVariables>(AiEvalScoresDocument, variables),
    ...options
  }
    )};

export const AiEvalScoresByTraceDocument = new TypedDocumentString(`
    query aiEvalScoresByTrace($traceId: ID!) {
  aiEvalScoresByTrace(traceId: $traceId) {
    comment
    createdBy
    createdDate
    dataType
    evalRuleId
    id
    name
    source
    spanId
    stringValue
    traceId
    value
    workspaceId
  }
}
    `);

export const useAiEvalScoresByTraceQuery = <
      TData = AiEvalScoresByTraceQuery,
      TError = unknown
    >(
      variables: AiEvalScoresByTraceQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalScoresByTraceQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalScoresByTraceQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalScoresByTraceQuery, TError, TData>(
      {
    queryKey: ['aiEvalScoresByTrace', variables],
    queryFn: fetcher<AiEvalScoresByTraceQuery, AiEvalScoresByTraceQueryVariables>(AiEvalScoresByTraceDocument, variables),
    ...options
  }
    )};

export const CreateAiEvalScoreDocument = new TypedDocumentString(`
    mutation createAiEvalScore($comment: String, $dataType: AiEvalScoreDataType!, $name: String!, $source: AiEvalScoreSource!, $spanId: ID, $stringValue: String, $traceId: ID!, $value: Float, $workspaceId: ID!) {
  createAiEvalScore(
    comment: $comment
    dataType: $dataType
    name: $name
    source: $source
    spanId: $spanId
    stringValue: $stringValue
    traceId: $traceId
    value: $value
    workspaceId: $workspaceId
  ) {
    id
    name
  }
}
    `);

export const useCreateAiEvalScoreMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiEvalScoreMutation, TError, CreateAiEvalScoreMutationVariables, TContext>) => {
    
    return useMutation<CreateAiEvalScoreMutation, TError, CreateAiEvalScoreMutationVariables, TContext>(
      {
    mutationKey: ['createAiEvalScore'],
    mutationFn: (variables?: CreateAiEvalScoreMutationVariables) => fetcher<CreateAiEvalScoreMutation, CreateAiEvalScoreMutationVariables>(CreateAiEvalScoreDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiEvalScoreDocument = new TypedDocumentString(`
    mutation deleteAiEvalScore($id: ID!) {
  deleteAiEvalScore(id: $id)
}
    `);

export const useDeleteAiEvalScoreMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiEvalScoreMutation, TError, DeleteAiEvalScoreMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiEvalScoreMutation, TError, DeleteAiEvalScoreMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiEvalScore'],
    mutationFn: (variables?: DeleteAiEvalScoreMutationVariables) => fetcher<DeleteAiEvalScoreMutation, DeleteAiEvalScoreMutationVariables>(DeleteAiEvalScoreDocument, variables)(),
    ...options
  }
    )};

export const AiEvalScoreAnalyticsDocument = new TypedDocumentString(`
    query aiEvalScoreAnalytics($workspaceId: ID!, $startDate: Long!, $endDate: Long!) {
  aiEvalScoreAnalytics(
    workspaceId: $workspaceId
    startDate: $startDate
    endDate: $endDate
  ) {
    average
    count
    dataType
    distribution {
      count
      value
    }
    max
    min
    name
  }
}
    `);

export const useAiEvalScoreAnalyticsQuery = <
      TData = AiEvalScoreAnalyticsQuery,
      TError = unknown
    >(
      variables: AiEvalScoreAnalyticsQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalScoreAnalyticsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalScoreAnalyticsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalScoreAnalyticsQuery, TError, TData>(
      {
    queryKey: ['aiEvalScoreAnalytics', variables],
    queryFn: fetcher<AiEvalScoreAnalyticsQuery, AiEvalScoreAnalyticsQueryVariables>(AiEvalScoreAnalyticsDocument, variables),
    ...options
  }
    )};

export const AiEvalScoreTrendDocument = new TypedDocumentString(`
    query aiEvalScoreTrend($workspaceId: ID!, $name: String!, $startDate: Long!, $endDate: Long!) {
  aiEvalScoreTrend(
    workspaceId: $workspaceId
    name: $name
    startDate: $startDate
    endDate: $endDate
  ) {
    average
    count
    day
  }
}
    `);

export const useAiEvalScoreTrendQuery = <
      TData = AiEvalScoreTrendQuery,
      TError = unknown
    >(
      variables: AiEvalScoreTrendQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalScoreTrendQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalScoreTrendQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalScoreTrendQuery, TError, TData>(
      {
    queryKey: ['aiEvalScoreTrend', variables],
    queryFn: fetcher<AiEvalScoreTrendQuery, AiEvalScoreTrendQueryVariables>(AiEvalScoreTrendDocument, variables),
    ...options
  }
    )};

export const AiGatewayBudgetDocument = new TypedDocumentString(`
    query aiGatewayBudget($workspaceId: ID!) {
  aiGatewayBudget(workspaceId: $workspaceId) {
    alertThreshold
    amount
    createdDate
    enabled
    enforcementMode
    id
    lastModifiedDate
    period
    version
    workspaceId
  }
}
    `);

export const useAiGatewayBudgetQuery = <
      TData = AiGatewayBudgetQuery,
      TError = unknown
    >(
      variables: AiGatewayBudgetQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayBudgetQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayBudgetQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayBudgetQuery, TError, TData>(
      {
    queryKey: ['aiGatewayBudget', variables],
    queryFn: fetcher<AiGatewayBudgetQuery, AiGatewayBudgetQueryVariables>(AiGatewayBudgetDocument, variables),
    ...options
  }
    )};

export const CreateAiGatewayBudgetDocument = new TypedDocumentString(`
    mutation createAiGatewayBudget($input: CreateAiGatewayBudgetInput!) {
  createAiGatewayBudget(input: $input) {
    alertThreshold
    amount
    createdDate
    enabled
    enforcementMode
    id
    lastModifiedDate
    period
    version
    workspaceId
  }
}
    `);

export const useCreateAiGatewayBudgetMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiGatewayBudgetMutation, TError, CreateAiGatewayBudgetMutationVariables, TContext>) => {
    
    return useMutation<CreateAiGatewayBudgetMutation, TError, CreateAiGatewayBudgetMutationVariables, TContext>(
      {
    mutationKey: ['createAiGatewayBudget'],
    mutationFn: (variables?: CreateAiGatewayBudgetMutationVariables) => fetcher<CreateAiGatewayBudgetMutation, CreateAiGatewayBudgetMutationVariables>(CreateAiGatewayBudgetDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiGatewayBudgetDocument = new TypedDocumentString(`
    mutation updateAiGatewayBudget($id: ID!, $input: UpdateAiGatewayBudgetInput!) {
  updateAiGatewayBudget(id: $id, input: $input) {
    alertThreshold
    amount
    createdDate
    enabled
    enforcementMode
    id
    lastModifiedDate
    period
    version
    workspaceId
  }
}
    `);

export const useUpdateAiGatewayBudgetMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiGatewayBudgetMutation, TError, UpdateAiGatewayBudgetMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiGatewayBudgetMutation, TError, UpdateAiGatewayBudgetMutationVariables, TContext>(
      {
    mutationKey: ['updateAiGatewayBudget'],
    mutationFn: (variables?: UpdateAiGatewayBudgetMutationVariables) => fetcher<UpdateAiGatewayBudgetMutation, UpdateAiGatewayBudgetMutationVariables>(UpdateAiGatewayBudgetDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiGatewayBudgetDocument = new TypedDocumentString(`
    mutation deleteAiGatewayBudget($id: ID!) {
  deleteAiGatewayBudget(id: $id)
}
    `);

export const useDeleteAiGatewayBudgetMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiGatewayBudgetMutation, TError, DeleteAiGatewayBudgetMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiGatewayBudgetMutation, TError, DeleteAiGatewayBudgetMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiGatewayBudget'],
    mutationFn: (variables?: DeleteAiGatewayBudgetMutationVariables) => fetcher<DeleteAiGatewayBudgetMutation, DeleteAiGatewayBudgetMutationVariables>(DeleteAiGatewayBudgetDocument, variables)(),
    ...options
  }
    )};

export const AiGatewayModelsDocument = new TypedDocumentString(`
    query aiGatewayModels {
  aiGatewayModels {
    alias
    capabilities
    contextWindow
    createdDate
    enabled
    id
    inputCostPerMTokens
    lastModifiedDate
    name
    outputCostPerMTokens
    providerId
    version
  }
}
    `);

export const useAiGatewayModelsQuery = <
      TData = AiGatewayModelsQuery,
      TError = unknown
    >(
      variables?: AiGatewayModelsQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayModelsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayModelsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayModelsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['aiGatewayModels'] : ['aiGatewayModels', variables],
    queryFn: fetcher<AiGatewayModelsQuery, AiGatewayModelsQueryVariables>(AiGatewayModelsDocument, variables),
    ...options
  }
    )};

export const AiGatewayModelsByProviderDocument = new TypedDocumentString(`
    query aiGatewayModelsByProvider($providerId: ID!) {
  aiGatewayModelsByProvider(providerId: $providerId) {
    alias
    capabilities
    contextWindow
    createdDate
    enabled
    id
    inputCostPerMTokens
    lastModifiedDate
    name
    outputCostPerMTokens
    providerId
    version
  }
}
    `);

export const useAiGatewayModelsByProviderQuery = <
      TData = AiGatewayModelsByProviderQuery,
      TError = unknown
    >(
      variables: AiGatewayModelsByProviderQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayModelsByProviderQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayModelsByProviderQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayModelsByProviderQuery, TError, TData>(
      {
    queryKey: ['aiGatewayModelsByProvider', variables],
    queryFn: fetcher<AiGatewayModelsByProviderQuery, AiGatewayModelsByProviderQueryVariables>(AiGatewayModelsByProviderDocument, variables),
    ...options
  }
    )};

export const CreateAiGatewayModelDocument = new TypedDocumentString(`
    mutation createAiGatewayModel($input: CreateAiGatewayModelInput!) {
  createAiGatewayModel(input: $input) {
    alias
    capabilities
    contextWindow
    createdDate
    enabled
    id
    inputCostPerMTokens
    lastModifiedDate
    name
    outputCostPerMTokens
    providerId
    version
  }
}
    `);

export const useCreateAiGatewayModelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiGatewayModelMutation, TError, CreateAiGatewayModelMutationVariables, TContext>) => {
    
    return useMutation<CreateAiGatewayModelMutation, TError, CreateAiGatewayModelMutationVariables, TContext>(
      {
    mutationKey: ['createAiGatewayModel'],
    mutationFn: (variables?: CreateAiGatewayModelMutationVariables) => fetcher<CreateAiGatewayModelMutation, CreateAiGatewayModelMutationVariables>(CreateAiGatewayModelDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiGatewayModelDocument = new TypedDocumentString(`
    mutation updateAiGatewayModel($id: ID!, $input: UpdateAiGatewayModelInput!) {
  updateAiGatewayModel(id: $id, input: $input) {
    alias
    capabilities
    contextWindow
    createdDate
    enabled
    id
    inputCostPerMTokens
    lastModifiedDate
    name
    outputCostPerMTokens
    providerId
    version
  }
}
    `);

export const useUpdateAiGatewayModelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiGatewayModelMutation, TError, UpdateAiGatewayModelMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiGatewayModelMutation, TError, UpdateAiGatewayModelMutationVariables, TContext>(
      {
    mutationKey: ['updateAiGatewayModel'],
    mutationFn: (variables?: UpdateAiGatewayModelMutationVariables) => fetcher<UpdateAiGatewayModelMutation, UpdateAiGatewayModelMutationVariables>(UpdateAiGatewayModelDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiGatewayModelDocument = new TypedDocumentString(`
    mutation deleteAiGatewayModel($id: ID!) {
  deleteAiGatewayModel(id: $id)
}
    `);

export const useDeleteAiGatewayModelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiGatewayModelMutation, TError, DeleteAiGatewayModelMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiGatewayModelMutation, TError, DeleteAiGatewayModelMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiGatewayModel'],
    mutationFn: (variables?: DeleteAiGatewayModelMutationVariables) => fetcher<DeleteAiGatewayModelMutation, DeleteAiGatewayModelMutationVariables>(DeleteAiGatewayModelDocument, variables)(),
    ...options
  }
    )};

export const PlaygroundChatCompletionDocument = new TypedDocumentString(`
    mutation playgroundChatCompletion($input: PlaygroundChatCompletionInput!) {
  playgroundChatCompletion(input: $input) {
    completionTokens
    content
    cost
    finishReason
    latencyMs
    model
    promptTokens
    totalTokens
    traceId
  }
}
    `);

export const usePlaygroundChatCompletionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<PlaygroundChatCompletionMutation, TError, PlaygroundChatCompletionMutationVariables, TContext>) => {
    
    return useMutation<PlaygroundChatCompletionMutation, TError, PlaygroundChatCompletionMutationVariables, TContext>(
      {
    mutationKey: ['playgroundChatCompletion'],
    mutationFn: (variables?: PlaygroundChatCompletionMutationVariables) => fetcher<PlaygroundChatCompletionMutation, PlaygroundChatCompletionMutationVariables>(PlaygroundChatCompletionDocument, variables)(),
    ...options
  }
    )};

export const AiGatewayProjectsDocument = new TypedDocumentString(`
    query aiGatewayProjects($workspaceId: ID!) {
  aiGatewayProjects(workspaceId: $workspaceId) {
    cachingEnabled
    cacheTtlMinutes
    compressionEnabled
    createdDate
    description
    id
    lastModifiedDate
    logRetentionDays
    name
    retryMaxAttempts
    routingPolicyId
    slug
    timeoutSeconds
    version
    workspaceId
  }
}
    `);

export const useAiGatewayProjectsQuery = <
      TData = AiGatewayProjectsQuery,
      TError = unknown
    >(
      variables: AiGatewayProjectsQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayProjectsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayProjectsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayProjectsQuery, TError, TData>(
      {
    queryKey: ['aiGatewayProjects', variables],
    queryFn: fetcher<AiGatewayProjectsQuery, AiGatewayProjectsQueryVariables>(AiGatewayProjectsDocument, variables),
    ...options
  }
    )};

export const CreateAiGatewayProjectDocument = new TypedDocumentString(`
    mutation createAiGatewayProject($input: CreateAiGatewayProjectInput!) {
  createAiGatewayProject(input: $input) {
    id
    name
    slug
  }
}
    `);

export const useCreateAiGatewayProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiGatewayProjectMutation, TError, CreateAiGatewayProjectMutationVariables, TContext>) => {
    
    return useMutation<CreateAiGatewayProjectMutation, TError, CreateAiGatewayProjectMutationVariables, TContext>(
      {
    mutationKey: ['createAiGatewayProject'],
    mutationFn: (variables?: CreateAiGatewayProjectMutationVariables) => fetcher<CreateAiGatewayProjectMutation, CreateAiGatewayProjectMutationVariables>(CreateAiGatewayProjectDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiGatewayProjectDocument = new TypedDocumentString(`
    mutation updateAiGatewayProject($id: ID!, $input: UpdateAiGatewayProjectInput!) {
  updateAiGatewayProject(id: $id, input: $input) {
    id
    name
    slug
  }
}
    `);

export const useUpdateAiGatewayProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiGatewayProjectMutation, TError, UpdateAiGatewayProjectMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiGatewayProjectMutation, TError, UpdateAiGatewayProjectMutationVariables, TContext>(
      {
    mutationKey: ['updateAiGatewayProject'],
    mutationFn: (variables?: UpdateAiGatewayProjectMutationVariables) => fetcher<UpdateAiGatewayProjectMutation, UpdateAiGatewayProjectMutationVariables>(UpdateAiGatewayProjectDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiGatewayProjectDocument = new TypedDocumentString(`
    mutation deleteAiGatewayProject($id: ID!) {
  deleteAiGatewayProject(id: $id)
}
    `);

export const useDeleteAiGatewayProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiGatewayProjectMutation, TError, DeleteAiGatewayProjectMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiGatewayProjectMutation, TError, DeleteAiGatewayProjectMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiGatewayProject'],
    mutationFn: (variables?: DeleteAiGatewayProjectMutationVariables) => fetcher<DeleteAiGatewayProjectMutation, DeleteAiGatewayProjectMutationVariables>(DeleteAiGatewayProjectDocument, variables)(),
    ...options
  }
    )};

export const AiGatewayProvidersDocument = new TypedDocumentString(`
    query aiGatewayProviders {
  aiGatewayProviders {
    baseUrl
    config
    createdBy
    createdDate
    enabled
    id
    lastModifiedBy
    lastModifiedDate
    name
    type
    version
  }
}
    `);

export const useAiGatewayProvidersQuery = <
      TData = AiGatewayProvidersQuery,
      TError = unknown
    >(
      variables?: AiGatewayProvidersQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayProvidersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayProvidersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayProvidersQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['aiGatewayProviders'] : ['aiGatewayProviders', variables],
    queryFn: fetcher<AiGatewayProvidersQuery, AiGatewayProvidersQueryVariables>(AiGatewayProvidersDocument, variables),
    ...options
  }
    )};

export const AiGatewayProviderDocument = new TypedDocumentString(`
    query aiGatewayProvider($id: ID!) {
  aiGatewayProvider(id: $id) {
    baseUrl
    config
    createdBy
    createdDate
    enabled
    id
    lastModifiedBy
    lastModifiedDate
    name
    type
    version
  }
}
    `);

export const useAiGatewayProviderQuery = <
      TData = AiGatewayProviderQuery,
      TError = unknown
    >(
      variables: AiGatewayProviderQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayProviderQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayProviderQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayProviderQuery, TError, TData>(
      {
    queryKey: ['aiGatewayProvider', variables],
    queryFn: fetcher<AiGatewayProviderQuery, AiGatewayProviderQueryVariables>(AiGatewayProviderDocument, variables),
    ...options
  }
    )};

export const CreateAiGatewayProviderDocument = new TypedDocumentString(`
    mutation createAiGatewayProvider($input: CreateAiGatewayProviderInput!) {
  createAiGatewayProvider(input: $input) {
    baseUrl
    config
    createdBy
    createdDate
    enabled
    id
    lastModifiedBy
    lastModifiedDate
    name
    type
    version
  }
}
    `);

export const useCreateAiGatewayProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiGatewayProviderMutation, TError, CreateAiGatewayProviderMutationVariables, TContext>) => {
    
    return useMutation<CreateAiGatewayProviderMutation, TError, CreateAiGatewayProviderMutationVariables, TContext>(
      {
    mutationKey: ['createAiGatewayProvider'],
    mutationFn: (variables?: CreateAiGatewayProviderMutationVariables) => fetcher<CreateAiGatewayProviderMutation, CreateAiGatewayProviderMutationVariables>(CreateAiGatewayProviderDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiGatewayProviderDocument = new TypedDocumentString(`
    mutation updateAiGatewayProvider($id: ID!, $input: UpdateAiGatewayProviderInput!) {
  updateAiGatewayProvider(id: $id, input: $input) {
    baseUrl
    config
    createdBy
    createdDate
    enabled
    id
    lastModifiedBy
    lastModifiedDate
    name
    type
    version
  }
}
    `);

export const useUpdateAiGatewayProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiGatewayProviderMutation, TError, UpdateAiGatewayProviderMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiGatewayProviderMutation, TError, UpdateAiGatewayProviderMutationVariables, TContext>(
      {
    mutationKey: ['updateAiGatewayProvider'],
    mutationFn: (variables?: UpdateAiGatewayProviderMutationVariables) => fetcher<UpdateAiGatewayProviderMutation, UpdateAiGatewayProviderMutationVariables>(UpdateAiGatewayProviderDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiGatewayProviderDocument = new TypedDocumentString(`
    mutation deleteAiGatewayProvider($id: ID!) {
  deleteAiGatewayProvider(id: $id)
}
    `);

export const useDeleteAiGatewayProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiGatewayProviderMutation, TError, DeleteAiGatewayProviderMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiGatewayProviderMutation, TError, DeleteAiGatewayProviderMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiGatewayProvider'],
    mutationFn: (variables?: DeleteAiGatewayProviderMutationVariables) => fetcher<DeleteAiGatewayProviderMutation, DeleteAiGatewayProviderMutationVariables>(DeleteAiGatewayProviderDocument, variables)(),
    ...options
  }
    )};

export const AiGatewayRateLimitsDocument = new TypedDocumentString(`
    query aiGatewayRateLimits($workspaceId: ID!) {
  aiGatewayRateLimits(workspaceId: $workspaceId) {
    createdDate
    enabled
    id
    lastModifiedDate
    limitType
    limitValue
    name
    projectId
    propertyKey
    scope
    version
    windowSeconds
    workspaceId
  }
}
    `);

export const useAiGatewayRateLimitsQuery = <
      TData = AiGatewayRateLimitsQuery,
      TError = unknown
    >(
      variables: AiGatewayRateLimitsQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayRateLimitsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayRateLimitsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayRateLimitsQuery, TError, TData>(
      {
    queryKey: ['aiGatewayRateLimits', variables],
    queryFn: fetcher<AiGatewayRateLimitsQuery, AiGatewayRateLimitsQueryVariables>(AiGatewayRateLimitsDocument, variables),
    ...options
  }
    )};

export const CreateAiGatewayRateLimitDocument = new TypedDocumentString(`
    mutation createAiGatewayRateLimit($input: CreateAiGatewayRateLimitInput!) {
  createAiGatewayRateLimit(input: $input) {
    createdDate
    enabled
    id
    lastModifiedDate
    limitType
    limitValue
    name
    projectId
    propertyKey
    scope
    version
    windowSeconds
    workspaceId
  }
}
    `);

export const useCreateAiGatewayRateLimitMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiGatewayRateLimitMutation, TError, CreateAiGatewayRateLimitMutationVariables, TContext>) => {
    
    return useMutation<CreateAiGatewayRateLimitMutation, TError, CreateAiGatewayRateLimitMutationVariables, TContext>(
      {
    mutationKey: ['createAiGatewayRateLimit'],
    mutationFn: (variables?: CreateAiGatewayRateLimitMutationVariables) => fetcher<CreateAiGatewayRateLimitMutation, CreateAiGatewayRateLimitMutationVariables>(CreateAiGatewayRateLimitDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiGatewayRateLimitDocument = new TypedDocumentString(`
    mutation updateAiGatewayRateLimit($id: ID!, $input: UpdateAiGatewayRateLimitInput!) {
  updateAiGatewayRateLimit(id: $id, input: $input) {
    createdDate
    enabled
    id
    lastModifiedDate
    limitType
    limitValue
    name
    projectId
    propertyKey
    scope
    version
    windowSeconds
    workspaceId
  }
}
    `);

export const useUpdateAiGatewayRateLimitMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiGatewayRateLimitMutation, TError, UpdateAiGatewayRateLimitMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiGatewayRateLimitMutation, TError, UpdateAiGatewayRateLimitMutationVariables, TContext>(
      {
    mutationKey: ['updateAiGatewayRateLimit'],
    mutationFn: (variables?: UpdateAiGatewayRateLimitMutationVariables) => fetcher<UpdateAiGatewayRateLimitMutation, UpdateAiGatewayRateLimitMutationVariables>(UpdateAiGatewayRateLimitDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiGatewayRateLimitDocument = new TypedDocumentString(`
    mutation deleteAiGatewayRateLimit($id: ID!) {
  deleteAiGatewayRateLimit(id: $id)
}
    `);

export const useDeleteAiGatewayRateLimitMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiGatewayRateLimitMutation, TError, DeleteAiGatewayRateLimitMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiGatewayRateLimitMutation, TError, DeleteAiGatewayRateLimitMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiGatewayRateLimit'],
    mutationFn: (variables?: DeleteAiGatewayRateLimitMutationVariables) => fetcher<DeleteAiGatewayRateLimitMutation, DeleteAiGatewayRateLimitMutationVariables>(DeleteAiGatewayRateLimitDocument, variables)(),
    ...options
  }
    )};

export const AiGatewayRequestLogsDocument = new TypedDocumentString(`
    query aiGatewayRequestLogs($startDate: Long!, $endDate: Long!) {
  aiGatewayRequestLogs(startDate: $startDate, endDate: $endDate) {
    apiKeyId
    cacheHit
    cost
    createdDate
    errorMessage
    id
    inputTokens
    latencyMs
    outputTokens
    requestId
    requestedModel
    routedModel
    routedProvider
    routingPolicyId
    routingStrategy
    status
  }
}
    `);

export const useAiGatewayRequestLogsQuery = <
      TData = AiGatewayRequestLogsQuery,
      TError = unknown
    >(
      variables: AiGatewayRequestLogsQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayRequestLogsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayRequestLogsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayRequestLogsQuery, TError, TData>(
      {
    queryKey: ['aiGatewayRequestLogs', variables],
    queryFn: fetcher<AiGatewayRequestLogsQuery, AiGatewayRequestLogsQueryVariables>(AiGatewayRequestLogsDocument, variables),
    ...options
  }
    )};

export const AiGatewayRoutingPoliciesDocument = new TypedDocumentString(`
    query aiGatewayRoutingPolicies {
  aiGatewayRoutingPolicies {
    config
    createdDate
    deployments {
      enabled
      id
      maxRpm
      maxTpm
      modelId
      priorityOrder
      routingPolicyId
      weight
    }
    enabled
    fallbackModel
    id
    lastModifiedDate
    name
    strategy
    version
  }
}
    `);

export const useAiGatewayRoutingPoliciesQuery = <
      TData = AiGatewayRoutingPoliciesQuery,
      TError = unknown
    >(
      variables?: AiGatewayRoutingPoliciesQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayRoutingPoliciesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayRoutingPoliciesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayRoutingPoliciesQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['aiGatewayRoutingPolicies'] : ['aiGatewayRoutingPolicies', variables],
    queryFn: fetcher<AiGatewayRoutingPoliciesQuery, AiGatewayRoutingPoliciesQueryVariables>(AiGatewayRoutingPoliciesDocument, variables),
    ...options
  }
    )};

export const CreateAiGatewayRoutingPolicyDocument = new TypedDocumentString(`
    mutation createAiGatewayRoutingPolicy($input: CreateAiGatewayRoutingPolicyInput!) {
  createAiGatewayRoutingPolicy(input: $input) {
    config
    createdDate
    enabled
    fallbackModel
    id
    lastModifiedDate
    name
    strategy
    version
  }
}
    `);

export const useCreateAiGatewayRoutingPolicyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiGatewayRoutingPolicyMutation, TError, CreateAiGatewayRoutingPolicyMutationVariables, TContext>) => {
    
    return useMutation<CreateAiGatewayRoutingPolicyMutation, TError, CreateAiGatewayRoutingPolicyMutationVariables, TContext>(
      {
    mutationKey: ['createAiGatewayRoutingPolicy'],
    mutationFn: (variables?: CreateAiGatewayRoutingPolicyMutationVariables) => fetcher<CreateAiGatewayRoutingPolicyMutation, CreateAiGatewayRoutingPolicyMutationVariables>(CreateAiGatewayRoutingPolicyDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiGatewayRoutingPolicyDocument = new TypedDocumentString(`
    mutation updateAiGatewayRoutingPolicy($id: ID!, $input: UpdateAiGatewayRoutingPolicyInput!) {
  updateAiGatewayRoutingPolicy(id: $id, input: $input) {
    config
    createdDate
    enabled
    fallbackModel
    id
    lastModifiedDate
    name
    strategy
    version
  }
}
    `);

export const useUpdateAiGatewayRoutingPolicyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiGatewayRoutingPolicyMutation, TError, UpdateAiGatewayRoutingPolicyMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiGatewayRoutingPolicyMutation, TError, UpdateAiGatewayRoutingPolicyMutationVariables, TContext>(
      {
    mutationKey: ['updateAiGatewayRoutingPolicy'],
    mutationFn: (variables?: UpdateAiGatewayRoutingPolicyMutationVariables) => fetcher<UpdateAiGatewayRoutingPolicyMutation, UpdateAiGatewayRoutingPolicyMutationVariables>(UpdateAiGatewayRoutingPolicyDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiGatewayRoutingPolicyDocument = new TypedDocumentString(`
    mutation deleteAiGatewayRoutingPolicy($id: ID!) {
  deleteAiGatewayRoutingPolicy(id: $id)
}
    `);

export const useDeleteAiGatewayRoutingPolicyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiGatewayRoutingPolicyMutation, TError, DeleteAiGatewayRoutingPolicyMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiGatewayRoutingPolicyMutation, TError, DeleteAiGatewayRoutingPolicyMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiGatewayRoutingPolicy'],
    mutationFn: (variables?: DeleteAiGatewayRoutingPolicyMutationVariables) => fetcher<DeleteAiGatewayRoutingPolicyMutation, DeleteAiGatewayRoutingPolicyMutationVariables>(DeleteAiGatewayRoutingPolicyDocument, variables)(),
    ...options
  }
    )};

export const AiGatewaySpendSummariesDocument = new TypedDocumentString(`
    query aiGatewaySpendSummaries($startDate: Long!, $endDate: Long!) {
  aiGatewaySpendSummaries(startDate: $startDate, endDate: $endDate) {
    apiKeyId
    createdDate
    id
    model
    periodEnd
    periodStart
    provider
    requestCount
    totalCost
    totalInputTokens
    totalOutputTokens
  }
}
    `);

export const useAiGatewaySpendSummariesQuery = <
      TData = AiGatewaySpendSummariesQuery,
      TError = unknown
    >(
      variables: AiGatewaySpendSummariesQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewaySpendSummariesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewaySpendSummariesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewaySpendSummariesQuery, TError, TData>(
      {
    queryKey: ['aiGatewaySpendSummaries', variables],
    queryFn: fetcher<AiGatewaySpendSummariesQuery, AiGatewaySpendSummariesQueryVariables>(AiGatewaySpendSummariesDocument, variables),
    ...options
  }
    )};

export const AiGatewayTagsDocument = new TypedDocumentString(`
    query aiGatewayTags($workspaceId: ID!) {
  aiGatewayTags(workspaceId: $workspaceId) {
    color
    createdDate
    id
    lastModifiedDate
    name
    version
    workspaceId
  }
}
    `);

export const useAiGatewayTagsQuery = <
      TData = AiGatewayTagsQuery,
      TError = unknown
    >(
      variables: AiGatewayTagsQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayTagsQuery, TError, TData>(
      {
    queryKey: ['aiGatewayTags', variables],
    queryFn: fetcher<AiGatewayTagsQuery, AiGatewayTagsQueryVariables>(AiGatewayTagsDocument, variables),
    ...options
  }
    )};

export const CreateAiGatewayTagDocument = new TypedDocumentString(`
    mutation createAiGatewayTag($input: CreateAiGatewayTagInput!) {
  createAiGatewayTag(input: $input) {
    color
    id
    name
  }
}
    `);

export const useCreateAiGatewayTagMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiGatewayTagMutation, TError, CreateAiGatewayTagMutationVariables, TContext>) => {
    
    return useMutation<CreateAiGatewayTagMutation, TError, CreateAiGatewayTagMutationVariables, TContext>(
      {
    mutationKey: ['createAiGatewayTag'],
    mutationFn: (variables?: CreateAiGatewayTagMutationVariables) => fetcher<CreateAiGatewayTagMutation, CreateAiGatewayTagMutationVariables>(CreateAiGatewayTagDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiGatewayTagDocument = new TypedDocumentString(`
    mutation updateAiGatewayTag($id: ID!, $name: String, $color: String) {
  updateAiGatewayTag(color: $color, id: $id, name: $name) {
    color
    id
    name
  }
}
    `);

export const useUpdateAiGatewayTagMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiGatewayTagMutation, TError, UpdateAiGatewayTagMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiGatewayTagMutation, TError, UpdateAiGatewayTagMutationVariables, TContext>(
      {
    mutationKey: ['updateAiGatewayTag'],
    mutationFn: (variables?: UpdateAiGatewayTagMutationVariables) => fetcher<UpdateAiGatewayTagMutation, UpdateAiGatewayTagMutationVariables>(UpdateAiGatewayTagDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiGatewayTagDocument = new TypedDocumentString(`
    mutation deleteAiGatewayTag($id: ID!) {
  deleteAiGatewayTag(id: $id)
}
    `);

export const useDeleteAiGatewayTagMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiGatewayTagMutation, TError, DeleteAiGatewayTagMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiGatewayTagMutation, TError, DeleteAiGatewayTagMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiGatewayTag'],
    mutationFn: (variables?: DeleteAiGatewayTagMutationVariables) => fetcher<DeleteAiGatewayTagMutation, DeleteAiGatewayTagMutationVariables>(DeleteAiGatewayTagDocument, variables)(),
    ...options
  }
    )};

export const AiGatewayWorkspaceSettingsDocument = new TypedDocumentString(`
    query aiGatewayWorkspaceSettings($workspaceId: ID!) {
  aiGatewayWorkspaceSettings(workspaceId: $workspaceId) {
    cacheEnabled
    cacheTtlSeconds
    defaultRoutingPolicyId
    logRetentionDays
    redactPii
    retryCount
    softBudgetWarningPct
    timeoutMs
    workspaceId
  }
}
    `);

export const useAiGatewayWorkspaceSettingsQuery = <
      TData = AiGatewayWorkspaceSettingsQuery,
      TError = unknown
    >(
      variables: AiGatewayWorkspaceSettingsQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayWorkspaceSettingsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayWorkspaceSettingsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayWorkspaceSettingsQuery, TError, TData>(
      {
    queryKey: ['aiGatewayWorkspaceSettings', variables],
    queryFn: fetcher<AiGatewayWorkspaceSettingsQuery, AiGatewayWorkspaceSettingsQueryVariables>(AiGatewayWorkspaceSettingsDocument, variables),
    ...options
  }
    )};

export const UpdateAiGatewayWorkspaceSettingsDocument = new TypedDocumentString(`
    mutation updateAiGatewayWorkspaceSettings($input: AiGatewayWorkspaceSettingsInput!) {
  updateAiGatewayWorkspaceSettings(input: $input) {
    cacheEnabled
    cacheTtlSeconds
    defaultRoutingPolicyId
    logRetentionDays
    redactPii
    retryCount
    softBudgetWarningPct
    timeoutMs
    workspaceId
  }
}
    `);

export const useUpdateAiGatewayWorkspaceSettingsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiGatewayWorkspaceSettingsMutation, TError, UpdateAiGatewayWorkspaceSettingsMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiGatewayWorkspaceSettingsMutation, TError, UpdateAiGatewayWorkspaceSettingsMutationVariables, TContext>(
      {
    mutationKey: ['updateAiGatewayWorkspaceSettings'],
    mutationFn: (variables?: UpdateAiGatewayWorkspaceSettingsMutationVariables) => fetcher<UpdateAiGatewayWorkspaceSettingsMutation, UpdateAiGatewayWorkspaceSettingsMutationVariables>(UpdateAiGatewayWorkspaceSettingsDocument, variables)(),
    ...options
  }
    )};

export const AiObservabilityAlertEventsDocument = new TypedDocumentString(`
    query aiObservabilityAlertEvents($alertRuleId: ID!) {
  aiObservabilityAlertEvents(alertRuleId: $alertRuleId) {
    alertRuleId
    createdDate
    id
    message
    status
    triggeredValue
  }
}
    `);

export const useAiObservabilityAlertEventsQuery = <
      TData = AiObservabilityAlertEventsQuery,
      TError = unknown
    >(
      variables: AiObservabilityAlertEventsQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityAlertEventsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityAlertEventsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityAlertEventsQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityAlertEvents', variables],
    queryFn: fetcher<AiObservabilityAlertEventsQuery, AiObservabilityAlertEventsQueryVariables>(AiObservabilityAlertEventsDocument, variables),
    ...options
  }
    )};

export const AcknowledgeAiObservabilityAlertEventDocument = new TypedDocumentString(`
    mutation acknowledgeAiObservabilityAlertEvent($id: ID!) {
  acknowledgeAiObservabilityAlertEvent(id: $id) {
    alertRuleId
    createdDate
    id
    message
    status
    triggeredValue
  }
}
    `);

export const useAcknowledgeAiObservabilityAlertEventMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<AcknowledgeAiObservabilityAlertEventMutation, TError, AcknowledgeAiObservabilityAlertEventMutationVariables, TContext>) => {
    
    return useMutation<AcknowledgeAiObservabilityAlertEventMutation, TError, AcknowledgeAiObservabilityAlertEventMutationVariables, TContext>(
      {
    mutationKey: ['acknowledgeAiObservabilityAlertEvent'],
    mutationFn: (variables?: AcknowledgeAiObservabilityAlertEventMutationVariables) => fetcher<AcknowledgeAiObservabilityAlertEventMutation, AcknowledgeAiObservabilityAlertEventMutationVariables>(AcknowledgeAiObservabilityAlertEventDocument, variables)(),
    ...options
  }
    )};

export const AiObservabilityAlertRulesDocument = new TypedDocumentString(`
    query aiObservabilityAlertRules($workspaceId: ID!) {
  aiObservabilityAlertRules(workspaceId: $workspaceId) {
    channelIds
    condition
    cooldownMinutes
    createdDate
    enabled
    filters
    id
    lastModifiedDate
    metric
    name
    projectId
    snoozedUntil
    threshold
    version
    windowMinutes
    workspaceId
  }
}
    `);

export const useAiObservabilityAlertRulesQuery = <
      TData = AiObservabilityAlertRulesQuery,
      TError = unknown
    >(
      variables: AiObservabilityAlertRulesQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityAlertRulesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityAlertRulesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityAlertRulesQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityAlertRules', variables],
    queryFn: fetcher<AiObservabilityAlertRulesQuery, AiObservabilityAlertRulesQueryVariables>(AiObservabilityAlertRulesDocument, variables),
    ...options
  }
    )};

export const AiObservabilityAlertRuleDocument = new TypedDocumentString(`
    query aiObservabilityAlertRule($id: ID!) {
  aiObservabilityAlertRule(id: $id) {
    channelIds
    condition
    cooldownMinutes
    createdDate
    enabled
    filters
    id
    lastModifiedDate
    metric
    name
    projectId
    snoozedUntil
    threshold
    version
    windowMinutes
    workspaceId
  }
}
    `);

export const useAiObservabilityAlertRuleQuery = <
      TData = AiObservabilityAlertRuleQuery,
      TError = unknown
    >(
      variables: AiObservabilityAlertRuleQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityAlertRuleQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityAlertRuleQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityAlertRuleQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityAlertRule', variables],
    queryFn: fetcher<AiObservabilityAlertRuleQuery, AiObservabilityAlertRuleQueryVariables>(AiObservabilityAlertRuleDocument, variables),
    ...options
  }
    )};

export const CreateAiObservabilityAlertRuleDocument = new TypedDocumentString(`
    mutation createAiObservabilityAlertRule($input: AiObservabilityAlertRuleInput!) {
  createAiObservabilityAlertRule(input: $input) {
    channelIds
    condition
    cooldownMinutes
    createdDate
    enabled
    filters
    id
    lastModifiedDate
    metric
    name
    projectId
    threshold
    version
    windowMinutes
    workspaceId
  }
}
    `);

export const useCreateAiObservabilityAlertRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiObservabilityAlertRuleMutation, TError, CreateAiObservabilityAlertRuleMutationVariables, TContext>) => {
    
    return useMutation<CreateAiObservabilityAlertRuleMutation, TError, CreateAiObservabilityAlertRuleMutationVariables, TContext>(
      {
    mutationKey: ['createAiObservabilityAlertRule'],
    mutationFn: (variables?: CreateAiObservabilityAlertRuleMutationVariables) => fetcher<CreateAiObservabilityAlertRuleMutation, CreateAiObservabilityAlertRuleMutationVariables>(CreateAiObservabilityAlertRuleDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiObservabilityAlertRuleDocument = new TypedDocumentString(`
    mutation updateAiObservabilityAlertRule($id: ID!, $input: AiObservabilityAlertRuleInput!) {
  updateAiObservabilityAlertRule(id: $id, input: $input) {
    channelIds
    condition
    cooldownMinutes
    createdDate
    enabled
    filters
    id
    lastModifiedDate
    metric
    name
    projectId
    threshold
    version
    windowMinutes
    workspaceId
  }
}
    `);

export const useUpdateAiObservabilityAlertRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiObservabilityAlertRuleMutation, TError, UpdateAiObservabilityAlertRuleMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiObservabilityAlertRuleMutation, TError, UpdateAiObservabilityAlertRuleMutationVariables, TContext>(
      {
    mutationKey: ['updateAiObservabilityAlertRule'],
    mutationFn: (variables?: UpdateAiObservabilityAlertRuleMutationVariables) => fetcher<UpdateAiObservabilityAlertRuleMutation, UpdateAiObservabilityAlertRuleMutationVariables>(UpdateAiObservabilityAlertRuleDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiObservabilityAlertRuleDocument = new TypedDocumentString(`
    mutation deleteAiObservabilityAlertRule($id: ID!) {
  deleteAiObservabilityAlertRule(id: $id)
}
    `);

export const useDeleteAiObservabilityAlertRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiObservabilityAlertRuleMutation, TError, DeleteAiObservabilityAlertRuleMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiObservabilityAlertRuleMutation, TError, DeleteAiObservabilityAlertRuleMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiObservabilityAlertRule'],
    mutationFn: (variables?: DeleteAiObservabilityAlertRuleMutationVariables) => fetcher<DeleteAiObservabilityAlertRuleMutation, DeleteAiObservabilityAlertRuleMutationVariables>(DeleteAiObservabilityAlertRuleDocument, variables)(),
    ...options
  }
    )};

export const TestAiObservabilityAlertRuleDocument = new TypedDocumentString(`
    mutation testAiObservabilityAlertRule($id: ID!) {
  testAiObservabilityAlertRule(id: $id)
}
    `);

export const useTestAiObservabilityAlertRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<TestAiObservabilityAlertRuleMutation, TError, TestAiObservabilityAlertRuleMutationVariables, TContext>) => {
    
    return useMutation<TestAiObservabilityAlertRuleMutation, TError, TestAiObservabilityAlertRuleMutationVariables, TContext>(
      {
    mutationKey: ['testAiObservabilityAlertRule'],
    mutationFn: (variables?: TestAiObservabilityAlertRuleMutationVariables) => fetcher<TestAiObservabilityAlertRuleMutation, TestAiObservabilityAlertRuleMutationVariables>(TestAiObservabilityAlertRuleDocument, variables)(),
    ...options
  }
    )};

export const SnoozeAiObservabilityAlertRuleDocument = new TypedDocumentString(`
    mutation snoozeAiObservabilityAlertRule($id: ID!, $until: Long!) {
  snoozeAiObservabilityAlertRule(id: $id, until: $until) {
    id
    snoozedUntil
  }
}
    `);

export const useSnoozeAiObservabilityAlertRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SnoozeAiObservabilityAlertRuleMutation, TError, SnoozeAiObservabilityAlertRuleMutationVariables, TContext>) => {
    
    return useMutation<SnoozeAiObservabilityAlertRuleMutation, TError, SnoozeAiObservabilityAlertRuleMutationVariables, TContext>(
      {
    mutationKey: ['snoozeAiObservabilityAlertRule'],
    mutationFn: (variables?: SnoozeAiObservabilityAlertRuleMutationVariables) => fetcher<SnoozeAiObservabilityAlertRuleMutation, SnoozeAiObservabilityAlertRuleMutationVariables>(SnoozeAiObservabilityAlertRuleDocument, variables)(),
    ...options
  }
    )};

export const UnsnoozeAiObservabilityAlertRuleDocument = new TypedDocumentString(`
    mutation unsnoozeAiObservabilityAlertRule($id: ID!) {
  unsnoozeAiObservabilityAlertRule(id: $id) {
    id
    snoozedUntil
  }
}
    `);

export const useUnsnoozeAiObservabilityAlertRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UnsnoozeAiObservabilityAlertRuleMutation, TError, UnsnoozeAiObservabilityAlertRuleMutationVariables, TContext>) => {
    
    return useMutation<UnsnoozeAiObservabilityAlertRuleMutation, TError, UnsnoozeAiObservabilityAlertRuleMutationVariables, TContext>(
      {
    mutationKey: ['unsnoozeAiObservabilityAlertRule'],
    mutationFn: (variables?: UnsnoozeAiObservabilityAlertRuleMutationVariables) => fetcher<UnsnoozeAiObservabilityAlertRuleMutation, UnsnoozeAiObservabilityAlertRuleMutationVariables>(UnsnoozeAiObservabilityAlertRuleDocument, variables)(),
    ...options
  }
    )};

export const AiObservabilityExportJobsDocument = new TypedDocumentString(`
    query aiObservabilityExportJobs($workspaceId: ID!) {
  aiObservabilityExportJobs(workspaceId: $workspaceId) {
    createdBy
    createdDate
    errorMessage
    filePath
    filters
    format
    id
    projectId
    recordCount
    scope
    status
    type
    workspaceId
  }
}
    `);

export const useAiObservabilityExportJobsQuery = <
      TData = AiObservabilityExportJobsQuery,
      TError = unknown
    >(
      variables: AiObservabilityExportJobsQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityExportJobsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityExportJobsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityExportJobsQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityExportJobs', variables],
    queryFn: fetcher<AiObservabilityExportJobsQuery, AiObservabilityExportJobsQueryVariables>(AiObservabilityExportJobsDocument, variables),
    ...options
  }
    )};

export const AiObservabilityExportJobDocument = new TypedDocumentString(`
    query aiObservabilityExportJob($id: ID!) {
  aiObservabilityExportJob(id: $id) {
    createdBy
    createdDate
    errorMessage
    filePath
    filters
    format
    id
    projectId
    recordCount
    scope
    status
    type
    workspaceId
  }
}
    `);

export const useAiObservabilityExportJobQuery = <
      TData = AiObservabilityExportJobQuery,
      TError = unknown
    >(
      variables: AiObservabilityExportJobQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityExportJobQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityExportJobQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityExportJobQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityExportJob', variables],
    queryFn: fetcher<AiObservabilityExportJobQuery, AiObservabilityExportJobQueryVariables>(AiObservabilityExportJobDocument, variables),
    ...options
  }
    )};

export const CreateAiObservabilityExportJobDocument = new TypedDocumentString(`
    mutation createAiObservabilityExportJob($workspaceId: ID!, $projectId: ID, $format: AiObservabilityExportFormat!, $scope: AiObservabilityExportScope!, $filters: String) {
  createAiObservabilityExportJob(
    workspaceId: $workspaceId
    projectId: $projectId
    format: $format
    scope: $scope
    filters: $filters
  ) {
    createdBy
    createdDate
    format
    id
    scope
    status
    type
    workspaceId
  }
}
    `);

export const useCreateAiObservabilityExportJobMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiObservabilityExportJobMutation, TError, CreateAiObservabilityExportJobMutationVariables, TContext>) => {
    
    return useMutation<CreateAiObservabilityExportJobMutation, TError, CreateAiObservabilityExportJobMutationVariables, TContext>(
      {
    mutationKey: ['createAiObservabilityExportJob'],
    mutationFn: (variables?: CreateAiObservabilityExportJobMutationVariables) => fetcher<CreateAiObservabilityExportJobMutation, CreateAiObservabilityExportJobMutationVariables>(CreateAiObservabilityExportJobDocument, variables)(),
    ...options
  }
    )};

export const CancelAiObservabilityExportJobDocument = new TypedDocumentString(`
    mutation cancelAiObservabilityExportJob($id: ID!) {
  cancelAiObservabilityExportJob(id: $id) {
    id
    status
  }
}
    `);

export const useCancelAiObservabilityExportJobMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CancelAiObservabilityExportJobMutation, TError, CancelAiObservabilityExportJobMutationVariables, TContext>) => {
    
    return useMutation<CancelAiObservabilityExportJobMutation, TError, CancelAiObservabilityExportJobMutationVariables, TContext>(
      {
    mutationKey: ['cancelAiObservabilityExportJob'],
    mutationFn: (variables?: CancelAiObservabilityExportJobMutationVariables) => fetcher<CancelAiObservabilityExportJobMutation, CancelAiObservabilityExportJobMutationVariables>(CancelAiObservabilityExportJobDocument, variables)(),
    ...options
  }
    )};

export const AiObservabilityNotificationChannelsDocument = new TypedDocumentString(`
    query aiObservabilityNotificationChannels($workspaceId: ID!) {
  aiObservabilityNotificationChannels(workspaceId: $workspaceId) {
    config
    createdDate
    enabled
    id
    lastModifiedDate
    name
    type
    version
    workspaceId
  }
}
    `);

export const useAiObservabilityNotificationChannelsQuery = <
      TData = AiObservabilityNotificationChannelsQuery,
      TError = unknown
    >(
      variables: AiObservabilityNotificationChannelsQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityNotificationChannelsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityNotificationChannelsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityNotificationChannelsQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityNotificationChannels', variables],
    queryFn: fetcher<AiObservabilityNotificationChannelsQuery, AiObservabilityNotificationChannelsQueryVariables>(AiObservabilityNotificationChannelsDocument, variables),
    ...options
  }
    )};

export const CreateAiObservabilityNotificationChannelDocument = new TypedDocumentString(`
    mutation createAiObservabilityNotificationChannel($input: AiObservabilityNotificationChannelInput!) {
  createAiObservabilityNotificationChannel(input: $input) {
    config
    createdDate
    enabled
    id
    lastModifiedDate
    name
    type
    version
    workspaceId
  }
}
    `);

export const useCreateAiObservabilityNotificationChannelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiObservabilityNotificationChannelMutation, TError, CreateAiObservabilityNotificationChannelMutationVariables, TContext>) => {
    
    return useMutation<CreateAiObservabilityNotificationChannelMutation, TError, CreateAiObservabilityNotificationChannelMutationVariables, TContext>(
      {
    mutationKey: ['createAiObservabilityNotificationChannel'],
    mutationFn: (variables?: CreateAiObservabilityNotificationChannelMutationVariables) => fetcher<CreateAiObservabilityNotificationChannelMutation, CreateAiObservabilityNotificationChannelMutationVariables>(CreateAiObservabilityNotificationChannelDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiObservabilityNotificationChannelDocument = new TypedDocumentString(`
    mutation updateAiObservabilityNotificationChannel($id: ID!, $input: AiObservabilityNotificationChannelInput!) {
  updateAiObservabilityNotificationChannel(id: $id, input: $input) {
    config
    createdDate
    enabled
    id
    lastModifiedDate
    name
    type
    version
    workspaceId
  }
}
    `);

export const useUpdateAiObservabilityNotificationChannelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiObservabilityNotificationChannelMutation, TError, UpdateAiObservabilityNotificationChannelMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiObservabilityNotificationChannelMutation, TError, UpdateAiObservabilityNotificationChannelMutationVariables, TContext>(
      {
    mutationKey: ['updateAiObservabilityNotificationChannel'],
    mutationFn: (variables?: UpdateAiObservabilityNotificationChannelMutationVariables) => fetcher<UpdateAiObservabilityNotificationChannelMutation, UpdateAiObservabilityNotificationChannelMutationVariables>(UpdateAiObservabilityNotificationChannelDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiObservabilityNotificationChannelDocument = new TypedDocumentString(`
    mutation deleteAiObservabilityNotificationChannel($id: ID!) {
  deleteAiObservabilityNotificationChannel(id: $id)
}
    `);

export const useDeleteAiObservabilityNotificationChannelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiObservabilityNotificationChannelMutation, TError, DeleteAiObservabilityNotificationChannelMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiObservabilityNotificationChannelMutation, TError, DeleteAiObservabilityNotificationChannelMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiObservabilityNotificationChannel'],
    mutationFn: (variables?: DeleteAiObservabilityNotificationChannelMutationVariables) => fetcher<DeleteAiObservabilityNotificationChannelMutation, DeleteAiObservabilityNotificationChannelMutationVariables>(DeleteAiObservabilityNotificationChannelDocument, variables)(),
    ...options
  }
    )};

export const TestAiObservabilityNotificationChannelDocument = new TypedDocumentString(`
    mutation testAiObservabilityNotificationChannel($id: ID!) {
  testAiObservabilityNotificationChannel(id: $id)
}
    `);

export const useTestAiObservabilityNotificationChannelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<TestAiObservabilityNotificationChannelMutation, TError, TestAiObservabilityNotificationChannelMutationVariables, TContext>) => {
    
    return useMutation<TestAiObservabilityNotificationChannelMutation, TError, TestAiObservabilityNotificationChannelMutationVariables, TContext>(
      {
    mutationKey: ['testAiObservabilityNotificationChannel'],
    mutationFn: (variables?: TestAiObservabilityNotificationChannelMutationVariables) => fetcher<TestAiObservabilityNotificationChannelMutation, TestAiObservabilityNotificationChannelMutationVariables>(TestAiObservabilityNotificationChannelDocument, variables)(),
    ...options
  }
    )};

export const AiObservabilitySessionsDocument = new TypedDocumentString(`
    query aiObservabilitySessions($workspaceId: ID!) {
  aiObservabilitySessions(workspaceId: $workspaceId) {
    createdDate
    id
    lastModifiedDate
    name
    projectId
    traceCount
    userId
    version
    workspaceId
  }
}
    `);

export const useAiObservabilitySessionsQuery = <
      TData = AiObservabilitySessionsQuery,
      TError = unknown
    >(
      variables: AiObservabilitySessionsQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilitySessionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilitySessionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilitySessionsQuery, TError, TData>(
      {
    queryKey: ['aiObservabilitySessions', variables],
    queryFn: fetcher<AiObservabilitySessionsQuery, AiObservabilitySessionsQueryVariables>(AiObservabilitySessionsDocument, variables),
    ...options
  }
    )};

export const AiObservabilitySessionDocument = new TypedDocumentString(`
    query aiObservabilitySession($id: ID!) {
  aiObservabilitySession(id: $id) {
    createdDate
    id
    lastModifiedDate
    name
    projectId
    traces {
      createdDate
      id
      name
      source
      status
      totalCost
      totalInputTokens
      totalLatencyMs
      totalOutputTokens
      userId
    }
    userId
    version
    workspaceId
  }
}
    `);

export const useAiObservabilitySessionQuery = <
      TData = AiObservabilitySessionQuery,
      TError = unknown
    >(
      variables: AiObservabilitySessionQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilitySessionQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilitySessionQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilitySessionQuery, TError, TData>(
      {
    queryKey: ['aiObservabilitySession', variables],
    queryFn: fetcher<AiObservabilitySessionQuery, AiObservabilitySessionQueryVariables>(AiObservabilitySessionDocument, variables),
    ...options
  }
    )};

export const AiObservabilityTracesDocument = new TypedDocumentString(`
    query aiObservabilityTraces($endDate: Long!, $model: String, $source: AiObservabilityTraceSource, $startDate: Long!, $status: AiObservabilityTraceStatus, $tagId: ID, $userId: String, $workspaceId: ID!) {
  aiObservabilityTraces(
    endDate: $endDate
    model: $model
    source: $source
    startDate: $startDate
    status: $status
    tagId: $tagId
    userId: $userId
    workspaceId: $workspaceId
  ) {
    createdDate
    id
    input
    lastModifiedDate
    metadata
    name
    output
    projectId
    sessionId
    source
    status
    totalCost
    totalInputTokens
    totalLatencyMs
    totalOutputTokens
    userId
    version
    workspaceId
  }
}
    `);

export const useAiObservabilityTracesQuery = <
      TData = AiObservabilityTracesQuery,
      TError = unknown
    >(
      variables: AiObservabilityTracesQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityTracesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityTracesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityTracesQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityTraces', variables],
    queryFn: fetcher<AiObservabilityTracesQuery, AiObservabilityTracesQueryVariables>(AiObservabilityTracesDocument, variables),
    ...options
  }
    )};

export const AiObservabilityTraceDocument = new TypedDocumentString(`
    query aiObservabilityTrace($id: ID!) {
  aiObservabilityTrace(id: $id) {
    createdDate
    id
    input
    lastModifiedDate
    metadata
    name
    output
    projectId
    sessionId
    source
    spans {
      cost
      createdDate
      endTime
      id
      input
      inputTokens
      latencyMs
      level
      metadata
      model
      name
      output
      outputTokens
      parentSpanId
      provider
      startTime
      status
      traceId
      type
      version
    }
    status
    tagIds
    totalCost
    totalInputTokens
    totalLatencyMs
    totalOutputTokens
    userId
    version
    workspaceId
  }
}
    `);

export const useAiObservabilityTraceQuery = <
      TData = AiObservabilityTraceQuery,
      TError = unknown
    >(
      variables: AiObservabilityTraceQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityTraceQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityTraceQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityTraceQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityTrace', variables],
    queryFn: fetcher<AiObservabilityTraceQuery, AiObservabilityTraceQueryVariables>(AiObservabilityTraceDocument, variables),
    ...options
  }
    )};

export const SetAiObservabilityTraceTagsDocument = new TypedDocumentString(`
    mutation setAiObservabilityTraceTags($traceId: ID!, $tagIds: [ID!]!) {
  setAiObservabilityTraceTags(traceId: $traceId, tagIds: $tagIds) {
    id
    tagIds
  }
}
    `);

export const useSetAiObservabilityTraceTagsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetAiObservabilityTraceTagsMutation, TError, SetAiObservabilityTraceTagsMutationVariables, TContext>) => {
    
    return useMutation<SetAiObservabilityTraceTagsMutation, TError, SetAiObservabilityTraceTagsMutationVariables, TContext>(
      {
    mutationKey: ['setAiObservabilityTraceTags'],
    mutationFn: (variables?: SetAiObservabilityTraceTagsMutationVariables) => fetcher<SetAiObservabilityTraceTagsMutation, SetAiObservabilityTraceTagsMutationVariables>(SetAiObservabilityTraceTagsDocument, variables)(),
    ...options
  }
    )};

export const AiObservabilityWebhookDeliveriesDocument = new TypedDocumentString(`
    query aiObservabilityWebhookDeliveries($subscriptionId: ID!) {
  aiObservabilityWebhookDeliveries(subscriptionId: $subscriptionId) {
    attemptCount
    createdDate
    deliveredDate
    errorMessage
    eventType
    httpStatus
    id
    status
    subscriptionId
  }
}
    `);

export const useAiObservabilityWebhookDeliveriesQuery = <
      TData = AiObservabilityWebhookDeliveriesQuery,
      TError = unknown
    >(
      variables: AiObservabilityWebhookDeliveriesQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityWebhookDeliveriesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityWebhookDeliveriesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityWebhookDeliveriesQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityWebhookDeliveries', variables],
    queryFn: fetcher<AiObservabilityWebhookDeliveriesQuery, AiObservabilityWebhookDeliveriesQueryVariables>(AiObservabilityWebhookDeliveriesDocument, variables),
    ...options
  }
    )};

export const AiObservabilityWebhookSubscriptionsDocument = new TypedDocumentString(`
    query aiObservabilityWebhookSubscriptions($workspaceId: ID!) {
  aiObservabilityWebhookSubscriptions(workspaceId: $workspaceId) {
    createdDate
    enabled
    events
    id
    lastModifiedDate
    lastTriggeredDate
    name
    projectId
    url
    version
    workspaceId
  }
}
    `);

export const useAiObservabilityWebhookSubscriptionsQuery = <
      TData = AiObservabilityWebhookSubscriptionsQuery,
      TError = unknown
    >(
      variables: AiObservabilityWebhookSubscriptionsQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityWebhookSubscriptionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityWebhookSubscriptionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityWebhookSubscriptionsQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityWebhookSubscriptions', variables],
    queryFn: fetcher<AiObservabilityWebhookSubscriptionsQuery, AiObservabilityWebhookSubscriptionsQueryVariables>(AiObservabilityWebhookSubscriptionsDocument, variables),
    ...options
  }
    )};

export const AiObservabilityWebhookSubscriptionDocument = new TypedDocumentString(`
    query aiObservabilityWebhookSubscription($id: ID!) {
  aiObservabilityWebhookSubscription(id: $id) {
    createdDate
    enabled
    events
    id
    lastModifiedDate
    lastTriggeredDate
    name
    projectId
    url
    version
    workspaceId
  }
}
    `);

export const useAiObservabilityWebhookSubscriptionQuery = <
      TData = AiObservabilityWebhookSubscriptionQuery,
      TError = unknown
    >(
      variables: AiObservabilityWebhookSubscriptionQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityWebhookSubscriptionQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityWebhookSubscriptionQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityWebhookSubscriptionQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityWebhookSubscription', variables],
    queryFn: fetcher<AiObservabilityWebhookSubscriptionQuery, AiObservabilityWebhookSubscriptionQueryVariables>(AiObservabilityWebhookSubscriptionDocument, variables),
    ...options
  }
    )};

export const CreateAiObservabilityWebhookSubscriptionDocument = new TypedDocumentString(`
    mutation createAiObservabilityWebhookSubscription($workspaceId: ID!, $projectId: ID, $name: String!, $url: String!, $secret: String, $events: String!, $enabled: Boolean!) {
  createAiObservabilityWebhookSubscription(
    workspaceId: $workspaceId
    projectId: $projectId
    name: $name
    url: $url
    secret: $secret
    events: $events
    enabled: $enabled
  ) {
    createdDate
    enabled
    events
    id
    name
    url
    version
    workspaceId
  }
}
    `);

export const useCreateAiObservabilityWebhookSubscriptionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiObservabilityWebhookSubscriptionMutation, TError, CreateAiObservabilityWebhookSubscriptionMutationVariables, TContext>) => {
    
    return useMutation<CreateAiObservabilityWebhookSubscriptionMutation, TError, CreateAiObservabilityWebhookSubscriptionMutationVariables, TContext>(
      {
    mutationKey: ['createAiObservabilityWebhookSubscription'],
    mutationFn: (variables?: CreateAiObservabilityWebhookSubscriptionMutationVariables) => fetcher<CreateAiObservabilityWebhookSubscriptionMutation, CreateAiObservabilityWebhookSubscriptionMutationVariables>(CreateAiObservabilityWebhookSubscriptionDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiObservabilityWebhookSubscriptionDocument = new TypedDocumentString(`
    mutation updateAiObservabilityWebhookSubscription($id: ID!, $name: String!, $url: String!, $secret: String, $events: String!, $enabled: Boolean!) {
  updateAiObservabilityWebhookSubscription(
    id: $id
    name: $name
    url: $url
    secret: $secret
    events: $events
    enabled: $enabled
  ) {
    createdDate
    enabled
    events
    id
    name
    url
    version
    workspaceId
  }
}
    `);

export const useUpdateAiObservabilityWebhookSubscriptionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiObservabilityWebhookSubscriptionMutation, TError, UpdateAiObservabilityWebhookSubscriptionMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiObservabilityWebhookSubscriptionMutation, TError, UpdateAiObservabilityWebhookSubscriptionMutationVariables, TContext>(
      {
    mutationKey: ['updateAiObservabilityWebhookSubscription'],
    mutationFn: (variables?: UpdateAiObservabilityWebhookSubscriptionMutationVariables) => fetcher<UpdateAiObservabilityWebhookSubscriptionMutation, UpdateAiObservabilityWebhookSubscriptionMutationVariables>(UpdateAiObservabilityWebhookSubscriptionDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiObservabilityWebhookSubscriptionDocument = new TypedDocumentString(`
    mutation deleteAiObservabilityWebhookSubscription($id: ID!) {
  deleteAiObservabilityWebhookSubscription(id: $id)
}
    `);

export const useDeleteAiObservabilityWebhookSubscriptionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiObservabilityWebhookSubscriptionMutation, TError, DeleteAiObservabilityWebhookSubscriptionMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiObservabilityWebhookSubscriptionMutation, TError, DeleteAiObservabilityWebhookSubscriptionMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiObservabilityWebhookSubscription'],
    mutationFn: (variables?: DeleteAiObservabilityWebhookSubscriptionMutationVariables) => fetcher<DeleteAiObservabilityWebhookSubscriptionMutation, DeleteAiObservabilityWebhookSubscriptionMutationVariables>(DeleteAiObservabilityWebhookSubscriptionDocument, variables)(),
    ...options
  }
    )};

export const TestAiObservabilityWebhookSubscriptionDocument = new TypedDocumentString(`
    mutation testAiObservabilityWebhookSubscription($id: ID!) {
  testAiObservabilityWebhookSubscription(id: $id)
}
    `);

export const useTestAiObservabilityWebhookSubscriptionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<TestAiObservabilityWebhookSubscriptionMutation, TError, TestAiObservabilityWebhookSubscriptionMutationVariables, TContext>) => {
    
    return useMutation<TestAiObservabilityWebhookSubscriptionMutation, TError, TestAiObservabilityWebhookSubscriptionMutationVariables, TContext>(
      {
    mutationKey: ['testAiObservabilityWebhookSubscription'],
    mutationFn: (variables?: TestAiObservabilityWebhookSubscriptionMutationVariables) => fetcher<TestAiObservabilityWebhookSubscriptionMutation, TestAiObservabilityWebhookSubscriptionMutationVariables>(TestAiObservabilityWebhookSubscriptionDocument, variables)(),
    ...options
  }
    )};

export const AiPromptsDocument = new TypedDocumentString(`
    query aiPrompts($workspaceId: ID!) {
  aiPrompts(workspaceId: $workspaceId) {
    createdDate
    description
    id
    lastModifiedDate
    name
    projectId
    version
    versions {
      active
      commitMessage
      content
      createdBy
      createdDate
      environment
      id
      metrics {
        avgCostUsd
        avgLatencyMs
        errorRate
        invocationCount
      }
      promptId
      type
      variables
      versionNumber
    }
    workspaceId
  }
}
    `);

export const useAiPromptsQuery = <
      TData = AiPromptsQuery,
      TError = unknown
    >(
      variables: AiPromptsQueryVariables,
      options?: Omit<UseQueryOptions<AiPromptsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiPromptsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiPromptsQuery, TError, TData>(
      {
    queryKey: ['aiPrompts', variables],
    queryFn: fetcher<AiPromptsQuery, AiPromptsQueryVariables>(AiPromptsDocument, variables),
    ...options
  }
    )};

export const AiPromptDocument = new TypedDocumentString(`
    query aiPrompt($id: ID!) {
  aiPrompt(id: $id) {
    createdDate
    description
    id
    lastModifiedDate
    name
    projectId
    version
    versions {
      active
      commitMessage
      content
      createdBy
      createdDate
      environment
      id
      metrics {
        avgCostUsd
        avgLatencyMs
        errorRate
        invocationCount
      }
      promptId
      type
      variables
      versionNumber
    }
    workspaceId
  }
}
    `);

export const useAiPromptQuery = <
      TData = AiPromptQuery,
      TError = unknown
    >(
      variables: AiPromptQueryVariables,
      options?: Omit<UseQueryOptions<AiPromptQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiPromptQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiPromptQuery, TError, TData>(
      {
    queryKey: ['aiPrompt', variables],
    queryFn: fetcher<AiPromptQuery, AiPromptQueryVariables>(AiPromptDocument, variables),
    ...options
  }
    )};

export const CreateAiPromptDocument = new TypedDocumentString(`
    mutation createAiPrompt($input: CreateAiPromptInput!) {
  createAiPrompt(input: $input) {
    createdDate
    description
    id
    lastModifiedDate
    name
    projectId
    version
    workspaceId
  }
}
    `);

export const useCreateAiPromptMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiPromptMutation, TError, CreateAiPromptMutationVariables, TContext>) => {
    
    return useMutation<CreateAiPromptMutation, TError, CreateAiPromptMutationVariables, TContext>(
      {
    mutationKey: ['createAiPrompt'],
    mutationFn: (variables?: CreateAiPromptMutationVariables) => fetcher<CreateAiPromptMutation, CreateAiPromptMutationVariables>(CreateAiPromptDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiPromptDocument = new TypedDocumentString(`
    mutation updateAiPrompt($id: ID!, $input: UpdateAiPromptInput!) {
  updateAiPrompt(id: $id, input: $input) {
    createdDate
    description
    id
    lastModifiedDate
    name
    projectId
    version
    workspaceId
  }
}
    `);

export const useUpdateAiPromptMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiPromptMutation, TError, UpdateAiPromptMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiPromptMutation, TError, UpdateAiPromptMutationVariables, TContext>(
      {
    mutationKey: ['updateAiPrompt'],
    mutationFn: (variables?: UpdateAiPromptMutationVariables) => fetcher<UpdateAiPromptMutation, UpdateAiPromptMutationVariables>(UpdateAiPromptDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiPromptDocument = new TypedDocumentString(`
    mutation deleteAiPrompt($id: ID!) {
  deleteAiPrompt(id: $id)
}
    `);

export const useDeleteAiPromptMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiPromptMutation, TError, DeleteAiPromptMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiPromptMutation, TError, DeleteAiPromptMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiPrompt'],
    mutationFn: (variables?: DeleteAiPromptMutationVariables) => fetcher<DeleteAiPromptMutation, DeleteAiPromptMutationVariables>(DeleteAiPromptDocument, variables)(),
    ...options
  }
    )};

export const CreateAiPromptVersionDocument = new TypedDocumentString(`
    mutation createAiPromptVersion($input: CreateAiPromptVersionInput!) {
  createAiPromptVersion(input: $input) {
    active
    commitMessage
    content
    createdBy
    createdDate
    environment
    id
    promptId
    type
    variables
    versionNumber
  }
}
    `);

export const useCreateAiPromptVersionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiPromptVersionMutation, TError, CreateAiPromptVersionMutationVariables, TContext>) => {
    
    return useMutation<CreateAiPromptVersionMutation, TError, CreateAiPromptVersionMutationVariables, TContext>(
      {
    mutationKey: ['createAiPromptVersion'],
    mutationFn: (variables?: CreateAiPromptVersionMutationVariables) => fetcher<CreateAiPromptVersionMutation, CreateAiPromptVersionMutationVariables>(CreateAiPromptVersionDocument, variables)(),
    ...options
  }
    )};

export const SetActiveAiPromptVersionDocument = new TypedDocumentString(`
    mutation setActiveAiPromptVersion($promptVersionId: ID!, $environment: String!) {
  setActiveAiPromptVersion(
    promptVersionId: $promptVersionId
    environment: $environment
  )
}
    `);

export const useSetActiveAiPromptVersionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetActiveAiPromptVersionMutation, TError, SetActiveAiPromptVersionMutationVariables, TContext>) => {
    
    return useMutation<SetActiveAiPromptVersionMutation, TError, SetActiveAiPromptVersionMutationVariables, TContext>(
      {
    mutationKey: ['setActiveAiPromptVersion'],
    mutationFn: (variables?: SetActiveAiPromptVersionMutationVariables) => fetcher<SetActiveAiPromptVersionMutation, SetActiveAiPromptVersionMutationVariables>(SetActiveAiPromptVersionDocument, variables)(),
    ...options
  }
    )};

export const WorkspaceAiGatewayModelsDocument = new TypedDocumentString(`
    query workspaceAiGatewayModels($workspaceId: ID!) {
  workspaceAiGatewayModels(workspaceId: $workspaceId) {
    alias
    capabilities
    contextWindow
    defaultRoutingPolicyId
    createdDate
    enabled
    id
    inputCostPerMTokens
    lastModifiedDate
    name
    outputCostPerMTokens
    providerId
    version
  }
}
    `);

export const useWorkspaceAiGatewayModelsQuery = <
      TData = WorkspaceAiGatewayModelsQuery,
      TError = unknown
    >(
      variables: WorkspaceAiGatewayModelsQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceAiGatewayModelsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceAiGatewayModelsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceAiGatewayModelsQuery, TError, TData>(
      {
    queryKey: ['workspaceAiGatewayModels', variables],
    queryFn: fetcher<WorkspaceAiGatewayModelsQuery, WorkspaceAiGatewayModelsQueryVariables>(WorkspaceAiGatewayModelsDocument, variables),
    ...options
  }
    )};

export const CreateWorkspaceAiGatewayModelDocument = new TypedDocumentString(`
    mutation createWorkspaceAiGatewayModel($input: CreateWorkspaceAiGatewayModelInput!) {
  createWorkspaceAiGatewayModel(input: $input) {
    alias
    capabilities
    contextWindow
    defaultRoutingPolicyId
    createdDate
    enabled
    id
    inputCostPerMTokens
    lastModifiedDate
    name
    outputCostPerMTokens
    providerId
    version
  }
}
    `);

export const useCreateWorkspaceAiGatewayModelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateWorkspaceAiGatewayModelMutation, TError, CreateWorkspaceAiGatewayModelMutationVariables, TContext>) => {
    
    return useMutation<CreateWorkspaceAiGatewayModelMutation, TError, CreateWorkspaceAiGatewayModelMutationVariables, TContext>(
      {
    mutationKey: ['createWorkspaceAiGatewayModel'],
    mutationFn: (variables?: CreateWorkspaceAiGatewayModelMutationVariables) => fetcher<CreateWorkspaceAiGatewayModelMutation, CreateWorkspaceAiGatewayModelMutationVariables>(CreateWorkspaceAiGatewayModelDocument, variables)(),
    ...options
  }
    )};

export const DeleteWorkspaceAiGatewayModelDocument = new TypedDocumentString(`
    mutation deleteWorkspaceAiGatewayModel($workspaceId: ID!, $modelId: ID!) {
  deleteWorkspaceAiGatewayModel(workspaceId: $workspaceId, modelId: $modelId)
}
    `);

export const useDeleteWorkspaceAiGatewayModelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteWorkspaceAiGatewayModelMutation, TError, DeleteWorkspaceAiGatewayModelMutationVariables, TContext>) => {
    
    return useMutation<DeleteWorkspaceAiGatewayModelMutation, TError, DeleteWorkspaceAiGatewayModelMutationVariables, TContext>(
      {
    mutationKey: ['deleteWorkspaceAiGatewayModel'],
    mutationFn: (variables?: DeleteWorkspaceAiGatewayModelMutationVariables) => fetcher<DeleteWorkspaceAiGatewayModelMutation, DeleteWorkspaceAiGatewayModelMutationVariables>(DeleteWorkspaceAiGatewayModelDocument, variables)(),
    ...options
  }
    )};

export const UpdateWorkspaceAiGatewayModelDocument = new TypedDocumentString(`
    mutation updateWorkspaceAiGatewayModel($id: ID!, $input: UpdateAiGatewayModelInput!) {
  updateWorkspaceAiGatewayModel(id: $id, input: $input) {
    alias
    capabilities
    contextWindow
    defaultRoutingPolicyId
    createdDate
    enabled
    id
    inputCostPerMTokens
    lastModifiedDate
    name
    outputCostPerMTokens
    providerId
    version
  }
}
    `);

export const useUpdateWorkspaceAiGatewayModelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateWorkspaceAiGatewayModelMutation, TError, UpdateWorkspaceAiGatewayModelMutationVariables, TContext>) => {
    
    return useMutation<UpdateWorkspaceAiGatewayModelMutation, TError, UpdateWorkspaceAiGatewayModelMutationVariables, TContext>(
      {
    mutationKey: ['updateWorkspaceAiGatewayModel'],
    mutationFn: (variables?: UpdateWorkspaceAiGatewayModelMutationVariables) => fetcher<UpdateWorkspaceAiGatewayModelMutation, UpdateWorkspaceAiGatewayModelMutationVariables>(UpdateWorkspaceAiGatewayModelDocument, variables)(),
    ...options
  }
    )};

export const WorkspaceAiGatewayProvidersDocument = new TypedDocumentString(`
    query workspaceAiGatewayProviders($workspaceId: ID!) {
  workspaceAiGatewayProviders(workspaceId: $workspaceId) {
    baseUrl
    config
    createdBy
    createdDate
    enabled
    id
    lastModifiedBy
    lastModifiedDate
    name
    type
    version
  }
}
    `);

export const useWorkspaceAiGatewayProvidersQuery = <
      TData = WorkspaceAiGatewayProvidersQuery,
      TError = unknown
    >(
      variables: WorkspaceAiGatewayProvidersQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceAiGatewayProvidersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceAiGatewayProvidersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceAiGatewayProvidersQuery, TError, TData>(
      {
    queryKey: ['workspaceAiGatewayProviders', variables],
    queryFn: fetcher<WorkspaceAiGatewayProvidersQuery, WorkspaceAiGatewayProvidersQueryVariables>(WorkspaceAiGatewayProvidersDocument, variables),
    ...options
  }
    )};

export const CreateWorkspaceAiGatewayProviderDocument = new TypedDocumentString(`
    mutation createWorkspaceAiGatewayProvider($input: CreateWorkspaceAiGatewayProviderInput!) {
  createWorkspaceAiGatewayProvider(input: $input) {
    baseUrl
    config
    createdBy
    createdDate
    enabled
    id
    lastModifiedBy
    lastModifiedDate
    name
    type
    version
  }
}
    `);

export const useCreateWorkspaceAiGatewayProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateWorkspaceAiGatewayProviderMutation, TError, CreateWorkspaceAiGatewayProviderMutationVariables, TContext>) => {
    
    return useMutation<CreateWorkspaceAiGatewayProviderMutation, TError, CreateWorkspaceAiGatewayProviderMutationVariables, TContext>(
      {
    mutationKey: ['createWorkspaceAiGatewayProvider'],
    mutationFn: (variables?: CreateWorkspaceAiGatewayProviderMutationVariables) => fetcher<CreateWorkspaceAiGatewayProviderMutation, CreateWorkspaceAiGatewayProviderMutationVariables>(CreateWorkspaceAiGatewayProviderDocument, variables)(),
    ...options
  }
    )};

export const DeleteWorkspaceAiGatewayProviderDocument = new TypedDocumentString(`
    mutation deleteWorkspaceAiGatewayProvider($workspaceId: ID!, $providerId: ID!) {
  deleteWorkspaceAiGatewayProvider(
    workspaceId: $workspaceId
    providerId: $providerId
  )
}
    `);

export const useDeleteWorkspaceAiGatewayProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteWorkspaceAiGatewayProviderMutation, TError, DeleteWorkspaceAiGatewayProviderMutationVariables, TContext>) => {
    
    return useMutation<DeleteWorkspaceAiGatewayProviderMutation, TError, DeleteWorkspaceAiGatewayProviderMutationVariables, TContext>(
      {
    mutationKey: ['deleteWorkspaceAiGatewayProvider'],
    mutationFn: (variables?: DeleteWorkspaceAiGatewayProviderMutationVariables) => fetcher<DeleteWorkspaceAiGatewayProviderMutation, DeleteWorkspaceAiGatewayProviderMutationVariables>(DeleteWorkspaceAiGatewayProviderDocument, variables)(),
    ...options
  }
    )};

export const UpdateWorkspaceAiGatewayProviderDocument = new TypedDocumentString(`
    mutation updateWorkspaceAiGatewayProvider($workspaceId: ID!, $id: ID!, $input: UpdateAiGatewayProviderInput!) {
  updateWorkspaceAiGatewayProvider(
    workspaceId: $workspaceId
    id: $id
    input: $input
  ) {
    baseUrl
    config
    createdBy
    createdDate
    enabled
    id
    lastModifiedBy
    lastModifiedDate
    name
    type
    version
  }
}
    `);

export const useUpdateWorkspaceAiGatewayProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateWorkspaceAiGatewayProviderMutation, TError, UpdateWorkspaceAiGatewayProviderMutationVariables, TContext>) => {
    
    return useMutation<UpdateWorkspaceAiGatewayProviderMutation, TError, UpdateWorkspaceAiGatewayProviderMutationVariables, TContext>(
      {
    mutationKey: ['updateWorkspaceAiGatewayProvider'],
    mutationFn: (variables?: UpdateWorkspaceAiGatewayProviderMutationVariables) => fetcher<UpdateWorkspaceAiGatewayProviderMutation, UpdateWorkspaceAiGatewayProviderMutationVariables>(UpdateWorkspaceAiGatewayProviderDocument, variables)(),
    ...options
  }
    )};

export const TestWorkspaceAiGatewayProviderConnectionDocument = new TypedDocumentString(`
    mutation testWorkspaceAiGatewayProviderConnection($workspaceId: ID!, $providerId: ID!) {
  testWorkspaceAiGatewayProviderConnection(
    workspaceId: $workspaceId
    providerId: $providerId
  ) {
    errorMessage
    latencyMs
    ok
  }
}
    `);

export const useTestWorkspaceAiGatewayProviderConnectionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<TestWorkspaceAiGatewayProviderConnectionMutation, TError, TestWorkspaceAiGatewayProviderConnectionMutationVariables, TContext>) => {
    
    return useMutation<TestWorkspaceAiGatewayProviderConnectionMutation, TError, TestWorkspaceAiGatewayProviderConnectionMutationVariables, TContext>(
      {
    mutationKey: ['testWorkspaceAiGatewayProviderConnection'],
    mutationFn: (variables?: TestWorkspaceAiGatewayProviderConnectionMutationVariables) => fetcher<TestWorkspaceAiGatewayProviderConnectionMutation, TestWorkspaceAiGatewayProviderConnectionMutationVariables>(TestWorkspaceAiGatewayProviderConnectionDocument, variables)(),
    ...options
  }
    )};

export const WorkspaceAiGatewayRequestLogsDocument = new TypedDocumentString(`
    query workspaceAiGatewayRequestLogs($endDate: Long!, $propertyKey: String, $propertyValue: String, $startDate: Long!, $workspaceId: ID!) {
  workspaceAiGatewayRequestLogs(
    endDate: $endDate
    propertyKey: $propertyKey
    propertyValue: $propertyValue
    startDate: $startDate
    workspaceId: $workspaceId
  ) {
    apiKeyId
    cacheHit
    cost
    createdDate
    errorMessage
    id
    inputTokens
    latencyMs
    outputTokens
    requestId
    requestedModel
    routedModel
    routedProvider
    routingPolicyId
    routingStrategy
    status
  }
}
    `);

export const useWorkspaceAiGatewayRequestLogsQuery = <
      TData = WorkspaceAiGatewayRequestLogsQuery,
      TError = unknown
    >(
      variables: WorkspaceAiGatewayRequestLogsQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceAiGatewayRequestLogsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceAiGatewayRequestLogsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceAiGatewayRequestLogsQuery, TError, TData>(
      {
    queryKey: ['workspaceAiGatewayRequestLogs', variables],
    queryFn: fetcher<WorkspaceAiGatewayRequestLogsQuery, WorkspaceAiGatewayRequestLogsQueryVariables>(WorkspaceAiGatewayRequestLogsDocument, variables),
    ...options
  }
    )};

export const WorkspaceAiGatewayRoutingPoliciesDocument = new TypedDocumentString(`
    query workspaceAiGatewayRoutingPolicies($workspaceId: ID!) {
  workspaceAiGatewayRoutingPolicies(workspaceId: $workspaceId) {
    config
    createdDate
    deployments {
      enabled
      id
      maxRpm
      maxTpm
      modelId
      priorityOrder
      routingPolicyId
      weight
    }
    enabled
    fallbackModel
    id
    lastModifiedDate
    name
    strategy
    version
  }
}
    `);

export const useWorkspaceAiGatewayRoutingPoliciesQuery = <
      TData = WorkspaceAiGatewayRoutingPoliciesQuery,
      TError = unknown
    >(
      variables: WorkspaceAiGatewayRoutingPoliciesQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceAiGatewayRoutingPoliciesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceAiGatewayRoutingPoliciesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceAiGatewayRoutingPoliciesQuery, TError, TData>(
      {
    queryKey: ['workspaceAiGatewayRoutingPolicies', variables],
    queryFn: fetcher<WorkspaceAiGatewayRoutingPoliciesQuery, WorkspaceAiGatewayRoutingPoliciesQueryVariables>(WorkspaceAiGatewayRoutingPoliciesDocument, variables),
    ...options
  }
    )};

export const CreateWorkspaceAiGatewayRoutingPolicyDocument = new TypedDocumentString(`
    mutation createWorkspaceAiGatewayRoutingPolicy($input: CreateWorkspaceAiGatewayRoutingPolicyInput!) {
  createWorkspaceAiGatewayRoutingPolicy(input: $input) {
    config
    createdDate
    enabled
    fallbackModel
    id
    lastModifiedDate
    name
    strategy
    version
  }
}
    `);

export const useCreateWorkspaceAiGatewayRoutingPolicyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateWorkspaceAiGatewayRoutingPolicyMutation, TError, CreateWorkspaceAiGatewayRoutingPolicyMutationVariables, TContext>) => {
    
    return useMutation<CreateWorkspaceAiGatewayRoutingPolicyMutation, TError, CreateWorkspaceAiGatewayRoutingPolicyMutationVariables, TContext>(
      {
    mutationKey: ['createWorkspaceAiGatewayRoutingPolicy'],
    mutationFn: (variables?: CreateWorkspaceAiGatewayRoutingPolicyMutationVariables) => fetcher<CreateWorkspaceAiGatewayRoutingPolicyMutation, CreateWorkspaceAiGatewayRoutingPolicyMutationVariables>(CreateWorkspaceAiGatewayRoutingPolicyDocument, variables)(),
    ...options
  }
    )};

export const DeleteWorkspaceAiGatewayRoutingPolicyDocument = new TypedDocumentString(`
    mutation deleteWorkspaceAiGatewayRoutingPolicy($workspaceId: ID!, $routingPolicyId: ID!) {
  deleteWorkspaceAiGatewayRoutingPolicy(
    workspaceId: $workspaceId
    routingPolicyId: $routingPolicyId
  )
}
    `);

export const useDeleteWorkspaceAiGatewayRoutingPolicyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteWorkspaceAiGatewayRoutingPolicyMutation, TError, DeleteWorkspaceAiGatewayRoutingPolicyMutationVariables, TContext>) => {
    
    return useMutation<DeleteWorkspaceAiGatewayRoutingPolicyMutation, TError, DeleteWorkspaceAiGatewayRoutingPolicyMutationVariables, TContext>(
      {
    mutationKey: ['deleteWorkspaceAiGatewayRoutingPolicy'],
    mutationFn: (variables?: DeleteWorkspaceAiGatewayRoutingPolicyMutationVariables) => fetcher<DeleteWorkspaceAiGatewayRoutingPolicyMutation, DeleteWorkspaceAiGatewayRoutingPolicyMutationVariables>(DeleteWorkspaceAiGatewayRoutingPolicyDocument, variables)(),
    ...options
  }
    )};

export const UpdateWorkspaceAiGatewayRoutingPolicyDocument = new TypedDocumentString(`
    mutation updateWorkspaceAiGatewayRoutingPolicy($workspaceId: ID!, $id: ID!, $input: UpdateAiGatewayRoutingPolicyInput!) {
  updateWorkspaceAiGatewayRoutingPolicy(
    workspaceId: $workspaceId
    id: $id
    input: $input
  ) {
    config
    createdDate
    enabled
    fallbackModel
    id
    lastModifiedDate
    name
    strategy
    version
  }
}
    `);

export const useUpdateWorkspaceAiGatewayRoutingPolicyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateWorkspaceAiGatewayRoutingPolicyMutation, TError, UpdateWorkspaceAiGatewayRoutingPolicyMutationVariables, TContext>) => {
    
    return useMutation<UpdateWorkspaceAiGatewayRoutingPolicyMutation, TError, UpdateWorkspaceAiGatewayRoutingPolicyMutationVariables, TContext>(
      {
    mutationKey: ['updateWorkspaceAiGatewayRoutingPolicy'],
    mutationFn: (variables?: UpdateWorkspaceAiGatewayRoutingPolicyMutationVariables) => fetcher<UpdateWorkspaceAiGatewayRoutingPolicyMutation, UpdateWorkspaceAiGatewayRoutingPolicyMutationVariables>(UpdateWorkspaceAiGatewayRoutingPolicyDocument, variables)(),
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

export const AddProjectUserDocument = new TypedDocumentString(`
    mutation AddProjectUser($projectId: ID!, $userId: ID!, $role: ProjectRole!) {
  addProjectUser(projectId: $projectId, userId: $userId, role: $role) {
    id
    projectId
    userId
    projectRole
    user {
      email
      firstName
      lastName
    }
  }
}
    `);

export const useAddProjectUserMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<AddProjectUserMutation, TError, AddProjectUserMutationVariables, TContext>) => {
    
    return useMutation<AddProjectUserMutation, TError, AddProjectUserMutationVariables, TContext>(
      {
    mutationKey: ['AddProjectUser'],
    mutationFn: (variables?: AddProjectUserMutationVariables) => fetcher<AddProjectUserMutation, AddProjectUserMutationVariables>(AddProjectUserDocument, variables)(),
    ...options
  }
    )};

export const AddWorkspaceUserDocument = new TypedDocumentString(`
    mutation AddWorkspaceUser($workspaceId: ID!, $userId: ID!, $role: WorkspaceRole!) {
  addWorkspaceUser(workspaceId: $workspaceId, userId: $userId, role: $role) {
    id
    workspaceId
    userId
    workspaceRole
    user {
      email
      firstName
      lastName
    }
  }
}
    `);

export const useAddWorkspaceUserMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<AddWorkspaceUserMutation, TError, AddWorkspaceUserMutationVariables, TContext>) => {
    
    return useMutation<AddWorkspaceUserMutation, TError, AddWorkspaceUserMutationVariables, TContext>(
      {
    mutationKey: ['AddWorkspaceUser'],
    mutationFn: (variables?: AddWorkspaceUserMutationVariables) => fetcher<AddWorkspaceUserMutation, AddWorkspaceUserMutationVariables>(AddWorkspaceUserDocument, variables)(),
    ...options
  }
    )};

export const AffectedWorkflowsDocument = new TypedDocumentString(`
    query affectedWorkflows($workspaceId: ID!, $userLogin: String!) {
  affectedWorkflows(workspaceId: $workspaceId, userLogin: $userLogin) {
    workflowId
    workflowName
    connectionIds
  }
}
    `);

export const useAffectedWorkflowsQuery = <
      TData = AffectedWorkflowsQuery,
      TError = unknown
    >(
      variables: AffectedWorkflowsQueryVariables,
      options?: Omit<UseQueryOptions<AffectedWorkflowsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AffectedWorkflowsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AffectedWorkflowsQuery, TError, TData>(
      {
    queryKey: ['affectedWorkflows', variables],
    queryFn: fetcher<AffectedWorkflowsQuery, AffectedWorkflowsQueryVariables>(AffectedWorkflowsDocument, variables),
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

export const CreateOrganizationConnectionDocument = new TypedDocumentString(`
    mutation createOrganizationConnection($input: CreateOrganizationConnectionInput!) {
  createOrganizationConnection(input: $input)
}
    `);

export const useCreateOrganizationConnectionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateOrganizationConnectionMutation, TError, CreateOrganizationConnectionMutationVariables, TContext>) => {
    
    return useMutation<CreateOrganizationConnectionMutation, TError, CreateOrganizationConnectionMutationVariables, TContext>(
      {
    mutationKey: ['createOrganizationConnection'],
    mutationFn: (variables?: CreateOrganizationConnectionMutationVariables) => fetcher<CreateOrganizationConnectionMutation, CreateOrganizationConnectionMutationVariables>(CreateOrganizationConnectionDocument, variables)(),
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

export const DeleteOrganizationConnectionDocument = new TypedDocumentString(`
    mutation deleteOrganizationConnection($connectionId: ID!) {
  deleteOrganizationConnection(connectionId: $connectionId)
}
    `);

export const useDeleteOrganizationConnectionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteOrganizationConnectionMutation, TError, DeleteOrganizationConnectionMutationVariables, TContext>) => {
    
    return useMutation<DeleteOrganizationConnectionMutation, TError, DeleteOrganizationConnectionMutationVariables, TContext>(
      {
    mutationKey: ['deleteOrganizationConnection'],
    mutationFn: (variables?: DeleteOrganizationConnectionMutationVariables) => fetcher<DeleteOrganizationConnectionMutation, DeleteOrganizationConnectionMutationVariables>(DeleteOrganizationConnectionDocument, variables)(),
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

export const DemoteConnectionToPrivateDocument = new TypedDocumentString(`
    mutation demoteConnectionToPrivate($workspaceId: ID!, $connectionId: ID!) {
  demoteConnectionToPrivate(
    workspaceId: $workspaceId
    connectionId: $connectionId
  )
}
    `);

export const useDemoteConnectionToPrivateMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DemoteConnectionToPrivateMutation, TError, DemoteConnectionToPrivateMutationVariables, TContext>) => {
    
    return useMutation<DemoteConnectionToPrivateMutation, TError, DemoteConnectionToPrivateMutationVariables, TContext>(
      {
    mutationKey: ['demoteConnectionToPrivate'],
    mutationFn: (variables?: DemoteConnectionToPrivateMutationVariables) => fetcher<DemoteConnectionToPrivateMutation, DemoteConnectionToPrivateMutationVariables>(DemoteConnectionToPrivateDocument, variables)(),
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

export const MyProjectScopesDocument = new TypedDocumentString(`
    query MyProjectScopes($projectId: ID!) {
  myProjectScopes(projectId: $projectId)
}
    `);

export const useMyProjectScopesQuery = <
      TData = MyProjectScopesQuery,
      TError = unknown
    >(
      variables: MyProjectScopesQueryVariables,
      options?: Omit<UseQueryOptions<MyProjectScopesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<MyProjectScopesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<MyProjectScopesQuery, TError, TData>(
      {
    queryKey: ['MyProjectScopes', variables],
    queryFn: fetcher<MyProjectScopesQuery, MyProjectScopesQueryVariables>(MyProjectScopesDocument, variables),
    ...options
  }
    )};

export const MyWorkspaceRoleDocument = new TypedDocumentString(`
    query MyWorkspaceRole($workspaceId: ID!) {
  myWorkspaceRole(workspaceId: $workspaceId)
}
    `);

export const useMyWorkspaceRoleQuery = <
      TData = MyWorkspaceRoleQuery,
      TError = unknown
    >(
      variables: MyWorkspaceRoleQueryVariables,
      options?: Omit<UseQueryOptions<MyWorkspaceRoleQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<MyWorkspaceRoleQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<MyWorkspaceRoleQuery, TError, TData>(
      {
    queryKey: ['MyWorkspaceRole', variables],
    queryFn: fetcher<MyWorkspaceRoleQuery, MyWorkspaceRoleQueryVariables>(MyWorkspaceRoleDocument, variables),
    ...options
  }
    )};

export const OrganizationConnectionsDocument = new TypedDocumentString(`
    query organizationConnections($environmentId: ID) {
  organizationConnections(environmentId: $environmentId) {
    id
    name
    componentName
    environmentId
    visibility
    createdBy
    createdDate
    lastModifiedDate
  }
}
    `);

export const useOrganizationConnectionsQuery = <
      TData = OrganizationConnectionsQuery,
      TError = unknown
    >(
      variables?: OrganizationConnectionsQueryVariables,
      options?: Omit<UseQueryOptions<OrganizationConnectionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<OrganizationConnectionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<OrganizationConnectionsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['organizationConnections'] : ['organizationConnections', variables],
    queryFn: fetcher<OrganizationConnectionsQuery, OrganizationConnectionsQueryVariables>(OrganizationConnectionsDocument, variables),
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

export const ProjectUsersDocument = new TypedDocumentString(`
    query ProjectUsers($projectId: ID!) {
  projectUsers(projectId: $projectId) {
    id
    projectId
    userId
    projectRole
    user {
      email
      firstName
      lastName
    }
    createdDate
  }
}
    `);

export const useProjectUsersQuery = <
      TData = ProjectUsersQuery,
      TError = unknown
    >(
      variables: ProjectUsersQueryVariables,
      options?: Omit<UseQueryOptions<ProjectUsersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ProjectUsersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ProjectUsersQuery, TError, TData>(
      {
    queryKey: ['ProjectUsers', variables],
    queryFn: fetcher<ProjectUsersQuery, ProjectUsersQueryVariables>(ProjectUsersDocument, variables),
    ...options
  }
    )};

export const PromoteAllPrivateConnectionsToWorkspaceDocument = new TypedDocumentString(`
    mutation PromoteAllPrivateConnectionsToWorkspace($workspaceId: ID!) {
  promoteAllPrivateConnectionsToWorkspace(workspaceId: $workspaceId) {
    attempted
    promoted
    skipped
    failed
    failures {
      connectionId
      errorCode
      message
    }
  }
}
    `);

export const usePromoteAllPrivateConnectionsToWorkspaceMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<PromoteAllPrivateConnectionsToWorkspaceMutation, TError, PromoteAllPrivateConnectionsToWorkspaceMutationVariables, TContext>) => {
    
    return useMutation<PromoteAllPrivateConnectionsToWorkspaceMutation, TError, PromoteAllPrivateConnectionsToWorkspaceMutationVariables, TContext>(
      {
    mutationKey: ['PromoteAllPrivateConnectionsToWorkspace'],
    mutationFn: (variables?: PromoteAllPrivateConnectionsToWorkspaceMutationVariables) => fetcher<PromoteAllPrivateConnectionsToWorkspaceMutation, PromoteAllPrivateConnectionsToWorkspaceMutationVariables>(PromoteAllPrivateConnectionsToWorkspaceDocument, variables)(),
    ...options
  }
    )};

export const PromoteConnectionToWorkspaceDocument = new TypedDocumentString(`
    mutation promoteConnectionToWorkspace($workspaceId: ID!, $connectionId: ID!) {
  promoteConnectionToWorkspace(
    workspaceId: $workspaceId
    connectionId: $connectionId
  )
}
    `);

export const usePromoteConnectionToWorkspaceMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<PromoteConnectionToWorkspaceMutation, TError, PromoteConnectionToWorkspaceMutationVariables, TContext>) => {
    
    return useMutation<PromoteConnectionToWorkspaceMutation, TError, PromoteConnectionToWorkspaceMutationVariables, TContext>(
      {
    mutationKey: ['promoteConnectionToWorkspace'],
    mutationFn: (variables?: PromoteConnectionToWorkspaceMutationVariables) => fetcher<PromoteConnectionToWorkspaceMutation, PromoteConnectionToWorkspaceMutationVariables>(PromoteConnectionToWorkspaceDocument, variables)(),
    ...options
  }
    )};

export const ReassignAllConnectionsDocument = new TypedDocumentString(`
    mutation reassignAllConnections($workspaceId: ID!, $userLogin: String!, $newOwnerLogin: String!) {
  reassignAllConnections(
    workspaceId: $workspaceId
    userLogin: $userLogin
    newOwnerLogin: $newOwnerLogin
  )
}
    `);

export const useReassignAllConnectionsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ReassignAllConnectionsMutation, TError, ReassignAllConnectionsMutationVariables, TContext>) => {
    
    return useMutation<ReassignAllConnectionsMutation, TError, ReassignAllConnectionsMutationVariables, TContext>(
      {
    mutationKey: ['reassignAllConnections'],
    mutationFn: (variables?: ReassignAllConnectionsMutationVariables) => fetcher<ReassignAllConnectionsMutation, ReassignAllConnectionsMutationVariables>(ReassignAllConnectionsDocument, variables)(),
    ...options
  }
    )};

export const RemoveProjectUserDocument = new TypedDocumentString(`
    mutation RemoveProjectUser($projectId: ID!, $userId: ID!) {
  removeProjectUser(projectId: $projectId, userId: $userId)
}
    `);

export const useRemoveProjectUserMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RemoveProjectUserMutation, TError, RemoveProjectUserMutationVariables, TContext>) => {
    
    return useMutation<RemoveProjectUserMutation, TError, RemoveProjectUserMutationVariables, TContext>(
      {
    mutationKey: ['RemoveProjectUser'],
    mutationFn: (variables?: RemoveProjectUserMutationVariables) => fetcher<RemoveProjectUserMutation, RemoveProjectUserMutationVariables>(RemoveProjectUserDocument, variables)(),
    ...options
  }
    )};

export const RemoveWorkspaceUserDocument = new TypedDocumentString(`
    mutation RemoveWorkspaceUser($workspaceId: ID!, $userId: ID!) {
  removeWorkspaceUser(workspaceId: $workspaceId, userId: $userId)
}
    `);

export const useRemoveWorkspaceUserMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RemoveWorkspaceUserMutation, TError, RemoveWorkspaceUserMutationVariables, TContext>) => {
    
    return useMutation<RemoveWorkspaceUserMutation, TError, RemoveWorkspaceUserMutationVariables, TContext>(
      {
    mutationKey: ['RemoveWorkspaceUser'],
    mutationFn: (variables?: RemoveWorkspaceUserMutationVariables) => fetcher<RemoveWorkspaceUserMutation, RemoveWorkspaceUserMutationVariables>(RemoveWorkspaceUserDocument, variables)(),
    ...options
  }
    )};

export const RevokeConnectionFromProjectDocument = new TypedDocumentString(`
    mutation revokeConnectionFromProject($workspaceId: ID!, $connectionId: ID!, $projectId: ID!) {
  revokeConnectionFromProject(
    workspaceId: $workspaceId
    connectionId: $connectionId
    projectId: $projectId
  )
}
    `);

export const useRevokeConnectionFromProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RevokeConnectionFromProjectMutation, TError, RevokeConnectionFromProjectMutationVariables, TContext>) => {
    
    return useMutation<RevokeConnectionFromProjectMutation, TError, RevokeConnectionFromProjectMutationVariables, TContext>(
      {
    mutationKey: ['revokeConnectionFromProject'],
    mutationFn: (variables?: RevokeConnectionFromProjectMutationVariables) => fetcher<RevokeConnectionFromProjectMutation, RevokeConnectionFromProjectMutationVariables>(RevokeConnectionFromProjectDocument, variables)(),
    ...options
  }
    )};

export const SetConnectionProjectsDocument = new TypedDocumentString(`
    mutation SetConnectionProjects($workspaceId: ID!, $connectionId: ID!, $projectIds: [ID!]!) {
  setConnectionProjects(
    workspaceId: $workspaceId
    connectionId: $connectionId
    projectIds: $projectIds
  )
}
    `);

export const useSetConnectionProjectsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetConnectionProjectsMutation, TError, SetConnectionProjectsMutationVariables, TContext>) => {
    
    return useMutation<SetConnectionProjectsMutation, TError, SetConnectionProjectsMutationVariables, TContext>(
      {
    mutationKey: ['SetConnectionProjects'],
    mutationFn: (variables?: SetConnectionProjectsMutationVariables) => fetcher<SetConnectionProjectsMutation, SetConnectionProjectsMutationVariables>(SetConnectionProjectsDocument, variables)(),
    ...options
  }
    )};

export const ShareConnectionToProjectDocument = new TypedDocumentString(`
    mutation shareConnectionToProject($workspaceId: ID!, $connectionId: ID!, $projectId: ID!) {
  shareConnectionToProject(
    workspaceId: $workspaceId
    connectionId: $connectionId
    projectId: $projectId
  )
}
    `);

export const useShareConnectionToProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ShareConnectionToProjectMutation, TError, ShareConnectionToProjectMutationVariables, TContext>) => {
    
    return useMutation<ShareConnectionToProjectMutation, TError, ShareConnectionToProjectMutationVariables, TContext>(
      {
    mutationKey: ['shareConnectionToProject'],
    mutationFn: (variables?: ShareConnectionToProjectMutationVariables) => fetcher<ShareConnectionToProjectMutation, ShareConnectionToProjectMutationVariables>(ShareConnectionToProjectDocument, variables)(),
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

export const UnresolvedConnectionsDocument = new TypedDocumentString(`
    query unresolvedConnections($workspaceId: ID!, $userLogin: String!) {
  unresolvedConnections(workspaceId: $workspaceId, userLogin: $userLogin) {
    connectionId
    connectionName
    visibility
    environmentId
    dependentWorkflowCount
  }
}
    `);

export const useUnresolvedConnectionsQuery = <
      TData = UnresolvedConnectionsQuery,
      TError = unknown
    >(
      variables: UnresolvedConnectionsQueryVariables,
      options?: Omit<UseQueryOptions<UnresolvedConnectionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<UnresolvedConnectionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<UnresolvedConnectionsQuery, TError, TData>(
      {
    queryKey: ['unresolvedConnections', variables],
    queryFn: fetcher<UnresolvedConnectionsQuery, UnresolvedConnectionsQueryVariables>(UnresolvedConnectionsDocument, variables),
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

export const UpdateOrganizationConnectionDocument = new TypedDocumentString(`
    mutation updateOrganizationConnection($connectionId: ID!, $name: String!, $tagIds: [ID!], $version: Int!) {
  updateOrganizationConnection(
    connectionId: $connectionId
    name: $name
    tagIds: $tagIds
    version: $version
  )
}
    `);

export const useUpdateOrganizationConnectionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateOrganizationConnectionMutation, TError, UpdateOrganizationConnectionMutationVariables, TContext>) => {
    
    return useMutation<UpdateOrganizationConnectionMutation, TError, UpdateOrganizationConnectionMutationVariables, TContext>(
      {
    mutationKey: ['updateOrganizationConnection'],
    mutationFn: (variables?: UpdateOrganizationConnectionMutationVariables) => fetcher<UpdateOrganizationConnectionMutation, UpdateOrganizationConnectionMutationVariables>(UpdateOrganizationConnectionDocument, variables)(),
    ...options
  }
    )};

export const UpdateProjectUserRoleDocument = new TypedDocumentString(`
    mutation UpdateProjectUserRole($projectId: ID!, $userId: ID!, $role: ProjectRole!) {
  updateProjectUserRole(projectId: $projectId, userId: $userId, role: $role) {
    id
    projectRole
  }
}
    `);

export const useUpdateProjectUserRoleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateProjectUserRoleMutation, TError, UpdateProjectUserRoleMutationVariables, TContext>) => {
    
    return useMutation<UpdateProjectUserRoleMutation, TError, UpdateProjectUserRoleMutationVariables, TContext>(
      {
    mutationKey: ['UpdateProjectUserRole'],
    mutationFn: (variables?: UpdateProjectUserRoleMutationVariables) => fetcher<UpdateProjectUserRoleMutation, UpdateProjectUserRoleMutationVariables>(UpdateProjectUserRoleDocument, variables)(),
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

export const UpdateWorkspaceUserRoleDocument = new TypedDocumentString(`
    mutation UpdateWorkspaceUserRole($workspaceId: ID!, $userId: ID!, $role: WorkspaceRole!) {
  updateWorkspaceUserRole(workspaceId: $workspaceId, userId: $userId, role: $role) {
    id
    workspaceRole
  }
}
    `);

export const useUpdateWorkspaceUserRoleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateWorkspaceUserRoleMutation, TError, UpdateWorkspaceUserRoleMutationVariables, TContext>) => {
    
    return useMutation<UpdateWorkspaceUserRoleMutation, TError, UpdateWorkspaceUserRoleMutationVariables, TContext>(
      {
    mutationKey: ['UpdateWorkspaceUserRole'],
    mutationFn: (variables?: UpdateWorkspaceUserRoleMutationVariables) => fetcher<UpdateWorkspaceUserRoleMutation, UpdateWorkspaceUserRoleMutationVariables>(UpdateWorkspaceUserRoleDocument, variables)(),
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

export const WorkspaceChatWorkflowsDocument = new TypedDocumentString(`
    query workspaceChatWorkflows($workspaceId: ID!, $environmentId: ID!) {
  workspaceChatWorkflows(workspaceId: $workspaceId, environmentId: $environmentId) {
    projectDeploymentId
    projectId
    projectName
    workflowExecutionId
    workflowLabel
  }
}
    `);

export const useWorkspaceChatWorkflowsQuery = <
      TData = WorkspaceChatWorkflowsQuery,
      TError = unknown
    >(
      variables: WorkspaceChatWorkflowsQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceChatWorkflowsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceChatWorkflowsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceChatWorkflowsQuery, TError, TData>(
      {
    queryKey: ['workspaceChatWorkflows', variables],
    queryFn: fetcher<WorkspaceChatWorkflowsQuery, WorkspaceChatWorkflowsQueryVariables>(WorkspaceChatWorkflowsDocument, variables),
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

export const WorkspaceUsersDocument = new TypedDocumentString(`
    query WorkspaceUsers($workspaceId: ID!) {
  workspaceUsers(workspaceId: $workspaceId) {
    id
    workspaceId
    userId
    workspaceRole
    user {
      email
      firstName
      lastName
    }
    createdDate
  }
}
    `);

export const useWorkspaceUsersQuery = <
      TData = WorkspaceUsersQuery,
      TError = unknown
    >(
      variables: WorkspaceUsersQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceUsersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceUsersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceUsersQuery, TError, TData>(
      {
    queryKey: ['WorkspaceUsers', variables],
    queryFn: fetcher<WorkspaceUsersQuery, WorkspaceUsersQueryVariables>(WorkspaceUsersDocument, variables),
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
