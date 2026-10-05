import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import {ComponentIcon, WorkflowIcon} from 'lucide-react';
import {ReactNode} from 'react';

import {McpServerToolsContentProps} from './McpServerTabs';

interface McpServerToolsPanelProps extends McpServerToolsContentProps {
    componentList: ReactNode;
    isComponentListEmpty: boolean;
    isWorkflowListEmpty: boolean;
    workflowList: ReactNode;
}

const McpServerToolsPanel = ({
    activeToolsTab,
    componentList,
    isComponentListEmpty,
    isWorkflowListEmpty,
    onAddComponentClick,
    onAddWorkflowsClick,
    workflowList,
}: McpServerToolsPanelProps) => {
    if (activeToolsTab === 'components') {
        return isComponentListEmpty ? (
            <div className="flex justify-center py-8">
                <EmptyList
                    button={<Button label="Add Component" onClick={onAddComponentClick} />}
                    icon={<ComponentIcon className="size-24 text-gray-300" />}
                    message="No components added to this server."
                    title="No Components"
                />
            </div>
        ) : (
            componentList
        );
    }

    return isWorkflowListEmpty ? (
        <div className="flex justify-center py-8">
            <EmptyList
                button={<Button label="Add Workflows" onClick={onAddWorkflowsClick} />}
                icon={<WorkflowIcon className="size-24 text-gray-300" />}
                message="No workflows added to this server."
                title="No Workflows"
            />
        </div>
    ) : (
        workflowList
    );
};

export default McpServerToolsPanel;
