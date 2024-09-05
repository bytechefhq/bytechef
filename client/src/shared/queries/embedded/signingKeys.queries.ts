/* eslint-disable sort-keys */

import {SigningKey, SigningKeyApi} from '@/shared/middleware/embedded/user';
import {useQuery} from '@tanstack/react-query';

export const SigningKeyKeys = {
    signingKeys: ['signingKeys'] as const,
};

export const useGeSigningKeysQuery = () =>
    useQuery<SigningKey[], Error>({
        queryKey: SigningKeyKeys.signingKeys,
        queryFn: () => new SigningKeyApi().getSigningKeys(),
    });
