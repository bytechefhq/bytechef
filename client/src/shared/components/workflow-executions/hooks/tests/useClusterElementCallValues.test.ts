import {ClusterElementCallTaskExecutionI} from '@/shared/components/workflow-executions/util/toClusterElementCallTaskExecutions';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementCallValues from '../useClusterElementCallValues';

interface LogsQueryVariablesI {
    filter: {clusterElementCallId: string; searchText: string; taskExecutionId?: string} | null;
    jobId: string;
    page: number;
    size: number;
}

interface LogsQueryResultI {
    isLoading: boolean;
    messages?: string[];
}

const hoisted = vi.hoisted(() => ({
    editorResultsByCallId: {} as Record<string, LogsQueryResultI>,
    productionResultsByCallId: {} as Record<string, LogsQueryResultI>,
    useEditorJobFileLogsQuery: vi.fn(),
    useJobFileLogsQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useEditorJobFileLogsQuery: hoisted.useEditorJobFileLogsQuery,
    useJobFileLogsQuery: hoisted.useJobFileLogsQuery,
}));

const toContent = (messages?: string[]) => messages?.map((message) => ({message}));

const getQueryResult = (resultsByCallId: Record<string, LogsQueryResultI>, variables: LogsQueryVariablesI) =>
    resultsByCallId[variables.filter?.clusterElementCallId ?? ''] ?? {isLoading: false};

const createCallExecution = (overrides: Partial<ClusterElementCallTaskExecutionI> = {}) =>
    ({
        clusterElementCallId: 'call-1',
        parentTaskExecutionId: 'task-execution-1',
        ...overrides,
    }) as ClusterElementCallTaskExecutionI;

const toolCallMessage = (entry: object) => JSON.stringify(entry);

describe('useClusterElementCallValues', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.editorResultsByCallId = {};
        hoisted.productionResultsByCallId = {};

        hoisted.useJobFileLogsQuery.mockImplementation((variables: LogsQueryVariablesI) => {
            const queryResult = getQueryResult(hoisted.productionResultsByCallId, variables);

            return {
                data: queryResult.messages ? {jobFileLogs: {content: toContent(queryResult.messages)}} : undefined,
                isLoading: queryResult.isLoading,
            };
        });

        hoisted.useEditorJobFileLogsQuery.mockImplementation((variables: LogsQueryVariablesI) => {
            const queryResult = getQueryResult(hoisted.editorResultsByCallId, variables);

            return {
                data: queryResult.messages
                    ? {editorJobFileLogs: {content: toContent(queryResult.messages)}}
                    : undefined,
                isLoading: queryResult.isLoading,
            };
        });
    });

    describe('without a call execution', () => {
        it('returns no values and keeps every log query disabled', () => {
            const {result} = renderHook(() => useClusterElementCallValues({jobId: 'job-1'}));

            expect(result.current).toEqual({input: undefined, isLoading: false, output: undefined});

            for (const [, options] of hoisted.useJobFileLogsQuery.mock.calls) {
                expect(options).toEqual({enabled: false});
            }

            for (const [, options] of hoisted.useEditorJobFileLogsQuery.mock.calls) {
                expect(options).toEqual({enabled: false});
            }
        });
    });

    describe('with own values on the call execution', () => {
        it('returns the own input and output without querying the logs', () => {
            const callExecution = createCallExecution({input: {query: 'weather'}, output: {temperature: 21}});

            const {result} = renderHook(() => useClusterElementCallValues({callExecution, jobId: 'job-1'}));

            expect(result.current).toEqual({input: {query: 'weather'}, isLoading: false, output: {temperature: 21}});
            expect(hoisted.useJobFileLogsQuery).toHaveBeenCalledWith(expect.anything(), {enabled: false});
            expect(hoisted.useJobFileLogsQuery).not.toHaveBeenCalledWith(expect.anything(), {enabled: true});
        });

        it('treats an output alone as own values', () => {
            const callExecution = createCallExecution({output: 'done' as unknown as object});

            const {result} = renderHook(() => useClusterElementCallValues({callExecution, jobId: 'job-1'}));

            expect(result.current).toEqual({input: undefined, isLoading: false, output: 'done'});
            expect(hoisted.useJobFileLogsQuery).not.toHaveBeenCalledWith(expect.anything(), {enabled: true});
        });
    });

    describe('without a job id', () => {
        it('keeps the log queries disabled', () => {
            const callExecution = createCallExecution();

            const {result} = renderHook(() => useClusterElementCallValues({callExecution}));

            expect(result.current).toEqual({input: undefined, isLoading: false, output: undefined});
            expect(hoisted.useJobFileLogsQuery).not.toHaveBeenCalledWith(expect.anything(), {enabled: true});
        });
    });

    describe('reading the tool call log entries', () => {
        it('queries the production logs filtered by the call id and parent task execution', () => {
            hoisted.productionResultsByCallId['call-1'] = {isLoading: false, messages: []};

            renderHook(() => useClusterElementCallValues({callExecution: createCallExecution(), jobId: 'job-1'}));

            expect(hoisted.useJobFileLogsQuery).toHaveBeenCalledWith(
                {
                    filter: {
                        clusterElementCallId: 'call-1',
                        searchText: '"toolName":',
                        taskExecutionId: 'task-execution-1',
                    },
                    jobId: 'job-1',
                    page: 0,
                    size: 100,
                },
                {enabled: true}
            );
            expect(hoisted.useEditorJobFileLogsQuery).not.toHaveBeenCalledWith(expect.anything(), {enabled: true});
        });

        it('takes the last logged inputs and output and parses a JSON output', () => {
            hoisted.productionResultsByCallId['call-1'] = {
                isLoading: false,
                messages: [
                    toolCallMessage({inputs: {city: 'Paris'}, toolName: 'weather'}),
                    toolCallMessage({output: '{"temperature":18}', toolName: 'weather'}),
                    toolCallMessage({inputs: {city: 'Zagreb'}, toolName: 'weather'}),
                    toolCallMessage({output: '{"temperature":21}', toolName: 'weather'}),
                    toolCallMessage({inputs: null, output: null, toolName: 'weather'}),
                ],
            };

            const {result} = renderHook(() =>
                useClusterElementCallValues({callExecution: createCallExecution(), jobId: 'job-1'})
            );

            expect(result.current).toEqual({input: {city: 'Zagreb'}, isLoading: false, output: {temperature: 21}});
        });

        it('keeps a non-JSON string output and a non-string output as they are', () => {
            hoisted.productionResultsByCallId['call-1'] = {
                isLoading: false,
                messages: [toolCallMessage({output: 'plain text', toolName: 'weather'})],
            };

            const {rerender, result} = renderHook(() =>
                useClusterElementCallValues({callExecution: createCallExecution(), jobId: 'job-1'})
            );

            expect(result.current.output).toBe('plain text');

            hoisted.productionResultsByCallId['call-1'] = {
                isLoading: false,
                messages: [toolCallMessage({output: {temperature: 5}, toolName: 'weather'})],
            };

            rerender();

            expect(result.current.output).toEqual({temperature: 5});
        });

        it('ignores log lines that are not tool call entries', () => {
            hoisted.productionResultsByCallId['call-1'] = {
                isLoading: false,
                messages: [
                    toolCallMessage({inputs: {city: 'Zagreb'}, toolName: 'weather'}),
                    'not json "toolName":',
                    toolCallMessage({inputs: {city: 'Ignored'}, toolName: 42}),
                    toolCallMessage({toolName: 'weather'}),
                    'null',
                ],
            };

            const {result} = renderHook(() =>
                useClusterElementCallValues({callExecution: createCallExecution(), jobId: 'job-1'})
            );

            expect(result.current).toEqual({input: {city: 'Zagreb'}, isLoading: false, output: undefined});
        });

        it('reports loading while the logs are being fetched', () => {
            hoisted.productionResultsByCallId['call-1'] = {isLoading: true};

            const {result} = renderHook(() =>
                useClusterElementCallValues({callExecution: createCallExecution(), jobId: 'job-1'})
            );

            expect(result.current.isLoading).toBe(true);
        });

        it('reads the editor logs in the editor environment', () => {
            hoisted.productionResultsByCallId['call-1'] = {
                isLoading: false,
                messages: [toolCallMessage({inputs: {source: 'production'}, toolName: 'weather'})],
            };
            hoisted.editorResultsByCallId['call-1'] = {
                isLoading: false,
                messages: [toolCallMessage({inputs: {source: 'editor'}, toolName: 'weather'})],
            };

            const {result} = renderHook(() =>
                useClusterElementCallValues({
                    callExecution: createCallExecution(),
                    isEditorEnvironment: true,
                    jobId: 'job-1',
                })
            );

            expect(result.current.input).toEqual({source: 'editor'});
            expect(hoisted.useEditorJobFileLogsQuery).toHaveBeenCalledWith(expect.objectContaining({jobId: 'job-1'}), {
                enabled: true,
            });
            expect(hoisted.useJobFileLogsQuery).not.toHaveBeenCalledWith(expect.anything(), {enabled: true});
        });

        it('reports the editor query loading state in the editor environment', () => {
            hoisted.productionResultsByCallId['call-1'] = {isLoading: false};
            hoisted.editorResultsByCallId['call-1'] = {isLoading: true};

            const {result} = renderHook(() =>
                useClusterElementCallValues({
                    callExecution: createCallExecution(),
                    isEditorEnvironment: true,
                    jobId: 'job-1',
                })
            );

            expect(result.current.isLoading).toBe(true);
        });
    });

    describe('falling back to the subagent task call', () => {
        it('reads the task call entries when the call itself logged nothing', () => {
            hoisted.productionResultsByCallId['call-1'] = {isLoading: false, messages: []};
            hoisted.productionResultsByCallId['task-call-1'] = {
                isLoading: false,
                messages: [
                    toolCallMessage({inputs: {prompt: 'Summarize', subagentType: 'general'}, toolName: 'Task'}),
                    toolCallMessage({output: '"Summary"', toolName: 'Task'}),
                ],
            };

            const {result} = renderHook(() =>
                useClusterElementCallValues({
                    callExecution: createCallExecution({subagentTaskCallId: 'task-call-1'}),
                    jobId: 'job-1',
                })
            );

            expect(result.current).toEqual({input: {prompt: 'Summarize'}, isLoading: false, output: 'Summary'});
            expect(hoisted.useJobFileLogsQuery).toHaveBeenCalledWith(
                expect.objectContaining({
                    filter: {
                        clusterElementCallId: 'task-call-1',
                        searchText: '"toolName":',
                        taskExecutionId: 'task-execution-1',
                    },
                }),
                {enabled: true}
            );
        });

        it('returns task call inputs without a prompt unchanged', () => {
            hoisted.productionResultsByCallId['call-1'] = {isLoading: false, messages: []};
            hoisted.productionResultsByCallId['task-call-1'] = {
                isLoading: false,
                messages: [toolCallMessage({inputs: 'raw input', toolName: 'Task'})],
            };

            const {result} = renderHook(() =>
                useClusterElementCallValues({
                    callExecution: createCallExecution({subagentTaskCallId: 'task-call-1'}),
                    jobId: 'job-1',
                })
            );

            expect(result.current.input).toBe('raw input');
        });

        it('reports the task call loading state', () => {
            hoisted.productionResultsByCallId['call-1'] = {isLoading: false, messages: []};
            hoisted.productionResultsByCallId['task-call-1'] = {isLoading: true};

            const {result} = renderHook(() =>
                useClusterElementCallValues({
                    callExecution: createCallExecution({subagentTaskCallId: 'task-call-1'}),
                    jobId: 'job-1',
                })
            );

            expect(result.current).toEqual({input: undefined, isLoading: true, output: undefined});
        });

        it('does not fall back while the own entries are still loading', () => {
            hoisted.productionResultsByCallId['call-1'] = {isLoading: true};

            const {result} = renderHook(() =>
                useClusterElementCallValues({
                    callExecution: createCallExecution({subagentTaskCallId: 'task-call-1'}),
                    jobId: 'job-1',
                })
            );

            expect(result.current.isLoading).toBe(true);
            expect(hoisted.useJobFileLogsQuery).not.toHaveBeenCalledWith(
                expect.objectContaining({filter: expect.objectContaining({clusterElementCallId: 'task-call-1'})}),
                {enabled: true}
            );
        });

        it('does not fall back when the call logged its own entries', () => {
            hoisted.productionResultsByCallId['call-1'] = {
                isLoading: false,
                messages: [toolCallMessage({inputs: {own: true}, toolName: 'weather'})],
            };
            hoisted.productionResultsByCallId['task-call-1'] = {
                isLoading: false,
                messages: [toolCallMessage({inputs: {prompt: 'Task'}, toolName: 'Task'})],
            };

            const {result} = renderHook(() =>
                useClusterElementCallValues({
                    callExecution: createCallExecution({subagentTaskCallId: 'task-call-1'}),
                    jobId: 'job-1',
                })
            );

            expect(result.current.input).toEqual({own: true});
        });

        it('does not fall back without a subagent task call id', () => {
            hoisted.productionResultsByCallId['call-1'] = {isLoading: false, messages: []};

            const {result} = renderHook(() =>
                useClusterElementCallValues({callExecution: createCallExecution(), jobId: 'job-1'})
            );

            expect(result.current).toEqual({input: undefined, isLoading: false, output: undefined});

            const enabledCallIds = hoisted.useJobFileLogsQuery.mock.calls
                .filter(([, options]) => (options as {enabled: boolean}).enabled)
                .map(([variables]) => (variables as LogsQueryVariablesI).filter?.clusterElementCallId);

            expect(new Set(enabledCallIds)).toEqual(new Set(['call-1']));
        });
    });
});
