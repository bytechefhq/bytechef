import {ConnectedUser, ConnectedUserFromJSON} from '@/ee/shared/middleware/embedded/connected-user';
import {useGetConnectedUsersQuery} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

/**
 * The connected users an owner control offers, for the current environment.
 *
 * <p>
 * {@code Page.content} is typed as {@code Array<object>} by the generated client, so the rows have to go back through
 * {@code ConnectedUserFromJSON} to become usable. Shared rather than repeated, because both console pages need the
 * same list and the conversion is easy to forget.
 */
const useEmbeddedConnectedUsers = (): {connectedUsers: ConnectedUser[]; isLoading: boolean} => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data: page, isLoading} = useGetConnectedUsersQuery({environmentId: currentEnvironmentId});

    const connectedUsers = useMemo(
        () => (page?.content ?? []).map((content) => ConnectedUserFromJSON(content)),
        [page?.content]
    );

    return {connectedUsers, isLoading};
};

export default useEmbeddedConnectedUsers;
