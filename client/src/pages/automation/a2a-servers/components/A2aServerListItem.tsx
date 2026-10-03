import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {A2aServer, useDeleteA2aServerMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';
import {useState} from 'react';

import A2aServerDialog from './A2aServerDialog';
import A2aServerWorkflowDialog from './A2aServerWorkflowDialog';
import A2aProjectList from './a2a-project-list/A2aProjectList';

interface A2aServerListItemProps {
    a2aServer: A2aServer;
}

const A2aServerListItem = ({a2aServer}: A2aServerListItemProps) => {
    const [addProjectDialogOpen, setAddProjectDialogOpen] = useState(false);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [editDialogOpen, setEditDialogOpen] = useState(false);

    const queryClient = useQueryClient();

    const deleteA2aServerMutation = useDeleteA2aServerMutation();

    const agentCardUrl = a2aServer.secretKey
        ? `${window.location.origin}/api/automation/a2a/${a2aServer.secretKey}/.well-known/agent-card.json`
        : undefined;

    const handleDelete = () => {
        deleteA2aServerMutation.mutate(
            {id: a2aServer.id},
            {
                onSuccess: () => {
                    void queryClient.invalidateQueries({queryKey: ['a2aServers']});

                    setDeleteDialogOpen(false);
                },
            }
        );
    };

    return (
        <div className="flex flex-col gap-3 rounded-md border border-border/50 bg-background p-4">
            <div className="flex items-center justify-between">
                <div className="flex flex-col gap-1">
                    <div className="flex items-center gap-2">
                        <span className="font-semibold">{a2aServer.name}</span>

                        <Badge
                            label={a2aServer.enabled ? 'Enabled' : 'Disabled'}
                            styleType={a2aServer.enabled ? 'success-filled' : 'secondary-filled'}
                        />

                        {a2aServer.authenticationRequired && (
                            <Badge label="Auth required" styleType="secondary-outline" />
                        )}
                    </div>

                    {a2aServer.description && (
                        <span className="text-sm text-muted-foreground">{a2aServer.description}</span>
                    )}

                    {agentCardUrl && (
                        <span className="mt-1 font-mono text-xs break-all text-muted-foreground">{agentCardUrl}</span>
                    )}
                </div>

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button
                            aria-label="Server actions"
                            icon={<EllipsisVerticalIcon />}
                            size="icon"
                            variant="ghost"
                        />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => setAddProjectDialogOpen(true)}>Add Project</DropdownMenuItem>

                        <DropdownMenuItem onClick={() => setEditDialogOpen(true)}>Edit</DropdownMenuItem>

                        <DropdownMenuSeparator />

                        <DropdownMenuItem onClick={() => setDeleteDialogOpen(true)} variant="destructive">
                            Delete
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            <A2aProjectList a2aServer={a2aServer} />

            <A2aServerDialog
                a2aServer={a2aServer}
                onOpenChange={setEditDialogOpen}
                open={editDialogOpen}
                triggerNode={<span className="hidden" />}
            />

            <A2aServerWorkflowDialog
                a2aServer={a2aServer}
                onOpenChange={setAddProjectDialogOpen}
                open={addProjectDialogOpen}
            />

            <AlertDialog
                isPending={deleteA2aServerMutation.isPending}
                onCancel={() => setDeleteDialogOpen(false)}
                onConfirm={handleDelete}
                open={deleteDialogOpen}
            />
        </div>
    );
};

export default A2aServerListItem;
