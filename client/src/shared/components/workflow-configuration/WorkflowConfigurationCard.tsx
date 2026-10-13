import Switch from '@/components/Switch/Switch';
import {Label} from '@/components/ui/label';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {FileInputIcon, Link2Icon, WorkflowIcon} from 'lucide-react';
import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface WorkflowConfigurationCardProps {
    connectionCount: number;
    connectionsContent: ReactNode;
    enabled: boolean;
    inputCount: number;
    inputsContent: ReactNode;
    label?: string;
    onEnabledChange: (enabled: boolean) => void;
    showWorkflowToggle: boolean;
    switchId: string;
}

const WorkflowConfigurationCard = ({
    connectionCount,
    connectionsContent,
    enabled,
    inputCount,
    inputsContent,
    label,
    onEnabledChange,
    showWorkflowToggle,
    switchId,
}: WorkflowConfigurationCardProps) => (
    <div
        className={twMerge(
            showWorkflowToggle &&
                'flex flex-col gap-3 rounded-lg border border-stroke-neutral-secondary bg-surface-main p-2'
        )}
    >
        {showWorkflowToggle && (
            <div className="flex items-center rounded-lg">
                <Label
                    className={twMerge(
                        'flex w-full cursor-pointer items-center gap-2 text-base font-normal',
                        enabled && 'font-semibold'
                    )}
                    htmlFor={switchId}
                >
                    <div
                        className={twMerge(
                            'rounded-lg bg-surface-neutral-tertiary p-2',
                            enabled && 'bg-surface-brand-secondary'
                        )}
                    >
                        <WorkflowIcon
                            className={twMerge(
                                'size-4 text-content-neutral-secondary transition-colors',
                                enabled && 'text-content-brand-primary'
                            )}
                        />
                    </div>

                    {label}
                </Label>

                <Switch aria-label={label} checked={enabled} id={switchId} onCheckedChange={onEnabledChange} />
            </div>
        )}

        {(enabled || !showWorkflowToggle) && (
            <Tabs className="flex flex-col gap-2.5" defaultValue="connections">
                <TabsList className="flex w-full">
                    <TabsTrigger className="flex w-full data-[state=active]:shadow-none" value="connections">
                        <Link2Icon className="mr-2 size-4" />

                        <span>Connections</span>

                        <span className="ml-1">({connectionCount})</span>
                    </TabsTrigger>

                    <TabsTrigger className="flex w-full data-[state=active]:shadow-none" value="inputs">
                        <FileInputIcon className="mr-2 size-4" />

                        <span>Inputs</span>

                        <span className="ml-1">({inputCount})</span>
                    </TabsTrigger>
                </TabsList>

                <TabsContent className="mt-0 space-y-4" tabIndex={-1} value="connections">
                    {connectionsContent}
                </TabsContent>

                <TabsContent className="mt-0" tabIndex={-1} value="inputs">
                    {inputsContent}
                </TabsContent>
            </Tabs>
        )}
    </div>
);

export default WorkflowConfigurationCard;
