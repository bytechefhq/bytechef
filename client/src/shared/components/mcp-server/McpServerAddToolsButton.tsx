import Button from '@/components/Button/Button';
import {ButtonGroup, ButtonGroupSeparator} from '@/components/ui/button-group';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import useButtonGroupDropdownAlign from '@/shared/hooks/useButtonGroupDropdownAlign';
import {McpServer} from '@/shared/middleware/graphql';
import {ChevronDownIcon, ComponentIcon, WorkflowIcon} from 'lucide-react';
import {ComponentType, useState} from 'react';

export interface McpServerComponentDialogProps {
    mcpServerId: string;
    onOpenChange: (open: boolean) => void;
    open: boolean;
}

export interface McpServerWorkflowDialogProps {
    mcpServer: McpServer;
    onClose: () => void;
}

export interface McpServerAddToolsButtonProps {
    mcpComponentDialog: ComponentType<McpServerComponentDialogProps>;
    mcpServer: McpServer;
    workflowDialog: ComponentType<McpServerWorkflowDialogProps>;
}

const McpServerAddToolsButton = ({
    mcpComponentDialog: McpComponentDialog,
    mcpServer,
    workflowDialog: WorkflowDialog,
}: McpServerAddToolsButtonProps) => {
    const [showMcpComponentDialog, setShowMcpComponentDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const {alignOffset, buttonGroupRef, dropdownMenuTriggerRef, handleOpenChange} = useButtonGroupDropdownAlign();

    return (
        <>
            <ButtonGroup ref={buttonGroupRef}>
                <Button
                    label="Add Component"
                    onClick={() => setShowMcpComponentDialog(true)}
                    size="sm"
                    variant="secondary"
                />

                <ButtonGroupSeparator />

                <DropdownMenu onOpenChange={handleOpenChange}>
                    <DropdownMenuTrigger asChild>
                        <Button
                            aria-label="Add Tools"
                            icon={<ChevronDownIcon />}
                            ref={dropdownMenuTriggerRef}
                            size="iconSm"
                            variant="secondary"
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
                <WorkflowDialog mcpServer={mcpServer} onClose={() => setShowWorkflowDialog(false)} />
            )}
        </>
    );
};

export default McpServerAddToolsButton;
