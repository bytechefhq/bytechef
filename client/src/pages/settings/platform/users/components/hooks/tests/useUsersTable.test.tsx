import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useUsersTable from '../useUsersTable';

vi.mock('@/shared/middleware/graphql', () => ({
    useUsersQuery: vi.fn(() => ({
        data: {
            users: {
                content: [
                    {
                        activated: true,
                        authorities: ['ROLE_ADMIN'],
                        email: 'admin@test.com',
                        firstName: 'Admin',
                        id: '1',
                        lastName: 'User',
                        login: 'admin',
                    },
                    {
                        activated: false,
                        authorities: ['ROLE_USER'],
                        email: 'user@test.com',
                        firstName: 'Regular',
                        id: '2',
                        lastName: 'User',
                        login: 'user',
                    },
                ],
                number: 0,
                size: 20,
                totalElements: 2,
                totalPages: 1,
            },
        },
        error: null,
        isLoading: false,
    })),
}));

describe('useUsersTable', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('returns users from query', () => {
        const {result} = renderHook(() => useUsersTable());

        expect(result.current.users).toHaveLength(2);
        expect(result.current.users[0]?.login).toBe('admin');
    });

    it('returns loading state as false', () => {
        const {result} = renderHook(() => useUsersTable());

        expect(result.current.isLoading).toBe(false);
    });

    it('returns error as null', () => {
        const {result} = renderHook(() => useUsersTable());

        expect(result.current.error).toBeNull();
    });
});
