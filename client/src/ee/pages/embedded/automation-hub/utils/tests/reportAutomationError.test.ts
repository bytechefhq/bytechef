import {reportRemoveError, reportSetEnabledError} from '@/ee/pages/embedded/automation-hub/utils/reportAutomationError';
import {ResponseError} from '@/ee/shared/middleware/embedded/public';
import {toast} from 'sonner';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('sonner', () => ({toast: {error: vi.fn()}}));

function buildResponseError(status: number, body: string | null = null): ResponseError {
    return new ResponseError(new Response(body, {status}), 'Response returned an error code');
}

function buildConflictError(body: Record<string, unknown>): ResponseError {
    return buildResponseError(409, JSON.stringify(body));
}

describe('reportSetEnabledError', () => {
    beforeEach(() => {
        vi.mocked(toast.error).mockReset();
    });

    it('stays silent for response errors the fetch interceptor already reported', async () => {
        await reportSetEnabledError(buildResponseError(500), {label: 'Sync'});

        expect(toast.error).not.toHaveBeenCalled();
    });

    it('asks to connect the missing component using its title', async () => {
        await reportSetEnabledError(buildConflictError({missingConnectionComponentName: 'slack'}), {
            components: [
                {name: 'github', title: 'GitHub'},
                {name: 'slack', title: 'Slack'},
            ],
            label: 'Sync',
        });

        expect(toast.error).toHaveBeenCalledWith('Could not enable "Sync"', {
            description: 'Connect a Slack account first, then enable it again.',
        });
    });

    it('falls back to the component name when no title is known', async () => {
        await reportSetEnabledError(buildConflictError({missingConnectionComponentName: 'slack'}), {label: 'Sync'});

        expect(toast.error).toHaveBeenCalledWith('Could not enable "Sync"', {
            description: 'Connect a slack account first, then enable it again.',
        });
    });

    it('asks for a missing input and reports which one', async () => {
        const onMissingInput = vi.fn();

        await reportSetEnabledError(buildConflictError({missingInputName: 'channel'}), {label: 'Sync', onMissingInput});

        expect(toast.error).toHaveBeenCalledWith('Could not enable "Sync"', {
            description: 'The "channel" setting needs a value first.',
        });
        expect(onMissingInput).toHaveBeenCalledWith('channel');
    });

    it('reports a missing input without a callback', async () => {
        await reportSetEnabledError(buildConflictError({missingInputName: 'channel'}), {label: 'Sync'});

        expect(toast.error).toHaveBeenCalledTimes(1);
    });

    it('prefers the missing connection over a missing input', async () => {
        const onMissingInput = vi.fn();

        await reportSetEnabledError(
            buildConflictError({missingConnectionComponentName: 'slack', missingInputName: 'channel'}),
            {label: 'Sync', onMissingInput}
        );

        expect(toast.error).toHaveBeenCalledWith('Could not enable "Sync"', {
            description: 'Connect a slack account first, then enable it again.',
        });
        expect(onMissingInput).not.toHaveBeenCalled();
    });

    it('asks to fix a single workflow error', async () => {
        await reportSetEnabledError(buildConflictError({invalidWorkflowErrorCount: 1}), {label: 'Sync'});

        expect(toast.error).toHaveBeenCalledWith('Could not enable "Sync"', {
            description: 'Fix the error in this workflow first, then enable it again.',
        });
    });

    it('asks to fix several workflow errors', async () => {
        await reportSetEnabledError(buildConflictError({invalidWorkflowErrorCount: 4}), {label: 'Sync'});

        expect(toast.error).toHaveBeenCalledWith('Could not enable "Sync"', {
            description: 'Fix the 4 errors in this workflow first, then enable it again.',
        });
    });

    it('falls back to the error message for a conflict without a recognised body', async () => {
        await reportSetEnabledError(buildConflictError({}), {label: 'Sync'});

        expect(toast.error).toHaveBeenCalledWith('Could not enable "Sync"', {
            description: 'Response returned an error code',
        });
    });

    it('falls back to the error message for a conflict whose body is not JSON', async () => {
        await reportSetEnabledError(buildResponseError(409, 'not json'), {label: 'Sync'});

        expect(toast.error).toHaveBeenCalledWith('Could not enable "Sync"', {
            description: 'Response returned an error code',
        });
    });

    it('leaves the conflict body readable for later consumers', async () => {
        const error = buildConflictError({missingInputName: 'channel'});

        await reportSetEnabledError(error, {label: 'Sync'});

        await expect(error.response.json()).resolves.toEqual({missingInputName: 'channel'});
    });

    it('reports a plain error with its message', async () => {
        await reportSetEnabledError(new Error('Network down'), {label: 'Sync'});

        expect(toast.error).toHaveBeenCalledWith('Could not enable "Sync"', {description: 'Network down'});
    });

    it('reports an error without a message and a non-error value without a description', async () => {
        await reportSetEnabledError(new Error(''), {label: 'Sync'});
        await reportSetEnabledError('boom', {label: 'Sync'});

        expect(toast.error).toHaveBeenNthCalledWith(1, 'Could not enable "Sync"', {description: undefined});
        expect(toast.error).toHaveBeenNthCalledWith(2, 'Could not enable "Sync"', {description: undefined});
    });
});

describe('reportRemoveError', () => {
    beforeEach(() => {
        vi.mocked(toast.error).mockReset();
    });

    it('stays silent for response errors the fetch interceptor already reported', () => {
        reportRemoveError(buildResponseError(404), 'Sync');

        expect(toast.error).not.toHaveBeenCalled();
    });

    it('reports a conflict response error', () => {
        reportRemoveError(buildResponseError(409), 'Sync');

        expect(toast.error).toHaveBeenCalledWith('Could not remove "Sync"', {
            description: 'Response returned an error code',
        });
    });

    it('reports a plain error with its message', () => {
        reportRemoveError(new Error('Network down'), 'Sync');

        expect(toast.error).toHaveBeenCalledWith('Could not remove "Sync"', {description: 'Network down'});
    });

    it('reports a non-error value without a description', () => {
        reportRemoveError(undefined, 'Sync');

        expect(toast.error).toHaveBeenCalledWith('Could not remove "Sync"', {description: undefined});
    });
});
