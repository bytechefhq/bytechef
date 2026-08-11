import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Checkbox} from '@/components/ui/checkbox';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {getRoleLabel} from '@/shared/util/role-utils';

import useInviteUserDialog from './hooks/useInviteUserDialog';

const WORKSPACE_ROLES = ['ADMIN', 'EDITOR', 'VIEWER'];

const InviteUserDialog = () => {
    const {
        authorities,
        handleClose,
        handleEmailChange,
        handleInvite,
        handleOpenChange,
        handleRoleChange,
        handleWorkspaceRoleChange,
        handleWorkspaceToggle,
        inviteDisabled,
        inviteEmail,
        inviteRole,
        inviteWorkspaces,
        open,
        workspaces,
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
                        The invitee receives a link on which they set their own password. Optionally add them to
                        workspaces now — you can also do that later from each workspace.
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
                            <label className="text-sm font-medium">Role</label>

                            <Select onValueChange={(value) => handleRoleChange(value)} value={inviteRole ?? undefined}>
                                <SelectTrigger>
                                    <SelectValue placeholder="Select role" />
                                </SelectTrigger>

                                <SelectContent>
                                    {authorities.map((authority) => (
                                        <SelectItem key={authority} value={authority}>
                                            {getRoleLabel(authority)}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>

                        <fieldset className="space-y-2 border-0 p-0">
                            <legend className="text-sm font-medium">Workspaces</legend>

                            {workspaces.length === 0 && (
                                <p className="text-xs text-muted-foreground">No workspaces available.</p>
                            )}

                            {workspaces.map((workspace) => {
                                const assignment = inviteWorkspaces.find(
                                    (inviteWorkspace) => inviteWorkspace.workspaceId === workspace.id
                                );

                                return (
                                    <div className="flex items-center justify-between gap-2" key={workspace.id}>
                                        <label className="flex items-center gap-2 text-sm">
                                            <Checkbox
                                                checked={assignment !== undefined}
                                                onCheckedChange={() => handleWorkspaceToggle(workspace.id)}
                                            />

                                            {workspace.name}
                                        </label>

                                        {assignment && (
                                            <Select
                                                onValueChange={(value) =>
                                                    handleWorkspaceRoleChange(workspace.id, value)
                                                }
                                                value={assignment.roleName}
                                            >
                                                <SelectTrigger className="w-32">
                                                    <SelectValue />
                                                </SelectTrigger>

                                                <SelectContent>
                                                    {WORKSPACE_ROLES.map((workspaceRole) => (
                                                        <SelectItem key={workspaceRole} value={workspaceRole}>
                                                            {getRoleLabel(workspaceRole)}
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                        )}
                                    </div>
                                );
                            })}
                        </fieldset>
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
