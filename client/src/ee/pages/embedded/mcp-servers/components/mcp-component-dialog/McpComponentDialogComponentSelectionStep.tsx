import McpComponentSelectionGrid from '@/pages/platform/mcp-servers/components/McpComponentSelectionGrid';
import {McpComponent} from '@/shared/middleware/graphql';

import {McpIntegrationComponentOptionI} from './hooks/useMcpComponentDialog';
import useMcpComponentDialogComponentSelectionStep from './hooks/useMcpComponentDialogComponentSelectionStep';

interface ComponentSelectionStepProps {
    mcpComponent?: McpComponent;
    mcpServerId: string;
    open: boolean;
    onComponentSelect: (component: McpIntegrationComponentOptionI) => void;
}

const McpComponentDialogComponentSelectionStep = ({
    mcpComponent,
    mcpServerId,
    onComponentSelect,
    open,
}: ComponentSelectionStepProps) => {
    const {filteredComponents, isComponentAdded, isLoadingComponents, searchTerm, setSearchTerm} =
        useMcpComponentDialogComponentSelectionStep(open, mcpServerId, mcpComponent);

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
