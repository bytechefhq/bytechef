import {ConnectedUserFromJSON} from '@/ee/shared/middleware/embedded/connected-user';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useGetConnectedUsersQuery} from '@/shared/queries/embedded/connectedUsers.queries';
import {UsersIcon} from 'lucide-react';
import {useSearchParams} from 'react-router-dom';

const ConnectedUsersLeftSidebarNav = () => {
    const [searchParams] = useSearchParams();
    const selectedUserId = searchParams.get('connectedUserId')
        ? parseInt(searchParams.get('connectedUserId')!)
        : undefined;

    // Fetch connected users
    const {
        data: connectedUsersPage,
        error,
        isLoading,
    } = useGetConnectedUsersQuery({
        pageNumber: 0,
    });

    // Process the data
    const connectedUsers =
        connectedUsersPage?.content?.map((connectedUser: object) => ConnectedUserFromJSON(connectedUser)) || [];

    return (
        <LeftSidebarNav
            body={
                <>
                    {isLoading && <div className="text-sm text-muted-foreground">Loading users...</div>}

                    {error && <div className="text-sm text-muted-foreground">Error loading users</div>}

                    {!isLoading && !error && connectedUsers.length === 0 ? (
                        <div className="text-sm text-muted-foreground">No connected users</div>
                    ) : !isLoading && !error ? (
                        connectedUsers.map((user) => (
                            <LeftSidebarNavItem
                                icon={<UsersIcon className="mr-2 size-4" />}
                                item={{
                                    current: selectedUserId === user.id,
                                    id: user.id,
                                    name: user.name || user.email || `User ${user.externalId}`,
                                }}
                                key={user.externalId}
                                toLink={`?connectedUserId=${user.id}`}
                            />
                        ))
                    ) : null}
                </>
            }
            title="Connected Users"
        />
    );
};

export default ConnectedUsersLeftSidebarNav;
