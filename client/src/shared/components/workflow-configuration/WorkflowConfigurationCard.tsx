import Switch from '@/components/Switch/Switch';
import {Label} from '@/components/ui/label';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {AlertTriangleIcon, FileInputIcon, Link2Icon, WorkflowIcon, WrenchIcon} from 'lucide-react';
import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface WorkflowConfigurationCardProps {
    badge?: ReactNode;
    connectionCount: number;
    connectionsContent: ReactNode;
    connectionsWarning?: boolean;
    defaultTab?: 'connections' | 'inputs';
    disabled?: boolean;
    disabledMessage?: string;
    enabled: boolean;
    forceMountContent?: boolean;
    inputCount: number;
    inputsContent: ReactNode;
    label?: string;
    onEnabledChange: (enabled: boolean) => void;
    showTabs?: boolean;
    showWorkflowToggle: boolean;
    switchId: string;
    toolContent?: ReactNode;
}

const WorkflowConfigurationCard = ({
    badge,
    connectionCount,
    connectionsContent,
    connectionsWarning = false,
    defaultTab = 'connections',
    disabled = false,
    disabledMessage,
    enabled,
    forceMountContent = false,
    inputCount,
    inputsContent,
    label,
    onEnabledChange,
    showTabs = true,
    showWorkflowToggle,
    switchId,
    toolContent,
}: WorkflowConfigurationCardProps) => (
    <div
        aria-label={showWorkflowToggle ? label : undefined}
        className={twMerge(
            showWorkflowToggle &&
                'flex flex-col gap-3 rounded-lg border border-stroke-neutral-secondary bg-surface-main p-2'
        )}
        role={showWorkflowToggle ? 'group' : undefined}
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

                    <span className="flex flex-col">
                        {badge ? (
                            <span className="flex items-center gap-2">
                                {label}

                                {badge}
                            </span>
                        ) : (
                            label
                        )}

                        {disabled && disabledMessage && (
                            <span className="text-xs font-normal text-content-neutral-secondary">
                                {disabledMessage}
                            </span>
                        )}
                    </span>
                </Label>

                <Switch
                    aria-label={label}
                    checked={enabled}
                    disabled={disabled}
                    id={switchId}
                    onCheckedChange={onEnabledChange}
                />
            </div>
        )}

        {showTabs && (enabled || !showWorkflowToggle) && (
            <Tabs className="flex flex-col gap-2.5" defaultValue={defaultTab}>
                <TabsList className="flex w-full">
                    <TabsTrigger className="flex w-full data-[state=active]:shadow-none" value="connections">
                        <Link2Icon className="mr-2 size-4" />

                        <span>Connections</span>

                        <span className="ml-1">({connectionCount})</span>

                        {connectionsWarning && (
                            <AlertTriangleIcon
                                aria-label="Missing connection"
                                className="ml-1 size-4 text-content-warning-primary"
                                role="img"
                            />
                        )}
                    </TabsTrigger>

                    <TabsTrigger className="flex w-full data-[state=active]:shadow-none" value="inputs">
                        <FileInputIcon className="mr-2 size-4" />

                        <span>Inputs</span>

                        <span className="ml-1">({inputCount})</span>
                    </TabsTrigger>

                    {toolContent && (
                        <TabsTrigger className="flex w-full data-[state=active]:shadow-none" value="tool">
                            <WrenchIcon className="mr-2 size-4" />

                            <span>Tool</span>
                        </TabsTrigger>
                    )}
                </TabsList>

                <TabsContent
                    className="mt-0 space-y-4 pt-3.5 data-[state=inactive]:hidden"
                    forceMount={forceMountContent || undefined}
                    tabIndex={-1}
                    value="connections"
                >
                    {connectionsContent}
                </TabsContent>

                <TabsContent
                    className="mt-0 pt-3.5 data-[state=inactive]:hidden"
                    forceMount={forceMountContent || undefined}
                    tabIndex={-1}
                    value="inputs"
                >
                    {inputsContent}
                </TabsContent>

                {toolContent && (
                    <TabsContent className="mt-0 pt-3.5" tabIndex={-1} value="tool">
                        {toolContent}
                    </TabsContent>
                )}
            </Tabs>
        )}
    </div>
);

export default WorkflowConfigurationCard;
