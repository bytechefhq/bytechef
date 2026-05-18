import {describe, expect, it} from 'vitest';

import isActionDefinitionFresh from './isActionDefinitionFresh';

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
 * Before bytechefhq/bytechef#4831 the guard only compared the action name, so switching between
 * such components short-circuited the fetch and left the panel rendering the previous
 * component's properties. The guard must also consider the owning component name + version.
 *
 * These tests exercise the real isActionDefinitionFresh function used by the hook so that any
 * regression in production logic also fails the tests.
 */
type ActionDefinitionFixtureType = {componentName: string; componentVersion: number; name: string};

describe('currentActionDefinition freshness guard', () => {
    it('treats the definition as stale when switching components that share an action name', () => {
        expect(
            isActionDefinitionFresh(
                {componentName: 'openrouter', componentVersion: 1, name: 'ask'},
                {name: 'openai', version: 1},
                'ask'
            )
        ).toBe(false);
    });

    it('treats the definition as fresh when component and operation both match', () => {
        expect(
            isActionDefinitionFresh(
                {componentName: 'openai', componentVersion: 1, name: 'ask'},
                {name: 'openai', version: 1},
                'ask'
            )
        ).toBe(true);
    });

    it('treats the definition as stale when the operation name changes within the same component', () => {
        expect(
            isActionDefinitionFresh(
                {componentName: 'openai', componentVersion: 1, name: 'ask'},
                {name: 'openai', version: 1},
                'chat'
            )
        ).toBe(false);
    });

    it('treats the definition as stale when the component version changes', () => {
        expect(
            isActionDefinitionFresh(
                {componentName: 'openai', componentVersion: 1, name: 'ask'},
                {name: 'openai', version: 2},
                'ask'
            )
        ).toBe(false);
    });

    it('treats the definition as stale when no definition has been fetched yet', () => {
        expect(isActionDefinitionFresh(undefined, {name: 'openai', version: 1}, 'ask')).toBe(false);
    });

    /**
     * Regression scenario for bytechefhq/bytechef#4831 — reproduces the user-visible flow:
     * 1. Add OpenRouter `ask` → fetch happens, definition is fresh.
     * 2. Add OpenAI `ask` → guard must flag stale so the fetch re-runs.
     * 3. Switch back to OpenRouter `ask` → guard must flag stale again.
     */
    it('re-flags the definition as stale every time the user switches between components sharing an action name', () => {
        let currentActionDefinition: ActionDefinitionFixtureType | undefined;

        expect(isActionDefinitionFresh(currentActionDefinition, {name: 'openrouter', version: 1}, 'ask')).toBe(false);

        currentActionDefinition = {componentName: 'openrouter', componentVersion: 1, name: 'ask'};

        expect(isActionDefinitionFresh(currentActionDefinition, {name: 'openrouter', version: 1}, 'ask')).toBe(true);

        expect(isActionDefinitionFresh(currentActionDefinition, {name: 'openai', version: 1}, 'ask')).toBe(false);

        currentActionDefinition = {componentName: 'openai', componentVersion: 1, name: 'ask'};

        expect(isActionDefinitionFresh(currentActionDefinition, {name: 'openai', version: 1}, 'ask')).toBe(true);

        expect(isActionDefinitionFresh(currentActionDefinition, {name: 'openrouter', version: 1}, 'ask')).toBe(false);
    });
});

/**
 * Tests for the action-only match guard in the fetch-action-definition effect.
 *
 * Some components expose both triggers and actions (e.g., Productboard has a `newNote` trigger
 * and a `getFeature` action). Before this guard, opening a trigger panel left
 * `currentOperationName = 'newNote'`; clicking a sibling action node on the same component then
 * fired `GET /component-definitions/productboard/1/actions/newNote` during the transitional
 * render — a 400 because `newNote` is a trigger, not an action. The guard must match only
 * against `currentComponentDefinition.actions`, not the union of actions/triggers/cluster
 * elements that `matchingOperation` resolves against.
 */
type ComponentDefinitionWithOperationsType = {
    actions?: Array<{name: string}>;
    triggers?: Array<{name: string}>;
};

function shouldFireActionFetch(
    componentDefinition: ComponentDefinitionWithOperationsType | undefined,
    currentOperationName: string,
    currentNodeIsTrigger: boolean
): boolean {
    const matchingAction = componentDefinition?.actions?.find((action) => action.name === currentOperationName);

    return !!componentDefinition?.actions && !currentNodeIsTrigger && !!matchingAction;
}

describe('action-fetch effect: action-only match guard', () => {
    const productboard: ComponentDefinitionWithOperationsType = {
        actions: [{name: 'getFeature'}],
        triggers: [{name: 'newNote'}],
    };

    it('does not fire the action fetch when stale operationName matches a trigger but not an action', () => {
        expect(shouldFireActionFetch(productboard, 'newNote', false)).toBe(false);
    });

    it('fires the action fetch when operationName matches an action on the component', () => {
        expect(shouldFireActionFetch(productboard, 'getFeature', false)).toBe(true);
    });

    it('does not fire when currentNode is a trigger, even if operationName matches an action', () => {
        expect(shouldFireActionFetch(productboard, 'getFeature', true)).toBe(false);
    });

    it('does not fire when component has no actions', () => {
        expect(shouldFireActionFetch({triggers: [{name: 'newNote'}]}, 'newNote', false)).toBe(false);
    });

    it('does not fire when operationName is empty (e.g., switching to a task dispatcher)', () => {
        expect(shouldFireActionFetch(productboard, '', false)).toBe(false);
    });
});
