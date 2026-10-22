import LoadingIcon from '@/components/LoadingIcon';
import TaskExecutionChildrenList from '@/shared/components/workflow-executions/TaskExecutionChildrenList';
import {Job, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {useCallback, useEffect, useMemo} from 'react';

export interface LazyTaskExecutionChildrenOptionsI {
    refetchInterval?: number | false;
    workflowExecutionId: number;
}

interface LazyTaskExecutionChildrenProps {
    defaultValue?: string[];
    jobStatus?: Job['status'];
    lazy: LazyTaskExecutionChildrenOptionsI;
    onExecutionClick: (execution: TaskExecution | TriggerExecution) => void;
    parentTaskExecution: TaskExecution;
    selectedExecutionId: string;
}

const LazyTaskExecutionChildren = ({
    defaultValue,
    jobStatus,
    lazy,
    onExecutionClick,
    parentTaskExecution,
    selectedExecutionId,
}: LazyTaskExecutionChildrenProps) => {
    const {data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading} =
        useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery(
            {id: lazy.workflowExecutionId, taskExecutionId: +parentTaskExecution.id!},
            true,
            lazy.refetchInterval
        );

    const pages = useMemo(() => data?.pages || [], [data]);

    const iterations = useMemo(
        () =>
            pages.some((page) => page.iterations !== undefined)
                ? pages.flatMap((page) => page.iterations || [])
                : undefined,
        [pages]
    );

    const children = useMemo(
        () => (iterations === undefined ? pages.flatMap((page) => page.children || []) : undefined),
        [iterations, pages]
    );

    const loadedCount = iterations?.length ?? children?.length ?? 0;
    const totalElements = pages[pages.length - 1]?.totalElements ?? loadedCount;
    const remainingCount = Math.max(totalElements - loadedCount, 0);

    const focusedIterationIndex = useMemo(() => {
        const iterationValuePrefix = `${parentTaskExecution.id}-iteration-`;

        const iterationIndexes = (defaultValue || [])
            .filter((value) => value.startsWith(iterationValuePrefix))
            .map((value) => Number(value.slice(iterationValuePrefix.length)))
            .filter((iterationIndex) => Number.isInteger(iterationIndex));

        return iterationIndexes.length > 0 ? Math.max(...iterationIndexes) : undefined;
    }, [defaultValue, parentTaskExecution.id]);

    const handleLoadMore = useCallback(() => {
        if (hasNextPage && !isFetchingNextPage) {
            fetchNextPage();
        }
    }, [fetchNextPage, hasNextPage, isFetchingNextPage]);

    useEffect(() => {
        if (focusedIterationIndex === undefined || isLoading || focusedIterationIndex < loadedCount) {
            return;
        }

        handleLoadMore();
    }, [focusedIterationIndex, handleLoadMore, isLoading, loadedCount]);

    return (
        <>
            <TaskExecutionChildrenList
                defaultValue={defaultValue}
                hasMore={!!hasNextPage}
                iterations={iterations}
                jobStatus={jobStatus}
                lazy={lazy}
                onExecutionClick={onExecutionClick}
                onLoadMore={handleLoadMore}
                parentTaskExecution={parentTaskExecution}
                remainingCount={remainingCount}
                selectedExecutionId={selectedExecutionId}
            >
                {children}
            </TaskExecutionChildrenList>

            {(isLoading || isFetchingNextPage) && (
                <div className="flex items-center justify-center p-2 text-content-neutral-secondary">
                    <LoadingIcon />
                </div>
            )}
        </>
    );
};

export default LazyTaskExecutionChildren;
