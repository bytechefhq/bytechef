import {Sheet, SheetCloseButton, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import ConnectedUserSheetPanel from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanel';
import ConnectedUserSheetTitle from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetTitle';
import useConnectedUserSheetStore from '@/ee/pages/embedded/connected-users/stores/useConnectedUserSheetStore';
import {useGetConnectedUserQuery} from '@/ee/shared/queries/embedded/connectedUsers.queries';

const ConnectedUserSheet = () => {
    const {connectedUserId, connectedUserSheetOpen, setConnectedUserSheetOpen} = useConnectedUserSheetStore();

    const {data: connectedUser, isLoading: connectedUserLoading} = useGetConnectedUserQuery(
        connectedUserId,
        connectedUserSheetOpen
    );

    return (
        <Sheet onOpenChange={() => setConnectedUserSheetOpen(!connectedUserSheetOpen)} open={connectedUserSheetOpen}>
            <SheetContent className="flex w-11/12 flex-col gap-0 p-4 sm:max-w-screen-md">
                <SheetHeader className="flex flex-row items-center justify-between space-y-0">
                    <SheetTitle className="flex justify-between">
                        {connectedUser && <ConnectedUserSheetTitle connectedUser={connectedUser} />}
                    </SheetTitle>

                    <SheetCloseButton />
                </SheetHeader>

                {connectedUserLoading ? (
                    <span>Loading...</span>
                ) : (
                    connectedUser && <ConnectedUserSheetPanel connectedUser={connectedUser} />
                )}
            </SheetContent>
        </Sheet>
    );
};

export default ConnectedUserSheet;
