import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBasesFilterTitle from '../KnowledgeBasesFilterTitle';

const hoisted = vi.hoisted(() => {
    return {
        mockUseKnowledgeBasesFilterTitle: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBasesFilterTitle', () => ({
    default: hoisted.mockUseKnowledgeBasesFilterTitle,
}));

vi.mock('@/components/Badge/Badge', () => ({
    default: ({label}: {label: string; styleType?: string; weight?: string}) => (
        <span data-testid="badge">{label}</span>
    ),
}));

const mockAllTags = [
    {id: '1', name: 'Tag 1'},
    {id: '2', name: 'Tag 2'},
];

const mockTagsByKnowledgeBase = [{knowledgeBaseId: 'kb-1', tags: [{id: '1', name: 'Tag 1'}]}];

const defaultMockReturn = {
    pageTitle: undefined,
    tagId: null,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBasesFilterTitle.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('KnowledgeBasesFilterTitle', () => {
    it('renders filter by text', () => {
        render(<KnowledgeBasesFilterTitle allTags={mockAllTags} tagsByKnowledgeBaseData={mockTagsByKnowledgeBase} />);

        expect(screen.getByText('Filter by')).toBeInTheDocument();
    });

    it('shows none when no tag filter', () => {
        render(<KnowledgeBasesFilterTitle allTags={mockAllTags} tagsByKnowledgeBaseData={mockTagsByKnowledgeBase} />);

        expect(screen.getByText('none')).toBeInTheDocument();
    });

    it('shows tag label when tag filter is active', () => {
        hoisted.mockUseKnowledgeBasesFilterTitle.mockReturnValue({
            pageTitle: 'Tag 1',
            tagId: '1',
        });

        render(<KnowledgeBasesFilterTitle allTags={mockAllTags} tagsByKnowledgeBaseData={mockTagsByKnowledgeBase} />);

        expect(screen.getByText('tag:')).toBeInTheDocument();
    });

    it('renders badge with tag name', () => {
        hoisted.mockUseKnowledgeBasesFilterTitle.mockReturnValue({
            pageTitle: 'Tag 1',
            tagId: '1',
        });

        render(<KnowledgeBasesFilterTitle allTags={mockAllTags} tagsByKnowledgeBaseData={mockTagsByKnowledgeBase} />);

        expect(screen.getByTestId('badge')).toHaveTextContent('Tag 1');
    });

    it('shows Unknown Tag when pageTitle is not a string', () => {
        hoisted.mockUseKnowledgeBasesFilterTitle.mockReturnValue({
            pageTitle: undefined,
            tagId: '1',
        });

        render(<KnowledgeBasesFilterTitle allTags={mockAllTags} tagsByKnowledgeBaseData={mockTagsByKnowledgeBase} />);

        expect(screen.getByTestId('badge')).toHaveTextContent('Unknown Tag');
    });

    it('passes correct props to hook', () => {
        render(<KnowledgeBasesFilterTitle allTags={mockAllTags} tagsByKnowledgeBaseData={mockTagsByKnowledgeBase} />);

        expect(hoisted.mockUseKnowledgeBasesFilterTitle).toHaveBeenCalledWith({
            allTags: mockAllTags,
            tagsByKnowledgeBaseData: mockTagsByKnowledgeBase,
        });
    });
});
