import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {AlertTriangleIcon, BlocksIcon, CableIcon, Code2Icon, SlidersIcon, SparklesIcon} from 'lucide-react';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

import {WorkflowIssueSeverityType} from '../stores/useWorkflowIssuesStore';

export interface WorkflowRightSidebarProps {
    copilotPanelOpen: boolean;
    issueCount: number;
    issueSeverity?: WorkflowIssueSeverityType;
    issuesSidebarOpen: boolean;
    onComponentsAndFlowControlsClick: () => void;
    onCopilotClick: () => void;
    onWorkflowCodeEditorClick: () => void;
    onWorkflowInputsClick: () => void;
    onWorkflowIssuesClick: () => void;
    onWorkflowOutputsClick: () => void;
    rightSidebarOpen: boolean;
    showCopilot?: boolean;
    showWorkflowInputs?: boolean;
}
const WorkflowRightSidebar = ({
    copilotPanelOpen,
    issueCount,
    issueSeverity,
    issuesSidebarOpen,
    onComponentsAndFlowControlsClick,
    onCopilotClick,
    onWorkflowCodeEditorClick,
    onWorkflowInputsClick,
    onWorkflowIssuesClick,
    onWorkflowOutputsClick,
    rightSidebarOpen,
    showCopilot = true,
    showWorkflowInputs = true,
}: WorkflowRightSidebarProps) => {
    const ai = useApplicationInfoStore((state) => state.ai);

    const ff_1570 = useFeatureFlagsStore()('ff-1570');
    const ff_1840 = useFeatureFlagsStore()('ff-1840');

    const copilotEnabled = ai.copilot.enabled && ff_1570;

    const rightSidebarNavigation = useMemo(
        () =>
            [
                ...[
                    {
                        icon: (
                            <span className="relative">
                                <AlertTriangleIcon />

                                {issueCount > 0 && (
                                    <span
                                        className={twMerge(
                                            'absolute -top-1.5 -right-2 min-w-4 rounded-full px-1 text-center text-[10px] leading-4 font-semibold text-white',
                                            issueSeverity === 'ERROR'
                                                ? 'bg-content-destructive'
                                                : 'bg-content-onwarning'
                                        )}
                                    >
                                        {issueCount}
                                    </span>
                                )}
                            </span>
                        ),
                        name: 'Workflow Issues',
                        onClick: onWorkflowIssuesClick,
                    },
                ],
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
                    return showCopilot && copilotEnabled;
                }

                if (item.name === 'Workflow Outputs') {
                    return ff_1840;
                }

                return true;
            }),
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [
            copilotEnabled,
            copilotPanelOpen,
            ff_1840,
            issueCount,
            issueSeverity,
            issuesSidebarOpen,
            rightSidebarOpen,
            showCopilot,
        ]
    );

    const activeItemStyling =
        'bg-surface-brand-secondary text-content-brand-primary hover:bg-surface-brand-secondary-hover hover:text-content-brand-primary';

    return (
        <aside className="absolute right-0 m-2 flex flex-col items-center gap-1 rounded-md border border-stroke-neutral-secondary bg-background p-1">
            {rightSidebarNavigation.map((item) => {
                const isActive =
                    (item.name === 'Components & Flow Controls' && rightSidebarOpen) ||
                    (item.name === 'Copilot' && copilotPanelOpen) ||
                    (item.name === 'Workflow Issues' && issuesSidebarOpen);

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
