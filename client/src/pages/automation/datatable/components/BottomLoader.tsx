interface BottomLoaderProps {
    isFetchingNextPage: boolean;
    rowCount: number;
}

const BottomLoader = ({isFetchingNextPage, rowCount}: BottomLoaderProps) => {
    return (
        <div className="flex w-full items-center justify-start gap-3">
            <span className="text-xs text-muted-foreground">Total rows: {rowCount}</span>

            {isFetchingNextPage && <span className="text-xs text-muted-foreground">Loading…</span>}
        </div>
    );
};

export default BottomLoader;
