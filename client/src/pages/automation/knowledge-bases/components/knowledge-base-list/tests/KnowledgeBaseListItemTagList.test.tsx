import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseListItemTagList from '../KnowledgeBaseListItemTagList';

const hoisted = vi.hoisted(() => {
    return {
        mockUseKnowledgeBaseListItemTagList: vi.fn(),
        updateTagsMutation: {mutate: vi.fn()},
    };
});

vi.mock('../hooks/useKnowledgeBaseListItemTagList', () => ({
    default: hoisted.mockUseKnowledgeBaseListItemTagList,
}));

vi.mock('@/shared/components/TagList', () => ({
    default: ({
        id,
        remainingTags,
        tags,
    }: {
        getRequest?: unknown;
        id: number;
        remainingTags?: unknown[];
        tags: unknown[];
        updateTagsMutation?: unknown;
    }) => (
        <div data-testid="tag-list">
            ID: {id}, Tags: {tags.length}, Remaining: {remainingTags?.length ?? 0}
        </div>
    ),
}));

const mockTags = [
    {id: '1', name: 'Tag 1'},
    {id: '2', name: 'Tag 2'},
];

const mockRemainingTags = [{id: '3', name: 'Tag 3'}];

const defaultMockReturn = {
    convertedRemainingTags: mockRemainingTags.map((tag) => ({...tag, id: Number(tag.id)})),
    convertedTags: mockTags.map((tag) => ({...tag, id: Number(tag.id)})),
    updateTagsMutation: hoisted.updateTagsMutation,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseListItemTagList.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('KnowledgeBaseListItemTagList', () => {
    it('renders tag list component', () => {
        render(
            <KnowledgeBaseListItemTagList knowledgeBaseId="123" remainingTags={mockRemainingTags} tags={mockTags} />
        );

        expect(screen.getByTestId('tag-list')).toBeInTheDocument();
    });

    it('passes converted id to TagList', () => {
        render(
            <KnowledgeBaseListItemTagList knowledgeBaseId="123" remainingTags={mockRemainingTags} tags={mockTags} />
        );

        expect(screen.getByTestId('tag-list')).toHaveTextContent('ID: 123');
    });

    it('passes converted tags to TagList', () => {
        render(
            <KnowledgeBaseListItemTagList knowledgeBaseId="123" remainingTags={mockRemainingTags} tags={mockTags} />
        );

        expect(screen.getByTestId('tag-list')).toHaveTextContent('Tags: 2');
    });

    it('passes converted remaining tags to TagList', () => {
        render(
            <KnowledgeBaseListItemTagList knowledgeBaseId="123" remainingTags={mockRemainingTags} tags={mockTags} />
        );

        expect(screen.getByTestId('tag-list')).toHaveTextContent('Remaining: 1');
    });

    it('passes correct props to hook', () => {
        render(
            <KnowledgeBaseListItemTagList knowledgeBaseId="123" remainingTags={mockRemainingTags} tags={mockTags} />
        );

        expect(hoisted.mockUseKnowledgeBaseListItemTagList).toHaveBeenCalledWith({
            knowledgeBaseId: '123',
            remainingTags: mockRemainingTags,
            tags: mockTags,
        });
    });

    it('handles undefined remaining tags', () => {
        hoisted.mockUseKnowledgeBaseListItemTagList.mockReturnValue({
            ...defaultMockReturn,
            convertedRemainingTags: undefined,
        });

        render(<KnowledgeBaseListItemTagList knowledgeBaseId="123" tags={mockTags} />);

        expect(screen.getByTestId('tag-list')).toHaveTextContent('Remaining: 0');
    });
});
