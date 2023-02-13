import React, {useEffect, useState} from 'react';
import Footer from './Footer';
import Header from './Header';
import {CategoryModel, TagModel} from '../../../data-access/integration';
import {useIntegrationTagsMutation} from 'mutations/integrations.mutations';

export const IntegrationItem: React.FC<{
    button: string;
    name: string;
    status: boolean;
    onChangeState: () => void;
    description?: string;
    category?: CategoryModel;
    date?: Date;
    id?: number;
    tags?: Array<TagModel>;
    workflowIds?: string[];
    remainingTags?: TagModel[];
}> = ({
    id,
    name,
    status,
    description,
    category,
    tags,
    date,
    remainingTags,
    onChangeState,
}) => {
    const mutation = useIntegrationTagsMutation();
    const [needsRefetch, setNeedsRefetch] = useState(false);

    useEffect(() => {
        if (needsRefetch) {
            setNeedsRefetch(false);
            onChangeState();
        }
    }, [needsRefetch, onChangeState]);

    const handleOnAddTag = (newTag: TagModel) => {
        const newTags = (tags && [...tags]) || [];
        newTags.push(newTag);
        mutation.mutate({
            id: id || 0,
            putIntegrationTagsRequestModel: {
                tags: newTags || [],
            },
        });

        setNeedsRefetch(true);
    };

    const handleOnDeleteTag = (deletedTag: TagModel) => {
        const newTags = tags?.filter((tag) => tag.id !== deletedTag.id) || [];
        mutation.mutate({
            id: id || 0,
            putIntegrationTagsRequestModel: {
                tags: newTags || [],
            },
        });

        setNeedsRefetch(true);
    };

    return (
        <div>
            <Header
                id={id}
                name={name}
                status={status}
                description={description}
            />

            <Footer
                category={category}
                tags={tags}
                date={date}
                remainingTags={remainingTags}
                onAddTag={handleOnAddTag}
                onDeleteTag={handleOnDeleteTag}
            />
        </div>
    );
};
