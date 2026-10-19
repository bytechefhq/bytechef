import {act, createTestQueryClientWrapper, renderHook, resetAll} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {useResumeJobMutation} from '../resumeJobs.mutations';

describe('useResumeJobMutation', () => {
    const fetchSpy = vi.fn<typeof fetch>();
    const originalFetch = global.fetch;

    beforeEach(() => {
        global.fetch = fetchSpy;
    });

    afterEach(() => {
        global.fetch = originalFetch;

        resetAll();
    });

    async function submitDecision(status: number, body: string | null = null) {
        fetchSpy.mockResolvedValue(new Response(body, {status}));

        const {result} = renderHook(() => useResumeJobMutation(), {wrapper: createTestQueryClientWrapper()});

        let submission: Promise<void> | undefined;

        act(() => {
            submission = result.current.mutateAsync({approved: true, data: {}, id: 'resume-1'});
        });

        return submission!;
    }

    it('posts the decision to the resume endpoint', async () => {
        await submitDecision(204);

        expect(fetchSpy).toHaveBeenCalledWith('/job/resume/resume-1', {
            body: JSON.stringify({approved: true}),
            headers: {'Content-Type': 'application/json'},
            method: 'POST',
        });
    });

    it('asks the user to try again when the workflow is still preparing', async () => {
        await expect(submitDecision(409)).rejects.toThrow('Still preparing — try again in a moment.');
    });

    it('tells the user the workflow failed', async () => {
        await expect(submitDecision(422)).rejects.toThrow(
            'The workflow failed, so this decision can no longer be applied.'
        );
    });

    it('tells the user the approval can no longer be submitted when it is gone', async () => {
        await expect(submitDecision(410)).rejects.toThrow('has expired or was already resolved');
    });

    it('shows why the server rejected the form inputs', async () => {
        await expect(
            submitDecision(400, JSON.stringify({detail: "The approval form is incomplete: 'Reason' is required."}))
        ).rejects.toThrow("The approval form is incomplete: 'Reason' is required.");
    });

    it('reports the status of a rejection without details', async () => {
        await expect(submitDecision(400)).rejects.toThrow('Submission failed (HTTP 400).');
    });

    it('reports the status of any other failure', async () => {
        await expect(submitDecision(500)).rejects.toThrow('Submission failed (HTTP 500).');
    });
});
