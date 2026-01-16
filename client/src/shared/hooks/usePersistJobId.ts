import {useCallback} from 'react';

export const usePersistJobId = (workflowId?: string, environmentId?: number) => {
    const getStorageKey = useCallback(
        () =>
            workflowId && environmentId != null ? `bytechef.workflow-test-run.${workflowId}:${environmentId}` : null,
        [environmentId, workflowId]
    );

    const getPersistedJobId = useCallback(() => {
        const key = getStorageKey();

        if (!key) {
            return null;
        }

        try {
            return localStorage.getItem(key);
        } catch {
            return null;
        }
    }, [getStorageKey]);

    const persistJobId = useCallback(
        (jobId: string | null) => {
            const key = getStorageKey();

            if (!key) {
                return;
            }

            if (jobId) {
                try {
                    localStorage.setItem(key, jobId);
                } catch (error) {
                    console.error('Failed to persist job ID:', error);
                }
            } else {
                try {
                    localStorage.removeItem(key);
                } catch {
                    // ignore
                }
            }
        },
        [getStorageKey]
    );

    return {
        getPersistedJobId,
        persistJobId,
    };
};
