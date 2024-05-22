import ComboBox from '@/components/ComboBox';
import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import {CategoryModel, IntegrationModel, TagModel} from '@/shared/middleware/embedded/configuration';
import {ComponentDefinitionBasicModel} from '@/shared/middleware/platform/configuration';
import {
    useCreateIntegrationMutation,
    useUpdateIntegrationMutation,
} from '@/shared/mutations/embedded/integrations.mutations';
import {
    IntegrationCategoryKeys,
    useGetIntegrationCategoriesQuery,
} from '@/shared/queries/embedded/integrationCategories.queries';
import {IntegrationTagKeys, useGetIntegrationTagsQuery} from '@/shared/queries/embedded/integrationTags.quries';
import {IntegrationKeys, useGetIntegrationsQuery} from '@/shared/queries/embedded/integrations.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';

interface IntegrationDialogProps {
    integration: IntegrationModel | undefined;
    onClose?: (integration?: IntegrationModel) => void;
    triggerNode?: ReactNode;
}

const IntegrationDialog = ({integration, onClose, triggerNode}: IntegrationDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const form = useForm<IntegrationModel>({
        defaultValues: {
            allowMultipleInstances: false,
            category: integration?.category
                ? {
                      label: integration?.category?.name,
                      ...integration?.category,
                  }
                : undefined,
            componentName: integration?.componentName || '',
            componentVersion: 1,
            description: integration?.description || '',
            tags:
                integration?.tags?.map((tag: TagModel) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        } as IntegrationModel,
    });

    const {control, getValues, handleSubmit, reset, setValue} = form;

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const {data: categories, error: categoriesError, isLoading: categoriesLoading} = useGetIntegrationCategoriesQuery();

    const {data: integrations} = useGetIntegrationsQuery({});

    const {data: tags, error: tagsError, isLoading: tagsLoading} = useGetIntegrationTagsQuery();

    const queryClient = useQueryClient();

    const onSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: IntegrationCategoryKeys.integrationCategories,
        });
        queryClient.invalidateQueries({
            queryKey: IntegrationKeys.integrations,
        });
        queryClient.invalidateQueries({
            queryKey: IntegrationTagKeys.integrationTags,
        });

        closeDialog();
    };

    const createIntegrationMutation = useCreateIntegrationMutation({
        onSuccess,
    });

    const updateIntegrationMutation = useUpdateIntegrationMutation({
        onSuccess,
    });

    const tagNames = integration?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    function closeDialog() {
        reset();

        setIsOpen(false);

        if (onClose) {
            onClose();
        }
    }

    function saveIntegration() {
        const formData = getValues();

        if (!formData) {
            return;
        }

        const tagValues = formData.tags?.map((tag: TagModel) => {
            return {id: tag.id, name: tag.name, version: tag.version};
        });

        const category = formData?.category?.name ? formData?.category : undefined;

        if (integration?.id) {
            updateIntegrationMutation.mutate({
                ...integration,
                ...formData,
                category,
            } as IntegrationModel);
        } else {
            createIntegrationMutation.mutate({
                ...formData,
                category,
                tags: tagValues,
            } as IntegrationModel);
        }
    }

    return (
        <Dialog
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            open={isOpen}
        >
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent>
                <Form {...form}>
                    <DialogHeader>
                        <DialogTitle>{`${integration?.id ? 'Edit' : 'Create'} Integration`}</DialogTitle>

                        <DialogDescription>
                            {`Use this to ${
                                integration?.id ? 'edit' : 'create'
                            } your integration which will contain related workflows`}
                        </DialogDescription>
                    </DialogHeader>

                    {categoriesError && !categoriesLoading && `An error has occurred: ${categoriesError.message}`}

                    {tagsError && !tagsLoading && `An error has occurred: ${tagsError.message}`}

                    <FormField
                        control={control}
                        name="componentName"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Component</FormLabel>

                                {field.value}

                                <FormControl>
                                    {componentDefinitions && (
                                        <ComboBox
                                            disabled={!!integration?.id}
                                            items={componentDefinitions
                                                .filter(
                                                    (componentDefinition) =>
                                                        integrations?.filter(
                                                            (integration) =>
                                                                integration.componentName === componentDefinition.name
                                                        ).length === 0
                                                )
                                                .map((componentDefinition) => ({
                                                    componentDefinition,
                                                    icon: componentDefinition.icon,
                                                    label: componentDefinition.title!,
                                                    value: componentDefinition.name,
                                                }))}
                                            maxHeight={true}
                                            name="component"
                                            onBlur={field.onBlur}
                                            onChange={(item) =>
                                                setValue(
                                                    'componentName',
                                                    (item?.componentDefinition as ComponentDefinitionBasicModel).name
                                                )
                                            }
                                            value={field.value}
                                        />
                                    )}
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                        rules={{required: true}}
                    />

                    {!categoriesLoading && (
                        <FormField
                            control={control}
                            name="category"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Category</FormLabel>

                                    <FormControl>
                                        <CreatableSelect
                                            field={field}
                                            isMulti={false}
                                            onCreateOption={(inputValue: string) => {
                                                setValue('category', {
                                                    label: inputValue,
                                                    name: inputValue,
                                                    value: inputValue,
                                                    /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
                                                } as any);
                                            }}
                                            options={categories!.map((category: CategoryModel) => ({
                                                label: category.name,
                                                value: category.name.toLowerCase().replace(/\W/g, ''),
                                                ...category,
                                            }))}
                                            placeholder="Marketing, Sales, Social Media..."
                                        />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                    )}

                    <FormField
                        control={control}
                        name="description"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Description</FormLabel>

                                <FormControl>
                                    <Textarea placeholder="Cute description of your integration" rows={5} {...field} />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    {remainingTags && (
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
                                            onCreateOption={(inputValue: string) => {
                                                setValue('tags', [
                                                    ...getValues().tags!,
                                                    {
                                                        label: inputValue,
                                                        name: inputValue,
                                                        value: inputValue,
                                                    },
                                                ] as never[]);
                                            }}
                                            options={remainingTags!.map((tag: TagModel) => {
                                                return {
                                                    label: tag.name,
                                                    value: tag.name.toLowerCase().replace(/\W/g, ''),
                                                    ...tag,
                                                };
                                            })}
                                        />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                    )}

                    <DialogFooter>
                        <DialogClose asChild>
                            <Button type="button" variant="outline">
                                Cancel
                            </Button>
                        </DialogClose>

                        <Button onClick={handleSubmit(saveIntegration)} type="submit">
                            Save
                        </Button>
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default IntegrationDialog;
