import {getApiKeyMutationHooks} from '@/shared/components/api-keys/utils/getApiKeyMutationHooks';
import {ApiKey, PlatformType} from '@/shared/middleware/graphql';
import {QueryClient} from '@tanstack/react-query';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    createMutateMock: vi.fn(),
    deleteMutateMock: vi.fn(),
    useCreateApiKeyMutationMock: vi.fn(),
    useDeleteApiKeyMutationMock: vi.fn(),
    useUpdateApiKeyMutationMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    PlatformType: {Automation: 'AUTOMATION', Embedded: 'EMBEDDED'},
    useCreateApiKeyMutation: hoisted.useCreateApiKeyMutationMock,
    useDeleteApiKeyMutation: hoisted.useDeleteApiKeyMutationMock,
    useUpdateApiKeyMutation: hoisted.useUpdateApiKeyMutationMock,
}));

const ENVIRONMENT_ID = 1;

let queryClient: QueryClient;

const getOnSuccess = (hookMock: ReturnType<typeof vi.fn>) => hookMock.mock.calls[0][0].onSuccess;

describe('getApiKeyMutationHooks', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        queryClient = new QueryClient();

        vi.spyOn(queryClient, 'invalidateQueries');

        hoisted.useCreateApiKeyMutationMock.mockReturnValue({mutate: hoisted.createMutateMock});
        hoisted.useDeleteApiKeyMutationMock.mockReturnValue({isPending: false, mutate: hoisted.deleteMutateMock});
        hoisted.useUpdateApiKeyMutationMock.mockReturnValue({});
    });

    it('creates the key in the environment with the platform type and hands back the secret', () => {
        const onSuccess = vi.fn();

        const mutationHooks = getApiKeyMutationHooks({
            environmentId: ENVIRONMENT_ID,
            queryClient,
            queryKey: ['apiKeys'],
            type: PlatformType.Embedded,
        });

        const {result} = renderHook(() => mutationHooks.useCreateApiKeyMutation({onSuccess}));

        result.current.mutate({name: 'Backend'} as ApiKey);

        expect(hoisted.createMutateMock).toHaveBeenCalledWith({
            environmentId: ENVIRONMENT_ID,
            name: 'Backend',
            type: PlatformType.Embedded,
        });

        getOnSuccess(hoisted.useCreateApiKeyMutationMock)({createApiKey: 'secret-key'});

        expect(queryClient.invalidateQueries).toHaveBeenCalledWith({queryKey: ['apiKeys']});
        expect(onSuccess).toHaveBeenCalledWith('secret-key');
    });

    it('creates the key without a platform type when none is given', () => {
        const mutationHooks = getApiKeyMutationHooks({
            environmentId: ENVIRONMENT_ID,
            queryClient,
            queryKey: ['adminApiKeys'],
        });

        const {result} = renderHook(() => mutationHooks.useCreateApiKeyMutation());

        result.current.mutate({name: 'Admin'} as ApiKey);

        expect(hoisted.createMutateMock).toHaveBeenCalledWith({environmentId: ENVIRONMENT_ID, name: 'Admin'});
    });

    it('deletes the key by id and refreshes the list', () => {
        const onSuccess = vi.fn();

        const mutationHooks = getApiKeyMutationHooks({
            environmentId: ENVIRONMENT_ID,
            queryClient,
            queryKey: ['adminApiKeys'],
        });

        const {result} = renderHook(() => mutationHooks.useDeleteApiKeyMutation({onSuccess}));

        result.current.mutate({apiKeyId: '7'} as never);

        expect(hoisted.deleteMutateMock).toHaveBeenCalledWith({id: '7'});

        getOnSuccess(hoisted.useDeleteApiKeyMutationMock)({deleteApiKey: true});

        expect(queryClient.invalidateQueries).toHaveBeenCalledWith({queryKey: ['adminApiKeys']});
        expect(onSuccess).toHaveBeenCalledWith(true);
    });

    it('refreshes the list after an update', () => {
        const onSuccess = vi.fn();

        const mutationHooks = getApiKeyMutationHooks({
            environmentId: ENVIRONMENT_ID,
            queryClient,
            queryKey: ['apiKeys'],
        });

        renderHook(() => mutationHooks.useUpdateApiKeyMutation({onSuccess}));

        getOnSuccess(hoisted.useUpdateApiKeyMutationMock)({updateApiKey: true});

        expect(queryClient.invalidateQueries).toHaveBeenCalledWith({queryKey: ['apiKeys']});
        expect(onSuccess).toHaveBeenCalledWith(true);
    });
});
