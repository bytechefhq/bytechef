import Button from '@/components/Button/Button';
import ConnectedUserMcpServerListItem from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-mcp-server-list/ConnectedUserMcpServerListItem';
import ProvisionMcpServerDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-mcp-server-dialog/ProvisionMcpServerDialog';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import {useConnectedUserMcpServersQuery} from '@/shared/middleware/graphql';
import {PlusIcon} from 'lucide-react';
import {useState} from 'react';

const ConnectedUserSheetPanelMcpServerList = ({
    connectedUserId,
    connectedUserIntegrationInstances,
}: {
    connectedUserId: number;
    connectedUserIntegrationInstances: ConnectedUserIntegrationInstance[];
}) => {
    const [showProvisionDialog, setShowProvisionDialog] = useState(false);

    const canViewMcpServers = useHasEmbeddedScope('MCP_VIEW');

    const {data, isLoading} = useConnectedUserMcpServersQuery(
        {
            connectedUserId: connectedUserId.toString(),
        },
        {enabled: canViewMcpServers}
    );

    if (isLoading) {
        return <div className="py-4 text-sm text-muted-foreground">Loading...</div>;
    }

    const mcpServers = data?.connectedUserMcpServers ?? [];

    return (
        <div>
            <div className="flex items-center justify-between pb-2">
                <h3 className="text-sm font-semibold">MCP Servers</h3>

                <Button
                    icon={<PlusIcon />}
                    label="New MCP Server"
                    onClick={() => setShowProvisionDialog(true)}
                    size="sm"
                    variant="secondary"
                />
            </div>

            {mcpServers.length > 0 ? (
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
            )}

            {showProvisionDialog && (
                <ProvisionMcpServerDialog
                    connectedUserId={connectedUserId}
                    onClose={() => setShowProvisionDialog(false)}
                />
            )}
        </div>
    );
};

export default ConnectedUserSheetPanelMcpServerList;
