import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {UsersIcon} from 'lucide-react';

const ConnectedUsersLeftSidebarNav = ({
    connectedUserId,
    connectedUsers,
}: {
    connectedUserId?: number;
    connectedUsers: ConnectedUser[];
}) => {
    return (
        <LeftSidebarNav
            body={
                connectedUsers.length > 0 ? (
                    connectedUsers.map((user) => (
                        <LeftSidebarNavItem
                            icon={<UsersIcon className="mr-2 size-4" />}
                            item={{
                                current: connectedUserId === user.id,
                                id: user.id,
                                name: user.name || user.email || `User ${user.externalId}`,
                            }}
                            key={user.id}
                            toLink={`?connectedUserId=${user.id ?? ''}`}
                        />
                    ))
                ) : (
                    <span className="px-2 text-xs">No connected users.</span>
                )
            }
            title="Connected Users"
        />
    );
};

export default ConnectedUsersLeftSidebarNav;
