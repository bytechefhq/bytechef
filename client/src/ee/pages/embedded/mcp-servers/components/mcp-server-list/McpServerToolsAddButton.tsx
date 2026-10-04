import Button from '@/components/Button/Button';
import {ButtonGroup} from '@/components/ui/button-group';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import McpIntegrationInstanceConfigurationWorkflowDialog from '@/ee/pages/embedded/mcp-servers/components/McpIntegrationInstanceConfigurationWorkflowDialog';
import McpComponentDialog from '@/ee/pages/embedded/mcp-servers/components/mcp-component-dialog/McpComponentDialog';
import useButtonGroupDropdownAlign from '@/shared/hooks/useButtonGroupDropdownAlign';
import {McpServer} from '@/shared/middleware/graphql';
import {ChevronDownIcon, ComponentIcon, WorkflowIcon} from 'lucide-react';
import {useState} from 'react';

interface McpServerToolsAddButtonProps {
    mcpServer: McpServer;
}

const McpServerToolsAddButton = ({mcpServer}: McpServerToolsAddButtonProps) => {
    const [showMcpComponentDialog, setShowMcpComponentDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const {alignOffset, buttonGroupRef, dropdownMenuTriggerRef, handleOpenChange} = useButtonGroupDropdownAlign();

    return (
        <>
            <ButtonGroup ref={buttonGroupRef}>
                <Button label="Add Component" onClick={() => setShowMcpComponentDialog(true)} size="sm" />

                <DropdownMenu onOpenChange={handleOpenChange}>
                    <DropdownMenuTrigger asChild>
                        <Button
                            aria-label="Add Tools"
                            icon={<ChevronDownIcon />}
                            ref={dropdownMenuTriggerRef}
                            size="iconSm"
                        />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="start" alignOffset={alignOffset}>
                        <DropdownMenuItem onClick={() => setShowMcpComponentDialog(true)}>
                            <ComponentIcon /> Add Component
                        </DropdownMenuItem>

                        <DropdownMenuItem onClick={() => setShowWorkflowDialog(true)}>
                            <WorkflowIcon /> Add Workflows
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </ButtonGroup>

            {showMcpComponentDialog && (
                <McpComponentDialog
                    mcpServerId={mcpServer.id}
                    onOpenChange={setShowMcpComponentDialog}
                    open={showMcpComponentDialog}
                />
            )}

            {showWorkflowDialog && (
                <McpIntegrationInstanceConfigurationWorkflowDialog
                    mcpServer={mcpServer}
                    onClose={() => setShowWorkflowDialog(false)}
                />
            )}
        </>
    );
};

export default McpServerToolsAddButton;
