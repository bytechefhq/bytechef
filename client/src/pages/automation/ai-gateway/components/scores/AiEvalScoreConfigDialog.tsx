import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiEvalScoreDataType,
    useCreateAiEvalScoreConfigMutation,
    useDeleteAiEvalScoreConfigMutation,
    useUpdateAiEvalScoreConfigMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import {AiEvalScoreConfigType} from '../../types';

interface AiEvalScoreConfigDialogProps {
    editingConfig?: AiEvalScoreConfigType;
    onClose: () => void;
}

const AiEvalScoreConfigDialog = ({editingConfig, onClose}: AiEvalScoreConfigDialogProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const queryClient = useQueryClient();

    const [categories, setCategories] = useState(editingConfig?.categories || '');
    const [dataType, setDataType] = useState(editingConfig?.dataType || 'NUMERIC');
    const [description, setDescription] = useState(editingConfig?.description || '');
    const [maxValue, setMaxValue] = useState(editingConfig?.maxValue?.toString() || '1');
    const [minValue, setMinValue] = useState(editingConfig?.minValue?.toString() || '0');
    const [name, setName] = useState(editingConfig?.name || '');

    const createMutation = useCreateAiEvalScoreConfigMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiEvalScoreConfigs']});
            onClose();
        },
    });

    const updateMutation = useUpdateAiEvalScoreConfigMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiEvalScoreConfigs']});
            onClose();
        },
    });

    const deleteMutation = useDeleteAiEvalScoreConfigMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiEvalScoreConfigs']});
            onClose();
        },
    });

    const handleSubmit = () => {
        if (editingConfig) {
            updateMutation.mutate({
                categories: dataType === 'CATEGORICAL' ? categories : undefined,
                dataType: dataType as AiEvalScoreDataType,
                description: description || undefined,
                id: editingConfig.id,
                maxValue: dataType === 'NUMERIC' ? parseFloat(maxValue) : undefined,
                minValue: dataType === 'NUMERIC' ? parseFloat(minValue) : undefined,
                name,
            });
        } else {
            createMutation.mutate({
                categories: dataType === 'CATEGORICAL' ? categories : undefined,
                dataType: dataType as AiEvalScoreDataType,
                description: description || undefined,
                maxValue: dataType === 'NUMERIC' ? parseFloat(maxValue) : undefined,
                minValue: dataType === 'NUMERIC' ? parseFloat(minValue) : undefined,
                name,
                workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
            });
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                <h3 className="mb-4 text-lg font-semibold">
                    {editingConfig ? 'Edit Score Config' : 'New Score Config'}
                </h3>

                <fieldset className="space-y-4 border-0">
                    <div>
                        <label className="mb-1 block text-sm font-medium" htmlFor="scoreConfigName">
                            Name
                        </label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            id="scoreConfigName"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="e.g., relevance, helpfulness, safety"
                            value={name}
                        />
                    </div>

                    <div>
                        <label className="mb-1 block text-sm font-medium" htmlFor="scoreConfigDataType">
                            Data Type
                        </label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            id="scoreConfigDataType"
                            onChange={(event) => setDataType(event.target.value)}
                            value={dataType}
                        >
                            <option value="NUMERIC">Numeric</option>

                            <option value="BOOLEAN">Boolean</option>

                            <option value="CATEGORICAL">Categorical</option>
                        </select>
                    </div>

                    {dataType === 'NUMERIC' && (
                        <div className="flex gap-4">
                            <div className="flex-1">
                                <label className="mb-1 block text-sm font-medium" htmlFor="scoreConfigMinValue">
                                    Min Value
                                </label>

                                <input
                                    className="w-full rounded-md border px-3 py-2 text-sm"
                                    id="scoreConfigMinValue"
                                    onChange={(event) => setMinValue(event.target.value)}
                                    type="number"
                                    value={minValue}
                                />
                            </div>

                            <div className="flex-1">
                                <label className="mb-1 block text-sm font-medium" htmlFor="scoreConfigMaxValue">
                                    Max Value
                                </label>

                                <input
                                    className="w-full rounded-md border px-3 py-2 text-sm"
                                    id="scoreConfigMaxValue"
                                    onChange={(event) => setMaxValue(event.target.value)}
                                    type="number"
                                    value={maxValue}
                                />
                            </div>
                        </div>
                    )}

                    {dataType === 'CATEGORICAL' && (
                        <div>
                            <label className="mb-1 block text-sm font-medium" htmlFor="scoreConfigCategories">
                                Categories (JSON array)
                            </label>

                            <input
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                id="scoreConfigCategories"
                                onChange={(event) => setCategories(event.target.value)}
                                placeholder='["good", "bad", "neutral"]'
                                value={categories}
                            />
                        </div>
                    )}

                    <div>
                        <label className="mb-1 block text-sm font-medium" htmlFor="scoreConfigDescription">
                            Description
                        </label>

                        <textarea
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            id="scoreConfigDescription"
                            onChange={(event) => setDescription(event.target.value)}
                            placeholder="What does this score measure?"
                            rows={2}
                            value={description}
                        />
                    </div>
                </fieldset>

                <div className="mt-6 flex justify-between">
                    <div>
                        {editingConfig && (
                            <button
                                className="rounded-md px-3 py-1.5 text-sm text-red-600 hover:bg-red-50"
                                onClick={() => deleteMutation.mutate({id: editingConfig.id})}
                            >
                                Delete
                            </button>
                        )}
                    </div>

                    <div className="flex gap-2">
                        <button
                            className="rounded-md px-3 py-1.5 text-sm text-muted-foreground hover:bg-muted"
                            onClick={onClose}
                        >
                            Cancel
                        </button>

                        <button
                            className="rounded-md bg-primary px-3 py-1.5 text-sm text-primary-foreground hover:bg-primary/90"
                            disabled={!name}
                            onClick={handleSubmit}
                        >
                            {editingConfig ? 'Update' : 'Create'}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AiEvalScoreConfigDialog;
