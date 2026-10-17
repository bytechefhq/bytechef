import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {type ReactNode, createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useComponentCatalog} from './useComponentCatalog';

const {
    getComponentConnectionDefinitionMock,
    getComponentDefinitionMock,
    getComponentDefinitionsMock,
    getEmbeddedComponentDefinitionsMock,
} = vi.hoisted(() => ({
    getComponentConnectionDefinitionMock: vi.fn(),
    getComponentDefinitionMock: vi.fn(),
    getComponentDefinitionsMock: vi.fn(),
    getEmbeddedComponentDefinitionsMock: vi.fn(),
}));

vi.mock('@/ee/shared/middleware/embedded/configuration', () => ({
    ComponentDefinitionApi: class {
        getComponentDefinitions = getEmbeddedComponentDefinitionsMock;
    },
}));

vi.mock('@/shared/middleware/automation/configuration', () => ({
    ComponentDefinitionApi: class {
        getComponentDefinitions = getComponentDefinitionsMock;
    },
}));

vi.mock('@/shared/middleware/platform/configuration', () => ({
    ComponentDefinitionApi: class {
        getComponentDefinition = getComponentDefinitionMock;
    },
    ConnectionDefinitionApi: class {
        getComponentConnectionDefinition = getComponentConnectionDefinitionMock;
    },
}));

const componentList = [{name: 'slack', version: 1}];

let queryClient: QueryClient;

function wrapper({children}: {children: ReactNode}) {
    return createElement(QueryClientProvider, {client: queryClient}, children);
}

describe('useComponentCatalog', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        getComponentDefinitionsMock.mockResolvedValue(componentList);
        getEmbeddedComponentDefinitionsMock.mockResolvedValue([{name: 'embedded-one', version: 2}]);
        getComponentDefinitionMock.mockResolvedValue({name: 'slack'});
        getComponentConnectionDefinitionMock.mockResolvedValue({name: 'slack'});
    });

    it('exposes the component list and fetches a definition once per component', async () => {
        const {result} = renderHook(() => useComponentCatalog(), {wrapper});

        await waitFor(() => expect(result.current.components).toHaveLength(1));

        await result.current.getDefinition('slack');
        await result.current.getDefinition('slack');

        expect(getComponentDefinitionMock).toHaveBeenCalledTimes(1);
        expect(getComponentDefinitionMock).toHaveBeenCalledWith({componentName: 'slack', componentVersion: 1});
    });

    it('fetches a connection definition once per component', async () => {
        const {result} = renderHook(() => useComponentCatalog(), {wrapper});

        await waitFor(() => expect(result.current.components).toHaveLength(1));

        await result.current.getConnectionDefinition('slack');
        await result.current.getConnectionDefinition('slack');

        expect(getComponentConnectionDefinitionMock).toHaveBeenCalledTimes(1);
        expect(getComponentConnectionDefinitionMock).toHaveBeenCalledWith({
            componentName: 'slack',
            componentVersion: 1,
        });
    });

    it('resolves undefined without fetching for an unknown component without a version', async () => {
        const {result} = renderHook(() => useComponentCatalog(), {wrapper});

        expect(await result.current.getDefinition('unknown')).toBeUndefined();
        expect(await result.current.getConnectionDefinition('unknown')).toBeUndefined();
        expect(getComponentDefinitionMock).not.toHaveBeenCalled();
        expect(getComponentConnectionDefinitionMock).not.toHaveBeenCalled();
    });

    it('uses an explicit version for an unlisted component', async () => {
        const {result} = renderHook(() => useComponentCatalog(), {wrapper});

        await result.current.getDefinition('unknown', 3);

        expect(getComponentDefinitionMock).toHaveBeenCalledWith({componentName: 'unknown', componentVersion: 3});
    });

    it('keeps the platform and embedded lists apart in one query client', async () => {
        const platform = renderHook(() => useComponentCatalog(), {wrapper});
        const embedded = renderHook(() => useComponentCatalog({embedded: true}), {wrapper});

        await waitFor(() => expect(platform.result.current.components).toEqual(componentList));
        await waitFor(() => expect(embedded.result.current.components).toEqual([{name: 'embedded-one', version: 2}]));

        expect(getComponentDefinitionsMock).toHaveBeenCalledTimes(1);
        expect(getEmbeddedComponentDefinitionsMock).toHaveBeenCalledTimes(1);
        expect(getComponentDefinitionsMock).toHaveBeenCalledWith({actionDefinitions: true, triggerDefinitions: true});
    });

    it('keeps the catalog identity stable across re-renders', () => {
        const {rerender, result} = renderHook(() => useComponentCatalog(), {wrapper});

        const first = result.current;

        rerender();

        expect(result.current).toBe(first);
    });

    it('reads the embedded list when embedded is set', async () => {
        const {result} = renderHook(() => useComponentCatalog({embedded: true}), {wrapper});

        await waitFor(() => expect(result.current.components).toEqual([{name: 'embedded-one', version: 2}]));
    });
});
