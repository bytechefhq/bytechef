import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBasesLeftSidebarNav from '../KnowledgeBasesLeftSidebarNav';

const hoisted = vi.hoisted(() => {
    return {
        mockUseKnowledgeBasesLeftSidebarNav: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBasesLeftSidebarNav', () => ({
    default: hoisted.mockUseKnowledgeBasesLeftSidebarNav,
}));

vi.mock('@/shared/layout/LeftSidebarNav', () => ({
    LeftSidebarNav: ({body, title}: {body: React.ReactNode; title: string}) => (
        <nav data-testid="left-sidebar-nav">
            <h3>{title}</h3>

            {body}
        </nav>
    ),
    LeftSidebarNavItem: ({
        item,
        toLink,
    }: {
        icon?: React.ReactNode;
        item: {current: boolean; id: number | string; name: string};
        toLink: string;
    }) => (
        <a data-current={item.current} data-testid={`nav-item-${item.id}`} href={toLink}>
            {item.name}
        </a>
    ),
}));

const defaultMockReturn = {
    hasData: true,
    isLoading: false,
    tagId: null,
    tags: [
        {id: '1', name: 'Tag 1'},
        {id: '2', name: 'Tag 2'},
    ],
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBasesLeftSidebarNav.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('KnowledgeBasesLeftSidebarNav', () => {
    it('renders sidebar nav when has data', () => {
        render(<KnowledgeBasesLeftSidebarNav />);

        expect(screen.getByTestId('left-sidebar-nav')).toBeInTheDocument();
    });

    it('renders nothing when no data', () => {
        hoisted.mockUseKnowledgeBasesLeftSidebarNav.mockReturnValue({
            ...defaultMockReturn,
            hasData: false,
        });

        render(<KnowledgeBasesLeftSidebarNav />);

        expect(screen.queryByTestId('left-sidebar-nav')).not.toBeInTheDocument();
    });

    it('renders title', () => {
        render(<KnowledgeBasesLeftSidebarNav />);

        expect(screen.getByText('Tags')).toBeInTheDocument();
    });

    it('renders tag items', () => {
        render(<KnowledgeBasesLeftSidebarNav />);

        expect(screen.getByTestId('nav-item-1')).toBeInTheDocument();
        expect(screen.getByTestId('nav-item-2')).toBeInTheDocument();
    });

    it('renders tag names', () => {
        render(<KnowledgeBasesLeftSidebarNav />);

        expect(screen.getByText('Tag 1')).toBeInTheDocument();
        expect(screen.getByText('Tag 2')).toBeInTheDocument();
    });

    it('marks current tag as current', () => {
        hoisted.mockUseKnowledgeBasesLeftSidebarNav.mockReturnValue({
            ...defaultMockReturn,
            tagId: '1',
        });

        render(<KnowledgeBasesLeftSidebarNav />);

        expect(screen.getByTestId('nav-item-1')).toHaveAttribute('data-current', 'true');
        expect(screen.getByTestId('nav-item-2')).toHaveAttribute('data-current', 'false');
    });

    it('renders correct links', () => {
        render(<KnowledgeBasesLeftSidebarNav />);

        expect(screen.getByTestId('nav-item-1')).toHaveAttribute('href', '?tagId=1');
        expect(screen.getByTestId('nav-item-2')).toHaveAttribute('href', '?tagId=2');
    });

    it('shows no tags message when tags array is empty', () => {
        hoisted.mockUseKnowledgeBasesLeftSidebarNav.mockReturnValue({
            ...defaultMockReturn,
            tags: [],
        });

        render(<KnowledgeBasesLeftSidebarNav />);

        expect(screen.getByText('No defined tags.')).toBeInTheDocument();
    });

    it('does not render items when loading', () => {
        hoisted.mockUseKnowledgeBasesLeftSidebarNav.mockReturnValue({
            ...defaultMockReturn,
            isLoading: true,
        });

        render(<KnowledgeBasesLeftSidebarNav />);

        expect(screen.queryByTestId('nav-item-1')).not.toBeInTheDocument();
    });
});
