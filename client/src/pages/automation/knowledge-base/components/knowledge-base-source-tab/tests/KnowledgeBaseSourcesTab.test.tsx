import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseSourcesTab from '../KnowledgeBaseSourcesTab';

const hoisted = vi.hoisted(() => {
    return {
        accountAuthorities: ['ROLE_ADMIN'],
        knowledgeBaseSourcesData: {
            data: {knowledgeBaseSources: [] as unknown[]},
            isLoading: false,
        },
    };
});

vi.mock('@/shared/middleware/graphql', () => ({
    useKnowledgeBaseSourcesQuery: () => hoisted.knowledgeBaseSourcesData,
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: vi.fn((selector: (state: {account: {authorities: string[]}}) => unknown) =>
        selector({account: {authorities: hoisted.accountAuthorities}})
    ),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 0})
    ),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 100})
    ),
}));

vi.mock('../AddKnowledgeBaseSourceDialog', () => ({
    default: ({knowledgeBaseId, trigger}: {knowledgeBaseId: string; trigger?: React.ReactNode}) => (
        <div data-testid={`add-kb-source-dialog-${knowledgeBaseId}`}>{trigger}</div>
    ),
}));

vi.mock('../KnowledgeBaseSourceRowActionsMenu', () => ({
    default: ({isAdmin, source}: {isAdmin: boolean; source: {id: string}}) =>
        isAdmin ? <div data-testid={`kbs-actions-${source.id}`} /> : null,
}));

vi.mock('@/components/Button/Button', () => ({
    default: ({children}: {children: React.ReactNode}) => <button data-testid="button">{children}</button>,
}));

const mockSources = [
    {
        cadence: '@daily',
        enabled: true,
        id: 'kbs-1',
        knowledgeBaseId: 'kb-1',
        lastSyncRunAt: null,
        name: 'Source One',
        sourceComponentName: 'github',
        status: 'READY',
    },
    {
        cadence: '@hourly',
        enabled: true,
        id: 'kbs-2',
        knowledgeBaseId: 'kb-2',
        lastSyncRunAt: null,
        name: 'Belongs to Other KB',
        sourceComponentName: 'notion',
        status: 'READY',
    },
];

beforeEach(() => {
    windowResizeObserver();
    hoisted.accountAuthorities = ['ROLE_ADMIN'];
    hoisted.knowledgeBaseSourcesData = {
        data: {knowledgeBaseSources: mockSources},
        isLoading: false,
    };
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('KnowledgeBaseSourcesTab', () => {
    it('renders the sync source heading', () => {
        render(<KnowledgeBaseSourcesTab knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Sync Sources')).toBeInTheDocument();
    });

    it('only renders sources that match the current knowledgeBaseId', () => {
        render(<KnowledgeBaseSourcesTab knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Source One')).toBeInTheDocument();
        expect(screen.queryByText('Belongs to Other KB')).not.toBeInTheDocument();
    });

    it('renders status badge with correct label', () => {
        render(<KnowledgeBaseSourcesTab knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Ready')).toBeInTheDocument();
    });

    it('shows loading state', () => {
        hoisted.knowledgeBaseSourcesData = {
            data: {knowledgeBaseSources: []},
            isLoading: true,
        };

        render(<KnowledgeBaseSourcesTab knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Loading sources...')).toBeInTheDocument();
    });

    it('shows empty message when no sources match', () => {
        hoisted.knowledgeBaseSourcesData = {
            data: {knowledgeBaseSources: []},
            isLoading: false,
        };

        render(<KnowledgeBaseSourcesTab knowledgeBaseId="kb-1" />);

        expect(screen.getByText(/No sync sources yet/)).toBeInTheDocument();
    });

    it('hides Add Source dialog and actions menu for non-admin', () => {
        hoisted.accountAuthorities = ['ROLE_USER'];

        render(<KnowledgeBaseSourcesTab knowledgeBaseId="kb-1" />);

        expect(screen.queryByTestId('add-kb-source-dialog-kb-1')).not.toBeInTheDocument();
        expect(screen.queryByTestId('kbs-actions-kbs-1')).not.toBeInTheDocument();
    });

    it('shows Add Source dialog and actions menu for admin', () => {
        render(<KnowledgeBaseSourcesTab knowledgeBaseId="kb-1" />);

        expect(screen.getByTestId('add-kb-source-dialog-kb-1')).toBeInTheDocument();
        expect(screen.getByTestId('kbs-actions-kbs-1')).toBeInTheDocument();
    });
});
