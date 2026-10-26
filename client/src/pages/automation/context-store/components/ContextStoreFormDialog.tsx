import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogTrigger,
} from '@/components/Dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ContextStore, useCreateContextStoreMutation, useUpdateContextStoreMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useEffect, useState} from 'react';

type ContextStoreFormDialogPropsType = {
    contextStore?: Pick<ContextStore, 'description' | 'id' | 'name' | 'version'>;
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    trigger?: ReactNode;
};

const ContextStoreFormDialog = ({
    contextStore,
    onOpenChange,
    open: controlledOpen,
    trigger,
}: ContextStoreFormDialogPropsType) => {
    const [internalOpen, setInternalOpen] = useState(false);
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const isControlled = controlledOpen !== undefined;
    const open = isControlled ? controlledOpen : internalOpen;
    const isEdit = contextStore !== undefined;

    const setOpen = (nextOpen: boolean) => {
        if (isControlled) {
            onOpenChange?.(nextOpen);
        } else {
            setInternalOpen(nextOpen);
        }
    };

    useEffect(() => {
        if (open && contextStore) {
            setName(contextStore.name);
            setDescription(contextStore.description ?? '');
        } else if (!open) {
            setName('');
            setDescription('');
        }
    }, [contextStore, open]);

    const createMutation = useCreateContextStoreMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['contextStores']});
            setOpen(false);
        },
    });

    const updateMutation = useUpdateContextStoreMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['contextStores']});
            setOpen(false);
        },
    });

    const isSubmitting = createMutation.isPending || updateMutation.isPending;

    const handleSubmit = () => {
        if (name.trim() === '') {
            return;
        }

        if (isEdit && contextStore) {
            updateMutation.mutate({
                id: contextStore.id,
                input: {
                    description: description.trim() === '' ? undefined : description,
                    name,
                    version: contextStore.version,
                },
                workspaceId: String(currentWorkspaceId),
            });
        } else {
            createMutation.mutate({
                environmentId: String(currentEnvironmentId),
                input: {
                    description: description.trim() === '' ? undefined : description,
                    name,
                },
                workspaceId: String(currentWorkspaceId),
            });
        }
    };

    return (
        <Dialog onOpenChange={setOpen} open={open}>
            {trigger && <DialogTrigger asChild>{trigger}</DialogTrigger>}

            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description={
                            isEdit
                                ? 'Update the name or description of this Context Store.'
                                : 'Create a parent Context Store. Sources you add later will hang off this entity and ' +
                                  'inherit its environment.'
                        }
                        title={isEdit ? 'Edit Context Store' : 'New Context Store'}
                    />

                    <DialogBody>
                        <fieldset className="space-y-4 border-0 p-0">
                            <fieldset className="space-y-1 border-0 p-0">
                                <Label htmlFor="context-store-name">Name</Label>

                                <Input
                                    id="context-store-name"
                                    onChange={(event) => setName(event.target.value)}
                                    placeholder="Customer mirror"
                                    value={name}
                                />
                            </fieldset>

                            <fieldset className="space-y-1 border-0 p-0">
                                <Label htmlFor="context-store-description">Description</Label>

                                <Textarea
                                    id="context-store-description"
                                    onChange={(event) => setDescription(event.target.value)}
                                    placeholder="Optional"
                                    value={description}
                                />
                            </fieldset>
                        </fieldset>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button
                            disabled={isSubmitting || name.trim() === ''}
                            label={isEdit ? 'Save' : 'Create'}
                            onClick={handleSubmit}
                            type="button"
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default ContextStoreFormDialog;
