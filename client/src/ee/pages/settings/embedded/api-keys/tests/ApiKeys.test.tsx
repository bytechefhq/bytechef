import ApiKeys from '@/ee/pages/settings/embedded/api-keys/ApiKeys';
import {useApiKeysStore} from '@/ee/shared/components/api-keys/stores/useApiKeysStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

interface MutationOptionsI {
    onSuccess?: (data: Record<string, unknown>) => void;
}

const hoisted = vi.hoisted(() => ({
    createVariables: [] as unknown[],
    deleteVariables: [] as unknown[],
    embeddedApiKeysQuery: vi.fn(),
    grants: {} as Record<number, string[]>,
    invalidateQueries: vi.fn(),
    sharedOperation: vi.fn(),
    updateVariables: [] as unknown[],
    useRealScopeHook: false,
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/hooks/useHasEmbeddedScope')>();
    const {environmentStore: mockedEnvironmentStore} = await import('@/shared/stores/useEnvironmentStore');

    return {
        ...actual,
        useHasEmbeddedScope: (scope: string, environmentId?: number) =>
            hoisted.useRealScopeHook
                ? actual.useHasEmbeddedScope(scope as never, environmentId)
                : (
                      hoisted.grants[environmentId ?? mockedEnvironmentStore.getState().currentEnvironmentId] ?? []
                  ).includes(scope),
    };
});

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useApiKeysQuery: hoisted.sharedOperation,
        useCreateApiKeyMutation: hoisted.sharedOperation,
        useCreateEmbeddedApiKeyMutation: (options: MutationOptionsI) => ({
            mutate: (variables: unknown) => {
                hoisted.createVariables.push(variables);

                options.onSuccess?.({createEmbeddedApiKey: 'sk_created_full_secret_value'});
            },
        }),
        useDeleteApiKeyMutation: hoisted.sharedOperation,
        useDeleteEmbeddedApiKeyMutation: (options: MutationOptionsI) => ({
            isPending: false,
            mutate: (variables: unknown) => {
                hoisted.deleteVariables.push(variables);

                options.onSuccess?.({deleteEmbeddedApiKey: true});
            },
        }),
        useEmbeddedApiKeysQuery: hoisted.embeddedApiKeysQuery,
        useUpdateApiKeyMutation: hoisted.sharedOperation,
        useUpdateEmbeddedApiKeyMutation: (options: MutationOptionsI) => ({
            mutate: (variables: unknown) => {
                hoisted.updateVariables.push(variables);

                options.onSuccess?.({updateEmbeddedApiKey: {id: '1', name: 'Renamed key'}});
            },
        }),
    };
});

vi.mock('@tanstack/react-query', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@tanstack/react-query')>();

    return {
        ...actual,
        useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
    };
});

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({children, header}: {children: ReactNode; header: ReactNode}) => (
        <div>
            {header}

            {children}
        </div>
    ),
}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({right, title}: {right?: ReactNode; title?: ReactNode}) => (
        <div>
            <h1>{title}</h1>

            {right}
        </div>
    ),
}));

const DEVELOPMENT = 0;
const PRODUCTION = 2;
const PLAINTEXT_SECRET = 'sk_live_plaintext_secret_ab12cd';

const EMBEDDED_API_KEYS = [
    {
        createdBy: 'admin@localhost.com',
        createdDate: 1_700_000_000_000,
        id: '1',
        lastUsedDate: 1_700_000_000_000,
        name: 'Primary key',
        secretKey: PLAINTEXT_SECRET,
    },
    {
        createdBy: 'admin@localhost.com',
        createdDate: 1_700_000_000_000,
        id: '2',
        lastUsedDate: 1_700_000_000_000,
        name: 'Obfuscated key',
        secretKey: '..........................zz99yy',
    },
];

const grantInProduction = (scopes: string[]) => {
    hoisted.grants = {[DEVELOPMENT]: ['API_KEY_CREATE', 'API_KEY_DELETE', 'API_KEY_VIEW'], [PRODUCTION]: scopes};
};

describe('embedded ApiKeys', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.createVariables = [];
        hoisted.deleteVariables = [];
        hoisted.embeddedApiKeysQuery.mockReset();
        hoisted.embeddedApiKeysQuery.mockReturnValue({
            data: {embeddedApiKeys: EMBEDDED_API_KEYS},
            error: null,
            isLoading: false,
        });
        hoisted.grants = {};
        hoisted.invalidateQueries.mockReset();
        hoisted.sharedOperation.mockReset();
        hoisted.updateVariables = [];
        hoisted.useRealScopeHook = false;

        act(() => {
            environmentStore.setState({currentEnvironmentId: PRODUCTION});
            useApiKeysStore.setState({
                currentApiKey: undefined,
                secretKey: undefined,
                showDeleteDialog: false,
                showEditDialog: false,
            });
        });
    });

    afterEach(() => {
        act(() => {
            authenticationStore.setState({account: undefined, authenticated: false});
            environmentStore.setState({currentEnvironmentId: DEVELOPMENT});
        });

        resetAll();
    });

    it('testEmbeddedApiKeysUseTheEmbeddedOperations', async () => {
        grantInProduction(['API_KEY_CREATE', 'API_KEY_DELETE', 'API_KEY_VIEW']);

        render(<ApiKeys />);

        expect(hoisted.embeddedApiKeysQuery).toHaveBeenCalledWith({environmentId: `${PRODUCTION}`});
        expect(screen.getByText('Primary key')).toBeInTheDocument();
        expect(screen.queryByText(PLAINTEXT_SECRET)).not.toBeInTheDocument();
        expect(screen.getByText(`${'.'.repeat(26)}ab12cd`)).toBeInTheDocument();
        expect(screen.getByText('..........................zz99yy')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'New API Key'}));
        await userEvent.type(await screen.findByRole('textbox'), 'CI key');
        await userEvent.click(screen.getByRole('button', {name: 'Create API Key'}));

        await waitFor(() =>
            expect(hoisted.createVariables).toEqual([{input: {environmentId: `${PRODUCTION}`, name: 'CI key'}}])
        );
        expect(await screen.findByDisplayValue('sk_created_full_secret_value')).toBeInTheDocument();
        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['EmbeddedApiKeys']});

        await userEvent.click(screen.getByRole('button', {name: 'Done'}));

        await userEvent.click(screen.getAllByRole('button', {name: 'Edit API Key'})[0]);

        const nameInput = await screen.findByRole('textbox');

        await userEvent.clear(nameInput);
        await userEvent.type(nameInput, 'Renamed key');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(hoisted.updateVariables).toEqual([{input: {id: '1', name: 'Renamed key'}}]));

        await userEvent.click(screen.getAllByRole('button', {name: 'Delete API Key'})[0]);
        await userEvent.click(await screen.findByRole('button', {name: 'Delete'}));

        await waitFor(() => expect(hoisted.deleteVariables).toEqual([{id: '1'}]));
        expect(hoisted.invalidateQueries).toHaveBeenCalledTimes(3);
        expect(hoisted.sharedOperation).not.toHaveBeenCalled();
    });

    it('testAViewerHasNoApiKeysPage', () => {
        grantInProduction(['CONNECTION_VIEW', 'EMBEDDED_VIEW']);

        render(<ApiKeys />);

        expect(screen.getByText('You do not have permission to view the API keys of this environment.')).toBeVisible();
        expect(screen.queryByText('Primary key')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New API Key'})).not.toBeInTheDocument();
        expect(hoisted.embeddedApiKeysQuery).not.toHaveBeenCalled();
    });

    it('testApiKeyControlsFollowTheApiKeyScopes', () => {
        grantInProduction(['API_KEY_VIEW']);

        const viewOnlyRender = render(<ApiKeys />);

        expect(screen.getByText('Primary key')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New API Key'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Edit API Key'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Delete API Key'})).not.toBeInTheDocument();

        viewOnlyRender.unmount();

        grantInProduction(['API_KEY_DELETE', 'API_KEY_VIEW']);

        render(<ApiKeys />);

        expect(screen.queryByRole('button', {name: 'Edit API Key'})).not.toBeInTheDocument();
        expect(screen.getAllByRole('button', {name: 'Delete API Key'})).toHaveLength(2);
    });

    it('testATenantAdminSeesEveryControl', () => {
        hoisted.useRealScopeHook = true;

        act(() => {
            authenticationStore.setState({
                account: {authorities: ['ROLE_ADMIN'], login: 'admin'} as never,
                authenticated: true,
            });
        });

        render(<ApiKeys />);

        expect(screen.getByRole('button', {name: 'New API Key'})).toBeInTheDocument();
        expect(screen.getAllByRole('button', {name: 'Edit API Key'})).toHaveLength(2);
        expect(screen.getAllByRole('button', {name: 'Delete API Key'})).toHaveLength(2);
    });
});
