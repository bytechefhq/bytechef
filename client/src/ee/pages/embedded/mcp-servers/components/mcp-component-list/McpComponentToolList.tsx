import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import {McpComponent, McpTool} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {ComponentIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

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

    const {data: componentDefinition} = useGetComponentDefinitionQuery({componentName, componentVersion});

    const tools = mcpTools?.filter((tool): tool is McpTool => tool !== null && tool.name !== null) || [];

    const toolDescriptionsByName = useMemo(() => {
        const descriptions: Record<string, string> = {};

        componentDefinition?.clusterElements
            ?.filter((element) => element.type === 'TOOLS')
            .forEach((element) => {
                if (element.description) {
                    descriptions[element.name] = element.description;
                }
            });

        return descriptions;
    }, [componentDefinition?.clusterElements]);

    return tools.length > 0 ? (
        <div className="flex flex-col gap-1">
            {tools.map((tool) => (
                <McpComponentToolListItem
                    componentName={componentName}
                    componentVersion={componentVersion}
                    connectionId={connectionId}
                    description={toolDescriptionsByName[tool.name]}
                    key={tool.name}
                    mcpTool={tool}
                />
            ))}
        </div>
    ) : (
        <div className="flex justify-center py-4">
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
