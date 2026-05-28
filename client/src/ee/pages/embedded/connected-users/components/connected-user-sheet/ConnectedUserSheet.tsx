import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import ConnectedUserSheetActions from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetActions';
import ConnectedUserSheetPanel from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanel';
import ConnectedUserSheetTitle from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetTitle';
import useConnectedUserSheetStore from '@/ee/pages/embedded/connected-users/stores/useConnectedUserSheetStore';
import {useGetConnectedUserQuery} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {VisuallyHidden} from 'radix-ui';
import {useShallow} from 'zustand/react/shallow';

const ConnectedUserSheet = () => {
    const {connectedUserId, connectedUserSheetOpen, setConnectedUserSheetOpen} = useConnectedUserSheetStore(
        useShallow((state) => ({
            connectedUserId: state.connectedUserId,
            connectedUserSheetOpen: state.connectedUserSheetOpen,
            setConnectedUserSheetOpen: state.setConnectedUserSheetOpen,
        }))
    );

    const {data: connectedUser, isLoading: connectedUserLoading} = useGetConnectedUserQuery(
        connectedUserId,
        connectedUserSheetOpen
    );

    return (
        <Sheet onOpenChange={() => setConnectedUserSheetOpen(!connectedUserSheetOpen)} open={connectedUserSheetOpen}>
            <VisuallyHidden.Root>
                <SheetTitle>{connectedUser?.name ?? connectedUser?.externalId ?? 'Connected user'}</SheetTitle>
            </VisuallyHidden.Root>

            <SheetContent
                className="top-3 right-4 bottom-4 flex h-auto w-11/12 flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-(--breakpoint-md)"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                    <div className="flex flex-1 items-center">
                        {connectedUser && <ConnectedUserSheetTitle connectedUser={connectedUser} />}
                    </div>

                    <div className="flex items-center gap-x-1">
                        {connectedUser && <ConnectedUserSheetActions connectedUser={connectedUser} />}

                        <SheetCloseButton />
                    </div>
                </header>

                <div className="flex min-h-0 flex-1 flex-col overflow-y-auto p-3">
                    {connectedUserLoading ? (
                        <span>Loading...</span>
                    ) : (
                        connectedUser && <ConnectedUserSheetPanel connectedUser={connectedUser} />
                    )}
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default ConnectedUserSheet;
