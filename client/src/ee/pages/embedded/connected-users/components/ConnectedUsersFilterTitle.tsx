import {Badge} from '@/components/ui/badge';
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

                    <Badge variant="secondary">
                        <span className="text-sm">{filterData.status}</span>
                    </Badge>
                </>
            )}
        </div>
    );
};

export default ConnectedUsersFilterTitle;
