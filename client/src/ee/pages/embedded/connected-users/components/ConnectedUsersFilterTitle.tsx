import Badge from '@/components/Badge/Badge';
const ConnectedUsersFilterTitle = ({
    filterData,
}: {
    filterData: {
        status?: string;
    };
}) => {
    return (
        <div className="space-x-1">
            <span className="text-sm font-semibold text-muted-foreground uppercase">Filter by </span>

            {filterData.status && (
                <>
                    <span className="text-sm text-muted-foreground uppercase">status:</span>

                    <Badge label={filterData.status} styleType="secondary-filled" weight="semibold" />
                </>
            )}
        </div>
    );
};

export default ConnectedUsersFilterTitle;
