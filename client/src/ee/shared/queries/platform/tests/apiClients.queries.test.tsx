import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

const {getApiClientsMock} = vi.hoisted(() => ({
    getApiClientsMock: vi.fn(),
}));

vi.mock('@/ee/shared/middleware/automation/api-platform', () => ({
    ApiClientApi: vi.fn(function ApiClientApiMock() {
        return {getApiClients: getApiClientsMock};
    }),
}));

import {ApiClientKeys, useGetApiClientsQuery} from '../apiClients.queries';

describe('apiClients.queries', () => {
    it('loads the api clients under the apiClients key', async () => {
        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});
        const apiClients = [{id: 1, name: 'Billing'}];

        getApiClientsMock.mockResolvedValue(apiClients);

        const {result} = renderHook(() => useGetApiClientsQuery(), {
            wrapper: ({children}: {children: ReactNode}) => (
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            ),
        });

        await waitFor(() => expect(result.current.data).toEqual(apiClients));
        expect(queryClient.getQueryData(ApiClientKeys.apiClients)).toEqual(apiClients);
    });
});
