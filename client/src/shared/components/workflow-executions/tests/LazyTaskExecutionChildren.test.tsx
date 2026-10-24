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
    TASK_EXECUTION_CHILDREN_MAX_PAGES: 10,
    TASK_EXECUTION_CHILDREN_PAGE_SIZE: 50,
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

const onErrorTaskExecution = {
    id: '6',
    jobId: '7',
    priority: 0,
    status: 'COMPLETED',
    title: 'On error',
    type: 'on-error/v1',
} as TaskExecution;

const plainTaskExecution = {
    id: '5',
    jobId: '7',
    priority: 0,
    status: 'COMPLETED',
    title: 'Parallel',
    type: 'parallel/v1',
} as TaskExecution;

const createPlainPage = (childCount: number, totalElements: number, pageNumber = 0): TaskExecutionChildrenPage =>
    TaskExecutionChildrenPageFromJSON({
        children: Array.from({length: childCount}, (_, index) => ({
            childCount: 0,
            id: `${200 + pageNumber * 50 + index}`,
            jobId: '7',
            priority: 0,
            status: 'COMPLETED',
            title: `Child task ${index + 1}`,
        })),
        iterations: [],
        number: pageNumber,
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
    pageCount = 1,
    totalElements,
}: {
    childCount: number;
    fetchNextPage?: () => void;
    hasNextPage?: boolean;
    isFetchNextPageError?: boolean;
    isFetchingNextPage?: boolean;
    pageCount?: number;
    totalElements: number;
}) => {
    hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery.mockReturnValue({
        data: {
            pages: Array.from({length: pageCount}, (_, pageNumber) =>
                createPlainPage(childCount, totalElements, pageNumber)
            ),
        },
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
    fetchPreviousPage = vi.fn(),
    hasNextPage = false,
    hasPreviousPage = false,
    isFetchNextPageError = false,
    iterationCount,
    pageNumber = 0,
    totalElements,
}: {
    fetchNextPage?: () => void;
    fetchPreviousPage?: () => void;
    hasNextPage?: boolean;
    hasPreviousPage?: boolean;
    isFetchNextPageError?: boolean;
    iterationCount: number;
    pageNumber?: number;
    totalElements: number;
}) => {
    hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery.mockReturnValue({
        data: {
            pages: [
                {
                    iterations: Array.from({length: iterationCount}, (_, index) => createIteration(index)),
                    number: pageNumber,
                    numberOfElements: iterationCount,
                    size: 50,
                    totalElements,
                    totalPages: Math.ceil(totalElements / 50),
                },
            ],
        },
        fetchNextPage,
        fetchPreviousPage,
        hasNextPage,
        hasPreviousPage,
        isFetchNextPageError,
        isFetchingNextPage: false,
        isLoading: false,
    });

    return fetchNextPage;
};

describe('LazyTaskExecutionChildren', () => {
    let intersectionCallbacks: Array<{
        callback: IntersectionObserverCallback;
        element: Element;
        options?: IntersectionObserverInit;
    }>;

    beforeEach(() => {
        intersectionCallbacks = [];

        hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery.mockReset();

        vi.stubGlobal(
            'IntersectionObserver',
            class {
                constructor(
                    private callback: IntersectionObserverCallback,
                    private options?: IntersectionObserverInit
                ) {}

                disconnect() {
                    intersectionCallbacks = intersectionCallbacks.filter((entry) => entry.callback !== this.callback);
                }

                observe(element: Element) {
                    intersectionCallbacks.push({callback: this.callback, element, options: this.options});
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
            false,
            0
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

    it('observesTheSentinelWithinItsScrollAreaViewport', () => {
        mockQueryResult({hasNextPage: true, iterationCount: 50, totalElements: 60});

        render(
            <div data-radix-scroll-area-viewport="" data-testid="viewport">
                <LazyTaskExecutionChildren
                    lazy={{workflowExecutionId: 9}}
                    onExecutionClick={vi.fn()}
                    parentTaskExecution={loopTaskExecution}
                    selectedExecutionId=""
                />
            </div>
        );

        const sentinel = screen.getByText('Loading 10 more iterations...');
        const observed = intersectionCallbacks.find((entry) => entry.element === sentinel);

        expect(observed?.options?.root).toBe(screen.getByTestId('viewport'));
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

    it('refetchesOpenPagesOfAStartedParentWhileRunning', () => {
        mockQueryResult({iterationCount: 1, totalElements: 1});

        render(
            <LazyTaskExecutionChildren
                lazy={{refetchInterval: 2000, workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={{...loopTaskExecution, status: 'STARTED'}}
                selectedExecutionId=""
            />
        );

        expect(hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery).toHaveBeenCalledWith(
            {id: 9, taskExecutionId: 4},
            true,
            2000,
            0
        );
    });

    it('refetchesOpenPagesOnceWhenTheParentFinishes', () => {
        mockQueryResult({iterationCount: 1, totalElements: 1});

        const refetch = vi.fn();

        hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery.mockReturnValue({
            ...hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery(),
            refetch,
        });

        const {rerender} = render(
            <LazyTaskExecutionChildren
                lazy={{refetchInterval: 2000, workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={{...loopTaskExecution, status: 'STARTED'}}
                selectedExecutionId=""
            />
        );

        expect(refetch).not.toHaveBeenCalled();

        rerender(
            <LazyTaskExecutionChildren
                lazy={{refetchInterval: 2000, workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={{...loopTaskExecution, status: 'COMPLETED'}}
                selectedExecutionId=""
            />
        );

        rerender(
            <LazyTaskExecutionChildren
                lazy={{refetchInterval: 2000, workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={{...loopTaskExecution, status: 'COMPLETED'}}
                selectedExecutionId=""
            />
        );

        expect(refetch).toHaveBeenCalledTimes(1);
    });

    it('doesNotRefetchOpenPagesOfAParentThatWasAlreadyFinished', () => {
        mockQueryResult({iterationCount: 1, totalElements: 1});

        const refetch = vi.fn();

        hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery.mockReturnValue({
            ...hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery(),
            refetch,
        });

        render(
            <LazyTaskExecutionChildren
                lazy={{refetchInterval: false, workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={{...loopTaskExecution, status: 'COMPLETED'}}
                selectedExecutionId=""
            />
        );

        expect(refetch).not.toHaveBeenCalled();
    });

    it.each(['COMPLETED', 'FAILED', 'CANCELLED'] as const)('doesNotRefetchPagesOfA%sParentWhileRunning', (status) => {
        mockQueryResult({iterationCount: 1, totalElements: 1});

        render(
            <LazyTaskExecutionChildren
                lazy={{refetchInterval: 2000, workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={{...loopTaskExecution, status}}
                selectedExecutionId=""
            />
        );

        expect(hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery).toHaveBeenCalledWith(
            {id: 9, taskExecutionId: 4},
            true,
            false,
            0
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

        expect(screen.queryByText('Loading 50 more iterations...')).not.toBeInTheDocument();
        expect(intersectionCallbacks).toHaveLength(0);
        expect(fetchNextPage).not.toHaveBeenCalled();

        await userEvent.click(screen.getByRole('button', {name: 'Retry loading more task executions'}));

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

    it('loadsPagesUntilFocusedPlainChildIsLoaded', () => {
        const fetchNextPage = mockPlainQueryResult({childCount: 50, hasNextPage: true, totalElements: 387});

        render(
            <LazyTaskExecutionChildren
                defaultValue={['5', '586']}
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={plainTaskExecution}
                selectedExecutionId="586"
            />
        );

        expect(fetchNextPage).toHaveBeenCalledTimes(1);
    });

    it('doesNotLoadMorePagesWhenFocusedPlainChildIsLoaded', () => {
        const fetchNextPage = mockPlainQueryResult({childCount: 50, hasNextPage: true, totalElements: 387});

        render(
            <LazyTaskExecutionChildren
                defaultValue={['5', '210']}
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={plainTaskExecution}
                selectedExecutionId="210"
            />
        );

        expect(fetchNextPage).not.toHaveBeenCalled();
    });

    it('doesNotLoadMorePagesWhenPlainParentIsNotOnTheFocusPath', () => {
        const fetchNextPage = mockPlainQueryResult({childCount: 50, hasNextPage: true, totalElements: 387});

        render(
            <LazyTaskExecutionChildren
                defaultValue={['3', '586']}
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={plainTaskExecution}
                selectedExecutionId="586"
            />
        );

        expect(fetchNextPage).not.toHaveBeenCalled();
    });

    it('doesNotLoadMorePagesForFocusedPlainChildWhileFetchingOrAfterAFailedFetch', () => {
        const fetchingNextPage = mockPlainQueryResult({
            childCount: 50,
            hasNextPage: true,
            isFetchingNextPage: true,
            totalElements: 387,
        });

        const {unmount} = render(
            <LazyTaskExecutionChildren
                defaultValue={['5', '586']}
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={plainTaskExecution}
                selectedExecutionId="586"
            />
        );

        expect(fetchingNextPage).not.toHaveBeenCalled();

        unmount();

        const failedNextPage = mockPlainQueryResult({
            childCount: 50,
            hasNextPage: true,
            isFetchNextPageError: true,
            totalElements: 387,
        });

        render(
            <LazyTaskExecutionChildren
                defaultValue={['5', '586']}
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={plainTaskExecution}
                selectedExecutionId="586"
            />
        );

        expect(failedNextPage).not.toHaveBeenCalled();
    });

    it('doesNotLoadMorePagesForFocusedPlainChildWithoutANextPage', () => {
        const fetchNextPage = mockPlainQueryResult({childCount: 50, hasNextPage: false, totalElements: 50});

        render(
            <LazyTaskExecutionChildren
                defaultValue={['5', '586']}
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={plainTaskExecution}
                selectedExecutionId="586"
            />
        );

        expect(fetchNextPage).not.toHaveBeenCalled();
    });

    it('stopsLoadingPagesForAFocusedPlainChildWithoutAnIndexOnceThePageLimitIsLoaded', () => {
        const fetchNextPage = mockPlainQueryResult({
            childCount: 50,
            hasNextPage: true,
            pageCount: 10,
            totalElements: 5000,
        });

        render(
            <LazyTaskExecutionChildren
                defaultValue={['5', '4999']}
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={plainTaskExecution}
                selectedExecutionId="4999"
            />
        );

        expect(fetchNextPage).not.toHaveBeenCalled();
    });

    it('startsOnThePageOfTheFocusedIteration', () => {
        mockQueryResult({iterationCount: 50, pageNumber: 2, totalElements: 300});

        render(
            <LazyTaskExecutionChildren
                defaultValue={['4', '4-iteration-120', '9']}
                lazy={{
                    focusPath: [{taskExecutionId: '4'}, {iterationIndex: 120, taskExecutionId: '9'}],
                    workflowExecutionId: 9,
                }}
                onExecutionClick={vi.fn()}
                parentTaskExecution={loopTaskExecution}
                selectedExecutionId="9"
            />
        );

        expect(hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery).toHaveBeenCalledWith(
            {id: 9, taskExecutionId: 4},
            true,
            false,
            2
        );
    });

    it('startsOnThePageOfTheFocusedPlainChild', () => {
        mockPlainQueryResult({childCount: 50, totalElements: 387});

        render(
            <LazyTaskExecutionChildren
                lazy={{
                    focusPath: [{taskExecutionId: '5'}, {childIndex: 336, taskExecutionId: '586'}],
                    workflowExecutionId: 9,
                }}
                onExecutionClick={vi.fn()}
                parentTaskExecution={plainTaskExecution}
                selectedExecutionId="586"
            />
        );

        expect(hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery).toHaveBeenCalledWith(
            {id: 9, taskExecutionId: 5},
            true,
            false,
            6
        );
    });

    it('numbersIterationsOfALaterPageFromTheirPositionInTheLoop', () => {
        mockQueryResult({hasPreviousPage: true, iterationCount: 2, pageNumber: 2, totalElements: 102});

        render(
            <LazyTaskExecutionChildren
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={loopTaskExecution}
                selectedExecutionId=""
            />
        );

        expect(screen.getByText('Loop iteration 101')).toBeInTheDocument();
        expect(screen.getByText('Loop iteration 102')).toBeInTheDocument();
        expect(screen.queryByText('Loop iteration 1')).not.toBeInTheDocument();
    });

    it('loadsEarlierIterationsOnRequest', async () => {
        const fetchPreviousPage = vi.fn();

        mockQueryResult({
            fetchPreviousPage,
            hasPreviousPage: true,
            iterationCount: 50,
            pageNumber: 2,
            totalElements: 300,
        });

        render(
            <LazyTaskExecutionChildren
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={loopTaskExecution}
                selectedExecutionId=""
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Show 50 earlier iterations'}));

        expect(fetchPreviousPage).toHaveBeenCalledTimes(1);
    });

    it('loadsEarlierPagesUntilFocusedIterationIsVisible', () => {
        const fetchPreviousPage = vi.fn();

        const fetchNextPage = mockQueryResult({
            fetchPreviousPage,
            hasNextPage: true,
            hasPreviousPage: true,
            iterationCount: 50,
            pageNumber: 2,
            totalElements: 300,
        });

        render(
            <LazyTaskExecutionChildren
                defaultValue={['4', '4-iteration-30']}
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={loopTaskExecution}
                selectedExecutionId=""
            />
        );

        expect(fetchPreviousPage).toHaveBeenCalledTimes(1);
        expect(fetchNextPage).not.toHaveBeenCalled();
    });

    it('stopsSeekingTheFocusedIterationOnceItWasReached', () => {
        const fetchPreviousPage = vi.fn();

        mockQueryResult({fetchPreviousPage, hasNextPage: true, iterationCount: 50, totalElements: 600});

        const {rerender} = render(
            <LazyTaskExecutionChildren
                defaultValue={['4', '4-iteration-10']}
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={loopTaskExecution}
                selectedExecutionId=""
            />
        );

        mockQueryResult({
            fetchPreviousPage,
            hasNextPage: true,
            hasPreviousPage: true,
            iterationCount: 50,
            pageNumber: 1,
            totalElements: 600,
        });

        rerender(
            <LazyTaskExecutionChildren
                defaultValue={['4', '4-iteration-10']}
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={loopTaskExecution}
                selectedExecutionId=""
            />
        );

        expect(fetchPreviousPage).not.toHaveBeenCalled();
    });

    it('stopsSeekingTheFocusedPlainChildOnceItWasReached', () => {
        const fetchPreviousPage = vi.fn();
        const focusPath = [{taskExecutionId: '5'}, {childIndex: 10, taskExecutionId: '210'}];

        hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery.mockReturnValue({
            data: {pages: [createPlainPage(50, 600)]},
            fetchNextPage: vi.fn(),
            fetchPreviousPage,
            hasNextPage: true,
            hasPreviousPage: false,
            isFetchingNextPage: false,
            isLoading: false,
        });

        const {rerender} = render(
            <LazyTaskExecutionChildren
                defaultValue={['5', '210']}
                lazy={{focusPath, workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={plainTaskExecution}
                selectedExecutionId="210"
            />
        );

        hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery.mockReturnValue({
            data: {pages: [{...createPlainPage(50, 600), children: [], number: 1}]},
            fetchNextPage: vi.fn(),
            fetchPreviousPage,
            hasNextPage: true,
            hasPreviousPage: true,
            isFetchingNextPage: false,
            isLoading: false,
        });

        rerender(
            <LazyTaskExecutionChildren
                defaultValue={['5', '210']}
                lazy={{focusPath, workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={plainTaskExecution}
                selectedExecutionId="210"
            />
        );

        expect(fetchPreviousPage).not.toHaveBeenCalled();
    });

    it('stopsSeekingAFocusedPlainChildThatIsMissingFromItsPage', () => {
        const fetchNextPage = mockPlainQueryResult({childCount: 50, hasNextPage: true, totalElements: 600});

        render(
            <LazyTaskExecutionChildren
                defaultValue={['5', '999']}
                lazy={{
                    focusPath: [{taskExecutionId: '5'}, {childIndex: 10, taskExecutionId: '999'}],
                    workflowExecutionId: 9,
                }}
                onExecutionClick={vi.fn()}
                parentTaskExecution={plainTaskExecution}
                selectedExecutionId="999"
            />
        );

        expect(fetchNextPage).not.toHaveBeenCalled();
    });

    it('showsARetryControlWhenTheFirstPageFails', async () => {
        const refetch = vi.fn();

        hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery.mockReturnValue({
            data: undefined,
            fetchNextPage: vi.fn(),
            fetchPreviousPage: vi.fn(),
            hasNextPage: false,
            hasPreviousPage: false,
            isError: true,
            isFetchNextPageError: false,
            isFetchingNextPage: false,
            isLoading: false,
            refetch,
        });

        render(
            <LazyTaskExecutionChildren
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={loopTaskExecution}
                selectedExecutionId=""
            />
        );

        expect(screen.getByText('Failed to load task executions.')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Retry loading task executions'}));

        expect(refetch).toHaveBeenCalledTimes(1);
    });

    it('keepsLoadedChildrenAndOffersARetryWhenARefetchFails', async () => {
        const refetch = vi.fn();

        hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery.mockReturnValue({
            data: {pages: [createPlainPage(2, 2)]},
            fetchNextPage: vi.fn(),
            fetchPreviousPage: vi.fn(),
            hasNextPage: false,
            hasPreviousPage: false,
            isError: true,
            isFetchNextPageError: false,
            isFetchingNextPage: false,
            isLoading: false,
            isRefetchError: true,
            refetch,
        });

        render(
            <LazyTaskExecutionChildren
                lazy={{refetchInterval: 2000, workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={{...plainTaskExecution, status: 'STARTED'}}
                selectedExecutionId=""
            />
        );

        expect(screen.getByText('Live updates failed.')).toBeInTheDocument();
        expect(screen.getByText('Child task 1')).toBeInTheDocument();
        expect(screen.getByText('Child task 2')).toBeInTheDocument();
        expect(screen.queryByText('Failed to load task executions.')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Retry live updates'}));

        expect(refetch).toHaveBeenCalledTimes(1);
    });

    it('doesNotShowTheLiveUpdatesNoticeWhenOnlyTheNextPageFailed', () => {
        mockPlainQueryResult({childCount: 50, hasNextPage: true, isFetchNextPageError: true, totalElements: 100});

        render(
            <LazyTaskExecutionChildren
                lazy={{workflowExecutionId: 9}}
                onExecutionClick={vi.fn()}
                parentTaskExecution={plainTaskExecution}
                selectedExecutionId=""
            />
        );

        expect(screen.queryByText('Live updates failed.')).not.toBeInTheDocument();
        expect(screen.getByText('Failed to load more task executions.')).toBeInTheDocument();
    });

    describe('earlier page failure', () => {
        const mockFailedPreviousPage = (fetchPreviousPage: () => void) =>
            hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery.mockReturnValue({
                data: {
                    pages: [
                        {
                            iterations: Array.from({length: 50}, (_, index) => createIteration(index)),
                            number: 2,
                            numberOfElements: 50,
                            size: 50,
                            totalElements: 300,
                            totalPages: 6,
                        },
                    ],
                },
                fetchNextPage: vi.fn(),
                fetchPreviousPage,
                hasNextPage: true,
                hasPreviousPage: true,
                isError: true,
                isFetchNextPageError: false,
                isFetchPreviousPageError: true,
                isFetchingNextPage: false,
                isFetchingPreviousPage: false,
                isLoading: false,
                isRefetchError: false,
                refetch: vi.fn(),
            });

        it('offersARetryInsteadOfTheLoadEarlierControl', async () => {
            const fetchPreviousPage = vi.fn();

            mockFailedPreviousPage(fetchPreviousPage);

            render(
                <LazyTaskExecutionChildren
                    lazy={{workflowExecutionId: 9}}
                    onExecutionClick={vi.fn()}
                    parentTaskExecution={loopTaskExecution}
                    selectedExecutionId=""
                />
            );

            expect(screen.getByText('Failed to load earlier task executions.')).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Show 50 earlier iterations'})).not.toBeInTheDocument();
            expect(screen.queryByText('Live updates failed.')).not.toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', {name: 'Retry loading earlier task executions'}));

            expect(fetchPreviousPage).toHaveBeenCalledTimes(1);
        });

        it('stopsSeekingAnEarlierFocusedIterationWhileThePreviousPageFetchFailed', () => {
            const fetchPreviousPage = vi.fn();

            mockFailedPreviousPage(fetchPreviousPage);

            const {rerender} = render(
                <LazyTaskExecutionChildren
                    defaultValue={['4', '4-iteration-30']}
                    lazy={{workflowExecutionId: 9}}
                    onExecutionClick={vi.fn()}
                    parentTaskExecution={loopTaskExecution}
                    selectedExecutionId=""
                />
            );

            rerender(
                <LazyTaskExecutionChildren
                    defaultValue={['4', '4-iteration-30']}
                    lazy={{workflowExecutionId: 9}}
                    onExecutionClick={vi.fn()}
                    parentTaskExecution={loopTaskExecution}
                    selectedExecutionId=""
                />
            );

            expect(fetchPreviousPage).not.toHaveBeenCalled();
        });
    });

    describe('on-error branches', () => {
        const mockOnErrorPage = (iterations: ReturnType<typeof createIteration>[]) =>
            hoisted.useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery.mockReturnValue({
                data: {
                    pages: [
                        TaskExecutionChildrenPageFromJSON({
                            children: [],
                            iterations,
                            number: 0,
                            numberOfElements: 2,
                            size: 50,
                            totalElements: 2,
                            totalPages: 1,
                        }),
                    ],
                },
                fetchNextPage: vi.fn(),
                fetchPreviousPage: vi.fn(),
                hasNextPage: false,
                hasPreviousPage: false,
                isFetchNextPageError: false,
                isFetchingNextPage: false,
                isLoading: false,
            });

        it('labelsTheTryAndErrorBranchesTheServerSends', () => {
            mockOnErrorPage([createIteration(0), createIteration(1)]);

            render(
                <LazyTaskExecutionChildren
                    lazy={{workflowExecutionId: 9}}
                    onExecutionClick={vi.fn()}
                    parentTaskExecution={onErrorTaskExecution}
                    selectedExecutionId=""
                />
            );

            expect(screen.getByText('Try branch')).toBeInTheDocument();
            expect(screen.getByText('Error branch')).toBeInTheDocument();
            expect(screen.queryByText(/iteration/)).not.toBeInTheDocument();
        });

        it('dropsAnEmptyTryBranchAndStillLabelsTheRemainingOneTheErrorBranch', () => {
            mockOnErrorPage([[], createIteration(1)]);

            render(
                <LazyTaskExecutionChildren
                    lazy={{workflowExecutionId: 9}}
                    onExecutionClick={vi.fn()}
                    parentTaskExecution={onErrorTaskExecution}
                    selectedExecutionId=""
                />
            );

            expect(screen.queryByText('Try branch')).not.toBeInTheDocument();
            expect(screen.getByText('Error branch')).toBeInTheDocument();
        });

        it('dropsAnEmptyErrorBranch', () => {
            mockOnErrorPage([createIteration(0), []]);

            render(
                <LazyTaskExecutionChildren
                    lazy={{workflowExecutionId: 9}}
                    onExecutionClick={vi.fn()}
                    parentTaskExecution={onErrorTaskExecution}
                    selectedExecutionId=""
                />
            );

            expect(screen.getByText('Try branch')).toBeInTheDocument();
            expect(screen.queryByText('Error branch')).not.toBeInTheDocument();
        });
    });
});
