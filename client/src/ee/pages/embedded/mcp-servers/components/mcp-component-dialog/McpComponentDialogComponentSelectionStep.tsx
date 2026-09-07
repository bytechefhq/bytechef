import McpComponentSelectionGrid from '@/pages/automation/mcp-servers/components/mcp-component-dialog/McpComponentSelectionGrid';
import {ComponentDefinition} from '@/shared/middleware/graphql';

import useMcpComponentDialogComponentSelectionStep from './hooks/useMcpComponentDialogComponentSelectionStep';

interface ComponentSelectionStepProps {
    open: boolean;
    onComponentSelect: (component: ComponentDefinition) => void;
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
