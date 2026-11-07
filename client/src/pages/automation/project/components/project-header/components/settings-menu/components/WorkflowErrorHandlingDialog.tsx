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
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import {
    useEligibleErrorWorkflowsQuery,
    useProjectWorkflowErrorConfigQuery,
    useUpdateProjectWorkflowErrorWorkflowMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo, useState} from 'react';

type ModeType = 'disabled' | 'inherit' | 'override';

interface WorkflowErrorHandlingDialogProps {
    onClose: () => void;
    projectId: string;
    projectVersion: number;
    projectWorkflowId: string;
}

const WorkflowErrorHandlingDialog = ({
    onClose,
    projectId,
    projectVersion,
    projectWorkflowId,
}: WorkflowErrorHandlingDialogProps) => {
    const queryClient = useQueryClient();

    const {data: eligibleData} = useEligibleErrorWorkflowsQuery({projectId, projectVersion});
    const {data: configData, isLoading: configLoading} = useProjectWorkflowErrorConfigQuery({id: projectWorkflowId});

    const currentConfig = configData?.projectWorkflow;

    const eligibleWorkflows = useMemo(
        () =>
            (eligibleData?.eligibleErrorWorkflows || []).filter(
                (eligibleWorkflow) => eligibleWorkflow.id !== projectWorkflowId
            ),
        [eligibleData, projectWorkflowId]
    );

    const [mode, setMode] = useState<ModeType>(
        currentConfig?.errorWorkflowDisabled
            ? 'disabled'
            : currentConfig?.errorProjectWorkflowId
              ? 'override'
              : 'inherit'
    );
    const [selectedWorkflowId, setSelectedWorkflowId] = useState(
        currentConfig?.errorProjectWorkflowId ? String(currentConfig.errorProjectWorkflowId) : ''
    );

    const updateMutation = useUpdateProjectWorkflowErrorWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['projectWorkflowErrorConfig', {id: projectWorkflowId}]});

            onClose();
        },
    });

    useEffect(() => {
        if (configData?.projectWorkflow) {
            const loadedConfig = configData.projectWorkflow;

            setMode(
                loadedConfig.errorWorkflowDisabled
                    ? 'disabled'
                    : loadedConfig.errorProjectWorkflowId
                      ? 'override'
                      : 'inherit'
            );
            setSelectedWorkflowId(
                loadedConfig.errorProjectWorkflowId ? String(loadedConfig.errorProjectWorkflowId) : ''
            );
        }
    }, [configData]);

    const handleSave = () => {
        updateMutation.mutate({
            errorProjectWorkflowId: mode === 'override' ? selectedWorkflowId : undefined,
            errorWorkflowDisabled: mode === 'disabled',
            projectId,
            projectWorkflowId,
        });
    };

    return (
        <Dialog onOpenChange={onClose} open={true}>
            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description="Explicit disable beats the project's inherited default; an override runs instead of the project default."
                        title="Error Handling"
                    />

                    <DialogBody>
                        <RadioGroup
                            className="space-y-3"
                            onValueChange={(value) => setMode(value as ModeType)}
                            value={mode}
                        >
                            <div className="flex items-center gap-2">
                                <RadioGroupItem id="error-handling-inherit" value="inherit" />

                                <label htmlFor="error-handling-inherit">Inherit project default</label>
                            </div>

                            <div className="flex items-center gap-2">
                                <RadioGroupItem
                                    disabled={eligibleWorkflows.length === 0}
                                    id="error-handling-override"
                                    value="override"
                                />

                                <label htmlFor="error-handling-override">Override</label>

                                {mode === 'override' && (
                                    <Select onValueChange={setSelectedWorkflowId} value={selectedWorkflowId}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Select a workflow" />
                                        </SelectTrigger>

                                        <SelectContent>
                                            {eligibleWorkflows.map((eligibleWorkflow) => (
                                                <SelectItem key={eligibleWorkflow.id} value={eligibleWorkflow.id}>
                                                    {eligibleWorkflow.workflow.label}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                )}
                            </div>

                            <div className="flex items-center gap-2">
                                <RadioGroupItem id="error-handling-disabled" value="disabled" />

                                <label htmlFor="error-handling-disabled">Disabled</label>
                            </div>
                        </RadioGroup>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button
                            disabled={configLoading || (mode === 'override' && !selectedWorkflowId)}
                            label="Save"
                            onClick={handleSave}
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowErrorHandlingDialog;
