import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {BlocksIcon, CableIcon, Code2Icon, SlidersIcon, SparklesIcon} from 'lucide-react';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

export interface WorkflowRightSidebarProps {
    copilotPanelOpen: boolean;
    onComponentsAndFlowControlsClick: () => void;
    onCopilotClick: () => void;
    onWorkflowCodeEditorClick: () => void;
    onWorkflowInputsClick: () => void;
    onWorkflowOutputsClick: () => void;
    rightSidebarOpen: boolean;
    showWorkflowInputs?: boolean;
}
const WorkflowRightSidebar = ({
    copilotPanelOpen,
    onComponentsAndFlowControlsClick,
    onCopilotClick,
    onWorkflowCodeEditorClick,
    onWorkflowInputsClick,
    onWorkflowOutputsClick,
    rightSidebarOpen,
    showWorkflowInputs = true,
}: WorkflowRightSidebarProps) => {
    const ff_1570 = useFeatureFlagsStore()('ff-1570');
    const ff_1840 = useFeatureFlagsStore()('ff-1840');

    const rightSidebarNavigation = useMemo(
        () =>
            [
                ...[
                    {
                        icon: <BlocksIcon />,
                        name: 'Components & Flow Controls',
                        onClick: onComponentsAndFlowControlsClick,
                    },
                ],
                ...(showWorkflowInputs
                    ? [
                          {
                              icon: <SlidersIcon />,
                              name: 'Workflow Inputs',
                              onClick: onWorkflowInputsClick,
                          },
                      ]
                    : []),
                ...[
                    {
                        icon: <CableIcon />,
                        name: 'Workflow Outputs',
                        onClick: onWorkflowOutputsClick,
                    },
                    {
                        icon: <Code2Icon />,
                        name: 'Workflow Code Editor',
                        onClick: onWorkflowCodeEditorClick,
                    },
                    {
                        icon: <SparklesIcon />,
                        name: 'Copilot',
                        onClick: onCopilotClick,
                    },
                ],
            ].filter((item) => {
                if (item.name === 'Copilot') {
                    return ff_1570;
                }

                if (item.name === 'Workflow Outputs') {
                    return ff_1840;
                }

                return true;
            }),
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [copilotPanelOpen, ff_1840, rightSidebarOpen]
    );

    const activeItemStyling =
        'bg-surface-brand-secondary text-content-brand-primary hover:bg-surface-brand-secondary-hover hover:text-content-brand-primary';

    return (
        <aside className="absolute right-0 m-2 flex flex-col items-center gap-1 rounded-md border border-stroke-neutral-secondary bg-background p-1">
            {rightSidebarNavigation.map((item) => {
                const isActive =
                    (item.name === 'Components & Flow Controls' && rightSidebarOpen) ||
                    (item.name === 'Copilot' && copilotPanelOpen);

                return (
                    <Tooltip key={item.name}>
                        <TooltipTrigger asChild>
                            <Button
                                aria-label={item.name}
                                className={twMerge(isActive && activeItemStyling)}
                                icon={item.icon}
                                key={item.name}
                                onClick={item.onClick}
                                size="iconSm"
                                variant="ghost"
                            />
                        </TooltipTrigger>

                        <TooltipContent side="left">{item.name}</TooltipContent>
                    </Tooltip>
                );
            })}
        </aside>
    );
};

export default WorkflowRightSidebar;
