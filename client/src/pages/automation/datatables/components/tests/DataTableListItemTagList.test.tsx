import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DataTableListItemTagList from '../DataTableListItemTagList';

const hoisted = vi.hoisted(() => {
    return {
        mockUpdateTagsMutation: {mutate: vi.fn()},
        mockUseDataTableListItemTagList: vi.fn(),
    };
});

vi.mock('../hooks/useDataTableListItemTagList', () => ({
    default: hoisted.mockUseDataTableListItemTagList,
}));

vi.mock('@/shared/components/TagList', () => ({
    default: ({
        id,
        remainingTags,
        tags,
    }: {
        getRequest: unknown;
        id: number;
        remainingTags?: {id: number; name: string}[];
        tags: {id: number; name: string}[];
        updateTagsMutation: unknown;
    }) => (
        <div data-testid="tag-list">
            <span data-testid="tag-list-id">{id}</span>

            {tags.map((tag) => (
                <span data-testid={`tag-${tag.id}`} key={tag.id}>
                    {tag.name}
                </span>
            ))}

            {remainingTags?.map((tag) => (
                <span data-testid={`remaining-tag-${tag.id}`} key={tag.id}>
                    {tag.name}
                </span>
            ))}
        </div>
    ),
}));

const mockTags = [
    {id: '1', name: 'Important'},
    {id: '2', name: 'Production'},
];

const mockRemainingTags = [{id: '3', name: 'Archived'}];

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseDataTableListItemTagList.mockReturnValue({
        updateTagsMutation: hoisted.mockUpdateTagsMutation,
    });
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DataTableListItemTagList', () => {
    it('should render TagList component', () => {
        render(<DataTableListItemTagList datatableId="123" remainingTags={mockRemainingTags} tags={mockTags} />);

        expect(screen.getByTestId('tag-list')).toBeInTheDocument();
    });

    it('should pass correct id to TagList', () => {
        render(<DataTableListItemTagList datatableId="123" remainingTags={mockRemainingTags} tags={mockTags} />);

        expect(screen.getByTestId('tag-list-id')).toHaveTextContent('123');
    });

    it('should render all tags', () => {
        render(<DataTableListItemTagList datatableId="123" remainingTags={mockRemainingTags} tags={mockTags} />);

        expect(screen.getByTestId('tag-1')).toHaveTextContent('Important');
        expect(screen.getByTestId('tag-2')).toHaveTextContent('Production');
    });

    it('should render remaining tags', () => {
        render(<DataTableListItemTagList datatableId="123" remainingTags={mockRemainingTags} tags={mockTags} />);

        expect(screen.getByTestId('remaining-tag-3')).toHaveTextContent('Archived');
    });

    it('should call useDataTableListItemTagList with correct tableId', () => {
        render(<DataTableListItemTagList datatableId="456" remainingTags={mockRemainingTags} tags={mockTags} />);

        expect(hoisted.mockUseDataTableListItemTagList).toHaveBeenCalledWith({tableId: '456'});
    });
});

describe('DataTableListItemTagList empty state', () => {
    it('should render TagList with empty tags', () => {
        render(<DataTableListItemTagList datatableId="123" remainingTags={[]} tags={[]} />);

        expect(screen.getByTestId('tag-list')).toBeInTheDocument();
        expect(screen.queryByTestId('tag-1')).not.toBeInTheDocument();
    });
});
