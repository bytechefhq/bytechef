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
    item: IntegrationModel | undefined;
    openModal?: boolean;
    onClose?: () => void;
}

const IntegrationModal = ({
    id,
    item,
    openModal = false,
}: IntegrationModalProps) => {
    const [isOpen, setIsOpen] = useState(openModal);

    useEffect(() => {
        setIsOpen(openModal);
    }, [openModal]);

    const {control, getValues, setValue, handleSubmit, reset} = useForm({
        defaultValues: {
            name: item?.name || '',
            description: item?.description || '',
            category:
                {...item?.category, label: item?.category?.name} || undefined,
            tags: item?.tags?.map((x) => ({...x, label: x.name})) || [],
            // workflowIds: item?.workflowIds,
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

    const remainingTags = tags?.filter(
        (x) => !item?.tags?.map((y) => y.name).includes(x.name)
    );

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

    const queryClient = useQueryClient();

    function createIntegration() {
        const formData = getValues();

        const tagValues = formData.tags?.map((tag: TagModel) => {
            return {id: tag.id, name: tag.name, version: tag.version};
        });

        if (id != undefined) {
            putMutation.mutate({
                ...item,
                ...formData,
                createdDate: item?.createdDate,
            } as IntegrationModel);
        } else {
            mutation.mutate({...formData, tags: tagValues} as IntegrationModel);
        }
    }

    return (
        <Modal
            confirmButtonLabel={(id && 'Edit') || 'Create'}
            description={`Use this to ${
                (id && 'edit') || 'create'
            } your integration which will contain related workflows`}
            handleConfirmButtonClick={handleSubmit(createIntegration)}
            triggerLabel={(id && 'Edit Integration') || 'Create Integration'}
            title={(id && 'Edit Integration') || 'Create Integration'}
            form={true}
            isOpen={isOpen}
            setIsOpen={setIsOpen}
            showTriggerLabel={id === undefined}
        >
            {categoriesError &&
                !categoriesIsLoading &&
                `An error has occurred: ${categoriesError.message}`}
            {tagsError &&
                !tagsIsLoading &&
                `An error has occurred: ${tagsError.message}`}

            <Controller
                name="name"
                control={control}
                rules={{required: true}}
                render={({field, fieldState: {error, isTouched}}) => (
                    <Input
                        error={isTouched && !!error}
                        label="Name"
                        placeholder="My CRM Integration"
                        {...field}
                    />
                )}
            />

            <Controller
                control={control}
                name="description"
                render={({field}) => (
                    <TextArea
                        label="Description"
                        placeholder="Cute description of your integration"
                        {...field}
                    />
                )}
            />

            {!categoriesIsLoading && (
                <Controller
                    control={control}
                    name="category"
                    render={({field}) => (
                        <CreatableSelect
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
                            name={field.name}
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
                            isMulti={true}
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

            <div className="mt-4 flex justify-end">
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
