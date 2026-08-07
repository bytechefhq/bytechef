import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {aiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {
    TASKS_PAGE_SIZE,
    cancelTaskRunIfStreaming,
    getTasksPage,
    reconcileProbedTaskActivity,
} from '../AiHubTasksSidebar';
import {AiHubTaskI} from '../api/tasks.api';

function buildTask(overrides: Partial<AiHubTaskI> = {}): AiHubTaskI {
    return {
        aiHubPersonalAgentId: null,
        autoTitled: true,
        createdAt: new Date().toISOString(),
        id: 1,
        kind: 'STANDARD',
        lastPreview: null,
        messageCount: 0,
        status: 'ACTIVE',
        threadId: 'thread-1',
        title: 'Task',
        updatedAt: new Date().toISOString(),
        userId: 1,
        workflowExecutionId: null,
        workspaceId: 1,
        ...overrides,
    };
}

describe('cancelTaskRunIfStreaming', () => {
    beforeEach(() => {
        aiHubRunStateStore.getState().reset();
        aiHubTasksStore.setState({taskActivity: {}});
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    function buildCancel() {
        return {cancelAiHubRun: vi.fn(), cancelWorkflowChatTurn: vi.fn()};
    }

    it('cancels the LLM run (with runId) and clears run state for a focused, streaming STANDARD task', () => {
        aiHubRunStateStore.getState().setTaskRunning('thread-1', true);
        aiHubRunStateStore.getState().setTaskRunId('thread-1', 'run-1');

        const cancel = buildCancel();

        cancelTaskRunIfStreaming(buildTask({id: 7, kind: 'STANDARD', threadId: 'thread-1'}), 1, cancel);

        expect(cancel.cancelAiHubRun).toHaveBeenCalledWith({id: '7', runId: 'run-1', workspaceId: '1'});
        expect(cancel.cancelWorkflowChatTurn).not.toHaveBeenCalled();
        expect(aiHubRunStateStore.getState().runningByTask['thread-1']).toBe(false);
    });

    it('cancels the workflow-chat turn for a streaming WORKFLOW_CHAT task', () => {
        aiHubRunStateStore.getState().setTaskRunning('thread-1', true);

        const cancel = buildCancel();

        cancelTaskRunIfStreaming(buildTask({id: 8, kind: 'WORKFLOW_CHAT', threadId: 'thread-1'}), 1, cancel);

        expect(cancel.cancelWorkflowChatTurn).toHaveBeenCalledWith({id: '8', workspaceId: '1'});
        expect(cancel.cancelAiHubRun).not.toHaveBeenCalled();
    });

    it('treats a background task with a probe-driven running activity state as streaming', () => {
        aiHubTasksStore.getState().setActivityState('thread-1', 'running');

        const cancel = buildCancel();

        cancelTaskRunIfStreaming(buildTask({id: 9, kind: 'STANDARD', threadId: 'thread-1'}), 1, cancel);

        expect(cancel.cancelAiHubRun).toHaveBeenCalledWith({id: '9', runId: undefined, workspaceId: '1'});
    });

    it('is a no-op when the task is not streaming', () => {
        const cancel = buildCancel();

        cancelTaskRunIfStreaming(buildTask({id: 10, threadId: 'thread-1'}), 1, cancel);

        expect(cancel.cancelAiHubRun).not.toHaveBeenCalled();
        expect(cancel.cancelWorkflowChatTurn).not.toHaveBeenCalled();
    });

    it('is a no-op when the workspace id is missing', () => {
        aiHubRunStateStore.getState().setTaskRunning('thread-1', true);

        const cancel = buildCancel();

        cancelTaskRunIfStreaming(buildTask({id: 11, threadId: 'thread-1'}), undefined, cancel);

        expect(cancel.cancelAiHubRun).not.toHaveBeenCalled();
    });
});

describe('reconcileProbedTaskActivity', () => {
    it('does NOT re-set running for the focused thread when the probe still reports in-flight (just-cancelled race)', () => {
        const decision = reconcileProbedTaskActivity({
            currentActivity: undefined,
            isFocused: true,
            isInFlight: true,
            isRunningByTask: false,
        });

        expect(decision.setActivityRunning).toBeFalsy();
    });

    it('sets running for a NON-focused (background) thread the probe reports in-flight with no activity yet', () => {
        const decision = reconcileProbedTaskActivity({
            currentActivity: undefined,
            isFocused: false,
            isInFlight: true,
            isRunningByTask: false,
        });

        expect(decision.setActivityRunning).toBe(true);
    });

    it('preserves paused regardless of probe', () => {
        const decision = reconcileProbedTaskActivity({
            currentActivity: 'paused',
            isFocused: false,
            isInFlight: false,
            isRunningByTask: true,
        });

        expect(decision).toEqual({});
    });

    it('clears a focused running dot once the runtime has released the task (not in flight, runningByTask false)', () => {
        const decision = reconcileProbedTaskActivity({
            currentActivity: 'running',
            isFocused: true,
            isInFlight: false,
            isRunningByTask: false,
        });

        expect(decision.clearActivity).toBe(true);
    });

    it('does not clear a focused running dot while the runtime still owns it (runningByTask true)', () => {
        const decision = reconcileProbedTaskActivity({
            currentActivity: 'running',
            isFocused: true,
            isInFlight: false,
            isRunningByTask: true,
        });

        expect(decision).toEqual({});
    });

    it('clears both the running flag and the dot for a finished background task', () => {
        const decision = reconcileProbedTaskActivity({
            currentActivity: 'running',
            isFocused: false,
            isInFlight: false,
            isRunningByTask: true,
        });

        expect(decision.clearRunningFlag).toBe(true);
        expect(decision.clearActivity).toBe(true);
    });
});

describe('getTasksPage', () => {
    const tasks = Array.from({length: TASKS_PAGE_SIZE * 2 + 5}, (_, index) => buildTask({id: index + 1}));

    it('returns all tasks with no hidden count when the list fits within the visible window', () => {
        const {hiddenCount, visibleTasks} = getTasksPage(tasks.slice(0, 5), TASKS_PAGE_SIZE);

        expect(visibleTasks).toHaveLength(5);
        expect(hiddenCount).toBe(0);
    });

    it('caps the visible tasks at the window size and reports the remainder as hidden', () => {
        const {hiddenCount, visibleTasks} = getTasksPage(tasks, TASKS_PAGE_SIZE);

        expect(visibleTasks).toHaveLength(TASKS_PAGE_SIZE);
        expect(hiddenCount).toBe(TASKS_PAGE_SIZE + 5);
    });

    it('reveals the next page after the window grows', () => {
        const {hiddenCount, visibleTasks} = getTasksPage(tasks, TASKS_PAGE_SIZE * 2);

        expect(visibleTasks).toHaveLength(TASKS_PAGE_SIZE * 2);
        expect(hiddenCount).toBe(5);
    });

    it('handles an empty list', () => {
        const {hiddenCount, visibleTasks} = getTasksPage([], TASKS_PAGE_SIZE);

        expect(visibleTasks).toHaveLength(0);
        expect(hiddenCount).toBe(0);
    });
});
