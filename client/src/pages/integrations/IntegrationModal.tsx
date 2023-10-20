import React, {useState} from 'react';

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
import {useIntegrationMutation} from '../../mutations/integrations.mutations';

const IntegrationModal = () => {
    const [isOpen, setIsOpen] = useState(false);

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
            name: '',
            description: '',
            category: undefined,
            tags: [],
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

    function createIntegration() {
        const formData = getValues();

        mutation.mutate({...formData} as IntegrationModel);
    }

    return (
        <Modal
            confirmButtonLabel="Create"
            description="Create your integration which will contain related workflows"
            form
            isOpen={isOpen}
            setIsOpen={setIsOpen}
            title="Create Integration"
            triggerLabel="Create Integration"
            onCloseClick={reset}
            onConfirmButtonClick={handleSubmit(createIntegration)}
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
                        />
                    )}
                />
            )}

            {!tagsIsLoading && (
                <Controller
                    control={control}
                    name="tags"
                    render={({field}) => (
                        <CreatableSelect
                            field={field}
                            isMulti
                            label="Tags"
                            name="tags"
                            options={tags!.map((tag: TagModel) => ({
                                label: `${tag.name
                                    .charAt(0)
                                    .toUpperCase()}${tag.name.slice(1)}`,
                                value: tag.name
                                    .toLowerCase()
                                    .replace(/\W/g, ''),
                                ...tag,
                            }))}
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
                    label="Create"
                    onClick={handleSubmit(createIntegration)}
                    type="submit"
                />
            </div>
        </Modal>
    );
};

export default IntegrationModal;
