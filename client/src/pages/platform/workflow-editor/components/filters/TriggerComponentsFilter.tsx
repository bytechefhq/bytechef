import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {ReactNode} from 'react';

import ComponentsFilter from './ComponentsFilter';

interface TriggerComponentsFilterProps {
    deselectAllCategories: () => void;
    filterState: {
        activeView: 'all' | 'filtered';
        filteredCount: number;
        savedCategories: string[];
        searchValue: string;
        selectedCategories: string[];
    };
    filteredCategories: {label: string; icon?: ReactNode}[];
    filteredComponents: ComponentDefinitionBasic[];
    setActiveView: (view: 'all' | 'filtered') => void;
    setSearchValue: (value: string) => void;
    toggleCategory: (category: string) => void;
    triggerComponentDefinitions: ComponentDefinitionBasic[];
}

const TriggerComponentsFilter = ({
    deselectAllCategories,
    filterState,
    filteredCategories,
    filteredComponents,
    setActiveView,
    setSearchValue,
    toggleCategory,
    triggerComponentDefinitions,
}: TriggerComponentsFilterProps) => {
    return (
        <ComponentsFilter
            componentDefinitions={triggerComponentDefinitions}
            deselectAllCategories={deselectAllCategories}
            filterLabel="triggers"
            filterState={filterState}
            filterTooltip="Filter triggers by category"
            filteredCategories={filteredCategories}
            filteredComponents={filteredComponents}
            setActiveView={setActiveView}
            setSearchValue={setSearchValue}
            toggleCategory={toggleCategory}
        />
    );
};

export default TriggerComponentsFilter;
