import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiObservabilityExportFormat,
    AiObservabilityExportScope,
    useCreateAiObservabilityExportJobMutation,
} from '@/shared/middleware/graphql';
import {XIcon} from 'lucide-react';
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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-semibold">New Export</h3>

                    <button className="text-muted-foreground hover:text-foreground" onClick={onClose}>
                        <XIcon className="size-5" />
                    </button>
                </div>

                <fieldset className="mb-4 border-0 p-0">
                    <label className="mb-1 block text-sm font-medium">Scope</label>

                    <select
                        className="w-full rounded-md border bg-background px-3 py-2 text-sm"
                        onChange={(event) => setScope(event.target.value as AiObservabilityExportScope)}
                        value={scope}
                    >
                        {SCOPE_OPTIONS.map((option) => (
                            <option key={option.value} value={option.value}>
                                {option.label}
                            </option>
                        ))}
                    </select>
                </fieldset>

                <fieldset className="mb-6 border-0 p-0">
                    <label className="mb-1 block text-sm font-medium">Format</label>

                    <select
                        className="w-full rounded-md border bg-background px-3 py-2 text-sm"
                        onChange={(event) => setFormat(event.target.value as AiObservabilityExportFormat)}
                        value={format}
                    >
                        {FORMAT_OPTIONS.map((option) => (
                            <option key={option.value} value={option.value}>
                                {option.label}
                            </option>
                        ))}
                    </select>
                </fieldset>

                <div className="flex justify-end gap-2">
                    <button
                        className="rounded-md bg-muted px-4 py-2 text-sm text-muted-foreground hover:bg-muted/80"
                        onClick={onClose}
                    >
                        Cancel
                    </button>

                    <button
                        className="rounded-md bg-primary px-4 py-2 text-sm text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                        disabled={createExportJobMutation.isPending}
                        onClick={handleCreate}
                    >
                        {createExportJobMutation.isPending ? 'Creating...' : 'Create Export'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AiObservabilityExportJobDialog;
