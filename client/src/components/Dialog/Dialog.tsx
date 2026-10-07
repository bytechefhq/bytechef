import {Dialog as DialogPrimitive} from 'radix-ui';
import {ComponentPropsWithRef, createContext, useContext, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

const overlayStyles =
    'fixed inset-0 z-50 bg-black/50 data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:animate-in data-[state=open]:fade-in-0';

const contentStyles =
    'fixed top-[50%] left-[50%] z-50 flex max-h-[calc(100dvh-2rem)] w-full max-w-[calc(100%-2rem)] translate-x-[-50%] translate-y-[-50%] rounded-lg shadow-xl duration-200 outline-none data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95 data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=open]:zoom-in-95';

const sidebarLayoutStyles = 'lg:h-[648px] lg:w-[860px] lg:gap-2 lg:bg-surface-main lg:p-2';

const contentSizeStyles = {
    custom: '',
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

const Dialog = (props: ComponentPropsWithRef<typeof DialogPrimitive.Root>) => <DialogPrimitive.Root {...props} />;

Dialog.displayName = 'Dialog';

interface DialogContentProps extends ComponentPropsWithRef<typeof DialogPrimitive.Content> {
    hasSidebar?: boolean;
    size?: DialogContentSizeType;
}

const DialogContent = ({children, className, hasSidebar = false, size = 'sm', ...props}: DialogContentProps) => {
    const layoutContextValue = useMemo(() => ({hasSidebar}), [hasSidebar]);

    return (
        <DialogPrimitive.Portal>
            <DialogPrimitive.Overlay className={overlayStyles} data-slot="dialog-overlay" />

            <DialogPrimitive.Content
                className={twMerge(
                    contentStyles,
                    contentSizeStyles[size],
                    hasSidebar && sidebarLayoutStyles,
                    className
                )}
                data-slot="dialog-content"
                {...props}
            >
                <DialogLayoutContext value={layoutContextValue}>{children}</DialogLayoutContext>
            </DialogPrimitive.Content>
        </DialogPrimitive.Portal>
    );
};

DialogContent.displayName = 'DialogContent';

const DialogClose = (props: ComponentPropsWithRef<typeof DialogPrimitive.Close>) => (
    <DialogPrimitive.Close data-slot="dialog-close" {...props} />
);

DialogClose.displayName = 'DialogClose';

const DialogDescription = (props: ComponentPropsWithRef<typeof DialogPrimitive.Description>) => (
    <DialogPrimitive.Description data-slot="dialog-description" {...props} />
);

DialogDescription.displayName = 'DialogDescription';

const DialogTrigger = (props: ComponentPropsWithRef<typeof DialogPrimitive.Trigger>) => (
    <DialogPrimitive.Trigger data-slot="dialog-trigger" {...props} />
);

DialogTrigger.displayName = 'DialogTrigger';

/**
 * Radix requires every DialogContent to carry a title. DialogHeader is the usual way to give one, but it also renders
 * visible chrome, so full-bleed surfaces that supply their own toolbar pair this with VisuallyHidden instead.
 */
const DialogTitle = (props: ComponentPropsWithRef<typeof DialogPrimitive.Title>) => (
    <DialogPrimitive.Title data-slot="dialog-title" {...props} />
);

DialogTitle.displayName = 'DialogTitle';

const useDialogLayout = () => useContext(DialogLayoutContext);

export {Dialog, DialogClose, DialogContent, DialogDescription, DialogTitle, DialogTrigger, useDialogLayout};
export type {DialogContentProps, DialogContentSizeType, DialogLayoutContextI};
