import Button from '@/components/Button/Button';
import {useCreateAiPromptMutation, useUpdateAiPromptMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {useCallback, useState} from 'react';

import {AiPromptType} from '../../types';

interface AiPromptDialogProps {
    onClose: () => void;
    prompt?: AiPromptType;
    workspaceId: string;
}

const AiPromptDialog = ({onClose, prompt, workspaceId}: AiPromptDialogProps) => {
    const [description, setDescription] = useState(prompt?.description ?? '');
    const [name, setName] = useState(prompt?.name ?? '');

    const queryClient = useQueryClient();

    const isEditMode = !!prompt;

    const createMutation = useCreateAiPromptMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiPrompts']});

            onClose();
        },
    });

    const updateMutation = useUpdateAiPromptMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiPrompts']});

            onClose();
        },
    });

    const handleSubmit = useCallback(() => {
        if (isEditMode) {
            updateMutation.mutate({
                id: prompt.id,
                input: {
                    description: description || undefined,
                    name,
                },
            });
        } else {
            createMutation.mutate({
                input: {
                    description: description || undefined,
                    name,
                    workspaceId,
                },
            });
        }
    }, [createMutation, description, isEditMode, name, prompt, updateMutation, workspaceId]);

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-medium">{isEditMode ? 'Edit Prompt' : 'Create Prompt'}</h3>

                    <button onClick={onClose}>
                        <XIcon className="size-4" />
                    </button>
                </div>

                <div className="space-y-4">
                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Name</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="My Prompt Template"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Description (optional)</label>

                        <textarea
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setDescription(event.target.value)}
                            placeholder="Describe what this prompt does..."
                            rows={3}
                            value={description}
                        />
                    </fieldset>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={!name || createMutation.isPending || updateMutation.isPending}
                        label={isEditMode ? 'Save' : 'Create'}
                        onClick={handleSubmit}
                    />
                </div>
            </div>
        </div>
    );
};

export default AiPromptDialog;
