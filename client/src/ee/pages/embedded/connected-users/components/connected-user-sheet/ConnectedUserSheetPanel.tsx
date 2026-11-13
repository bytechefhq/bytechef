import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import ConnectedUserSheetPanelConnectionList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelConnectionList';
import ConnectedUserSheetPanelExternalInvocationList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelExternalInvocationList';
import ConnectedUserSheetPanelIntegrationList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationList';
import ConnectedUserSheetPanelMcpServerList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelMcpServerList';
import ConnectedUserSheetPanelProfile from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelProfile';
import ConnectedUserSheetPanelWorkflowExecutionList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelWorkflowExecutionList';
import ConnectedUserSheetPanelWorkflowList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelWorkflowList';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';

interface ConnectedUserSheetPanelProps {
    connectedUser: ConnectedUser;
}

const ConnectedUserSheetPanel = ({connectedUser}: ConnectedUserSheetPanelProps) => {
    return (
        <div className="flex min-h-full w-full shrink-0 flex-col space-y-10 rounded-lg bg-background p-3">
            <div className="w-full space-y-2">
                <div className="text-base font-semibold">Profile</div>

                <ConnectedUserSheetPanelProfile connectedUser={connectedUser} />
            </div>

            <Tabs className="w-full" defaultValue="integrations">
                <TabsList>
                    <TabsTrigger value="integrations">Integrations</TabsTrigger>

                    <TabsTrigger value="workflows">Automations</TabsTrigger>

                    <TabsTrigger value="mcp-servers">MCP Servers</TabsTrigger>

                    <TabsTrigger value="connections">Connections</TabsTrigger>

                    <TabsTrigger value="workflow-executions">Workflow Executions</TabsTrigger>

                    <TabsTrigger value="external-invocations">External Invocations</TabsTrigger>
                </TabsList>

                <TabsContent value="integrations">
                    {connectedUser.integrationInstances && (
                        <ConnectedUserSheetPanelIntegrationList
                            connectedUser={connectedUser}
                            connectedUserId={connectedUser.id!}
                            connectedUserIntegrationInstances={connectedUser.integrationInstances}
                        />
                    )}
                </TabsContent>

                <TabsContent value="workflows">
                    {connectedUser.id != null && (
                        <ConnectedUserSheetPanelWorkflowList
                            connectedUserEmail={connectedUser.email}
                            connectedUserId={connectedUser.id}
                            connectedUserName={connectedUser.name}
                        />
                    )}
                </TabsContent>

                <TabsContent value="mcp-servers">
                    {connectedUser.id != null && (
                        <ConnectedUserSheetPanelMcpServerList
                            connectedUserId={connectedUser.id}
                            connectedUserIntegrationInstances={connectedUser.integrationInstances ?? []}
                        />
                    )}
                </TabsContent>

                <TabsContent value="connections">
                    {connectedUser.id != null && (
                        <ConnectedUserSheetPanelConnectionList
                            connectedUserId={connectedUser.id}
                            connectedUserIntegrationInstances={connectedUser.integrationInstances ?? []}
                        />
                    )}
                </TabsContent>

                <TabsContent value="workflow-executions">
                    {connectedUser.id != null && (
                        <ConnectedUserSheetPanelWorkflowExecutionList connectedUserId={connectedUser.id} />
                    )}
                </TabsContent>

                <TabsContent value="external-invocations">
                    {connectedUser.id != null && (
                        <ConnectedUserSheetPanelExternalInvocationList connectedUserId={connectedUser.id} />
                    )}
                </TabsContent>
            </Tabs>
        </div>
    );
};

export default ConnectedUserSheetPanel;
