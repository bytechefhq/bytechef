import AlertDialog from '@/components/AlertDialog';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import AppSecretKeyView from '@/pages/automation/apps/components/AppSecretKeyView';
import {App, useRotateAppSecretKeyMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

interface AppRotateKeyDialogProps {
    app: App;
    onClose: () => void;
    open: boolean;
}

const AppRotateKeyDialog = ({app, onClose, open}: AppRotateKeyDialogProps) => {
    const [secretKey, setSecretKey] = useState<string | undefined>();

    const queryClient = useQueryClient();

    const rotateAppSecretKeyMutation = useRotateAppSecretKeyMutation({
        onSuccess: (data) => {
            queryClient.invalidateQueries({queryKey: ['apps']});
            queryClient.invalidateQueries({queryKey: ['app']});

            setSecretKey(data.rotateAppSecretKey.secretKey);
        },
    });

    function handleClose() {
        setSecretKey(undefined);

        onClose();
    }

    if (secretKey) {
        return (
            <Dialog onOpenChange={(nextOpen) => !nextOpen && handleClose()} open={open}>
                <DialogContent aria-describedby={undefined} size="md">
                    <DialogMain>
                        <DialogHeader title="Save your new app key" />

                        <DialogBody>
                            <AppSecretKeyView secretKey={secretKey} />
                        </DialogBody>

                        <DialogFooter>
                            <DialogCancelButton label="Done" />
                        </DialogFooter>
                    </DialogMain>
                </DialogContent>
            </Dialog>
        );
    }

    return (
        <AlertDialog
            confirmButtonVariant="default"
            confirmLabel="Rotate key"
            description={`The current key of "${app.name}" stops working immediately. Update the host's environment with the new key right after rotating.`}
            isPending={rotateAppSecretKeyMutation.isPending}
            onCancel={handleClose}
            onConfirm={() => rotateAppSecretKeyMutation.mutate({id: app.id})}
            open={open}
            title="Rotate secret key?"
        />
    );
};

export default AppRotateKeyDialog;
