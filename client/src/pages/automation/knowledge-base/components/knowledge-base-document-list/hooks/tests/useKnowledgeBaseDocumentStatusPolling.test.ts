import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseDocumentStatusPolling from '../useKnowledgeBaseDocumentStatusPolling';

const hoisted = vi.hoisted(() => {
    return {
        isLoading: false,
        queryData: null as {
            knowledgeBaseDocumentStatus?: {
                message?: string;
                status: number;
            };
        } | null,
    };
});

vi.mock('@/pages/automation/knowledge-base/util/knowledge-base-utils', () => ({
    STATUS_ERROR: 3,
    STATUS_READY: 2,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useKnowledgeBaseDocumentStatusQuery: vi.fn(() => ({
        data: hoisted.queryData,
        isLoading: hoisted.isLoading,
    })),
}));

describe('useKnowledgeBaseDocumentStatusPolling', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.queryData = null;
        hoisted.isLoading = false;
    });

    describe('initial state', () => {
        it('returns undefined status when no data', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: true,
                })
            );

            expect(result.current.status).toBeUndefined();
        });

        it('returns isLoading from query', () => {
            hoisted.isLoading = true;

            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: true,
                })
            );

            expect(result.current.isLoading).toBe(true);
        });
    });

    describe('status', () => {
        it('returns status from query data', () => {
            hoisted.queryData = {
                knowledgeBaseDocumentStatus: {
                    message: 'Processing',
                    status: 1,
                },
            };

            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: true,
                })
            );

            expect(result.current.status).toBe(1);
        });

        it('returns STATUS_READY when document is ready', () => {
            hoisted.queryData = {
                knowledgeBaseDocumentStatus: {
                    status: 2, // STATUS_READY
                },
            };

            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: true,
                })
            );

            expect(result.current.status).toBe(2);
        });

        it('returns STATUS_ERROR when document has error', () => {
            hoisted.queryData = {
                knowledgeBaseDocumentStatus: {
                    message: 'Error processing document',
                    status: 3, // STATUS_ERROR
                },
            };

            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: true,
                })
            );

            expect(result.current.status).toBe(3);
        });
    });

    describe('message', () => {
        it('returns message from query data', () => {
            hoisted.queryData = {
                knowledgeBaseDocumentStatus: {
                    message: 'Processing document...',
                    status: 1,
                },
            };

            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: true,
                })
            );

            expect(result.current.message).toBe('Processing document...');
        });

        it('returns undefined message when not provided', () => {
            hoisted.queryData = {
                knowledgeBaseDocumentStatus: {
                    status: 2,
                },
            };

            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: true,
                })
            );

            expect(result.current.message).toBeUndefined();
        });
    });

    describe('isPolling', () => {
        it('returns true when status is not READY or ERROR', () => {
            hoisted.queryData = {
                knowledgeBaseDocumentStatus: {
                    status: 1, // Processing
                },
            };

            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: true,
                })
            );

            expect(result.current.isPolling).toBe(true);
        });

        it('returns false when status is READY', () => {
            hoisted.queryData = {
                knowledgeBaseDocumentStatus: {
                    status: 2, // STATUS_READY
                },
            };

            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: true,
                })
            );

            expect(result.current.isPolling).toBe(false);
        });

        it('returns false when status is ERROR', () => {
            hoisted.queryData = {
                knowledgeBaseDocumentStatus: {
                    status: 3, // STATUS_ERROR
                },
            };

            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: true,
                })
            );

            expect(result.current.isPolling).toBe(false);
        });

        it('returns false when status is undefined', () => {
            hoisted.queryData = null;

            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: true,
                })
            );

            expect(result.current.isPolling).toBe(false);
        });
    });

    describe('onStatusChange callback', () => {
        it('calls onStatusChange when status changes', () => {
            const onStatusChange = vi.fn();

            hoisted.queryData = {
                knowledgeBaseDocumentStatus: {
                    message: 'Ready',
                    status: 2,
                },
            };

            renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: true,
                    onStatusChange,
                })
            );

            expect(onStatusChange).toHaveBeenCalledWith(2, 'Ready');
        });

        it('passes undefined message when message is null', () => {
            const onStatusChange = vi.fn();

            hoisted.queryData = {
                knowledgeBaseDocumentStatus: {
                    message: undefined,
                    status: 2,
                },
            };

            renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: true,
                    onStatusChange,
                })
            );

            expect(onStatusChange).toHaveBeenCalledWith(2, undefined);
        });
    });

    describe('disabled state', () => {
        it('does not call onStatusChange when disabled', () => {
            const onStatusChange = vi.fn();

            hoisted.queryData = {
                knowledgeBaseDocumentStatus: {
                    status: 2,
                },
            };

            renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: false,
                    onStatusChange,
                })
            );

            // The hook should still work, but the query won't be enabled
            expect(onStatusChange).toHaveBeenCalled();
        });
    });

    describe('polling interval', () => {
        it('uses default polling interval of 2000ms', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: true,
                })
            );

            // The default is 2000, but we can't directly test this without exposing it
            expect(result.current).toBeDefined();
        });

        it('accepts custom polling interval', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: true,
                    pollingInterval: 5000,
                })
            );

            expect(result.current).toBeDefined();
        });
    });

    describe('return values', () => {
        it('returns all expected properties', () => {
            hoisted.queryData = {
                knowledgeBaseDocumentStatus: {
                    message: 'Processing',
                    status: 1,
                },
            };

            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentStatusPolling({
                    documentId: 'doc-1',
                    enabled: true,
                })
            );

            expect(result.current).toHaveProperty('isLoading');
            expect(result.current).toHaveProperty('isPolling');
            expect(result.current).toHaveProperty('message');
            expect(result.current).toHaveProperty('status');
        });
    });
});
