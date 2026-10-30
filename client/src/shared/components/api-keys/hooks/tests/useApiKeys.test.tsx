import useApiKeys from '@/shared/components/api-keys/hooks/useApiKeys';
import {ApiKeysProvider, SimpleMutationProps} from '@/shared/components/api-keys/providers/apiKeysProvider';
import {useApiKeysStore} from '@/shared/components/api-keys/stores/useApiKeysStore';
import {ApiKey} from '@/shared/middleware/graphql';
import {UseMutationResult, UseQueryResult} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    deleteMutateMock: vi.fn(),
    deleteProps: undefined as SimpleMutationProps | undefined,
}));

const mutation = (mutate: () => void) => ({isPending: false, mutate}) as unknown as UseMutationResult<unknown, Error>;

const wrapper = ({children}: {children: ReactNode}) => (
    <ApiKeysProvider
        value={{
            useApiKeysQuery: () =>
                ({data: [], error: null, isLoading: false}) as unknown as UseQueryResult<ApiKey[], Error>,
            useCreateApiKeyMutation: () => mutation(vi.fn()),
            useDeleteApiKeyMutation: (props) => {
                hoisted.deleteProps = props;

                return mutation(hoisted.deleteMutateMock);
            },
            useUpdateApiKeyMutation: () => mutation(vi.fn()),
        }}
    >
        {children}
    </ApiKeysProvider>
);

describe('useApiKeys', () => {
    beforeEach(() => {
        hoisted.deleteMutateMock.mockReset();

        useApiKeysStore.setState({currentApiKey: {id: '5', name: 'Production key'}, showDeleteDialog: true});
    });

    it('deletes the key by id and closes the delete dialog', () => {
        hoisted.deleteMutateMock.mockImplementation(() => hoisted.deleteProps?.onSuccess?.(undefined));

        const {result} = renderHook(() => useApiKeys(), {wrapper});

        act(() => result.current.handleDelete(5));

        expect(hoisted.deleteMutateMock).toHaveBeenCalledWith({apiKeyId: '5'});
        expect(useApiKeysStore.getState().showDeleteDialog).toBe(false);
        expect(useApiKeysStore.getState().currentApiKey).toBeUndefined();
    });
});
