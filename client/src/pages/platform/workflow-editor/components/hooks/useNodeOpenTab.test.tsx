import {renderHook} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import useNodeOpenTab from './useNodeOpenTab';

interface HookPropsI {
    componentDefinition?: {connectionRequired?: boolean; name?: string};
    connectionsFetched: boolean;
    nodeComponentName?: string;
    testConfigurationConnectionsCount: number;
    workflowNodeName?: string;
}

const SLACK_DEFINITION = {connectionRequired: true, name: 'slack'};

const INITIAL_PROPS: HookPropsI = {
    componentDefinition: SLACK_DEFINITION,
    connectionsFetched: true,
    nodeComponentName: 'slack',
    testConfigurationConnectionsCount: 0,
    workflowNodeName: 'slack_1',
};

const renderNodeOpenTab = (initialProps: HookPropsI) => {
    const onConnectionTabRequired = vi.fn();

    const hook = renderHook((props: HookPropsI) => useNodeOpenTab({...props, onConnectionTabRequired}), {
        initialProps,
    });

    return {...hook, onConnectionTabRequired};
};

describe('useNodeOpenTab', () => {
    it('opens the connection tab when a required connection is missing', () => {
        const {onConnectionTabRequired} = renderNodeOpenTab(INITIAL_PROPS);

        expect(onConnectionTabRequired).toHaveBeenCalledTimes(1);
    });

    it('keeps the current tab when the node has a connection', () => {
        const {onConnectionTabRequired} = renderNodeOpenTab({...INITIAL_PROPS, testConfigurationConnectionsCount: 1});

        expect(onConnectionTabRequired).not.toHaveBeenCalled();
    });

    it('waits for the connections to be fetched before deciding', () => {
        const {onConnectionTabRequired, rerender} = renderNodeOpenTab({...INITIAL_PROPS, connectionsFetched: false});

        expect(onConnectionTabRequired).not.toHaveBeenCalled();

        rerender(INITIAL_PROPS);

        expect(onConnectionTabRequired).toHaveBeenCalledTimes(1);
    });

    it('waits for the definition of the current node component', () => {
        const {onConnectionTabRequired, rerender} = renderNodeOpenTab({
            ...INITIAL_PROPS,
            componentDefinition: {connectionRequired: false, name: 'gmail'},
        });

        expect(onConnectionTabRequired).not.toHaveBeenCalled();

        rerender(INITIAL_PROPS);

        expect(onConnectionTabRequired).toHaveBeenCalledTimes(1);
    });

    it('decides only once per node', () => {
        const {onConnectionTabRequired, rerender} = renderNodeOpenTab(INITIAL_PROPS);

        rerender({...INITIAL_PROPS, testConfigurationConnectionsCount: 1});
        rerender(INITIAL_PROPS);

        expect(onConnectionTabRequired).toHaveBeenCalledTimes(1);
    });

    it('decides again when another node is opened', () => {
        const {onConnectionTabRequired, rerender} = renderNodeOpenTab(INITIAL_PROPS);

        rerender({...INITIAL_PROPS, workflowNodeName: 'slack_2'});

        expect(onConnectionTabRequired).toHaveBeenCalledTimes(2);
    });

    it('decides again for the same node after the panel had no node', () => {
        const {onConnectionTabRequired, rerender} = renderNodeOpenTab(INITIAL_PROPS);

        rerender({...INITIAL_PROPS, workflowNodeName: undefined});
        rerender(INITIAL_PROPS);

        expect(onConnectionTabRequired).toHaveBeenCalledTimes(2);
    });

    it('does nothing without a node', () => {
        const {onConnectionTabRequired} = renderNodeOpenTab({...INITIAL_PROPS, workflowNodeName: undefined});

        expect(onConnectionTabRequired).not.toHaveBeenCalled();
    });
});
