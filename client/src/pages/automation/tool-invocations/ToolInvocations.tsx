import Button from '@/components/Button/Button';
import ComboBox, {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import DatePicker from '@/components/DatePicker/DatePicker';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import TablePagination from '@/components/TablePagination';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ToolInvocationsTable from '@/pages/automation/tool-invocations/components/ToolInvocationsTable';
import {useToolInvocations} from '@/pages/automation/tool-invocations/hooks/useToolInvocations';
import ExecutionsTabs from '@/shared/components/ExecutionsTabs';
import Footer from '@/shared/layout/Footer';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {ActivityIcon, RefreshCwIcon} from 'lucide-react';
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
];

const surfaceOptions = [
    ANY_FILTER_OPTION,
    {label: 'Automation MCP', value: 'MCP_AUTOMATION'},
    {label: 'Embedded MCP', value: 'MCP_EMBEDDED'},
    {label: 'Management MCP', value: 'MCP_MANAGEMENT'},
    {label: 'Embedded API (Action)', value: 'EMBEDDED_API_ACTION'},
    {label: 'Embedded API (Tool)', value: 'EMBEDDED_API_TOOL'},
    {label: 'AI Agent', value: 'AI_AGENT'},
];

interface ToolInvocationsProps {
    basePath?: string;
    mcpServerOptions?: Array<ComboBoxItemType>;
}

export const ToolInvocations = ({basePath = '/automation/executions', mcpServerOptions}: ToolInvocationsProps) => {
    const {
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
        refetchToolInvocations,
        toolInvocationLogPage,
        toolInvocationLogs,
        toolInvocationLogsError,
        toolInvocationLogsIsFetching,
        toolInvocationLogsIsLoading,
    } = useToolInvocations();

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
                    centerTitle
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
                    title={<ExecutionsTabs basePath={basePath} />}
                />
            }
            leftSidebarBody={
                <div className="space-y-4 px-4">
                    <div className="flex flex-col space-y-2">
                        <Label>Surface</Label>

                        <ComboBox items={surfaceOptions} onChange={handleSurfaceChange} value={filterSurface} />
                    </div>

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
            leftSidebarWidth="64"
        >
            <PageLoader errors={[toolInvocationLogsError]} loading={toolInvocationLogsIsLoading}>
                {toolInvocationLogs.length > 0 ? (
                    <ToolInvocationsTable toolInvocationLogs={toolInvocationLogs} />
                ) : (
                    <EmptyList
                        icon={<ActivityIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="No tool invocations have been recorded yet."
                        title="No Tool Invocations"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};
