import LoadingIcon from '@/components/LoadingIcon';
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
import {SendIcon} from 'lucide-react';
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
                queryKey: ProjectKeys.filteredProjects({id: currentWorkspaceId!}),
            });

            toast({
                description: 'The project has been published.',
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
            <Tooltip>
                <PopoverTrigger asChild>
                    <TooltipTrigger asChild>
                        <Button
                            className="shadow-none hover:border-stroke-neutral-primary-hover hover:bg-surface-neutral-primary-hover active:border-stroke-brand-primary-pressed active:bg-surface-brand-secondary active:text-content-brand-primary-pressed [&[data-state=open]]:border-stroke-brand-primary-pressed [&[data-state=open]]:bg-surface-brand-secondary [&[data-state=open]]:text-content-brand-primary-pressed"
                            variant="outline"
                        >
                            <SendIcon /> Publish
                        </Button>
                    </TooltipTrigger>
                </PopoverTrigger>

                <TooltipContent>Publish the project</TooltipContent>
            </Tooltip>

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
                            <Button
                                className="bg-surface-brand-primary shadow-none hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-pressed"
                                disabled={publishProjectMutation.isPending}
                                size="sm"
                                type="submit"
                            >
                                {publishProjectMutation.isPending && <LoadingIcon />}
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
