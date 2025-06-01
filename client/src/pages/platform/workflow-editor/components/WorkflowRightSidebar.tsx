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
                        icon: BlocksIcon,
                        name: 'Components & Flow Controls',
                        onClick: onComponentsAndFlowControlsClick,
                    },
                ],
                ...(showWorkflowInputs
                    ? [
                          {
                              icon: SlidersIcon,
                              name: 'Workflow Inputs',
                              onClick: onWorkflowInputsClick,
                          },
                      ]
                    : []),
                ...[
                    {
                        icon: CableIcon,
                        name: 'Workflow Outputs',
                        onClick: onWorkflowOutputsClick,
                    },
                    {
                        icon: Code2Icon,
                        name: 'Workflow Code Editor',
                        onClick: onWorkflowCodeEditorClick,
                    },
                    {
                        icon: SparklesIcon,
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

    return (
        <aside className="absolute right-0 m-2 flex flex-col items-center gap-1 rounded-md border border-stroke-neutral-secondary bg-background p-1">
            {rightSidebarNavigation.map((item) => (
                <button
                    className={twMerge(
                        'flex items-center rounded-md p-2 hover:bg-surface-neutral-primary-hover [&_svg]:size-4',

                        item.name === 'Components & Flow Controls' &&
                            rightSidebarOpen &&
                            'bg-surface-brand-secondary text-content-brand-primary',

                        item.name === 'Copilot' &&
                            copilotPanelOpen &&
                            'bg-surface-brand-secondary text-content-brand-primary'
                    )}
                    key={item.name}
                    onClick={item.onClick}
                >
                    <Tooltip>
                        <TooltipTrigger asChild>{item.icon && <item.icon aria-hidden="true" />}</TooltipTrigger>

                        <TooltipContent side="left">{item.name}</TooltipContent>
                    </Tooltip>

                    <span className="sr-only">{item.name}</span>
                </button>
            ))}
        </aside>
    );
};

export default WorkflowRightSidebar;
