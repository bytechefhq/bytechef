import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    PlaygroundChatCompletionMutation,
    PlaygroundChatRole,
    usePlaygroundChatCompletionMutation,
    useWorkspaceAiGatewayModelsQuery,
    useWorkspaceAiGatewayProvidersQuery,
} from '@/shared/middleware/graphql';
import {getCookie} from '@/shared/util/cookie-utils';
import {ColumnsIcon, PlayIcon, TerminalIcon} from 'lucide-react';
import {useCallback, useMemo, useState} from 'react';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

import {parseStreamChunk, splitSseEvents} from '../../util/sse-stream-parser';
import PlaygroundMessageList, {type PlaygroundMessageI} from './PlaygroundMessageList';
import PlaygroundParametersPanel from './PlaygroundParametersPanel';
import PlaygroundResponsePanel from './PlaygroundResponsePanel';

interface PlaygroundStreamChunkI {
    content: string | null;
    cost: number | null;
    finished: boolean;
    inputTokens: number | null;
    latencyMs: number | null;
    outputTokens: number | null;
    traceId: number | null;
}

type PlaygroundModeType = 'chat' | 'text';

type PlaygroundResponseType = NonNullable<PlaygroundChatCompletionMutation['playgroundChatCompletion']>;

const AiGatewayPlayground = () => {
    const [compareMode, setCompareMode] = useState(false);
    const [maxTokens, setMaxTokens] = useState(2048);
    const [messages, setMessages] = useState<PlaygroundMessageI[]>([{content: '', role: 'USER'}]);
    const [mode, setMode] = useState<PlaygroundModeType>('text');
    const [responseLeft, setResponseLeft] = useState<PlaygroundResponseType | undefined>(undefined);
    const [responseRight, setResponseRight] = useState<PlaygroundResponseType | undefined>(undefined);
    const [selectedModelLeft, setSelectedModelLeft] = useState<string | undefined>(undefined);
    const [selectedModelRight, setSelectedModelRight] = useState<string | undefined>(undefined);
    const [streamEnabled, setStreamEnabled] = useState(false);
    const [streaming, setStreaming] = useState(false);
    const [temperature, setTemperature] = useState(1.0);
    const [textPrompt, setTextPrompt] = useState('');
    const [topP, setTopP] = useState(1.0);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data: modelsData, isLoading: modelsIsLoading} = useWorkspaceAiGatewayModelsQuery(
        {workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : ''},
        {enabled: currentWorkspaceId != null}
    );
    const {data: providersData} = useWorkspaceAiGatewayProvidersQuery(
        {workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : ''},
        {enabled: currentWorkspaceId != null}
    );

    const mutationLeft = usePlaygroundChatCompletionMutation({
        onSuccess: (data) => {
            if (data.playgroundChatCompletion) {
                setResponseLeft(data.playgroundChatCompletion);
            }
        },
    });
    const mutationRight = usePlaygroundChatCompletionMutation({
        onSuccess: (data) => {
            if (data.playgroundChatCompletion) {
                setResponseRight(data.playgroundChatCompletion);
            }
        },
    });

    const enabledModels = useMemo(
        () => (modelsData?.workspaceAiGatewayModels ?? []).filter((model) => model?.enabled),
        [modelsData]
    );

    const providerNameMap = useMemo(() => {
        const providerMap = new Map<string, string>();

        for (const provider of providersData?.workspaceAiGatewayProviders ?? []) {
            if (provider) {
                providerMap.set(provider.id, provider.name);
            }
        }

        return providerMap;
    }, [providersData]);

    const buildMessages = useCallback((): PlaygroundMessageI[] => {
        if (mode === 'text') {
            return [{content: textPrompt, role: 'USER'}];
        }

        return messages;
    }, [messages, mode, textPrompt]);

    const runStream = useCallback(
        async (model: string, inputMessages: Array<{content: string; role: PlaygroundChatRole}>) => {
            setResponseLeft({content: '', model} as PlaygroundResponseType);
            setStreaming(true);

            try {
                const response = await fetch('/api/internal/ai-gateway/playground/chat/completions/stream', {
                    body: JSON.stringify({
                        maxTokens,
                        messages: inputMessages,
                        model,
                        temperature,
                        topP,
                        workspaceId: currentWorkspaceId,
                    }),
                    credentials: 'include',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
                    },
                    method: 'POST',
                });

                if (!response.ok || !response.body) {
                    throw new Error(`Stream request failed: ${response.status}`);
                }

                const reader = response.body.getReader();
                const decoder = new TextDecoder();

                let buffer = '';
                let accumulatedContent = '';
                let parseFailures = 0;

                while (true) {
                    const {done, value} = await reader.read();

                    if (done) {
                        break;
                    }

                    buffer += decoder.decode(value, {stream: true});

                    const {events, remainder} = splitSseEvents(buffer);

                    buffer = remainder;

                    for (const {data: json, event: eventName} of events) {
                        if (eventName === 'error') {
                            const errorChunk = parseStreamChunk<{message?: string}>(json);

                            throw new Error(errorChunk?.message || 'Stream error from server');
                        }

                        const chunk = parseStreamChunk<PlaygroundStreamChunkI>(json);

                        if (chunk == null) {
                            parseFailures += 1;

                            continue;
                        }

                        if (chunk.finished) {
                            setResponseLeft({
                                completionTokens: chunk.outputTokens ?? null,
                                content: accumulatedContent,
                                cost: chunk.cost ?? null,
                                finishReason: null,
                                latencyMs: chunk.latencyMs ?? null,
                                model,
                                promptTokens: chunk.inputTokens ?? null,
                                totalTokens:
                                    chunk.inputTokens != null && chunk.outputTokens != null
                                        ? chunk.inputTokens + chunk.outputTokens
                                        : null,
                                traceId: chunk.traceId ?? null,
                            } as PlaygroundResponseType);
                        } else if (chunk.content) {
                            accumulatedContent += chunk.content;

                            setResponseLeft({
                                content: accumulatedContent,
                                model,
                            } as PlaygroundResponseType);
                        }
                    }
                }

                if (parseFailures > 0) {
                    toast.warning(
                        `${parseFailures} stream chunk${parseFailures === 1 ? '' : 's'} could not be parsed — response may be incomplete.`
                    );
                }
            } catch (error) {
                console.error('Playground streaming failed', error);

                const message = error instanceof Error ? error.message : 'Streaming request failed';

                setResponseLeft({
                    content: `Error: ${message}`,
                    model,
                } as PlaygroundResponseType);
            } finally {
                setStreaming(false);
            }
        },
        [currentWorkspaceId, maxTokens, temperature, topP]
    );

    const handleRun = useCallback(() => {
        if (!selectedModelLeft) {
            return;
        }

        const playgroundMessages = buildMessages();

        if (playgroundMessages.length === 0 || !playgroundMessages[0].content.trim()) {
            return;
        }

        const inputMessages = playgroundMessages.map((message) => ({
            content: message.content,
            role: message.role as PlaygroundChatRole,
        }));

        if (streamEnabled && !compareMode) {
            void runStream(selectedModelLeft, inputMessages);

            return;
        }

        setResponseLeft(undefined);

        mutationLeft.mutate({
            input: {
                maxTokens,
                messages: inputMessages,
                model: selectedModelLeft,
                temperature,
                topP,
            },
        });

        if (compareMode && selectedModelRight) {
            setResponseRight(undefined);

            mutationRight.mutate({
                input: {
                    maxTokens,
                    messages: inputMessages,
                    model: selectedModelRight,
                    temperature,
                    topP,
                },
            });
        }
    }, [
        buildMessages,
        compareMode,
        maxTokens,
        mutationLeft,
        mutationRight,
        runStream,
        selectedModelLeft,
        selectedModelRight,
        streamEnabled,
        temperature,
        topP,
    ]);

    const isRunning = mutationLeft.isPending || mutationRight.isPending || streaming;

    if (modelsIsLoading) {
        return <PageLoader loading={true} />;
    }

    if (enabledModels.length === 0) {
        return (
            <EmptyList
                icon={<TerminalIcon className="size-12 text-muted-foreground" />}
                message="Configure and enable models in the Models tab to use the Playground."
                title="No Models Available"
            />
        );
    }

    return (
        <div className="flex h-full w-full gap-4 px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="flex flex-1 flex-col">
                <div className="mb-4 flex items-center justify-between py-2">
                    <h2 className="text-lg font-semibold">Playground</h2>

                    <div className="flex items-center gap-2">
                        <div className="flex rounded-md border">
                            <button
                                className={twMerge(
                                    'px-3 py-1 text-sm',
                                    mode === 'text'
                                        ? 'bg-primary text-primary-foreground'
                                        : 'text-muted-foreground hover:text-foreground'
                                )}
                                onClick={() => setMode('text')}
                            >
                                Text
                            </button>

                            <button
                                className={twMerge(
                                    'px-3 py-1 text-sm',
                                    mode === 'chat'
                                        ? 'bg-primary text-primary-foreground'
                                        : 'text-muted-foreground hover:text-foreground'
                                )}
                                onClick={() => setMode('chat')}
                            >
                                Chat
                            </button>
                        </div>

                        <button
                            className={twMerge(
                                'flex items-center gap-1 rounded-md border px-3 py-1 text-sm',
                                compareMode
                                    ? 'border-primary bg-primary/10 text-primary'
                                    : 'text-muted-foreground hover:text-foreground'
                            )}
                            onClick={() => setCompareMode(!compareMode)}
                        >
                            <ColumnsIcon className="size-4" />
                            Compare
                        </button>
                    </div>
                </div>

                <div className={twMerge('mb-4 gap-4', compareMode ? 'grid grid-cols-2' : 'flex flex-col')}>
                    <fieldset className="space-y-2 border-0 p-0">
                        <legend className="text-sm font-medium">{compareMode ? 'Model A' : 'Model'}</legend>

                        <Select onValueChange={setSelectedModelLeft} value={selectedModelLeft}>
                            <SelectTrigger>
                                <SelectValue placeholder="Select a model" />
                            </SelectTrigger>

                            <SelectContent>
                                {enabledModels.map((model) =>
                                    model ? (
                                        <SelectItem key={model.id} value={model.name}>
                                            {model.name}

                                            {providerNameMap.get(model.providerId)
                                                ? ` (${providerNameMap.get(model.providerId)})`
                                                : ''}
                                        </SelectItem>
                                    ) : null
                                )}
                            </SelectContent>
                        </Select>
                    </fieldset>

                    {compareMode && (
                        <fieldset className="space-y-2 border-0 p-0">
                            <legend className="text-sm font-medium">Model B</legend>

                            <Select onValueChange={setSelectedModelRight} value={selectedModelRight}>
                                <SelectTrigger>
                                    <SelectValue placeholder="Select a model" />
                                </SelectTrigger>

                                <SelectContent>
                                    {enabledModels.map((model) =>
                                        model ? (
                                            <SelectItem key={model.id} value={model.name}>
                                                {model.name}

                                                {providerNameMap.get(model.providerId)
                                                    ? ` (${providerNameMap.get(model.providerId)})`
                                                    : ''}
                                            </SelectItem>
                                        ) : null
                                    )}
                                </SelectContent>
                            </Select>
                        </fieldset>
                    )}
                </div>

                <div className="mb-4">
                    {mode === 'text' ? (
                        <Textarea
                            className="min-h-[120px] resize-y"
                            onChange={(event) => setTextPrompt(event.target.value)}
                            placeholder="Enter your prompt..."
                            value={textPrompt}
                        />
                    ) : (
                        <PlaygroundMessageList messages={messages} onMessagesChange={setMessages} />
                    )}
                </div>

                <div className="mb-4 flex items-center gap-3">
                    <Button
                        disabled={!selectedModelLeft || isRunning}
                        icon={<PlayIcon className="size-4" />}
                        label={isRunning ? 'Running...' : 'Run'}
                        onClick={handleRun}
                    />

                    <label
                        className={twMerge(
                            'flex items-center gap-1.5 text-sm',
                            compareMode ? 'text-muted-foreground/60' : 'text-muted-foreground'
                        )}
                    >
                        <input
                            checked={streamEnabled && !compareMode}
                            disabled={compareMode}
                            onChange={(event) => setStreamEnabled(event.target.checked)}
                            type="checkbox"
                        />
                        Stream
                    </label>
                </div>

                <div className={twMerge('flex-1', compareMode ? 'grid grid-cols-2 gap-4' : '')}>
                    <PlaygroundResponsePanel isLoading={mutationLeft.isPending} response={responseLeft} />

                    {compareMode && (
                        <PlaygroundResponsePanel isLoading={mutationRight.isPending} response={responseRight} />
                    )}
                </div>
            </div>

            <div className="w-64 shrink-0 border-l pl-4">
                <PlaygroundParametersPanel
                    maxTokens={maxTokens}
                    onMaxTokensChange={setMaxTokens}
                    onTemperatureChange={setTemperature}
                    onTopPChange={setTopP}
                    temperature={temperature}
                    topP={topP}
                />
            </div>
        </div>
    );
};

export default AiGatewayPlayground;
