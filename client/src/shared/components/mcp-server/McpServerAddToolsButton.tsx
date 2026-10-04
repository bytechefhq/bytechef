import Button from '@/components/Button/Button';
import {ButtonGroup} from '@/components/ui/button-group';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import useButtonGroupDropdownAlign from '@/shared/hooks/useButtonGroupDropdownAlign';
import {ChevronDownIcon, ComponentIcon, WorkflowIcon} from 'lucide-react';
import {ReactNode, useState} from 'react';

interface McpServerAddToolsButtonProps {
    renderMcpComponentDialog: (onClose: () => void) => ReactNode;
    renderWorkflowDialog: (onClose: () => void) => ReactNode;
}

const McpServerAddToolsButton = ({renderMcpComponentDialog, renderWorkflowDialog}: McpServerAddToolsButtonProps) => {
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

            {showMcpComponentDialog && renderMcpComponentDialog(() => setShowMcpComponentDialog(false))}

            {showWorkflowDialog && renderWorkflowDialog(() => setShowWorkflowDialog(false))}
        </>
    );
};

export default McpServerAddToolsButton;
