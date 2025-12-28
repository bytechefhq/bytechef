import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {usePersistJobId} from '@/shared/hooks/usePersistJobId';
import {WorkflowTestApi} from '@/shared/middleware/platform/workflow/test';
import {useEffect, useRef, useState} from 'react';
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

    useEffect(() => {
        latestRunningRef.current = workflowIsRunning;
    }, [workflowIsRunning]);

    useEffect(() => {
        // Intercept keyboard reload to show the custom dialog
        const onKeyDown = (event: KeyboardEvent) => {
            if (!latestRunningRef.current) {
                return;
            }

            const key = event.key.toLowerCase();

            const isReload = key === 'f5' || ((event.ctrlKey || event.metaKey) && key === 'r');

            if (isReload) {
                event.preventDefault();
                event.stopPropagation();
                setPendingAction('reload');
                setShowLeaveDialog(true);
            }
        };

        const stopBestEffort = async () => {
            const jobId = workflowTestExecution?.job?.id ?? getPersistedJobId();

            if (!jobId) {
                return;
            }

            await workflowTestApi.stopWorkflowTest({jobId}).finally(() => persistJobId(null));
        };

        const onBeforeUnload = (e: BeforeUnloadEvent) => {
            if (!latestRunningRef.current) {
                return;
            }

            if (suppressBeforeUnloadPromptRef.current) {
                return;
            }

            // Trigger the native confirmation dialog to prevent closing the page while a job is running
            e.preventDefault();

            // Setting returnValue is required by some browsers to show the prompt
            e.returnValue = '';
        };

        const onPageHide = () => {
            isUnloadingRef.current = true;

            stopBestEffort();
        };

        const onVisibilityChange = () => {
            if (document.visibilityState === 'hidden') {
                isUnloadingRef.current = true;

                stopBestEffort();
            }
        };

        window.addEventListener('keydown', onKeyDown);
        window.addEventListener('beforeunload', onBeforeUnload);
        window.addEventListener('pagehide', onPageHide);
        document.addEventListener('visibilitychange', onVisibilityChange);

        return () => {
            window.removeEventListener('keydown', onKeyDown);
            window.removeEventListener('beforeunload', onBeforeUnload);
            window.removeEventListener('pagehide', onPageHide);
            document.removeEventListener('visibilitychange', onVisibilityChange);

            // Only stop when leaving via SPA navigation (component unmount) – but skip if the page is unloading
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
    }, [getPersistedJobId, persistJobId, workflowTestExecution?.job?.id]);

    // Block SPA navigation when a run is active and show the same leave dialog
    const blocker = useBlocker(workflowIsRunning);

    useEffect(() => {
        if (blocker && blocker.state === 'blocked' && latestRunningRef.current) {
            setPendingAction('nav');
            setShowLeaveDialog(true);
        }
    }, [blocker, blocker?.state]);

    const confirmLeave = async () => {
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
    };

    const cancelLeave = () => {
        setPendingAction(null);
        setShowLeaveDialog(false);

        // Cancel the blocked navigation if any
        if (blocker && blocker.state === 'blocked' && typeof blocker.reset === 'function') {
            blocker.reset();
        }
    };

    return {
        cancelLeave,
        confirmLeave,
        setShowLeaveDialog,
        showLeaveDialog,
        workflowIsRunning,
        workflowTestExecution,
    };
}
