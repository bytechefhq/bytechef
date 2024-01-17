import {ChevronLeftIcon, ChevronRightIcon} from '@radix-ui/react-icons';
import {useCallback} from 'react';
import {twMerge} from 'tailwind-merge';

const visiblePageButtonCount = 6;

interface ButtonProps {
    pageIndexToMap: number;
    pageNumber: number;
    onClick: (pageNumber: number) => void;
}

const Button = ({onClick, pageIndexToMap, pageNumber}: ButtonProps) => (
    <button
        aria-current="page"
        className={twMerge([
            'relative inline-flex items-center px-4 py-2 text-sm font-semibold',
            pageIndexToMap === pageNumber
                ? 'z-10 bg-blue-600 text-white focus:z-20 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600'
                : 'ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus:z-20 focus:outline-offset-0',
        ])}
        key={pageIndexToMap}
        onClick={() => onClick(pageIndexToMap)}
    >
        {pageIndexToMap + 1}
    </button>
);

interface PaginationProps {
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
    onClick: (pageNumber: number) => void;
}

const Pagination = ({onClick, pageNumber, pageSize, totalElements, totalPages}: PaginationProps) => {
    const renderPageLinks = useCallback(() => {
        if (totalPages === 0) {
            return null;
        }

        let numberOfButtons = totalPages < visiblePageButtonCount ? totalPages : visiblePageButtonCount;

        const pageIndices = [pageNumber];
        numberOfButtons--;

        for (let itemIndex = 0; itemIndex < numberOfButtons; itemIndex++) {
            const pageNumberBefore = pageIndices[0] - 1;
            const pageNumberAfter = pageIndices[pageIndices.length - 1] + 1;

            if (pageNumberBefore >= 0 && (itemIndex < numberOfButtons / 2 || pageNumberAfter > totalPages - 1)) {
                pageIndices.unshift(pageNumberBefore);
            } else {
                pageIndices.push(pageNumberAfter);
            }
        }

        return pageIndices.map((pageIndexToMap) => (
            <Button
                key={`paginator_button_${pageIndexToMap}`}
                onClick={onClick}
                pageIndexToMap={pageIndexToMap}
                pageNumber={pageNumber}
            />
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
                        Showing
                        <span className="px-2 font-medium">{pageNumber * pageSize + 1}</span>
                        to
                        <span className="px-2 font-medium">{Math.min(totalElements, (pageNumber + 1) * pageSize)}</span>
                        of
                        <span className="px-2 font-medium">{totalElements}</span>
                        results
                    </p>
                </div>

                <div>
                    <nav aria-label="Pagination" className="isolate inline-flex -space-x-px rounded-md">
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

                            <ChevronLeftIcon aria-hidden="true" className="size-5" />
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

                            <ChevronRightIcon aria-hidden="true" className="size-5" />
                        </button>
                    </nav>
                </div>
            </div>
        </div>
    );
};

export default Pagination;
