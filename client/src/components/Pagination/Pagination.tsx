import {ChevronLeftIcon, ChevronRightIcon} from '@heroicons/react/24/outline';
import {useCallback} from 'react';

const Pagination = ({
    pageNumber,
    pageSize,
    totalElements,
    totalPages,
    onClick,
}: {
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
    onClick: (pageNumber: number) => void;
}): JSX.Element => {
    const renderPageLinks = useCallback(() => {
        if (totalPages === 0) {
            return null;
        }

        const visiblePageButtonCount = 6;

        let numberOfButtons =
            totalPages < visiblePageButtonCount
                ? totalPages
                : visiblePageButtonCount;
        const pageIndices = [pageNumber];
        numberOfButtons--;

        [...Array(numberOfButtons)].forEach((_item, itemIndex) => {
            const pageNumberBefore = pageIndices[0] - 1;
            const pageNumberAfter = pageIndices[pageIndices.length - 1] + 1;
            if (
                pageNumberBefore >= 0 &&
                (itemIndex < numberOfButtons / 2 ||
                    pageNumberAfter > totalPages - 1)
            ) {
                pageIndices.unshift(pageNumberBefore);
            } else {
                pageIndices.push(pageNumberAfter);
            }
        });

        return pageIndices.map((pageIndexToMap) => (
            <button
                aria-current="page"
                className={
                    pageIndexToMap === pageNumber
                        ? 'relative z-10 inline-flex items-center bg-blue-600 px-4 py-2 text-sm font-semibold text-white focus:z-20 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600'
                        : 'relative inline-flex items-center px-4 py-2 text-sm font-semibold text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus:z-20 focus:outline-offset-0'
                }
                key={pageIndexToMap}
                onClick={() => onClick(pageIndexToMap)}
            >
                {pageIndexToMap + 1}
            </button>
        ));
    }, [pageNumber, totalPages, onClick]);

    return (
        <div className="flex w-full items-center justify-between border-t border-gray-200 bg-white py-3">
            <div className="flex flex-1 justify-between sm:hidden">
                <button
                    className="relative inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                    disabled={pageNumber === 0}
                    onClick={() => {
                        if (pageNumber > 0) {
                            onClick(pageNumber - 1);
                        }
                    }}
                >
                    Previous
                </button>

                <button
                    className="relative ml-3 inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                    disabled={pageNumber === totalPages - 1}
                    onClick={() => {
                        if (pageNumber < totalPages - 1) {
                            onClick(pageNumber + 1);
                        }
                    }}
                >
                    Next
                </button>
            </div>

            <div className="hidden sm:flex sm:flex-1 sm:items-center sm:justify-between">
                <div>
                    <p className="text-sm text-gray-700">
                        Showing{' '}
                        <span className="font-medium">
                            {pageNumber * pageSize + 1}
                        </span>{' '}
                        to{' '}
                        <span className="font-medium">
                            {Math.min(
                                totalElements,
                                (pageNumber + 1) * pageSize
                            )}
                        </span>{' '}
                        of <span className="font-medium">{totalElements}</span>{' '}
                        results
                    </p>
                </div>

                <div>
                    <nav
                        className="isolate inline-flex -space-x-px rounded-md"
                        aria-label="Pagination"
                    >
                        <button
                            className="relative inline-flex items-center rounded-l-md p-2 text-gray-400 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus:z-20 focus:outline-offset-0"
                            disabled={pageNumber === 0}
                            onClick={() => {
                                if (pageNumber > 0) {
                                    onClick(pageNumber - 1);
                                }
                            }}
                        >
                            <span className="sr-only">Previous</span>

                            <ChevronLeftIcon
                                className="h-5 w-5"
                                aria-hidden="true"
                            />
                        </button>

                        {renderPageLinks()}

                        <button
                            className="relative inline-flex items-center rounded-r-md p-2 text-gray-400 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus:z-20 focus:outline-offset-0"
                            disabled={pageNumber === totalPages - 1}
                            onClick={() => {
                                if (pageNumber < totalPages - 1) {
                                    onClick(pageNumber + 1);
                                }
                            }}
                        >
                            <span className="sr-only">Next</span>

                            <ChevronRightIcon
                                className="h-5 w-5"
                                aria-hidden="true"
                            />
                        </button>
                    </nav>
                </div>
            </div>
        </div>
    );
};

export default Pagination;
