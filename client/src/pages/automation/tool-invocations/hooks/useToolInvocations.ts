import {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import {ToolInvocationLogsQuery, useToolInvocationLogsQuery} from '@/shared/middleware/graphql';
import {useState} from 'react';

type ToolInvocationLogItemType = ToolInvocationLogsQuery['toolInvocationLogs']['content'][number];

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

export const useToolInvocations = (): UseToolInvocationsI => {
    const [filterEndDate, setFilterEndDate] = useState<Date | undefined>(undefined);
    const [filterMcpServerId, setFilterMcpServerId] = useState<string | undefined>(undefined);
    const [filterOutcome, setFilterOutcome] = useState<string | undefined>(undefined);
    const [filterPageNumber, setFilterPageNumber] = useState<number>(0);
    const [filterStartDate, setFilterStartDate] = useState<Date | undefined>(undefined);
    const [filterSurface, setFilterSurface] = useState<string | undefined>(undefined);

    const {
        data,
        error: toolInvocationLogsError,
        isFetching: toolInvocationLogsIsFetching,
        isLoading: toolInvocationLogsIsLoading,
        refetch,
    } = useToolInvocationLogsQuery({
        fromDate: filterStartDate ? filterStartDate.getTime() : undefined,
        mcpServerId: filterMcpServerId ? Number(filterMcpServerId) : undefined,
        outcome: filterOutcome || undefined,
        page: filterPageNumber,
        surface: filterSurface || undefined,
        toDate: filterEndDate ? filterEndDate.getTime() : undefined,
    });

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
        refetchToolInvocations: refetch,
        toolInvocationLogPage: data?.toolInvocationLogs,
        toolInvocationLogs: data?.toolInvocationLogs.content ?? [],
        toolInvocationLogsError: (toolInvocationLogsError as Error | null) ?? null,
        toolInvocationLogsIsFetching,
        toolInvocationLogsIsLoading,
    };
};
