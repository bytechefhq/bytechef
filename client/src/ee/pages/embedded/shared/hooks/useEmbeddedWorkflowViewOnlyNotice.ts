import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useEmbeddedScopeState} from '@/shared/hooks/useHasEmbeddedScope';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';

export const useEmbeddedWorkflowViewOnlyNotice = (): boolean => {
    const embeddedScopeStatus = usePermissionStore(
        (state) => state.embeddedScopeStates[DEVELOPMENT_ENVIRONMENT]?.status
    );

    const workflowEditState = useEmbeddedScopeState('WORKFLOW_EDIT', DEVELOPMENT_ENVIRONMENT);

    if (workflowEditState.granted || workflowEditState.editionUnknown) {
        return false;
    }

    return workflowEditState.error || embeddedScopeStatus === 'loaded';
};
