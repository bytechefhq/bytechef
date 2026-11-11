import {ComponentDefinitionBasic} from '@/shared/middleware/automation/configuration';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpComponentDialogComponentSelectionStep from '../useMcpComponentDialogComponentSelectionStep';

const hoisted = vi.hoisted(() => ({
    components: [] as unknown[],
    useMcpComponentsByServerIdQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useMcpComponentsByServerIdQuery: hoisted.useMcpComponentsByServerIdQuery,
}));

const mockServerComponents = (mcpComponents: unknown[], isLoading = false) =>
    hoisted.useMcpComponentsByServerIdQuery.mockReturnValue({
        data: {mcpComponentsByServerId: mcpComponents},
        isLoading,
    });

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

const renderSelectionStep = () => renderHook(() => useMcpComponentDialogComponentSelectionStep(true, '1070'));

const selectableNames = () => {
    const {result} = renderSelectionStep();

    return result.current.filteredComponents.map((component) => component.name);
};

describe('useMcpComponentDialogComponentSelectionStep', () => {
    beforeEach(() => {
        hoisted.useMcpComponentsByServerIdQuery.mockReset();

        mockServerComponents([]);
    });

    it('loads the components of the given server only while the dialog is open', () => {
        renderSelectionStep();

        expect(hoisted.useMcpComponentsByServerIdQuery).toHaveBeenLastCalledWith(
            {mcpServerId: '1070'},
            {enabled: true}
        );

        renderHook(() => useMcpComponentDialogComponentSelectionStep(false, '1070'));

        expect(hoisted.useMcpComponentsByServerIdQuery).toHaveBeenLastCalledWith(
            {mcpServerId: '1070'},
            {enabled: false}
        );

        renderHook(() => useMcpComponentDialogComponentSelectionStep(true, ''));

        expect(hoisted.useMcpComponentsByServerIdQuery).toHaveBeenLastCalledWith({mcpServerId: ''}, {enabled: false});
    });

    it('stays loading until the server components are loaded', () => {
        mockServerComponents([], true);

        const {result} = renderSelectionStep();

        expect(result.current.isLoadingComponents).toBe(true);
    });

    it('marks a component version that is already on the server as added', () => {
        const gmailVersion1 = componentDefinition({name: 'googleMail', version: 1});
        const gmailVersion2 = componentDefinition({name: 'googleMail', version: 2});
        const slack = componentDefinition({name: 'slack', version: 1});

        hoisted.components = [gmailVersion1, gmailVersion2, slack];
        mockServerComponents([{componentName: 'googleMail', componentVersion: 1, id: '5'}]);

        const {result} = renderSelectionStep();

        expect(result.current.isComponentAdded(gmailVersion1)).toBe(true);
        expect(result.current.isComponentAdded(gmailVersion2)).toBe(false);
        expect(result.current.isComponentAdded(slack)).toBe(false);
    });

    it('offers an ordinary component whose actions are exposed as tools', () => {
        hoisted.components = [componentDefinition({clusterElementsCount: {TOOLS: 3}, name: 'activeCampaign'})];

        expect(selectableNames()).toEqual(['activeCampaign']);
    });

    // The AI Agent is a canvas other cluster elements plug into; it has nothing of its own to expose.
    it('excludes a cluster root', () => {
        hoisted.components = [componentDefinition({clusterRoot: true, name: 'aiAgent'})];

        expect(selectableNames()).toEqual([]);
    });

    // Airtable also contributes data stream SOURCE and DESTINATION elements; its tools are still callable.
    it('offers a component that contributes cluster elements of other types alongside its tools', () => {
        hoisted.components = [
            componentDefinition({
                clusterElementsCount: {DESTINATION: 1, SOURCE: 1, TOOLS: 4},
                name: 'airtable',
            }),
        ];

        expect(selectableNames()).toEqual(['airtable']);
    });

    it('excludes a component that exposes no tools at all', () => {
        hoisted.components = [componentDefinition({clusterElementsCount: {}, name: 'noTools'})];

        expect(selectableNames()).toEqual([]);
    });
});
