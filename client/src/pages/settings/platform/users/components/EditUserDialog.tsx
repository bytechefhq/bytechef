import Button from '@/components/Button/Button';
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

import useEditUserDialog from './hooks/useEditUserDialog';

const EditUserDialog = () => {
    const {
        authorities,
        editRole,
        editUser,
        handleClose,
        handleOpenChange,
        handleRoleChange,
        handleUpdate,
        open,
        updateDisabled,
    } = useEditUserDialog();

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
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

                            <p className="text-sm text-muted-foreground">{editUser?.email ?? editUser?.login}</p>
                        </div>

                        <div className="space-y-2">
                            <label className="text-sm font-medium">Role</label>

                            <Select onValueChange={(value) => handleRoleChange(value)} value={editRole ?? undefined}>
                                <SelectTrigger>
                                    <SelectValue placeholder="Select role" />
                                </SelectTrigger>

                                <SelectContent>
                                    {authorities.map((authority) => (
                                        <SelectItem key={authority} value={authority}>
                                            {authority}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                    </div>

                    <DialogFooter>
                        <DialogClose asChild>
                            <Button onClick={handleClose} variant="outline">
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
};

export default EditUserDialog;
