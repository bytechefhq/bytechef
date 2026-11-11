import {MultiSelect} from '@/components/MultiSelect/MultiSelect';
import McpComponentDialogShell, {
    type McpComponentDialogProps,
} from '@/shared/components/mcp-server/McpComponentDialogShell';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';

import McpComponentDialogComponentSelectionStep from './McpComponentDialogComponentSelectionStep';
import McpComponentDialogToolSelectionStep from './McpComponentDialogToolSelectionStep';
import useMcpComponentDialog from './hooks/useMcpComponentDialog';

const McpComponentDialog = ({mcpComponent, mcpServerId, onOpenChange, open, triggerNode}: McpComponentDialogProps) => {
    const {
        aiValuesByTool,
        authoritiesLoading,
        authorityOptions,
        currentStep,
        existingTools,
        handleBack,
        handleComponentSelect,
        handleOpenChange,
        handleSave,
        isSavePending,
        registerToolForm,
        requiredAuthorities,
        savedParametersByToolName,
        selectedComponent,
        selectedConnection,
        selectedTools,
        setRequiredAuthorities,
        setSelectedConnection,
        setSelectedTools,
    } = useMcpComponentDialog({mcpComponent, mcpServerId, onOpenChange, open});

    const ff_6383 = useFeatureFlagsStore()('ff-6383');

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
                    mcpServerId={mcpServerId}
                    onComponentSelect={handleComponentSelect}
                    open={open ?? true}
                />
            )}

            {currentStep === 'tools' && (
                <>
                    <McpComponentDialogToolSelectionStep
                        aiValuesByTool={aiValuesByTool}
                        existingTools={existingTools}
                        mcpComponent={mcpComponent}
                        onConnectionChange={setSelectedConnection}
                        onToolFormChange={registerToolForm}
                        onToolsChange={setSelectedTools}
                        open={open ?? true}
                        savedParametersByToolName={savedParametersByToolName}
                        selectedComponent={selectedComponent}
                        selectedConnection={selectedConnection}
                        selectedTools={selectedTools}
                    />

                    {ff_6383 && (
                        <fieldset className="mt-4 space-y-2 border-0 p-0 px-1">
                            <label className="text-sm font-medium" htmlFor="mcp-component-required-authorities">
                                Required Authorities
                            </label>

                            <MultiSelect
                                id="mcp-component-required-authorities"
                                onValueChange={setRequiredAuthorities}
                                options={authorityOptions}
                                optionsLoading={authoritiesLoading}
                                placeholder="Select authorities"
                                searchable
                                value={requiredAuthorities}
                            />

                            <p className="text-xs text-muted-foreground">
                                When the server enforces tool authorization, only callers holding one of these
                                authorities see this component&apos;s tools.
                            </p>
                        </fieldset>
                    )}
                </>
            )}
        </McpComponentDialogShell>
    );
};

export default McpComponentDialog;
