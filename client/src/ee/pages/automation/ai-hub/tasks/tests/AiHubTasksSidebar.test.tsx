import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {aiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {
    TASKS_PAGE_SIZE,
    cancelTaskRunIfStreaming,
    getTasksPage,
    handleArtifactQuickOpen,
    openArtifactInTask,
    reconcileProbedTaskActivity,
} from '../AiHubTasksSidebar';
import {AiHubTaskArtifactI, AiHubTaskI} from '../api/tasks.api';

const {mockGetProject, mockToastError} = vi.hoisted(() => ({
    mockGetProject: vi.fn(),
    mockToastError: vi.fn(),
}));

vi.mock('@/shared/middleware/automation/configuration', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/automation/configuration');

    class MockProjectApi {
        getProject = mockGetProject;
    }

    return {
        ...actual,
        ProjectApi: MockProjectApi,
    };
});

vi.mock('sonner', () => ({
    toast: {
        error: mockToastError,
    },
}));

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

function buildFileArtifact(overrides: Partial<AiHubTaskArtifactI> = {}): AiHubTaskArtifactI {
    return {
        artifactId: 'file-1',
        artifactName: 'a.txt',
        createdAt: new Date().toISOString(),
        id: 1,
        kind: 'FILE_CREATED',
        metadataJson: null,
        status: 'APPLIED',
        taskId: 1,
        ...overrides,
    };
}

describe('handleArtifactQuickOpen', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({
            activeTabId: undefined,
            activeTaskId: undefined,
            openTabs: [],
            rightPanelOpen: false,
            snapshotsByTaskId: {},
            tasksSidebarCollapsed: true,
        });
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('opens a workflowExecution tab for a WORKFLOW_EXECUTION_STARTED artifact and does not call window.open', () => {
        const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);

        const workflowExecutionArtifact: AiHubTaskArtifactI = {
            artifactId: '777',
            artifactName: 'Run #777',
            createdAt: new Date().toISOString(),
            id: 1,
            kind: 'WORKFLOW_EXECUTION_STARTED',
            metadataJson: null,
            status: 'APPLIED',
            taskId: 42,
        };

        handleArtifactQuickOpen(workflowExecutionArtifact);

        const openTabs = aiHubTabsStore.getState().openTabs;

        expect(openTabs).toHaveLength(1);

        const openedTab = openTabs[0]!;

        expect(openedTab.kind).toBe('workflowExecution');

        if (openedTab.kind === 'workflowExecution') {
            expect(openedTab.workflowExecutionId).toBe(777);
            expect(openedTab.name).toBe('Run #777');
        }

        expect(aiHubTabsStore.getState().rightPanelOpen).toBe(true);

        expect(openSpy).not.toHaveBeenCalled();
    });

    it('opens a skill tab for a SKILL_REFERENCED artifact using artifactId as the skillId', () => {
        const skillArtifact: AiHubTaskArtifactI = {
            artifactId: '7',
            artifactName: 'Triage',
            createdAt: new Date().toISOString(),
            id: 1,
            kind: 'SKILL_REFERENCED',
            metadataJson: null,
            status: 'APPLIED',
            taskId: 42,
        };

        handleArtifactQuickOpen(skillArtifact);

        const openTabs = aiHubTabsStore.getState().openTabs;

        expect(openTabs).toHaveLength(1);

        const openedTab = openTabs[0]!;

        expect(openedTab.kind).toBe('skill');

        if (openedTab.kind === 'skill') {
            expect(openedTab.skillId).toBe('7');
            expect(openedTab.name).toBe('Triage');
        }
    });

    it('opens a custom component tab for a CUSTOM_COMPONENT_REFERENCED artifact using artifactId as the customComponentId', () => {
        const customComponentArtifact: AiHubTaskArtifactI = {
            artifactId: '9',
            artifactName: 'My Component',
            createdAt: new Date().toISOString(),
            id: 1,
            kind: 'CUSTOM_COMPONENT_REFERENCED',
            metadataJson: null,
            status: 'APPLIED',
            taskId: 42,
        };

        handleArtifactQuickOpen(customComponentArtifact);

        const openTabs = aiHubTabsStore.getState().openTabs;

        expect(openTabs).toHaveLength(1);

        const openedTab = openTabs[0]!;

        expect(openedTab.kind).toBe('customComponent');

        if (openedTab.kind === 'customComponent') {
            expect(openedTab.customComponentId).toBe('9');
            expect(openedTab.name).toBe('My Component');
        }
    });

    describe('CODE_WORKFLOW_REFERENCED', () => {
        function buildCodeWorkflowArtifact(overrides: Partial<AiHubTaskArtifactI> = {}): AiHubTaskArtifactI {
            return {
                artifactId: '11',
                artifactName: 'My Code Workflow',
                createdAt: new Date().toISOString(),
                id: 1,
                kind: 'CODE_WORKFLOW_REFERENCED',
                metadataJson: null,
                status: 'APPLIED',
                taskId: 42,
                ...overrides,
            };
        }

        beforeEach(() => {
            mockGetProject.mockReset();
            mockToastError.mockReset();
        });

        it('fetches the project and opens a codeWorkflow tab using its codeWorkflowLanguage, using artifactId as the projectId', async () => {
            mockGetProject.mockResolvedValue({codeWorkflowLanguage: 'PYTHON', id: 11});

            await handleArtifactQuickOpen(buildCodeWorkflowArtifact());

            expect(mockGetProject).toHaveBeenCalledWith({id: 11});

            const openTabs = aiHubTabsStore.getState().openTabs;

            expect(openTabs).toHaveLength(1);

            const openedTab = openTabs[0]!;

            expect(openedTab.kind).toBe('codeWorkflow');

            if (openedTab.kind === 'codeWorkflow') {
                expect(openedTab.projectId).toBe('11');
                expect(openedTab.language).toBe('PYTHON');
                expect(openedTab.name).toBe('My Code Workflow');
            }

            expect(mockToastError).not.toHaveBeenCalled();
        });

        it('surfaces a toast and does not open a tab when the project has no codeWorkflowLanguage', async () => {
            mockGetProject.mockResolvedValue({id: 11});

            await handleArtifactQuickOpen(buildCodeWorkflowArtifact());

            expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
            expect(mockToastError).toHaveBeenCalledWith(expect.stringContaining('is no longer a code workflow'));
        });

        it('surfaces a toast and does not open a tab when the project fetch fails', async () => {
            mockGetProject.mockRejectedValue(new Error('network down'));

            await handleArtifactQuickOpen(buildCodeWorkflowArtifact());

            expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
            expect(mockToastError).toHaveBeenCalledWith(expect.stringContaining('network down'));
        });
    });
});

describe('openArtifactInTask', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({
            activeTabId: undefined,
            activeTaskId: undefined,
            openTabs: [],
            rightPanelOpen: false,
            snapshotsByTaskId: {},
            tasksSidebarCollapsed: true,
        });
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('opens the resource without switching when the artifact belongs to the current task', async () => {
        const switchTask = vi.fn();
        const task = buildTask({id: 42, threadId: 'thread-42'});
        const artifact = buildFileArtifact({artifactId: 'file-42', taskId: 42});

        await openArtifactInTask(artifact, task, 42, switchTask);

        expect(switchTask).not.toHaveBeenCalled();

        const openTabs = aiHubTabsStore.getState().openTabs;

        expect(openTabs).toHaveLength(1);
        expect(openTabs[0]!.kind).toBe('file');
    });

    it('switches to the owning task first, then opens the resource on that task tab set', async () => {
        // Seed: task 1 is active with one tab already open.
        const existingTabId = aiHubTabsStore.getState().openFileTab('file-1', 'a.txt');

        aiHubTabsStore.setState({activeTaskId: 1});

        const switchTask = vi.fn().mockResolvedValue(true);
        const task = buildTask({id: 2, threadId: 'thread-2'});
        const artifact = buildFileArtifact({artifactId: 'file-2', artifactName: 'b.txt', taskId: 2});

        await openArtifactInTask(artifact, task, 1, switchTask);

        expect(switchTask).toHaveBeenCalledWith(task);

        const state = aiHubTabsStore.getState();

        // The tabs store is now mirroring task 2, and the resource opened there — not on task 1.
        expect(state.activeTaskId).toBe(2);
        expect(state.openTabs).toHaveLength(1);

        const openedTab = state.openTabs[0]!;

        expect(openedTab.kind).toBe('file');

        if (openedTab.kind === 'file') {
            expect(openedTab.fileId).toBe('file-2');
        }

        // Task 1's tab was snapshotted away, not lost.
        expect(state.snapshotsByTaskId[1]?.openTabs).toHaveLength(1);
        expect(state.snapshotsByTaskId[1]?.openTabs[0]!.id).toBe(existingTabId);
    });

    it('does not open the resource when the task switch fails', async () => {
        aiHubTabsStore.setState({activeTaskId: 1});

        const switchTask = vi.fn().mockResolvedValue(false);
        const task = buildTask({id: 2, threadId: 'thread-2'});
        const artifact = buildFileArtifact({artifactId: 'file-2', taskId: 2});

        await openArtifactInTask(artifact, task, 1, switchTask);

        expect(switchTask).toHaveBeenCalledWith(task);
        expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
    });
});

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
