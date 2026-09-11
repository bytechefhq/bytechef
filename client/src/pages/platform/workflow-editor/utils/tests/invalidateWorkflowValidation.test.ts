import {QueryClient} from '@tanstack/react-query';
import {describe, expect, it, vi} from 'vitest';

import invalidateWorkflowValidation, {WORKFLOW_VALIDATION_QUERY_KEY} from '../invalidateWorkflowValidation';

describe('invalidateWorkflowValidation', () => {
    it('invalidates every ValidateWorkflow query regardless of its variables', () => {
        const queryClient = new QueryClient();
        const invalidateQueries = vi.spyOn(queryClient, 'invalidateQueries');

        invalidateWorkflowValidation(queryClient);

        expect(invalidateQueries).toHaveBeenCalledWith({queryKey: WORKFLOW_VALIDATION_QUERY_KEY});
    });
});
