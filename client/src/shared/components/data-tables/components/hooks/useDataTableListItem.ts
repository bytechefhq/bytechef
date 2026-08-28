import {DataTable} from '@/shared/middleware/graphql';
import {MouseEvent, useCallback} from 'react';
import {useNavigate} from 'react-router-dom';

interface UseDataTableListItemProps {
    table: DataTable;
}

interface UseDataTableListItemI {
    columnCountLabel: string;
    handleDataTableListItemTagListClick: (event: MouseEvent) => void;
    handleRowClick: () => void;
}

export default function useDataTableListItem({table}: UseDataTableListItemProps): UseDataTableListItemI {
    const navigate = useNavigate();

    const columnCount = table.columns?.length ?? 0;
    const columnCountLabel = columnCount === 1 ? `${columnCount} column` : `${columnCount} columns`;

    const handleDataTableListItemTagListClick = useCallback((event: MouseEvent) => {
        event.preventDefault();
        event.stopPropagation();
    }, []);

    const handleRowClick = useCallback(() => {
        navigate(`/automation/datatables/${table.id}`);
    }, [navigate, table.id]);

    return {
        columnCountLabel,
        handleDataTableListItemTagListClick,
        handleRowClick,
    };
}
