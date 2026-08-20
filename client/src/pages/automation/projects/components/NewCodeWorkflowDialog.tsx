import Button from '@/components/Button/Button';
import CreatableSelect from '@/components/CreatableSelect/CreatableSelect';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ProjectApi} from '@/shared/middleware/automation/configuration';
import {CodeWorkflowLanguage, useCreateCodeWorkflowMutation} from '@/shared/middleware/graphql';
import {useGetProjectCategoriesQuery} from '@/shared/queries/automation/projectCategories.queries';
import {useGetProjectTagsQuery} from '@/shared/queries/automation/projectTags.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {useForm} from 'react-hook-form';
import {useNavigate} from 'react-router-dom';
import {z} from 'zod';

const formSchema = z.object({
    categoryId: z.string().optional(),
    description: z.string().optional(),
    language: z.enum(CodeWorkflowLanguage),
    name: z.string().min(1, 'Name is required').max(256, 'Name cannot be longer than 256 characters'),
    tags: z.array(z.object({label: z.string(), value: z.string()})).optional(),
});

interface NewCodeWorkflowDialogProps {
    onClose: () => void;
}

const NewCodeWorkflowDialog = ({onClose}: NewCodeWorkflowDialogProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            categoryId: undefined,
            description: '',
            language: CodeWorkflowLanguage.Javascript,
            name: '',
            tags: [],
        },
        resolver: zodResolver(formSchema),
    });

    const {control, getValues, handleSubmit, setValue} = form;

    const {data: categories} = useGetProjectCategoriesQuery();
    const {data: tags} = useGetProjectTagsQuery(currentWorkspaceId!);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const createCodeWorkflowMutation = useCreateCodeWorkflowMutation({
        onSuccess: async (data) => {
            const projectId = Number(data.createCodeWorkflow);

            const project = await queryClient.fetchQuery({
                queryFn: () => new ProjectApi().getProject({id: projectId}),
                queryKey: ProjectKeys.project(projectId),
            });

            onClose();

            navigate(`/automation/projects/${projectId}/project-workflows/${project.projectWorkflowIds![0]}`);
        },
    });

    const handleCreateCodeWorkflow = () => {
        const {categoryId, description, language, name, tags} = getValues();

        createCodeWorkflowMutation.mutate({
            categoryId: categoryId || undefined,
            description: description || undefined,
            language,
            name,
            tags: tags?.map((tag) => tag.label),
            workspaceId: currentWorkspaceId!,
        });
    };

    return (
        <Dialog onOpenChange={(open) => !open && !createCodeWorkflowMutation.isPending && onClose()} open>
            <DialogContent
                onInteractOutside={(event) => createCodeWorkflowMutation.isPending && event.preventDefault()}
            >
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(handleCreateCodeWorkflow)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <div className="flex flex-col space-y-1">
                                <DialogTitle>New Code Project</DialogTitle>

                                <DialogDescription>
                                    Create a code-backed project and start editing its source.
                                </DialogDescription>
                            </div>

                            <DialogCloseButton />
                        </DialogHeader>

                        <FormField
                            control={control}
                            name="language"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Language</FormLabel>

                                    <Select onValueChange={field.onChange} value={field.value}>
                                        <FormControl>
                                            <SelectTrigger>
                                                <SelectValue />
                                            </SelectTrigger>
                                        </FormControl>

                                        <SelectContent>
                                            <SelectItem value={CodeWorkflowLanguage.Javascript}>JavaScript</SelectItem>

                                            <SelectItem value={CodeWorkflowLanguage.Python}>Python</SelectItem>

                                            {/* RUBY-DISABLED: org.graalvm.polyglot:ruby is published only up to
                                            25.0.0 and crashes on the Truffle 25.2.4 the server pins, so Ruby cannot
                                            be offered. Re-enable once a polyglot ruby jar built on Truffle 25.2+
                                            ships (or GraalVM is downgraded). Grep RUBY-DISABLED.

                                            <SelectItem value={CodeWorkflowLanguage.Ruby}>Ruby</SelectItem> */}
                                        </SelectContent>
                                    </Select>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={control}
                            name="name"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Project Name</FormLabel>

                                    <FormControl>
                                        <Input {...field} />
                                    </FormControl>

                                    <FormDescription>
                                        A unique name identifying the project. The source declares it too, and renaming
                                        by editing the source is not supported.
                                    </FormDescription>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={control}
                            name="description"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Description</FormLabel>

                                    <FormControl>
                                        <Textarea placeholder="Cute description of your project" rows={3} {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        {categories && categories.length > 0 && (
                            <FormField
                                control={control}
                                name="categoryId"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Category</FormLabel>

                                        <Select defaultValue={field.value} onValueChange={field.onChange}>
                                            <FormControl>
                                                <SelectTrigger>
                                                    <SelectValue placeholder="Marketing, Sales, Social Media..." />
                                                </SelectTrigger>
                                            </FormControl>

                                            <SelectContent>
                                                {categories.map((category) => (
                                                    <SelectItem key={category.id} value={String(category.id)}>
                                                        {category.name}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        )}

                        <FormField
                            control={control}
                            name="tags"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Tags</FormLabel>

                                    <FormControl>
                                        <CreatableSelect
                                            field={field}
                                            isMulti
                                            onCreateOption={(inputValue: string) =>
                                                setValue('tags', [
                                                    ...(getValues().tags ?? []),
                                                    {label: inputValue, value: inputValue},
                                                ])
                                            }
                                            options={(tags ?? []).map((tag) => ({
                                                label: tag.name,
                                                value: tag.name,
                                            }))}
                                        />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <DialogFooter>
                            <Button
                                disabled={createCodeWorkflowMutation.isPending}
                                onClick={onClose}
                                type="button"
                                variant="ghost"
                            >
                                Cancel
                            </Button>

                            <Button disabled={createCodeWorkflowMutation.isPending} type="submit">
                                Create
                            </Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default NewCodeWorkflowDialog;
