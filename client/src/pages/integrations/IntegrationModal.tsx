import React, {useEffect, useState} from 'react';

import Input from 'components/Input/Input';
import Modal from 'components/Modal/Modal';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import TextArea from 'components/TextArea/TextArea';
import {Controller, useForm} from 'react-hook-form';
import Button from 'components/Button/Button';
import {
    IntegrationKeys,
    useGetIntegrationCategoriesQuery,
    useGetIntegrationTagsQuery,
} from '../../queries/integrations';
import {useQueryClient} from '@tanstack/react-query';
import {
    CategoryModel,
    IntegrationModel,
    TagModel,
} from '../../middleware/integration';
import {
    useIntegrationMutation,
    useIntegrationPutMutation,
} from '../../mutations/integrations.mutations';

interface IntegrationModalProps {
    id?: number;
    integrationItem: IntegrationModel | undefined;
    visible?: boolean;
}

const IntegrationModal = ({
    id,
    integrationItem,
    visible = false,
}: IntegrationModalProps) => {
    const [isOpen, setIsOpen] = useState(visible);

    useEffect(() => {
        setIsOpen(visible);
    }, [visible]);

    const {
        control,
        formState: {errors, touchedFields},
        handleSubmit,
        getValues,
        register,
        reset,
        setValue,
    } = useForm({
        defaultValues: {
            name: integrationItem?.name || '',
            description: integrationItem?.description || '',
            category:
                {
                    label: integrationItem?.category?.name,
                    ...integrationItem?.category,
                } || undefined,
            tags:
                integrationItem?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        },
    });

    const {
        isLoading: categoriesIsLoading,
        error: categoriesError,
        data: categories,
    } = useGetIntegrationCategoriesQuery();

    const {
        isLoading: tagsIsLoading,
        error: tagsError,
        data: tags,
    } = useGetIntegrationTagsQuery();

    const queryClient = useQueryClient();

    const tagNames = integrationItem?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    const mutation = useIntegrationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(
                IntegrationKeys.integrationCategories
            );
            queryClient.invalidateQueries(IntegrationKeys.integrations);
            queryClient.invalidateQueries(IntegrationKeys.integrationTags);

            setIsOpen(false);

            reset();
        },
    });

    const putMutation = useIntegrationPutMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(
                IntegrationKeys.integrationCategories
            );

            queryClient.invalidateQueries(IntegrationKeys.integrations);

            queryClient.invalidateQueries(IntegrationKeys.integrationTags);

            setIsOpen(false);

            reset();
        },
    });

    function createIntegration() {
        const formData = getValues();

        const tagValues = formData.tags?.map((tag: TagModel) => {
            return {id: tag.id, name: tag.name, version: tag.version};
        });

        if (!id) {
            putMutation.mutate({
                ...integrationItem,
                ...formData,
                createdDate: integrationItem?.createdDate,
            } as IntegrationModel);
        } else {
            mutation.mutate({...formData, tags: tagValues} as IntegrationModel);
        }
    }

    return (
        <Modal
            confirmButtonLabel={id ? 'Edit' : 'Create'}
            description={`Use this to ${
                id ? 'edit' : 'create'
            } your integration which will contain related workflows`}
            form
            isOpen={isOpen}
            onCloseClick={reset}
            onConfirmButtonClick={handleSubmit(createIntegration)}
            setIsOpen={setIsOpen}
            title={`${id ? 'Edit' : 'Create'} Integration`}
            triggerLabel={`${id ? 'Edit' : 'Create'} Integration`}
        >
            {categoriesError &&
                !categoriesIsLoading &&
                `An error has occurred: ${categoriesError.message}`}

            {tagsError &&
                !tagsIsLoading &&
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

            {!categoriesIsLoading && (
                <Controller
                    control={control}
                    name="category"
                    render={({field}) => (
                        <CreatableSelect
                            field={field}
                            isMulti={false}
                            label="Category"
                            name="category"
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
                            value={field.value}
                            onBlur={field.onBlur}
                            onChange={field.onChange}
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
                            name={field.name}
                            value={field.value}
                            onBlur={field.onBlur}
                            onChange={field.onChange}
                        />
                    )}
                />
            )}

            <div className="mt-4 flex justify-end space-x-1">
                <Button
                    displayType="lightBorder"
                    label="Cancel"
                    type="button"
                    onClick={() => {
                        setIsOpen(false);

                        reset();
                    }}
                />

                <Button
                    label={(id && 'Edit') || 'Create'}
                    onClick={handleSubmit(createIntegration)}
                    type="submit"
                />
            </div>
        </Modal>
    );
};

export default IntegrationModal;
