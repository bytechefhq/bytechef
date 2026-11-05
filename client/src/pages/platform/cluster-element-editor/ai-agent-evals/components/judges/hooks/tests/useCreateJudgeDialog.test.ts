import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {AiAgentJudgeType} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useCreateJudgeDialog, {JudgeEditDataI} from '../useCreateJudgeDialog';

const hoisted = vi.hoisted(() => ({
    useGetClusterElementDefinitionQuery: vi.fn(),
    useGetRootComponentClusterElementDefinitions: vi.fn(),
    useGetWorkspaceConnectionsQuery: vi.fn(),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    useGetWorkspaceConnectionsQuery: hoisted.useGetWorkspaceConnectionsQuery,
}));

vi.mock('@/shared/queries/platform/clusterElementDefinitions.queries', () => ({
    useGetClusterElementDefinitionQuery: hoisted.useGetClusterElementDefinitionQuery,
    useGetRootComponentClusterElementDefinitions: hoisted.useGetRootComponentClusterElementDefinitions,
}));

const modelProviders = [
    {componentName: 'openAi', componentVersion: 2},
    {componentName: 'anthropic', componentVersion: 1},
];

const connections = [{id: 11, name: 'OpenAI production'}];

const modelOptions = [
    {label: 'GPT-4o', value: 'gpt-4o'},
    {label: 'GPT-4o mini', value: 'gpt-4o-mini'},
];

const renderJudgeDialog = (editData?: JudgeEditDataI, withUpdate = true) => {
    const onClose = vi.fn();
    const onCreate = vi.fn();
    const onUpdate = vi.fn();

    const hook = renderHook(() =>
        useCreateJudgeDialog({editData, onClose, onCreate, onUpdate: withUpdate ? onUpdate : undefined})
    );

    return {...hook, onClose, onCreate, onUpdate};
};

const getLastEnabledArgument = (queryMock: ReturnType<typeof vi.fn>) => queryMock.mock.lastCall?.[1];

describe('useCreateJudgeDialog', () => {
    beforeEach(() => {
        useWorkspaceStore.setState({currentWorkspaceId: 1049});

        hoisted.useGetRootComponentClusterElementDefinitions.mockReset();
        hoisted.useGetRootComponentClusterElementDefinitions.mockReturnValue({data: modelProviders});
        hoisted.useGetWorkspaceConnectionsQuery.mockReset();
        hoisted.useGetWorkspaceConnectionsQuery.mockReturnValue({data: connections});
        hoisted.useGetClusterElementDefinitionQuery.mockReset();
        hoisted.useGetClusterElementDefinitionQuery.mockReturnValue({
            data: {
                properties: [
                    {name: 'temperature', type: 'NUMBER'},
                    {name: 'model', options: modelOptions, type: 'STRING'},
                ],
            },
        });
    });

    describe('initial state', () => {
        it('starts a new LLM rule judge with default settings', () => {
            const {result} = renderJudgeDialog();

            expect(result.current.isEditing).toBe(false);
            expect(result.current.isLlmRule).toBe(true);
            expect(result.current.judgeType).toBe(AiAgentJudgeType.LlmRule);
            expect(result.current.name).toBe('');
            expect(result.current.algorithm).toBe('COSINE');
            expect(result.current.caseSensitive).toBe(true);
            expect(result.current.mustNotContain).toBe(false);
            expect(result.current.mustNotMatch).toBe(false);
            expect(result.current.threshold).toBe('0.8');
            expect(result.current.toolComparison).toBe('AT_LEAST');
            expect(result.current.toolCount).toBe(1);
            expect(result.current.toolPosition).toBe('ANYWHERE');
            expect(result.current.modelProviders).toEqual(modelProviders);
        });

        it('loads the configuration of an edited judge', () => {
            const {result} = renderJudgeDialog({
                configuration: {
                    algorithm: 'LEVENSHTEIN',
                    caseSensitive: false,
                    comparison: 'EXACTLY',
                    count: 3,
                    expectedOutput: 'Hello',
                    mustNotContain: true,
                    position: 'FIRST',
                    threshold: 0.5,
                    toolName: 'search',
                },
                id: 'judge-1',
                name: 'Similarity judge',
                type: AiAgentJudgeType.Similarity,
            });

            expect(result.current.isEditing).toBe(true);
            expect(result.current.isLlmRule).toBe(false);
            expect(result.current.name).toBe('Similarity judge');
            expect(result.current.algorithm).toBe('LEVENSHTEIN');
            expect(result.current.caseSensitive).toBe(false);
            expect(result.current.expectedOutput).toBe('Hello');
            expect(result.current.mustNotContain).toBe(true);
            expect(result.current.threshold).toBe('0.5');
            expect(result.current.toolComparison).toBe('EXACTLY');
            expect(result.current.toolCount).toBe(3);
            expect(result.current.toolName).toBe('search');
            expect(result.current.toolPosition).toBe('FIRST');
        });
    });

    describe('model provider lookups', () => {
        it('loads model providers only for LLM rule judges', () => {
            const {result} = renderJudgeDialog();

            expect(hoisted.useGetRootComponentClusterElementDefinitions).toHaveBeenLastCalledWith(
                {clusterElementType: 'model', rootComponentName: 'aiAgent', rootComponentVersion: 1},
                true
            );

            act(() => result.current.setJudgeType(AiAgentJudgeType.ContainsText));

            expect(getLastEnabledArgument(hoisted.useGetRootComponentClusterElementDefinitions)).toBe(false);
            expect(result.current.isLlmRule).toBe(false);
        });

        it('waits for a provider before loading connections and the model definition', () => {
            renderJudgeDialog();

            expect(getLastEnabledArgument(hoisted.useGetWorkspaceConnectionsQuery)).toBe(false);
            expect(hoisted.useGetWorkspaceConnectionsQuery.mock.lastCall?.[0]).toEqual({
                componentName: undefined,
                id: 1049,
            });
            expect(getLastEnabledArgument(hoisted.useGetClusterElementDefinitionQuery)).toBe(false);
        });

        it('loads the connections and model options of the selected provider version', () => {
            const {result} = renderJudgeDialog();

            act(() => result.current.handleProviderChange('openAi'));

            expect(hoisted.useGetWorkspaceConnectionsQuery).toHaveBeenLastCalledWith(
                {componentName: 'openAi', id: 1049},
                true
            );
            expect(hoisted.useGetClusterElementDefinitionQuery).toHaveBeenLastCalledWith(
                {clusterElementName: 'model', componentName: 'openAi', componentVersion: 2},
                true
            );
            expect(result.current.allConnections).toEqual(connections);
            expect(result.current.modelOptions).toEqual(modelOptions);
        });

        it('does not load the model definition of a provider missing from the provider list', () => {
            const {result} = renderJudgeDialog();

            act(() => result.current.handleProviderChange('mistral'));

            expect(hoisted.useGetClusterElementDefinitionQuery).toHaveBeenLastCalledWith(
                {clusterElementName: 'model', componentName: 'mistral', componentVersion: 1},
                false
            );
        });

        it('does not load connections without a current workspace', () => {
            useWorkspaceStore.setState({currentWorkspaceId: undefined as unknown as number});

            const {result} = renderJudgeDialog();

            act(() => result.current.handleProviderChange('openAi'));

            expect(getLastEnabledArgument(hoisted.useGetWorkspaceConnectionsQuery)).toBe(false);
        });

        it('defaults the model options and lists to empty', () => {
            hoisted.useGetRootComponentClusterElementDefinitions.mockReturnValue({data: undefined});
            hoisted.useGetWorkspaceConnectionsQuery.mockReturnValue({data: undefined});
            hoisted.useGetClusterElementDefinitionQuery.mockReturnValue({data: {properties: [{name: 'model'}]}});

            const {result} = renderJudgeDialog();

            expect(result.current.modelProviders).toEqual([]);
            expect(result.current.allConnections).toEqual([]);
            expect(result.current.modelOptions).toEqual([]);
        });

        it('clears the connection and model when the provider changes', () => {
            const {result} = renderJudgeDialog();

            act(() => {
                result.current.handleProviderChange('openAi');
                result.current.setConnectionId('11');
                result.current.setModel('gpt-4o');
            });

            act(() => result.current.handleProviderChange('anthropic'));

            expect(result.current.provider).toBe('anthropic');
            expect(result.current.connectionId).toBe('');
            expect(result.current.model).toBe('');
        });
    });

    describe('handleSubmit', () => {
        it('does not submit without a name', () => {
            const {onClose, onCreate, result} = renderJudgeDialog();

            act(() => result.current.setName('   '));
            act(() => result.current.handleSubmit());

            expect(onCreate).not.toHaveBeenCalled();
            expect(onClose).not.toHaveBeenCalled();
        });

        it('creates an LLM rule judge with the provider, version, model and connection', () => {
            const {onClose, onCreate, result} = renderJudgeDialog();

            act(() => {
                result.current.setName('  Polite  ');
                result.current.setRule('  Be polite  ');
                result.current.handleProviderChange('openAi');
            });

            act(() => {
                result.current.setModel(' gpt-4o ');
                result.current.setConnectionId('11');
            });

            act(() => result.current.handleSubmit());

            expect(onCreate).toHaveBeenCalledWith('Polite', AiAgentJudgeType.LlmRule, {
                componentName: 'openAi',
                componentVersion: 2,
                connectionId: 11,
                model: 'gpt-4o',
                rule: 'Be polite',
            });
            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('creates an LLM rule judge with only the rule when nothing else is chosen', () => {
            const {onCreate, result} = renderJudgeDialog();

            act(() => {
                result.current.setName('Polite');
                result.current.setRule('Be polite');
                result.current.setModel('   ');
            });

            act(() => result.current.handleSubmit());

            expect(onCreate).toHaveBeenCalledWith('Polite', AiAgentJudgeType.LlmRule, {rule: 'Be polite'});
        });

        it.each([
            [
                AiAgentJudgeType.ContainsText,
                (current: ReturnType<typeof useCreateJudgeDialog>) => {
                    current.setSearchText('  refund  ');
                    current.setMustNotContain(true);
                },
                {mustNotContain: true, text: 'refund'},
            ],
            [
                AiAgentJudgeType.RegexMatch,
                (current: ReturnType<typeof useCreateJudgeDialog>) => current.setRegexPattern(' ^\\d+$ '),
                {mustNotMatch: false, pattern: '^\\d+$'},
            ],
            [
                AiAgentJudgeType.ResponseLength,
                (current: ReturnType<typeof useCreateJudgeDialog>) => {
                    current.setMinLength('10');
                    current.setMaxLength('200');
                },
                {maxLength: 200, minLength: 10},
            ],
            [AiAgentJudgeType.ResponseLength, () => undefined, {}],
            [
                AiAgentJudgeType.Similarity,
                (current: ReturnType<typeof useCreateJudgeDialog>) => {
                    current.setExpectedOutput(' Hello ');
                    current.setThreshold('0.9');
                    current.setAlgorithm('JACCARD');
                },
                {algorithm: 'JACCARD', expectedOutput: 'Hello', threshold: 0.9},
            ],
            [
                AiAgentJudgeType.StringEquals,
                (current: ReturnType<typeof useCreateJudgeDialog>) => {
                    current.setExpectedValue(' yes ');
                    current.setCaseSensitive(false);
                },
                {caseSensitive: false, expectedValue: 'yes'},
            ],
            [
                AiAgentJudgeType.ToolUsage,
                (current: ReturnType<typeof useCreateJudgeDialog>) => {
                    current.setToolName(' search ');
                    current.setToolCount(2);
                    current.setToolComparison('AT_MOST');
                    current.setToolPosition('LAST');
                },
                {comparison: 'AT_MOST', count: 2, position: 'LAST', toolName: 'search'},
            ],
            [AiAgentJudgeType.JsonSchema, () => undefined, {schema: ''}],
        ])('builds the %s configuration', (judgeType, configure, expectedConfiguration) => {
            const {onCreate, result} = renderJudgeDialog();

            act(() => {
                result.current.setName('Judge');
                result.current.setJudgeType(judgeType);
            });

            act(() => configure(result.current));

            act(() => result.current.handleSubmit());

            expect(onCreate).toHaveBeenCalledWith('Judge', judgeType, expectedConfiguration);
        });

        it('rejects an invalid JSON schema and keeps the dialog open', () => {
            const {onClose, onCreate, result} = renderJudgeDialog();

            act(() => {
                result.current.setName('Schema');
                result.current.setJudgeType(AiAgentJudgeType.JsonSchema);
                result.current.setJsonSchema('{invalid');
            });

            act(() => result.current.handleSubmit());

            expect(result.current.jsonSchemaError).toBe('Invalid JSON schema');
            expect(onCreate).not.toHaveBeenCalled();
            expect(onClose).not.toHaveBeenCalled();
        });

        it('clears the JSON schema error once the schema is valid', () => {
            const {onCreate, result} = renderJudgeDialog();

            act(() => {
                result.current.setName('Schema');
                result.current.setJudgeType(AiAgentJudgeType.JsonSchema);
                result.current.setJsonSchema('{invalid');
            });

            act(() => result.current.handleSubmit());

            act(() => result.current.setJsonSchema(' {"type":"object"} '));

            act(() => result.current.handleSubmit());

            expect(result.current.jsonSchemaError).toBe('');
            expect(onCreate).toHaveBeenCalledWith('Schema', AiAgentJudgeType.JsonSchema, {schema: '{"type":"object"}'});
        });

        it('updates an edited judge', () => {
            const {onClose, onCreate, onUpdate, result} = renderJudgeDialog({
                configuration: {pattern: 'old'},
                id: 'judge-1',
                name: 'Regex',
                type: AiAgentJudgeType.RegexMatch,
            });

            act(() => {
                result.current.setName(' Regex v2 ');
                result.current.setRegexPattern('new');
                result.current.setMustNotMatch(true);
            });

            act(() => result.current.handleSubmit());

            expect(onUpdate).toHaveBeenCalledWith('judge-1', 'Regex v2', {mustNotMatch: true, pattern: 'new'});
            expect(onCreate).not.toHaveBeenCalled();
            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('creates the judge when editing without an update handler', () => {
            const {onCreate, result} = renderJudgeDialog(
                {configuration: {text: 'refund'}, id: 'judge-1', name: 'Contains', type: AiAgentJudgeType.ContainsText},
                false
            );

            act(() => result.current.handleSubmit());

            expect(onCreate).toHaveBeenCalledWith('Contains', AiAgentJudgeType.ContainsText, {
                mustNotContain: false,
                text: 'refund',
            });
        });
    });
});
