import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseListItemTagList from '../KnowledgeBaseListItemTagList';

const hoisted = vi.hoisted(() => {
    return {
        mockUseKnowledgeBaseListItemTagList: vi.fn(),
        updateTagsMutation: {mutate: vi.fn()},
    };
});

const hoistedScope = vi.hoisted(() => ({canEditKnowledgeBase: true}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: () => hoistedScope.canEditKnowledgeBase,
}));

vi.mock('../hooks/useKnowledgeBaseListItemTagList', () => ({
    default: hoisted.mockUseKnowledgeBaseListItemTagList,
}));

vi.mock('@/shared/components/TagList', () => ({
    default: ({
        id,
        readOnly,
        remainingTags,
        tags,
    }: {
        getRequest?: unknown;
        id: number;
        readOnly?: boolean;
        remainingTags?: unknown[];
        tags: unknown[];
        updateTagsMutation?: unknown;
    }) => (
        <div data-read-only={String(Boolean(readOnly))} data-testid="tag-list">
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
    hoistedScope.canEditKnowledgeBase = true;

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

    it('renders an editable tag list with KNOWLEDGE_BASE_EDIT', () => {
        render(
            <KnowledgeBaseListItemTagList knowledgeBaseId="123" remainingTags={mockRemainingTags} tags={mockTags} />
        );

        expect(screen.getByTestId('tag-list')).toHaveAttribute('data-read-only', 'false');
    });

    it('renders a read-only tag list without KNOWLEDGE_BASE_EDIT', () => {
        hoistedScope.canEditKnowledgeBase = false;

        render(
            <KnowledgeBaseListItemTagList knowledgeBaseId="123" remainingTags={mockRemainingTags} tags={mockTags} />
        );

        expect(screen.getByTestId('tag-list')).toHaveAttribute('data-read-only', 'true');
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
