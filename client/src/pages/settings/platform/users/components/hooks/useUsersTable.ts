import {useUsersQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

interface UseUsersTableProps {
    pageNumber?: number;
    pageSize?: number;
}

export default function useUsersTable({pageNumber = 0, pageSize = 20}: UseUsersTableProps = {}) {
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
