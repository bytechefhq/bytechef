import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpToolOptionsLoader from '../useMcpToolOptionsLoader';

const hoisted = vi.hoisted(() => ({
    fetchResult: vi.fn(),
    fetcher: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    ClusterElementOptionsDocument: 'clusterElementOptions',
}));

vi.mock('@/shared/middleware/graphqlFetcher', () => ({
    fetcher: (...args: unknown[]) => {
        hoisted.fetcher(...args);

        return hoisted.fetchResult;
    },
}));

const property = {name: 'repo', optionsDataSource: {optionsLookupDependsOn: ['owner']}} as never;

const renderLoader = (connectionId?: string | null, connectionRequired?: boolean) => {
    const queryClient = new QueryClient();

    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    return renderHook(
        () =>
            useMcpToolOptionsLoader({
                componentName: 'github',
                componentVersion: 1,
                connectionId,
                connectionRequired,
                toolName: 'listRepos',
            }),
        {wrapper}
    ).result.current;
};

describe('useMcpToolOptionsLoader', () => {
    beforeEach(() => {
        hoisted.fetchResult.mockReset();
        hoisted.fetcher.mockReset();
    });

    it('resolves options from the cluster element options query', async () => {
        hoisted.fetchResult.mockResolvedValue({clusterElementOptions: [{label: 'A', value: 'a'}]});

        const loadOptions = renderLoader('7', true);

        await expect(loadOptions(property, 'repo', {owner: 'me'})).resolves.toEqual([{label: 'A', value: 'a'}]);

        expect(hoisted.fetcher).toHaveBeenCalledWith('clusterElementOptions', {
            clusterElementName: 'listRepos',
            componentName: 'github',
            componentVersion: 1,
            connectionId: '7',
            inputParameters: {owner: 'me'},
            lookupDependsOnPaths: ['owner'],
            propertyName: 'repo',
        });
    });

    it('resolves null without a connection when the property needs one', async () => {
        const loadOptions = renderLoader(null, true);

        await expect(loadOptions(property, 'repo', {})).resolves.toBeNull();

        expect(hoisted.fetchResult).not.toHaveBeenCalled();
    });

    it.each([
        ['missing', {}],
        ['null', {owner: null}],
        ['empty', {owner: ''}],
        ['an expression', {owner: "=fromAi('owner', 'STRING')"}],
    ])('resolves null without fetching when a lookup dependency is %s', async (_case, values) => {
        hoisted.fetchResult.mockResolvedValue({clusterElementOptions: [{label: 'A', value: 'a'}]});

        const loadOptions = renderLoader('7', true);

        await expect(loadOptions(property, 'repo', values)).resolves.toBeNull();

        expect(hoisted.fetcher).not.toHaveBeenCalled();
    });

    it('fetches when the property has no lookup dependencies', async () => {
        hoisted.fetchResult.mockResolvedValue({clusterElementOptions: [{label: 'A', value: 'a'}]});

        const loadOptions = renderLoader('7', true);

        await expect(
            loadOptions({name: 'channel', optionsDataSource: {optionsLookupDependsOn: []}} as never, 'channel', {})
        ).resolves.toEqual([{label: 'A', value: 'a'}]);
    });

    it('reuses a fetched options list within the stale time', async () => {
        hoisted.fetchResult.mockResolvedValue({clusterElementOptions: [{label: 'A', value: 'a'}]});

        const loadOptions = renderLoader('7', true);

        await loadOptions(property, 'repo', {owner: 'me'});
        await loadOptions(property, 'repo', {owner: 'me'});

        expect(hoisted.fetchResult).toHaveBeenCalledTimes(1);
    });

    it('resolves null on error', async () => {
        hoisted.fetchResult.mockRejectedValue(new Error('boom'));

        const loadOptions = renderLoader('7', true);

        await expect(loadOptions(property, 'repo', {owner: 'me'})).resolves.toBeNull();
    });
});
