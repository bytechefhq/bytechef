import LoadingIcon from '@/components/LoadingIcon';
import {Checkbox} from '@/components/ui/checkbox';
import {ComponentDefinition, McpToolsByComponentIdQuery} from '@/shared/middleware/graphql';

import useMcpComponentDialogToolSelectionStep, {SelectedToolI} from './hooks/useMcpComponentDialogToolSelectionStep';

interface ToolSelectionStepProps {
    existingTools?: McpToolsByComponentIdQuery;
    onToolsChange: (tools: SelectedToolI[]) => void;
    selectedComponent: ComponentDefinition | null;
    selectedTools: SelectedToolI[];
}

const McpComponentDialogToolSelectionStep = ({
    existingTools,
    onToolsChange,
    selectedComponent,
    selectedTools,
}: ToolSelectionStepProps) => {
    const {
        allToolsSelected,
        handleSelectAllTools,
        handleToolToggle,
        isLoadingComponentDefinition,
        selectAllCheckboxRef,
        toolElements,
    } = useMcpComponentDialogToolSelectionStep({
        existingTools,
        onToolsChange,
        selectedComponent,
        selectedTools,
    });

    return (
        <div className="space-y-4 py-4">
            {isLoadingComponentDefinition ? (
                <div className="flex items-center justify-center py-8">
                    <LoadingIcon className="size-6" />
                </div>
            ) : toolElements.length === 0 ? (
                <div className="py-8 text-center text-muted-foreground">No tools available for this component.</div>
            ) : (
                <>
                    <div className="flex items-center space-x-3">
                        <Checkbox
                            checked={allToolsSelected}
                            id="select-all-tools"
                            onCheckedChange={(checked) => handleSelectAllTools(checked as boolean)}
                            ref={selectAllCheckboxRef}
                        />

                        <label className="cursor-pointer text-sm font-medium leading-none" htmlFor="select-all-tools">
                            Select All Tools ({toolElements.length})
                        </label>
                    </div>

                    <div className="divide-y">
                        {toolElements.map((tool) => (
                            <div className="flex items-center space-x-3 py-3 hover:bg-gray-50" key={tool.name}>
                                <Checkbox
                                    checked={selectedTools.some((selectedTool) => selectedTool.name === tool.name)}
                                    id={tool.name}
                                    onCheckedChange={(checked) => handleToolToggle(tool, checked as boolean)}
                                />

                                <div className="flex-1">
                                    <label className="cursor-pointer text-sm font-medium" htmlFor={tool.name}>
                                        {tool.title || tool.name}
                                    </label>

                                    {tool.description && (
                                        <p className="mt-1 text-sm text-muted-foreground">{tool.description}</p>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                </>
            )}
        </div>
    );
};

export default McpComponentDialogToolSelectionStep;
