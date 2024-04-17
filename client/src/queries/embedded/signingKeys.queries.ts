/* eslint-disable sort-keys */

import {SigningKeyApi, SigningKeyModel} from '@/middleware/embedded/user';
import {useQuery} from '@tanstack/react-query';

export const SigningKeyKeys = {
    signingKeys: ['signingKeys'] as const,
};

export const useGeSigningKeysQuery = () =>
    useQuery<SigningKeyModel[], Error>({
        queryKey: SigningKeyKeys.signingKeys,
        queryFn: () => new SigningKeyApi().getSigningKeys(),
    });
