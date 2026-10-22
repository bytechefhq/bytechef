import Button from '@/components/Button/Button';
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
    const {data, fetchNextPage, hasNextPage, isFetchNextPageError, isFetchingNextPage, isLoading} =
        useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery(
            {id: lazy.workflowExecutionId, taskExecutionId: +parentTaskExecution.id!},
            true,
            lazy.refetchInterval
        );

    const pages = useMemo(() => data?.pages || [], [data]);

    const iterations = useMemo(
        () =>
            pages.some((page) => (page.iterations?.length || 0) > 0)
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

    const focusedChildId = useMemo(() => {
        if (children === undefined || !defaultValue) {
            return undefined;
        }

        const parentIndex = defaultValue.indexOf(`${parentTaskExecution.id}`);
        const nextValue = parentIndex === -1 ? undefined : defaultValue[parentIndex + 1];

        if (nextValue === undefined || nextValue.startsWith(`${parentTaskExecution.id}-iteration-`)) {
            return undefined;
        }

        return nextValue;
    }, [children, defaultValue, parentTaskExecution.id]);

    const isFocusedChildLoaded = useMemo(
        () =>
            focusedChildId === undefined ||
            (children || []).some((childTaskExecution) => childTaskExecution.id === focusedChildId),
        [children, focusedChildId]
    );

    const handleLoadMore = useCallback(() => {
        if (hasNextPage && !isFetchingNextPage && !isFetchNextPageError) {
            void fetchNextPage();
        }
    }, [fetchNextPage, hasNextPage, isFetchNextPageError, isFetchingNextPage]);

    useEffect(() => {
        if (focusedIterationIndex === undefined || isLoading || focusedIterationIndex < loadedCount) {
            return;
        }

        handleLoadMore();
    }, [focusedIterationIndex, handleLoadMore, isLoading, loadedCount]);

    useEffect(() => {
        if (isFocusedChildLoaded || isLoading) {
            return;
        }

        handleLoadMore();
    }, [handleLoadMore, isFocusedChildLoaded, isLoading]);

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

            {isFetchNextPageError && hasNextPage && !isFetchingNextPage && (
                <div className="flex items-center justify-center gap-x-2 p-2 text-sm text-content-neutral-secondary">
                    <span>Failed to load more task executions.</span>

                    <Button label="Retry" onClick={() => void fetchNextPage()} size="xs" variant="outline" />
                </div>
            )}

            {(isLoading || isFetchingNextPage) && (
                <div className="flex items-center justify-center p-2 text-content-neutral-secondary">
                    <LoadingIcon />
                </div>
            )}
        </>
    );
};

export default LazyTaskExecutionChildren;
