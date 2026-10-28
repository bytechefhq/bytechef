import {renderHook} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import useWorkflowComponentDefinitions, {getWorkflowComponentNames} from '../useWorkflowComponentDefinitions';

const hoisted = vi.hoisted(() => ({
    componentDefinitionsQuery: vi.fn(),
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: hoisted.componentDefinitionsQuery,
}));

vi.mock('@/shared/queries/platform/taskDispatcherDefinitions.queries', () => ({
    useGetTaskDispatcherDefinitionsQuery: () => ({
        data: [{icon: 'loop.svg', name: 'loop', title: 'Loop'}, {name: 'nameless'}],
    }),
}));

describe('getWorkflowComponentNames', () => {
    it('lists the trigger components first, then the task components, each once', () => {
        expect(
            getWorkflowComponentNames({
                workflowTaskComponentNames: ['slack', 'webhook', 'slack', 'logger'],
                workflowTriggerComponentNames: ['webhook'],
            })
        ).toEqual(['webhook', 'slack', 'logger']);
    });

    it('returns nothing for a missing workflow', () => {
        expect(getWorkflowComponentNames(undefined)).toEqual([]);
    });
});

describe('useWorkflowComponentDefinitions', () => {
    it('keys the component and task dispatcher definitions by name using the automation query by default', () => {
        hoisted.componentDefinitionsQuery.mockReturnValue({
            data: [{icon: 'slack.svg', name: 'slack', title: 'Slack'}],
        });

        const {result} = renderHook(() => useWorkflowComponentDefinitions());

        expect(hoisted.componentDefinitionsQuery).toHaveBeenCalledWith({
            actionDefinitions: true,
            triggerDefinitions: true,
        });
        expect(result.current.workflowComponentDefinitions).toEqual({
            slack: {icon: 'slack.svg', name: 'slack', title: 'Slack'},
        });
        expect(result.current.workflowTaskDispatcherDefinitions).toEqual({
            loop: {icon: 'loop.svg', name: 'loop', title: 'Loop'},
            nameless: {icon: undefined, name: 'nameless', title: undefined},
        });
    });

    it('reads the component definitions through the query hook it is given', () => {
        const embeddedQuery = vi.fn().mockReturnValue({data: [{name: 'gmail', title: 'Gmail'}]});

        const {result} = renderHook(() => useWorkflowComponentDefinitions(embeddedQuery));

        expect(embeddedQuery).toHaveBeenCalledWith({actionDefinitions: true, triggerDefinitions: true});
        expect(result.current.workflowComponentDefinitions).toEqual({
            gmail: {icon: undefined, name: 'gmail', title: 'Gmail'},
        });
    });
});
