import {createTestQueryClientWrapper, renderHook, resetAll, waitFor} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {useRestoreProjectVersionWorkflowMutation} from '../workflows.mutations';

const {restoreProjectVersionWorkflowMock} = vi.hoisted(() => ({
    restoreProjectVersionWorkflowMock: vi.fn(),
}));

vi.mock('@/shared/middleware/automation/configuration', () => ({
    WorkflowApi: class {
        restoreProjectVersionWorkflow = restoreProjectVersionWorkflowMock;
    },
}));

describe('workflows mutations', () => {
    afterEach(() => {
        resetAll();
    });

    it('sends a single workflow restore through to the workflow API', async () => {
        const onSuccess = vi.fn();

        restoreProjectVersionWorkflowMock.mockResolvedValue(undefined);

        const {result} = renderHook(() => useRestoreProjectVersionWorkflowMutation({onSuccess}), {
            wrapper: createTestQueryClientWrapper(),
        });

        result.current.mutate({id: 7, projectVersion: 1, workflowUuid: 'workflow-uuid'});

        await waitFor(() => expect(onSuccess).toHaveBeenCalled());

        expect(restoreProjectVersionWorkflowMock).toHaveBeenCalledWith({
            id: 7,
            projectVersion: 1,
            workflowUuid: 'workflow-uuid',
        });
    });
});
