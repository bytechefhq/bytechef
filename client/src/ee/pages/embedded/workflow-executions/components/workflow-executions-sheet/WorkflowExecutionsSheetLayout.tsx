import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import TablePagination from '@/components/TablePagination';
import {SheetCloseButton} from '@/components/ui/sheet';
import {Tabs, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ActivityIcon, RefreshCwIcon, WorkflowIcon} from 'lucide-react';
import {ReactNode, useState} from 'react';
import {twMerge} from 'tailwind-merge';

type WorkflowExecutionsSheetTabType = 'executions' | 'workflow';

interface WorkflowExecutionPageI {
    size?: number;
    totalElements?: number;
    totalPages?: number;
}

interface WorkflowExecutionsSheetLayoutProps {
    emptyMessage: string;
    executionSheet: ReactNode;
    executionsTable: ReactNode;
    hasExecutions: boolean;
    onPageNumberChange: (pageNumber: number) => void;
    onRefresh: () => void;
    pageNumber: number;
    parentLabel?: string;
    version?: number;
    workflowExecutionPage?: WorkflowExecutionPageI;
    workflowExecutionsError: Error | null;
    workflowExecutionsIsFetching: boolean;
    workflowExecutionsIsLoading: boolean;
    workflowLabel?: string | null;
    workflowPanel: ReactNode;
}

const WorkflowExecutionsSheetLayout = ({
    emptyMessage,
    executionSheet,
    executionsTable,
    hasExecutions,
    onPageNumberChange,
    onRefresh,
    pageNumber,
    parentLabel,
    version,
    workflowExecutionPage,
    workflowExecutionsError,
    workflowExecutionsIsFetching,
    workflowExecutionsIsLoading,
    workflowLabel,
    workflowPanel,
}: WorkflowExecutionsSheetLayoutProps) => {
    const [activeTab, setActiveTab] = useState<WorkflowExecutionsSheetTabType>('executions');

    const executionsTabActive = activeTab === 'executions';

    return (
        <>
            <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                <div className="flex items-center gap-x-2">
                    <WorkflowIcon />

                    <span className="flex gap-x-1 text-base text-content-neutral-secondary">
                        {parentLabel && <span>{`${parentLabel} /`}</span>}

                        <strong className="text-content-neutral-primary">{workflowLabel}</strong>

                        {version != null && <span>{`/ V${version}`}</span>}
                    </span>

                    <Tabs
                        className="ml-4"
                        onValueChange={(value) => setActiveTab(value as WorkflowExecutionsSheetTabType)}
                        value={activeTab}
                    >
                        <TabsList>
                            <TabsTrigger value="executions">Executions</TabsTrigger>

                            <TabsTrigger value="workflow">Workflow</TabsTrigger>
                        </TabsList>
                    </Tabs>
                </div>

                <div className="flex items-center gap-1">
                    {executionsTabActive && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    aria-label="Refresh workflow executions"
                                    disabled={workflowExecutionsIsFetching}
                                    icon={
                                        <RefreshCwIcon
                                            className={twMerge(workflowExecutionsIsFetching && 'animate-spin')}
                                        />
                                    }
                                    onClick={onRefresh}
                                    size="icon"
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Refresh</TooltipContent>
                        </Tooltip>
                    )}

                    <SheetCloseButton />
                </div>
            </header>

            <div className={twMerge('flex min-h-0 flex-1 flex-col', !executionsTabActive && 'hidden')}>
                <div className="m-3 flex min-h-0 flex-1 flex-col overflow-hidden rounded-lg bg-surface-neutral-primary">
                    <div className="flex min-h-0 flex-1 overflow-y-auto">
                        <PageLoader errors={[workflowExecutionsError]} loading={workflowExecutionsIsLoading}>
                            {hasExecutions ? (
                                executionsTable
                            ) : (
                                <EmptyList
                                    icon={<ActivityIcon className="size-24 text-stroke-neutral-tertiary" />}
                                    message={emptyMessage}
                                    title="No Executions"
                                />
                            )}
                        </PageLoader>
                    </div>

                    {workflowExecutionPage && hasExecutions && (
                        <div className="shrink-0 border-t border-stroke-neutral-primary">
                            <TablePagination
                                onClick={onPageNumberChange}
                                pageNumber={pageNumber}
                                pageSize={workflowExecutionPage.size!}
                                totalElements={workflowExecutionPage.totalElements!}
                                totalPages={workflowExecutionPage.totalPages!}
                            />
                        </div>
                    )}
                </div>
            </div>

            {!executionsTabActive && workflowPanel}

            {executionSheet}
        </>
    );
};

export default WorkflowExecutionsSheetLayout;
