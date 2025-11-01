/* eslint-disable sort-keys */

import {SigningKey, SigningKeyApi} from '@/ee/shared/middleware/embedded/security';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQuery} from '@tanstack/react-query';

export const SigningKeyKeys = {
    signingKeys: ['signingKeys'] as const,
};

export const useGeSigningKeysQuery = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    return useQuery<SigningKey[], Error>({
        queryKey: SigningKeyKeys.signingKeys,
        queryFn: () =>
            new SigningKeyApi().getSigningKeys({
                environmentId: currentEnvironmentId,
            }),
    });
};
