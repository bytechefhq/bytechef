import {reportMutationError} from '@/shared/error/useReportQueryError';
import {toast} from 'sonner';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubTasksKeys} from './useTasks';

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
    },
}));

describe('useTasks: query keys', () => {
    it('produces stable, namespaced keys per (workspace, environment, status) triple', () => {
        // Pin the cache-key shape so a downstream rename in AiHubTasksKeys (e.g. dropping the
        // workspaceId or environment from `list`) does not silently drop the cache scoping and cause cross-workspace
        // or cross-environment bleed. The environment ordinal (DEVELOPMENT=0, STAGING=1, PRODUCTION=2) sits between
        // workspaceId and status so prefix invalidations against [...all, 'list', workspaceId] still catch every
        // env+status combination.
        expect(AiHubTasksKeys.list(1, 0, 'ACTIVE')).toEqual(['aiHubTasks', 'list', 1, 0, 'ACTIVE']);

        expect(AiHubTasksKeys.list(2, 2, 'ARCHIVED')).toEqual(['aiHubTasks', 'list', 2, 2, 'ARCHIVED']);
    });

    it('messages, artifacts, and audit keys all start with the shared root', () => {
        expect(AiHubTasksKeys.messages(7, 1)[0]).toBe('aiHubTasks');
        expect(AiHubTasksKeys.artifacts(7, 1)[0]).toBe('aiHubTasks');
        expect(AiHubTasksKeys.artifactAudit(1, {kind: 'FILE_CREATED'})[0]).toBe('aiHubTasks');
    });

    it('artifactAudit embeds the filter object so different filters get distinct cache entries', () => {
        const a = AiHubTasksKeys.artifactAudit(1, {kind: 'FILE_CREATED'});
        const b = AiHubTasksKeys.artifactAudit(1, {kind: 'WORKFLOW_CREATED'});

        expect(a).not.toEqual(b);
    });
});

describe('useTasks: reportMutationError', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('toasts the error message verbatim when present', () => {
        reportMutationError('Delete task', new Error('Forbidden'));

        expect(toast.error).toHaveBeenCalledWith('Forbidden');
    });

    it('falls back to "${action} failed" when the error has no message', () => {
        reportMutationError('Delete task', new Error(''));

        expect(toast.error).toHaveBeenCalledWith('Delete task failed');
    });
});
