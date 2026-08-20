import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import ResourceVisibilityBadge from '@/shared/components/visibility/ResourceVisibilityBadge';
import {
    useAffectedWorkflowsQuery,
    useReassignAllConnectionsMutation,
    useUnresolvedConnectionsQuery,
    useUsersQuery,
} from '@/shared/middleware/graphql';
import {AlertTriangleIcon} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface ConnectionReassignmentDialogProps {
    onClose: () => void;
    open: boolean;
    userLogin: string;
    workspaceId: number;
}

const ConnectionReassignmentDialog = ({onClose, open, userLogin, workspaceId}: ConnectionReassignmentDialogProps) => {
    const [newOwnerLogin, setNewOwnerLogin] = useState('');
    const [reassignmentError, setReassignmentError] = useState<string | null>(null);

    const {data: unresolvedConnectionsData, isLoading: isLoadingConnections} = useUnresolvedConnectionsQuery(
        {userLogin, workspaceId: String(workspaceId)},
        {enabled: open}
    );

    const {data: affectedWorkflowsData} = useAffectedWorkflowsQuery(
        {userLogin, workspaceId: String(workspaceId)},
        {enabled: open}
    );

    const {data: usersData} = useUsersQuery({pageSize: 500}, {enabled: open});

    const {isPending: isReassigning, mutate: reassignAll} = useReassignAllConnectionsMutation({
        // Keep an inline banner; the global toast from useFetchInterceptor can be missed while the
        // user is focused on this dialog's select.
        onError: (error: Error) => {
            setReassignmentError(error.message || 'Failed to reassign connections. Please try again.');
        },
        onSuccess: () => {
            onClose();
        },
    });

    const connections = useMemo(
        () => unresolvedConnectionsData?.unresolvedConnections ?? [],
        [unresolvedConnectionsData]
    );

    const affectedWorkflowCount = useMemo(
        () => affectedWorkflowsData?.affectedWorkflows?.length ?? 0,
        [affectedWorkflowsData]
    );

    const availableUsers = useMemo(() => {
        const allUsers = usersData?.users?.content ?? [];

        return allUsers.filter(
            (user): user is NonNullable<typeof user> & {login: string} =>
                user != null && user.login != null && user.login !== userLogin && user.activated === true
        );
    }, [usersData, userLogin]);

    const handleReassignAll = () => {
        if (!newOwnerLogin) {
            return;
        }

        setReassignmentError(null);

        reassignAll({newOwnerLogin, userLogin, workspaceId: String(workspaceId)});
    };

    const isReassignDisabled = !newOwnerLogin || isReassigning || connections.length === 0;

    // Clear the select + inline error whenever the dialog is closed, so reopening it does not show
    // the previous owner preselected or a stale error banner.
    useEffect(() => {
        if (!open) {
            setNewOwnerLogin('');
            setReassignmentError(null);
        }
    }, [open]);

    return (
        <Dialog onOpenChange={(isOpen) => !isOpen && onClose()} open={open}>
            <DialogContent className="sm:max-w-2xl">
                <div className="flex flex-col gap-4">
                    <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                        <div className="flex flex-col space-y-1">
                            <DialogTitle>Reassign Connections</DialogTitle>

                            <DialogDescription>
                                {`The user ${userLogin} owns ${connections.length} connection${connections.length !== 1 ? 's' : ''} that must be reassigned before removal.`}
                            </DialogDescription>
                        </div>

                        <DialogCloseButton />
                    </DialogHeader>

                    {reassignmentError && (
                        <div className="flex items-start gap-2 rounded-md border border-red-200 bg-surface-destructive-secondary p-3 text-sm text-content-destructive">
                            <AlertTriangleIcon className="mt-0.5 size-4 shrink-0" />

                            <span>{reassignmentError}</span>
                        </div>
                    )}

                    {affectedWorkflowCount > 0 && (
                        <div className="flex items-start gap-2 rounded-md border border-amber-200 bg-surface-warning-secondary p-3 text-sm text-content-warning-primary">
                            <AlertTriangleIcon className="mt-0.5 size-4 shrink-0" />

                            <span>
                                {affectedWorkflowCount} workflow{affectedWorkflowCount !== 1 ? 's' : ''} will be
                                affected by this reassignment.
                            </span>
                        </div>
                    )}

                    <div className="max-h-72 overflow-auto rounded-md border">
                        <Table>
                            <TableHeader>
                                <TableRow className="border-b-border/50">
                                    <TableHead className="sticky top-0 z-10 bg-surface-neutral-primary px-3 py-2 text-left text-xs font-medium tracking-wide text-content-neutral-secondary uppercase">
                                        Connection
                                    </TableHead>

                                    <TableHead className="sticky top-0 z-10 bg-surface-neutral-primary px-3 py-2 text-left text-xs font-medium tracking-wide text-content-neutral-secondary uppercase">
                                        Scope
                                    </TableHead>

                                    <TableHead className="sticky top-0 z-10 bg-surface-neutral-primary px-3 py-2 text-left text-xs font-medium tracking-wide text-content-neutral-secondary uppercase">
                                        Environment
                                    </TableHead>

                                    <TableHead className="sticky top-0 z-10 bg-surface-neutral-primary px-3 py-2 text-right text-xs font-medium tracking-wide text-content-neutral-secondary uppercase">
                                        Workflows
                                    </TableHead>
                                </TableRow>
                            </TableHeader>

                            <TableBody>
                                {isLoadingConnections ? (
                                    <TableRow>
                                        <TableCell className="px-3 py-4 text-center text-muted-foreground" colSpan={4}>
                                            Loading connections...
                                        </TableCell>
                                    </TableRow>
                                ) : connections.length === 0 ? (
                                    <TableRow>
                                        <TableCell className="px-3 py-4 text-center text-muted-foreground" colSpan={4}>
                                            No connections require reassignment.
                                        </TableCell>
                                    </TableRow>
                                ) : (
                                    connections.map((connection) => (
                                        <TableRow className="border-b-border/50" key={connection.connectionId}>
                                            <TableCell className="px-3 py-2 text-sm font-medium">
                                                {connection.connectionName}
                                            </TableCell>

                                            <TableCell className="px-3 py-2">
                                                <ResourceVisibilityBadge visibility={connection.visibility} />
                                            </TableCell>

                                            <TableCell className="px-3 py-2 text-sm text-muted-foreground">
                                                {connection.environmentId}
                                            </TableCell>

                                            <TableCell
                                                className={twMerge(
                                                    'px-3 py-2 text-right text-sm',
                                                    connection.dependentWorkflowCount > 0
                                                        ? 'font-medium text-content-warning-primary'
                                                        : 'text-muted-foreground'
                                                )}
                                            >
                                                {connection.dependentWorkflowCount}
                                            </TableCell>
                                        </TableRow>
                                    ))
                                )}
                            </TableBody>
                        </Table>
                    </div>

                    <div className="flex items-center gap-3">
                        <span className="shrink-0 text-sm font-medium">Reassign all to</span>

                        <Select onValueChange={setNewOwnerLogin} value={newOwnerLogin}>
                            <SelectTrigger className="flex-1">
                                <SelectValue placeholder="Select new owner..." />
                            </SelectTrigger>

                            <SelectContent>
                                {availableUsers.map((user) => (
                                    <SelectItem key={user.login} value={user.login}>
                                        {[user.firstName, user.lastName].filter(Boolean).join(' ') || user.login}

                                        {user.email ? ` (${user.email})` : ''}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    <DialogFooter>
                        <DialogClose asChild>
                            <Button onClick={onClose} variant="outline">
                                Cancel
                            </Button>
                        </DialogClose>

                        <Button disabled={isReassignDisabled} onClick={handleReassignAll}>
                            {isReassigning ? 'Reassigning...' : 'Reassign All'}
                        </Button>
                    </DialogFooter>
                </div>
            </DialogContent>
        </Dialog>
    );
};

export default ConnectionReassignmentDialog;
