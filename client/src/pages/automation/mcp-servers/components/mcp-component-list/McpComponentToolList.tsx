import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import {McpComponent, McpTool} from '@/shared/middleware/graphql';
import {ComponentIcon} from 'lucide-react';
import {useState} from 'react';

import McpComponentDialog from '../mcp-component-dialog/McpComponentDialog';
import McpComponentToolListItem from './McpComponentToolListItem';

interface McpComponentToolListProps {
    componentName: string;
    componentVersion: number;
    connectionId?: string | null;
    mcpComponent: McpComponent;
    mcpServerId: string;
    mcpTools?: Array<McpTool | null> | null;
}

const McpComponentToolList = ({
    componentName,
    componentVersion,
    connectionId,
    mcpComponent,
    mcpServerId,
    mcpTools,
}: McpComponentToolListProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);

    const tools = mcpTools?.filter((tool): tool is McpTool => tool !== null && tool.name !== null) || [];

    return tools.length > 0 ? (
        <div className="flex flex-wrap gap-2 py-2 pl-6">
            {tools.map((tool) => (
                <McpComponentToolListItem
                    componentName={componentName}
                    componentVersion={componentVersion}
                    connectionId={connectionId}
                    key={tool.name}
                    mcpTool={tool}
                />
            ))}
        </div>
    ) : (
        <div className="flex justify-center py-4 pl-6">
            <EmptyList
                button={<Button label="Edit Tools" onClick={() => setShowEditDialog(true)} />}
                icon={<ComponentIcon className="size-12 text-gray-300" />}
                message="This component has no selected tools."
                title="No Tools"
            />

            <McpComponentDialog
                mcpComponent={mcpComponent}
                mcpServerId={mcpServerId}
                onOpenChange={setShowEditDialog}
                open={showEditDialog}
            />
        </div>
    );
};

export default McpComponentToolList;
