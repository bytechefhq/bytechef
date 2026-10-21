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
import {AppInstance, useRotateAppInstanceSecretKeyMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

interface AppRotateKeyDialogProps {
    appInstance: Pick<AppInstance, 'id' | 'name'>;
    onClose: () => void;
    open: boolean;
}

const AppRotateKeyDialog = ({appInstance, onClose, open}: AppRotateKeyDialogProps) => {
    const [secretKey, setSecretKey] = useState<string | undefined>();

    const queryClient = useQueryClient();

    const rotateAppInstanceSecretKeyMutation = useRotateAppInstanceSecretKeyMutation({
        onSuccess: (data) => {
            queryClient.invalidateQueries({queryKey: ['apps']});
            queryClient.invalidateQueries({queryKey: ['app']});
            queryClient.invalidateQueries({queryKey: ['hostedAppInstances']});

            setSecretKey(data.rotateAppInstanceSecretKey.secretKey);
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
                        <DialogHeader title="Save your new instance key" />

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
            description={`The current key of "${appInstance.name}" stops working immediately. Update the host's environment with the new key right after rotating.`}
            isPending={rotateAppInstanceSecretKeyMutation.isPending}
            onCancel={handleClose}
            onConfirm={() => rotateAppInstanceSecretKeyMutation.mutate({id: appInstance.id})}
            open={open}
            title="Rotate secret key?"
        />
    );
};

export default AppRotateKeyDialog;
