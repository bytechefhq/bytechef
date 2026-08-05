import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {aiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubTaskArtifactI, AiHubTaskI, getTaskArtifacts, getTaskMessages} from '../api/tasks.api';

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 99})
    ),
}));

// useSwitchTask gained a useNavigate() call so the route flips when switching tasks from
// any CC page (not just /ai-hub which has its own URL sync). Stub it here so the hook resolves
// cleanly outside a Router context — the tests don't assert on the navigate target since the canonical
// /ai-hub route's URL-sync effect would also have produced the same end state.
vi.mock('react-router-dom', () => ({
    useNavigate: () => vi.fn(),
}));

vi.mock('../api/tasks.api', async () => {
    const actual = await vi.importActual<typeof import('../api/tasks.api')>('../api/tasks.api');

    return {
        ...actual,
        getTaskArtifacts: vi.fn(),
        getTaskMessages: vi.fn(),
    };
});

vi.mock('@/shared/error/useReportQueryError', () => ({
    reportMutationError: vi.fn(),
    useReportQueryError: vi.fn(),
}));

const {useSwitchTask} = await import('./useSwitchTask');
const {reportMutationError} = await import('@/shared/error/useReportQueryError');

const mockGetTaskArtifacts = vi.mocked(getTaskArtifacts);
const mockGetTaskMessages = vi.mocked(getTaskMessages);
const mockReportMutationError = vi.mocked(reportMutationError);

const buildTask = (overrides: Partial<AiHubTaskI> = {}): AiHubTaskI => ({
    aiHubPersonalAgentId: null,
    autoTitled: false,
    createdAt: '2026-04-01T00:00:00Z',
    id: 7,
    kind: 'STANDARD',
    lastPreview: null,
    messageCount: 0,
    status: 'ACTIVE',
    threadId: 'thread-target',
    title: 'Target',
    updatedAt: '2026-04-01T00:00:00Z',
    userId: 1,
    workflowExecutionId: null,
    workspaceId: 99,
    ...overrides,
});

describe('useSwitchTask', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        // Most tests exercise message mapping only; the artifact-link-card reconstruction is opt-in
        // per test via mockGetTaskArtifacts.mockResolvedValue([...]). Default to an empty list so
        // Promise.all's getTaskArtifacts leg resolves cleanly instead of returning undefined.
        mockGetTaskArtifacts.mockResolvedValue([]);

        aiHubStore.setState({messages: [], taskId: 'thread-source'});
        aiHubTasksStore.setState({currentTaskId: 1});
    });

    it('updates command center store before tasks store', async () => {
        // Ordering matters: if tasks-store flips first, a consumer keyed on currentTaskId
        // will render the new task header against the *previous* messages for one frame.
        const observedThreadIdAtTasksFlip: Array<string | undefined> = [];

        const originalSetId = aiHubTasksStore.getState().setCurrentTaskId;

        aiHubTasksStore.setState({
            setCurrentTaskId: (id) => {
                observedThreadIdAtTasksFlip.push(aiHubStore.getState().taskId);

                originalSetId(id);
            },
        });

        mockGetTaskMessages.mockResolvedValue([
            {content: 'hi', role: 'user', timestamp: '2026-04-01T00:00:00Z', toolEventsJson: null},
        ]);

        const {result} = renderHook(() => useSwitchTask());

        await act(async () => {
            await result.current(buildTask());
        });

        // When the tasks store flipped, the command center store had already been updated to the target
        // thread — that's the contract this test pins.
        expect(observedThreadIdAtTasksFlip).toEqual(['thread-target']);
        expect(aiHubTasksStore.getState().currentTaskId).toBe(7);
        expect(mockReportMutationError).not.toHaveBeenCalled();
    });

    it('maps user/assistant/system roles and filters tool', async () => {
        mockGetTaskMessages.mockResolvedValue([
            {content: 'hi', role: 'user', timestamp: '2026-04-01T00:00:00Z', toolEventsJson: null},
            {content: 'hello', role: 'assistant', timestamp: '2026-04-01T00:00:01Z', toolEventsJson: null},
            {content: 'sys', role: 'system', timestamp: '2026-04-01T00:00:02Z', toolEventsJson: null},
            {content: 'tool-payload', role: 'tool', timestamp: '2026-04-01T00:00:03Z', toolEventsJson: null},
        ]);

        const {result} = renderHook(() => useSwitchTask());

        await act(async () => {
            await result.current(buildTask());
        });

        expect(aiHubStore.getState().taskId).toBe('thread-target');
        expect(aiHubStore.getState().messages).toEqual([
            {content: 'hi', role: 'user'},
            {content: 'hello', role: 'assistant'},
            {content: 'sys', role: 'system'},
        ]);
    });

    it('drops messages with unknown server roles instead of routing them', async () => {
        const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

        mockGetTaskMessages.mockResolvedValue([
            {content: 'kept', role: 'user', timestamp: '2026-04-01T00:00:00Z', toolEventsJson: null},
            // role-drift: server shipped an unknown role; the silent-drop branch must not let it through
            {content: 'dropped', role: 'TOOL_USE', timestamp: '2026-04-01T00:00:01Z', toolEventsJson: null},
            {content: 'tool-payload', role: 'tool', timestamp: '2026-04-01T00:00:02Z', toolEventsJson: null},
        ]);

        const {result} = renderHook(() => useSwitchTask());

        await act(async () => {
            await result.current(buildTask());
        });

        const stored = aiHubStore.getState().messages;

        expect(stored).toHaveLength(1);
        expect(stored[0]).toMatchObject({content: 'kept', role: 'user'});
        expect(consoleSpy).toHaveBeenCalledWith(expect.stringContaining('Unknown message role'));

        consoleSpy.mockRestore();
    });

    it('switches optimistically and reports the failure when getTaskMessages rejects', async () => {
        const failure = new Error('network down');

        mockGetTaskMessages.mockRejectedValue(failure);

        const {result} = renderHook(() => useSwitchTask());

        let returned: boolean | undefined;

        await act(async () => {
            returned = await result.current(buildTask());
        });

        expect(returned).toBe(false);
        expect(mockReportMutationError).toHaveBeenCalledWith('Switch task', failure);

        // The switch is applied immediately (navigate-first) so the click never blocks on the history
        // fetch. A failed fetch therefore still lands the user on the target task — the task is real,
        // only its history failed to load, so the thread is empty and re-clicking retries.
        expect(aiHubStore.getState().taskId).toBe('thread-target');
        expect(aiHubStore.getState().messages).toEqual([]);
        expect(aiHubTasksStore.getState().currentTaskId).toBe(7);
    });

    it('returns true on success so the caller can keep the dialog open on failure', async () => {
        mockGetTaskMessages.mockResolvedValue([
            {content: 'hi', role: 'user', timestamp: '2026-04-01T00:00:00Z', toolEventsJson: null},
        ]);

        const {result} = renderHook(() => useSwitchTask());

        let returned: boolean | undefined;

        await act(async () => {
            returned = await result.current(buildTask());
        });

        expect(returned).toBe(true);
    });

    describe('artifact link card rehydration', () => {
        const buildArtifact = (overrides: Partial<AiHubTaskArtifactI> = {}): AiHubTaskArtifactI => ({
            artifactId: 'artifact-1',
            artifactName: 'Artifact',
            createdAt: '2026-04-01T00:00:00Z',
            id: 1,
            kind: 'CUSTOM_COMPONENT_REFERENCED',
            metadataJson: null,
            status: 'APPLIED',
            taskId: 7,
            ...overrides,
        });

        beforeEach(() => {
            mockGetTaskMessages.mockResolvedValue([]);
        });

        it('rehydrates a CUSTOM_COMPONENT_REFERENCED artifact as an openCustomComponentTab tool-call card', async () => {
            mockGetTaskArtifacts.mockResolvedValue([
                buildArtifact({artifactId: '9', artifactName: 'My Component', kind: 'CUSTOM_COMPONENT_REFERENCED'}),
            ]);

            const {result} = renderHook(() => useSwitchTask());

            await act(async () => {
                await result.current(buildTask());
            });

            const messages = aiHubStore.getState().messages;

            expect(messages).toHaveLength(1);

            const content = messages[0]!.content;

            expect(content).toEqual([
                expect.objectContaining({
                    args: {customComponentId: '9', name: 'My Component'},
                    toolName: 'openCustomComponentTab',
                    type: 'tool-call',
                }),
            ]);
        });

        it('rehydrates a CODE_WORKFLOW_REFERENCED artifact as an openCodeWorkflowTab tool-call card using artifactId as the projectId', async () => {
            mockGetTaskArtifacts.mockResolvedValue([
                buildArtifact({artifactId: '11', artifactName: 'My Code Workflow', kind: 'CODE_WORKFLOW_REFERENCED'}),
            ]);

            const {result} = renderHook(() => useSwitchTask());

            await act(async () => {
                await result.current(buildTask());
            });

            const messages = aiHubStore.getState().messages;

            expect(messages).toHaveLength(1);

            const content = messages[0]!.content;

            // The rehydrated args don't carry `language` — the artifact never stashed it (see
            // AiHubTasksSidebar's openCodeWorkflowArtifact for the live quick-open path that resolves it
            // via a project fetch); this reconstructed card is metadata-only, mirroring
            // CUSTOM_COMPONENT_REFERENCED above.
            expect(content).toEqual([
                expect.objectContaining({
                    args: {name: 'My Code Workflow', projectId: '11'},
                    toolName: 'openCodeWorkflowTab',
                    type: 'tool-call',
                }),
            ]);
        });
    });
});
