import {COMPONENT_CATEGORY_ICON} from '@/shared/constants';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useCallback, useMemo, useState} from 'react';

type ActiveViewType = 'all' | 'filtered';

export const useComponentFiltering = ({
    actionComponentDefinitions,
}: {
    actionComponentDefinitions: ComponentDefinitionBasic[];
}) => {
    const [filterState, setFilterState] = useState<{
        activeView: ActiveViewType;
        filteredCount: number;
        savedCategories: string[];
        searchValue: string;
        selectedCategories: string[];
    }>({
        activeView: 'all',
        filteredCount: 0,
        savedCategories: [],
        searchValue: '',
        selectedCategories: [],
    });

    const ff_3158 = useFeatureFlagsStore()('ff-3158');

    const setSearchValue = useCallback((value: string) => {
        setFilterState((state) => ({...state, searchValue: value}));
    }, []);

    const toggleCategory = useCallback((category: string) => {
        setFilterState((state) => {
            const isAlreadySelected = state.selectedCategories.includes(category);

            const newCategories = isAlreadySelected
                ? state.selectedCategories.filter((selectedCategory) => selectedCategory !== category)
                : [...state.selectedCategories, category];

            return {
                ...state,
                activeView: 'filtered',
                selectedCategories: newCategories,
            };
        });
    }, []);

    const deselectAllCategories = useCallback(() => {
        setFilterState((state) => ({
            ...state,
            activeView: 'all',
            selectedCategories: [],
        }));
    }, []);

    const getCategoryLabels = useCallback((component: ComponentDefinitionBasic) => {
        return component.componentCategories?.map((category) => category.label?.toLowerCase() || '') || [];
    }, []);

    const calculateFilteredComponentsCount = useCallback(
        (components: ComponentDefinitionBasic[], categories: string[]) => {
            if (!components || categories.length === 0) {
                return 0;
            }

            const filteredComponents = components.filter((component) => {
                const categoryLabels = getCategoryLabels(component);

                return categories.some((category) => categoryLabels.includes(category.toLowerCase()));
            });

            return filteredComponents.length;
        },
        [getCategoryLabels]
    );

    const setActiveView = useCallback(
        (view: ActiveViewType) => {
            setFilterState((state) => {
                const count = calculateFilteredComponentsCount(actionComponentDefinitions, state.selectedCategories);

                if (view === 'all' && state.selectedCategories.length > 0) {
                    return {
                        ...state,
                        activeView: view,
                        filteredCount: count,
                        savedCategories: [...state.selectedCategories],
                    };
                }

                if (view === 'filtered' && state.savedCategories.length > 0) {
                    return {
                        ...state,
                        activeView: view,
                        selectedCategories: [...state.savedCategories],
                    };
                }

                return {...state, activeView: view};
            });
        },
        [actionComponentDefinitions, calculateFilteredComponentsCount]
    );

    const uniqueCategories = useMemo(() => {
        if (!actionComponentDefinitions?.length) {
            return [];
        }

        const categories: {icon: JSX.Element | null; label: string}[] = [];

        actionComponentDefinitions.forEach((component) => {
            component.componentCategories?.forEach((componentCategory) => {
                const label = componentCategory.label;

                if (label && !categories.some((category) => category.label === label)) {
                    categories.push({
                        icon: COMPONENT_CATEGORY_ICON?.[label.toLowerCase()] || null,
                        label,
                    });
                }
            });
        });

        return categories;
    }, [actionComponentDefinitions]);

    const filteredCategories = useMemo(() => {
        const categoriesFromFilteredComponents = uniqueCategories.filter((category) =>
            category.label.toLowerCase().includes(filterState.searchValue.toLowerCase())
        );

        const filteredPreviouslySelectedCategoryLabels = filterState.selectedCategories.filter(
            (selectedCategoryLabel) =>
                selectedCategoryLabel.toLowerCase().includes(filterState.searchValue.toLowerCase()) &&
                !categoriesFromFilteredComponents.some((category) => category.label === selectedCategoryLabel)
        );

        const formattedPreviouslySelectedCategories = filteredPreviouslySelectedCategoryLabels.map((selectedLabel) => ({
            icon: COMPONENT_CATEGORY_ICON?.[selectedLabel.toLowerCase()] || null,
            label: selectedLabel,
        }));

        return [...categoriesFromFilteredComponents, ...formattedPreviouslySelectedCategories];
    }, [filterState.searchValue, filterState.selectedCategories, uniqueCategories]);

    const filteredComponents = useMemo(() => {
        if (!actionComponentDefinitions) {
            return [];
        }

        if (filterState.activeView === 'all' || filterState.selectedCategories.length === 0) {
            return actionComponentDefinitions.filter((component) => {
                return !(!ff_3158 && component.name === 'claudeCode');
            });
        }

        return actionComponentDefinitions.filter((component) => {
            const categoryLabels = getCategoryLabels(component);

            if (!ff_3158 && component.name === 'claudeCode') {
                return false;
            }

            return filterState.selectedCategories.some((category: string) =>
                categoryLabels.includes((category || '').toLowerCase())
            );
        });
    }, [
        actionComponentDefinitions,
        ff_3158,
        filterState.activeView,
        filterState.selectedCategories,
        getCategoryLabels,
    ]);

    return {
        deselectAllCategories,
        filterState,
        filteredCategories,
        filteredComponents,
        setActiveView,
        setSearchValue,
        toggleCategory,
        uniqueCategories,
    };
};
