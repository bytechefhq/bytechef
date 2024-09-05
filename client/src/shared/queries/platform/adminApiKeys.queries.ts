/* eslint-disable sort-keys */
import {AdminApiKey, AdminApiKeyApi} from '@/shared/middleware/platform/user';
import {useQuery} from '@tanstack/react-query';

export const AdminApiKeyKeys = {
    adminApiKeys: ['adminApiKeys'] as const,
};

export const useGetAdminApiKeysQuery = () =>
    useQuery<AdminApiKey[], Error>({
        queryKey: AdminApiKeyKeys.adminApiKeys,
        queryFn: () => new AdminApiKeyApi().getAdminApiKeys(),
    });
