import Button from '@/components/Button/Button';
import ComboBox from '@/components/ComboBox/ComboBox';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Textarea} from '@/components/ui/textarea';
import {Category, Integration, Tag} from '@/ee/shared/middleware/embedded/configuration';
import {
    useCreateIntegrationMutation,
    useUpdateIntegrationMutation,
} from '@/ee/shared/mutations/embedded/integrations.mutations';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {
    IntegrationCategoryKeys,
    useGetIntegrationCategoriesQuery,
} from '@/ee/shared/queries/embedded/integrationCategories.queries';
import {IntegrationTagKeys, useGetIntegrationTagsQuery} from '@/ee/shared/queries/embedded/integrationTags.quries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useQueryClient} from '@tanstack/react-query';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';

interface IntegrationDialogProps {
    integration: Integration | undefined;
    onClose?: (integration?: Integration) => void;
    triggerNode?: ReactNode;
}

const IntegrationDialog = ({integration, onClose, triggerNode}: IntegrationDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const {captureIntegrationCreated} = useAnalytics();

    const form = useForm<Integration>({
        defaultValues: {
            category: integration?.category
                ? {
                      label: integration?.category?.name,
                      ...integration?.category,
                  }
                : undefined,
            componentName: integration?.componentName || '',
            description: integration?.description || '',
            multipleInstances: false,
            name: integration?.name || '',
            tags:
                integration?.tags?.map((tag: Tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        } as Integration,
    });

    const {control, getValues, handleSubmit, reset, setValue} = form;

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const {data: categories, error: categoriesError, isLoading: categoriesLoading} = useGetIntegrationCategoriesQuery();

    const {data: tags, error: tagsError, isLoading: tagsLoading} = useGetIntegrationTagsQuery();

    const queryClient = useQueryClient();

    const onSuccess = (integrationId: number | void) => {
        captureIntegrationCreated();

        if (!integrationId && integration) {
            integrationId = integration.id!;
        }

        if (integrationId) {
            queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integration(integrationId),
            });
        }

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

        const tagValues = formData.tags?.map((tag: Tag) => {
            return {id: tag.id, name: tag.name, version: tag.version};
        });

        const category = formData?.category?.name ? formData?.category : undefined;

        if (integration?.id) {
            updateIntegrationMutation.mutate({
                ...integration,
                ...formData,
                category,
            } as Integration);
        } else {
            createIntegrationMutation.mutate({
                ...formData,
                category,
                tags: tagValues,
            } as Integration);
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
                    <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                        <div className="flex flex-col space-y-1">
                            <DialogTitle>{`${integration?.id ? 'Edit' : 'Create'} Integration`}</DialogTitle>

                            <DialogDescription>
                                {`Use this to ${
                                    integration?.id ? 'edit' : 'create'
                                } your integration which will contain workflows`}
                            </DialogDescription>
                        </div>

                        <DialogCloseButton />
                    </DialogHeader>

                    {categoriesError && !categoriesLoading && `An error has occurred: ${categoriesError.message}`}

                    {tagsError && !tagsLoading && `An error has occurred: ${tagsError.message}`}

                    <FormField
                        control={control}
                        name="componentName"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Component</FormLabel>

                                <FormControl>
                                    {componentDefinitions && (
                                        <ComboBox
                                            disabled={!!integration?.id}
                                            items={componentDefinitions.map((componentDefinition) => ({
                                                componentDefinition,
                                                icon: componentDefinition.icon,
                                                label: componentDefinition.title!,
                                                value: componentDefinition.name,
                                            }))}
                                            maxHeight={true}
                                            name="component"
                                            onBlur={field.onBlur}
                                            onChange={(item) => {
                                                const componentName = (
                                                    item?.componentDefinition as ComponentDefinitionBasic
                                                ).name;
                                                const title = (item?.componentDefinition as ComponentDefinitionBasic)
                                                    .title;

                                                setValue('componentName', componentName);
                                                setValue('name', title);
                                            }}
                                            value={field.value}
                                        />
                                    )}
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                        rules={{required: true}}
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
                                            options={categories!.map((category: Category) => ({
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
                                            options={remainingTags!.map((tag: Tag) => {
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
                            <Button label="Cancel" type="button" variant="outline" />
                        </DialogClose>

                        <Button label="Save" onClick={handleSubmit(saveIntegration)} type="submit" />
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default IntegrationDialog;
