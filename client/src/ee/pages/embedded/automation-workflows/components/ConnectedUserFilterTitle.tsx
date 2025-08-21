import {Badge} from '@/components/ui/badge';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';

const ConnectedUserFilterTitle = ({
    connectedUsers,
    filterData,
}: {
    filterData: {id?: number};
    connectedUsers?: ConnectedUser[];
}) => {
    const pageTitle = connectedUsers?.find((connectedUser) => connectedUser.id === filterData.id)?.externalId;

    return (
        <div className="space-x-1">
            <span className="text-sm font-semibold uppercase text-muted-foreground">Filter by </span>

            <span className="text-sm uppercase text-muted-foreground">Connected User:</span>

            <Badge variant="secondary">
                <span className="text-sm">{pageTitle ?? 'All Users'}</span>
            </Badge>
        </div>
    );
};

export default ConnectedUserFilterTitle;
