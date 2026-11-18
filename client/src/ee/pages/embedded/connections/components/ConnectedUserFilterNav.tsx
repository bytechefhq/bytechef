import ComboBox, {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {useGetConnectedUserQuery, useGetConnectedUsersQuery} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {LeftSidebarNav} from '@/shared/layout/LeftSidebarNav';
import {useMemo, useState} from 'react';
import {useDebounce} from 'use-debounce';

const ADMIN_CONNECTIONS_LABEL = 'Admin connections';

const LOADING_CONNECTED_USER_LABEL = 'Loading...';

const NO_CONNECTED_USER_VALUE = -1;

const SEARCH_DEBOUNCE_MILLISECONDS = 300;

interface ConnectedUserFilterNavProps {
    environmentId: number;
    onSelect: (connectedUserId?: number) => void;
    selectedConnectedUserId?: number;
}

export const getConnectedUserLabel = (connectedUser: ConnectedUser) => connectedUser.name || connectedUser.externalId;

const ConnectedUserFilterNav = ({environmentId, onSelect, selectedConnectedUserId}: ConnectedUserFilterNavProps) => {
    const [search, setSearch] = useState('');

    const [debouncedSearch] = useDebounce(search, SEARCH_DEBOUNCE_MILLISECONDS);

    const {data: connectedUsersPage} = useGetConnectedUsersQuery({
        environmentId,
        pageNumber: 0,
        search: debouncedSearch || undefined,
    });

    const {data: selectedConnectedUser} = useGetConnectedUserQuery(
        selectedConnectedUserId ?? 0,
        selectedConnectedUserId !== undefined
    );

    const items = useMemo<ComboBoxItemType[]>(() => {
        const connectedUsers = [...((connectedUsersPage?.content ?? []) as ConnectedUser[])];

        if (
            selectedConnectedUser &&
            !connectedUsers.some((connectedUser) => connectedUser.id === selectedConnectedUser.id)
        ) {
            connectedUsers.unshift(selectedConnectedUser);
        }

        const connectedUserItems = connectedUsers
            .filter((connectedUser) => connectedUser.id != null)
            .map((connectedUser) => ({
                group: 'Connected Users',
                label: getConnectedUserLabel(connectedUser),
                value: connectedUser.id!,
            }));

        if (selectedConnectedUserId === undefined) {
            return connectedUserItems;
        }

        if (!connectedUserItems.some((connectedUserItem) => connectedUserItem.value === selectedConnectedUserId)) {
            connectedUserItems.unshift({
                group: 'Connected Users',
                label: LOADING_CONNECTED_USER_LABEL,
                value: selectedConnectedUserId,
            });
        }

        return [{label: ADMIN_CONNECTIONS_LABEL, value: NO_CONNECTED_USER_VALUE}, ...connectedUserItems];
    }, [connectedUsersPage, selectedConnectedUser, selectedConnectedUserId]);

    const selectedLabel = useMemo(
        () => items.find((item) => item.value === selectedConnectedUserId)?.label ?? ADMIN_CONNECTIONS_LABEL,
        [items, selectedConnectedUserId]
    );

    return (
        <LeftSidebarNav
            body={
                <div className="px-2">
                    <ComboBox
                        aria-label={`Connected user: ${selectedLabel}`}
                        emptyMessage="No connected users found."
                        items={items}
                        onChange={(item) => onSelect(item?.value === NO_CONNECTED_USER_VALUE ? undefined : item?.value)}
                        onSearchChange={setSearch}
                        placeholder={ADMIN_CONNECTIONS_LABEL}
                        value={selectedConnectedUserId}
                    />
                </div>
            }
            title="Connected Users"
        />
    );
};

export default ConnectedUserFilterNav;
