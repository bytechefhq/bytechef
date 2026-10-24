import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import ConnectedUserMetadataFields, {
    type MetadataRowI,
    metadataRowsToRecord,
} from '@/ee/pages/embedded/connected-users/components/ConnectedUserMetadataFields';
import useConnectedUserSheetStore from '@/ee/pages/embedded/connected-users/stores/useConnectedUserSheetStore';
import {useCreateConnectedUserMutation} from '@/ee/shared/mutations/embedded/connectedUsers.mutations';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useForm} from 'react-hook-form';

interface ConnectedUserCreateFormI {
    email: string;
    externalId: string;
    metadata: MetadataRowI[];
    name: string;
}

const ConnectedUserCreateDialog = ({onClose}: {onClose: () => void}) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const setConnectedUserId = useConnectedUserSheetStore((state) => state.setConnectedUserId);
    const setConnectedUserSheetOpen = useConnectedUserSheetStore((state) => state.setConnectedUserSheetOpen);

    const queryClient = useQueryClient();

    const {
        control,
        formState: {errors},
        handleSubmit,
        register,
    } = useForm<ConnectedUserCreateFormI>({
        defaultValues: {email: '', externalId: '', metadata: [], name: ''},
    });

    const createConnectedUserMutation = useCreateConnectedUserMutation({
        onSuccess: (connectedUser) => {
            void queryClient.invalidateQueries({queryKey: ConnectedUserKeys.connectedUsers});

            onClose();

            if (connectedUser.id != null) {
                setConnectedUserId(connectedUser.id);
                setConnectedUserSheetOpen(true);
            }
        },
    });

    const onSubmit = (values: ConnectedUserCreateFormI) => {
        createConnectedUserMutation.mutate({
            createConnectedUserRequest: {
                email: values.email || undefined,
                environmentId: currentEnvironmentId,
                externalId: values.externalId,
                metadata: metadataRowsToRecord(values.metadata),
                name: values.name || undefined,
            },
        });
    };

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent className="sm:max-w-lg">
                <DialogHeader>
                    <DialogTitle>New connected user</DialogTitle>
                </DialogHeader>

                <form className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)}>
                    <div className="flex flex-col gap-1.5">
                        <Label htmlFor="connected-user-external-id">
                            External ID <span className="text-destructive">*</span>
                        </Label>

                        <Input
                            id="connected-user-external-id"
                            {...register('externalId', {
                                maxLength: {message: 'External ID must be at most 256 characters', value: 256},
                                required: 'External ID is required',
                                validate: (value) =>
                                    value === value.trim() || 'External ID must not start or end with whitespace',
                            })}
                        />

                        {errors.externalId && <p className="text-sm text-destructive">{errors.externalId.message}</p>}
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

                    <DialogFooter>
                        <Button
                            disabled={createConnectedUserMutation.isPending}
                            label="Cancel"
                            onClick={onClose}
                            type="button"
                            variant="outline"
                        />

                        <Button disabled={createConnectedUserMutation.isPending} label="Create" type="submit" />
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
};

export default ConnectedUserCreateDialog;
