import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ContextStore, useCreateContextStoreMutation, useUpdateContextStoreMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useEffect, useState} from 'react';

type ContextStoreFormDialogPropsType = {
    /**
     * Existing store to edit. When omitted, the dialog is in create mode. Carries the optimistic-locking version so
     * concurrent edits surface as a 409-style conflict.
     */
    contextStore?: Pick<ContextStore, 'description' | 'id' | 'name' | 'version'>;
    /** Controlled open state (used when triggered from a row action menu). Omit to use uncontrolled with a trigger. */
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    trigger?: ReactNode;
};

/**
 * Compact create/update dialog for a parent {@link ContextStore}. Reuse for both "New Context Store" buttons and
 * per-row "Edit" actions in the management page.
 */
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

    // Re-seed form fields when an existing store is opened for edit. Closing the dialog clears the local state so the
    // next open starts fresh.
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

            <DialogContent className="sm:max-w-[480px]">
                <DialogHeader>
                    <DialogTitle>{isEdit ? 'Edit Context Store' : 'New Context Store'}</DialogTitle>

                    <DialogDescription>
                        {isEdit
                            ? 'Update the name or description of this Context Store.'
                            : 'Create a parent Context Store. Sources you add later will hang off this entity and ' +
                              'inherit its environment.'}
                    </DialogDescription>
                </DialogHeader>

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

                <DialogFooter>
                    <Button label="Cancel" onClick={() => setOpen(false)} type="button" variant="outline" />

                    <Button
                        disabled={isSubmitting || name.trim() === ''}
                        label={isEdit ? 'Save' : 'Create'}
                        onClick={handleSubmit}
                        type="button"
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default ContextStoreFormDialog;
