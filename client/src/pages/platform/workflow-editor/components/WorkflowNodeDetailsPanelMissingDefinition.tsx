import Button from '@/components/Button/Button';
import {TriangleAlertIcon, XIcon} from 'lucide-react';
import {ReactNode} from 'react';

interface WorkflowNodeDetailsPanelMissingDefinitionProps {
    closeButton?: ReactNode;
    label?: string;
    message: string;
    onClose: () => void;
    operationSelect?: ReactNode;
    workflowNodeName: string;
}

const WorkflowNodeDetailsPanelMissingDefinition = ({
    closeButton,
    label,
    message,
    onClose,
    operationSelect,
    workflowNodeName,
}: WorkflowNodeDetailsPanelMissingDefinitionProps) => (
    <div className="flex h-full flex-col divide-y divide-muted bg-background dark:bg-popover">
        <header className="flex h-20 shrink-0 items-center justify-between p-4 text-lg font-medium">
            <div className="flex min-w-0 flex-1 flex-col items-start">
                <span className="max-w-full truncate text-lg font-semibold">{label || workflowNodeName}</span>

                <span className="text-sm text-muted-foreground">({workflowNodeName})</span>
            </div>

            {closeButton ? (
                closeButton
            ) : (
                <Button
                    aria-label="Close the node details dialog"
                    icon={<XIcon aria-hidden="true" />}
                    onClick={onClose}
                    size="icon"
                    variant="ghost"
                />
            )}
        </header>

        <main className="flex flex-col">
            {operationSelect}

            <div className="m-4 flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
                <TriangleAlertIcon aria-hidden="true" className="mt-0.5 size-4 shrink-0" />

                <span>{message}</span>
            </div>
        </main>
    </div>
);

export default WorkflowNodeDetailsPanelMissingDefinition;
