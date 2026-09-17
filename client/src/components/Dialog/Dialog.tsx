import {
    Dialog as ShadcnDialog,
    DialogClose as ShadcnDialogClose,
    DialogContent as ShadcnDialogContent,
    DialogTrigger as ShadcnDialogTrigger,
} from '@/components/ui/dialog';
import {ComponentPropsWithRef, createContext, useContext, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

const contentStyles =
    'flex max-h-[calc(100dvh-2rem)] w-full max-w-[calc(100%-2rem)] gap-0 rounded-lg border-0 bg-transparent p-0 shadow-xl sm:w-auto sm:max-w-[calc(100%-2rem)]';

const sidebarLayoutStyles = 'lg:h-[648px] lg:w-[860px] lg:gap-2 lg:bg-surface-main lg:p-2';

interface DialogLayoutContextI {
    hasSidebar: boolean;
}

const DialogLayoutContext = createContext<DialogLayoutContextI>({hasSidebar: false});

const Dialog = (props: ComponentPropsWithRef<typeof ShadcnDialog>) => <ShadcnDialog {...props} />;

Dialog.displayName = 'Dialog';

interface DialogContentProps extends ComponentPropsWithRef<typeof ShadcnDialogContent> {
    hasSidebar?: boolean;
}

const DialogContent = ({children, className, hasSidebar = false, ...props}: DialogContentProps) => {
    const layoutContextValue = useMemo(() => ({hasSidebar}), [hasSidebar]);

    return (
        <ShadcnDialogContent
            className={twMerge(contentStyles, hasSidebar && sidebarLayoutStyles, className)}
            {...props}
        >
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
export type {DialogContentProps, DialogLayoutContextI};
