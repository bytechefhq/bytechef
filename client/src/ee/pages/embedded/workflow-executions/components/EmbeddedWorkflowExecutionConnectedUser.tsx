import {useGetConnectedUserQuery} from '@/ee/shared/queries/embedded/connectedUsers.queries';

interface EmbeddedWorkflowExecutionConnectedUserProps {
    connectedUserId?: number;
}

const EmbeddedWorkflowExecutionConnectedUser = ({connectedUserId}: EmbeddedWorkflowExecutionConnectedUserProps) => {
    const {data: connectedUser} = useGetConnectedUserQuery(connectedUserId ?? 0, connectedUserId != null);

    if (connectedUserId == null) {
        return null;
    }

    return <>{connectedUser?.externalId ?? connectedUserId}</>;
};

export default EmbeddedWorkflowExecutionConnectedUser;
