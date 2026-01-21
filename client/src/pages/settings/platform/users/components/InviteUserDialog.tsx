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
import {Input} from '@/components/ui/input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';

import useInviteUserDialog from './hooks/useInviteUserDialog';

const InviteUserDialog = () => {
    const {
        authorities,
        handleClose,
        handleEmailChange,
        handleInvite,
        handleOpenChange,
        handleRegeneratePassword,
        handleRoleChange,
        inviteDisabled,
        inviteEmail,
        invitePassword,
        inviteRole,
        open,
    } = useInviteUserDialog();

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogContent>
                <div className="flex flex-col gap-4">
                    <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                        <DialogTitle>Invite User</DialogTitle>

                        <DialogCloseButton />
                    </DialogHeader>

                    <p className="text-sm text-muted-foreground">
                        Enter the user email. A strong password is pre-generated according to the security rules. This
                        password will be included in the invitation email.
                    </p>

                    <div className="space-y-4">
                        <div className="space-y-2">
                            <label className="text-sm font-medium">Email</label>

                            <Input
                                onChange={(event) => handleEmailChange(event.target.value)}
                                placeholder="user@example.com"
                                type="email"
                                value={inviteEmail}
                            />
                        </div>

                        <div className="space-y-2">
                            <label className="text-sm font-medium">Password</label>

                            <Input readOnly type="text" value={invitePassword} />

                            <div>
                                <Button onClick={handleRegeneratePassword} size="sm" variant="outline">
                                    Regenerate
                                </Button>
                            </div>

                            <p className="text-xs text-muted-foreground">
                                Password must be at least 8 characters and include at least 1 uppercase letter and 1
                                number.
                            </p>
                        </div>

                        <div className="space-y-2">
                            <label className="text-sm font-medium">Role</label>

                            <Select onValueChange={(value) => handleRoleChange(value)} value={inviteRole ?? undefined}>
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

                        <Button disabled={inviteDisabled} onClick={handleInvite}>
                            Invite
                        </Button>
                    </DialogFooter>
                </div>
            </DialogContent>
        </Dialog>
    );
};

export default InviteUserDialog;
