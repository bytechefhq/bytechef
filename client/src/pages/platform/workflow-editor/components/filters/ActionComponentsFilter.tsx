import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {ReactNode} from 'react';

import ComponentsFilter from './ComponentsFilter';

interface ActionComponentsFilterProps {
    actionComponentDefinitions: ComponentDefinitionBasic[];
    deselectAllCategories: () => void;
    filterState: {
        activeView: 'all' | 'filtered';
        selectedCategories: string[];
        searchValue: string;
        filteredCount: number;
    };
    filteredCategories: {label: string; icon?: ReactNode}[];
    filteredComponents: ComponentDefinitionBasic[];
    setActiveView: (view: 'all' | 'filtered') => void;
    setSearchValue: (value: string) => void;
    toggleCategory: (category: string) => void;
}

const ActionComponentsFilter = ({
    actionComponentDefinitions,
    deselectAllCategories,
    filterState,
    filteredCategories,
    filteredComponents,
    setActiveView,
    setSearchValue,
    toggleCategory,
}: ActionComponentsFilterProps) => {
    return (
        <ComponentsFilter
            componentDefinitions={actionComponentDefinitions}
            deselectAllCategories={deselectAllCategories}
            filterLabel="actions"
            filterState={filterState}
            filterTooltip="Filter actions by category"
            filteredCategories={filteredCategories}
            filteredComponents={filteredComponents}
            setActiveView={setActiveView}
            setSearchValue={setSearchValue}
            toggleCategory={toggleCategory}
        />
    );
};

export default ActionComponentsFilter;
