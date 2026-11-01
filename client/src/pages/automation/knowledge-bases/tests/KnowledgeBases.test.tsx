import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBases from '../KnowledgeBases';

const hoisted = vi.hoisted(() => {
    return {
        currentEnvironmentId: 1,
        currentWorkspaceId: 1049,
        mockUseKnowledgeBaseEmbeddingActiveQuery: vi.fn(),
        mockUseKnowledgeBases: vi.fn(),
        navigate: vi.fn(),
    };
});

vi.mock('../components/hooks/useKnowledgeBases', () => ({
    default: hoisted.mockUseKnowledgeBases,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector: (state: {currentWorkspaceId: number}) => number) =>
        selector({currentWorkspaceId: hoisted.currentWorkspaceId})
    ),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector: (state: {currentEnvironmentId: number}) => number) =>
        selector({currentEnvironmentId: hoisted.currentEnvironmentId})
    ),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useKnowledgeBaseEmbeddingActiveQuery: hoisted.mockUseKnowledgeBaseEmbeddingActiveQuery,
    };
});

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => hoisted.navigate,
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

vi.mock('@/components/EmptyList', () => ({
    default: ({
        button,
        icon,
        message,
        title,
    }: {
        button: React.ReactNode;
        icon: React.ReactNode;
        message: string;
        title: string;
    }) => (
        <div data-testid="empty-list">
            {icon}

            <h2>{title}</h2>

            <p>{message}</p>

            {button}
        </div>
    ),
}));

vi.mock('@/components/Button/Button', () => ({
    default: ({children, label, onClick}: {children?: React.ReactNode; label?: string; onClick?: () => void}) => (
        <button data-testid="button" onClick={onClick}>
            {children || label}
        </button>
    ),
}));

vi.mock('../components/CreateKnowledgeBaseDialog', () => ({
    default: ({trigger, workspaceId}: {trigger?: React.ReactNode; workspaceId: string}) => (
        <div data-testid={`create-dialog-${workspaceId}`}>{trigger}</div>
    ),
}));

vi.mock('../components/KnowledgeBasesFilterTitle', () => ({
    default: () => <div data-testid="filter-title">Filter Title</div>,
}));

vi.mock('../components/KnowledgeBasesLeftSidebarNav', () => ({
    default: () => <nav data-testid="left-sidebar-nav">Sidebar</nav>,
}));

vi.mock('../components/knowledge-base-list/KnowledgeBaseList', () => ({
    default: ({
        knowledgeBases,
    }: {
        allTags: unknown[];
        knowledgeBases: unknown[];
        tagsByKnowledgeBaseData: unknown[];
    }) => <div data-testid="knowledge-base-list">List ({knowledgeBases.length} items)</div>,
}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({
        right,
        title,
    }: {
        centerTitle?: boolean;
        position?: string;
        right?: React.ReactNode;
        title?: React.ReactNode;
    }) => (
        <header data-testid="header">
            <div data-testid="header-title">{title}</div>

            <div data-testid="header-right">{right}</div>
        </header>
    ),
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({
        children,
        header,
        leftSidebarBody,
        leftSidebarHeader,
        leftSidebarHidden,
    }: {
        children: React.ReactNode;
        header: React.ReactNode;
        leftSidebarBody: React.ReactNode;
        leftSidebarHeader: React.ReactNode;
        leftSidebarHidden?: boolean;
        leftSidebarWidth: string;
    }) => (
        <div data-testid="layout-container">
            <div data-testid="layout-header">{header}</div>

            {!leftSidebarHidden && (
                <>
                    <div data-testid="layout-sidebar-header">{leftSidebarHeader}</div>

                    <div data-testid="layout-sidebar-body">{leftSidebarBody}</div>
                </>
            )}

            <div data-testid="layout-content">{children}</div>
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
];

const mockTagsByKnowledgeBase = [{knowledgeBaseId: 'kb-1', tags: [{id: '1', name: 'Tag 1'}]}];

const defaultMockReturn = {
    allTags: mockAllTags,
    error: null,
    filteredKnowledgeBases: mockKnowledgeBases,
    isLoading: false,
    knowledgeBases: mockKnowledgeBases,
    tagId: undefined,
    tagsByKnowledgeBaseData: mockTagsByKnowledgeBase,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBases.mockReturnValue({...defaultMockReturn});
    hoisted.mockUseKnowledgeBaseEmbeddingActiveQuery.mockReturnValue({
        data: {knowledgeBaseEmbeddingActive: true},
    });
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('KnowledgeBases', () => {
    it('renders layout container', () => {
        render(<KnowledgeBases />);

        expect(screen.getByTestId('layout-container')).toBeInTheDocument();
    });

    it('renders left sidebar nav', () => {
        render(<KnowledgeBases />);

        expect(screen.getByTestId('left-sidebar-nav')).toBeInTheDocument();
    });

    it('renders sidebar header', () => {
        render(<KnowledgeBases />);

        expect(screen.getByText('Knowledge Bases')).toBeInTheDocument();
    });

    it('shows loading state', () => {
        hoisted.mockUseKnowledgeBases.mockReturnValue({
            ...defaultMockReturn,
            isLoading: true,
        });

        render(<KnowledgeBases />);

        expect(screen.getByTestId('page-loader-loading')).toBeInTheDocument();
    });

    it('shows error state', () => {
        hoisted.mockUseKnowledgeBases.mockReturnValue({
            ...defaultMockReturn,
            error: new Error('Test error'),
        });

        render(<KnowledgeBases />);

        expect(screen.getByTestId('page-loader-error')).toBeInTheDocument();
    });

    it('renders knowledge base list when has data', () => {
        render(<KnowledgeBases />);

        expect(screen.getByTestId('knowledge-base-list')).toBeInTheDocument();
    });

    it('shows correct item count in list', () => {
        render(<KnowledgeBases />);

        expect(screen.getByTestId('knowledge-base-list')).toHaveTextContent('List (2 items)');
    });

    it('renders main header when knowledge bases exist', () => {
        render(<KnowledgeBases />);

        // The main header should contain the filter title
        expect(screen.getByTestId('layout-header')).toBeInTheDocument();
    });

    it('renders filter title in header', () => {
        render(<KnowledgeBases />);

        expect(screen.getByTestId('filter-title')).toBeInTheDocument();
    });

    it('renders create dialog in header', () => {
        render(<KnowledgeBases />);

        expect(screen.getByTestId('create-dialog-1049')).toBeInTheDocument();
    });

    it('shows the getting-started guide without the sidebar when there are no knowledge bases', () => {
        hoisted.mockUseKnowledgeBases.mockReturnValue({
            ...defaultMockReturn,
            filteredKnowledgeBases: [],
            knowledgeBases: [],
        });

        render(<KnowledgeBases />);

        expect(screen.getByText('Create your first knowledge base')).toBeInTheDocument();
        expect(screen.getByText('Turn on an embedding model').closest('li')).not.toHaveAttribute('aria-current');
        expect(screen.getByText('Create a knowledge base').closest('li')).toHaveAttribute('aria-current', 'step');
        expect(screen.getByTestId('create-dialog-1049')).toHaveTextContent('Create Knowledge Base');
        expect(screen.queryByTestId('layout-sidebar-body')).not.toBeInTheDocument();
        expect(screen.getByTestId('header-title')).toHaveTextContent('Knowledge Bases');
    });

    it('shows filtered empty message when tag filter active', () => {
        hoisted.mockUseKnowledgeBases.mockReturnValue({
            ...defaultMockReturn,
            filteredKnowledgeBases: [],
            tagId: '1',
        });

        render(<KnowledgeBases />);

        expect(screen.getByText('No Matching Knowledge Bases')).toBeInTheDocument();
        expect(screen.getByText('No knowledge bases match the selected tag.')).toBeInTheDocument();
        expect(screen.getByTestId('left-sidebar-nav')).toBeInTheDocument();
    });

    it('does not render main header when no knowledge bases', () => {
        hoisted.mockUseKnowledgeBases.mockReturnValue({
            ...defaultMockReturn,
            filteredKnowledgeBases: [],
            knowledgeBases: [],
        });

        render(<KnowledgeBases />);

        expect(screen.queryByTestId('filter-title')).not.toBeInTheDocument();
    });
});

describe('KnowledgeBases embedding gate', () => {
    beforeEach(() => {
        windowResizeObserver();
        hoisted.mockUseKnowledgeBases.mockReturnValue({...defaultMockReturn});
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('asks to turn the embedding model back on when knowledge bases exist but embedding is not active', async () => {
        hoisted.mockUseKnowledgeBaseEmbeddingActiveQuery.mockReturnValue({
            data: {knowledgeBaseEmbeddingActive: false},
        });

        render(<KnowledgeBases />);

        expect(screen.getByRole('heading', {name: 'Turn on an embedding model'})).toBeInTheDocument();

        await userEvent.click(screen.getByText('Open AI Providers'));

        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/settings/ai-providers');
        expect(screen.queryByTestId('knowledge-base-list')).not.toBeInTheDocument();
        expect(screen.queryByTestId('empty-list')).not.toBeInTheDocument();
        expect(screen.queryByTestId('filter-title')).not.toBeInTheDocument();
        expect(screen.queryByText('New Knowledge Base')).not.toBeInTheDocument();
        expect(screen.queryByTestId('layout-sidebar-body')).not.toBeInTheDocument();
    });

    it('shows the list when embedding is active', () => {
        hoisted.mockUseKnowledgeBaseEmbeddingActiveQuery.mockReturnValue({
            data: {knowledgeBaseEmbeddingActive: true},
        });

        render(<KnowledgeBases />);

        expect(screen.queryByText('Turn on an embedding model')).not.toBeInTheDocument();
        expect(screen.getByTestId('knowledge-base-list')).toBeInTheDocument();
    });

    it('shows the list by default when query has no data (fail-open)', () => {
        hoisted.mockUseKnowledgeBaseEmbeddingActiveQuery.mockReturnValue({data: undefined});

        render(<KnowledgeBases />);

        expect(screen.queryByText('Turn on an embedding model')).not.toBeInTheDocument();
    });

    it('shows the loader while the embedding query is loading', () => {
        hoisted.mockUseKnowledgeBaseEmbeddingActiveQuery.mockReturnValue({data: undefined, isLoading: true});

        render(<KnowledgeBases />);

        expect(screen.getByTestId('page-loader-loading')).toBeInTheDocument();
        expect(screen.queryByTestId('knowledge-base-list')).not.toBeInTheDocument();
    });

    it('starts the guide at the embedding model when there are no knowledge bases', () => {
        hoisted.mockUseKnowledgeBaseEmbeddingActiveQuery.mockReturnValue({
            data: {knowledgeBaseEmbeddingActive: false},
        });

        hoisted.mockUseKnowledgeBases.mockReturnValue({
            ...defaultMockReturn,
            filteredKnowledgeBases: [],
            knowledgeBases: [],
        });

        render(<KnowledgeBases />);

        expect(screen.getByText('Create your first knowledge base')).toBeInTheDocument();
        expect(screen.getByText('Turn on an embedding model').closest('li')).toHaveAttribute('aria-current', 'step');
        expect(screen.getByText('Open AI Providers')).toBeInTheDocument();
        expect(screen.queryByText('Create Knowledge Base')).not.toBeInTheDocument();
    });
});
