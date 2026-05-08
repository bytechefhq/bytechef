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
import {useAiPromptsQuery, useDeleteAiPromptMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {FileTextIcon, PencilIcon, PlusIcon, TrashIcon} from 'lucide-react';
import {useCallback, useState} from 'react';

import {AiPromptType} from '../../types';
import AiPromptDetail from './AiPromptDetail';
import AiPromptDialog from './AiPromptDialog';

const ENVIRONMENTS = ['production', 'staging', 'development'];

const AiPrompts = () => {
    const [deletingPromptId, setDeletingPromptId] = useState<string | undefined>(undefined);
    const [editingPrompt, setEditingPrompt] = useState<AiPromptType | undefined>(undefined);
    const [selectedPromptId, setSelectedPromptId] = useState<string | undefined>(undefined);
    const [showDialog, setShowDialog] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data: promptsData, isLoading: promptsIsLoading} = useAiPromptsQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const deletePromptMutation = useDeleteAiPromptMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiPrompts']});

            setDeletingPromptId(undefined);
        },
    });

    const prompts = promptsData?.aiPrompts ?? [];

    const handleCloseDialog = useCallback(() => {
        setShowDialog(false);
        setEditingPrompt(undefined);
    }, []);

    const handleConfirmDelete = useCallback(() => {
        if (deletingPromptId) {
            deletePromptMutation.mutate({id: deletingPromptId});
        }
    }, [deletePromptMutation, deletingPromptId]);

    const handleEditPrompt = useCallback((prompt: AiPromptType) => {
        setEditingPrompt(prompt);
        setShowDialog(true);
    }, []);

    if (promptsIsLoading) {
        return <PageLoader loading={true} />;
    }

    if (selectedPromptId) {
        return <AiPromptDetail onBack={() => setSelectedPromptId(undefined)} promptId={selectedPromptId} />;
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            {prompts.length === 0 ? (
                <EmptyList
                    button={<Button label="Create Prompt" onClick={() => setShowDialog(true)} />}
                    icon={<FileTextIcon className="size-12 text-muted-foreground" />}
                    message="Create prompt templates to manage and version your LLM prompts."
                    title="No Prompts"
                />
            ) : (
                <>
                    <div className="mb-4 flex items-center justify-end py-4">
                        <Button
                            icon={<PlusIcon className="size-4" />}
                            label="Create Prompt"
                            onClick={() => setShowDialog(true)}
                        />
                    </div>

                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <thead>
                                <tr className="border-b text-muted-foreground">
                                    <th className="pb-2 font-medium">Name</th>

                                    <th className="pb-2 font-medium">Description</th>

                                    <th className="pb-2 font-medium">Versions</th>

                                    <th className="pb-2 font-medium">Environments</th>

                                    <th className="pb-2 font-medium">Actions</th>
                                </tr>
                            </thead>

                            <tbody>
                                {prompts.map((prompt) =>
                                    prompt ? (
                                        <tr
                                            className="cursor-pointer border-b hover:bg-muted/50"
                                            key={prompt.id}
                                            onClick={() => setSelectedPromptId(prompt.id)}
                                        >
                                            <td className="py-3 font-medium">{prompt.name}</td>

                                            <td className="py-3 text-muted-foreground">{prompt.description || '-'}</td>

                                            <td className="py-3">{prompt.versions?.length ?? 0}</td>

                                            <td className="py-3">
                                                <div className="flex gap-1">
                                                    {ENVIRONMENTS.map((environment) => {
                                                        const hasActive = prompt.versions?.some(
                                                            (version) =>
                                                                version &&
                                                                version.environment === environment &&
                                                                version.active
                                                        );

                                                        if (!hasActive) {
                                                            return null;
                                                        }

                                                        return (
                                                            <span
                                                                className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-800"
                                                                key={environment}
                                                            >
                                                                {environment}
                                                            </span>
                                                        );
                                                    })}
                                                </div>
                                            </td>

                                            <td className="py-3">
                                                <div className="flex gap-2">
                                                    <button
                                                        className="text-muted-foreground hover:text-foreground"
                                                        onClick={(event) => {
                                                            event.stopPropagation();
                                                            handleEditPrompt(prompt);
                                                        }}
                                                    >
                                                        <PencilIcon className="size-4" />
                                                    </button>

                                                    <button
                                                        className="text-destructive hover:text-destructive/80"
                                                        onClick={(event) => {
                                                            event.stopPropagation();
                                                            setDeletingPromptId(prompt.id);
                                                        }}
                                                    >
                                                        <TrashIcon className="size-4" />
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    ) : null
                                )}
                            </tbody>
                        </table>
                    </div>
                </>
            )}

            <AlertDialog open={!!deletingPromptId}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete the prompt and all its versions.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setDeletingPromptId(undefined)}>Cancel</AlertDialogCancel>

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
                <AiPromptDialog
                    onClose={handleCloseDialog}
                    prompt={editingPrompt}
                    workspaceId={String(currentWorkspaceId)}
                />
            )}
        </div>
    );
};

export default AiPrompts;
