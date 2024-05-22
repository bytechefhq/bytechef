import {Sheet, SheetContent} from '@/components/ui/sheet';
import ConnectedUserSheetPanel from '@/pages/embedded/connected-users/components/ConnectedUserSheetPanel';
import useConnectedUserSheetStore from '@/pages/embedded/connected-users/stores/useConnectedUserSheetStore';
import {useGetConnectedUserQuery} from '@/shared/queries/embedded/connectedUsers.queries';

const ConnectedUserSheet = () => {
    const {connectedUserId, connectedUserSheetOpen, setConnectedUserSheetOpen} = useConnectedUserSheetStore();

    const {data: connectedUser, isLoading: connectedUserLoading} = useGetConnectedUserQuery(
        connectedUserId,
        connectedUserSheetOpen
    );

    return (
        <Sheet onOpenChange={() => setConnectedUserSheetOpen(!connectedUserSheetOpen)} open={connectedUserSheetOpen}>
            <SheetContent className="flex w-11/12 gap-0 p-0 sm:max-w-screen-sm">
                {connectedUserLoading && <span>Loading...</span>}

                {connectedUser && <ConnectedUserSheetPanel connectedUser={connectedUser} />}
            </SheetContent>
        </Sheet>
    );
};

export default ConnectedUserSheet;
