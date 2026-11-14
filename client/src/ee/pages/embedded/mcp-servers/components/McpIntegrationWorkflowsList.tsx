import Switch from '@/components/Switch/Switch';
import {Label} from '@/components/ui/label';
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {WorkflowIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface McpIntegrationWorkflowsListProps {
    enabledWorkflowIds: string[];
    onSelectedWorkflowIdsChange: (selectedWorkflowIds: string[]) => void;
    selectedWorkflowIds: string[];
    workflows: Workflow[];
}

const McpIntegrationWorkflowsList = ({
    enabledWorkflowIds,
    onSelectedWorkflowIdsChange,
    selectedWorkflowIds,
    workflows,
}: McpIntegrationWorkflowsListProps) => (
    <div className="min-h-full space-y-5">
        {workflows.map((workflow) => {
            const selected = selectedWorkflowIds.includes(workflow.id!);
            const disabled = !enabledWorkflowIds.includes(workflow.id!) && !selected;
            const label = workflow.label || workflow.id!;

            return (
                <div
                    className="flex items-center rounded-lg border border-stroke-neutral-secondary bg-surface-main p-2"
                    key={workflow.id}
                >
                    <Label
                        className={twMerge(
                            'flex w-full cursor-pointer items-center gap-2 text-base font-normal',
                            selected && 'font-semibold'
                        )}
                        htmlFor={`mcp-workflow-${workflow.id}`}
                    >
                        <div
                            className={twMerge(
                                'rounded-lg bg-surface-neutral-tertiary p-2',
                                selected && 'bg-surface-brand-secondary'
                            )}
                        >
                            <WorkflowIcon
                                className={twMerge(
                                    'size-4 text-content-neutral-secondary transition-colors',
                                    selected && 'text-content-brand-primary'
                                )}
                            />
                        </div>

                        <span className="flex flex-col">
                            {label}

                            {disabled && (
                                <span className="text-xs font-normal text-content-neutral-secondary">
                                    Turned off in the configuration
                                </span>
                            )}
                        </span>
                    </Label>

                    <Switch
                        aria-label={label}
                        checked={selected}
                        disabled={disabled}
                        id={`mcp-workflow-${workflow.id}`}
                        onCheckedChange={(checked) =>
                            onSelectedWorkflowIdsChange(
                                checked
                                    ? [...selectedWorkflowIds, workflow.id!]
                                    : selectedWorkflowIds.filter((workflowId) => workflowId !== workflow.id)
                            )
                        }
                    />
                </div>
            );
        })}
    </div>
);

export default McpIntegrationWorkflowsList;
