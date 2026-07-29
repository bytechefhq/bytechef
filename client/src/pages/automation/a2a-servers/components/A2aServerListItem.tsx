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

interface A2aServerListItemProps {
    a2aServer: A2aServer;
}

const A2aServerListItem = ({a2aServer}: A2aServerListItemProps) => {
    const [editDialogOpen, setEditDialogOpen] = useState(false);
    const [skillsDialogOpen, setSkillsDialogOpen] = useState(false);

    const queryClient = useQueryClient();

    const deleteA2aServerMutation = useDeleteA2aServerMutation();

    const agentCardUrl = `${window.location.origin}/api/automation/a2a/${a2aServer.secretKey}/.well-known/agent-card.json`;

    const handleDeleteClick = () => {
        deleteA2aServerMutation.mutate(
            {id: a2aServer.id},
            {
                onSuccess: () => queryClient.invalidateQueries({queryKey: ['a2aServers']}),
            }
        );
    };

    return (
        <div className="flex items-center justify-between rounded-md border border-border/50 bg-background p-4">
            <div className="flex flex-col gap-1">
                <div className="flex items-center gap-2">
                    <span className="font-semibold">{a2aServer.name}</span>

                    <Badge
                        label={a2aServer.enabled ? 'Enabled' : 'Disabled'}
                        styleType={a2aServer.enabled ? 'success-filled' : 'secondary-filled'}
                    />

                    {a2aServer.authenticationRequired && <Badge label="Auth required" styleType="secondary-outline" />}
                </div>

                {a2aServer.description && <span className="text-sm text-muted-foreground">{a2aServer.description}</span>}

                <span className="mt-1 font-mono text-xs break-all text-muted-foreground">{agentCardUrl}</span>
            </div>

            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => setSkillsDialogOpen(true)}>Manage Skills</DropdownMenuItem>

                    <DropdownMenuItem onClick={() => setEditDialogOpen(true)}>Edit</DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem className="text-destructive" onClick={handleDeleteClick}>
                        Delete
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            <A2aServerDialog
                a2aServer={a2aServer}
                onOpenChange={setEditDialogOpen}
                open={editDialogOpen}
                triggerNode={<span className="hidden" />}
            />

            <A2aServerWorkflowDialog
                a2aServer={a2aServer}
                onOpenChange={setSkillsDialogOpen}
                open={skillsDialogOpen}
            />
        </div>
    );
};

export default A2aServerListItem;
