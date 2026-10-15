import {useCallback, useEffect, useRef, useState} from 'react';
import {createRoot} from 'react-dom/client';
import ActivationDialog, {ActivationDialogProps} from './ActivationDialog';

export type UseActivationDialogProps = Omit<ActivationDialogProps, 'onClose'> & {onClose?: () => void};

interface ActivationDialogHookReturnType {
    closeDialog: () => void;
    openDialog: () => void;
}

export default function useActivationDialog({
    onClose,
    ...dialogProps
}: UseActivationDialogProps): ActivationDialogHookReturnType {
    const [isOpen, setIsOpen] = useState(false);

    const portalContainerRef = useRef<HTMLDivElement | null>(null);
    const rootRef = useRef<ReturnType<typeof createRoot> | null>(null);

    const openDialog = useCallback(() => setIsOpen(true), []);

    const closeDialog = useCallback(() => {
        setIsOpen(false);

        onClose?.();
    }, [onClose]);

    useEffect(() => {
        const container = document.createElement('div');

        container.setAttribute('data-bytechef-activation-dialog', '');

        document.body.appendChild(container);

        portalContainerRef.current = container;

        return () => {
            const root = rootRef.current;

            rootRef.current = null;
            portalContainerRef.current = null;

            queueMicrotask(() => {
                root?.unmount();

                container.remove();
            });
        };
    }, []);

    useEffect(() => {
        if (!rootRef.current) {
            if (!isOpen || !portalContainerRef.current) {
                return;
            }

            rootRef.current = createRoot(portalContainerRef.current);
        }

        rootRef.current.render(isOpen ? <ActivationDialog {...dialogProps} onClose={closeDialog} /> : null);
        // dialogProps is a fresh object each render; its members are the real dependencies.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        closeDialog,
        isOpen,
        dialogProps.baseUrl,
        dialogProps.connectionCreationAllowed,
        dialogProps.environment,
        dialogProps.jwtToken,
        dialogProps.mode,
        dialogProps.onActivated,
        dialogProps.onEditWorkflow,
        dialogProps.workflowTemplateId,
    ]);

    return {closeDialog, openDialog};
}
