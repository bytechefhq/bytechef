import React, {useEffect, useState} from 'react';

import Input from 'components/Input/Input';
import Modal from 'components/Modal/Modal';
import MultiSelect from 'components/MultiSelect/MultiSelect';
import TextArea from 'components/TextArea/TextArea';

interface Tag {
    readonly label: string;
    readonly value: string;
}

interface IntegrationData {
    category: string;
    description: string;
    name: string;
    tags: Array<Tag>;
    workflowIds: Array<string>;
}

const IntegrationModal: React.FC = () => {
    const [availableTags, setAvailableTags] = useState([]);
    const [tagsInputValue, setTagsInputValue] = useState<Array<Tag> | Tag | []>(
        []
    );
    const [integrationData, setIntegrationData] = useState<IntegrationData>({
        category: '',
        description: '',
        name: '',
        tags: [],
        workflowIds: [],
    });

    useEffect(() => {
        fetch('http://localhost:5173/api/integration-tags', {
            credentials: 'same-origin',
            headers: {'Content-Type': 'application/json'},
        })
            .then((response) => response.json())
            .then((response) => setAvailableTags(response.tags));
    }, []);

    function createIntegration(integrationData: IntegrationData) {
        const {category, description, name, tags, workflowIds} =
            integrationData;

        const tagValues = tags.map((tag) => tag.value);

        fetch('http://localhost:5173/api/integrations', {
            body: JSON.stringify({
                category,
                description,
                name,
                tags: tagValues,
                workflowIds,
            }),
            credentials: 'same-origin',
            headers: {'Content-Type': 'application/json'},
            method: 'POST',
        }).then(() => {
            setIntegrationData({
                category: '',
                description: '',
                name: '',
                tags: [],
                workflowIds: [],
            });

            setTagsInputValue([]);
        });
    }

    return (
        <Modal
            confirmButtonLabel="Create"
            description="Use this to create your integration which will contain related workflows"
            handleConfirmButtonClick={() => createIntegration(integrationData)}
            triggerLabel="Create Integration"
            title="Create Integration"
        >
            <Input
                label="Name"
                name="name"
                onChange={(event) =>
                    setIntegrationData({
                        ...integrationData,
                        name: event.target.value,
                    })
                }
                placeholder="My CRM Integration"
                required
                value={integrationData.name}
            />

            <TextArea
                label="Description"
                name="description"
                onChange={(event) =>
                    setIntegrationData({
                        ...integrationData,
                        description: event.target.value,
                    })
                }
                placeholder="Cute description of your integration"
                value={integrationData.description}
            />

            <Input
                label="Category"
                name="category"
                onChange={(event) =>
                    setIntegrationData({
                        ...integrationData,
                        category: event.target.value,
                    })
                }
                placeholder="Marketing, Sales, Social Media..."
                value={integrationData.category}
            />

            <MultiSelect
                label="Tags"
                name="tags"
                onChange={(newTag: Tag) => {
                    setIntegrationData({
                        ...integrationData,
                        // @ts-expect-error expects an Array<Tag> but receives a single Tag
                        tags: newTag,
                    });

                    setTagsInputValue(newTag);
                }}
                onCreateOption={(inputValue: string) => {
                    const newOption = {
                        label: inputValue,
                        value: inputValue.toLowerCase().replace(/\W/g, ''),
                    };

                    // @ts-expect-error tagsInputValue can be not an array
                    setTagsInputValue([...tagsInputValue, newOption]);

                    setIntegrationData({
                        ...integrationData,
                        tags: [...integrationData.tags, newOption],
                    });
                }}
                options={availableTags.map((tag: string) => ({
                    label: `${tag.charAt(0).toUpperCase()}${tag.slice(1)}`,
                    value: tag.toLowerCase().replace(/\W/g, ''),
                }))}
                value={tagsInputValue}
            />
        </Modal>
    );
};

export default IntegrationModal;
