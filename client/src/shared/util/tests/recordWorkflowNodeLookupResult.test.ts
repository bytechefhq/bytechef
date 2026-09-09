import useWorkflowIssuesStore from '@/pages/platform/workflow-editor/stores/useWorkflowIssuesStore';
import recordWorkflowNodeLookupResult from '@/shared/util/recordWorkflowNodeLookupResult';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const optionsUrl = 'http://localhost/internal/workflows/1/workflow-nodes/dataTable_1/options/table';

const createResponse = (status: number, body: {detail?: string; title?: string} = {}, rejects = false) => ({
    clone: () => ({
        json: () => (rejects ? Promise.reject(new Error('not json')) : Promise.resolve(body)),
    }),
    status,
    url: optionsUrl,
});

describe('recordWorkflowNodeLookupResult', () => {
    beforeEach(() => {
        useWorkflowIssuesStore.setState({liveIssues: {}});
    });

    it('ignores a url that is not a node lookup', () => {
        const handled = recordWorkflowNodeLookupResult({
            clone: () => ({json: () => Promise.resolve({})}),
            status: 500,
            url: 'http://localhost/internal/workflows/1',
        });

        expect(handled).toBe(false);
        expect(Object.keys(useWorkflowIssuesStore.getState().liveIssues)).toHaveLength(0);
    });

    it('clears a recorded failure once the lookup succeeds', () => {
        const clearLookupFailure = vi.spyOn(useWorkflowIssuesStore.getState(), 'clearLookupFailure');

        expect(recordWorkflowNodeLookupResult(createResponse(200))).toBe(true);
        expect(clearLookupFailure).toHaveBeenCalledWith('dataTable_1', 'table');
    });

    it('records the server reason for a failed lookup', async () => {
        expect(recordWorkflowNodeLookupResult(createResponse(400, {detail: 'Table is gone'}))).toBe(true);

        await vi.waitFor(() => {
            const issues = Object.values(useWorkflowIssuesStore.getState().liveIssues);

            expect(issues[0]?.message).toBe('Table is gone');
        });
    });

    it('falls back to the status when the body carries no reason', async () => {
        recordWorkflowNodeLookupResult(createResponse(503, {}, true));

        await vi.waitFor(() => {
            const issues = Object.values(useWorkflowIssuesStore.getState().liveIssues);

            expect(issues[0]?.message).toBe('Request failed with status 503');
        });
    });
});
