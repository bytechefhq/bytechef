import React, {useEffect, useState} from 'react';
import Footer from './Footer';
import Header from './Header';
import {CategoryModel, TagModel} from '../../../data-access/integration';
import {useIntegrationTagsMutation} from 'mutations/integrations.mutations';

export const IntegrationItem: React.FC<{
    button: string;
    name: string;
    status: boolean;
    description?: string;
    category?: CategoryModel;
    date?: Date;
    id?: number;
    tags?: TagModel[];
    workflowIds?: string[];
    remainingTags?: TagModel[];
}> = ({id, name, status, description, category, tags, date, remainingTags}) => {
    const mutation = useIntegrationTagsMutation();
    const [reload, setReload] = useState(false);

    useEffect(() => {
        if (reload) {
            window.location.reload();
        }
    }, [reload]);

    const handleOnAddTag = (newTag: TagModel) => {
        const newTags = (tags && [...tags]) || [];
        newTags.push(newTag);
        mutation.mutate({id: id || 0, tagModel: newTags || []});
        setReload(true);
    };

    const handleOnDeleteTag = (deletedTag: TagModel) => {
        const newTags = tags?.filter((tag) => tag.id !== deletedTag.id) || [];
        mutation.mutate({id: id || 0, tagModel: newTags});
        setReload(true);
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
