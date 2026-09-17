import Button from '@/components/Button/Button';
import {
    DialogClose as ShadcnDialogClose,
    DialogDescription as ShadcnDialogDescription,
    DialogTitle as ShadcnDialogTitle,
} from '@/components/ui/dialog';
import {XIcon} from 'lucide-react';
import {type ComponentPropsWithRef, type ReactElement, type ReactNode, useEffect, useRef} from 'react';
import {twMerge} from 'tailwind-merge';

import {useDialogLayout} from './Dialog';
import {useOptionalDialogSteps} from './hooks/useDialogSteps';

type DialogMainPropsType = ComponentPropsWithRef<'div'>;

type DialogBodyPropsType = ComponentPropsWithRef<'div'>;

interface DialogHeaderProps extends Omit<ComponentPropsWithRef<'div'>, 'title'> {
    description?: ReactNode;
    dialogDescription?: string;
    dialogTitle?: string;
    endContent?: ReactNode;
    icon?: ReactElement;
    showCloseButton?: boolean;
    title?: string;
}

interface DialogFooterProps extends ComponentPropsWithRef<'div'> {
    startContent?: ReactNode;
}

const headingStyles = 'text-xl leading-7 font-medium text-content-neutral-primary outline-none';

const descriptionStyles = 'text-sm text-content-neutral-secondary';

const DialogMain = ({className, ...props}: DialogMainPropsType) => (
    <div
        className={twMerge(
            'flex min-h-0 flex-1 flex-col rounded-lg border border-stroke-neutral-primary bg-surface-neutral-primary sm:min-w-[512px]',
            className
        )}
        data-slot="dialog-main"
        {...props}
    />
);

DialogMain.displayName = 'DialogMain';

const DialogHeader = ({
    className,
    description,
    dialogDescription,
    dialogTitle,
    endContent,
    icon,
    showCloseButton = true,
    title,
    ...props
}: DialogHeaderProps) => {
    const headingRef = useRef<HTMLHeadingElement>(null);
    const previousStepIdRef = useRef<string | undefined>(undefined);

    const {hasSidebar} = useDialogLayout();
    const dialogSteps = useOptionalDialogSteps();

    const currentStepId = dialogSteps?.currentStep.id;
    const headingText = title || dialogSteps?.currentStep.label;
    const stepPositionLabel =
        dialogSteps && hasSidebar
            ? `Step ${dialogSteps.currentStepIndex + 1} of ${dialogSteps.steps.length}`
            : undefined;

    useEffect(() => {
        if (previousStepIdRef.current !== undefined && previousStepIdRef.current !== currentStepId) {
            headingRef.current?.focus();
        }

        previousStepIdRef.current = currentStepId;
    }, [currentStepId]);

    return (
        <div
            className={twMerge('flex items-center justify-between gap-2 p-4', className)}
            data-slot="dialog-header"
            {...props}
        >
            <div className="flex min-w-0 flex-col gap-1">
                <div className="flex items-center gap-2">
                    {icon && (
                        <span className="flex shrink-0 items-center text-content-neutral-primary [&_svg]:size-5">
                            {icon}
                        </span>
                    )}

                    {(headingText || dialogTitle) && (
                        <ShadcnDialogTitle className={headingStyles} ref={headingRef} tabIndex={-1}>
                            {dialogTitle ? (
                                <>
                                    <span className="sr-only">
                                        {headingText ? `${dialogTitle} - ${headingText}` : dialogTitle}
                                    </span>

                                    <span aria-hidden="true" className="lg:hidden">{`${dialogTitle} - `}</span>

                                    <span aria-hidden="true">{headingText}</span>
                                </>
                            ) : (
                                headingText
                            )}
                        </ShadcnDialogTitle>
                    )}
                </div>

                {(description || dialogDescription) && (
                    <ShadcnDialogDescription className={twMerge(descriptionStyles, !description && 'lg:sr-only')}>
                        {dialogDescription && (
                            <span className={description ? 'lg:sr-only' : undefined}>
                                {description ? `${dialogDescription} ` : dialogDescription}
                            </span>
                        )}

                        {description}
                    </ShadcnDialogDescription>
                )}

                {stepPositionLabel && (
                    <span className="text-xs leading-4 font-medium text-content-neutral-secondary lg:hidden">
                        {stepPositionLabel}
                    </span>
                )}
            </div>

            <div className="flex shrink-0 items-center gap-2">
                {endContent}

                {showCloseButton && (
                    <ShadcnDialogClose asChild>
                        <Button aria-label="Close" icon={<XIcon />} size="iconXs" variant="ghost" />
                    </ShadcnDialogClose>
                )}
            </div>
        </div>
    );
};

DialogHeader.displayName = 'DialogHeader';

const DialogBody = ({className, ...props}: DialogBodyPropsType) => (
    <div
        className={twMerge('min-h-0 flex-1 overflow-y-auto px-4 pb-4', className)}
        data-slot="dialog-body"
        {...props}
    />
);

DialogBody.displayName = 'DialogBody';

const DialogFooter = ({children, className, startContent, ...props}: DialogFooterProps) => (
    <div
        className={twMerge(
            'flex items-center justify-between gap-2 border-t border-stroke-neutral-primary px-4 py-2.5',
            className
        )}
        data-slot="dialog-footer"
        {...props}
    >
        <div className="flex items-center gap-1.5">{startContent}</div>

        <div className="flex items-center gap-2">{children}</div>
    </div>
);

DialogFooter.displayName = 'DialogFooter';

export {DialogBody, DialogFooter, DialogHeader, DialogMain};
export type {
    DialogBodyPropsType as DialogBodyProps,
    DialogFooterProps,
    DialogHeaderProps,
    DialogMainPropsType as DialogMainProps,
};
