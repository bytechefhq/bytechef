import Button from '@/components/Button/Button';
import ComboBox, {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import DatePicker from '@/components/DatePicker/DatePicker';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import TablePagination from '@/components/TablePagination';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ToolInvocationsTable from '@/pages/automation/tool-invocations/components/ToolInvocationsTable';
import {
    ToolInvocationsWorkspaceScopeI,
    useToolInvocations,
} from '@/pages/automation/tool-invocations/hooks/useToolInvocations';
import {useInitialLoadComplete} from '@/shared/hooks/useInitialLoadComplete';
import Footer from '@/shared/layout/Footer';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {PlatformType} from '@/shared/middleware/graphql';
import {HammerIcon, RefreshCwIcon} from 'lucide-react';
import {ReactNode, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

const ANY_FILTER_OPTION = {label: 'Any', value: ''};

const outcomeOptions = [
    ANY_FILTER_OPTION,
    {label: 'Success', value: 'SUCCESS'},
    {label: 'Error', value: 'ERROR'},
    {label: 'Connection required', value: 'CONNECTION_REQUIRED'},
    {label: 'Timeout', value: 'TIMEOUT'},
    {label: 'Approval required', value: 'APPROVAL_REQUIRED'},
    {label: 'Approval denied', value: 'APPROVAL_DENIED'},
    {label: 'Suspended', value: 'SUSPENDED'},
];

const surfaceOptionsByType: Record<PlatformType, Array<ComboBoxItemType>> = {
    [PlatformType.Automation]: [
        {label: 'Automation MCP', value: 'MCP_AUTOMATION'},
        {label: 'App action', value: 'AUTOMATION_APP_ACTION'},
        {label: 'App API', value: 'AUTOMATION_APP_API'},
        {label: 'App tool', value: 'AUTOMATION_APP_TOOL'},
        {label: 'App proxy', value: 'AUTOMATION_APP_PROXY'},
        {label: 'App workflow', value: 'AUTOMATION_APP_WORKFLOW'},
    ],
    [PlatformType.Embedded]: [
        {label: 'Embedded MCP', value: 'MCP_EMBEDDED'},
        {label: 'Embedded API (Action)', value: 'EMBEDDED_API_ACTION'},
        {label: 'Embedded API (Tool)', value: 'EMBEDDED_API_TOOL'},
    ],
};

interface ToolInvocationsProps {
    appOptions?: Array<ComboBoxItemType>;
    environmentId?: number;
    /* Shown instead of the filters and the table when nothing has been recorded yet and no filter is set. */
    firstRunEmptyState: ReactNode;
    mcpServerOptions?: Array<ComboBoxItemType>;
    type: PlatformType;
    workspaceScope?: ToolInvocationsWorkspaceScopeI;
}

export const ToolInvocations = ({
    appOptions,
    environmentId,
    firstRunEmptyState,
    mcpServerOptions,
    type,
    workspaceScope,
}: ToolInvocationsProps) => {
    const {
        filterAppId,
        filterEndDate,
        filterMcpServerId,
        filterOutcome,
        filterPageNumber,
        filterStartDate,
        filterSurface,
        handleAppChange,
        handleEndDateChange,
        handleMcpServerChange,
        handleOutcomeChange,
        handlePaginationClick,
        handleStartDateChange,
        handleSurfaceChange,
        refetchToolInvocations,
        toolInvocationLogPage,
        toolInvocationLogs,
        toolInvocationLogsError,
        toolInvocationLogsIsFetching,
        toolInvocationLogsIsLoading,
    } = useToolInvocations({environmentId, type, workspaceScope});

    const initialLoadComplete = useInitialLoadComplete(toolInvocationLogsIsLoading);

    const mcpServerNames = useMemo(
        () =>
            Object.fromEntries((mcpServerOptions ?? []).map((option) => [String(option.value), String(option.label)])),
        [mcpServerOptions]
    );

    const showSurface = surfaceOptionsByType[type].length > 1;

    const visibleSurfaceOptions = useMemo(() => [ANY_FILTER_OPTION, ...surfaceOptionsByType[type]], [type]);

    const hasActiveFilters =
        !!filterAppId ||
        !!filterEndDate ||
        !!filterMcpServerId ||
        !!filterOutcome ||
        !!filterStartDate ||
        !!filterSurface;

    const isFirstRun =
        !toolInvocationLogsIsLoading &&
        !toolInvocationLogsError &&
        !hasActiveFilters &&
        filterPageNumber === 0 &&
        toolInvocationLogs.length === 0;

    return (
        <LayoutContainer
            footer={
                toolInvocationLogPage?.content &&
                toolInvocationLogPage.content.length > 0 && (
                    <Footer centerTitle className="border-t border-stroke-neutral-primary 3xl:w-full" position="main">
                        <TablePagination
                            onClick={handlePaginationClick}
                            pageNumber={filterPageNumber}
                            pageSize={toolInvocationLogPage.size}
                            totalElements={toolInvocationLogPage.totalElements}
                            totalPages={toolInvocationLogPage.totalPages}
                        />
                    </Footer>
                )
            }
            header={
                <Header
                    className="3xl:w-full"
                    position="main"
                    right={
                        <div className="flex items-center gap-1">
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        aria-label="Refresh tool invocations"
                                        disabled={toolInvocationLogsIsFetching}
                                        icon={
                                            <RefreshCwIcon
                                                className={twMerge(toolInvocationLogsIsFetching && 'animate-spin')}
                                            />
                                        }
                                        onClick={() => refetchToolInvocations()}
                                        size="icon"
                                        variant="ghost"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>Refresh</TooltipContent>
                            </Tooltip>
                        </div>
                    }
                    title="Tool Invocations"
                />
            }
            leftSidebarBody={
                <div className="space-y-4 px-2">
                    {showSurface && (
                        <div className="flex flex-col space-y-2">
                            <Label>Surface</Label>

                            <ComboBox
                                items={visibleSurfaceOptions}
                                onChange={handleSurfaceChange}
                                value={filterSurface}
                            />
                        </div>
                    )}

                    <div className="flex flex-col space-y-2">
                        <Label>Outcome</Label>

                        <ComboBox items={outcomeOptions} onChange={handleOutcomeChange} value={filterOutcome} />
                    </div>

                    {mcpServerOptions && mcpServerOptions.length > 0 && (
                        <div className="flex flex-col space-y-2">
                            <Label>MCP Server</Label>

                            <ComboBox
                                items={[ANY_FILTER_OPTION, ...mcpServerOptions]}
                                onChange={handleMcpServerChange}
                                value={filterMcpServerId}
                            />
                        </div>
                    )}

                    {appOptions && appOptions.length > 0 && (
                        <div className="flex flex-col space-y-2">
                            <Label>App</Label>

                            <ComboBox
                                items={[ANY_FILTER_OPTION, ...appOptions]}
                                onChange={handleAppChange}
                                value={filterAppId}
                            />
                        </div>
                    )}

                    <div className="flex flex-col space-y-2">
                        <Label>Start date</Label>

                        <DatePicker onChange={handleStartDateChange} value={filterStartDate} />
                    </div>

                    <div className="flex flex-col space-y-2">
                        <Label>End date</Label>

                        <DatePicker onChange={handleEndDateChange} value={filterEndDate} />
                    </div>
                </div>
            }
            leftSidebarHeader={<Header position="sidebar" title="Tool Invocations" />}
            leftSidebarHidden={!initialLoadComplete || isFirstRun}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[toolInvocationLogsError]} loading={toolInvocationLogsIsLoading}>
                {toolInvocationLogs.length > 0 ? (
                    <div className="w-full self-start p-4 pt-0">
                        <ToolInvocationsTable
                            mcpServerNames={mcpServerNames}
                            showSurface={showSurface}
                            toolInvocationLogs={toolInvocationLogs}
                        />
                    </div>
                ) : isFirstRun ? (
                    firstRunEmptyState
                ) : (
                    <EmptyList
                        icon={<HammerIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="No tool invocations match the current filter."
                        title="No Matching Tool Invocations"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};
