import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiObservabilityExportFormat,
    AiObservabilityExportScope,
    useCreateAiObservabilityExportJobMutation,
} from '@/shared/middleware/graphql';
import {useState} from 'react';

interface AiObservabilityExportJobDialogProps {
    onClose: () => void;
}

const SCOPE_OPTIONS: {label: string; value: AiObservabilityExportScope}[] = [
    {label: 'Traces', value: AiObservabilityExportScope.Traces},
    {label: 'Request Logs', value: AiObservabilityExportScope.RequestLogs},
    {label: 'Sessions', value: AiObservabilityExportScope.Sessions},
    {label: 'Prompts', value: AiObservabilityExportScope.Prompts},
];

const FORMAT_OPTIONS: {label: string; value: AiObservabilityExportFormat}[] = [
    {label: 'CSV', value: AiObservabilityExportFormat.Csv},
    {label: 'JSON', value: AiObservabilityExportFormat.Json},
    {label: 'JSONL', value: AiObservabilityExportFormat.Jsonl},
];

const AiObservabilityExportJobDialog = ({onClose}: AiObservabilityExportJobDialogProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [format, setFormat] = useState<AiObservabilityExportFormat>(AiObservabilityExportFormat.Json);
    const [scope, setScope] = useState<AiObservabilityExportScope>(AiObservabilityExportScope.Traces);

    const createExportJobMutation = useCreateAiObservabilityExportJobMutation({});

    const handleCreate = () => {
        createExportJobMutation.mutate(
            {
                format: format,
                scope: scope,
                workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
            },
            {
                onSuccess: () => {
                    onClose();
                },
            }
        );
    };

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
                    <DialogTitle>New Export</DialogTitle>
                </DialogHeader>

                <fieldset className="border-0">
                    <Label className="mb-1 block" htmlFor="ai-observability-export-job-scope">
                        Scope
                    </Label>

                    <Select onValueChange={(value) => setScope(value as AiObservabilityExportScope)} value={scope}>
                        <SelectTrigger id="ai-observability-export-job-scope">
                            <SelectValue placeholder="Select a scope" />
                        </SelectTrigger>

                        <SelectContent>
                            {SCOPE_OPTIONS.map((option) => (
                                <SelectItem key={option.value} value={option.value}>
                                    {option.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </fieldset>

                <fieldset className="border-0">
                    <Label className="mb-1 block" htmlFor="ai-observability-export-job-format">
                        Format
                    </Label>

                    <Select onValueChange={(value) => setFormat(value as AiObservabilityExportFormat)} value={format}>
                        <SelectTrigger id="ai-observability-export-job-format">
                            <SelectValue placeholder="Select a format" />
                        </SelectTrigger>

                        <SelectContent>
                            {FORMAT_OPTIONS.map((option) => (
                                <SelectItem key={option.value} value={option.value}>
                                    {option.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </fieldset>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={createExportJobMutation.isPending}
                        label={createExportJobMutation.isPending ? 'Creating...' : 'Create Export'}
                        onClick={handleCreate}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiObservabilityExportJobDialog;
