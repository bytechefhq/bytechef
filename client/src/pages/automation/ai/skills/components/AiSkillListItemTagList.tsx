import TagList from '@/shared/components/TagList';
import {Tag, useAiSkillTagsQuery, useUpdateAiSkillTagsMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';
import {toast} from 'sonner';

interface AiSkillListItemTagListProps {
    skillId: string;
    tags: Tag[];
}

const convertTagToTagListFormat = (tag: Tag) => ({
    ...tag,
    id: tag.id ? Number(tag.id) : undefined,
});

const AiSkillListItemTagList = ({skillId, tags}: AiSkillListItemTagListProps) => {
    const queryClient = useQueryClient();

    const {data: tagsData} = useAiSkillTagsQuery();

    const updateTagsMutation = useUpdateAiSkillTagsMutation({
        onError: (error: Error) => {
            toast.error('Failed to update skill tags', {
                description: error.message,
            });
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiSkills']});
            queryClient.invalidateQueries({queryKey: ['aiSkillTags']});
        },
    });

    const convertedTags = useMemo(() => tags.map(convertTagToTagListFormat), [tags]);

    const remainingTags = useMemo(() => {
        const currentTagIds = new Set(tags.map((tag) => String(tag.id)));

        return ((tagsData?.aiSkillTags ?? []) as Tag[])
            .filter((tag) => !currentTagIds.has(String(tag.id)))
            .map(convertTagToTagListFormat);
    }, [tags, tagsData]);

    return (
        <TagList
            getRequest={(_id, newTags) => ({
                id: skillId,
                tags: newTags.map((tag) => ({id: tag.id ? String(tag.id) : undefined, name: tag.name})),
            })}
            id={+skillId}
            remainingTags={remainingTags}
            tags={convertedTags}
            updateTagsMutation={updateTagsMutation}
        />
    );
};

export default AiSkillListItemTagList;
