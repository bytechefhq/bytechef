import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useState} from 'react';

const useMcpComponentDialogComponentSelectionStep = (open: boolean) => {
    const [searchTerm, setSearchTerm] = useState('');

    const {data: components = [], isLoading: isLoadingComponents} = useGetComponentDefinitionsQuery(
        {
            actionDefinitions: true,
        },
        open
    );

    const filteredComponents = components.filter((component) => {
        const hasTools = component.clusterElementsCount?.TOOLS && component.clusterElementsCount.TOOLS > 0;

        if (!hasTools) {
            return false;
        }

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
