import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBase from '../KnowledgeBase';

const hoisted = vi.hoisted(() => {
    return {
        mockUseKnowledgeBase: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBase', () => ({
    default: hoisted.mockUseKnowledgeBase,
}));

vi.mock('@/components/PageLoader', () => ({
    default: ({children, errors, loading}: {children: React.ReactNode; errors: unknown[]; loading: boolean}) =>
        loading ? (
            <div data-testid="page-loader-loading">Loading...</div>
        ) : errors.filter(Boolean).length > 0 ? (
            <div data-testid="page-loader-error">Error</div>
        ) : (
            <div data-testid="page-loader-content">{children}</div>
        ),
}));

vi.mock('../components/KnowledgeBaseHeader', () => ({
    default: ({knowledgeBaseName}: {knowledgeBaseName?: string; onBackClick: () => void}) => (
        <header data-testid="knowledge-base-header">{knowledgeBaseName || 'Loading...'}</header>
    ),
}));

vi.mock('../components/KnowledgeBaseInfoCard', () => ({
    default: ({knowledgeBase}: {knowledgeBase: {name: string}}) => (
        <div data-testid="knowledge-base-info-card">{knowledgeBase.name}</div>
    ),
}));

vi.mock('../components/KnowledgeBaseLeftSidebarNav', () => ({
    default: () => <nav data-testid="knowledge-base-left-sidebar">Sidebar</nav>,
}));

vi.mock('../components/KnowledgeBaseTabs', () => ({
    default: ({documents, knowledgeBaseId}: {documents: unknown[]; knowledgeBaseId: string}) => (
        <div data-testid="knowledge-base-tabs">
            Tabs for {knowledgeBaseId} ({documents.length} docs)
        </div>
    ),
}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({title}: {position?: string; title: string}) => <header data-testid="sidebar-header">{title}</header>,
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({
        children,
        header,
        leftSidebarBody,
        leftSidebarHeader,
    }: {
        children: React.ReactNode;
        header: React.ReactNode;
        leftSidebarBody: React.ReactNode;
        leftSidebarHeader: React.ReactNode;
        leftSidebarWidth: string;
    }) => (
        <div data-testid="layout-container">
            <div data-testid="layout-header">{header}</div>

            <div data-testid="layout-sidebar-header">{leftSidebarHeader}</div>

            <div data-testid="layout-sidebar-body">{leftSidebarBody}</div>

            <div data-testid="layout-content">{children}</div>
        </div>
    ),
}));

const mockKnowledgeBase = {
    description: 'Test description',
    documents: [
        {id: 'doc-1', name: 'Document 1'},
        {id: 'doc-2', name: 'Document 2'},
    ],
    id: 'kb-1',
    maxChunkSize: 1024,
    minChunkSizeChars: 1,
    name: 'Test KB',
    overlap: 200,
};

const defaultMockReturn = {
    documents: mockKnowledgeBase.documents,
    error: null,
    handleBackClick: vi.fn(),
    isLoading: false,
    knowledgeBase: mockKnowledgeBase,
    knowledgeBaseId: 'kb-1',
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBase.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('KnowledgeBase', () => {
    it('renders layout container', () => {
        render(<KnowledgeBase />);

        expect(screen.getByTestId('layout-container')).toBeInTheDocument();
    });

    it('renders header with knowledge base name', () => {
        render(<KnowledgeBase />);

        expect(screen.getByTestId('knowledge-base-header')).toHaveTextContent('Test KB');
    });

    it('renders sidebar header', () => {
        render(<KnowledgeBase />);

        expect(screen.getByTestId('sidebar-header')).toHaveTextContent('Knowledge Base');
    });

    it('renders left sidebar nav', () => {
        render(<KnowledgeBase />);

        expect(screen.getByTestId('knowledge-base-left-sidebar')).toBeInTheDocument();
    });

    it('shows loading state', () => {
        hoisted.mockUseKnowledgeBase.mockReturnValue({
            ...defaultMockReturn,
            isLoading: true,
        });

        render(<KnowledgeBase />);

        expect(screen.getByTestId('page-loader-loading')).toBeInTheDocument();
    });

    it('shows error state', () => {
        hoisted.mockUseKnowledgeBase.mockReturnValue({
            ...defaultMockReturn,
            error: new Error('Test error'),
        });

        render(<KnowledgeBase />);

        expect(screen.getByTestId('page-loader-error')).toBeInTheDocument();
    });

    it('renders knowledge base info card when loaded', () => {
        render(<KnowledgeBase />);

        expect(screen.getByTestId('knowledge-base-info-card')).toBeInTheDocument();
    });

    it('renders knowledge base tabs when loaded', () => {
        render(<KnowledgeBase />);

        expect(screen.getByTestId('knowledge-base-tabs')).toBeInTheDocument();
    });

    it('passes correct props to tabs', () => {
        render(<KnowledgeBase />);

        expect(screen.getByTestId('knowledge-base-tabs')).toHaveTextContent('Tabs for kb-1 (2 docs)');
    });

    it('does not render content when knowledge base is undefined', () => {
        hoisted.mockUseKnowledgeBase.mockReturnValue({
            ...defaultMockReturn,
            knowledgeBase: undefined,
        });

        render(<KnowledgeBase />);

        expect(screen.queryByTestId('knowledge-base-info-card')).not.toBeInTheDocument();
        expect(screen.queryByTestId('knowledge-base-tabs')).not.toBeInTheDocument();
    });
});
