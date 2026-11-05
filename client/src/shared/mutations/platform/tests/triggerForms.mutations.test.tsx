import {act, createTestQueryClientWrapper, renderHook, resetAll} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {SubmitTriggerFormVariablesI, useSubmitTriggerFormMutation} from '../triggerForms.mutations';

const SUBMITTED_AT = 1700000000000;

describe('useSubmitTriggerFormMutation', () => {
    const fetchSpy = vi.fn<typeof fetch>();
    const originalFetch = global.fetch;

    beforeEach(() => {
        global.fetch = fetchSpy;

        fetchSpy.mockReset();

        vi.spyOn(Date, 'now').mockReturnValue(SUBMITTED_AT);
    });

    afterEach(() => {
        global.fetch = originalFetch;

        vi.restoreAllMocks();

        resetAll();
    });

    async function submitForm(variables: SubmitTriggerFormVariablesI, response = new Response(null, {status: 200})) {
        fetchSpy.mockResolvedValue(response);

        const {result} = renderHook(() => useSubmitTriggerFormMutation(), {wrapper: createTestQueryClientWrapper()});

        let submission: Promise<void> | undefined;

        act(() => {
            submission = result.current.mutateAsync(variables);
        });

        return submission!;
    }

    function getRequestInit(): RequestInit {
        return fetchSpy.mock.calls[0][1] as RequestInit;
    }

    it('posts plain values as JSON', async () => {
        await submitForm({values: {count: 2, name: 'Ada', tags: ['a', 'b']}, workflowExecutionId: 'execution-1'});

        expect(fetchSpy).toHaveBeenCalledWith('/webhooks/execution-1', {
            body: JSON.stringify({body: {count: 2, name: 'Ada', tags: ['a', 'b']}, submittedAt: SUBMITTED_AT}),
            headers: {'Content-Type': 'application/json'},
            method: 'POST',
        });
    });

    it('posts multipart form data when a value is a file', async () => {
        const avatar = new File(['image'], 'avatar.png', {type: 'image/png'});

        await submitForm({
            values: {
                avatar,
                count: 2,
                details: {city: 'Zagreb'},
                empty: null,
                missing: undefined,
                subscribed: false,
            },
            workflowExecutionId: 'execution-2',
        });

        const requestInit = getRequestInit();
        const formData = requestInit.body as FormData;

        expect(requestInit.headers).toEqual({});
        expect(requestInit.method).toBe('POST');
        expect(formData).toBeInstanceOf(FormData);
        expect(formData.get('submittedAt')).toBe(String(SUBMITTED_AT));
        expect((formData.get('body.avatar') as File).name).toBe('avatar.png');
        expect(formData.get('body.count')).toBe('2');
        expect(formData.get('body.details')).toBe('{"city":"Zagreb"}');
        expect(formData.get('body.subscribed')).toBe('false');
        expect(formData.has('body.empty')).toBe(false);
        expect(formData.has('body.missing')).toBe(false);
    });

    it('posts multipart form data when an array holds a file and appends each array item', async () => {
        const firstFile = new File(['one'], 'one.txt');
        const secondFile = new File(['two'], 'two.txt');

        await submitForm({
            values: {attachments: [firstFile, secondFile], labels: ['x', null, {nested: true}]},
            workflowExecutionId: 'execution-3',
        });

        const formData = getRequestInit().body as FormData;

        expect(formData.getAll('body.attachments').map((entry) => (entry as File).name)).toEqual([
            'one.txt',
            'two.txt',
        ]);
        expect(formData.getAll('body.labels')).toEqual(['x', '{"nested":true}']);
    });

    it('sends JSON when an array holds no file', async () => {
        await submitForm({values: {labels: ['x']}, workflowExecutionId: 'execution-4'});

        expect(getRequestInit().headers).toEqual({'Content-Type': 'application/json'});
    });

    it('rejects with the status text when the submission fails', async () => {
        await expect(
            submitForm(
                {values: {}, workflowExecutionId: 'execution-5'},
                new Response(null, {status: 500, statusText: 'Internal Server Error'})
            )
        ).rejects.toThrow('Submission failed: Internal Server Error');
    });
});
