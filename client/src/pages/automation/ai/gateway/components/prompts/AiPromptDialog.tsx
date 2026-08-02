import Button from '@/components/Button/Button';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {useCreateAiPromptMutation, useUpdateAiPromptMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
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
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent aria-describedby={undefined} className="max-w-md">
                <DialogHeader>
                    <DialogTitle>{isEditMode ? 'Edit Prompt' : 'Create Prompt'}</DialogTitle>
                </DialogHeader>

                <div className="space-y-4">
                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="ai-prompt-name">
                            Name
                        </Label>

                        <Input
                            id="ai-prompt-name"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="My Prompt Template"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="ai-prompt-description">
                            Description (optional)
                        </Label>

                        <Textarea
                            id="ai-prompt-description"
                            onChange={(event) => setDescription(event.target.value)}
                            placeholder="Describe what this prompt does..."
                            rows={3}
                            value={description}
                        />
                    </fieldset>
                </div>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={!name || createMutation.isPending || updateMutation.isPending}
                        label={isEditMode ? 'Save' : 'Create'}
                        onClick={handleSubmit}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiPromptDialog;
