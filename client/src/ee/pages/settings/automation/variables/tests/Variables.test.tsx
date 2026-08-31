import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Variables from '../Variables';

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    scopes: ['VARIABLE_VIEW', 'VARIABLE_MANAGE'] as string[],
    variables: [{environmentId: '0', id: '1', name: 'API_URL', value: 'x'}] as unknown[],
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateWorkspaceVariableMutation: vi.fn(() => ({mutate: hoisted.createMutate})),
    useDeleteWorkspaceVariableMutation: vi.fn(() => ({mutate: vi.fn()})),
    useMyWorkspaceScopesQuery: vi.fn(() => ({data: {myWorkspaceScopes: hoisted.scopes}, isLoading: false})),
    useUpdateWorkspaceVariableMutation: vi.fn(() => ({mutate: vi.fn()})),
    useWorkspaceVariablesQuery: vi.fn(() => ({
        data: {workspaceVariables: hoisted.variables},
        error: null,
        isLoading: false,
    })),
}));
vi.mock(import('@tanstack/react-query'), async (importOriginal) => ({
    ...(await importOriginal()),
    useQueryClient: vi.fn(() => ({invalidateQueries: vi.fn()}) as never),
}));

describe('Variables (workspace)', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.scopes = ['VARIABLE_VIEW', 'VARIABLE_MANAGE'];
        hoisted.variables = [{environmentId: '0', id: '1', name: 'API_URL', value: 'x'}];

        useWorkspaceStore.setState({currentWorkspaceId: 7});
    });

    it('renders the workspace variables', () => {
        render(<Variables />);

        expect(screen.getByText('API_URL')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'New Variable'})).toBeInTheDocument();
    });

    it('hides management controls without VARIABLE_MANAGE', () => {
        hoisted.scopes = ['VARIABLE_VIEW'];

        render(<Variables />);

        expect(screen.queryByRole('button', {name: 'New Variable'})).not.toBeInTheDocument();
    });
});
