import ConnectedUserMcpServerListItem from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-mcp-server-list/ConnectedUserMcpServerListItem';
import {useConnectedUserMcpServersQuery} from '@/shared/middleware/graphql';

const ConnectedUserSheetPanelMcpServerList = ({connectedUserId}: {connectedUserId: number}) => {
    const {data, isLoading} = useConnectedUserMcpServersQuery({
        connectedUserId: connectedUserId.toString(),
    });

    if (isLoading) {
        return <div className="py-4 text-sm text-muted-foreground">Loading...</div>;
    }

    const mcpServers = data?.connectedUserMcpServers ?? [];

    return mcpServers.length > 0 ? (
        <div className="divide-y">
            {mcpServers.map((mcpServer) => (
                <ConnectedUserMcpServerListItem key={mcpServer.id} mcpServer={mcpServer} />
            ))}
        </div>
    ) : (
        <div className="py-4 text-sm">No MCP servers expose this user's integrations.</div>
    );
};

export default ConnectedUserSheetPanelMcpServerList;
