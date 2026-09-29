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
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';

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
        roleSelectVisible,
    } = useInviteUserDialog();

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description="Enter the user email. A strong password is pre-generated according to the security rules. This password will be included in the invitation email."
                        title="Invite User"
                    />

                    <DialogBody>
                        <div className="flex flex-col gap-4">
                            <div className="flex flex-col gap-2">
                                <label className="text-sm font-medium">Email</label>

                                <Input
                                    onChange={(event) => handleEmailChange(event.target.value)}
                                    placeholder="user@example.com"
                                    type="email"
                                    value={inviteEmail}
                                />
                            </div>

                            <div className="flex flex-col gap-2">
                                <label className="text-sm font-medium">Password</label>

                                <Input readOnly type="text" value={invitePassword} />

                                <div>
                                    <Button
                                        label="Regenerate"
                                        onClick={handleRegeneratePassword}
                                        size="sm"
                                        variant="outline"
                                    />
                                </div>

                                <p className="text-xs text-muted-foreground">
                                    Password must be at least 8 characters and include at least 1 uppercase letter and 1
                                    number.
                                </p>
                            </div>

                            {roleSelectVisible && (
                                <div className="flex flex-col gap-2">
                                    <label className="text-sm font-medium">Role</label>

                                    <Select
                                        onValueChange={(value) => handleRoleChange(value)}
                                        value={inviteRole ?? undefined}
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
                            )}
                        </div>
                    </DialogBody>

                    <DialogFooter>
                        <DialogClose asChild>
                            <Button label="Cancel" onClick={handleClose} variant="outline" />
                        </DialogClose>

                        <Button disabled={inviteDisabled} label="Invite" onClick={handleInvite} />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default InviteUserDialog;
