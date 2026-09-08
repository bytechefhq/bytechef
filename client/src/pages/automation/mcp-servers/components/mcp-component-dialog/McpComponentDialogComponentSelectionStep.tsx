import McpComponentSelectionGrid from '@/pages/platform/mcp-servers/components/McpComponentSelectionGrid';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';

import useMcpComponentDialogComponentSelectionStep from './hooks/useMcpComponentDialogComponentSelectionStep';

interface ComponentSelectionStepProps {
    open: boolean;
    onComponentSelect: (component: ComponentDefinitionBasic) => void;
}

const McpComponentDialogComponentSelectionStep = ({onComponentSelect, open}: ComponentSelectionStepProps) => {
    const {filteredComponents, isLoadingComponents, searchTerm, setSearchTerm} =
        useMcpComponentDialogComponentSelectionStep(open);

    return (
        <McpComponentSelectionGrid
            components={filteredComponents}
            isLoading={isLoadingComponents}
            onComponentSelect={onComponentSelect}
            onSearchTermChange={setSearchTerm}
            searchTerm={searchTerm}
        />
    );
};

export default McpComponentDialogComponentSelectionStep;
