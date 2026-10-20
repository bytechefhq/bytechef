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
import {WorkspaceRole} from '@/shared/middleware/graphql';
import {getRoleLabel} from '@/shared/util/role-utils';
import {useEffect, useId, useState} from 'react';

const EMBEDDED_ROLES = Object.values(WorkspaceRole);

interface EmbeddedAddMemberDialogProps {
    onAdd: (email: string, role: WorkspaceRole) => void;
    onOpenChange: (open: boolean) => void;
    open: boolean;
}

const EmbeddedAddMemberDialog = ({onAdd, onOpenChange, open}: EmbeddedAddMemberDialogProps) => {
    const [email, setEmail] = useState('');
    const [role, setRole] = useState<WorkspaceRole>(WorkspaceRole.Editor);

    const emailInputId = useId();
    const roleSelectId = useId();

    useEffect(() => {
        if (open) {
            setEmail('');
            setRole(WorkspaceRole.Editor);
        }
    }, [open]);

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <DialogTitle>Add member</DialogTitle>

                    <DialogCloseButton />
                </DialogHeader>

                <fieldset className="space-y-4">
                    <p className="text-xs text-muted-foreground">
                        Adds someone who already has a ByteChef account. No email is sent.
                    </p>

                    <div className="space-y-2">
                        <label className="text-sm font-medium" htmlFor={emailInputId}>
                            Email
                        </label>

                        <Input
                            id={emailInputId}
                            onChange={(event) => setEmail(event.target.value)}
                            placeholder="colleague@example.com"
                            type="email"
                            value={email}
                        />
                    </div>

                    <div className="space-y-2">
                        <label className="text-sm font-medium" htmlFor={roleSelectId}>
                            Role
                        </label>

                        <Select onValueChange={(value) => setRole(value as WorkspaceRole)} value={role}>
                            <SelectTrigger id={roleSelectId}>
                                <SelectValue />
                            </SelectTrigger>

                            <SelectContent>
                                {EMBEDDED_ROLES.map((embeddedRole) => (
                                    <SelectItem key={embeddedRole} value={embeddedRole}>
                                        {getRoleLabel(embeddedRole)}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                </fieldset>

                <DialogFooter>
                    <DialogClose asChild>
                        <Button variant="outline">Cancel</Button>
                    </DialogClose>

                    <Button disabled={!email.trim()} onClick={() => onAdd(email.trim(), role)}>
                        Add
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default EmbeddedAddMemberDialog;
