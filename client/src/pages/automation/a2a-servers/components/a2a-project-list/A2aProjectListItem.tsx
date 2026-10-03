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
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {A2aServer, useDeleteA2aProjectMutation} from '@/shared/middleware/graphql';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon, WorkflowIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

import A2aServerWorkflowDialog from '../A2aServerWorkflowDialog';
import {A2aProjectItemType} from './hooks/useA2aProjectList';

interface A2aProjectListItemProps {
    a2aProject: A2aProjectItemType;
    a2aServer: A2aServer;
}

const A2aProjectListItem = ({a2aProject, a2aServer}: A2aProjectListItemProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditSkillsDialog, setShowEditSkillsDialog] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data: projects} = useGetWorkspaceProjectsQuery({
        apiCollections: false,
        id: currentWorkspaceId!,
        includeAllFields: false,
    });

    const deleteA2aProjectMutation = useDeleteA2aProjectMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['a2aProjectsByServerId']});

            setShowDeleteDialog(false);
        },
    });

    const projectName = useMemo(
        () => projects?.find((project) => Number(project.id) === Number(a2aProject.projectId))?.name,
        [projects, a2aProject.projectId]
    );

    const skillCount = a2aProject.workflowIds?.length || 0;

    return (
        <div className="flex items-center gap-2.5 rounded-md border border-border/50 px-3 py-2.5">
            <WorkflowIcon className="size-5 shrink-0 text-content-neutral-secondary" />

            <span className="min-w-0 flex-1 truncate text-sm font-medium">
                {projectName || `Project #${a2aProject.projectId}`}
            </span>

            {a2aProject.projectVersion && (
                <Badge label={`v${a2aProject.projectVersion}`} styleType="secondary-filled" weight="semibold" />
            )}

            <span className="text-xs text-muted-foreground">
                {skillCount === 1 ? '1 skill' : `${skillCount} skills`}
            </span>

            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button
                        aria-label="Project actions"
                        icon={<EllipsisVerticalIcon />}
                        size="iconSm"
                        variant="ghost"
                    />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => setShowEditSkillsDialog(true)}>Edit Skills</DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                        disabled={deleteA2aProjectMutation.isPending}
                        onClick={() => setShowDeleteDialog(true)}
                        variant="destructive"
                    >
                        Delete
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            <A2aServerWorkflowDialog
                a2aProject={a2aProject}
                a2aServer={a2aServer}
                onOpenChange={setShowEditSkillsDialog}
                open={showEditSkillsDialog}
            />

            <AlertDialog
                isPending={deleteA2aProjectMutation.isPending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={() => deleteA2aProjectMutation.mutate({id: a2aProject.id})}
                open={showDeleteDialog}
            />
        </div>
    );
};

export default A2aProjectListItem;
