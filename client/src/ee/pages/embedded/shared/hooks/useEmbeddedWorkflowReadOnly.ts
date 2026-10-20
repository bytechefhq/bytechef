import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';

export const useEmbeddedWorkflowReadOnly = (): boolean =>
    !useHasEmbeddedScope('WORKFLOW_EDIT', DEVELOPMENT_ENVIRONMENT);
