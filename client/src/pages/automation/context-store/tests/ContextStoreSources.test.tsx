import {fireEvent, render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ContextStoreSources from '../ContextStoreSources';

const hoisted = vi.hoisted(() => {
    return {
        accountAuthorities: ['ROLE_ADMIN'],
        mockUseContextStoreSources: vi.fn(),
    };
});

vi.mock('../components/hooks/useContextStoreSources', () => ({
    default: hoisted.mockUseContextStoreSources,
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: vi.fn((selector: (state: {account: {authorities: string[]}}) => unknown) =>
        selector({account: {authorities: hoisted.accountAuthorities}})
    ),
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
    default: ({button, message, title}: {button?: React.ReactNode; message: string; title: string}) => (
        <div data-testid="empty-list">
            <h2>{title}</h2>

            <p>{message}</p>

            {button}
        </div>
    ),
}));

vi.mock('@/components/Button/Button', () => ({
    default: ({children}: {children: React.ReactNode}) => <button data-testid="button">{children}</button>,
}));

vi.mock('@/shared/components/EnvironmentSelect', () => ({
    default: () => <div data-testid="environment-select" />,
}));

vi.mock('../components/AddContextSourceDialog', () => ({
    default: ({trigger}: {trigger?: React.ReactNode}) => <div data-testid="add-source-dialog">{trigger}</div>,
}));

vi.mock('../components/ContextStoreSourceRowActionsMenu', () => ({
    default: ({isAdmin, source}: {isAdmin: boolean; source: {id: string}}) =>
        isAdmin ? <div data-testid={`actions-menu-${source.id}`} /> : null,
}));

vi.mock('../components/ContextStoreSourceDetailDialog', () => ({
    default: ({open, source}: {open: boolean; source?: {id: string} | null}) =>
        open && source ? <div data-testid={`source-detail-dialog-${source.id}`} /> : null,
}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({right, title}: {right?: React.ReactNode; title?: React.ReactNode}) => (
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
        leftSidebarHeader,
    }: {
        children: React.ReactNode;
        header: React.ReactNode;
        leftSidebarHeader: React.ReactNode;
        leftSidebarWidth: string;
    }) => (
        <div data-testid="layout-container">
            <div data-testid="layout-header">{header}</div>

            <div data-testid="layout-sidebar-header">{leftSidebarHeader}</div>

            <div data-testid="layout-content">{children}</div>
        </div>
    ),
}));

vi.mock('react-router-dom', () => ({
    useParams: () => ({id: 'store-1'}),
}));

const mockSources = [
    {
        cadence: '@daily',
        contextStoreId: 'store-1',
        enabled: true,
        entityName: 'contacts',
        id: 's-1',
        lastSyncRunAt: null,
        name: 'Source One',
        sourceComponentName: 'hubspot',
        sourceComponentVersion: 1,
        status: 'READY',
    },
    {
        cadence: '@hourly',
        contextStoreId: 'store-1',
        enabled: false,
        entityName: 'leads',
        id: 's-2',
        lastSyncRunAt: null,
        name: 'Source Two',
        sourceComponentName: 'salesforce',
        sourceComponentVersion: 2,
        status: 'FAILED',
    },
];

const defaultMockReturn = {
    error: null,
    isLoading: false,
    sources: mockSources,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.accountAuthorities = ['ROLE_ADMIN'];
    hoisted.mockUseContextStoreSources.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ContextStoreSources', () => {
    it('renders layout container', () => {
        render(<ContextStoreSources />);

        expect(screen.getByTestId('layout-container')).toBeInTheDocument();
    });

    it('shows loading state', () => {
        hoisted.mockUseContextStoreSources.mockReturnValue({...defaultMockReturn, isLoading: true});

        render(<ContextStoreSources />);

        expect(screen.getByTestId('page-loader-loading')).toBeInTheDocument();
    });

    it('shows error state', () => {
        hoisted.mockUseContextStoreSources.mockReturnValue({...defaultMockReturn, error: new Error('boom')});

        render(<ContextStoreSources />);

        expect(screen.getByTestId('page-loader-error')).toBeInTheDocument();
    });

    it('renders source rows for each source', () => {
        render(<ContextStoreSources />);

        expect(screen.getByText('Source One')).toBeInTheDocument();
        expect(screen.getByText('Source Two')).toBeInTheDocument();
    });

    it('renders status badge for each source with correct label', () => {
        render(<ContextStoreSources />);

        expect(screen.getByText('Ready')).toBeInTheDocument();
        expect(screen.getByText('Failed')).toBeInTheDocument();
    });

    it('renders the entity name for each source', () => {
        render(<ContextStoreSources />);

        expect(screen.getByText('contacts')).toBeInTheDocument();
        expect(screen.getByText('leads')).toBeInTheDocument();
    });

    it('renders actions menu for admin', () => {
        render(<ContextStoreSources />);

        expect(screen.getByTestId('actions-menu-s-1')).toBeInTheDocument();
        expect(screen.getByTestId('actions-menu-s-2')).toBeInTheDocument();
    });

    it('hides Add Source button and actions menu for non-admin', () => {
        hoisted.accountAuthorities = ['ROLE_USER'];

        render(<ContextStoreSources />);

        expect(screen.queryByTestId('actions-menu-s-1')).not.toBeInTheDocument();
        expect(screen.queryByTestId('add-source-dialog')).not.toBeInTheDocument();
    });

    it('shows empty list when no sources', () => {
        hoisted.mockUseContextStoreSources.mockReturnValue({...defaultMockReturn, sources: []});

        render(<ContextStoreSources />);

        expect(screen.getByTestId('empty-list')).toBeInTheDocument();
        expect(screen.getByText('No Sources')).toBeInTheDocument();
    });

    it('opens the detail dialog for the clicked row instead of navigating', () => {
        render(<ContextStoreSources />);

        expect(screen.queryByTestId('source-detail-dialog-s-1')).not.toBeInTheDocument();

        fireEvent.click(screen.getByTestId('context-store-source-row-s-1'));

        expect(screen.getByTestId('source-detail-dialog-s-1')).toBeInTheDocument();
    });
});
