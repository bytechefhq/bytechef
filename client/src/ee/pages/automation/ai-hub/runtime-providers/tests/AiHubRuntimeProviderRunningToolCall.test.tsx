import {AiHubToolCallFallback} from '@/ee/pages/automation/ai-hub/messages/AiHubToolCallRenderer';
import useAiHubSettingsStore from '@/ee/pages/automation/ai-hub/stores/useAiHubSettingsStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {aiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {type AgentSubscriber} from '@ag-ui/client';
import {EventType} from '@ag-ui/core';
import {type ToolCallMessagePartProps} from '@assistant-ui/react';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {buildAiHubSubscriber, projectMessagesWithToolCalls} from '../AiHubRuntimeProvider';

vi.mock('@/ee/pages/automation/ai-hub/chats/api/chats.api', () => ({
    createAiHubChat: vi.fn(),
    generateAiHubChatTitle: vi.fn(),
    patchChat: vi.fn(),
}));

type SubscriberParamsType = Parameters<NonNullable<AgentSubscriber['onToolCallStartEvent']>>[0];

const baseParams = {agent: {}, input: {}, messages: [], state: {}};

const findBuildWorkflowPart = () => {
    const projectedMessages = projectMessagesWithToolCalls(
        aiHubStore.getState().messages,
        Object.values(aiChatToolCallStore.getState().toolCalls)
    );

    const assistantContent = projectedMessages[1].content as Array<{toolName?: string; type: string}>;

    return assistantContent.find((part) => part.type === 'tool-call' && part.toolName === 'buildWorkflow');
};

describe('a live AI Hub turn running buildWorkflow', () => {
    beforeEach(() => {
        aiChatToolCallStore.setState({order: [], toolCalls: {}});
        useAiHubSettingsStore.setState({showToolCalls: false});

        aiHubStore.setState({
            chatId: 'thread-1',
            messages: [
                {content: 'Build me a workflow', role: 'user'},
                {content: '', role: 'assistant'},
            ],
        });
    });

    it('shows the building row while the call runs and drops it once the result arrives', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: aiHubStore.getState().addMessage,
            appendToLastAssistantMessage: aiHubStore.getState().appendToLastAssistantMessage,
            assistantMessageIndex: 1,
            chatId: 'thread-1',
            getLastUserMessage: () => 'Build me a workflow',
        });

        subscriber.onTextMessageContentEvent!({
            ...baseParams,
            event: {delta: "Now I'll build the workflow.", messageId: 'm-1', type: EventType.TEXT_MESSAGE_CONTENT},
            textMessageBuffer: '',
        } as unknown as Parameters<NonNullable<AgentSubscriber['onTextMessageContentEvent']>>[0]);

        subscriber.onToolCallStartEvent!({
            ...baseParams,
            event: {toolCallId: 'call-build', toolCallName: 'buildWorkflow', type: EventType.TOOL_CALL_START},
        } as unknown as SubscriberParamsType);

        const runningPart = findBuildWorkflowPart();

        expect(runningPart).toBeDefined();

        const {unmount} = render(<AiHubToolCallFallback {...(runningPart as unknown as ToolCallMessagePartProps)} />);

        expect(screen.getByRole('status')).toHaveTextContent('Building workflow...');

        unmount();

        subscriber.onToolCallResultEvent!({
            ...baseParams,
            event: {
                content: 'The workflow is built.',
                messageId: 'm-2',
                role: 'tool',
                toolCallId: 'call-build',
                type: EventType.TOOL_CALL_RESULT,
            },
        } as unknown as Parameters<NonNullable<AgentSubscriber['onToolCallResultEvent']>>[0]);

        const {container} = render(
            <AiHubToolCallFallback {...(findBuildWorkflowPart() as unknown as ToolCallMessagePartProps)} />
        );

        expect(container).toBeEmptyDOMElement();
    });

    it('keeps the text before a tool call and puts the text after it in a new bubble', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: aiHubStore.getState().addMessage,
            appendToLastAssistantMessage: aiHubStore.getState().appendToLastAssistantMessage,
            assistantMessageIndex: 1,
            chatId: 'thread-1',
            getLastUserMessage: () => 'Build me a workflow',
        });

        const textEvent = (type: EventType, delta?: string) =>
            ({
                ...baseParams,
                event: {delta, messageId: 'm', type},
                textMessageBuffer: '',
            }) as never;

        subscriber.onTextMessageStartEvent!(textEvent(EventType.TEXT_MESSAGE_START));
        subscriber.onTextMessageContentEvent!(
            textEvent(EventType.TEXT_MESSAGE_CONTENT, "Now I'll build the workflow.")
        );
        subscriber.onTextMessageEndEvent?.(textEvent(EventType.TEXT_MESSAGE_END));

        subscriber.onToolCallStartEvent!({
            ...baseParams,
            event: {toolCallId: 'call-build', toolCallName: 'buildWorkflow', type: EventType.TOOL_CALL_START},
        } as unknown as SubscriberParamsType);

        subscriber.onTextMessageStartEvent!(textEvent(EventType.TEXT_MESSAGE_START));
        subscriber.onTextMessageContentEvent!(textEvent(EventType.TEXT_MESSAGE_CONTENT, 'The workflow is ready.'));

        const projectedMessages = projectMessagesWithToolCalls(
            aiHubStore.getState().messages,
            Object.values(aiChatToolCallStore.getState().toolCalls)
        );

        expect(projectedMessages).toHaveLength(3);
        expect(projectedMessages[1].content).toEqual([
            {text: "Now I'll build the workflow.", type: 'text'},
            expect.objectContaining({toolName: 'buildWorkflow', type: 'tool-call'}),
        ]);
        expect(projectedMessages[2]).toEqual({content: 'The workflow is ready.', role: 'assistant'});
    });

    it('puts text after a tool call that opened the turn in its own bubble below the tool call', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: aiHubStore.getState().addMessage,
            appendToLastAssistantMessage: aiHubStore.getState().appendToLastAssistantMessage,
            assistantMessageIndex: 1,
            chatId: 'thread-1',
            getLastUserMessage: () => 'Build me a workflow',
        });

        subscriber.onToolCallStartEvent!({
            ...baseParams,
            event: {toolCallId: 'call-build', toolCallName: 'buildWorkflow', type: EventType.TOOL_CALL_START},
        } as unknown as SubscriberParamsType);

        subscriber.onTextMessageStartEvent!({
            ...baseParams,
            event: {messageId: 'm', type: EventType.TEXT_MESSAGE_START},
        } as unknown as Parameters<NonNullable<AgentSubscriber['onTextMessageStartEvent']>>[0]);
        subscriber.onTextMessageContentEvent!({
            ...baseParams,
            event: {delta: 'Done.', messageId: 'm', type: EventType.TEXT_MESSAGE_CONTENT},
            textMessageBuffer: '',
        } as unknown as Parameters<NonNullable<AgentSubscriber['onTextMessageContentEvent']>>[0]);

        const projectedMessages = projectMessagesWithToolCalls(
            aiHubStore.getState().messages,
            Object.values(aiChatToolCallStore.getState().toolCalls)
        );

        expect(projectedMessages[1].content).toEqual([
            expect.objectContaining({toolName: 'buildWorkflow', type: 'tool-call'}),
        ]);
        expect(projectedMessages[2]).toEqual({content: 'Done.', role: 'assistant'});
    });
});
