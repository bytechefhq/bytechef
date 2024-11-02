import ConnectedUserSheetPanelIntegrations from '@/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrations';
import ConnectedUserSheetPanelProfile from '@/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelProfile';
import {ConnectedUser} from '@/shared/middleware/embedded/connected-user';

interface ConnectedUserSheetPanelProps {
    connectedUser: ConnectedUser;
}

const ConnectedUserSheetPanel = ({connectedUser}: ConnectedUserSheetPanelProps) => {
    return (
        <div className="flex w-full flex-col gap-4 px-2 pb-4">
            <div className="flex w-full flex-col space-x-4">
                <div className="w-full space-y-10">
                    <div className="w-full space-y-2 px-2">
                        <div className="text-base font-semibold">Profile</div>

                        <ConnectedUserSheetPanelProfile connectedUser={connectedUser} />
                    </div>

                    <div className="w-full space-y-2">
                        <div className="px-2 text-base font-semibold">Integrations</div>

                        {connectedUser.integrationInstances && (
                            <ConnectedUserSheetPanelIntegrations
                                connectedUserId={connectedUser.id!}
                                connectedUserIntegrationInstances={connectedUser.integrationInstances}
                            />
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ConnectedUserSheetPanel;
