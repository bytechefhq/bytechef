import {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import {sourceHasA2aServer, sourceHasMcpServer} from '@/pages/automation/external-invocations/utils/sourceServers';
import {
    ExternalInvocationLogsQuery,
    ExternalInvocationLogsQueryVariables,
    PlatformType,
    useExternalInvocationLogsQuery,
    useWorkspaceExternalInvocationLogsQuery,
} from '@/shared/middleware/graphql';
import {endOfDay} from 'date-fns';
import {useState} from 'react';

type ExternalInvocationLogItemType = ExternalInvocationLogsQuery['externalInvocationLogs']['content'][number];

export interface ExternalInvocationsWorkspaceScopeI {
    environmentId: number | undefined;
    workspaceId: number | undefined;
}

export interface UseExternalInvocationsOptionsI {
    environmentId?: number;
    type: PlatformType;
    workspaceScope?: ExternalInvocationsWorkspaceScopeI;
}

interface ExternalInvocationsFiltersI {
    a2aServerId: string | undefined;
    endDate: Date | undefined;
    mcpServerId: string | undefined;
    outcome: string | undefined;
    pageNumber: number;
    startDate: Date | undefined;
    source: string | undefined;
}

interface UseExternalInvocationsI {
    filterA2aServerId: string | undefined;
    filterEndDate: Date | undefined;
    filterMcpServerId: string | undefined;
    filterOutcome: string | undefined;
    filterPageNumber: number;
    filterSource: string | undefined;
    filterStartDate: Date | undefined;
    handleA2aServerChange: (item?: ComboBoxItemType) => void;
    handleEndDateChange: (date: Date | undefined) => void;
    handleMcpServerChange: (item?: ComboBoxItemType) => void;
    handleOutcomeChange: (item?: ComboBoxItemType) => void;
    handlePaginationClick: (pageNumber: number) => void;
    handleSourceChange: (item?: ComboBoxItemType) => void;
    handleStartDateChange: (date: Date | undefined) => void;
    refetchExternalInvocations: () => void;
    externalInvocationLogPage: ExternalInvocationLogsQuery['externalInvocationLogs'] | undefined;
    externalInvocationLogs: Array<ExternalInvocationLogItemType>;
    externalInvocationLogsError: Error | null;
    externalInvocationLogsIsFetching: boolean;
    externalInvocationLogsIsLoading: boolean;
}

const toFilterVariables = (
    filters: ExternalInvocationsFiltersI,
    type: PlatformType,
    environmentId?: number
): ExternalInvocationLogsQueryVariables => ({
    a2aServerId: filters.a2aServerId ? Number(filters.a2aServerId) : undefined,
    environmentId: environmentId != null ? String(environmentId) : undefined,
    fromDate: filters.startDate ? filters.startDate.getTime() : undefined,
    mcpServerId: filters.mcpServerId ? Number(filters.mcpServerId) : undefined,
    outcome: filters.outcome || undefined,
    page: filters.pageNumber,
    source: filters.source || undefined,
    toDate: filters.endDate ? endOfDay(filters.endDate).getTime() : undefined,
    type,
});

export const useExternalInvocations = ({
    environmentId,
    type,
    workspaceScope,
}: UseExternalInvocationsOptionsI): UseExternalInvocationsI => {
    const [filterA2aServerId, setFilterA2aServerId] = useState<string | undefined>(undefined);
    const [filterEndDate, setFilterEndDate] = useState<Date | undefined>(undefined);
    const [filterMcpServerId, setFilterMcpServerId] = useState<string | undefined>(undefined);
    const [filterOutcome, setFilterOutcome] = useState<string | undefined>(undefined);
    const [filterPageNumber, setFilterPageNumber] = useState<number>(0);
    const [filterStartDate, setFilterStartDate] = useState<Date | undefined>(undefined);
    const [filterSource, setFilterSource] = useState<string | undefined>(undefined);

    const filterVariables = toFilterVariables(
        {
            a2aServerId: filterA2aServerId,
            endDate: filterEndDate,
            mcpServerId: filterMcpServerId,
            outcome: filterOutcome,
            pageNumber: filterPageNumber,
            source: filterSource,
            startDate: filterStartDate,
        },
        type,
        environmentId
    );
    const workspaceScoped = workspaceScope !== undefined;

    const tenantQuery = useExternalInvocationLogsQuery(filterVariables, {enabled: !workspaceScoped});

    const workspaceQuery = useWorkspaceExternalInvocationLogsQuery(
        {
            ...filterVariables,
            environmentId: String(workspaceScope?.environmentId),
            workspaceId: String(workspaceScope?.workspaceId),
        },
        {enabled: workspaceScope?.environmentId != null && workspaceScope?.workspaceId != null}
    );

    const activeQuery = workspaceScoped ? workspaceQuery : tenantQuery;
    const externalInvocationLogPage = workspaceScoped
        ? workspaceQuery.data?.workspaceExternalInvocationLogs
        : tenantQuery.data?.externalInvocationLogs;

    const handleA2aServerChange = (item?: ComboBoxItemType) => {
        setFilterA2aServerId(item?.value ? String(item.value) : undefined);
        setFilterPageNumber(0);
    };

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

    const handleSourceChange = (item?: ComboBoxItemType) => {
        const source = item?.value ? String(item.value) : undefined;

        setFilterSource(source);

        if (!sourceHasA2aServer(source)) {
            setFilterA2aServerId(undefined);
        }

        if (!sourceHasMcpServer(source)) {
            setFilterMcpServerId(undefined);
        }

        setFilterPageNumber(0);
    };

    return {
        externalInvocationLogPage,
        externalInvocationLogs: externalInvocationLogPage?.content ?? [],
        externalInvocationLogsError: (activeQuery.error as Error | null) ?? null,
        externalInvocationLogsIsFetching: activeQuery.isFetching,
        externalInvocationLogsIsLoading: activeQuery.isLoading,
        filterA2aServerId,
        filterEndDate,
        filterMcpServerId,
        filterOutcome,
        filterPageNumber,
        filterSource,
        filterStartDate,
        handleA2aServerChange,
        handleEndDateChange,
        handleMcpServerChange,
        handleOutcomeChange,
        handlePaginationClick,
        handleSourceChange,
        handleStartDateChange,
        refetchExternalInvocations: activeQuery.refetch,
    };
};
