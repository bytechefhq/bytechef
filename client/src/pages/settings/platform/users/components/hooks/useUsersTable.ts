import {useUsersQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

interface UseUsersTableProps {
    pageNumber?: number;
    pageSize?: number;
}

interface UseUsersTableI {
    error: unknown;
    isLoading: boolean;
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
    users: Array<{
        activated?: boolean | null;
        authorities?: (string | null)[] | null;
        email?: string | null;
        firstName?: string | null;
        id?: string | null;
        lastName?: string | null;
        login?: string | null;
    } | null>;
}

export default function useUsersTable({pageNumber = 0, pageSize = 20}: UseUsersTableProps = {}): UseUsersTableI {
    const {data, error, isLoading} = useUsersQuery({pageNumber, pageSize});

    const usersPage = data?.users;
    const users = useMemo(() => usersPage?.content ?? [], [usersPage]);

    return {
        error,
        isLoading,
        pageNumber: usersPage?.number ?? 0,
        pageSize: usersPage?.size ?? pageSize,
        totalElements: usersPage?.totalElements ?? 0,
        totalPages: usersPage?.totalPages ?? 0,
        users,
    };
}
