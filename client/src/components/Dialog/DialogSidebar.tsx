import {DialogDescription as ShadcnDialogDescription, DialogTitle as ShadcnDialogTitle} from '@/components/ui/dialog';
import {type ComponentPropsWithRef, type ReactElement, type ReactNode, useLayoutEffect} from 'react';
import {twMerge} from 'tailwind-merge';

import {useDialogLayout} from './Dialog';

interface DialogSidebarProps extends Omit<ComponentPropsWithRef<'div'>, 'title'> {
    description?: ReactNode;
    icon?: ReactElement;
    title: string;
}

const DialogSidebar = ({children, className, description, icon, title, ...props}: DialogSidebarProps) => {
    const {registerSidebar} = useDialogLayout();

    useLayoutEffect(() => registerSidebar(), [registerSidebar]);

    return (
        <div
            className={twMerge('hidden w-80 shrink-0 flex-col gap-4 p-6 lg:flex', className)}
            data-slot="dialog-sidebar"
            {...props}
        >
            <div className="flex flex-col gap-1.5">
                <div className="flex items-center gap-2">
                    {icon && (
                        <span className="flex size-6 shrink-0 items-center justify-center text-content-neutral-primary [&_svg]:size-6">
                            {icon}
                        </span>
                    )}

                    <ShadcnDialogTitle className="text-xl leading-7 font-bold text-content-neutral-primary">
                        {title}
                    </ShadcnDialogTitle>
                </div>

                {description && (
                    <ShadcnDialogDescription className="text-sm text-content-neutral-secondary">
                        {description}
                    </ShadcnDialogDescription>
                )}
            </div>

            {children}
        </div>
    );
};

DialogSidebar.displayName = 'DialogSidebar';

export {DialogSidebar};
export type {DialogSidebarProps};
