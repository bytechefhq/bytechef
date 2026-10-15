import ConnectedUserMcpServerListItem from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-mcp-server-list/ConnectedUserMcpServerListItem';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {useConnectedUserMcpServersQuery} from '@/shared/middleware/graphql';

const ConnectedUserSheetPanelMcpServerList = ({
    connectedUserId,
    connectedUserIntegrationInstances,
}: {
    connectedUserId: number;
    connectedUserIntegrationInstances: ConnectedUserIntegrationInstance[];
}) => {
    const isTenantAdmin = useIsTenantAdmin();

    const {data, isLoading} = useConnectedUserMcpServersQuery(
        {
            connectedUserId: connectedUserId.toString(),
        },
        {enabled: isTenantAdmin}
    );

    if (isLoading) {
        return <div className="py-4 text-sm text-muted-foreground">Loading...</div>;
    }

    const mcpServers = data?.connectedUserMcpServers ?? [];

    return mcpServers.length > 0 ? (
        <div>
            {mcpServers.map((mcpServer) => (
                <ConnectedUserMcpServerListItem
                    connectedUserId={connectedUserId}
                    connectedUserIntegrationInstances={connectedUserIntegrationInstances}
                    key={mcpServer.id}
                    mcpServer={mcpServer}
                />
            ))}
        </div>
    ) : (
        <div className="py-4 text-sm">No MCP servers expose this user's integrations.</div>
    );
};

export default ConnectedUserSheetPanelMcpServerList;
