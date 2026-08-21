# Phase 4: Playground — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an interactive Playground tab to the AI Gateway UI that allows testing prompts against configured models with real-time response display, metrics, and trace linking. Playground requests flow through `AiGatewayFacade` so routing, cost tracking, and tracing all work automatically, with `source = PLAYGROUND` on the trace.

**Architecture:** A new GraphQL mutation `playgroundChatCompletion` in the existing `automation-ai-gateway-graphql` module wraps `AiGatewayFacade.chatCompletion()` with session-based auth (instead of API key auth used by the public REST endpoint). The mutation accepts model, messages, and parameters, constructs an `AiGatewayChatCompletionRequest`, and returns the response with metrics. Phase 1 tracing records the request with `source = PLAYGROUND`. The client adds a new Playground sidebar tab with model selector, prompt editor (text/chat modes), parameters panel, response panel, and side-by-side comparison.

**Tech Stack:** Java 25, Spring Boot 4, Spring GraphQL, React 19, TypeScript 5.9, TanStack Query, Tailwind CSS, Lucide Icons

**Reference spec:** `docs/superpowers/specs/2026-04-11-ai-gateway-observability-platform-design.md` — Phase 4 section

**Depends on:** Phase 1 (Tracing & Sessions) — `AiObservabilityTraceSource.PLAYGROUND` enum value, trace creation in facade

---

## File Map

### Server — GraphQL module (`server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `src/main/resources/graphql/ai-gateway-playground.graphqls` | Playground GraphQL schema |
| Create | `src/main/java/.../web/graphql/AiGatewayPlaygroundGraphQlController.java` | Playground mutation controller |

### Client (`client/src/`)

| Action | Path | Purpose |
|---|---|---|
| Create | `graphql/automation/ai-gateway/aiGatewayPlayground.graphql` | Playground GraphQL mutation operation |
| Modify | `pages/automation/ai-gateway/types.ts` | Add playground-related types |
| Modify | `pages/automation/ai-gateway/AiGateway.tsx` | Add Playground sidebar tab |
| Create | `pages/automation/ai-gateway/components/playground/AiGatewayPlayground.tsx` | Main playground component |
| Create | `pages/automation/ai-gateway/components/playground/PlaygroundMessageList.tsx` | Chat mode message list editor |
| Create | `pages/automation/ai-gateway/components/playground/PlaygroundResponsePanel.tsx` | Response display with metrics |
| Create | `pages/automation/ai-gateway/components/playground/PlaygroundParametersPanel.tsx` | Parameters sidebar (temperature, max_tokens, top_p) |

---

## Task 1: GraphQL Schema for Playground

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-gateway-playground.graphqls`

- [ ] **Step 1: Create ai-gateway-playground.graphqls**

```graphql
extend type Mutation {
    playgroundChatCompletion(input: PlaygroundChatCompletionInput!): PlaygroundChatCompletionResponse
}

input PlaygroundChatCompletionInput {
    maxTokens: Int
    messages: [PlaygroundChatMessageInput!]!
    model: String!
    temperature: Float
    topP: Float
}

input PlaygroundChatMessageInput {
    content: String!
    role: PlaygroundChatRole!
}

enum PlaygroundChatRole {
    ASSISTANT
    SYSTEM
    USER
}

type PlaygroundChatCompletionResponse {
    completionTokens: Int
    content: String
    cost: Float
    finishReason: String
    latencyMs: Int
    model: String
    promptTokens: Int
    totalTokens: Int
    traceId: ID
}
```

- [ ] **Step 2: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-gateway-playground.graphqls
git commit -m "732 Add GraphQL schema for playground chat completion"
```

---

## Task 2: GraphQL Controller for Playground

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiGatewayPlaygroundGraphQlController.java`

- [ ] **Step 1: Create AiGatewayPlaygroundGraphQlController**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatRole;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for the AI Gateway Playground.
 *
 * <p>Wraps {@link AiGatewayFacade#chatCompletion} with session-based authentication
 * so playground requests go through the full gateway pipeline (routing, cost tracking,
 * tracing with {@code source = PLAYGROUND}).
 *
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiGatewayPlaygroundGraphQlController {

    private final AiGatewayFacade aiGatewayFacade;

    @SuppressFBWarnings("EI")
    AiGatewayPlaygroundGraphQlController(AiGatewayFacade aiGatewayFacade) {
        this.aiGatewayFacade = aiGatewayFacade;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public PlaygroundChatCompletionResponse playgroundChatCompletion(
        @Argument PlaygroundChatCompletionInput input) {

        List<AiGatewayChatMessage> messages = input.messages()
            .stream()
            .map(message -> new AiGatewayChatMessage(
                AiGatewayChatRole.valueOf(message.role().name()),
                message.content(), null, null, null))
            .toList();

        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            input.model(), messages, input.temperature(), input.maxTokens(), input.topP(),
            false, null, null);

        long startTime = System.currentTimeMillis();

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request);

        long latencyMs = System.currentTimeMillis() - startTime;

        String content = null;
        String finishReason = null;

        if (response.choices() != null && !response.choices().isEmpty()) {
            AiGatewayChatCompletionResponse.Choice firstChoice = response.choices().getFirst();

            if (firstChoice.message() != null) {
                content = firstChoice.message().content();
            }

            finishReason = firstChoice.finishReason();
        }

        Integer promptTokens = null;
        Integer completionTokens = null;
        Integer totalTokens = null;
        BigDecimal cost = null;

        if (response.usage() != null) {
            promptTokens = response.usage().promptTokens();
            completionTokens = response.usage().completionTokens();
            totalTokens = response.usage().totalTokens();
        }

        return new PlaygroundChatCompletionResponse(
            content, response.model(), finishReason, promptTokens, completionTokens,
            totalTokens, cost, (int) latencyMs, null);
    }

    @SuppressFBWarnings("EI")
    public record PlaygroundChatCompletionInput(
        String model, List<PlaygroundChatMessageInput> messages,
        Double temperature, Integer maxTokens, Double topP) {
    }

    @SuppressFBWarnings("EI")
    public record PlaygroundChatMessageInput(String content, PlaygroundChatRole role) {
    }

    public enum PlaygroundChatRole {
        ASSISTANT, SYSTEM, USER
    }

    @SuppressFBWarnings("EI")
    public record PlaygroundChatCompletionResponse(
        String content, String model, String finishReason,
        Integer promptTokens, Integer completionTokens, Integer totalTokens,
        BigDecimal cost, Integer latencyMs, Long traceId) {
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiGatewayPlaygroundGraphQlController.java
git commit -m "732 Add GraphQL controller for playground chat completion"
```

---

## Task 3: Client — GraphQL Operation and Codegen

**Files:**
- Create: `client/src/graphql/automation/ai-gateway/aiGatewayPlayground.graphql`

- [ ] **Step 1: Create aiGatewayPlayground.graphql**

```graphql
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
```

- [ ] **Step 2: Run GraphQL codegen**

Run: `cd client && npx graphql-codegen`
Expected: generates updated `src/shared/middleware/graphql.ts` with `usePlaygroundChatCompletionMutation` hook

- [ ] **Step 3: Commit**

```bash
cd client
git add src/graphql/automation/ai-gateway/aiGatewayPlayground.graphql \
  src/shared/middleware/graphql.ts
git commit -m "732 client - Add GraphQL operation and codegen for playground chat completion"
```

---

## Task 4: Client — Playground Parameters Panel

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/playground/PlaygroundParametersPanel.tsx`

- [ ] **Step 1: Create PlaygroundParametersPanel.tsx**

```typescript
import {Label} from '@/components/ui/label';
import {Slider} from '@/components/ui/slider';

interface PlaygroundParametersPanelProps {
    maxTokens: number;
    onMaxTokensChange: (value: number) => void;
    onTemperatureChange: (value: number) => void;
    onTopPChange: (value: number) => void;
    temperature: number;
    topP: number;
}

const PlaygroundParametersPanel = ({
    maxTokens,
    onMaxTokensChange,
    onTemperatureChange,
    onTopPChange,
    temperature,
    topP,
}: PlaygroundParametersPanelProps) => {
    return (
        <fieldset className="space-y-5 border-0 p-0">
            <legend className="mb-3 text-sm font-semibold">Parameters</legend>

            <div className="space-y-2">
                <div className="flex items-center justify-between">
                    <Label htmlFor="temperature">Temperature</Label>

                    <span className="text-xs text-muted-foreground">{temperature.toFixed(2)}</span>
                </div>

                <Slider
                    id="temperature"
                    max={2}
                    min={0}
                    onValueChange={(values) => onTemperatureChange(values[0])}
                    step={0.01}
                    value={[temperature]}
                />
            </div>

            <div className="space-y-2">
                <div className="flex items-center justify-between">
                    <Label htmlFor="maxTokens">Max Tokens</Label>

                    <span className="text-xs text-muted-foreground">{maxTokens}</span>
                </div>

                <Slider
                    id="maxTokens"
                    max={16384}
                    min={1}
                    onValueChange={(values) => onMaxTokensChange(values[0])}
                    step={1}
                    value={[maxTokens]}
                />
            </div>

            <div className="space-y-2">
                <div className="flex items-center justify-between">
                    <Label htmlFor="topP">Top P</Label>

                    <span className="text-xs text-muted-foreground">{topP.toFixed(2)}</span>
                </div>

                <Slider
                    id="topP"
                    max={1}
                    min={0}
                    onValueChange={(values) => onTopPChange(values[0])}
                    step={0.01}
                    value={[topP]}
                />
            </div>
        </fieldset>
    );
};

export default PlaygroundParametersPanel;
```

- [ ] **Step 2: Verify the Slider component exists**

Run: `ls client/src/components/ui/slider.tsx`
If missing, check `client/src/components/ui/` for the correct component path. The Slider is a standard shadcn/ui component. If it does not exist, create it using `npx shadcn@latest add slider` from the `client/` directory.

- [ ] **Step 3: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/playground/PlaygroundParametersPanel.tsx
git commit -m "732 client - Add PlaygroundParametersPanel component with temperature, max tokens, top P sliders"
```

---

## Task 5: Client — Playground Message List

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/playground/PlaygroundMessageList.tsx`

- [ ] **Step 1: Create PlaygroundMessageList.tsx**

```typescript
import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {PlusIcon, TrashIcon} from 'lucide-react';
import {useCallback} from 'react';

export interface PlaygroundMessageI {
    content: string;
    role: 'ASSISTANT' | 'SYSTEM' | 'USER';
}

interface PlaygroundMessageListProps {
    messages: PlaygroundMessageI[];
    onMessagesChange: (messages: PlaygroundMessageI[]) => void;
}

const ROLE_OPTIONS: PlaygroundMessageI['role'][] = ['SYSTEM', 'USER', 'ASSISTANT'];

const PlaygroundMessageList = ({messages, onMessagesChange}: PlaygroundMessageListProps) => {
    const handleAddMessage = useCallback(() => {
        onMessagesChange([...messages, {content: '', role: 'USER'}]);
    }, [messages, onMessagesChange]);

    const handleRemoveMessage = useCallback(
        (index: number) => {
            const updatedMessages = messages.filter((_, messageIndex) => messageIndex !== index);

            onMessagesChange(updatedMessages);
        },
        [messages, onMessagesChange],
    );

    const handleContentChange = useCallback(
        (index: number, content: string) => {
            const updatedMessages = messages.map((message, messageIndex) =>
                messageIndex === index ? {...message, content} : message,
            );

            onMessagesChange(updatedMessages);
        },
        [messages, onMessagesChange],
    );

    const handleRoleChange = useCallback(
        (index: number, role: PlaygroundMessageI['role']) => {
            const updatedMessages = messages.map((message, messageIndex) =>
                messageIndex === index ? {...message, role} : message,
            );

            onMessagesChange(updatedMessages);
        },
        [messages, onMessagesChange],
    );

    return (
        <div className="space-y-3">
            {messages.map((message, index) => (
                <div className="flex gap-2" key={index}>
                    <div className="w-32 shrink-0">
                        <Select
                            onValueChange={(value) =>
                                handleRoleChange(index, value as PlaygroundMessageI['role'])
                            }
                            value={message.role}
                        >
                            <SelectTrigger className="h-9 text-xs">
                                <SelectValue />
                            </SelectTrigger>

                            <SelectContent>
                                {ROLE_OPTIONS.map((role) => (
                                    <SelectItem key={role} value={role}>
                                        {role.charAt(0) + role.slice(1).toLowerCase()}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    <Textarea
                        className="min-h-[60px] flex-1 resize-y text-sm"
                        onChange={(event) => handleContentChange(index, event.target.value)}
                        placeholder={`Enter ${message.role.toLowerCase()} message...`}
                        value={message.content}
                    />

                    <button
                        className="shrink-0 self-start p-2 text-destructive hover:text-destructive/80"
                        disabled={messages.length <= 1}
                        onClick={() => handleRemoveMessage(index)}
                    >
                        <TrashIcon className="size-4" />
                    </button>
                </div>
            ))}

            <Button
                className="mt-2"
                icon={<PlusIcon className="size-4" />}
                label="Add Message"
                onClick={handleAddMessage}
                variant="outline"
            />
        </div>
    );
};

export default PlaygroundMessageList;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/playground/PlaygroundMessageList.tsx
git commit -m "732 client - Add PlaygroundMessageList component for chat mode message editing"
```

---

## Task 6: Client — Playground Response Panel

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/playground/PlaygroundResponsePanel.tsx`

- [ ] **Step 1: Create PlaygroundResponsePanel.tsx**

```typescript
import {PlaygroundChatCompletionMutation} from '@/shared/middleware/graphql';
import {ActivityIcon, ClockIcon, CoinsIcon, HashIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

type PlaygroundResponseType = NonNullable<
    PlaygroundChatCompletionMutation['playgroundChatCompletion']
>;

interface PlaygroundResponsePanelProps {
    className?: string;
    isLoading: boolean;
    response: PlaygroundResponseType | undefined;
}

const PlaygroundResponsePanel = ({className, isLoading, response}: PlaygroundResponsePanelProps) => {
    if (isLoading) {
        return (
            <div className={twMerge('flex items-center justify-center rounded-lg border p-8', className)}>
                <div className="flex items-center gap-2 text-muted-foreground">
                    <ActivityIcon className="size-4 animate-pulse" />

                    <span className="text-sm">Generating response...</span>
                </div>
            </div>
        );
    }

    if (!response) {
        return (
            <div className={twMerge('flex items-center justify-center rounded-lg border p-8', className)}>
                <span className="text-sm text-muted-foreground">Response will appear here</span>
            </div>
        );
    }

    return (
        <div className={twMerge('rounded-lg border', className)}>
            <div className="flex flex-wrap gap-4 border-b px-4 py-2 text-xs text-muted-foreground">
                {response.model && (
                    <span className="flex items-center gap-1">
                        <HashIcon className="size-3" />
                        {response.model}
                    </span>
                )}

                {response.latencyMs != null && (
                    <span className="flex items-center gap-1">
                        <ClockIcon className="size-3" />
                        {response.latencyMs}ms
                    </span>
                )}

                {(response.promptTokens != null || response.completionTokens != null) && (
                    <span className="flex items-center gap-1">
                        <ActivityIcon className="size-3" />
                        {response.promptTokens ?? 0} in / {response.completionTokens ?? 0} out
                        {response.totalTokens != null && ` (${response.totalTokens} total)`}
                    </span>
                )}

                {response.cost != null && (
                    <span className="flex items-center gap-1">
                        <CoinsIcon className="size-3" />
                        ${Number(response.cost).toFixed(4)}
                    </span>
                )}

                {response.finishReason && (
                    <span className="rounded bg-muted px-1.5 py-0.5">
                        {response.finishReason}
                    </span>
                )}

                {response.traceId && (
                    <span className="text-primary">
                        Trace #{response.traceId}
                    </span>
                )}
            </div>

            <div className="whitespace-pre-wrap p-4 text-sm">
                {response.content || <span className="text-muted-foreground">Empty response</span>}
            </div>
        </div>
    );
};

export default PlaygroundResponsePanel;
```

- [ ] **Step 2: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/playground/PlaygroundResponsePanel.tsx
git commit -m "732 client - Add PlaygroundResponsePanel component with metrics display"
```

---

## Task 7: Client — Main Playground Component

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/playground/AiGatewayPlayground.tsx`

- [ ] **Step 1: Create AiGatewayPlayground.tsx**

```typescript
import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    PlaygroundChatCompletionMutation,
    usePlaygroundChatCompletionMutation,
    useWorkspaceAiGatewayModelsQuery,
    useWorkspaceAiGatewayProvidersQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ColumnsIcon, PlayIcon, TerminalIcon} from 'lucide-react';
import {useCallback, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import PlaygroundMessageList, {type PlaygroundMessageI} from './PlaygroundMessageList';
import PlaygroundParametersPanel from './PlaygroundParametersPanel';
import PlaygroundResponsePanel from './PlaygroundResponsePanel';

type PlaygroundModeType = 'chat' | 'text';

type PlaygroundResponseType = NonNullable<
    PlaygroundChatCompletionMutation['playgroundChatCompletion']
>;

const AiGatewayPlayground = () => {
    const [compareMode, setCompareMode] = useState(false);
    const [maxTokens, setMaxTokens] = useState(2048);
    const [messages, setMessages] = useState<PlaygroundMessageI[]>([{content: '', role: 'USER'}]);
    const [mode, setMode] = useState<PlaygroundModeType>('text');
    const [responseLeft, setResponseLeft] = useState<PlaygroundResponseType | undefined>(undefined);
    const [responseRight, setResponseRight] = useState<PlaygroundResponseType | undefined>(undefined);
    const [selectedModelLeft, setSelectedModelLeft] = useState<string | undefined>(undefined);
    const [selectedModelRight, setSelectedModelRight] = useState<string | undefined>(undefined);
    const [temperature, setTemperature] = useState(1.0);
    const [textPrompt, setTextPrompt] = useState('');
    const [topP, setTopP] = useState(1.0);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data: modelsData, isLoading: modelsIsLoading} = useWorkspaceAiGatewayModelsQuery({
        workspaceId: currentWorkspaceId + '',
    });
    const {data: providersData} = useWorkspaceAiGatewayProvidersQuery({
        workspaceId: currentWorkspaceId + '',
    });

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
        [modelsData],
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
            role: message.role,
        }));

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
        selectedModelLeft,
        selectedModelRight,
        temperature,
        topP,
    ]);

    const isRunning = mutationLeft.isPending || mutationRight.isPending;

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
                        <legend className="text-sm font-medium">
                            {compareMode ? 'Model A' : 'Model'}
                        </legend>

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

                <div className="mb-4">
                    <Button
                        disabled={!selectedModelLeft || isRunning}
                        icon={<PlayIcon className="size-4" />}
                        label={isRunning ? 'Running...' : 'Run'}
                        onClick={handleRun}
                    />
                </div>

                <div className={twMerge('flex-1', compareMode ? 'grid grid-cols-2 gap-4' : '')}>
                    <PlaygroundResponsePanel
                        isLoading={mutationLeft.isPending}
                        response={responseLeft}
                    />

                    {compareMode && (
                        <PlaygroundResponsePanel
                            isLoading={mutationRight.isPending}
                            response={responseRight}
                        />
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
```

- [ ] **Step 2: Verify the Select, Textarea, and Slider shadcn/ui components exist**

Run:
```bash
ls client/src/components/ui/select.tsx client/src/components/ui/textarea.tsx client/src/components/ui/slider.tsx
```

For any missing component, install it:
```bash
cd client && npx shadcn@latest add select textarea slider
```

- [ ] **Step 3: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/components/playground/AiGatewayPlayground.tsx
git commit -m "732 client - Add main AiGatewayPlayground component with text/chat modes and side-by-side comparison"
```

---

## Task 8: Client — Types Update and Sidebar Integration

**Files:**
- Modify: `client/src/pages/automation/ai-gateway/types.ts`
- Modify: `client/src/pages/automation/ai-gateway/AiGateway.tsx`

- [ ] **Step 1: Add playground types to types.ts**

Add to `client/src/pages/automation/ai-gateway/types.ts`:

```typescript
import {
    AiGatewayProjectsQuery,
    PlaygroundChatCompletionMutation,
    WorkspaceAiGatewayModelsQuery,
    WorkspaceAiGatewayProvidersQuery,
    WorkspaceAiGatewayRoutingPoliciesQuery,
} from '@/shared/middleware/graphql';

// ... existing types unchanged ...

export type PlaygroundChatCompletionResponseType = NonNullable<
    PlaygroundChatCompletionMutation['playgroundChatCompletion']
>;
```

Note: If Phase 1 has already been implemented, preserve any existing trace/session types and add `PlaygroundChatCompletionMutation` to the existing import.

- [ ] **Step 2: Update AiGateway.tsx sidebar**

In `client/src/pages/automation/ai-gateway/AiGateway.tsx`:

Add import:
```typescript
import AiGatewayPlayground from './components/playground/AiGatewayPlayground';
```

Update the type union to include `'playground'`:
```typescript
type AiGatewayPageType = 'budget' | 'models' | 'monitoring' | 'playground' | 'projects' | 'providers' | 'routing' | 'settings';
```

Add a `LeftSidebarNavItem` entry after the "Monitoring" item (before any Phase 1 tabs like "Traces" if present):
```typescript
<LeftSidebarNavItem
    item={{
        current: activePage === 'playground',
        name: 'Playground',
        onItemClick: () => setActivePage('playground'),
    }}
/>
```

Add conditional render:
```typescript
{activePage === 'playground' && <AiGatewayPlayground />}
```

- [ ] **Step 3: Verify lint and typecheck pass**

Run: `cd client && npm run check`
Expected: All checks pass

- [ ] **Step 4: Commit**

```bash
cd client
git add src/pages/automation/ai-gateway/types.ts src/pages/automation/ai-gateway/AiGateway.tsx
git commit -m "732 client - Add Playground sidebar tab to AI Gateway"
```

---

## Task 9: Server — Format Code

- [ ] **Step 1: Run Spotless**

Run: `./gradlew spotlessApply`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Commit if changes**

```bash
git add -u
git commit -m "732 Apply Spotless formatting"
```

---

## Task 10: Client — Format Code

- [ ] **Step 1: Run client formatting**

Run: `cd client && npm run format`

- [ ] **Step 2: Run full check**

Run: `cd client && npm run check`
Expected: All checks pass (lint, typecheck, tests)

- [ ] **Step 3: Commit if changes**

```bash
cd client
git add -u
git commit -m "732 client - Apply formatting"
```

---

## Task 11: Full Build Verification

- [ ] **Step 1: Server compilation**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Client build**

Run: `cd client && npm run build`
Expected: BUILD SUCCESSFUL

---

## Design Decisions

1. **Session auth via GraphQL mutation (not REST):** The existing public REST endpoint (`AiGatewayChatCompletionApiController`) uses API key auth. Playground needs session-based auth for logged-in admin users. A GraphQL mutation is the simplest path since GraphQL already uses Spring Security session auth. No new REST endpoint needed.

2. **No new database tables:** The spec explicitly states no new tables. Playground requests create traces with `source = PLAYGROUND` (Phase 1 infrastructure). The monitoring dashboard can filter by `source = API` to exclude playground traffic.

3. **Response DTO wraps facade response:** The `PlaygroundChatCompletionResponse` record flattens the OpenAI-style `AiGatewayChatCompletionResponse` into a simple structure optimized for the playground UI — single content string, flat token counts, latency.

4. **Side-by-side comparison:** Two independent mutation calls from the client. No server-side batching needed. Each call creates its own trace and request log entry independently.

5. **Model name as selector value:** The `model` field in `AiGatewayChatCompletionRequest` expects the model name string (e.g., "gpt-4"), which maps to how `AiGatewayFacade.resolveModel()` works. The UI selects from configured models and passes the name.

6. **Managed prompt mode deferred:** The spec mentions managed prompt mode (select prompt by name, auto-load template with variable fields), but this depends on Phase 2 (Prompt Management). It will be added as a follow-up once Phase 2 prompt infrastructure exists. The playground architecture supports this — just add a third mode tab that fetches prompts and renders variable fields.
