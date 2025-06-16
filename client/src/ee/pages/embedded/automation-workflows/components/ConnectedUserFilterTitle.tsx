import {Badge} from '@/components/ui/badge';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {Environment} from '@/shared/middleware/graphql';

const ConnectedUserFilterTitle = ({
    connectedUsers,
    environment,
    filterData,
}: {
    environment?: Environment;
    filterData: {id?: number};
    connectedUsers?: ConnectedUser[];
}) => {
    const pageTitle = connectedUsers?.find((connectedUser) => connectedUser.id === filterData.id)?.externalId;

    return (
        <div className="space-x-1">
            <span className="text-sm uppercase text-muted-foreground">Filter by environment:</span>

            <Badge variant="secondary">
                <span className="text-sm">{environment ?? 'All Environments'}</span>
            </Badge>

            <span className="text-sm uppercase text-muted-foreground">Connected User:</span>

            <Badge variant="secondary">
                <span className="text-sm">{pageTitle ?? 'All Users'}</span>
            </Badge>
        </div>
    );
};

export default ConnectedUserFilterTitle;
