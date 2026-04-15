import Button from '@/components/Button/Button';
import {useAiGatewayTagsQuery, useSetAiObservabilityTraceTagsMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon, XIcon} from 'lucide-react';
import {useCallback, useMemo, useState} from 'react';
import {toast} from 'sonner';

interface TraceTagEditorProps {
    traceId: string;
    tagIds: ReadonlyArray<string>;
    workspaceId: string;
}

/**
 * Inline tag editor for a trace. Renders selected tags as chips with remove buttons plus a dropdown of
 * workspace-available tags for adding. Uses the set-based {@code setAiObservabilityTraceTags} mutation — the caller
 * ships the full desired tagId list each time.
 */
const TraceTagEditor = ({tagIds, traceId, workspaceId}: TraceTagEditorProps) => {
    const [showPicker, setShowPicker] = useState(false);

    const queryClient = useQueryClient();

    const {data: tagsData} = useAiGatewayTagsQuery({workspaceId}, {enabled: Boolean(workspaceId)});

    const setTagsMutation = useSetAiObservabilityTraceTagsMutation({
        onError: (error: Error) => toast.error(`Tag update failed: ${error.message}`),
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityTrace']});

            setShowPicker(false);
        },
    });

    const allTags = useMemo(() => tagsData?.aiGatewayTags ?? [], [tagsData?.aiGatewayTags]);

    const attachedTags = useMemo(
        () => allTags.filter((tag): tag is NonNullable<typeof tag> => tag != null && tagIds.includes(tag.id)),
        [allTags, tagIds]
    );

    const availableTags = useMemo(
        () => allTags.filter((tag): tag is NonNullable<typeof tag> => tag != null && !tagIds.includes(tag.id)),
        [allTags, tagIds]
    );

    const handleRemove = useCallback(
        (tagId: string) => {
            setTagsMutation.mutate({
                tagIds: tagIds.filter((id) => id !== tagId),
                traceId,
            });
        },
        [setTagsMutation, tagIds, traceId]
    );

    const handleAdd = useCallback(
        (tagId: string) => {
            setTagsMutation.mutate({tagIds: [...tagIds, tagId], traceId});
        },
        [setTagsMutation, tagIds, traceId]
    );

    return (
        <div className="flex flex-wrap items-center gap-1.5">
            {attachedTags.length === 0 && !showPicker && <span className="text-xs text-muted-foreground">No tags</span>}

            {attachedTags.map((tag) => (
                <span
                    className="flex items-center gap-1 rounded-full border border-border/50 bg-background px-2 py-0.5 text-xs"
                    key={tag.id}
                    style={tag.color ? {borderColor: tag.color, color: tag.color} : undefined}
                >
                    {tag.name}

                    <button
                        aria-label={`Remove ${tag.name}`}
                        className="hover:text-destructive"
                        disabled={setTagsMutation.isPending}
                        onClick={() => handleRemove(tag.id)}
                        type="button"
                    >
                        <XIcon className="size-3" />
                    </button>
                </span>
            ))}

            {showPicker && availableTags.length > 0 && (
                <select
                    className="rounded-md border px-2 py-0.5 text-xs"
                    onChange={(event) => {
                        if (event.target.value) {
                            handleAdd(event.target.value);
                        }
                    }}
                    value=""
                >
                    <option value="">Add tag...</option>

                    {availableTags.map((tag) => (
                        <option key={tag.id} value={tag.id}>
                            {tag.name}
                        </option>
                    ))}
                </select>
            )}

            {!showPicker && availableTags.length > 0 && (
                <Button
                    icon={<PlusIcon className="size-3" />}
                    label="Add"
                    onClick={() => setShowPicker(true)}
                    size="xs"
                    variant="outline"
                />
            )}
        </div>
    );
};

export default TraceTagEditor;
