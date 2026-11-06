import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
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
    const [deleteConfirmationOpen, setDeleteConfirmationOpen] = useState(false);
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
        <>
            <Dialog
                onOpenChange={(open) => {
                    if (!open) {
                        onClose();
                    }
                }}
                open
            >
                <DialogContent aria-describedby={undefined}>
                    <DialogMain>
                        <DialogHeader title={editingConfig ? 'Edit Score Config' : 'New Score Config'} />

                        <DialogBody>
                            <fieldset className="space-y-4 border-0">
                                <div>
                                    <Label className="mb-1 block" htmlFor="scoreConfigName">
                                        Name
                                    </Label>

                                    <Input
                                        id="scoreConfigName"
                                        onChange={(event) => setName(event.target.value)}
                                        placeholder="e.g., relevance, helpfulness, safety"
                                        value={name}
                                    />
                                </div>

                                <div>
                                    <Label className="mb-1 block" htmlFor="scoreConfigDataType">
                                        Data Type
                                    </Label>

                                    <Select onValueChange={setDataType} value={dataType}>
                                        <SelectTrigger id="scoreConfigDataType">
                                            <SelectValue />
                                        </SelectTrigger>

                                        <SelectContent>
                                            <SelectItem value="NUMERIC">Numeric</SelectItem>

                                            <SelectItem value="BOOLEAN">Boolean</SelectItem>

                                            <SelectItem value="CATEGORICAL">Categorical</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>

                                {dataType === 'NUMERIC' && (
                                    <div className="flex gap-4">
                                        <div className="flex-1">
                                            <Label className="mb-1 block" htmlFor="scoreConfigMinValue">
                                                Min Value
                                            </Label>

                                            <Input
                                                id="scoreConfigMinValue"
                                                onChange={(event) => setMinValue(event.target.value)}
                                                type="number"
                                                value={minValue}
                                            />
                                        </div>

                                        <div className="flex-1">
                                            <Label className="mb-1 block" htmlFor="scoreConfigMaxValue">
                                                Max Value
                                            </Label>

                                            <Input
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
                                        <Label className="mb-1 block" htmlFor="scoreConfigCategories">
                                            Categories (JSON array)
                                        </Label>

                                        <Input
                                            id="scoreConfigCategories"
                                            onChange={(event) => setCategories(event.target.value)}
                                            placeholder='["good", "bad", "neutral"]'
                                            value={categories}
                                        />
                                    </div>
                                )}

                                <div>
                                    <Label className="mb-1 block" htmlFor="scoreConfigDescription">
                                        Description
                                    </Label>

                                    <Textarea
                                        id="scoreConfigDescription"
                                        onChange={(event) => setDescription(event.target.value)}
                                        placeholder="What does this score measure?"
                                        rows={2}
                                        value={description}
                                    />
                                </div>
                            </fieldset>
                        </DialogBody>

                        <DialogFooter
                            startContent={
                                editingConfig && (
                                    <Button
                                        disabled={deleteMutation.isPending}
                                        label="Delete"
                                        onClick={() => setDeleteConfirmationOpen(true)}
                                        variant="destructiveGhost"
                                    />
                                )
                            }
                        >
                            <DialogCancelButton />

                            <Button
                                disabled={!name || createMutation.isPending || updateMutation.isPending}
                                label={editingConfig ? 'Update' : 'Create'}
                                onClick={handleSubmit}
                            />
                        </DialogFooter>
                    </DialogMain>
                </DialogContent>
            </Dialog>

            {editingConfig && (
                <AlertDialog
                    description="This action cannot be undone."
                    isPending={deleteMutation.isPending}
                    onCancel={() => setDeleteConfirmationOpen(false)}
                    onConfirm={() => deleteMutation.mutate({id: editingConfig.id})}
                    open={deleteConfirmationOpen}
                    title={`Delete ${editingConfig.name} score config?`}
                />
            )}
        </>
    );
};

export default AiEvalScoreConfigDialog;
