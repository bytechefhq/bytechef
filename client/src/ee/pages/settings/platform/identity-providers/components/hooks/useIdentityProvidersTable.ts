import {IdentityProviderType, useIdentityProvidersQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

interface UseIdentityProvidersTableI {
    error: unknown;
    identityProviders: IdentityProviderType[];
    isLoading: boolean;
}

export default function useIdentityProvidersTable(): UseIdentityProvidersTableI {
    const {data, error, isLoading} = useIdentityProvidersQuery({});

    const identityProviders = useMemo(
        () => (data?.identityProviders ?? []).filter(Boolean) as IdentityProviderType[],
        [data]
    );

    return {
        error,
        identityProviders,
        isLoading,
    };
}
