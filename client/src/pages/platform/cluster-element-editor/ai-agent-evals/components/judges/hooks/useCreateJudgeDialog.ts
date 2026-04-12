import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {AiAgentJudgeType} from '@/shared/middleware/graphql';
import {useGetWorkspaceConnectionsQuery} from '@/shared/queries/automation/connections.queries';
import {
    useGetClusterElementDefinitionQuery,
    useGetRootComponentClusterElementDefinitions,
} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useMemo, useState} from 'react';

import type {StringProperty} from '@/shared/middleware/platform/configuration';

interface JudgeEditDataI {
    configuration: Record<string, unknown>;
    id: string;
    name: string;
    type: AiAgentJudgeType;
}

interface UseCreateJudgeFormProps {
    editData?: JudgeEditDataI;
    onClose: () => void;
    onCreate: (name: string, type: AiAgentJudgeType, configuration: Record<string, unknown>) => void;
    onUpdate?: (id: string, name?: string, configuration?: Record<string, unknown>) => void;
}

export default function useCreateJudgeDialog({editData, onClose, onCreate, onUpdate}: UseCreateJudgeFormProps) {
    const editConfig = (editData?.configuration ?? {}) as Record<string, unknown>;

    const [algorithm, setAlgorithm] = useState<string>(String(editConfig.algorithm ?? 'COSINE'));
    const [caseSensitive, setCaseSensitive] = useState(editConfig.caseSensitive !== false);
    const [connectionId, setConnectionId] = useState<string>(String(editConfig.connectionId ?? ''));
    const [expectedOutput, setExpectedOutput] = useState(String(editConfig.expectedOutput ?? ''));
    const [expectedValue, setExpectedValue] = useState(String(editConfig.expectedValue ?? ''));
    const [jsonSchema, setJsonSchema] = useState(String(editConfig.schema ?? ''));
    const [jsonSchemaError, setJsonSchemaError] = useState('');
    const [judgeType, setJudgeType] = useState<AiAgentJudgeType>(editData?.type ?? AiAgentJudgeType.LlmRule);
    const [maxLength, setMaxLength] = useState(String(editConfig.maxLength ?? ''));
    const [minLength, setMinLength] = useState(String(editConfig.minLength ?? ''));
    const [model, setModel] = useState(String(editConfig.model ?? ''));
    const [mustNotContain, setMustNotContain] = useState(editConfig.mustNotContain === true);
    const [mustNotMatch, setMustNotMatch] = useState(editConfig.mustNotMatch === true);
    const [name, setName] = useState(editData?.name ?? '');
    const [provider, setProvider] = useState(String(editConfig.componentName ?? ''));
    const [regexPattern, setRegexPattern] = useState(String(editConfig.pattern ?? ''));
    const [rule, setRule] = useState(String(editConfig.rule ?? ''));
    const [searchText, setSearchText] = useState(String(editConfig.text ?? ''));
    const [threshold, setThreshold] = useState(String(editConfig.threshold ?? '0.8'));
    const [toolComparison, setToolComparison] = useState(String(editConfig.comparison ?? 'AT_LEAST'));
    const [toolCount, setToolCount] = useState(Number(editConfig.count ?? 1));
    const [toolName, setToolName] = useState(String(editConfig.toolName ?? ''));
    const [toolPosition, setToolPosition] = useState(String(editConfig.position ?? 'ANYWHERE'));

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data: modelProviders = []} = useGetRootComponentClusterElementDefinitions(
        {clusterElementType: 'model', rootComponentName: 'aiAgent', rootComponentVersion: 1},
        judgeType === AiAgentJudgeType.LlmRule
    );

    const {data: allConnections = []} = useGetWorkspaceConnectionsQuery(
        {componentName: provider || undefined, id: currentWorkspaceId!},
        judgeType === AiAgentJudgeType.LlmRule && currentWorkspaceId != null && !!provider
    );

    const selectedProviderVersion = useMemo(
        () => modelProviders.find((modelProvider) => modelProvider.componentName === provider)?.componentVersion,
        [modelProviders, provider]
    );

    const {data: clusterElementDefinition} = useGetClusterElementDefinitionQuery(
        {clusterElementName: 'model', componentName: provider, componentVersion: selectedProviderVersion ?? 1},
        judgeType === AiAgentJudgeType.LlmRule && !!provider && selectedProviderVersion != null
    );

    const isLlmRule = judgeType === AiAgentJudgeType.LlmRule;
    const isEditing = !!editData;

    const modelOptions = useMemo(() => {
        const modelProperty = clusterElementDefinition?.properties?.find((property) => property.name === 'model') as
            | StringProperty
            | undefined;

        return modelProperty?.options ?? [];
    }, [clusterElementDefinition]);

    const handleProviderChange = (selectedProvider: string) => {
        setProvider(selectedProvider);
        setConnectionId('');
        setModel('');
    };

    const buildConfiguration = (): Record<string, unknown> | null => {
        switch (judgeType) {
            case AiAgentJudgeType.LlmRule: {
                const llmConfig: Record<string, unknown> = {rule: rule.trim()};

                if (provider) {
                    llmConfig.componentName = provider;
                }

                if (selectedProviderVersion != null) {
                    llmConfig.componentVersion = selectedProviderVersion;
                }

                if (model.trim()) {
                    llmConfig.model = model.trim();
                }

                if (connectionId) {
                    llmConfig.connectionId = Number(connectionId);
                }

                return llmConfig;
            }

            case AiAgentJudgeType.ContainsText:
                return {mustNotContain, text: searchText.trim()};

            case AiAgentJudgeType.RegexMatch:
                return {mustNotMatch, pattern: regexPattern.trim()};

            case AiAgentJudgeType.ResponseLength: {
                const config: Record<string, unknown> = {};

                if (minLength.trim()) {
                    config.minLength = Number(minLength);
                }

                if (maxLength.trim()) {
                    config.maxLength = Number(maxLength);
                }

                return config;
            }

            case AiAgentJudgeType.JsonSchema: {
                const trimmedSchema = jsonSchema.trim();

                if (trimmedSchema) {
                    try {
                        JSON.parse(trimmedSchema);

                        setJsonSchemaError('');
                    } catch {
                        setJsonSchemaError('Invalid JSON schema');

                        return null;
                    }
                }

                return {schema: trimmedSchema};
            }

            case AiAgentJudgeType.Similarity:
                return {
                    algorithm,
                    expectedOutput: expectedOutput.trim(),
                    threshold: Number(threshold),
                };

            case AiAgentJudgeType.StringEquals:
                return {caseSensitive, expectedValue: expectedValue.trim()};

            case AiAgentJudgeType.ToolUsage:
                return {
                    comparison: toolComparison,
                    count: toolCount,
                    position: toolPosition,
                    toolName: toolName.trim(),
                };

            default:
                return {};
        }
    };

    const handleSubmit = () => {
        if (!name.trim()) {
            return;
        }

        const configuration = buildConfiguration();

        if (configuration === null) {
            return;
        }

        if (isEditing && onUpdate) {
            onUpdate(editData.id, name.trim(), configuration);
        } else {
            onCreate(name.trim(), judgeType, configuration);
        }

        onClose();
    };

    return {
        algorithm,
        allConnections,
        caseSensitive,
        connectionId,
        expectedOutput,
        expectedValue,
        handleProviderChange,
        handleSubmit,
        isEditing,
        isLlmRule,
        jsonSchema,
        jsonSchemaError,
        judgeType,
        maxLength,
        minLength,
        model,
        modelOptions,
        modelProviders,
        mustNotContain,
        mustNotMatch,
        name,
        provider,
        regexPattern,
        rule,
        searchText,
        setAlgorithm,
        setCaseSensitive,
        setConnectionId,
        setExpectedOutput,
        setExpectedValue,
        setJsonSchema,
        setJsonSchemaError,
        setJudgeType,
        setMaxLength,
        setMinLength,
        setModel,
        setMustNotContain,
        setMustNotMatch,
        setName,
        setRegexPattern,
        setRule,
        setSearchText,
        setThreshold,
        setToolComparison,
        setToolCount,
        setToolName,
        setToolPosition,
        threshold,
        toolComparison,
        toolCount,
        toolName,
        toolPosition,
    };
}

export type {JudgeEditDataI};
