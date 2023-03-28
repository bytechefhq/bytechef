import {Close} from '@radix-ui/react-dialog';
import {useQueryClient} from '@tanstack/react-query';
import Button from 'components/Button/Button';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import Dialog from 'components/Dialog/Dialog';
import Input from 'components/Input/Input';
import TextArea from 'components/TextArea/TextArea';
import React, {useState} from 'react';
import {Controller, useForm} from 'react-hook-form';

import {
    CategoryModel,
    IntegrationModel,
    TagModel,
} from '../../../middleware/integration';
import {
    useCreateIntegrationMutation,
    useUpdateIntegrationMutation,
} from '../../../mutations/integrations.mutations';
import {
    IntegrationKeys,
    useGetIntegrationCategoriesQuery,
    useGetIntegrationTagsQuery,
} from '../../../queries/integrations.queries';

interface IntegrationDialogProps {
    integration: IntegrationModel | undefined;
    showTrigger?: boolean;
    visible?: boolean;
    onClose?: () => void;
}

const IntegrationDialog = ({
    integration,
    showTrigger = true,
    visible = false,
    onClose,
}: IntegrationDialogProps) => {
    const [isOpen, setIsOpen] = useState(visible);

    const {
        control,
        formState: {errors, touchedFields},
        handleSubmit,
        getValues,
        register,
        reset,
        setValue,
    } = useForm<IntegrationModel>({
        defaultValues: {
            category: integration?.category
                ? {
                      label: integration?.category?.name,
                      ...integration?.category,
                  }
                : undefined,
            description: integration?.description || '',
            name: integration?.name || '',
            tags:
                integration?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        } as IntegrationModel,
    });

    const {
        isLoading: categoriesLoading,
        error: categoriesError,
        data: categories,
    } = useGetIntegrationCategoriesQuery();

    const {
        isLoading: tagsLoading,
        error: tagsError,
        data: tags,
    } = useGetIntegrationTagsQuery();

    const queryClient = useQueryClient();

    const createIntegrationMutation = useCreateIntegrationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(
                IntegrationKeys.integrationCategories
            );
            queryClient.invalidateQueries(IntegrationKeys.integrations);
            queryClient.invalidateQueries(IntegrationKeys.integrationTags);

            closeDialog();
        },
    });

    const updateIntegrationMutation = useUpdateIntegrationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(
                IntegrationKeys.integrationCategories
            );
            queryClient.invalidateQueries(IntegrationKeys.integrations);
            queryClient.invalidateQueries(IntegrationKeys.integrationTags);

            closeDialog();
        },
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

    function createIntegration() {
        const formData = getValues();

        if (!formData) {
            return;
        }

        const tagValues = formData.tags?.map((tag: TagModel) => {
            return {id: tag.id, name: tag.name, version: tag.version};
        });

        const category = formData?.category?.name
            ? formData?.category
            : undefined;

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
            description={`Use this to ${
                integration?.id ? 'edit' : 'create'
            } your integration which will contain related workflows`}
            isOpen={isOpen}
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            title={`${integration?.id ? 'Edit' : 'Create'} Integration`}
            triggerLabel={
                showTrigger
                    ? `${integration?.id ? 'Edit' : 'Create'} Integration`
                    : undefined
            }
        >
            {categoriesError &&
                !categoriesLoading &&
                `An error has occurred: ${categoriesError.message}`}

            {tagsError &&
                !tagsLoading &&
                `An error has occurred: ${tagsError.message}`}

            <Input
                error={touchedFields.name && !!errors.name}
                label="Name"
                placeholder="My CRM Integration"
                {...register('name', {required: true})}
            />

            <TextArea
                label="Description"
                placeholder="Cute description of your integration"
                {...register('description')}
            />

            {!categoriesLoading && (
                <Controller
                    control={control}
                    name="category"
                    render={({field}) => (
                        <CreatableSelect
                            field={field}
                            isMulti={false}
                            label="Category"
                            options={categories!.map(
                                (category: CategoryModel) => ({
                                    label: `${category.name
                                        .charAt(0)
                                        .toUpperCase()}${category.name.slice(
                                        1
                                    )}`,
                                    value: category.name
                                        .toLowerCase()
                                        .replace(/\W/g, ''),
                                    ...category,
                                })
                            )}
                            placeholder="Marketing, Sales, Social Media..."
                            onCreateOption={(inputValue: string) => {
                                setValue('category', {
                                    label: inputValue,
                                    value: inputValue,
                                    name: inputValue,
                                    /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
                                } as any);
                            }}
                        />
                    )}
                />
            )}

            {remainingTags && (
                <Controller
                    control={control}
                    name="tags"
                    render={({field}) => (
                        <CreatableSelect
                            field={field}
                            isMulti
                            label="Tags"
                            options={remainingTags!.map((tag: TagModel) => {
                                return {
                                    label: `${tag.name
                                        .charAt(0)
                                        .toUpperCase()}${tag.name.slice(1)}`,
                                    value: tag.name
                                        .toLowerCase()
                                        .replace(/\W/g, ''),
                                    ...tag,
                                };
                            })}
                            onCreateOption={(inputValue: string) => {
                                setValue('tags', [
                                    ...getValues().tags!,
                                    {
                                        label: inputValue,
                                        value: inputValue,
                                        name: inputValue,
                                    },
                                ] as never[]);
                            }}
                        />
                    )}
                />
            )}

            <div className="mt-8 flex justify-end space-x-1">
                <Close asChild>
                    <Button
                        displayType="lightBorder"
                        label="Cancel"
                        type="button"
                    />
                </Close>

                <Button
                    label="Save"
                    type="submit"
                    onClick={handleSubmit(createIntegration)}
                />
            </div>
        </Dialog>
    );
};

export default IntegrationDialog;
