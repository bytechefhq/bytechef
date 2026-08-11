import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
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
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {CUSTOM_ROLE_PREFIX} from '@/ee/pages/settings/automation/users/util/workspace-role-values';
import {WorkspaceRole} from '@/shared/middleware/graphql';
import {getRoleLabel} from '@/shared/util/role-utils';
import {useEffect, useState} from 'react';

// Derive from the generated GraphQL enum so a new server-side role appears here without a client change.
const WORKSPACE_ROLES = Object.values(WorkspaceRole);

const INVITE_TAB = 'invite';
const ADD_TAB = 'add';

interface WorkspaceInviteUserDialogPropsI {
    addableUsers: {email: string; id: string}[];
    customRoles: {id: string; name: string}[];
    /**
     * Whether to offer the "Add existing user" tab. Listing every account in the tenant is ROLE_ADMIN-only and
     * deliberately so — it exposes the whole organisation's user list. A workspace admin adds an existing colleague
     * through invite-by-email instead, which reuses the account and consumes no extra seat.
     */
    isTenantAdmin: boolean;
    onAdd: (userId: string, roleValue: string) => void;
    onInvite: (email: string, roleValue: string) => void;
    onOpenChange: (open: boolean) => void;
    open: boolean;
}

const WorkspaceInviteUserDialog = ({
    addableUsers,
    customRoles,
    isTenantAdmin,
    onAdd,
    onInvite,
    onOpenChange,
    open,
}: WorkspaceInviteUserDialogPropsI) => {
    const [addUserId, setAddUserId] = useState('');
    const [email, setEmail] = useState('');
    // A plain string, not WorkspaceRole: the picker also offers custom roles, carried as `custom:<id>`.
    const [roleValue, setRoleValue] = useState<string>(WorkspaceRole.Editor);
    const [tab, setTab] = useState(INVITE_TAB);

    // Each opening starts clean. Without this a cancelled invite leaves its address prefilled the next time the
    // dialog opens, which reads as the previous attempt having been kept.
    useEffect(() => {
        if (open) {
            setAddUserId('');
            setEmail('');
            setRoleValue(WorkspaceRole.Editor);
            setTab(INVITE_TAB);
        }
    }, [open]);

    const roleSelect = (
        <Select onValueChange={setRoleValue} value={roleValue}>
            <SelectTrigger>
                <SelectValue />
            </SelectTrigger>

            <SelectContent>
                {WORKSPACE_ROLES.map((workspaceRole) => (
                    <SelectItem key={workspaceRole} value={workspaceRole}>
                        {getRoleLabel(workspaceRole)}
                    </SelectItem>
                ))}

                {customRoles.map((customRole) => (
                    <SelectItem key={customRole.id} value={`${CUSTOM_ROLE_PREFIX}${customRole.id}`}>
                        {customRole.name}
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>
    );

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Invite User</DialogTitle>

                    <DialogCloseButton />
                </DialogHeader>

                <Tabs onValueChange={setTab} value={tab}>
                    <TabsList>
                        <TabsTrigger value={INVITE_TAB}>Invite by email</TabsTrigger>

                        {isTenantAdmin && <TabsTrigger value={ADD_TAB}>Add existing user</TabsTrigger>}
                    </TabsList>

                    <TabsContent className="space-y-4 pt-4" value={INVITE_TAB}>
                        <p className="text-xs text-muted-foreground">
                            They receive a link on which they set their own password. If they already have an account,
                            it is reused.
                        </p>

                        <div className="space-y-2">
                            <label className="text-sm font-medium">Email</label>

                            <Input
                                onChange={(event) => setEmail(event.target.value)}
                                placeholder="colleague@example.com"
                                type="email"
                                value={email}
                            />
                        </div>

                        <div className="space-y-2">
                            <label className="text-sm font-medium">Role</label>

                            {roleSelect}
                        </div>
                    </TabsContent>

                    {isTenantAdmin && (
                        <TabsContent className="space-y-4 pt-4" value={ADD_TAB}>
                            <p className="text-xs text-muted-foreground">
                                Adds someone who already has a ByteChef account. No email is sent and no seat is
                                consumed.
                            </p>

                            <div className="space-y-2">
                                <label className="text-sm font-medium">User</label>

                                <Select onValueChange={setAddUserId} value={addUserId}>
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select a user" />
                                    </SelectTrigger>

                                    <SelectContent>
                                        {addableUsers.map((addableUser) => (
                                            <SelectItem key={addableUser.id} value={addableUser.id}>
                                                {addableUser.email}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>

                            <div className="space-y-2">
                                <label className="text-sm font-medium">Role</label>

                                {roleSelect}
                            </div>
                        </TabsContent>
                    )}
                </Tabs>

                <DialogFooter>
                    <DialogClose asChild>
                        <Button variant="outline">Cancel</Button>
                    </DialogClose>

                    {tab === INVITE_TAB ? (
                        <Button disabled={!email} onClick={() => onInvite(email, roleValue)}>
                            Invite
                        </Button>
                    ) : (
                        <Button disabled={!addUserId} onClick={() => onAdd(addUserId, roleValue)}>
                            Add
                        </Button>
                    )}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default WorkspaceInviteUserDialog;
