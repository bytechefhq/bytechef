import React, {useEffect, useState} from 'react';

import Input from 'components/Input/Input';
import Modal from 'components/Modal/Modal';
import MultiSelect from 'components/MultiSelect/MultiSelect';
import TextArea from 'components/TextArea/TextArea';
import {Controller, useForm} from 'react-hook-form';
import Button from 'components/Button/Button';

interface Tag {
    readonly label: string;
    readonly value: string;
}

const IntegrationModal: React.FC = () => {
    const [isOpen, setIsOpen] = useState(false);
    const [availableTags, setAvailableTags] = useState([]);

    const {control, getValues, handleSubmit, reset} = useForm({
        defaultValues: {
            name: '',
            description: '',
            category: '',
            tags: [],
        },
    });

    useEffect(() => {
        fetch('http://localhost:5173/api/integration-tags', {
            credentials: 'same-origin',
            headers: {'Content-Type': 'application/json'},
        })
            .then((response) => response.json())
            .then((response) => setAvailableTags(response.tags));
    }, []);

    function createIntegration() {
        const formData = getValues();

        const tagValues = formData.tags.map((tag: Tag) => tag.value);

        fetch('http://localhost:5173/api/integrations', {
            body: JSON.stringify({...formData, tags: tagValues}),
            credentials: 'same-origin',
            headers: {'Content-Type': 'application/json'},
            method: 'POST',
        }).then(() => {
            setIsOpen(false);

            reset();
        });
    }

    return (
        <Modal
            confirmButtonLabel="Create"
            description="Use this to create your integration which will contain related workflows"
            handleConfirmButtonClick={handleSubmit(createIntegration)}
            triggerLabel="Create Integration"
            title="Create Integration"
            form={true}
            isOpen={isOpen}
            setIsOpen={setIsOpen}
        >
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

            <Controller
                control={control}
                name="category"
                render={({field}) => (
                    <Input
                        label="Category"
                        placeholder="Marketing, Sales, Social Media..."
                        {...field}
                    />
                )}
            />

            <Controller
                control={control}
                name="tags"
                render={({field}) => (
                    <MultiSelect
                        label="Tags"
                        options={availableTags.map((tag: string) => ({
                            label: `${tag.charAt(0).toUpperCase()}${tag.slice(
                                1
                            )}`,
                            value: tag.toLowerCase().replace(/\W/g, ''),
                        }))}
                        {...field}
                    />
                )}
            />

            <div className="mt-4 flex justify-end">
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
