import {
    TaskExecution,
    TaskExecutionChildrenPage,
    TaskExecutionChildrenPageFromJSON,
} from '@/shared/middleware/automation/workflow/execution';
import {act, render, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import LazyTaskExecutionChildren from '../LazyTaskExecutionChildren';

const hoisted = vi.hoisted(() => ({
    useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery: vi.fn(),
}));

vi.mock('@/shared/queries/automation/workflowExecutions.queries', () => ({
    useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery:
        hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery,
}));

const loopTaskExecution = {
    id: '4',
    jobId: '7',
    priority: 0,
    status: 'COMPLETED',
    title: 'Loop',
    type: 'loop/v1',
} as TaskExecution;

const plainTaskExecution = {
    id: '5',
    jobId: '7',
    priority: 0,
    status: 'COMPLETED',
    title: 'Parallel',
    type: 'parallel/v1',
} as TaskExecution;

const createPlainPage = (childCount: number, totalElements: number): TaskExecutionChildrenPage =>
    TaskExecutionChildrenPageFromJSON({
        children: Array.from({length: childCount}, (_, index) => ({
            childCount: 0,
            id: `${200 + index}`,
            jobId: '7',
            priority: 0,
            status: 'COMPLETED',
            title: `Child task ${index + 1}`,
        })),
        iterations: [],
        number: 0,
        numberOfElements: childCount,
        size: 50,
        totalElements,
        totalPages: Math.ceil(totalElements / 50),
    });

const mockPlainQueryResult = ({
    childCount,
    fetchNextPage = vi.fn(),
    hasNextPage = false,
    isFetchNextPageError = false,
    isFetchingNextPage = false,
    totalElements,
}: {
    childCount: number;
    fetchNextPage?: () => void;
    hasNextPage?: boolean;
    isFetchNextPageError?: boolean;
    isFetchingNextPage?: boolean;
    totalElements: number;
}) => {
    hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery.mockReturnValue({
        data: {pages: [createPlainPage(childCount, totalElements)]},
        fetchNextPage,
        hasNextPage,
        isFetchNextPageError,
        isFetchingNextPage,
        isLoading: false,
    });

    return fetchNextPage;
};

const createIteration = (index: number) => [
    {id: `${100 + index}`, jobId: '7', priority: 0, status: 'COMPLETED', title: `Row task ${index + 1}`},
];

const mockQueryResult = ({
    fetchNextPage = vi.fn(),
    hasNextPage = false,
    isFetchNextPageError = false,
    iterationCount,
    totalElements,
}: {
    fetchNextPage?: () => void;
    hasNextPage?: boolean;
    isFetchNextPageError?: boolean;
    iterationCount: number;
    totalElements: number;
}) => {
    hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery.mockReturnValue({
        data: {
            pages: [
                {
                    iterations: Array.from({length: iterationCount}, (_, index) => createIteration(index)),
                    number: 0,
                    numberOfElements: iterationCount,
                    size: 50,
                    totalElements,
                    totalPages: Math.ceil(totalElements / 50),
                },
            ],
        },
        fetchNextPage,
        hasNextPage,
        isFetchNextPageError,
        isFetchingNextPage: false,
        isLoading: false,
    });

    return fetchNextPage;
};

describe('LazyTaskExecutionChildren', () => {
    let intersectionCallbacks: Array<{callback: IntersectionObserverCallback; element: Element}>;

    beforeEach(() => {
        intersectionCallbacks = [];

        hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery.mockReset();

        vi.stubGlobal(
            'IntersectionObserver',
            class {
                constructor(private callback: IntersectionObserverCallback) {}

                disconnect() {
                    intersectionCallbacks = intersectionCallbacks.filter((entry) => entry.callback !== this.callback);
                }

                observe(element: Element) {
                    intersectionCallbacks.push({callback: this.callback, element});
                }
            }
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('rendersLoopIterationsFromPages', () => {
        mockQueryResult({iterationCount: 2, totalElements: 2});

        render(
            <LazyTaskExecutionChildren
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={loopTaskExecution}
                selectedExecutionId=""
            />
        );

        expect(hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery).toHaveBeenCalledWith(
            {id: 9, taskExecutionId: 4},
            true,
            undefined
        );
        expect(screen.getByText('Loop iteration 1')).toBeInTheDocument();
        expect(screen.getByText('Loop iteration 2')).toBeInTheDocument();
        expect(screen.queryByText(/more iterations\.\.\./)).not.toBeInTheDocument();
    });

    it('loadsNextPageWhenSentinelIsVisible', () => {
        const fetchNextPage = mockQueryResult({hasNextPage: true, iterationCount: 50, totalElements: 60});

        render(
            <LazyTaskExecutionChildren
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={loopTaskExecution}
                selectedExecutionId=""
            />
        );

        const sentinel = screen.getByText('Loading 10 more iterations...');
        const observed = intersectionCallbacks.find((entry) => entry.element === sentinel);

        expect(observed).toBeDefined();
        expect(fetchNextPage).not.toHaveBeenCalled();

        act(() => {
            observed!.callback(
                [{isIntersecting: true, target: sentinel} as unknown as IntersectionObserverEntry],
                {} as IntersectionObserver
            );
        });

        expect(fetchNextPage).toHaveBeenCalled();
    });

    it('loadsPagesUntilFocusedIterationIsVisible', () => {
        const fetchNextPage = mockQueryResult({hasNextPage: true, iterationCount: 50, totalElements: 100});

        render(
            <LazyTaskExecutionChildren
                defaultValue={['4', '4-iteration-60']}
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={loopTaskExecution}
                selectedExecutionId=""
            />
        );

        expect(fetchNextPage).toHaveBeenCalled();
    });

    it('refetchesOpenPagesWhileRunning', () => {
        mockQueryResult({iterationCount: 1, totalElements: 1});

        render(
            <LazyTaskExecutionChildren
                lazy={{refetchInterval: 2000, workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={loopTaskExecution}
                selectedExecutionId=""
            />
        );

        expect(hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery).toHaveBeenCalledWith(
            {id: 9, taskExecutionId: 4},
            true,
            2000
        );
    });

    it('stopsLoadingPagesAfterAFailedPageFetchUntilRetried', async () => {
        const fetchNextPage = mockQueryResult({
            hasNextPage: true,
            isFetchNextPageError: true,
            iterationCount: 50,
            totalElements: 100,
        });

        render(
            <LazyTaskExecutionChildren
                defaultValue={['4', '4-iteration-60']}
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={loopTaskExecution}
                selectedExecutionId=""
            />
        );

        const sentinel = screen.getByText('Loading 50 more iterations...');
        const observed = intersectionCallbacks.find((entry) => entry.element === sentinel);

        expect(observed).toBeDefined();

        act(() => {
            observed!.callback(
                [{isIntersecting: true, target: sentinel} as unknown as IntersectionObserverEntry],
                {} as IntersectionObserver
            );
        });

        expect(fetchNextPage).not.toHaveBeenCalled();

        await userEvent.click(screen.getByRole('button', {name: 'Retry'}));

        expect(fetchNextPage).toHaveBeenCalledTimes(1);
    });

    it('rendersChildrenOfAPlainPageWithAnEmptyIterationsArray', () => {
        mockPlainQueryResult({childCount: 2, totalElements: 2});

        render(
            <LazyTaskExecutionChildren
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={plainTaskExecution}
                selectedExecutionId=""
            />
        );

        expect(screen.getByText('Child task 1')).toBeInTheDocument();
        expect(screen.getByText('Child task 2')).toBeInTheDocument();
        expect(screen.queryByText(/Loop iteration/)).not.toBeInTheDocument();
    });
});
