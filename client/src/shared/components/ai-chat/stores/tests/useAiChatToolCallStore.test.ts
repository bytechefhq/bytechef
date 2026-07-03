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

    describe('cross-task cleanup', () => {
        // The runtime provider calls resetForTask(currentId) on task switch so a stale
        // running tool-call from the previous task does not appear in the new task's
        // panel. These tests pin that contract — a regression that drops the resetForTask call
        // (or makes it a no-op) would leave task A's tool-calls dangling in task B's
        // store, which is the exact bug the commit message says was fixed.
        it('resetForTask drops only entries belonging to the named task', () => {
            aiChatToolCallStore.getState().startToolCall('call-A1', 'research', 0, 'conv-A');
            aiChatToolCallStore.getState().startToolCall('call-A2', 'research', 0, 'conv-A');
            aiChatToolCallStore.getState().startToolCall('call-B1', 'research', 0, 'conv-B');

            aiChatToolCallStore.getState().resetForTask('conv-A');

            const remaining = aiChatToolCallStore.getState().toolCalls;

            expect(aiChatToolCallStore.getState().order).toEqual(['call-B1']);
            expect(Object.keys(remaining)).toEqual(['call-B1']);
            expect(remaining['call-B1'].taskId).toBe('conv-B');
        });

        it('resetForTask is a no-op when no entries match', () => {
            aiChatToolCallStore.getState().startToolCall('call-A1', 'research', 0, 'conv-A');

            aiChatToolCallStore.getState().resetForTask('conv-other');

            expect(aiChatToolCallStore.getState().order).toEqual(['call-A1']);
        });

        it('resetForTask with undefined performs a hard reset', () => {
            // Mirrors the runtime provider's behavior when no task id is bound (e.g. the very
            // first turn before a task row exists). Hard reset prevents untracked entries from
            // lingering across tasks forever.
            aiChatToolCallStore.getState().startToolCall('call-A1', 'research', 0, 'conv-A');
            aiChatToolCallStore.getState().startToolCall('call-untracked', 'research', 0);

            aiChatToolCallStore.getState().resetForTask(undefined);

            expect(aiChatToolCallStore.getState().order).toEqual([]);
            expect(Object.keys(aiChatToolCallStore.getState().toolCalls)).toHaveLength(0);
        });

        it('resetForTask drops a still-running tool-call from the named task', () => {
            // The exact regression the commit message calls out: when the runtime provider switches AWAY
            // from a task mid-stream, it calls resetForTask(LEAVING_ID) to remove that
            // task's stale running entries. Without this, those entries would linger in the store
            // forever and bleed into any future task.
            aiChatToolCallStore.getState().startToolCall('call-A1', 'runChatWorkflow', 0, 'conv-A');
            aiChatToolCallStore.getState().appendProgressiveOutput('call-A1', 'Step 1: ');

            // Simulate the runtime provider's "leaving conv-A" cleanup.
            aiChatToolCallStore.getState().resetForTask('conv-A');

            // call-A1 should NOT remain — it would be a stale running entry attributed to a task
            // the user is no longer viewing.
            expect(aiChatToolCallStore.getState().toolCalls['call-A1']).toBeUndefined();
            expect(aiChatToolCallStore.getState().order).not.toContain('call-A1');
        });
    });
});
