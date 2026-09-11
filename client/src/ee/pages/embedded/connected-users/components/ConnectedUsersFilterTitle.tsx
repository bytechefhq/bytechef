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
            <span className="text-sm font-semibold text-muted-foreground uppercase">Filter by:</span>

            {filterData.status && (
                <Badge label={`Status: ${filterData.status}`} styleType="primary-outline" weight="semibold" />
            )}
        </div>
    );
};

export default ConnectedUsersFilterTitle;
