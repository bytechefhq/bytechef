import {reportMutationError} from '@/shared/error/useReportQueryError';
import {toast} from 'sonner';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubChatsKeys} from '../useChats';

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
    },
}));

describe('useChats: query keys', () => {
    it('produces stable, namespaced keys per (workspace, environment, status) triple', () => {
        expect(AiHubChatsKeys.list(1, 0, 'ACTIVE')).toEqual(['aiHubChats', 'list', 1, 0, 'ACTIVE']);

        expect(AiHubChatsKeys.list(2, 2, 'ARCHIVED')).toEqual(['aiHubChats', 'list', 2, 2, 'ARCHIVED']);
    });

    it('messages and artifacts keys start with the shared root', () => {
        expect(AiHubChatsKeys.messages(7, 1)).toEqual(['aiHubChats', 'messages', 7, 1]);
        expect(AiHubChatsKeys.artifacts(7, 1)).toEqual(['aiHubChats', 'artifacts', 7, 1]);
    });
});

describe('useChats: reportMutationError', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('toasts the error message verbatim when present', () => {
        reportMutationError('Delete chat', new Error('Forbidden'));

        expect(toast.error).toHaveBeenCalledWith('Forbidden');
    });

    it('falls back to "${action} failed" when the error has no message', () => {
        reportMutationError('Delete chat', new Error(''));

        expect(toast.error).toHaveBeenCalledWith('Delete chat failed');
    });
});
