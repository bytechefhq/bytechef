import Button from '@/components/Button/Button';
import McpToolListSkeleton from '@/pages/platform/mcp-servers/components/McpToolListSkeleton';
import {McpServer} from '@/shared/middleware/graphql';
import {PlusIcon} from 'lucide-react';
import {useState} from 'react';

import McpAutomationProjectConfigurationDialog from './McpAutomationProjectConfigurationDialog';
import McpAutomationProjectConfigurationListItem from './McpAutomationProjectConfigurationListItem';
import useMcpAutomationProjectConfigurationList from './hooks/useMcpAutomationProjectConfigurationList';

interface McpAutomationProjectConfigurationListProps {
    mcpServer: McpServer;
}

const McpAutomationProjectConfigurationList = ({mcpServer}: McpAutomationProjectConfigurationListProps) => {
    const [showAddDialog, setShowAddDialog] = useState(false);

    const {isLoading, mcpAutomationProjectConfigurations} = useMcpAutomationProjectConfigurationList(mcpServer.id);

    return (
        <section className="mt-4 flex flex-col gap-1.5">
            <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold">Automations</h3>

                <Button
                    icon={<PlusIcon />}
                    label="Add Automations"
                    onClick={() => setShowAddDialog(true)}
                    size="sm"
                    variant="secondary"
                />
            </div>

            {isLoading && <McpToolListSkeleton />}

            {!isLoading && mcpAutomationProjectConfigurations.length === 0 && (
                <p className="text-sm text-content-neutral-secondary">No automations added to this server.</p>
            )}

            {!isLoading &&
                mcpAutomationProjectConfigurations.map((mcpAutomationProjectConfiguration) => (
                    <McpAutomationProjectConfigurationListItem
                        key={mcpAutomationProjectConfiguration.id}
                        mcpAutomationProjectConfiguration={mcpAutomationProjectConfiguration}
                        mcpServer={mcpServer}
                    />
                ))}

            {showAddDialog && (
                <McpAutomationProjectConfigurationDialog
                    mcpServer={mcpServer}
                    onClose={() => setShowAddDialog(false)}
                />
            )}
        </section>
    );
};

export default McpAutomationProjectConfigurationList;
