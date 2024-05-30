import {Button} from '@/components/ui/button';
import {Label} from '@/components/ui/label';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useToast} from '@/components/ui/use-toast';
import {ProjectModel} from '@/shared/middleware/automation/configuration';
import {usePublishProjectMutation} from '@/shared/mutations/automation/projects.mutations';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {CircleDotIcon} from 'lucide-react';
import {useState} from 'react';

const ProjectPublishPopover = ({project}: {project: ProjectModel}) => {
    const [open, setOpen] = useState(false);
    const [description, setDescription] = useState<string | undefined>(undefined);

    const {toast} = useToast();

    const queryClient = useQueryClient();

    const publishProjectMutation = usePublishProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.project(project.id!),
            });

            toast({
                description: 'The project is published.',
            });

            setOpen(false);
        },
    });

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <PopoverTrigger asChild>
                <Button className="hover:bg-gray-200" disabled={!!project?.publishedDate} size="icon" variant="ghost">
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <CircleDotIcon className="h-5" />
                        </TooltipTrigger>

                        <TooltipContent>
                            {project?.publishedDate ? `The project is published` : `Publish the project`}
                        </TooltipContent>
                    </Tooltip>
                </Button>
            </PopoverTrigger>

            <PopoverContent align="end" className="flex h-full w-96 flex-col justify-between space-y-4">
                <h3 className="font-semibold">Publish Project</h3>

                <div className="flex-1">
                    <Label>Description</Label>

                    <Textarea className="h-28" onChange={(event) => setDescription(event.target.value)}></Textarea>
                </div>

                <div className="flex justify-end">
                    <Button
                        disabled={!!project?.publishedDate}
                        onClick={() =>
                            publishProjectMutation.mutate({
                                id: project.id!,
                                publishProjectRequestModel: {
                                    description,
                                },
                            })
                        }
                        size="sm"
                    >
                        Publish
                    </Button>
                </div>
            </PopoverContent>
        </Popover>
    );
};

export default ProjectPublishPopover;
