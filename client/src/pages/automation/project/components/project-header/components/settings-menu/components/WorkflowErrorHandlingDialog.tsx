import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
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

    // The query is async: useState initializers ran before it settled, so re-seed the three-state control when the
    // persisted config arrives. Without this the dialog shows 'inherit' on an uncached open and Save would clear a
    // real override/disable.
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
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Error Handling</DialogTitle>

                        <DialogDescription>
                            Explicit disable beats the project&apos;s inherited default; an override runs instead of the
                            project default.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <RadioGroup className="space-y-3" onValueChange={(value) => setMode(value as ModeType)} value={mode}>
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

                <DialogFooter>
                    <DialogClose asChild>
                        <Button label="Cancel" type="button" variant="outline" />
                    </DialogClose>

                    <Button
                        disabled={configLoading || (mode === 'override' && !selectedWorkflowId)}
                        label="Save"
                        onClick={handleSave}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowErrorHandlingDialog;
