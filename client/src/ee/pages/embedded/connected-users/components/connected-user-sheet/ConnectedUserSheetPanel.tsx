import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import ConnectedUserSheetPanelIntegrationList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationList';
import ConnectedUserSheetPanelProfile from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelProfile';
import ConnectedUserSheetPanelWorkflowList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelWorkflowList';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';

// Underline tab styling that matches the workflow node details panel — brand-colored active
// bottom border + text, no pill background — so tabs read consistently across the app.
const tabsTriggerClassName =
    'grow rounded-none border-0 border-b border-border py-2.5 text-content-neutral-secondary shadow-none hover:border-stroke-brand-primary hover:text-content-brand-primary data-[state=active]:border-stroke-brand-primary data-[state=active]:bg-transparent data-[state=active]:text-content-brand-primary data-[state=active]:shadow-none';

interface ConnectedUserSheetPanelProps {
    connectedUser: ConnectedUser;
}

const ConnectedUserSheetPanel = ({connectedUser}: ConnectedUserSheetPanelProps) => {
    return (
        <div className="flex size-full flex-col space-y-10 pt-4">
            <div className="w-full space-y-2">
                <div className="text-base font-semibold">Profile</div>

                <ConnectedUserSheetPanelProfile connectedUser={connectedUser} />
            </div>

            <Tabs className="w-full" defaultValue="integrations">
                <TabsList className="h-auto w-full justify-start rounded-none bg-transparent p-0">
                    <TabsTrigger className={tabsTriggerClassName} value="integrations">
                        Integrations
                    </TabsTrigger>

                    <TabsTrigger className={tabsTriggerClassName} value="workflows">
                        Workflows
                    </TabsTrigger>
                </TabsList>

                <TabsContent value="integrations">
                    {connectedUser.integrationInstances && (
                        <ConnectedUserSheetPanelIntegrationList
                            connectedUserId={connectedUser.id!}
                            connectedUserIntegrationInstances={connectedUser.integrationInstances}
                        />
                    )}
                </TabsContent>

                <TabsContent value="workflows">
                    {connectedUser.id != null && (
                        <ConnectedUserSheetPanelWorkflowList connectedUserId={connectedUser.id} />
                    )}
                </TabsContent>
            </Tabs>
        </div>
    );
};

export default ConnectedUserSheetPanel;
