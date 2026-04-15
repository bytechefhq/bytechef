import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAiGatewayTagsQuery, useDeleteAiGatewayTagMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {PencilIcon, PlusIcon, TagIcon, TrashIcon} from 'lucide-react';
import {useCallback, useState} from 'react';

import {AiGatewayTagType} from '../../types';
import AiGatewayTagDialog from './AiGatewayTagDialog';

const AiGatewayTags = () => {
    const [deletingTagId, setDeletingTagId] = useState<string | undefined>(undefined);
    const [editingTag, setEditingTag] = useState<AiGatewayTagType | undefined>(undefined);
    const [showDialog, setShowDialog] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data: tagsData, isLoading: tagsIsLoading} = useAiGatewayTagsQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const deleteTagMutation = useDeleteAiGatewayTagMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiGatewayTags']});

            setDeletingTagId(undefined);
        },
    });

    const tags = (tagsData?.aiGatewayTags ?? []).filter((tag): tag is AiGatewayTagType => tag != null);

    const handleConfirmDelete = useCallback(() => {
        if (deletingTagId) {
            deleteTagMutation.mutate({id: deletingTagId});
        }
    }, [deleteTagMutation, deletingTagId]);

    const handleEditTag = useCallback((tag: AiGatewayTagType) => {
        setEditingTag(tag);
        setShowDialog(true);
    }, []);

    const handleCloseDialog = useCallback(() => {
        setShowDialog(false);
        setEditingTag(undefined);
    }, []);

    if (tagsIsLoading) {
        return <PageLoader loading={true} />;
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            {tags.length === 0 ? (
                <EmptyList
                    button={<Button label="Add Tag" onClick={() => setShowDialog(true)} />}
                    icon={<TagIcon className="size-12 text-muted-foreground" />}
                    message="Create tags to label and group LLM Gateway resources."
                    title="No Tags Configured"
                />
            ) : (
                <>
                    <div className="mb-4 flex items-center justify-end py-4">
                        <Button
                            icon={<PlusIcon className="size-4" />}
                            label="Add Tag"
                            onClick={() => setShowDialog(true)}
                        />
                    </div>

                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <thead>
                                <tr className="border-b text-muted-foreground">
                                    <th className="pb-2 font-medium">Name</th>

                                    <th className="pb-2 font-medium">Color</th>

                                    <th className="pb-2 font-medium">Actions</th>
                                </tr>
                            </thead>

                            <tbody>
                                {tags.map((tag) => (
                                    <tr className="border-b" key={tag.id}>
                                        <td className="py-3 font-medium">{tag.name}</td>

                                        <td className="py-3">
                                            <div className="flex items-center gap-2">
                                                <span
                                                    className="inline-block size-4 rounded-full border"
                                                    style={{backgroundColor: tag.color || '#e5e7eb'}}
                                                />

                                                <span className="text-muted-foreground">{tag.color || 'default'}</span>
                                            </div>
                                        </td>

                                        <td className="py-3">
                                            <div className="flex gap-2">
                                                <button
                                                    className="text-muted-foreground hover:text-foreground"
                                                    onClick={() => handleEditTag(tag)}
                                                >
                                                    <PencilIcon className="size-4" />
                                                </button>

                                                <button
                                                    className="text-destructive hover:text-destructive/80"
                                                    onClick={() => setDeletingTagId(tag.id)}
                                                >
                                                    <TrashIcon className="size-4" />
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </>
            )}

            <AlertDialog open={!!deletingTagId}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete the tag.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setDeletingTagId(undefined)}>Cancel</AlertDialogCancel>

                        <AlertDialogAction
                            className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                            onClick={handleConfirmDelete}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {showDialog && currentWorkspaceId != null && (
                <AiGatewayTagDialog
                    onClose={handleCloseDialog}
                    tag={editingTag}
                    workspaceId={String(currentWorkspaceId)}
                />
            )}
        </div>
    );
};

export default AiGatewayTags;
