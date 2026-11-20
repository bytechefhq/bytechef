import {registerStaleChunkReload, reloadOnStaleChunk} from '@/shared/util/reloadOnStaleChunk';
import {afterEach, describe, expect, it, vi} from 'vitest';

const createStorage = () => {
    const values = new Map<string, string>();

    return {
        getItem: (key: string) => values.get(key) ?? null,
        setItem: (key: string, value: string) => {
            values.set(key, value);
        },
    };
};

describe('reloadOnStaleChunk', () => {
    it('reloads the page the first time a chunk fails to load', () => {
        const reload = vi.fn();

        expect(reloadOnStaleChunk({getStorage: createStorage, now: () => 100_000, reload})).toBe(true);
        expect(reload).toHaveBeenCalledTimes(1);
    });

    it('does not reload again while the chunk is still missing right after a reload', () => {
        const reload = vi.fn();
        const storage = createStorage();

        reloadOnStaleChunk({getStorage: () => storage, now: () => 100_000, reload});

        expect(reloadOnStaleChunk({getStorage: () => storage, now: () => 105_000, reload})).toBe(false);
        expect(reload).toHaveBeenCalledTimes(1);
    });

    it('reloads again once the cooldown has passed, for the next deploy', () => {
        const reload = vi.fn();
        const storage = createStorage();

        reloadOnStaleChunk({getStorage: () => storage, now: () => 100_000, reload});

        expect(reloadOnStaleChunk({getStorage: () => storage, now: () => 111_000, reload})).toBe(true);
        expect(reload).toHaveBeenCalledTimes(2);
    });

    it('does not reload when storage is unavailable, since nothing would stop a reload loop', () => {
        const reload = vi.fn();

        const getStorage = () => {
            throw new DOMException('The operation is insecure.', 'SecurityError');
        };

        expect(reloadOnStaleChunk({getStorage, reload})).toBe(false);
        expect(reload).not.toHaveBeenCalled();
    });
});

describe('registerStaleChunkReload', () => {
    afterEach(() => {
        window.sessionStorage.clear();

        vi.restoreAllMocks();
    });

    it('reloads the page when Vite reports a failed dynamic import', () => {
        const reload = vi.fn();

        vi.spyOn(window, 'location', 'get').mockReturnValue({...window.location, reload});

        registerStaleChunkReload();

        const event = new Event('vite:preloadError', {cancelable: true});

        window.dispatchEvent(event);

        expect(reload).toHaveBeenCalledTimes(1);
        expect(event.defaultPrevented).toBe(true);
    });

    it('lets Vite throw the error when the chunk is still missing right after a reload', () => {
        const reload = vi.fn();

        vi.spyOn(window, 'location', 'get').mockReturnValue({...window.location, reload});

        registerStaleChunkReload();

        window.sessionStorage.setItem('bytechef.stale-chunk-reload-at', String(Date.now()));

        const event = new Event('vite:preloadError', {cancelable: true});

        window.dispatchEvent(event);

        expect(reload).not.toHaveBeenCalled();
        expect(event.defaultPrevented).toBe(false);
    });
});
