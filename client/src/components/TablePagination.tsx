import {
    Pagination,
    PaginationContent,
    PaginationItem,
    PaginationLink,
    PaginationNext,
    PaginationPrevious,
} from '@/components/ui/pagination';
import {useCallback} from 'react';

const visiblePageButtonCount = 6;

interface TablePaginationProps {
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
    onClick: (pageNumber: number) => void;
}

const TablePagination = ({onClick, pageNumber, pageSize, totalElements, totalPages}: TablePaginationProps) => {
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
            <PaginationItem key={`pagination_item_${pageIndexToMap}`}>
                <PaginationLink
                    href="#"
                    isActive={pageIndexToMap === pageNumber}
                    onClick={() => onClick(pageIndexToMap)}
                >
                    {pageIndexToMap + 1}
                </PaginationLink>
            </PaginationItem>
        ));
    }, [pageNumber, totalPages, onClick]);

    return (
        <Pagination className="w-full">
            <PaginationContent className="w-full">
                <div className="flex-1">
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

                {totalPages > 1 && (
                    <>
                        {pageNumber > 0 && (
                            <PaginationItem>
                                <PaginationPrevious href="#" onClick={() => onClick(pageNumber - 1)} />
                            </PaginationItem>
                        )}

                        {renderPageLinks()}

                        {pageNumber < totalPages - 1 && (
                            <PaginationItem>
                                <PaginationNext
                                    href="#"
                                    onClick={() => {
                                        if (pageNumber < totalPages - 1) {
                                            onClick(pageNumber + 1);
                                        }
                                    }}
                                />
                            </PaginationItem>
                        )}
                    </>
                )}
            </PaginationContent>
        </Pagination>
    );
};

export default TablePagination;
