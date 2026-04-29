import {
    KnowledgeBaseDocumentTagsEntry,
    UpdateKnowledgeBaseDocumentTagsInput,
    useUpdateKnowledgeBaseDocumentTagsMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';

interface UseKnowledgeBaseDocumentListItemTagListProps {
    knowledgeBaseDocumentId: string;
    remainingTags?: string[];
    tags: string[];
}

export default function useKnowledgeBaseDocumentListItemTagList({
    knowledgeBaseDocumentId,
    remainingTags,
    tags,
}: UseKnowledgeBaseDocumentListItemTagListProps) {
    const queryClient = useQueryClient();

    const updateTagsMutation = useUpdateKnowledgeBaseDocumentTagsMutation({
        onError: (_error, _variables, context) => {
            if (context?.cachedTagsByDocumentData) {
                queryClient.setQueryData(['knowledgeBaseDocumentTagsByDocument'], context.cachedTagsByDocumentData);
            }
        },
        onMutate: async (variables: {input: UpdateKnowledgeBaseDocumentTagsInput}) => {
            await queryClient.cancelQueries({queryKey: ['knowledgeBaseDocumentTagsByDocument']});

            const previousTagsData = queryClient.getQueryData<{
                knowledgeBaseDocumentTagsByDocument: KnowledgeBaseDocumentTagsEntry[];
            }>(['knowledgeBaseDocumentTagsByDocument']);

            const optimisticTagsData = (() => {
                if (!previousTagsData?.knowledgeBaseDocumentTagsByDocument) {
                    return previousTagsData;
                }

                const updatedTags = variables.input.tags ?? [];
                const updatedTagsByDocument = previousTagsData.knowledgeBaseDocumentTagsByDocument.map((entry) =>
                    entry.knowledgeBaseDocumentId === knowledgeBaseDocumentId
                        ? {...entry, tags: updatedTags}
                        : entry
                );

                const documentHasTagsEntry = previousTagsData.knowledgeBaseDocumentTagsByDocument.some(
                    (entry) => entry.knowledgeBaseDocumentId === knowledgeBaseDocumentId
                );

                return documentHasTagsEntry
                    ? {...previousTagsData, knowledgeBaseDocumentTagsByDocument: updatedTagsByDocument}
                    : {
                          ...previousTagsData,
                          knowledgeBaseDocumentTagsByDocument: [
                              ...previousTagsData.knowledgeBaseDocumentTagsByDocument,
                              {knowledgeBaseDocumentId, tags: updatedTags},
                          ],
                      };
            })();

            queryClient.setQueryData(['knowledgeBaseDocumentTagsByDocument'], optimisticTagsData);

            return {cachedTagsByDocumentData: previousTagsData};
        },
        onSettled: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBaseDocumentTags']});
            queryClient.invalidateQueries({queryKey: ['knowledgeBaseDocumentTagsByDocument']});
            queryClient.invalidateQueries({queryKey: ['knowledgeBases']});
        },
    });

    const convertedTags = useMemo(() => tags.map((tagName) => ({name: tagName})), [tags]);

    const convertedRemainingTags = useMemo(
        () => remainingTags?.map((tagName) => ({name: tagName})),
        [remainingTags]
    );

    return {
        convertedRemainingTags,
        convertedTags,
        updateTagsMutation,
    };
}
