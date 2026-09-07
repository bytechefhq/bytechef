import {ComponentDefinitionBasic} from '@/shared/middleware/automation/configuration';
import {renderHook} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import useMcpComponentDialogComponentSelectionStep from '../useMcpComponentDialogComponentSelectionStep';

const hoisted = vi.hoisted(() => ({
    components: [] as unknown[],
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: hoisted.components, isLoading: false}),
}));

const componentDefinition = (overrides: Partial<ComponentDefinitionBasic>): ComponentDefinitionBasic =>
    ({
        clusterElementsCount: {TOOLS: 1},
        name: 'component',
        title: 'Component',
        version: 1,
        ...overrides,
    }) as ComponentDefinitionBasic;

const selectableNames = () => {
    const {result} = renderHook(() => useMcpComponentDialogComponentSelectionStep(true));

    return result.current.filteredComponents.map((component) => component.name);
};

describe('useMcpComponentDialogComponentSelectionStep', () => {
    it('offers an ordinary component whose actions are exposed as tools', () => {
        hoisted.components = [componentDefinition({clusterElementsCount: {TOOLS: 3}, name: 'activeCampaign'})];

        expect(selectableNames()).toEqual(['activeCampaign']);
    });

    // The AI Agent is a canvas other cluster elements plug into; it has nothing of its own to expose.
    it('excludes a cluster root', () => {
        hoisted.components = [componentDefinition({clusterRoot: true, name: 'aiAgent'})];

        expect(selectableNames()).toEqual([]);
    });

    // AI Agent Utils contributes CLAUDE_CODE_TOOLS alongside the TOOLS derived from its own actions.
    it('excludes a component contributing cluster elements of a non-tool type', () => {
        hoisted.components = [
            componentDefinition({
                clusterElementsCount: {CLAUDE_CODE_TOOLS: 8, TOOLS: 5},
                name: 'aiAgentUtils',
            }),
        ];

        expect(selectableNames()).toEqual([]);
    });

    it('excludes a component that exposes no tools at all', () => {
        hoisted.components = [componentDefinition({clusterElementsCount: {}, name: 'noTools'})];

        expect(selectableNames()).toEqual([]);
    });
});
