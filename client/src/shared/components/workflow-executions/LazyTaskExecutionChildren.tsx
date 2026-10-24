import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import TaskExecutionChildrenList from '@/shared/components/workflow-executions/TaskExecutionChildrenList';
import {
    FocusPathElement,
    Job,
    TaskExecution,
    TaskExecutionStatusEnum,
    TriggerExecution,
} from '@/shared/middleware/automation/workflow/execution';
import {
    TASK_EXECUTION_CHILDREN_MAX_PAGES,
    TASK_EXECUTION_CHILDREN_PAGE_SIZE,
    useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery,
} from '@/shared/queries/automation/workflowExecutions.queries';
import {useCallback, useEffect, useMemo, useRef} from 'react';

export interface LazyTaskExecutionChildrenOptionsI {
    focusPath?: FocusPathElement[];
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

const ACTIVE_TASK_EXECUTION_STATUSES: TaskExecution['status'][] = [
    TaskExecutionStatusEnum.Created,
    TaskExecutionStatusEnum.Started,
];

const getFocusedChildIndex = (focusPath: FocusPathElement[] | undefined, parentTaskExecutionId?: string) => {
    if (!focusPath || parentTaskExecutionId === undefined) {
        return undefined;
    }

    const parentPosition = focusPath.findIndex(
        (focusPathElement) => focusPathElement.taskExecutionId === parentTaskExecutionId
    );

    const focusedElement = parentPosition === -1 ? undefined : focusPath[parentPosition + 1];

    return focusedElement?.iterationIndex ?? focusedElement?.childIndex;
};

const LazyTaskExecutionChildren = ({
    defaultValue,
    jobStatus,
    lazy,
    onExecutionClick,
    parentTaskExecution,
    selectedExecutionId,
}: LazyTaskExecutionChildrenProps) => {
    const isParentActive = ACTIVE_TASK_EXECUTION_STATUSES.includes(parentTaskExecution.status);
    const focusedChildIndex = getFocusedChildIndex(lazy.focusPath, parentTaskExecution.id);

    const focusedChildReachedRef = useRef(false);
    const focusedIterationReachedRef = useRef(false);
    const parentActiveRef = useRef(isParentActive);

    const {
        data,
        fetchNextPage,
        fetchPreviousPage,
        hasNextPage,
        hasPreviousPage,
        isError,
        isFetchNextPageError,
        isFetchPreviousPageError,
        isFetchingNextPage,
        isFetchingPreviousPage,
        isLoading,
        isRefetchError,
        refetch,
    } = useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery(
        {id: lazy.workflowExecutionId, taskExecutionId: +parentTaskExecution.id!},
        true,
        isParentActive ? lazy.refetchInterval : false,
        focusedChildIndex === undefined ? 0 : Math.floor(focusedChildIndex / TASK_EXECUTION_CHILDREN_PAGE_SIZE)
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

    const firstPage = pages.at(0);
    const startIndex = firstPage ? firstPage.number * firstPage.size : 0;
    const loadedCount = iterations?.length ?? children?.length ?? 0;
    const endIndex = startIndex + loadedCount;
    const totalElements = pages.at(-1)?.totalElements ?? endIndex;
    const remainingCount = Math.max(totalElements - endIndex, 0);

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

    const handleLoadEarlier = useCallback(() => {
        if (hasPreviousPage && !isFetchingPreviousPage && !isFetchPreviousPageError) {
            void fetchPreviousPage();
        }
    }, [fetchPreviousPage, hasPreviousPage, isFetchPreviousPageError, isFetchingPreviousPage]);

    const handleLoadMore = useCallback(() => {
        if (hasNextPage && !isFetchingNextPage && !isFetchNextPageError) {
            void fetchNextPage();
        }
    }, [fetchNextPage, hasNextPage, isFetchNextPageError, isFetchingNextPage]);

    useEffect(() => {
        if (parentActiveRef.current && !isParentActive) {
            void refetch();
        }

        parentActiveRef.current = isParentActive;
    }, [isParentActive, refetch]);

    useEffect(() => {
        if (focusedIterationReachedRef.current || focusedIterationIndex === undefined || isLoading) {
            return;
        }

        if (focusedIterationIndex < startIndex) {
            handleLoadEarlier();
        } else if (focusedIterationIndex >= endIndex) {
            handleLoadMore();
        } else {
            focusedIterationReachedRef.current = true;
        }
    }, [endIndex, focusedIterationIndex, handleLoadEarlier, handleLoadMore, isLoading, startIndex]);

    useEffect(() => {
        if (focusedChildReachedRef.current || isLoading) {
            return;
        }

        if (isFocusedChildLoaded) {
            focusedChildReachedRef.current = focusedChildId !== undefined;

            return;
        }

        if (focusedChildIndex === undefined) {
            if (pages.length < TASK_EXECUTION_CHILDREN_MAX_PAGES) {
                handleLoadMore();
            } else {
                focusedChildReachedRef.current = true;
            }
        } else if (focusedChildIndex < startIndex) {
            handleLoadEarlier();
        } else if (focusedChildIndex >= endIndex) {
            handleLoadMore();
        } else {
            focusedChildReachedRef.current = true;
        }
    }, [
        endIndex,
        focusedChildId,
        focusedChildIndex,
        handleLoadEarlier,
        handleLoadMore,
        isFocusedChildLoaded,
        isLoading,
        pages.length,
        startIndex,
    ]);

    if (isError && !data) {
        return (
            <div
                className="flex items-center justify-center gap-x-2 p-2 text-sm text-content-neutral-secondary"
                role="status"
            >
                <span>Failed to load task executions.</span>

                <Button
                    aria-label="Retry loading task executions"
                    label="Retry"
                    onClick={() => void refetch()}
                    size="xs"
                    variant="outline"
                />
            </div>
        );
    }

    return (
        <>
            {isRefetchError && (
                <div
                    className="flex items-center justify-center gap-x-2 p-2 text-sm text-content-neutral-secondary"
                    role="status"
                >
                    <span>Live updates failed.</span>

                    <Button
                        aria-label="Retry live updates"
                        label="Retry"
                        onClick={() => void refetch()}
                        size="xs"
                        variant="outline"
                    />
                </div>
            )}

            {hasPreviousPage && !isFetchPreviousPageError && (
                <div className="flex items-center justify-center p-2">
                    <Button
                        disabled={isFetchingPreviousPage}
                        label={`Show ${Math.min(startIndex, TASK_EXECUTION_CHILDREN_PAGE_SIZE)} earlier ${iterations === undefined ? 'tasks' : 'iterations'}`}
                        onClick={handleLoadEarlier}
                        size="xs"
                        variant="outline"
                    />
                </div>
            )}

            {isFetchPreviousPageError && hasPreviousPage && !isFetchingPreviousPage && (
                <div
                    className="flex items-center justify-center gap-x-2 p-2 text-sm text-content-neutral-secondary"
                    role="status"
                >
                    <span>Failed to load earlier task executions.</span>

                    <Button
                        aria-label="Retry loading earlier task executions"
                        label="Retry"
                        onClick={() => void fetchPreviousPage()}
                        size="xs"
                        variant="outline"
                    />
                </div>
            )}

            {isFetchingPreviousPage && (
                <div className="flex items-center justify-center p-2 text-content-neutral-secondary" role="status">
                    <LoadingIcon />
                </div>
            )}

            <TaskExecutionChildrenList
                defaultValue={defaultValue}
                hasMore={!!hasNextPage && !isFetchNextPageError}
                indexOffset={startIndex}
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
                <div
                    className="flex items-center justify-center gap-x-2 p-2 text-sm text-content-neutral-secondary"
                    role="status"
                >
                    <span>Failed to load more task executions.</span>

                    <Button
                        aria-label="Retry loading more task executions"
                        label="Retry"
                        onClick={() => void fetchNextPage()}
                        size="xs"
                        variant="outline"
                    />
                </div>
            )}

            {(isLoading || isFetchingNextPage) && (
                <div className="flex items-center justify-center p-2 text-content-neutral-secondary" role="status">
                    <LoadingIcon />
                </div>
            )}
        </>
    );
};

export default LazyTaskExecutionChildren;
