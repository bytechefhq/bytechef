import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseLeftSidebarNav from '../KnowledgeBaseLeftSidebarNav';

const hoisted = vi.hoisted(() => {
    return {
        mockUseKnowledgeBaseLeftSidebarNav: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBaseLeftSidebarNav', () => ({
    default: hoisted.mockUseKnowledgeBaseLeftSidebarNav,
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
        item: {current: boolean; id: string; name: string};
        toLink: string;
    }) => (
        <a data-current={item.current} data-testid={`nav-item-${item.id}`} href={toLink}>
            {item.name}
        </a>
    ),
}));

const defaultMockReturn = {
    currentKnowledgeBaseId: 'kb-1',
    isLoading: false,
    knowledgeBases: [
        {id: 'kb-1', name: 'KB 1'},
        {id: 'kb-2', name: 'KB 2'},
    ],
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseLeftSidebarNav.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('KnowledgeBaseLeftSidebarNav', () => {
    it('renders sidebar nav', () => {
        render(<KnowledgeBaseLeftSidebarNav />);

        expect(screen.getByTestId('left-sidebar-nav')).toBeInTheDocument();
    });

    it('renders title', () => {
        render(<KnowledgeBaseLeftSidebarNav />);

        expect(screen.getByText('Knowledge Bases')).toBeInTheDocument();
    });

    it('renders knowledge base items', () => {
        render(<KnowledgeBaseLeftSidebarNav />);

        expect(screen.getByTestId('nav-item-kb-1')).toBeInTheDocument();
        expect(screen.getByTestId('nav-item-kb-2')).toBeInTheDocument();
    });

    it('renders knowledge base names', () => {
        render(<KnowledgeBaseLeftSidebarNav />);

        expect(screen.getByText('KB 1')).toBeInTheDocument();
        expect(screen.getByText('KB 2')).toBeInTheDocument();
    });

    it('marks current knowledge base as current', () => {
        render(<KnowledgeBaseLeftSidebarNav />);

        expect(screen.getByTestId('nav-item-kb-1')).toHaveAttribute('data-current', 'true');
        expect(screen.getByTestId('nav-item-kb-2')).toHaveAttribute('data-current', 'false');
    });

    it('renders correct links', () => {
        render(<KnowledgeBaseLeftSidebarNav />);

        expect(screen.getByTestId('nav-item-kb-1')).toHaveAttribute('href', '/automation/knowledge-bases/kb-1');
        expect(screen.getByTestId('nav-item-kb-2')).toHaveAttribute('href', '/automation/knowledge-bases/kb-2');
    });

    it('shows empty message when no knowledge bases', () => {
        hoisted.mockUseKnowledgeBaseLeftSidebarNav.mockReturnValue({
            ...defaultMockReturn,
            knowledgeBases: [],
        });

        render(<KnowledgeBaseLeftSidebarNav />);

        expect(screen.getByText('No knowledge bases.')).toBeInTheDocument();
    });

    it('does not render items when loading', () => {
        hoisted.mockUseKnowledgeBaseLeftSidebarNav.mockReturnValue({
            ...defaultMockReturn,
            isLoading: true,
        });

        render(<KnowledgeBaseLeftSidebarNav />);

        expect(screen.queryByTestId('nav-item-kb-1')).not.toBeInTheDocument();
    });
});
