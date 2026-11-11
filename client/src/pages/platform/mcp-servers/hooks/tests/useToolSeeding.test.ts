import {act, renderHook, waitFor} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import useToolSeeding from '../useToolSeeding';

interface SeedingPropsI {
    keys: string[];
    seed: (key: string) => Promise<Record<string, string>>;
    seedKey: string;
}

interface DeferredI {
    promise: Promise<Record<string, string>>;
    resolve: (values: Record<string, string>) => void;
}

const createDeferred = (): DeferredI => {
    let resolve: (values: Record<string, string>) => void = () => undefined;

    const promise = new Promise<Record<string, string>>((resolvePromise) => {
        resolve = resolvePromise;
    });

    return {promise, resolve};
};

const renderSeeding = (initialProps: SeedingPropsI) =>
    renderHook((props: SeedingPropsI) => useToolSeeding(props), {initialProps});

describe('useToolSeeding', () => {
    it('seeds every key and exposes the values', async () => {
        const seed = vi.fn(async (key: string) => ({field: `value-${key}`}));

        const {result} = renderSeeding({keys: ['first', 'second'], seed, seedKey: 'scope'});

        let seededValues: Record<string, Record<string, string>> = {};

        await act(async () => {
            seededValues = await result.current.waitForSeeding();
        });

        expect(seededValues).toEqual({first: {field: 'value-first'}, second: {field: 'value-second'}});

        await waitFor(() => {
            expect(result.current.seededValuesByKey.second).toEqual({field: 'value-second'});
        });
    });

    it('does not seed a key again while the seed key is unchanged', async () => {
        const seed = vi.fn(async () => ({}));

        const {rerender, result} = renderSeeding({keys: ['first'], seed, seedKey: 'scope'});

        rerender({keys: ['first'], seed, seedKey: 'scope'});

        await act(async () => {
            await result.current.waitForSeeding();
        });

        expect(seed).toHaveBeenCalledTimes(1);
    });

    it('reseeds every key when the seed key changes and waits for the new seed', async () => {
        const firstSeed = createDeferred();
        const secondSeed = createDeferred();
        const seed = vi.fn().mockReturnValueOnce(firstSeed.promise).mockReturnValueOnce(secondSeed.promise);

        const {rerender, result} = renderSeeding({keys: ['first'], seed, seedKey: 'one'});

        let seededValues: Record<string, Record<string, string>> | undefined;

        const waitPromise = result.current.waitForSeeding().then((values) => {
            seededValues = values;
        });

        rerender({keys: ['first'], seed, seedKey: 'two'});

        await act(async () => {
            firstSeed.resolve({field: 'stale'});
        });

        expect(seededValues).toBeUndefined();

        await act(async () => {
            secondSeed.resolve({field: 'fresh'});

            await waitPromise;
        });

        expect(seededValues).toEqual({first: {field: 'fresh'}});
        expect(result.current.seededValuesByKey.first).toEqual({field: 'fresh'});
    });

    it('drops the values of a removed key', async () => {
        const seed = vi.fn(async () => ({field: 'value'}));

        const {rerender, result} = renderSeeding({keys: ['first'], seed, seedKey: 'scope'});

        await waitFor(() => {
            expect(result.current.seededValuesByKey.first).toBeDefined();
        });

        rerender({keys: [], seed, seedKey: 'scope'});

        await waitFor(() => {
            expect(result.current.seededValuesByKey.first).toBeUndefined();
        });

        await expect(result.current.waitForSeeding()).resolves.toEqual({});
    });

    it('falls back to no values when a seed fails', async () => {
        const seed = vi.fn(async () => {
            throw new Error('load failed');
        });

        const {result} = renderSeeding({keys: ['first'], seed, seedKey: 'scope'});

        await expect(result.current.waitForSeeding()).resolves.toEqual({first: {}});
    });
});
