import McpComponentSelectionGrid from '@/pages/platform/mcp-servers/components/McpComponentSelectionGrid';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';

import useMcpComponentDialogComponentSelectionStep from './hooks/useMcpComponentDialogComponentSelectionStep';

interface ComponentSelectionStepProps {
    mcpServerId: string;
    open: boolean;
    onComponentSelect: (component: ComponentDefinitionBasic) => void;
}

const McpComponentDialogComponentSelectionStep = ({
    mcpServerId,
    onComponentSelect,
    open,
}: ComponentSelectionStepProps) => {
    const {filteredComponents, isComponentAdded, isLoadingComponents, searchTerm, setSearchTerm} =
        useMcpComponentDialogComponentSelectionStep(open, mcpServerId);

    return (
        <McpComponentSelectionGrid
            components={filteredComponents}
            isComponentAdded={isComponentAdded}
            isLoading={isLoadingComponents}
            onComponentSelect={onComponentSelect}
            onSearchTermChange={setSearchTerm}
            searchTerm={searchTerm}
        />
    );
};

export default McpComponentDialogComponentSelectionStep;
