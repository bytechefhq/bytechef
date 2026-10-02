import {
    Dialog as ShadcnDialog,
    DialogClose as ShadcnDialogClose,
    DialogContent as ShadcnDialogContent,
    DialogTitle as ShadcnDialogTitle,
    DialogTrigger as ShadcnDialogTrigger,
} from '@/components/ui/dialog';
import {ComponentPropsWithRef, createContext, useContext, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

const contentStyles =
    'flex max-h-[calc(100dvh-2rem)] w-full max-w-[calc(100%-2rem)] gap-0 rounded-lg border-0 bg-transparent p-0 shadow-xl sm:max-w-[calc(100%-2rem)]';

const sidebarLayoutStyles = 'lg:h-[648px] lg:w-[860px] lg:gap-2 lg:bg-surface-main lg:p-2';

const contentSizeStyles = {
    lg: 'sm:w-[800px]',
    md: 'sm:w-[640px]',
    sm: 'sm:w-[512px]',
    xl: 'sm:w-[1000px]',
} as const;

type DialogContentSizeType = keyof typeof contentSizeStyles;

interface DialogLayoutContextI {
    hasSidebar: boolean;
}

const DialogLayoutContext = createContext<DialogLayoutContextI>({hasSidebar: false});

const Dialog = (props: ComponentPropsWithRef<typeof ShadcnDialog>) => <ShadcnDialog {...props} />;

Dialog.displayName = 'Dialog';

interface DialogContentProps extends ComponentPropsWithRef<typeof ShadcnDialogContent> {
    hasSidebar?: boolean;
    size?: DialogContentSizeType;
}

const DialogContent = ({children, className, hasSidebar = false, size = 'sm', ...props}: DialogContentProps) => {
    const layoutContextValue = useMemo(() => ({hasSidebar}), [hasSidebar]);

    return (
        <ShadcnDialogContent
            className={twMerge(contentStyles, contentSizeStyles[size], hasSidebar && sidebarLayoutStyles, className)}
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

/**
 * Radix requires every DialogContent to carry a title. DialogHeader is the usual way to give one, but it also renders
 * visible chrome, so full-bleed surfaces that supply their own toolbar pair this with VisuallyHidden instead.
 */
const DialogTitle = (props: ComponentPropsWithRef<typeof ShadcnDialogTitle>) => <ShadcnDialogTitle {...props} />;

DialogTitle.displayName = 'DialogTitle';

const useDialogLayout = () => useContext(DialogLayoutContext);

export {Dialog, DialogClose, DialogContent, DialogTitle, DialogTrigger, useDialogLayout};
export type {DialogContentProps, DialogContentSizeType, DialogLayoutContextI};
