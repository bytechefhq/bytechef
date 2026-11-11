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
        aiValuesByTool,
        currentStep,
        existingTools,
        handleBack,
        handleComponentSelect,
        handleOpenChange,
        handleSave,
        isSavePending,
        registerToolForm,
        savedParametersByToolName,
        selectedComponent,
        selectedTools,
        setSelectedTools,
    } = useMcpComponentDialog({integrationUnavailable, mcpComponent, mcpServerId, onOpenChange, open});

    return (
        <McpComponentDialogShell
            currentStep={currentStep}
            editing={!!mcpComponent}
            isSavePending={isSavePending}
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
                    mcpComponent={mcpComponent}
                    mcpServerId={mcpServerId}
                    onComponentSelect={handleComponentSelect}
                    open={open ?? true}
                />
            )}

            {currentStep === 'tools' && (
                <McpComponentDialogToolSelectionStep
                    aiValuesByTool={aiValuesByTool}
                    connectionId={mcpComponent?.connectionId}
                    existingTools={existingTools}
                    onToolFormChange={registerToolForm}
                    onToolsChange={setSelectedTools}
                    savedParametersByToolName={savedParametersByToolName}
                    selectedComponent={selectedComponent}
                    selectedTools={selectedTools}
                />
            )}
        </McpComponentDialogShell>
    );
};

export default McpComponentDialog;
