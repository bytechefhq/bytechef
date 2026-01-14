import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {forwardRef, useImperativeHandle} from 'react';

import useEditUserDialog from './hooks/useEditUserDialog';

export interface EditUserDialogRefI {
    open: (login: string) => void;
}

const EditUserDialog = forwardRef<EditUserDialogRefI>(function EditUserDialog(_, ref) {
    const {authorities, editRole, editUser, handleClose, handleOpen, handleUpdate, open, setEditRole, updateDisabled} =
        useEditUserDialog();

    useImperativeHandle(ref, () => ({
        open: handleOpen,
    }));

    return (
        <Dialog onOpenChange={(o) => !o && handleClose()} open={open}>
            <DialogContent>
                <div className="flex flex-col gap-4">
                    <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                        <DialogTitle>Edit User</DialogTitle>

                        <DialogCloseButton />
                    </DialogHeader>

                    <p className="text-sm text-muted-foreground">Change the user role.</p>

                    <div className="space-y-4">
                        <div className="space-y-2">
                            <label className="text-sm font-medium">User</label>

                            <div className="text-sm text-muted-foreground">{editUser?.email ?? editUser?.login}</div>
                        </div>

                        <div className="space-y-2">
                            <label className="text-sm font-medium">Role</label>

                            <Select onValueChange={(v) => setEditRole(v)} value={editRole ?? undefined}>
                                <SelectTrigger>
                                    <SelectValue placeholder="Select role" />
                                </SelectTrigger>

                                <SelectContent>
                                    {authorities.map((role) => (
                                        <SelectItem key={role} value={role}>
                                            {role}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                    </div>

                    <DialogFooter>
                        <DialogClose asChild>
                            <Button onClick={handleClose} type="button" variant="outline">
                                Cancel
                            </Button>
                        </DialogClose>

                        <Button disabled={updateDisabled} onClick={handleUpdate}>
                            Save
                        </Button>
                    </DialogFooter>
                </div>
            </DialogContent>
        </Dialog>
    );
});

export default EditUserDialog;
