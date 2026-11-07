import {aiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {beforeEach, describe, expect, it} from 'vitest';

describe('aiChatToolCallStore', () => {
    beforeEach(() => {
        aiChatToolCallStore.setState({order: [], toolCalls: {}});
    });

    it('startToolCall registers a running entry tagged with the assistant message index', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'research', 3);

        const entry = aiChatToolCallStore.getState().toolCalls['call-1'];

        expect(entry).toBeDefined();
        expect(entry.toolName).toBe('research');
        expect(entry.status).toBe('running');
        expect(entry.messageIndex).toBe(3);
        expect(aiChatToolCallStore.getState().order).toEqual(['call-1']);
    });

    it('startToolCall is idempotent for the same id', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'research', 1);
        aiChatToolCallStore.getState().startToolCall('call-1', 'research', 1);

        expect(aiChatToolCallStore.getState().order).toEqual(['call-1']);
    });

    it('updateToolCallArgs merges args without changing status', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'createMemory', 0);
        aiChatToolCallStore.getState().updateToolCallArgs('call-1', {name: 'Daily standups'});

        const entry = aiChatToolCallStore.getState().toolCalls['call-1'];

        expect(entry.args).toEqual({name: 'Daily standups'});
        expect(entry.status).toBe('running');
    });

    it('completeToolCall flips status to success and stores the result', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'createMemory', 0);
        aiChatToolCallStore.getState().completeToolCall('call-1', {ok: true}, false);

        const entry = aiChatToolCallStore.getState().toolCalls['call-1'];

        expect(entry.status).toBe('success');
        expect(entry.result).toEqual({ok: true});
    });

    it('completeToolCall with isError=true marks the entry as error', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'createMemory', 0);
        aiChatToolCallStore.getState().completeToolCall('call-1', {error: 'boom'}, true);

        const entry = aiChatToolCallStore.getState().toolCalls['call-1'];

        expect(entry.status).toBe('error');
        expect(entry.result).toEqual({error: 'boom'});
    });

    it('appendProgressiveOutput accumulates chunks into progressiveOutput', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'runChatWorkflow', 0);
        aiChatToolCallStore.getState().appendProgressiveOutput('call-1', 'Step 1: ');
        aiChatToolCallStore.getState().appendProgressiveOutput('call-1', 'done\n\nStep 2: queued');

        expect(aiChatToolCallStore.getState().toolCalls['call-1'].progressiveOutput).toBe(
            'Step 1: done\n\nStep 2: queued'
        );
    });

    it('addProgress appends timestamped entries to the progress list', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'research', 0);
        aiChatToolCallStore.getState().addProgress('call-1', 'Fetching results');
        aiChatToolCallStore.getState().addProgress('call-1', 'Synthesizing answer');

        const entry = aiChatToolCallStore.getState().toolCalls['call-1'];

        expect(entry.progress).toHaveLength(2);
        expect(entry.progress.map((entry) => entry.text)).toEqual(['Fetching results', 'Synthesizing answer']);
    });

    it('addProgress coalesces a step identical to the latest one', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'buildWorkflow', 0);
        aiChatToolCallStore.getState().addProgress('call-1', 'Searching components');
        aiChatToolCallStore.getState().addProgress('call-1', 'Searching components');
        aiChatToolCallStore.getState().addProgress('call-1', 'Saving the workflow');
        aiChatToolCallStore.getState().addProgress('call-1', 'Searching components');

        expect(aiChatToolCallStore.getState().toolCalls['call-1'].progress.map((step) => step.text)).toEqual([
            'Searching components',
            'Saving the workflow',
            'Searching components',
        ]);
    });

    it('startProgress and finishProgress set the label and the outcome of a running call', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'buildWorkflow', 0);
        aiChatToolCallStore.getState().startProgress('call-1', 'Build a Gmail to Slack workflow');
        aiChatToolCallStore.getState().finishProgress('call-1', 'failed');

        const entry = aiChatToolCallStore.getState().toolCalls['call-1'];

        expect(entry.progressLabel).toBe('Build a Gmail to Slack workflow');
        expect(entry.progressOutcome).toBe('failed');
    });

    it('addProgress ignores steps after the delegate finished', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'buildWorkflow', 0);
        aiChatToolCallStore.getState().addProgress('call-1', 'Searching components');
        aiChatToolCallStore.getState().finishProgress('call-1', 'done');
        aiChatToolCallStore.getState().addProgress('call-1', 'Saving the workflow');

        expect(aiChatToolCallStore.getState().toolCalls['call-1'].progress).toHaveLength(1);
    });

    it('startProgress and finishProgress ignore a call that is no longer running', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'buildWorkflow', 0);
        aiChatToolCallStore.getState().completeToolCall('call-1', {ok: true}, false);
        aiChatToolCallStore.getState().startProgress('call-1', 'Late label');
        aiChatToolCallStore.getState().finishProgress('call-1', 'failed');

        const entry = aiChatToolCallStore.getState().toolCalls['call-1'];

        expect(entry.progressLabel).toBeUndefined();
        expect(entry.progressOutcome).toBeUndefined();
    });

    it('keeps the progress label and outcome through every terminal transition', () => {
        const store = aiChatToolCallStore.getState();

        for (const toolCallId of ['call-complete', 'call-fail-chat', 'call-fail-all']) {
            store.startToolCall(toolCallId, 'buildWorkflow', 0, toolCallId === 'call-fail-chat' ? 'chat-1' : 'chat-2');
            store.startProgress(toolCallId, 'Build it');
            store.finishProgress(toolCallId, 'failed');
        }

        store.completeToolCall('call-complete', {error: 'boom'}, true);
        store.failRunningInChat('chat-1', {error: 'switched'}, 'aborted');
        store.failAllRunning({error: 'network'});

        const {toolCalls} = aiChatToolCallStore.getState();

        expect(toolCalls['call-complete'].status).toBe('error');
        expect(toolCalls['call-fail-chat'].status).toBe('aborted');
        expect(toolCalls['call-fail-all'].status).toBe('error');

        for (const toolCallId of ['call-complete', 'call-fail-chat', 'call-fail-all']) {
            expect(toolCalls[toolCallId].progressLabel).toBe('Build it');
            expect(toolCalls[toolCallId].progressOutcome).toBe('failed');
        }
    });

    it('findRunningToolCallByName returns the most recent running call with that name', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'research', 0);
        aiChatToolCallStore.getState().startToolCall('call-2', 'workflowBuilder', 0);
        aiChatToolCallStore.getState().startToolCall('call-3', 'research', 0);

        const found = aiChatToolCallStore.getState().findRunningToolCallByName('research');

        expect(found?.toolCallId).toBe('call-3');
    });

    it('findRunningToolCallByName ignores completed calls', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'research', 0);
        aiChatToolCallStore.getState().completeToolCall('call-1', {}, false);

        expect(aiChatToolCallStore.getState().findRunningToolCallByName('research')).toBeUndefined();
    });

    it('reset clears toolCalls and order', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'research', 0);
        aiChatToolCallStore.getState().reset();

        expect(aiChatToolCallStore.getState().order).toEqual([]);
        expect(Object.keys(aiChatToolCallStore.getState().toolCalls)).toHaveLength(0);
    });

    describe('cross-chat cleanup', () => {
        it('removeFromMessageIndex drops the named chat entries from the message index on', () => {
            aiChatToolCallStore.getState().startToolCall('call-A1', 'research', 1, 'conv-A');
            aiChatToolCallStore.getState().startToolCall('call-A3', 'research', 3, 'conv-A');
            aiChatToolCallStore.getState().startToolCall('call-A5', 'research', 5, 'conv-A');
            aiChatToolCallStore.getState().startToolCall('call-B5', 'research', 5, 'conv-B');

            aiChatToolCallStore.getState().removeFromMessageIndex('conv-A', 3);

            expect(aiChatToolCallStore.getState().order).toEqual(['call-A1', 'call-B5']);
            expect(Object.keys(aiChatToolCallStore.getState().toolCalls)).toEqual(['call-A1', 'call-B5']);
        });

        it('resetForChat drops only entries belonging to the named chat', () => {
            aiChatToolCallStore.getState().startToolCall('call-A1', 'research', 0, 'conv-A');
            aiChatToolCallStore.getState().startToolCall('call-A2', 'research', 0, 'conv-A');
            aiChatToolCallStore.getState().startToolCall('call-B1', 'research', 0, 'conv-B');

            aiChatToolCallStore.getState().resetForChat('conv-A');

            const remaining = aiChatToolCallStore.getState().toolCalls;

            expect(aiChatToolCallStore.getState().order).toEqual(['call-B1']);
            expect(Object.keys(remaining)).toEqual(['call-B1']);
            expect(remaining['call-B1'].chatId).toBe('conv-B');
        });

        it('resetForChat is a no-op when no entries match', () => {
            aiChatToolCallStore.getState().startToolCall('call-A1', 'research', 0, 'conv-A');

            aiChatToolCallStore.getState().resetForChat('conv-other');

            expect(aiChatToolCallStore.getState().order).toEqual(['call-A1']);
        });

        it('resetForChat with undefined performs a hard reset', () => {
            aiChatToolCallStore.getState().startToolCall('call-A1', 'research', 0, 'conv-A');
            aiChatToolCallStore.getState().startToolCall('call-untracked', 'research', 0);

            aiChatToolCallStore.getState().resetForChat(undefined);

            expect(aiChatToolCallStore.getState().order).toEqual([]);
            expect(Object.keys(aiChatToolCallStore.getState().toolCalls)).toHaveLength(0);
        });

        it('resetForChat drops a still-running tool-call from the named chat', () => {
            aiChatToolCallStore.getState().startToolCall('call-A1', 'runChatWorkflow', 0, 'conv-A');
            aiChatToolCallStore.getState().appendProgressiveOutput('call-A1', 'Step 1: ');

            // Simulate the runtime provider's "leaving conv-A" cleanup.
            aiChatToolCallStore.getState().resetForChat('conv-A');

            expect(aiChatToolCallStore.getState().toolCalls['call-A1']).toBeUndefined();
            expect(aiChatToolCallStore.getState().order).not.toContain('call-A1');
        });
    });

    it('failRunningInChat keeps the running entries it is told to keep', () => {
        aiChatToolCallStore.getState().startToolCall('call-A1', 'runChatWorkflow', 0, 'conv-A');
        aiChatToolCallStore.getState().startToolCall('call-A2', 'research', 0, 'conv-A');

        aiChatToolCallStore.getState().failRunningInChat('conv-A', {reason: 'done'}, 'aborted', new Set(['call-A1']));

        expect(aiChatToolCallStore.getState().toolCalls['call-A1'].status).toBe('running');
        expect(aiChatToolCallStore.getState().toolCalls['call-A2'].status).toBe('aborted');
    });

    it('reopenToolCall returns a completed entry to running so progressive output appends again', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'runChatWorkflow', 0, 'conv-A');
        aiChatToolCallStore.getState().completeToolCall('call-1', {streaming: true}, false);

        aiChatToolCallStore.getState().reopenToolCall('call-1');
        aiChatToolCallStore.getState().appendProgressiveOutput('call-1', 'Hi');

        expect(aiChatToolCallStore.getState().toolCalls['call-1'].status).toBe('running');
        expect(aiChatToolCallStore.getState().toolCalls['call-1'].progressiveOutput).toBe('Hi');
    });

    it('reopenToolCall leaves an aborted entry aborted', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'runChatWorkflow', 0, 'conv-A');
        aiChatToolCallStore.getState().failRunningInChat('conv-A', {reason: 'stop'}, 'aborted');

        aiChatToolCallStore.getState().reopenToolCall('call-1');

        expect(aiChatToolCallStore.getState().toolCalls['call-1'].status).toBe('aborted');
    });
});
