import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {usePersistJobId} from '@/shared/hooks/usePersistJobId';
import {WorkflowTestApi} from '@/shared/middleware/platform/workflow/test';
import {useCallback, useEffect, useRef, useState} from 'react';
import {useBlocker} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const workflowTestApi = new WorkflowTestApi();

export function useWorkflowTestRunGuard(workflowId?: string, currentEnvironmentId?: number) {
    const [showLeaveDialog, setShowLeaveDialog] = useState(false);
    const [pendingAction, setPendingAction] = useState<null | 'reload' | 'nav'>(null);

    const {workflowIsRunning, workflowTestExecution} = useWorkflowEditorStore(
        useShallow((state) => ({
            workflowIsRunning: state.workflowIsRunning,
            workflowTestExecution: state.workflowTestExecution,
        }))
    );

    const {getPersistedJobId, persistJobId} = usePersistJobId(workflowId, currentEnvironmentId);

    const isUnloadingRef = useRef(false);
    const latestRunningRef = useRef<boolean>(workflowIsRunning);
    const navConfirmedRef = useRef(false);
    const suppressBeforeUnloadPromptRef = useRef(false);

    const blocker = useBlocker(workflowIsRunning);

    // Block SPA navigation when a workflow execution is active and show the same leave dialog
    const cancelLeave = useCallback(() => {
        setPendingAction(null);
        setShowLeaveDialog(false);

        if (blocker && blocker.state === 'blocked' && typeof blocker.reset === 'function') {
            blocker.reset();
        }
    }, [blocker]);

    const confirmLeave = useCallback(async () => {
        const jobId = workflowTestExecution?.job?.id ?? getPersistedJobId();

        setShowLeaveDialog(false);

        if (jobId) {
            await workflowTestApi.stopWorkflowTest({jobId}).finally(() => persistJobId(null));
        }

        if (pendingAction === 'reload') {
            latestRunningRef.current = false;
            isUnloadingRef.current = true;
            suppressBeforeUnloadPromptRef.current = true;
            window.location.reload();
        } else if (pendingAction === 'nav') {
            latestRunningRef.current = false;
            navConfirmedRef.current = true;

            if (blocker && typeof blocker.proceed === 'function') {
                blocker.proceed();
            }
        }

        setPendingAction(null);
    }, [blocker, getPersistedJobId, pendingAction, persistJobId, workflowTestExecution?.job?.id]);

    const onBeforeUnload = useCallback((event: BeforeUnloadEvent) => {
        if (!latestRunningRef.current) {
            return;
        }

        if (suppressBeforeUnloadPromptRef.current) {
            return;
        }

        event.preventDefault();
        event.returnValue = '';
    }, []);

    const onKeyDown = useCallback((event: KeyboardEvent) => {
        if (!latestRunningRef.current) {
            return;
        }

        const key = event.key.toLowerCase();

        const isReloadKey = key === 'f5' || ((event.ctrlKey || event.metaKey) && key === 'r');

        if (isReloadKey) {
            event.preventDefault();
            event.stopPropagation();
            setPendingAction('reload');
            setShowLeaveDialog(true);
        }
    }, []);

    const stopBestEffort = useCallback(async () => {
        const jobId = workflowTestExecution?.job?.id ?? getPersistedJobId();

        if (!jobId) {
            return;
        }

        await workflowTestApi.stopWorkflowTest({jobId}).finally(() => persistJobId(null));
    }, [getPersistedJobId, persistJobId, workflowTestExecution?.job?.id]);

    const onPageHide = useCallback(async () => {
        isUnloadingRef.current = true;

        await stopBestEffort();
    }, [stopBestEffort]);

    const onVisibilityChange = useCallback(async () => {
        if (document.visibilityState === 'hidden') {
            isUnloadingRef.current = true;

            await stopBestEffort();
        }
    }, [stopBestEffort]);

    useEffect(() => {
        latestRunningRef.current = workflowIsRunning;
    }, [workflowIsRunning]);

    // Intercept keyboard reload to show the custom dialog
    useEffect(() => {
        window.addEventListener('keydown', onKeyDown);
        window.addEventListener('beforeunload', onBeforeUnload);
        window.addEventListener('pagehide', onPageHide);
        document.addEventListener('visibilitychange', onVisibilityChange);

        return () => {
            window.removeEventListener('keydown', onKeyDown);
            window.removeEventListener('beforeunload', onBeforeUnload);
            window.removeEventListener('pagehide', onPageHide);
            document.removeEventListener('visibilitychange', onVisibilityChange);

            if (isUnloadingRef.current) {
                return;
            }

            // If we already confirmed a navigation and stopped explicitly, skip here to avoid double-stop
            if (navConfirmedRef.current) {
                return;
            }

            if (!latestRunningRef.current) {
                return;
            }

            const jobId = workflowTestExecution?.job?.id ?? getPersistedJobId();

            if (jobId) {
                workflowTestApi.stopWorkflowTest({jobId}).finally(() => persistJobId(null));
            }
        };
    }, [
        onBeforeUnload,
        onKeyDown,
        onPageHide,
        onVisibilityChange,
        getPersistedJobId,
        persistJobId,
        workflowTestExecution?.job?.id,
    ]);

    useEffect(() => {
        if (blocker && blocker.state === 'blocked' && latestRunningRef.current) {
            setPendingAction('nav');
            setShowLeaveDialog(true);
        }
    }, [blocker, blocker?.state]);

    return {
        cancelLeave,
        confirmLeave,
        setShowLeaveDialog,
        showLeaveDialog,
        workflowIsRunning,
        workflowTestExecution,
    };
}
