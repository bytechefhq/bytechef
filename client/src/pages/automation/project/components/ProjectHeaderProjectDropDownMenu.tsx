import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useToast} from '@/components/ui/use-toast';
import {ProjectModel} from '@/shared/middleware/automation/configuration';
import {useDuplicateProjectMutation} from '@/shared/mutations/automation/projects.mutations';
import {useCreateProjectWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {SettingsIcon} from 'lucide-react';
import {ChangeEvent, useRef} from 'react';
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
    const hiddenFileInputRef = useRef<HTMLInputElement>(null);

    const navigate = useNavigate();

    const {toast} = useToast();

    const queryClient = useQueryClient();

    const duplicateProjectMutation = useDuplicateProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            navigate(`/automation/projects/${project?.id}/project-workflows/${project?.projectWorkflowIds![0]}`);
        },
    });

    const importProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.project(project.id!)});

            if (hiddenFileInputRef.current) {
                hiddenFileInputRef.current.value = '';
            }

            toast({
                description: 'Workflow is imported.',
            });
        },
    });

    const handleFileChange = async (e: ChangeEvent<HTMLInputElement>) => {
        if (e.target.files) {
            importProjectWorkflowMutation.mutate({
                id: project.id!,
                workflowModel: {
                    definition: await e.target.files[0].text(),
                },
            });
        }
    };

    return (
        <>
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

                    <DropdownMenuItem
                        onClick={() => {
                            if (hiddenFileInputRef.current) {
                                hiddenFileInputRef.current.click();
                            }
                        }}
                    >
                        Import Workflow
                    </DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem className="text-destructive" onClick={() => onDelete()}>
                        Delete
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            <input className="hidden" onChange={handleFileChange} ref={hiddenFileInputRef} type="file" />
        </>
    );
};

export default ProjectHeaderProjectDropDownMenu;
