import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Environment} from '@/shared/middleware/graphql';
import {UsersIcon} from 'lucide-react';

const ConnectedUsersLeftSidebarNav = ({
    connectedUserId,
    connectedUsers,
    environment,
}: {
    connectedUserId?: number;
    connectedUsers: ConnectedUser[];
    environment?: Environment;
}) => {
    return (
        <LeftSidebarNav
            body={
                connectedUsers ? (
                    connectedUsers.map((user) => (
                        <LeftSidebarNavItem
                            icon={<UsersIcon className="mr-2 size-4" />}
                            item={{
                                current: connectedUserId === user.id,
                                id: user.id,
                                name: user.name || user.email || `User ${user.externalId}`,
                            }}
                            key={user.id}
                            toLink={`?connectedUserId=${user.id ?? ''}&environment=${environment ?? ''}`}
                        />
                    ))
                ) : (
                    <span className="px-3 text-xs">No connected users.</span>
                )
            }
            title="Connected Users"
        />
    );
};

export default ConnectedUsersLeftSidebarNav;
