import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseDocumentListItem from '../useKnowledgeBaseDocumentListItem';

const hoisted = vi.hoisted(() => {
    return {
        invalidateQueries: vi.fn(),
        pollingStatus: undefined as number | undefined,
    };
});

vi.mock(
    '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentStatusPolling',
    () => ({
        default: vi.fn(() => ({
            status: hoisted.pollingStatus,
        })),
    })
);

vi.mock('@/pages/automation/knowledge-base/util/knowledge-base-utils', () => ({
    STATUS_ERROR: 3,
    STATUS_READY: 2,
    getDocumentIcon: vi.fn(() => '<MockIcon />'),
    getStatusBadge: vi.fn((status: number) => `<StatusBadge status="${status}" />`),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        invalidateQueries: hoisted.invalidateQueries,
    })),
}));

const createMockDocument = (overrides = {}) => ({
    chunks: [
        {content: 'Content 1', id: 'chunk-1', knowledgeBaseDocumentId: 'doc-1', metadata: null},
        {content: 'Content 2', id: 'chunk-2', knowledgeBaseDocumentId: 'doc-1', metadata: null},
    ],
    createdDate: '2024-01-01',
    document: {
        extension: 'pdf',
        mimeType: 'application/pdf',
        name: 'test-file.pdf',
        url: 'https://example.com/test-file.pdf',
    },
    id: 'doc-1',
    name: 'Test Document',
    status: 2, // STATUS_READY
    ...overrides,
});

describe('useKnowledgeBaseDocumentListItem', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.pollingStatus = undefined;
    });

    describe('chunkCount', () => {
        it('returns correct chunk count', () => {
            const document = createMockDocument();

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItem({document}));

            expect(result.current.chunkCount).toBe(2);
        });

        it('returns 0 when no chunks', () => {
            const document = createMockDocument({chunks: []});

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItem({document}));

            expect(result.current.chunkCount).toBe(0);
        });

        it('returns 0 when chunks is null', () => {
            const document = createMockDocument({chunks: null});

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItem({document}));

            expect(result.current.chunkCount).toBe(0);
        });
    });

    describe('displayName', () => {
        it('returns document.document.name when available', () => {
            const document = createMockDocument();

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItem({document}));

            expect(result.current.displayName).toBe('test-file.pdf');
        });

        it('falls back to document.name when document.document.name is not available', () => {
            const document = createMockDocument({document: null});

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItem({document}));

            expect(result.current.displayName).toBe('Test Document');
        });

        it('falls back to document.name when document.document is null', () => {
            const document = createMockDocument({
                document: {extension: 'pdf', mimeType: 'application/pdf', name: null},
            });

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItem({document}));

            expect(result.current.displayName).toBe('Test Document');
        });
    });

    describe('chunksCollapsibleTriggerRef', () => {
        it('returns a ref object', () => {
            const document = createMockDocument();

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItem({document}));

            expect(result.current.chunksCollapsibleTriggerRef).toBeDefined();
            expect(result.current.chunksCollapsibleTriggerRef.current).toBeNull();
        });
    });

    describe('documentIcon', () => {
        it('returns document icon', () => {
            const document = createMockDocument();

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItem({document}));

            expect(result.current.documentIcon).toBeDefined();
        });
    });

    describe('statusBadge', () => {
        it('returns status badge based on current status', () => {
            const document = createMockDocument({status: 2});

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItem({document}));

            expect(result.current.statusBadge).toBeDefined();
        });

        it('uses polled status when document is still processing', () => {
            const document = createMockDocument({status: 1}); // STATUS_PROCESSING
            hoisted.pollingStatus = 2; // STATUS_READY

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItem({document}));

            expect(result.current.statusBadge).toBeDefined();
        });
    });

    describe('handleDocumentListItemClick', () => {
        it('returns a function', () => {
            const document = createMockDocument();

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItem({document}));

            expect(typeof result.current.handleDocumentListItemClick).toBe('function');
        });
    });

    describe('handleDocumentListItemKeyDown', () => {
        it('returns a function', () => {
            const document = createMockDocument();

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItem({document}));

            expect(typeof result.current.handleDocumentListItemKeyDown).toBe('function');
        });
    });

    describe('handleTagListClick', () => {
        it('returns a function', () => {
            const document = createMockDocument();

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItem({document}));

            expect(typeof result.current.handleTagListClick).toBe('function');
        });

        it('stops event propagation', () => {
            const document = createMockDocument();

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItem({document}));

            const mockEvent = {
                stopPropagation: vi.fn(),
            };

            result.current.handleTagListClick(mockEvent as unknown as React.MouseEvent<HTMLDivElement>);

            expect(mockEvent.stopPropagation).toHaveBeenCalled();
        });
    });

    describe('return values', () => {
        it('returns all expected properties', () => {
            const document = createMockDocument();

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItem({document}));

            expect(result.current).toHaveProperty('chunkCount');
            expect(result.current).toHaveProperty('chunksCollapsibleTriggerRef');
            expect(result.current).toHaveProperty('displayName');
            expect(result.current).toHaveProperty('documentIcon');
            expect(result.current).toHaveProperty('handleDocumentListItemClick');
            expect(result.current).toHaveProperty('handleDocumentListItemKeyDown');
            expect(result.current).toHaveProperty('handleTagListClick');
            expect(result.current).toHaveProperty('statusBadge');
        });
    });
});
