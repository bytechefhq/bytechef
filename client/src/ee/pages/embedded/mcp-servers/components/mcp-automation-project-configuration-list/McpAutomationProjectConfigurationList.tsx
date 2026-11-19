import Button from '@/components/Button/Button';
import McpToolListSkeleton from '@/pages/platform/mcp-servers/components/McpToolListSkeleton';
import {McpServer} from '@/shared/middleware/graphql';
import {PlusIcon} from 'lucide-react';

import McpAutomationProjectConfigurationDialog from './McpAutomationProjectConfigurationDialog';
import McpAutomationProjectConfigurationListItem from './McpAutomationProjectConfigurationListItem';
import useMcpAutomationProjectConfigurationList from './hooks/useMcpAutomationProjectConfigurationList';

interface McpAutomationProjectConfigurationListProps {
    mcpServer: McpServer;
}

const McpAutomationProjectConfigurationList = ({mcpServer}: McpAutomationProjectConfigurationListProps) => {
    const {isLoading, mcpAutomationProjectConfigurations} = useMcpAutomationProjectConfigurationList(mcpServer.id);

    return (
        <section className="mt-4 divide-y divide-stroke-neutral-primary">
            <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold">Automations</h3>

                <McpAutomationProjectConfigurationDialog
                    mcpServer={mcpServer}
                    triggerNode={<Button icon={<PlusIcon />} label="Add Automations" size="sm" variant="secondary" />}
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
        </section>
    );
};

export default McpAutomationProjectConfigurationList;
