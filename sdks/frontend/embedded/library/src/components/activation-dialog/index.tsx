import {useCallback, useEffect, useRef, useState} from 'react';
import {createRoot} from 'react-dom/client';
import ActivationDialog, {ActivationDialogProps} from './ActivationDialog';

export type UseActivationDialogProps = Omit<ActivationDialogProps, 'onClose'> & {onClose?: () => void};

interface ActivationDialogHookReturnType {
    closeDialog: () => void;
    openDialog: () => void;
}

const PORTAL_ID = 'activation-dialog-portal';

export default function useActivationDialog({
    onClose,
    ...dialogProps
}: UseActivationDialogProps): ActivationDialogHookReturnType {
    const [isOpen, setIsOpen] = useState(false);

    const portalContainerRef = useRef<HTMLElement | null>(null);
    const rootRef = useRef<ReturnType<typeof createRoot> | null>(null);

    const openDialog = useCallback(() => setIsOpen(true), []);

    const closeDialog = useCallback(() => {
        setIsOpen(false);

        onClose?.();
    }, [onClose]);

    useEffect(() => {
        let container = document.getElementById(PORTAL_ID);

        if (!container) {
            container = document.createElement('div');

            container.id = PORTAL_ID;

            document.body.appendChild(container);
        }

        portalContainerRef.current = container;

        return () => {
            const root = rootRef.current;

            rootRef.current = null;

            if (root) {
                queueMicrotask(() => root.unmount());
            }

            if (container && container.parentNode && container.childNodes.length === 0) {
                container.parentNode.removeChild(container);
            }
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
