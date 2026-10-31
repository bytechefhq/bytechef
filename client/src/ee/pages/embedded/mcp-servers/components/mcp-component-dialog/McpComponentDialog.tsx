import McpComponentDialogShell, {
    type McpComponentDialogProps,
} from '@/shared/components/mcp-server/McpComponentDialogShell';

import McpComponentDialogComponentSelectionStep from './McpComponentDialogComponentSelectionStep';
import McpComponentDialogToolSelectionStep from './McpComponentDialogToolSelectionStep';
import useMcpComponentDialog from './hooks/useMcpComponentDialog';

const McpComponentDialog = ({
    integrationUnavailable,
    mcpComponent,
    mcpServerId,
    onOpenChange,
    open,
    triggerNode,
}: McpComponentDialogProps & {integrationUnavailable?: boolean}) => {
    const {
        currentStep,
        existingTools,
        handleBack,
        handleComponentSelect,
        handleOpenChange,
        handleSave,
        selectedComponent,
        selectedTools,
        setSelectedTools,
    } = useMcpComponentDialog({integrationUnavailable, mcpComponent, mcpServerId, onOpenChange, open});

    return (
        <McpComponentDialogShell
            currentStep={currentStep}
            editing={!!mcpComponent}
            onBack={handleBack}
            onOpenChange={handleOpenChange}
            onSave={handleSave}
            open={open}
            selectedComponent={selectedComponent}
            selectedToolCount={selectedTools.length}
            triggerNode={triggerNode}
        >
            {currentStep === 'components' && (
                <McpComponentDialogComponentSelectionStep
                    componentName={mcpComponent?.componentName}
                    onComponentSelect={handleComponentSelect}
                    open={open ?? true}
                />
            )}

            {currentStep === 'tools' && (
                <McpComponentDialogToolSelectionStep
                    existingTools={existingTools}
                    onToolsChange={setSelectedTools}
                    selectedComponent={selectedComponent}
                    selectedTools={selectedTools}
                />
            )}
        </McpComponentDialogShell>
    );
};

export default McpComponentDialog;
