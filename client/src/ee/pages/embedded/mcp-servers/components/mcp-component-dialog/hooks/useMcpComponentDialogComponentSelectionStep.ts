import {useMcpComponentDefinitionsQuery} from '@/shared/middleware/graphql';
import {useState} from 'react';

const useMcpComponentDialogComponentSelectionStep = (open: boolean) => {
    const [searchTerm, setSearchTerm] = useState('');

    const {data, isLoading: isLoadingComponents} = useMcpComponentDefinitionsQuery(
        {},
        {
            enabled: open,
        }
    );

    const components = data?.mcpComponentDefinitions ?? [];

    const filteredComponents = components.filter((component) => {
        const searchLower = searchTerm.toLowerCase();
        const nameMatch = component.name.toLowerCase().includes(searchLower);
        const titleMatch = component.title?.toLowerCase().includes(searchLower);

        return nameMatch || titleMatch;
    });

    return {
        filteredComponents,
        isLoadingComponents,
        searchTerm,
        setSearchTerm,
    };
};

export default useMcpComponentDialogComponentSelectionStep;
