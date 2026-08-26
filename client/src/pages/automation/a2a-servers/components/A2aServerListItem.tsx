import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import EEVersion from '@/shared/edition/EEVersion';
import {
    A2aServer,
    PromotionResourceType,
    useDeleteA2aServerMutation,
    useEnvironmentsQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';
import {Suspense, lazy, useState} from 'react';

import A2aServerDialog from './A2aServerDialog';
import A2aServerWorkflowDialog from './A2aServerWorkflowDialog';

const EnvironmentPromotionDialog = lazy(
    () => import('@/ee/shared/components/environment-promotion/EnvironmentPromotionDialog')
);

interface A2aServerListItemProps {
    a2aServer: A2aServer;
}

const A2aServerListItem = ({a2aServer}: A2aServerListItemProps) => {
    const [editDialogOpen, setEditDialogOpen] = useState(false);
    const [showPromotionDialog, setShowPromotionDialog] = useState(false);
    const [skillsDialogOpen, setSkillsDialogOpen] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const deleteA2aServerMutation = useDeleteA2aServerMutation();

    const environmentsQuery = useEnvironmentsQuery();

    const showPromoteToEnvironment = (environmentsQuery.data?.environments?.length ?? 0) >= 2;

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

                {a2aServer.description && (
                    <span className="text-sm text-muted-foreground">{a2aServer.description}</span>
                )}

                <span className="mt-1 font-mono text-xs break-all text-muted-foreground">{agentCardUrl}</span>
            </div>

            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => setSkillsDialogOpen(true)}>Manage Skills</DropdownMenuItem>

                    <DropdownMenuItem onClick={() => setEditDialogOpen(true)}>Edit</DropdownMenuItem>

                    {showPromoteToEnvironment && (
                        <EEVersion hidden={true}>
                            <DropdownMenuItem onClick={() => setShowPromotionDialog(true)}>
                                Promote to environment…
                            </DropdownMenuItem>
                        </EEVersion>
                    )}

                    <DropdownMenuSeparator />

                    <DropdownMenuItem onClick={handleDeleteClick} variant="destructive">
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

            <A2aServerWorkflowDialog a2aServer={a2aServer} onOpenChange={setSkillsDialogOpen} open={skillsDialogOpen} />

            {showPromotionDialog && (
                <EEVersion hidden={true}>
                    <Suspense fallback={null}>
                        <EnvironmentPromotionDialog
                            onClose={() => setShowPromotionDialog(false)}
                            onPromoted={() => {
                                queryClient.invalidateQueries({queryKey: ['a2aServers']});
                            }}
                            resourceType={PromotionResourceType.A2AServer}
                            sourceEnvironmentId={+a2aServer.environmentId}
                            sourceId={a2aServer.id}
                            sourceName={a2aServer.name}
                            workspaceId={currentWorkspaceId!}
                        />
                    </Suspense>
                </EEVersion>
            )}
        </div>
    );
};

export default A2aServerListItem;
