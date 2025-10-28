import ConnectedUserSheetPanelIntegrationList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationList';
import ConnectedUserSheetPanelProfile from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelProfile';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';

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

            <div className="grow space-y-2">
                <div className="text-base font-semibold">Integrations</div>

                <div className="relative size-full overflow-scroll">
                    <div className="absolute inset-y-0 w-full">
                        {connectedUser.integrationInstances && (
                            <ConnectedUserSheetPanelIntegrationList
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
