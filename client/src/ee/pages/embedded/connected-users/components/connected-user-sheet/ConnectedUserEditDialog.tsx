import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import ConnectedUserMetadataFields, {
    type MetadataRowI,
    metadataRowsToRecord,
    recordToMetadataRows,
} from '@/ee/pages/embedded/connected-users/components/ConnectedUserMetadataFields';
import {ConnectedUser, ResponseError} from '@/ee/shared/middleware/embedded/connected-user';
import {useUpdateConnectedUserMutation} from '@/ee/shared/mutations/embedded/connectedUsers.mutations';
import {ConnectedUserKeys, useGetConnectedUserQuery} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {useForm} from 'react-hook-form';

interface ConnectedUserEditFormI {
    email: string;
    metadata: MetadataRowI[];
    name: string;
}

const toFormValues = (connectedUser: ConnectedUser): ConnectedUserEditFormI => ({
    email: connectedUser.email ?? '',
    metadata: recordToMetadataRows(connectedUser.metadata),
    name: connectedUser.name ?? '',
});

const ConnectedUserEditDialog = ({connectedUser, onClose}: {connectedUser: ConnectedUser; onClose: () => void}) => {
    const [conflict, setConflict] = useState(false);
    const [version, setVersion] = useState(connectedUser.version);

    const queryClient = useQueryClient();

    const {refetch} = useGetConnectedUserQuery(connectedUser.id!, false);

    const {control, handleSubmit, register, reset} = useForm<ConnectedUserEditFormI>({
        defaultValues: toFormValues(connectedUser),
    });

    const updateConnectedUserMutation = useUpdateConnectedUserMutation({
        onError: (error) => {
            if (error instanceof ResponseError && error.response.status === 409) {
                setConflict(true);
            }
        },
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ConnectedUserKeys.connectedUser(connectedUser.id!)});
            void queryClient.invalidateQueries({queryKey: ConnectedUserKeys.connectedUsers});

            onClose();
        },
    });

    const handleReload = async () => {
        const {data} = await refetch();

        if (data) {
            reset(toFormValues(data));

            setVersion(data.version);
        }

        setConflict(false);
    };

    const onSubmit = (values: ConnectedUserEditFormI) => {
        updateConnectedUserMutation.mutate({
            id: connectedUser.id!,
            updateConnectedUserRequest: {
                email: values.email,
                metadata: metadataRowsToRecord(values.metadata),
                name: values.name,
                version: version!,
            },
        });
    };

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent className="sm:max-w-lg">
                <DialogHeader>
                    <DialogTitle>Edit connected user</DialogTitle>
                </DialogHeader>

                <form className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)}>
                    <div className="flex flex-col gap-1.5">
                        <Label htmlFor="connected-user-external-id">External ID</Label>

                        <Input id="connected-user-external-id" readOnly value={connectedUser.externalId ?? ''} />
                    </div>

                    <div className="flex flex-col gap-1.5">
                        <Label htmlFor="connected-user-name">Name</Label>

                        <Input id="connected-user-name" {...register('name')} />
                    </div>

                    <div className="flex flex-col gap-1.5">
                        <Label htmlFor="connected-user-email">Email</Label>

                        <Input id="connected-user-email" type="email" {...register('email')} />
                    </div>

                    <ConnectedUserMetadataFields control={control} name="metadata" />

                    {conflict && (
                        <div className="flex items-center justify-between gap-2 text-sm text-destructive">
                            <p>This connected user changed since you opened it. Reload to see the latest values.</p>

                            <Button label="Reload" onClick={handleReload} type="button" variant="outline" />
                        </div>
                    )}

                    <DialogFooter>
                        <Button
                            disabled={updateConnectedUserMutation.isPending}
                            label="Cancel"
                            onClick={onClose}
                            type="button"
                            variant="outline"
                        />

                        <Button disabled={updateConnectedUserMutation.isPending} label="Save" type="submit" />
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
};

export default ConnectedUserEditDialog;
