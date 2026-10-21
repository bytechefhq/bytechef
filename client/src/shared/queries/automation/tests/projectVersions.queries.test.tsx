import {createTestQueryClientWrapper, renderHook, resetAll, waitFor} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {ProjectVersionKeys, useGetProjectWorkflowVersionsQuery} from '../projectVersions.queries';

const {getProjectWorkflowVersionsMock} = vi.hoisted(() => ({getProjectWorkflowVersionsMock: vi.fn()}));

vi.mock('@/shared/middleware/automation/configuration', () => ({
    ProjectApi: class {},
    WorkflowApi: class {
        getProjectWorkflowVersions = getProjectWorkflowVersionsMock;
    },
}));

describe('projectVersions.queries', () => {
    afterEach(() => {
        resetAll();
    });

    it('keys the workflow versions under the project versions, so invalidating a project refreshes them', () => {
        expect(ProjectVersionKeys.projectWorkflowVersions(7, 'workflow-uuid')).toEqual([
            'projectVersions',
            'projects',
            7,
            'workflows',
            'workflow-uuid',
        ]);
    });

    it('asks for the published versions that contain the workflow', async () => {
        getProjectWorkflowVersionsMock.mockResolvedValue([{version: 2}]);

        const {result} = renderHook(() => useGetProjectWorkflowVersionsQuery(7, 'workflow-uuid'), {
            wrapper: createTestQueryClientWrapper(),
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(getProjectWorkflowVersionsMock).toHaveBeenCalledWith({id: 7, workflowUuid: 'workflow-uuid'});
        expect(result.current.data).toEqual([{version: 2}]);
    });

    it('does not fetch when disabled', () => {
        renderHook(() => useGetProjectWorkflowVersionsQuery(7, 'workflow-uuid', false), {
            wrapper: createTestQueryClientWrapper(),
        });

        expect(getProjectWorkflowVersionsMock).not.toHaveBeenCalled();
    });
});
