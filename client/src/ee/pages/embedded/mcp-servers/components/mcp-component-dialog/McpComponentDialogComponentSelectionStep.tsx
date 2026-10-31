import McpComponentSelectionGrid from '@/pages/platform/mcp-servers/components/McpComponentSelectionGrid';

import {McpIntegrationComponentOptionI} from './hooks/useMcpComponentDialog';
import useMcpComponentDialogComponentSelectionStep from './hooks/useMcpComponentDialogComponentSelectionStep';

interface ComponentSelectionStepProps {
    componentName?: string;
    open: boolean;
    onComponentSelect: (component: McpIntegrationComponentOptionI) => void;
}

const McpComponentDialogComponentSelectionStep = ({
    componentName,
    onComponentSelect,
    open,
}: ComponentSelectionStepProps) => {
    const {filteredComponents, isLoadingComponents, searchTerm, setSearchTerm} =
        useMcpComponentDialogComponentSelectionStep(open, componentName);

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
