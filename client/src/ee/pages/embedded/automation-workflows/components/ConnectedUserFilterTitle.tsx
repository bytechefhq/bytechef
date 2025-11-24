import Badge from '@/components/Badge/Badge';
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

            <Badge label={pageTitle ?? 'All Users'} styleType="secondary-filled" weight="semibold" />
        </div>
    );
};

export default ConnectedUserFilterTitle;
