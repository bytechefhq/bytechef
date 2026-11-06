import Button from '@/components/Button/Button';
import ComboBox, {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import DatePicker from '@/components/DatePicker/DatePicker';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import TablePagination from '@/components/TablePagination';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ExternalInvocationsTable from '@/pages/automation/external-invocations/components/ExternalInvocationsTable';
import {
    ExternalInvocationsWorkspaceScopeI,
    useExternalInvocations,
} from '@/pages/automation/external-invocations/hooks/useExternalInvocations';
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
        {label: 'A2A', value: 'A2A'},
    ],
    [PlatformType.Embedded]: [
        {label: 'Embedded MCP', value: 'MCP_EMBEDDED'},
        {label: 'Embedded API (Action)', value: 'EMBEDDED_API_ACTION'},
        {label: 'Embedded API (Tool)', value: 'EMBEDDED_API_TOOL'},
    ],
};

interface ExternalInvocationsProps {
    a2aServerOptions?: Array<ComboBoxItemType>;
    environmentId?: number;
    /* Shown instead of the filters and the table when nothing has been recorded yet and no filter is set. */
    firstRunEmptyState: ReactNode;
    mcpServerOptions?: Array<ComboBoxItemType>;
    type: PlatformType;
    workspaceScope?: ExternalInvocationsWorkspaceScopeI;
}

export const ExternalInvocations = ({
    a2aServerOptions,
    environmentId,
    firstRunEmptyState,
    mcpServerOptions,
    type,
    workspaceScope,
}: ExternalInvocationsProps) => {
    const {
        externalInvocationLogPage,
        externalInvocationLogs,
        externalInvocationLogsError,
        externalInvocationLogsIsFetching,
        externalInvocationLogsIsLoading,
        filterA2aServerId,
        filterEndDate,
        filterMcpServerId,
        filterOutcome,
        filterPageNumber,
        filterStartDate,
        filterSurface,
        handleA2aServerChange,
        handleEndDateChange,
        handleMcpServerChange,
        handleOutcomeChange,
        handlePaginationClick,
        handleStartDateChange,
        handleSurfaceChange,
        refetchExternalInvocations,
    } = useExternalInvocations({environmentId, type, workspaceScope});

    const initialLoadComplete = useInitialLoadComplete(externalInvocationLogsIsLoading);

    const a2aServerNames = useMemo(
        () =>
            Object.fromEntries((a2aServerOptions ?? []).map((option) => [String(option.value), String(option.label)])),
        [a2aServerOptions]
    );

    const mcpServerNames = useMemo(
        () =>
            Object.fromEntries((mcpServerOptions ?? []).map((option) => [String(option.value), String(option.label)])),
        [mcpServerOptions]
    );

    const showSurface = surfaceOptionsByType[type].length > 1;

    const visibleSurfaceOptions = useMemo(() => [ANY_FILTER_OPTION, ...surfaceOptionsByType[type]], [type]);

    const hasActiveFilters =
        !!filterA2aServerId ||
        !!filterEndDate ||
        !!filterMcpServerId ||
        !!filterOutcome ||
        !!filterStartDate ||
        !!filterSurface;

    const isFirstRun =
        !externalInvocationLogsIsLoading &&
        !externalInvocationLogsError &&
        !hasActiveFilters &&
        filterPageNumber === 0 &&
        externalInvocationLogs.length === 0;

    return (
        <LayoutContainer
            footer={
                externalInvocationLogPage?.content &&
                externalInvocationLogPage.content.length > 0 && (
                    <Footer centerTitle className="border-t border-stroke-neutral-primary 3xl:w-full" position="main">
                        <TablePagination
                            onClick={handlePaginationClick}
                            pageNumber={filterPageNumber}
                            pageSize={externalInvocationLogPage.size}
                            totalElements={externalInvocationLogPage.totalElements}
                            totalPages={externalInvocationLogPage.totalPages}
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
                                        aria-label="Refresh external invocations"
                                        disabled={externalInvocationLogsIsFetching}
                                        icon={
                                            <RefreshCwIcon
                                                className={twMerge(externalInvocationLogsIsFetching && 'animate-spin')}
                                            />
                                        }
                                        onClick={() => refetchExternalInvocations()}
                                        size="icon"
                                        variant="ghost"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>Refresh</TooltipContent>
                            </Tooltip>
                        </div>
                    }
                    title="External Invocations"
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
                                aria-label="MCP Server"
                                items={[ANY_FILTER_OPTION, ...mcpServerOptions]}
                                onChange={handleMcpServerChange}
                                value={filterMcpServerId}
                            />
                        </div>
                    )}

                    {a2aServerOptions && a2aServerOptions.length > 0 && (
                        <div className="flex flex-col space-y-2">
                            <Label>A2A Server</Label>

                            <ComboBox
                                aria-label="A2A Server"
                                items={[ANY_FILTER_OPTION, ...a2aServerOptions]}
                                onChange={handleA2aServerChange}
                                value={filterA2aServerId}
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
            leftSidebarHeader={<Header position="sidebar" title="External Invocations" />}
            leftSidebarHidden={!initialLoadComplete || isFirstRun}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[externalInvocationLogsError]} loading={externalInvocationLogsIsLoading}>
                {externalInvocationLogs.length > 0 ? (
                    <div className="w-full self-start p-4 pt-0">
                        <ExternalInvocationsTable
                            a2aServerNames={a2aServerNames}
                            externalInvocationLogs={externalInvocationLogs}
                            mcpServerNames={mcpServerNames}
                            showSurface={showSurface}
                        />
                    </div>
                ) : isFirstRun ? (
                    firstRunEmptyState
                ) : (
                    <EmptyList
                        icon={<HammerIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="No external invocations match the current filter."
                        title="No Matching External Invocations"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};
