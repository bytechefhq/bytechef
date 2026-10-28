import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import TaskExecutionChildrenList from '@/shared/components/workflow-executions/TaskExecutionChildrenList';
import {useWorkflowExecutionDetailQueries} from '@/shared/components/workflow-executions/providers/workflowExecutionDetailQueriesProvider';
import {
    FocusPathElement,
    Job,
    TaskExecution,
    TaskExecutionChildrenPage,
    TaskExecutionStatusEnum,
    TriggerExecution,
} from '@/shared/middleware/automation/workflow/execution';
import {
    TASK_EXECUTION_CHILDREN_MAX_PAGES,
    TASK_EXECUTION_CHILDREN_PAGE_SIZE,
} from '@/shared/queries/automation/workflowExecutions.queries';
import {useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState} from 'react';

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

interface PageOffsetsSnapshotI {
    firstPageNumber: number;
    pageOffsets: number[];
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

const getPageRowCount = (page: TaskExecutionChildrenPage) =>
    (page.iterations?.length || 0) > 0 ? page.iterations!.length : page.children?.length || 0;

const measurePageOffsets = (listElement: HTMLElement | null, pages: TaskExecutionChildrenPage[]) => {
    const rowElements = listElement?.firstElementChild?.children;

    if (!listElement || !rowElements) {
        return [];
    }

    const listTop = listElement.getBoundingClientRect().top;
    const pageOffsets: number[] = [];

    let pageStartRowIndex = 0;

    for (const page of [...pages, undefined]) {
        const pageStartRowElement = rowElements[pageStartRowIndex];

        if (!pageStartRowElement) {
            break;
        }

        pageOffsets.push(pageStartRowElement.getBoundingClientRect().top - listTop);

        if (page) {
            pageStartRowIndex += getPageRowCount(page);
        }
    }

    return pageOffsets;
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

    const [droppedPagesHeight, setDroppedPagesHeight] = useState(0);

    const focusedChildReachedRef = useRef(false);
    const focusedIterationReachedRef = useRef(false);
    const listRef = useRef<HTMLDivElement>(null);
    const pageOffsetsSnapshotRef = useRef<PageOffsetsSnapshotI | undefined>(undefined);
    const parentActiveRef = useRef(isParentActive);

    const {useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery} = useWorkflowExecutionDetailQueries();

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
    const firstPageNumber = firstPage?.number;
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

    const snapshotPageOffsets = useCallback(() => {
        if (firstPageNumber !== undefined) {
            pageOffsetsSnapshotRef.current = {
                firstPageNumber,
                pageOffsets: measurePageOffsets(listRef.current, pages),
            };
        }
    }, [firstPageNumber, pages]);

    const handleLoadEarlier = useCallback(() => {
        if (hasPreviousPage && !isFetchingPreviousPage && !isFetchPreviousPageError) {
            snapshotPageOffsets();

            void fetchPreviousPage();
        }
    }, [fetchPreviousPage, hasPreviousPage, isFetchPreviousPageError, isFetchingPreviousPage, snapshotPageOffsets]);

    const handleLoadMore = useCallback(() => {
        if (hasNextPage && !isFetchingNextPage && !isFetchNextPageError) {
            snapshotPageOffsets();

            void fetchNextPage();
        }
    }, [fetchNextPage, hasNextPage, isFetchNextPageError, isFetchingNextPage, snapshotPageOffsets]);

    const handleRetryLoadEarlier = useCallback(() => {
        snapshotPageOffsets();

        void fetchPreviousPage();
    }, [fetchPreviousPage, snapshotPageOffsets]);

    const handleRetryLoadMore = useCallback(() => {
        snapshotPageOffsets();

        void fetchNextPage();
    }, [fetchNextPage, snapshotPageOffsets]);

    useLayoutEffect(() => {
        const pageOffsetsSnapshot = pageOffsetsSnapshotRef.current;

        if (
            !pageOffsetsSnapshot ||
            firstPageNumber === undefined ||
            firstPageNumber === pageOffsetsSnapshot.firstPageNumber
        ) {
            return;
        }

        pageOffsetsSnapshotRef.current = undefined;

        if (firstPageNumber === 0) {
            setDroppedPagesHeight(0);

            return;
        }

        if (firstPageNumber > pageOffsetsSnapshot.firstPageNumber) {
            const {pageOffsets} = pageOffsetsSnapshot;
            const droppedHeight =
                (pageOffsets[firstPageNumber - pageOffsetsSnapshot.firstPageNumber] ?? 0) - (pageOffsets[0] ?? 0);

            setDroppedPagesHeight((height) => height + Math.max(droppedHeight, 0));
        } else {
            const pageOffsets = measurePageOffsets(listRef.current, pages);
            const restoredHeight =
                (pageOffsets[pageOffsetsSnapshot.firstPageNumber - firstPageNumber] ?? 0) - (pageOffsets[0] ?? 0);

            setDroppedPagesHeight((height) => Math.max(height - Math.max(restoredHeight, 0), 0));
        }
    }, [firstPageNumber, pages]);

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

            {droppedPagesHeight > 0 && hasPreviousPage && (
                <div aria-hidden="true" data-testid="droppedPagesSpacer" style={{height: droppedPagesHeight}} />
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
                        onClick={handleRetryLoadEarlier}
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

            <div ref={listRef}>
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
            </div>

            {isFetchNextPageError && hasNextPage && !isFetchingNextPage && (
                <div
                    className="flex items-center justify-center gap-x-2 p-2 text-sm text-content-neutral-secondary"
                    role="status"
                >
                    <span>Failed to load more task executions.</span>

                    <Button
                        aria-label="Retry loading more task executions"
                        label="Retry"
                        onClick={handleRetryLoadMore}
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
