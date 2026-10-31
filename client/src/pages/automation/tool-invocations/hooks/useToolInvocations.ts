import {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import {
    PlatformType,
    ToolInvocationLogsQuery,
    ToolInvocationLogsQueryVariables,
    useToolInvocationLogsQuery,
    useWorkspaceToolInvocationLogsQuery,
} from '@/shared/middleware/graphql';
import {endOfDay} from 'date-fns';
import {useState} from 'react';

type ToolInvocationLogItemType = ToolInvocationLogsQuery['toolInvocationLogs']['content'][number];

export interface ToolInvocationsWorkspaceScopeI {
    environmentId: number | undefined;
    workspaceId: number | undefined;
}

interface ToolInvocationsFiltersI {
    endDate: Date | undefined;
    mcpServerId: string | undefined;
    outcome: string | undefined;
    pageNumber: number;
    startDate: Date | undefined;
    surface: string | undefined;
}

interface UseToolInvocationsI {
    filterEndDate: Date | undefined;
    filterMcpServerId: string | undefined;
    filterOutcome: string | undefined;
    filterPageNumber: number;
    filterStartDate: Date | undefined;
    filterSurface: string | undefined;
    handleEndDateChange: (date: Date | undefined) => void;
    handleMcpServerChange: (item?: ComboBoxItemType) => void;
    handleOutcomeChange: (item?: ComboBoxItemType) => void;
    handlePaginationClick: (pageNumber: number) => void;
    handleStartDateChange: (date: Date | undefined) => void;
    handleSurfaceChange: (item?: ComboBoxItemType) => void;
    refetchToolInvocations: () => void;
    toolInvocationLogPage: ToolInvocationLogsQuery['toolInvocationLogs'] | undefined;
    toolInvocationLogs: Array<ToolInvocationLogItemType>;
    toolInvocationLogsError: Error | null;
    toolInvocationLogsIsFetching: boolean;
    toolInvocationLogsIsLoading: boolean;
}

const toFilterVariables = (filters: ToolInvocationsFiltersI): ToolInvocationLogsQueryVariables => ({
    fromDate: filters.startDate ? filters.startDate.getTime() : undefined,
    mcpServerId: filters.mcpServerId ? Number(filters.mcpServerId) : undefined,
    outcome: filters.outcome || undefined,
    page: filters.pageNumber,
    surface: filters.surface || undefined,
    toDate: filters.endDate ? endOfDay(filters.endDate).getTime() : undefined,
    type: PlatformType.Automation,
});

export const useToolInvocations = (workspaceScope?: ToolInvocationsWorkspaceScopeI): UseToolInvocationsI => {
    const [filterEndDate, setFilterEndDate] = useState<Date | undefined>(undefined);
    const [filterMcpServerId, setFilterMcpServerId] = useState<string | undefined>(undefined);
    const [filterOutcome, setFilterOutcome] = useState<string | undefined>(undefined);
    const [filterPageNumber, setFilterPageNumber] = useState<number>(0);
    const [filterStartDate, setFilterStartDate] = useState<Date | undefined>(undefined);
    const [filterSurface, setFilterSurface] = useState<string | undefined>(undefined);

    const filterVariables = toFilterVariables({
        endDate: filterEndDate,
        mcpServerId: filterMcpServerId,
        outcome: filterOutcome,
        pageNumber: filterPageNumber,
        startDate: filterStartDate,
        surface: filterSurface,
    });
    const workspaceScoped = workspaceScope !== undefined;

    const tenantQuery = useToolInvocationLogsQuery(filterVariables, {enabled: !workspaceScoped});

    const workspaceQuery = useWorkspaceToolInvocationLogsQuery(
        {
            ...filterVariables,
            environmentId: String(workspaceScope?.environmentId),
            workspaceId: String(workspaceScope?.workspaceId),
        },
        {enabled: workspaceScope?.environmentId != null && workspaceScope?.workspaceId != null}
    );

    const activeQuery = workspaceScoped ? workspaceQuery : tenantQuery;
    const toolInvocationLogPage = workspaceScoped
        ? workspaceQuery.data?.workspaceToolInvocationLogs
        : tenantQuery.data?.toolInvocationLogs;

    const handleEndDateChange = (date: Date | undefined) => {
        setFilterEndDate(date);
        setFilterPageNumber(0);
    };

    const handleMcpServerChange = (item?: ComboBoxItemType) => {
        setFilterMcpServerId(item?.value ? String(item.value) : undefined);
        setFilterPageNumber(0);
    };

    const handleOutcomeChange = (item?: ComboBoxItemType) => {
        setFilterOutcome(item?.value ? String(item.value) : undefined);
        setFilterPageNumber(0);
    };

    const handlePaginationClick = (pageNumber: number) => setFilterPageNumber(pageNumber);

    const handleStartDateChange = (date: Date | undefined) => {
        setFilterStartDate(date);
        setFilterPageNumber(0);
    };

    const handleSurfaceChange = (item?: ComboBoxItemType) => {
        setFilterSurface(item?.value ? String(item.value) : undefined);
        setFilterPageNumber(0);
    };

    return {
        filterEndDate,
        filterMcpServerId,
        filterOutcome,
        filterPageNumber,
        filterStartDate,
        filterSurface,
        handleEndDateChange,
        handleMcpServerChange,
        handleOutcomeChange,
        handlePaginationClick,
        handleStartDateChange,
        handleSurfaceChange,
        refetchToolInvocations: activeQuery.refetch,
        toolInvocationLogPage,
        toolInvocationLogs: toolInvocationLogPage?.content ?? [],
        toolInvocationLogsError: (activeQuery.error as Error | null) ?? null,
        toolInvocationLogsIsFetching: activeQuery.isFetching,
        toolInvocationLogsIsLoading: activeQuery.isLoading,
    };
};
