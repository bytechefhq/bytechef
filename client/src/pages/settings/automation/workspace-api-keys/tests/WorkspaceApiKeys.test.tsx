import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ApiKeysProviderStateI} from '@/shared/components/api-keys/providers/apiKeysProvider';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {ApiKey, useWorkspaceApiKeysQuery} from '@/shared/middleware/graphql';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {QueryClient, QueryClientProvider, UseMutationResult, UseQueryResult} from '@tanstack/react-query';
import {render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkspaceApiKeys from '../WorkspaceApiKeys';

const probe = vi.hoisted(() => ({
    createMutation: undefined as UseMutationResult<unknown, Error> | undefined,
    createProps: {onError: vi.fn(), onSuccess: vi.fn()},
    deleteMutation: undefined as UseMutationResult<unknown, Error> | undefined,
    deleteProps: {onError: vi.fn(), onSuccess: vi.fn()},
    provider: undefined as ApiKeysProviderStateI | undefined,
    query: undefined as UseQueryResult<ApiKey[], Error> | undefined,
    updateMutation: undefined as UseMutationResult<unknown, Error> | undefined,
    updateProps: {onError: vi.fn(), onSuccess: vi.fn()},
}));

const mutationOptions = vi.hoisted(() => ({
    create: undefined as {onError: (error: unknown) => void; onSuccess: (data: unknown) => void} | undefined,
    delete: undefined as {onError: (error: unknown) => void; onSuccess: (data: unknown) => void} | undefined,
    update: undefined as {onError: (error: unknown) => void; onSuccess: (data: unknown) => void} | undefined,
}));

const mutate = vi.hoisted(() => ({create: vi.fn(), delete: vi.fn(), update: vi.fn()}));

vi.mock('@/shared/components/api-keys/ApiKeysContent', async () => {
    const {useApiKeysProvider} = await vi.importActual<
        typeof import('@/shared/components/api-keys/providers/apiKeysProvider')
    >('@/shared/components/api-keys/providers/apiKeysProvider');

    const ApiKeysContentProbe = () => {
        const provider = useApiKeysProvider();

        probe.provider = provider;
        probe.query = provider.useApiKeysQuery();
        probe.createMutation = provider.useCreateApiKeyMutation(probe.createProps);
        probe.deleteMutation = provider.useDeleteApiKeyMutation(probe.deleteProps);
        probe.updateMutation = provider.useUpdateApiKeyMutation(probe.updateProps);

        return null;
    };

    return {default: ApiKeysContentProbe};
});

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: vi.fn(),
}));

vi.mock('@/shared/hooks/useLoadWorkspaceScopes', () => ({
    useLoadWorkspaceScopes: vi.fn(() => ({loading: false})),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateWorkspaceApiKeyMutation: vi.fn((options) => {
        mutationOptions.create = options;

        return {isPending: false, mutate: mutate.create};
    }),
    useDeleteWorkspaceApiKeyMutation: vi.fn((options) => {
        mutationOptions.delete = options;

        return {isPending: false, mutate: mutate.delete};
    }),
    useUpdateWorkspaceApiKeyMutation: vi.fn((options) => {
        mutationOptions.update = options;

        return {isPending: false, mutate: mutate.update};
    }),
    useWorkspaceApiKeysQuery: vi.fn(),
}));

const apiKey = {id: '3', name: 'MCP client', secretKey: '********1234'} as ApiKey;

const renderWorkspaceApiKeys = (scopes: string[]) => {
    vi.mocked(useHasWorkspaceScope).mockImplementation((workspaceId, scope) => scopes.includes(scope));

    const queryClient = new QueryClient();

    const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

    render(
        <QueryClientProvider client={queryClient}>
            <WorkspaceApiKeys />
        </QueryClientProvider>
    );

    return {invalidateQueriesSpy};
};

describe('WorkspaceApiKeys', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        environmentStore.setState({currentEnvironmentId: 1});
        useWorkspaceStore.setState({currentWorkspaceId: 7});

        vi.mocked(useWorkspaceApiKeysQuery).mockReturnValue({
            data: {workspaceApiKeys: [apiKey]},
            error: null,
            isLoading: false,
        } as unknown as ReturnType<typeof useWorkspaceApiKeysQuery>);
    });

    it('lists the keys of the current environment and workspace for a member who can view them', () => {
        renderWorkspaceApiKeys(['API_KEY_VIEW', 'API_KEY_CREATE', 'API_KEY_DELETE']);

        expect(useWorkspaceApiKeysQuery).toHaveBeenCalledWith({environmentId: '1', workspaceId: '7'}, {enabled: true});
        expect(probe.query?.data).toEqual([apiKey]);
        expect(probe.provider?.permissions).toEqual({canCreate: true, canManage: true, canView: true, loading: false});
    });

    it('does not query the keys for a member without the view scope', () => {
        renderWorkspaceApiKeys([]);

        expect(useWorkspaceApiKeysQuery).toHaveBeenCalledWith({environmentId: '1', workspaceId: '7'}, {enabled: false});
        expect(probe.provider?.permissions).toEqual({
            canCreate: false,
            canManage: false,
            canView: false,
            loading: false,
        });
    });

    it('creates a key in the current environment and workspace and hands back its secret', () => {
        const {invalidateQueriesSpy} = renderWorkspaceApiKeys(['API_KEY_CREATE']);

        probe.createMutation?.mutate({name: 'Agent'} as never);

        expect(mutate.create).toHaveBeenCalledWith({environmentId: 1, name: 'Agent', workspaceId: 7});

        mutationOptions.create?.onSuccess({createWorkspaceApiKey: 'new-secret'});

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['workspaceApiKeys']});
        expect(probe.createProps.onSuccess).toHaveBeenCalledWith('new-secret');

        const error = new Error('Not allowed');

        mutationOptions.create?.onError(error);

        expect(probe.createProps.onError).toHaveBeenCalledWith(error);
    });

    it('deletes a key and refreshes the list', () => {
        const {invalidateQueriesSpy} = renderWorkspaceApiKeys(['API_KEY_DELETE']);

        probe.deleteMutation?.mutate({apiKeyId: '3'} as never);

        expect(mutate.delete).toHaveBeenCalledWith({apiKeyId: '3'});

        mutationOptions.delete?.onSuccess({deleteWorkspaceApiKey: true});

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['workspaceApiKeys']});
        expect(probe.deleteProps.onSuccess).toHaveBeenCalledWith(true);

        const error = new Error('Not allowed');

        mutationOptions.delete?.onError(error);

        expect(probe.deleteProps.onError).toHaveBeenCalledWith(error);
    });

    it('renames a key and refreshes the list', () => {
        const {invalidateQueriesSpy} = renderWorkspaceApiKeys(['API_KEY_DELETE']);

        probe.updateMutation?.mutate({id: '3', name: 'Renamed'} as never);

        expect(mutate.update).toHaveBeenCalledWith({apiKeyId: '3', name: 'Renamed'});

        mutationOptions.update?.onSuccess({updateWorkspaceApiKey: true});

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['workspaceApiKeys']});
        expect(probe.updateProps.onSuccess).toHaveBeenCalledWith(true);

        const error = new Error('Not allowed');

        mutationOptions.update?.onError(error);

        expect(probe.updateProps.onError).toHaveBeenCalledWith(error);
    });
});
