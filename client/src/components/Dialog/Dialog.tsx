import {
    Dialog as ShadcnDialog,
    DialogClose as ShadcnDialogClose,
    DialogContent as ShadcnDialogContent,
    DialogTrigger as ShadcnDialogTrigger,
} from '@/components/ui/dialog';
import {ComponentPropsWithRef, createContext, useCallback, useContext, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

const contentStyles =
    'flex max-h-[calc(100dvh-2rem)] w-full max-w-[calc(100%-2rem)] gap-0 rounded-lg border-0 bg-transparent p-0 shadow-xl sm:w-auto sm:max-w-[calc(100%-2rem)]';

const sidebarLayoutStyles =
    'lg:has-[[data-slot=dialog-sidebar]]:h-[648px] lg:has-[[data-slot=dialog-sidebar]]:w-[860px] lg:has-[[data-slot=dialog-sidebar]]:gap-2 lg:has-[[data-slot=dialog-sidebar]]:bg-surface-main lg:has-[[data-slot=dialog-sidebar]]:p-2';

interface DialogLayoutContextI {
    hasSidebar: boolean;
    registerSidebar: () => () => void;
}

const DialogLayoutContext = createContext<DialogLayoutContextI>({
    hasSidebar: false,
    registerSidebar: () => () => {},
});

const Dialog = (props: ComponentPropsWithRef<typeof ShadcnDialog>) => <ShadcnDialog {...props} />;

Dialog.displayName = 'Dialog';

type DialogContentPropsType = ComponentPropsWithRef<typeof ShadcnDialogContent>;

const DialogContent = ({children, className, ...props}: DialogContentPropsType) => {
    const [sidebarCount, setSidebarCount] = useState(0);

    const registerSidebar = useCallback(() => {
        setSidebarCount((previousSidebarCount) => previousSidebarCount + 1);

        return () => setSidebarCount((previousSidebarCount) => previousSidebarCount - 1);
    }, []);

    const layoutContextValue = useMemo(
        () => ({hasSidebar: sidebarCount > 0, registerSidebar}),
        [registerSidebar, sidebarCount]
    );

    return (
        <ShadcnDialogContent className={twMerge(contentStyles, sidebarLayoutStyles, className)} {...props}>
            <DialogLayoutContext value={layoutContextValue}>{children}</DialogLayoutContext>
        </ShadcnDialogContent>
    );
};

DialogContent.displayName = 'DialogContent';

const DialogClose = (props: ComponentPropsWithRef<typeof ShadcnDialogClose>) => <ShadcnDialogClose {...props} />;

DialogClose.displayName = 'DialogClose';

const DialogTrigger = (props: ComponentPropsWithRef<typeof ShadcnDialogTrigger>) => <ShadcnDialogTrigger {...props} />;

DialogTrigger.displayName = 'DialogTrigger';

const useDialogLayout = () => useContext(DialogLayoutContext);

export {Dialog, DialogClose, DialogContent, DialogTrigger, useDialogLayout};
export type {DialogContentPropsType as DialogContentProps, DialogLayoutContextI};
