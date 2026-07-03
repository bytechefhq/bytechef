import {
    WorkflowDataStoreApiType,
    WorkflowDataStoreContext,
    createWorkflowDataStore,
} from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {ReactNode, useRef} from 'react';

/**
 * Provides a fresh, isolated WorkflowDataStore instance to the subtree.
 * Use this when mounting multiple workflow editors concurrently (e.g. the
 * AI Hub's EmbeddableWorkflowEditor) so each editor has its own
 * store without state bleed.
 *
 * Existing callers of the singleton default export are unaffected.
 */
export function WorkflowDataStoreProvider({children}: {children: ReactNode}) {
    const storeRef = useRef<WorkflowDataStoreApiType>(null);

    if (!storeRef.current) {
        storeRef.current = createWorkflowDataStore();
    }

    return <WorkflowDataStoreContext.Provider value={storeRef.current}>{children}</WorkflowDataStoreContext.Provider>;
}
