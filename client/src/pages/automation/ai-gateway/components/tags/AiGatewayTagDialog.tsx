import Button from '@/components/Button/Button';
import {useCreateAiGatewayTagMutation, useUpdateAiGatewayTagMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {useCallback, useState} from 'react';

import {AiGatewayTagType} from '../../types';

interface AiGatewayTagDialogProps {
    onClose: () => void;
    tag?: AiGatewayTagType;
    workspaceId: string;
}

const AiGatewayTagDialog = ({onClose, tag, workspaceId}: AiGatewayTagDialogProps) => {
    const [color, setColor] = useState(tag?.color ?? '#3b82f6');
    const [name, setName] = useState(tag?.name ?? '');

    const queryClient = useQueryClient();

    const isEditMode = !!tag;

    const createMutation = useCreateAiGatewayTagMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiGatewayTags']});

            onClose();
        },
    });

    const updateMutation = useUpdateAiGatewayTagMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiGatewayTags']});

            onClose();
        },
    });

    const handleSubmit = useCallback(() => {
        if (isEditMode) {
            updateMutation.mutate({color: color || undefined, id: tag.id, name});
        } else {
            createMutation.mutate({input: {color: color || undefined, name, workspaceId}});
        }
    }, [color, createMutation, isEditMode, name, tag, updateMutation, workspaceId]);

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-medium">{isEditMode ? 'Edit Tag' : 'Add Tag'}</h3>

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
                            placeholder="production"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Color</label>

                        <div className="flex items-center gap-2">
                            <input
                                className="size-10 cursor-pointer rounded border"
                                onChange={(event) => setColor(event.target.value)}
                                type="color"
                                value={color}
                            />

                            <input
                                className="flex-1 rounded-md border px-3 py-2 text-sm"
                                onChange={(event) => setColor(event.target.value)}
                                placeholder="#3b82f6"
                                value={color}
                            />
                        </div>
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

export default AiGatewayTagDialog;
