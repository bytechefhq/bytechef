import {COMPONENT_CATEGORY_ICON} from '@/shared/constants';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useComponentFiltering} from '../useComponentFiltering';

const hoisted = vi.hoisted(() => ({enabledFeatureFlags: [] as string[]}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => (featureFlag: string) => hoisted.enabledFeatureFlags.includes(featureFlag),
}));

function makeComponentDefinition(name: string, categoryLabels: Array<string | undefined>): ComponentDefinitionBasic {
    return {
        componentCategories: categoryLabels.map((label) => ({label, name: label || 'unnamed'})),
        name,
        version: 1,
    };
}

const componentDefinitions: ComponentDefinitionBasic[] = [
    makeComponentDefinition('hubspot', ['CRM', 'Marketing Automation']),
    makeComponentDefinition('salesforce', ['CRM']),
    makeComponentDefinition('slack', ['Communication']),
    makeComponentDefinition('openAi', ['Artificial Intelligence']),
    makeComponentDefinition('claudeCode', ['Developer Tools']),
    {name: 'uncategorized', version: 1},
];

function renderComponentFiltering(definitions: ComponentDefinitionBasic[] = componentDefinitions) {
    return renderHook(() => useComponentFiltering({componentDefinitions: definitions}));
}

function getComponentNames(components: ComponentDefinitionBasic[]) {
    return components.map((component) => component.name);
}

describe('useComponentFiltering', () => {
    beforeEach(() => {
        hoisted.enabledFeatureFlags = [];
    });

    it('starts in the all view with no selection', () => {
        const {result} = renderComponentFiltering();

        expect(result.current.filterState).toEqual({
            activeView: 'all',
            filteredCount: 0,
            savedCategories: [],
            searchValue: '',
            selectedCategories: [],
        });
    });

    describe('uniqueCategories', () => {
        it('collects each category label once, in first-seen order, with its icon', () => {
            const {result} = renderComponentFiltering();

            expect(result.current.uniqueCategories).toEqual([
                {icon: COMPONENT_CATEGORY_ICON.crm, label: 'CRM'},
                {icon: COMPONENT_CATEGORY_ICON['marketing automation'] || null, label: 'Marketing Automation'},
                {icon: COMPONENT_CATEGORY_ICON.communication, label: 'Communication'},
                {icon: COMPONENT_CATEGORY_ICON['artificial intelligence'], label: 'Artificial Intelligence'},
                {icon: COMPONENT_CATEGORY_ICON['developer tools'] || null, label: 'Developer Tools'},
            ]);
        });

        it('uses a null icon for unknown categories and skips categories without a label', () => {
            const {result} = renderComponentFiltering([
                makeComponentDefinition('custom', ['Something Unknown', undefined]),
            ]);

            expect(result.current.uniqueCategories).toEqual([{icon: null, label: 'Something Unknown'}]);
        });

        it('returns no categories when there are no component definitions', () => {
            const {result} = renderComponentFiltering([]);

            expect(result.current.uniqueCategories).toEqual([]);
            expect(result.current.filteredCategories).toEqual([]);
            expect(result.current.filteredComponents).toEqual([]);
        });
    });

    describe('filteredComponents', () => {
        it('returns every component except claudeCode in the all view when ff-3158 is off', () => {
            const {result} = renderComponentFiltering();

            expect(getComponentNames(result.current.filteredComponents)).toEqual([
                'hubspot',
                'salesforce',
                'slack',
                'openAi',
                'uncategorized',
            ]);
        });

        it('includes claudeCode in the all view when ff-3158 is on', () => {
            hoisted.enabledFeatureFlags = ['ff-3158'];

            const {result} = renderComponentFiltering();

            expect(getComponentNames(result.current.filteredComponents)).toContain('claudeCode');
            expect(result.current.filteredComponents).toHaveLength(componentDefinitions.length);
        });

        it('filters by the selected categories case-insensitively', () => {
            const {result} = renderComponentFiltering();

            act(() => {
                result.current.toggleCategory('crm');
            });

            expect(result.current.filterState.activeView).toBe('filtered');
            expect(getComponentNames(result.current.filteredComponents)).toEqual(['hubspot', 'salesforce']);

            act(() => {
                result.current.toggleCategory('Communication');
            });

            expect(result.current.filterState.selectedCategories).toEqual(['crm', 'Communication']);
            expect(getComponentNames(result.current.filteredComponents)).toEqual(['hubspot', 'salesforce', 'slack']);
        });

        it('excludes claudeCode from a matching category unless ff-3158 is on', () => {
            const {rerender, result} = renderComponentFiltering();

            act(() => {
                result.current.toggleCategory('Developer Tools');
            });

            expect(result.current.filteredComponents).toEqual([]);

            hoisted.enabledFeatureFlags = ['ff-3158'];

            rerender();

            expect(getComponentNames(result.current.filteredComponents)).toEqual(['claudeCode']);
        });

        it('returns all components in the filtered view once every category is toggled off', () => {
            const {result} = renderComponentFiltering();

            act(() => {
                result.current.toggleCategory('CRM');
            });

            act(() => {
                result.current.toggleCategory('CRM');
            });

            expect(result.current.filterState).toMatchObject({activeView: 'filtered', selectedCategories: []});
            expect(result.current.filteredComponents).toHaveLength(componentDefinitions.length - 1);
        });
    });

    describe('deselectAllCategories', () => {
        it('clears the selection and returns to the all view', () => {
            const {result} = renderComponentFiltering();

            act(() => {
                result.current.toggleCategory('CRM');
                result.current.toggleCategory('Communication');
            });

            act(() => {
                result.current.deselectAllCategories();
            });

            expect(result.current.filterState).toMatchObject({activeView: 'all', selectedCategories: []});
            expect(result.current.filteredComponents).toHaveLength(componentDefinitions.length - 1);
        });
    });

    describe('setActiveView', () => {
        it('saves the selection and counts the matching components when switching to all', () => {
            const {result} = renderComponentFiltering();

            act(() => {
                result.current.toggleCategory('CRM');
                result.current.toggleCategory('Communication');
            });

            act(() => {
                result.current.setActiveView('all');
            });

            expect(result.current.filterState).toEqual({
                activeView: 'all',
                filteredCount: 3,
                savedCategories: ['CRM', 'Communication'],
                searchValue: '',
                selectedCategories: ['CRM', 'Communication'],
            });
            expect(result.current.filteredComponents).toHaveLength(componentDefinitions.length - 1);
        });

        it('does not count claudeCode when ff-3158 is off, matching the filtered view', () => {
            const {result} = renderComponentFiltering([
                makeComponentDefinition('claudeCode', ['AI']),
                makeComponentDefinition('openAi', ['AI']),
            ]);

            act(() => {
                result.current.toggleCategory('AI');
            });

            act(() => {
                result.current.setActiveView('all');
            });

            expect(result.current.filterState.filteredCount).toBe(1);

            act(() => {
                result.current.setActiveView('filtered');
            });

            expect(getComponentNames(result.current.filteredComponents)).toEqual(['openAi']);
            expect(result.current.filteredComponents).toHaveLength(result.current.filterState.filteredCount);
        });

        it('counts claudeCode when ff-3158 is on, matching the filtered view', () => {
            hoisted.enabledFeatureFlags = ['ff-3158'];

            const {result} = renderComponentFiltering([
                makeComponentDefinition('claudeCode', ['AI']),
                makeComponentDefinition('openAi', ['AI']),
            ]);

            act(() => {
                result.current.toggleCategory('AI');
            });

            act(() => {
                result.current.setActiveView('all');
            });

            expect(result.current.filterState.filteredCount).toBe(2);

            act(() => {
                result.current.setActiveView('filtered');
            });

            expect(getComponentNames(result.current.filteredComponents)).toEqual(['claudeCode', 'openAi']);
        });

        it('restores the saved selection when switching back to filtered', () => {
            const {result} = renderComponentFiltering();

            act(() => {
                result.current.toggleCategory('CRM');
            });

            act(() => {
                result.current.setActiveView('all');
            });

            act(() => {
                result.current.deselectAllCategories();
            });

            act(() => {
                result.current.setActiveView('filtered');
            });

            expect(result.current.filterState).toMatchObject({
                activeView: 'filtered',
                savedCategories: ['CRM'],
                selectedCategories: ['CRM'],
            });
            expect(getComponentNames(result.current.filteredComponents)).toEqual(['hubspot', 'salesforce']);
        });

        it('only changes the view when there is nothing to save or restore', () => {
            const {result} = renderComponentFiltering();

            act(() => {
                result.current.setActiveView('filtered');
            });

            expect(result.current.filterState).toEqual({
                activeView: 'filtered',
                filteredCount: 0,
                savedCategories: [],
                searchValue: '',
                selectedCategories: [],
            });

            act(() => {
                result.current.setActiveView('all');
            });

            expect(result.current.filterState).toMatchObject({activeView: 'all', filteredCount: 0});
        });
    });

    describe('filteredCategories', () => {
        it('returns every category when the search is empty', () => {
            const {result} = renderComponentFiltering();

            expect(result.current.filteredCategories).toEqual(result.current.uniqueCategories);
        });

        it('filters categories by a case-insensitive substring of the search value', () => {
            const {result} = renderComponentFiltering();

            act(() => {
                result.current.setSearchValue('COM');
            });

            expect(result.current.filterState.searchValue).toBe('COM');
            expect(result.current.filteredCategories.map((category) => category.label)).toEqual(['Communication']);
        });

        it('appends selected categories that no component provides when they match the search', () => {
            const {result} = renderComponentFiltering();

            act(() => {
                result.current.toggleCategory('Accounting');
                result.current.toggleCategory('Analytics');
                result.current.toggleCategory('CRM');
            });

            expect(result.current.filteredCategories.slice(-2)).toEqual([
                {icon: COMPONENT_CATEGORY_ICON.accounting, label: 'Accounting'},
                {icon: COMPONENT_CATEGORY_ICON.analytics, label: 'Analytics'},
            ]);
            expect(result.current.filteredCategories.filter((category) => category.label === 'CRM')).toHaveLength(1);

            act(() => {
                result.current.setSearchValue('acc');
            });

            expect(result.current.filteredCategories).toEqual([
                {icon: COMPONENT_CATEGORY_ICON.accounting, label: 'Accounting'},
            ]);
        });

        it('returns no categories when nothing matches the search', () => {
            const {result} = renderComponentFiltering();

            act(() => {
                result.current.setSearchValue('does not exist');
            });

            expect(result.current.filteredCategories).toEqual([]);
        });
    });
});
