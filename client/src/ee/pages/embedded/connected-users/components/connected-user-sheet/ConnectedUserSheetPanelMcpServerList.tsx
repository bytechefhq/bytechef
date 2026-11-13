import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectedUserSheetEmptyState from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetEmptyState';
import ConnectedUserSheetPanelListSkeleton from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelListSkeleton';
import ConnectedUserMcpServerListItem from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-mcp-server-list/ConnectedUserMcpServerListItem';
import ProvisionMcpServerDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-mcp-server-dialog/ProvisionMcpServerDialog';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {
    useConnectedUserMcpServersQuery,
    useProvisionableConnectedUserMcpServersQuery,
} from '@/shared/middleware/graphql';
import {PlusIcon, ServerIcon} from 'lucide-react';
import {useState} from 'react';

const ConnectedUserSheetPanelMcpServerList = ({
    connectedUserId,
    connectedUserIntegrationInstances,
}: {
    connectedUserId: number;
    connectedUserIntegrationInstances: ConnectedUserIntegrationInstance[];
}) => {
    const [showProvisionDialog, setShowProvisionDialog] = useState(false);

    const {data, isLoading} = useConnectedUserMcpServersQuery({
        connectedUserId: connectedUserId.toString(),
    });

    const {data: provisionableData} = useProvisionableConnectedUserMcpServersQuery({
        connectedUserId: connectedUserId.toString(),
    });

    if (isLoading) {
        return <ConnectedUserSheetPanelListSkeleton label="Loading MCP servers" />;
    }

    const mcpServers = data?.connectedUserMcpServers ?? [];

    const provisionableMcpServers = provisionableData?.provisionableConnectedUserMcpServers;

    const everyMcpServerProvisioned =
        !!provisionableMcpServers && provisionableMcpServers.every((mcpServer) => mcpServer.provisioned);

    return (
        <div>
            {mcpServers.length > 0 ? (
                <>
                    <div className="flex items-center justify-end pb-2">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <span className="block">
                                    <Button
                                        disabled={everyMcpServerProvisioned}
                                        icon={<PlusIcon />}
                                        label="Provision MCP Server"
                                        onClick={() => setShowProvisionDialog(true)}
                                        size="sm"
                                        variant="secondary"
                                    />
                                </span>
                            </TooltipTrigger>

                            {everyMcpServerProvisioned && (
                                <TooltipContent>Every MCP server is already provisioned for this user.</TooltipContent>
                            )}
                        </Tooltip>
                    </div>

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
                </>
            ) : (
                <ConnectedUserSheetEmptyState
                    description="An MCP server lets AI assistants, like Claude or Cursor, call tools on this user's integrations."
                    icon={<ServerIcon />}
                    nothingToProvision={provisionableMcpServers?.length === 0}
                    nothingToProvisionDescription="No MCP servers are defined yet. Create one on the MCP Servers page and pick the tools it exposes, then provision it for this user."
                    onProvision={() => setShowProvisionDialog(true)}
                    pageLink={{label: 'Go to MCP Servers', to: '/embedded/mcp-servers'}}
                    provisionLabel="Provision MCP Server"
                    title="No MCP servers yet"
                />
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
