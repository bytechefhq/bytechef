import {Button} from '@/components/ui/button';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useToast} from '@/hooks/use-toast';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {Project} from '@/shared/middleware/automation/configuration';
import {usePublishProjectMutation} from '@/shared/mutations/automation/projects.mutations';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {CircleDotIcon} from 'lucide-react';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import * as z from 'zod';

const formSchema = z.object({
    description: z.string().max(256).optional(),
});

const ProjectHeaderPublishPopover = ({project}: {project: Project}) => {
    const [open, setOpen] = useState(false);

    const {currentWorkspaceId} = useWorkspaceStore();

    const {captureProjectPublished} = useAnalytics();

    const {toast} = useToast();

    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
    });

    const {control, handleSubmit, reset} = form;

    const queryClient = useQueryClient();

    const publishProjectMutation = usePublishProjectMutation({
        onSuccess: () => {
            captureProjectPublished();

            queryClient.invalidateQueries({
                queryKey: ProjectKeys.project(project.id!),
            });

            queryClient.invalidateQueries({
                queryKey: ProjectKeys.filteredProjects({id: currentWorkspaceId}),
            });

            toast({
                description: 'The project is published.',
            });

            reset({description: undefined});

            setOpen(false);
        },
    });

    function publishProject({description}: {description?: string}) {
        publishProjectMutation.mutate({
            id: project.id!,
            publishProjectRequest: {
                description,
            },
        });
    }

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <PopoverTrigger asChild>
                <Button className="hover:bg-muted" size="icon" variant="ghost">
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <CircleDotIcon className="h-5" />
                        </TooltipTrigger>

                        <TooltipContent>Publish the project</TooltipContent>
                    </Tooltip>
                </Button>
            </PopoverTrigger>

            <PopoverContent align="end" className="flex h-full w-96 flex-col justify-between space-y-4">
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(publishProject)}>
                        <h3 className="font-semibold">Publish Project</h3>

                        <div className="flex-1">
                            <FormField
                                control={control}
                                name="description"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Description</FormLabel>

                                        <FormControl>
                                            <Textarea className="h-28" {...field}></Textarea>
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>

                        <div className="flex justify-end">
                            <Button size="sm" type="submit">
                                Publish
                            </Button>
                        </div>
                    </form>
                </Form>
            </PopoverContent>
        </Popover>
    );
};

export default ProjectHeaderPublishPopover;
