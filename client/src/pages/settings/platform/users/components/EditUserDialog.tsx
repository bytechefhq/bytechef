import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogClose,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';

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
                <DialogMain>
                    <DialogHeader description="Change the user role." title="Edit User" />

                    <DialogBody>
                        <div className="flex flex-col gap-4">
                            <div className="flex flex-col gap-2">
                                <label className="text-sm font-medium">User</label>

                                <p className="text-sm text-muted-foreground">{editUser?.email ?? editUser?.login}</p>
                            </div>

                            <div className="flex flex-col gap-2">
                                <label className="text-sm font-medium">Role</label>

                                <Select
                                    onValueChange={(value) => handleRoleChange(value)}
                                    value={editRole ?? undefined}
                                >
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
                    </DialogBody>

                    <DialogFooter>
                        <DialogClose asChild>
                            <Button label="Cancel" onClick={handleClose} variant="outline" />
                        </DialogClose>

                        <Button disabled={updateDisabled} label="Save" onClick={handleUpdate} />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default EditUserDialog;
