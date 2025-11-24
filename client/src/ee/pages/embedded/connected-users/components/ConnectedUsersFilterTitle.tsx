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
            <span className="text-sm font-semibold uppercase text-muted-foreground">Filter by </span>

            {filterData.status && (
                <>
                    <span className="text-sm uppercase text-muted-foreground">status:</span>

                    <Badge label={filterData.status} styleType="secondary-filled" weight="semibold" />
                </>
            )}
        </div>
    );
};

export default ConnectedUsersFilterTitle;
