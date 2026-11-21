import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {McpTool} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {ComponentIcon} from 'lucide-react';
import {ReactNode, useMemo, useState} from 'react';

import McpComponentToolListItem from './McpComponentToolListItem';

interface McpComponentToolListProps {
    componentName: string;
    componentVersion: number;
    connectionId?: string | null;
    embedded?: boolean;
    mcpTools?: Array<McpTool | null> | null;
    renderEditDialog: (open: boolean, onOpenChange: (open: boolean) => void) => ReactNode;
}

const McpComponentToolList = ({
    componentName,
    componentVersion,
    connectionId,
    embedded = false,
    mcpTools,
    renderEditDialog,
}: McpComponentToolListProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const canEditAutomationMcpServer = useHasWorkspaceScope(currentWorkspaceId, 'MCP_EDIT');
    const canEditEmbeddedMcpServer = useHasEmbeddedScope('MCP_EDIT');

    const canEditMcpServer = embedded ? canEditEmbeddedMcpServer : canEditAutomationMcpServer;

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
        <div className="divide-y divide-stroke-neutral-primary">
            {tools.map((tool) => (
                <McpComponentToolListItem
                    componentName={componentName}
                    componentVersion={componentVersion}
                    connectionId={connectionId}
                    connectionRequired={componentDefinition?.connectionRequired ?? true}
                    description={toolDescriptionsByName[tool.name]}
                    embedded={embedded}
                    enabledSwitchVisible={!embedded}
                    key={tool.name}
                    mcpTool={tool}
                />
            ))}
        </div>
    ) : (
        <div className="flex justify-center py-4">
            <EmptyList
                button={canEditMcpServer && <Button label="Edit Tools" onClick={() => setShowEditDialog(true)} />}
                icon={<ComponentIcon className="size-12 text-stroke-neutral-tertiary" />}
                message="This component has no selected tools."
                title="No Tools"
            />

            {renderEditDialog(showEditDialog, setShowEditDialog)}
        </div>
    );
};

export default McpComponentToolList;
