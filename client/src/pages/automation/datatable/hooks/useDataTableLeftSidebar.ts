import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useDataTablesQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo, useState} from 'react';

interface DataTableI {
    id: string;
    baseName: string;
}

interface UseDataTableLeftSidebarI {
    error: unknown;
    filteredTables: DataTableI[];
    handleSearchChange: (value: string) => void;
    isLoading: boolean;
    search: string;
}

export default function useDataTableLeftSidebar(): UseDataTableLeftSidebarI {
    const [search, setSearch] = useState('');

    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId) ?? 2;
    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId) ?? 1049;

    const {data, error, isLoading} = useDataTablesQuery({
        environmentId: String(environmentId),
        workspaceId: String(workspaceId),
    });

    const collator = useMemo(() => new Intl.Collator(undefined, {numeric: true, sensitivity: 'base'}), []);

    const filteredTables = useMemo(() => {
        const query = search.trim().toLowerCase();
        const tables = [...(data?.dataTables ?? [])].sort((tableA, tableB) =>
            collator.compare(tableA.baseName.trim(), tableB.baseName.trim())
        );

        if (!query) {
            return tables;
        }

        return tables.filter((table) => table.baseName.toLowerCase().includes(query));
    }, [data, search, collator]);

    const handleSearchChange = (value: string) => {
        setSearch(value);
    };

    return {
        error,
        filteredTables,
        handleSearchChange,
        isLoading,
        search,
    };
}
