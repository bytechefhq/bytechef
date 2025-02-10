import {RightSidebar} from '@/shared/layout/RightSidebar';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {CableIcon, Code2Icon, PuzzleIcon, SlidersIcon, SparklesIcon} from 'lucide-react';
import {useMemo} from 'react';

export interface WorkflowRightSidebarProps {
    copilotPanelOpen: boolean;
    onComponentsAndFlowControlsClick: () => void;
    onCopilotClick: () => void;
    onWorkflowCodeEditorClick: () => void;
    onWorkflowInputsClick: () => void;
    onWorkflowOutputsClick: () => void;
    rightSidebarOpen: boolean;
}
const WorkflowRightSidebar = ({
    copilotPanelOpen,
    onComponentsAndFlowControlsClick,
    onCopilotClick,
    onWorkflowCodeEditorClick,
    onWorkflowInputsClick,
    onWorkflowOutputsClick,
    rightSidebarOpen,
}: WorkflowRightSidebarProps) => {
    const ff_1570 = useFeatureFlagsStore()('ff-1570');
    const ff_1840 = useFeatureFlagsStore()('ff-1840');

    const rightSidebarNavigation = useMemo(
        () =>
            [
                {
                    icon: PuzzleIcon,
                    name: 'Components & Flow Controls',
                    onClick: onComponentsAndFlowControlsClick,
                },
                {
                    icon: SlidersIcon,
                    name: 'Workflow Inputs',
                    onClick: onWorkflowInputsClick,
                },
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
        <aside>
            <RightSidebar className="mx-3 mt-4 rounded-lg border" navigation={rightSidebarNavigation} />
        </aside>
    );
};

export default WorkflowRightSidebar;
