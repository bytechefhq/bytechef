import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseList from '../KnowledgeBaseList';

const hoisted = vi.hoisted(() => {
    return {
        mockUseKnowledgeBaseList: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBaseList', () => ({
    default: hoisted.mockUseKnowledgeBaseList,
}));

vi.mock('../KnowledgeBaseListItem', () => ({
    default: ({
        knowledgeBase,
        remainingTags,
        tags,
    }: {
        knowledgeBase: {id: string; name: string};
        remainingTags?: unknown[];
        tags: unknown[];
    }) => (
        <div data-testid={`list-item-${knowledgeBase.id}`}>
            {knowledgeBase.name} (tags: {tags.length}, remaining: {remainingTags?.length ?? 0})
        </div>
    ),
}));

const mockKnowledgeBases = [
    {id: 'kb-1', name: 'KB 1'},
    {id: 'kb-2', name: 'KB 2'},
];

const mockAllTags = [
    {id: '1', name: 'Tag 1'},
    {id: '2', name: 'Tag 2'},
    {id: '3', name: 'Tag 3'},
];

const mockTagsByKnowledgeBase = [
    {knowledgeBaseId: 'kb-1', tags: [{id: '1', name: 'Tag 1'}]},
    {knowledgeBaseId: 'kb-2', tags: [{id: '2', name: 'Tag 2'}]},
];

const defaultMockReturn = {
    sortedKnowledgeBases: mockKnowledgeBases,
    tagsByKnowledgeBaseMap: new Map([
        ['kb-1', [{id: '1', name: 'Tag 1'}]],
        ['kb-2', [{id: '2', name: 'Tag 2'}]],
    ]),
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseList.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('KnowledgeBaseList', () => {
    it('renders list items for each knowledge base', () => {
        render(
            <KnowledgeBaseList
                allTags={mockAllTags}
                knowledgeBases={mockKnowledgeBases}
                tagsByKnowledgeBaseData={mockTagsByKnowledgeBase}
            />
        );

        expect(screen.getByTestId('list-item-kb-1')).toBeInTheDocument();
        expect(screen.getByTestId('list-item-kb-2')).toBeInTheDocument();
    });

    it('passes correct tags to list items', () => {
        render(
            <KnowledgeBaseList
                allTags={mockAllTags}
                knowledgeBases={mockKnowledgeBases}
                tagsByKnowledgeBaseData={mockTagsByKnowledgeBase}
            />
        );

        expect(screen.getByTestId('list-item-kb-1')).toHaveTextContent('tags: 1');
        expect(screen.getByTestId('list-item-kb-2')).toHaveTextContent('tags: 1');
    });

    it('calculates remaining tags correctly', () => {
        render(
            <KnowledgeBaseList
                allTags={mockAllTags}
                knowledgeBases={mockKnowledgeBases}
                tagsByKnowledgeBaseData={mockTagsByKnowledgeBase}
            />
        );

        // KB 1 has Tag 1, so remaining should be Tag 2 and Tag 3 = 2
        expect(screen.getByTestId('list-item-kb-1')).toHaveTextContent('remaining: 2');
        // KB 2 has Tag 2, so remaining should be Tag 1 and Tag 3 = 2
        expect(screen.getByTestId('list-item-kb-2')).toHaveTextContent('remaining: 2');
    });

    it('passes correct props to hook', () => {
        render(
            <KnowledgeBaseList
                allTags={mockAllTags}
                knowledgeBases={mockKnowledgeBases}
                tagsByKnowledgeBaseData={mockTagsByKnowledgeBase}
            />
        );

        expect(hoisted.mockUseKnowledgeBaseList).toHaveBeenCalledWith({
            knowledgeBases: mockKnowledgeBases,
            tagsByKnowledgeBaseData: mockTagsByKnowledgeBase,
        });
    });

    it('handles empty knowledge bases list', () => {
        hoisted.mockUseKnowledgeBaseList.mockReturnValue({
            sortedKnowledgeBases: [],
            tagsByKnowledgeBaseMap: new Map(),
        });

        render(<KnowledgeBaseList allTags={mockAllTags} knowledgeBases={[]} tagsByKnowledgeBaseData={[]} />);

        expect(screen.queryByTestId('list-item-kb-1')).not.toBeInTheDocument();
    });

    it('handles knowledge base without tags', () => {
        hoisted.mockUseKnowledgeBaseList.mockReturnValue({
            sortedKnowledgeBases: [{id: 'kb-3', name: 'KB 3'}],
            tagsByKnowledgeBaseMap: new Map(),
        });

        render(
            <KnowledgeBaseList
                allTags={mockAllTags}
                knowledgeBases={[{id: 'kb-3', name: 'KB 3'}]}
                tagsByKnowledgeBaseData={[]}
            />
        );

        expect(screen.getByTestId('list-item-kb-3')).toHaveTextContent('tags: 0');
        expect(screen.getByTestId('list-item-kb-3')).toHaveTextContent('remaining: 3');
    });
});
