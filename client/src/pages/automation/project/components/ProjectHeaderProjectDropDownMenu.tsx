import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ProjectModel} from '@/middleware/automation/configuration';
import {useDuplicateProjectMutation} from '@/mutations/automation/projects.mutations';
import {ProjectKeys} from '@/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {SettingsIcon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

const ProjectHeaderProjectDropDownMenu = ({
    onDelete,
    onEdit,
    project,
}: {
    onDelete: () => void;
    onEdit: () => void;
    project: ProjectModel;
}) => {
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const duplicateProjectMutation = useDuplicateProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            navigate(`/automation/projects/${project?.id}/workflows/${project?.workflowIds![0]}`);
        },
    });

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <div>
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button className="hover:bg-gray-200" size="icon" variant="ghost">
                                <SettingsIcon className="h-5" />
                            </Button>
                        </TooltipTrigger>

                        <TooltipContent>Project Settings</TooltipContent>
                    </Tooltip>
                </div>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => onEdit()}>Edit</DropdownMenuItem>

                {project && (
                    <DropdownMenuItem onClick={() => duplicateProjectMutation.mutate(project.id!)}>
                        Duplicate
                    </DropdownMenuItem>
                )}

                <DropdownMenuSeparator />

                <DropdownMenuItem className="text-red-600" onClick={() => onDelete()}>
                    Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectHeaderProjectDropDownMenu;
