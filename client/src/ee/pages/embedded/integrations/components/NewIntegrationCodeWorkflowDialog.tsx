import Button from '@/components/Button/Button';
import ComboBox from '@/components/ComboBox/ComboBox';
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
import {IntegrationApi} from '@/ee/shared/middleware/embedded/configuration';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {useGetIntegrationCategoriesQuery} from '@/ee/shared/queries/embedded/integrationCategories.queries';
import {useGetIntegrationTagsQuery} from '@/ee/shared/queries/embedded/integrationTags.quries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import {CodeWorkflowLanguage, useCreateIntegrationCodeWorkflowMutation} from '@/shared/middleware/graphql';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {useForm} from 'react-hook-form';
import {useNavigate} from 'react-router-dom';
import {z} from 'zod';

const formSchema = z.object({
    categoryId: z.string().optional(),
    componentName: z.string().min(1, 'Component is required'),
    description: z.string().optional(),
    language: z.enum(CodeWorkflowLanguage),
    name: z.string().optional(),
    permissionExpression: z.string().optional(),
    tags: z.array(z.object({label: z.string(), value: z.string()})).optional(),
});

interface NewIntegrationCodeWorkflowDialogProps {
    onClose: () => void;
}

const NewIntegrationCodeWorkflowDialog = ({onClose}: NewIntegrationCodeWorkflowDialogProps) => {
    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            categoryId: undefined,
            componentName: '',
            description: '',
            language: CodeWorkflowLanguage.Javascript,
            name: '',
            permissionExpression: '',
            tags: [],
        },
        resolver: zodResolver(formSchema),
    });

    const {control, getValues, handleSubmit, setValue} = form;

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});
    const {data: categories} = useGetIntegrationCategoriesQuery();
    const {data: tags} = useGetIntegrationTagsQuery();

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const createIntegrationCodeWorkflowMutation = useCreateIntegrationCodeWorkflowMutation({
        onSuccess: async (data) => {
            const integrationId = Number(data.createIntegrationCodeWorkflow);

            const integration = await queryClient.fetchQuery({
                queryFn: () => new IntegrationApi().getIntegration({id: integrationId}),
                queryKey: IntegrationKeys.integration(integrationId),
            });

            onClose();

            navigate(
                `/embedded/integrations/${integrationId}/integration-workflows/${integration.integrationWorkflowIds![0]}`
            );
        },
    });

    const handleCreateIntegrationCodeWorkflow = () => {
        const {categoryId, componentName, description, language, name, permissionExpression, tags} = getValues();

        createIntegrationCodeWorkflowMutation.mutate({
            categoryId: categoryId || undefined,
            componentName,
            description: description || undefined,
            language,
            name: name || undefined,
            permissionExpression: permissionExpression || undefined,
            tags: tags?.map((tag) => tag.label),
        });
    };

    return (
        <Dialog onOpenChange={(open) => !open && !createIntegrationCodeWorkflowMutation.isPending && onClose()} open>
            <DialogContent
                onInteractOutside={(event) => createIntegrationCodeWorkflowMutation.isPending && event.preventDefault()}
            >
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(handleCreateIntegrationCodeWorkflow)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <div className="flex flex-col space-y-1">
                                <DialogTitle>New Code Integration</DialogTitle>

                                <DialogDescription>
                                    Create a code-backed integration and start editing its source.
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

                                            <SelectItem value={CodeWorkflowLanguage.Ruby}>Ruby</SelectItem>
                                        </SelectContent>
                                    </Select>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={control}
                            name="componentName"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Component</FormLabel>

                                    <FormControl>
                                        {componentDefinitions && (
                                            <ComboBox
                                                items={componentDefinitions.map((componentDefinition) => ({
                                                    icon: componentDefinition.icon,
                                                    label: componentDefinition.title!,
                                                    value: componentDefinition.name,
                                                }))}
                                                maxHeight={true}
                                                name="componentName"
                                                onBlur={field.onBlur}
                                                onChange={(item) => {
                                                    setValue('componentName', item?.value ?? '');

                                                    // Prefilled from the component, as the plain
                                                    // create-integration dialog does — the name
                                                    // defaults to the component anyway, so
                                                    // leaving it blank hides that from the user.
                                                    const selectedComponentDefinition = componentDefinitions?.find(
                                                        (componentDefinition) =>
                                                            componentDefinition.name === item?.value
                                                    );

                                                    setValue('name', selectedComponentDefinition?.title ?? '');
                                                }}
                                                value={field.value}
                                            />
                                        )}
                                    </FormControl>

                                    <FormDescription>
                                        The component this integration is built on. The source declares the same
                                        component name, and changing it by editing the source is not supported.
                                    </FormDescription>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={control}
                            name="name"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Name</FormLabel>

                                    <FormControl>
                                        <Input {...field} />
                                    </FormControl>

                                    <FormDescription>
                                        Defaults to the component name. Name it yourself when you keep more than one
                                        integration on the same component.
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
                                        <Textarea
                                            placeholder="Cute description of your integration"
                                            rows={3}
                                            {...field}
                                        />
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
                            name="permissionExpression"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Permission Expression</FormLabel>

                                    <FormControl>
                                        <Textarea placeholder="e.g. metadata['plan'] == 'pro'" rows={2} {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

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
                                disabled={createIntegrationCodeWorkflowMutation.isPending}
                                onClick={onClose}
                                type="button"
                                variant="ghost"
                            >
                                Cancel
                            </Button>

                            <Button disabled={createIntegrationCodeWorkflowMutation.isPending} type="submit">
                                Create
                            </Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default NewIntegrationCodeWorkflowDialog;
