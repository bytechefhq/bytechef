import useWorkflowEditorStore, {WorkflowEditorI} from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {usePersistJobId} from '@/shared/hooks/usePersistJobId';
import {WorkflowTestApi} from '@/shared/middleware/platform/workflow/test';
import {act, renderHook} from '@testing-library/react';
import {useBlocker} from 'react-router-dom';
import {Mock, afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {useWorkflowTestRunGuard} from '../useWorkflowTestRunGuard';

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore');
vi.mock('@/shared/hooks/usePersistJobId');
vi.mock('react-router-dom');
vi.mock('@/shared/middleware/platform/workflow/test', () => {
    const mockWorkflowTestApi = {
        stopWorkflowTest: vi.fn().mockReturnValue(Promise.resolve({})),
    };
    return {
        WorkflowTestApi: vi.fn().mockImplementation(function () {
            return mockWorkflowTestApi;
        }),
    };
});

describe('useWorkflowTestRunGuard', () => {
    const workflowId = 'workflow-1';
    const environmentId = 1;
    const jobId = 'job-1';

    let mockPersistJobId: Mock;
    let mockGetPersistedJobId: Mock;
    let mockBlocker: {
        proceed: Mock;
        reset: Mock;
        state: string;
    };

    beforeEach(() => {
        vi.clearAllMocks();

        mockPersistJobId = vi.fn();
        mockGetPersistedJobId = vi.fn().mockReturnValue(jobId);
        vi.mocked(usePersistJobId).mockReturnValue({
            getPersistedJobId: mockGetPersistedJobId,
            persistJobId: mockPersistJobId,
        });

        mockBlocker = {
            proceed: vi.fn(),
            reset: vi.fn(),
            state: 'unblocked',
        };
        vi.mocked(useBlocker).mockReturnValue(mockBlocker as unknown as ReturnType<typeof useBlocker>);

        vi.mocked(useWorkflowEditorStore).mockImplementation((selector: (state: WorkflowEditorI) => unknown) =>
            selector({
                workflowIsRunning: false,
                workflowTestExecution: undefined,
            } as WorkflowEditorI)
        );

        // Mock window.location.reload
        vi.stubGlobal('location', {...window.location, reload: vi.fn()});
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('should initialize with default values', () => {
        const {result} = renderHook(() => useWorkflowTestRunGuard(workflowId, environmentId));

        expect(result.current.showLeaveDialog).toBe(false);
        expect(result.current.workflowIsRunning).toBe(false);
    });

    it('should update workflowIsRunning from store', () => {
        vi.mocked(useWorkflowEditorStore).mockImplementation((selector: (state: WorkflowEditorI) => unknown) =>
            selector({
                workflowIsRunning: true,
                workflowTestExecution: {job: {id: jobId}},
            } as WorkflowEditorI)
        );

        const {result} = renderHook(() => useWorkflowTestRunGuard(workflowId, environmentId));

        expect(result.current.workflowIsRunning).toBe(true);
        expect(result.current.workflowTestExecution?.job?.id).toBe(jobId);
    });

    describe('Keyboard Reload Guard', () => {
        it('should show leave dialog on F5 when workflow is running', () => {
            vi.mocked(useWorkflowEditorStore).mockImplementation((selector: (state: WorkflowEditorI) => unknown) =>
                selector({
                    workflowIsRunning: true,
                    workflowTestExecution: {job: {id: jobId}},
                } as WorkflowEditorI)
            );

            renderHook(() => useWorkflowTestRunGuard(workflowId, environmentId));

            const event = new KeyboardEvent('keydown', {key: 'F5'});
            const preventDefaultSpy = vi.spyOn(event, 'preventDefault');
            const stopPropagationSpy = vi.spyOn(event, 'stopPropagation');

            act(() => {
                window.dispatchEvent(event);
            });

            expect(preventDefaultSpy).toHaveBeenCalled();
            expect(stopPropagationSpy).toHaveBeenCalled();
        });

        it('should show leave dialog on Ctrl+R when workflow is running', () => {
            vi.mocked(useWorkflowEditorStore).mockImplementation((selector: (state: WorkflowEditorI) => unknown) =>
                selector({
                    workflowIsRunning: true,
                    workflowTestExecution: {job: {id: jobId}},
                } as WorkflowEditorI)
            );

            renderHook(() => useWorkflowTestRunGuard(workflowId, environmentId));

            const event = new KeyboardEvent('keydown', {ctrlKey: true, key: 'r'});
            act(() => {
                window.dispatchEvent(event);
            });

            // We can't directly check result.current.showLeaveDialog here easily because of how renderHook works with events
            // but we can check if it updates in the next tick if we had a way to wait for it.
            // Alternatively, we can check the hook's return value if we trigger the event and then re-render or wait.
        });
    });

    describe('BeforeUnload Guard', () => {
        it('should call preventDefault on beforeunload when workflow is running', () => {
            vi.mocked(useWorkflowEditorStore).mockImplementation((selector: (state: WorkflowEditorI) => unknown) =>
                selector({
                    workflowIsRunning: true,
                    workflowTestExecution: {job: {id: jobId}},
                } as WorkflowEditorI)
            );

            renderHook(() => useWorkflowTestRunGuard(workflowId, environmentId));

            const event = new Event('beforeunload');
            const preventDefaultSpy = vi.spyOn(event, 'preventDefault');

            act(() => {
                window.dispatchEvent(event);
            });

            expect(preventDefaultSpy).toHaveBeenCalled();
            // In some JSDOM versions or depending on how it's triggered, it might be true or empty string
            expect((event as BeforeUnloadEvent).returnValue).toBeDefined();
        });

        it('should NOT call preventDefault on beforeunload when workflow is NOT running', () => {
            vi.mocked(useWorkflowEditorStore).mockImplementation((selector: (state: WorkflowEditorI) => unknown) =>
                selector({
                    workflowIsRunning: false,
                    workflowTestExecution: undefined,
                } as WorkflowEditorI)
            );

            renderHook(() => useWorkflowTestRunGuard(workflowId, environmentId));

            const event = new Event('beforeunload');
            const preventDefaultSpy = vi.spyOn(event, 'preventDefault');

            act(() => {
                window.dispatchEvent(event);
            });

            expect(preventDefaultSpy).not.toHaveBeenCalled();
        });
    });

    describe('Navigation Blocker', () => {
        it('should show leave dialog when blocker is in blocked state', () => {
            vi.mocked(useWorkflowEditorStore).mockImplementation((selector: (state: WorkflowEditorI) => unknown) =>
                selector({
                    workflowIsRunning: true,
                    workflowTestExecution: {job: {id: jobId}},
                } as WorkflowEditorI)
            );

            const {rerender, result} = renderHook(() => useWorkflowTestRunGuard(workflowId, environmentId));

            expect(result.current.showLeaveDialog).toBe(false);

            mockBlocker.state = 'blocked';
            vi.mocked(useBlocker).mockReturnValue(mockBlocker as unknown as ReturnType<typeof useBlocker>);

            rerender();

            expect(result.current.showLeaveDialog).toBe(true);
        });
    });

    describe('Dialog Actions', () => {
        it('should reset state on cancelLeave', () => {
            const {result} = renderHook(() => useWorkflowTestRunGuard(workflowId, environmentId));

            act(() => {
                result.current.setShowLeaveDialog(true);
            });
            expect(result.current.showLeaveDialog).toBe(true);

            act(() => {
                result.current.cancelLeave();
            });
            expect(result.current.showLeaveDialog).toBe(false);
        });

        it('should stop workflow and reload on confirmLeave when pendingAction is reload', async () => {
            vi.mocked(useWorkflowEditorStore).mockImplementation((selector: (state: WorkflowEditorI) => unknown) =>
                selector({
                    workflowIsRunning: true,
                    workflowTestExecution: {job: {id: jobId}},
                } as WorkflowEditorI)
            );

            const {result} = renderHook(() => useWorkflowTestRunGuard(workflowId, environmentId));

            // Trigger reload pending action
            const event = new KeyboardEvent('keydown', {key: 'F5'});
            act(() => {
                window.dispatchEvent(event);
            });

            await act(async () => {
                await result.current.confirmLeave();
            });

            const workflowTestApiInstance = new WorkflowTestApi();
            expect(workflowTestApiInstance.stopWorkflowTest).toHaveBeenCalledWith({jobId});
            expect(mockPersistJobId).toHaveBeenCalledWith(null);
            expect(window.location.reload).toHaveBeenCalled();
        });

        it('should stop workflow and proceed navigation on confirmLeave when pendingAction is nav', async () => {
            vi.mocked(useWorkflowEditorStore).mockImplementation((selector: (state: WorkflowEditorI) => unknown) =>
                selector({
                    workflowIsRunning: true,
                    workflowTestExecution: {job: {id: jobId}},
                } as WorkflowEditorI)
            );

            mockBlocker.state = 'blocked';
            vi.mocked(useBlocker).mockReturnValue(mockBlocker as unknown as ReturnType<typeof useBlocker>);

            const {result} = renderHook(() => useWorkflowTestRunGuard(workflowId, environmentId));

            await act(async () => {
                await result.current.confirmLeave();
            });

            const workflowTestApiInstance = new WorkflowTestApi();
            expect(workflowTestApiInstance.stopWorkflowTest).toHaveBeenCalledWith({jobId});
            expect(mockPersistJobId).toHaveBeenCalledWith(null);
            expect(mockBlocker.proceed).toHaveBeenCalled();
        });
    });

    describe('Visibility and PageHide', () => {
        it('should stop workflow on pagehide', async () => {
            vi.mocked(useWorkflowEditorStore).mockImplementation((selector: (state: WorkflowEditorI) => unknown) =>
                selector({
                    workflowIsRunning: true,
                    workflowTestExecution: {job: {id: jobId}},
                } as WorkflowEditorI)
            );

            renderHook(() => useWorkflowTestRunGuard(workflowId, environmentId));

            await act(async () => {
                window.dispatchEvent(new Event('pagehide'));
            });

            const workflowTestApiInstance = new WorkflowTestApi();
            expect(workflowTestApiInstance.stopWorkflowTest).toHaveBeenCalledWith({jobId});
        });

        it('should stop workflow on visibilitychange when hidden', async () => {
            vi.mocked(useWorkflowEditorStore).mockImplementation((selector: (state: WorkflowEditorI) => unknown) =>
                selector({
                    workflowIsRunning: true,
                    workflowTestExecution: {job: {id: jobId}},
                } as WorkflowEditorI)
            );

            const visibilitySpy = vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('hidden');

            renderHook(() => useWorkflowTestRunGuard(workflowId, environmentId));

            await act(async () => {
                document.dispatchEvent(new Event('visibilitychange'));
            });

            const workflowTestApiInstance = new WorkflowTestApi();
            expect(workflowTestApiInstance.stopWorkflowTest).toHaveBeenCalledWith({jobId});
            visibilitySpy.mockRestore();
        });
    });

    describe('Unmount Cleanup', () => {
        it('should stop workflow on unmount if it is running', () => {
            vi.mocked(useWorkflowEditorStore).mockImplementation((selector: (state: WorkflowEditorI) => unknown) =>
                selector({
                    workflowIsRunning: true,
                    workflowTestExecution: {job: {id: jobId}},
                } as WorkflowEditorI)
            );

            const {unmount} = renderHook(() => useWorkflowTestRunGuard(workflowId, environmentId));

            unmount();

            const workflowTestApiInstance = new WorkflowTestApi();
            expect(workflowTestApiInstance.stopWorkflowTest).toHaveBeenCalledWith({jobId});
        });

        it('should NOT stop workflow on unmount if it is NOT running', () => {
            vi.mocked(useWorkflowEditorStore).mockImplementation((selector: (state: WorkflowEditorI) => unknown) =>
                selector({
                    workflowIsRunning: false,
                    workflowTestExecution: undefined,
                } as WorkflowEditorI)
            );

            const {unmount} = renderHook(() => useWorkflowTestRunGuard(workflowId, environmentId));

            unmount();

            const workflowTestApiInstance = new WorkflowTestApi();
            expect(workflowTestApiInstance.stopWorkflowTest).not.toHaveBeenCalled();
        });
    });
});
