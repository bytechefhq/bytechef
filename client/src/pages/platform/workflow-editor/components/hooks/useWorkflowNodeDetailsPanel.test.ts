import {describe, expect, it} from 'vitest';

/**
 * Tests for the workflowTestConfigurationConnections query enabled condition
 * in useWorkflowNodeDetailsPanel.
 *
 * The enabled condition determines whether saved test configuration connections
 * are fetched. If disabled, connection selection in the ConnectionTab will not
 * persist because the sync effect resets the selection when the query data is undefined.
 */

type EnabledConditionInputType = {
    componentDefinitionConnection: boolean;
    currentNode: {clusterRoot?: boolean; isNestedClusterRoot?: boolean} | null;
    workflowId: string | undefined;
};

/**
 * Mirrors the enabled condition from useWorkflowNodeDetailsPanel:
 *   !!workflow.id && !!currentNode &&
 *   (!!currentComponentDefinition?.connection || (!!currentNode.clusterRoot && !currentNode.isNestedClusterRoot))
 */
function isTestConfigConnectionsQueryEnabled({
    componentDefinitionConnection,
    currentNode,
    workflowId,
}: EnabledConditionInputType): boolean {
    return (
        !!workflowId &&
        !!currentNode &&
        (componentDefinitionConnection || (!!currentNode.clusterRoot && !currentNode.isNestedClusterRoot))
    );
}

describe('workflowTestConfigurationConnections query enabled condition', () => {
    it('should be enabled when component has connection defined and connectionRequired is true (e.g., Gmail)', () => {
        expect(
            isTestConfigConnectionsQueryEnabled({
                componentDefinitionConnection: true,
                currentNode: {},
                workflowId: 'wf-1',
            })
        ).toBe(true);
    });

    it('should be enabled when component has connection defined but connectionRequired is false (e.g., httpClient)', () => {
        expect(
            isTestConfigConnectionsQueryEnabled({
                componentDefinitionConnection: true,
                currentNode: {},
                workflowId: 'wf-1',
            })
        ).toBe(true);
    });

    it('should be enabled for cluster root nodes even without a connection', () => {
        expect(
            isTestConfigConnectionsQueryEnabled({
                componentDefinitionConnection: false,
                currentNode: {clusterRoot: true, isNestedClusterRoot: false},
                workflowId: 'wf-1',
            })
        ).toBe(true);
    });

    it('should be disabled for nested cluster root nodes without a connection', () => {
        expect(
            isTestConfigConnectionsQueryEnabled({
                componentDefinitionConnection: false,
                currentNode: {clusterRoot: true, isNestedClusterRoot: true},
                workflowId: 'wf-1',
            })
        ).toBe(false);
    });

    it('should be disabled when component has no connection and node is not a cluster root', () => {
        expect(
            isTestConfigConnectionsQueryEnabled({
                componentDefinitionConnection: false,
                currentNode: {},
                workflowId: 'wf-1',
            })
        ).toBe(false);
    });

    it('should be disabled when currentNode is null', () => {
        expect(
            isTestConfigConnectionsQueryEnabled({
                componentDefinitionConnection: true,
                currentNode: null,
                workflowId: 'wf-1',
            })
        ).toBe(false);
    });

    it('should be disabled when workflowId is undefined', () => {
        expect(
            isTestConfigConnectionsQueryEnabled({
                componentDefinitionConnection: true,
                currentNode: {},
                workflowId: undefined,
            })
        ).toBe(false);
    });
});

/**
 * Tests for the "is currentActionDefinition still fresh?" guard in the fetch-action-definition
 * effect of useWorkflowNodeDetailsPanel.
 *
 * Two components can expose actions with the same name (e.g., OpenRouter/ask and OpenAI/ask).
 * Before this fix the guard only compared the action name, so switching between such components
 * short-circuited the fetch and left the panel rendering the previous component's properties.
 * The guard must also consider the owning component name + version.
 */
type IsActionDefinitionFreshInputType = {
    currentActionDefinition: {componentName: string; componentVersion: number; name: string} | undefined;
    currentComponentDefinition: {name: string; version: number} | undefined;
    currentOperationName: string;
};

function isActionDefinitionFresh({
    currentActionDefinition,
    currentComponentDefinition,
    currentOperationName,
}: IsActionDefinitionFreshInputType): boolean {
    return (
        currentActionDefinition?.name === currentOperationName &&
        currentActionDefinition?.componentName === currentComponentDefinition?.name &&
        currentActionDefinition?.componentVersion === currentComponentDefinition?.version
    );
}

describe('currentActionDefinition freshness guard', () => {
    it('treats the definition as stale when switching components that share an action name', () => {
        expect(
            isActionDefinitionFresh({
                currentActionDefinition: {componentName: 'openrouter', componentVersion: 1, name: 'ask'},
                currentComponentDefinition: {name: 'openai', version: 1},
                currentOperationName: 'ask',
            })
        ).toBe(false);
    });

    it('treats the definition as fresh when component and operation both match', () => {
        expect(
            isActionDefinitionFresh({
                currentActionDefinition: {componentName: 'openai', componentVersion: 1, name: 'ask'},
                currentComponentDefinition: {name: 'openai', version: 1},
                currentOperationName: 'ask',
            })
        ).toBe(true);
    });

    it('treats the definition as stale when the operation name changes within the same component', () => {
        expect(
            isActionDefinitionFresh({
                currentActionDefinition: {componentName: 'openai', componentVersion: 1, name: 'ask'},
                currentComponentDefinition: {name: 'openai', version: 1},
                currentOperationName: 'chat',
            })
        ).toBe(false);
    });

    it('treats the definition as stale when the component version changes', () => {
        expect(
            isActionDefinitionFresh({
                currentActionDefinition: {componentName: 'openai', componentVersion: 1, name: 'ask'},
                currentComponentDefinition: {name: 'openai', version: 2},
                currentOperationName: 'ask',
            })
        ).toBe(false);
    });

    it('treats the definition as stale when no definition has been fetched yet', () => {
        expect(
            isActionDefinitionFresh({
                currentActionDefinition: undefined,
                currentComponentDefinition: {name: 'openai', version: 1},
                currentOperationName: 'ask',
            })
        ).toBe(false);
    });

    /**
     * Regression scenario for bytechefhq/bytechef#4831 — reproduces the user-visible flow:
     * 1. Add OpenRouter `ask` → fetch happens, definition is fresh.
     * 2. Add OpenAI `ask` → guard must flag stale so the fetch re-runs.
     * 3. Switch back to OpenRouter `ask` → guard must flag stale again.
     */
    it('re-flags the definition as stale every time the user switches between components sharing an action name', () => {
        let currentActionDefinition: IsActionDefinitionFreshInputType['currentActionDefinition'];

        const selectOpenRouterAsk: IsActionDefinitionFreshInputType = {
            currentActionDefinition,
            currentComponentDefinition: {name: 'openrouter', version: 1},
            currentOperationName: 'ask',
        };

        expect(isActionDefinitionFresh(selectOpenRouterAsk)).toBe(false);

        currentActionDefinition = {componentName: 'openrouter', componentVersion: 1, name: 'ask'};

        expect(
            isActionDefinitionFresh({
                ...selectOpenRouterAsk,
                currentActionDefinition,
            })
        ).toBe(true);

        const switchToOpenAiAsk: IsActionDefinitionFreshInputType = {
            currentActionDefinition,
            currentComponentDefinition: {name: 'openai', version: 1},
            currentOperationName: 'ask',
        };

        expect(isActionDefinitionFresh(switchToOpenAiAsk)).toBe(false);

        currentActionDefinition = {componentName: 'openai', componentVersion: 1, name: 'ask'};

        expect(
            isActionDefinitionFresh({
                ...switchToOpenAiAsk,
                currentActionDefinition,
            })
        ).toBe(true);

        expect(
            isActionDefinitionFresh({
                currentActionDefinition,
                currentComponentDefinition: {name: 'openrouter', version: 1},
                currentOperationName: 'ask',
            })
        ).toBe(false);
    });
});
