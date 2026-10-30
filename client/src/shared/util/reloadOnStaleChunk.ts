const LAST_RELOAD_STORAGE_KEY = 'bytechef.stale-chunk-reload-at';

/**
 * A chunk that is still missing right after a reload is not stale, so another reload would only loop.
 */
const RELOAD_COOLDOWN_MILLISECONDS = 10_000;

interface ReloadOnStaleChunkOptionsI {
    getStorage: () => Pick<Storage, 'getItem' | 'setItem'>;
    now?: () => number;
    reload: () => void;
}

/**
 * Reloads the page when a lazily imported chunk fails to load. After a deploy the chunks of the previous build are
 * gone, so a tab still running that build gets a 404 for every route it has not opened yet. Reloading picks up the
 * new build instead of showing the route error page.
 *
 * @return whether a reload was triggered
 */
export const reloadOnStaleChunk = ({getStorage, now = Date.now, reload}: ReloadOnStaleChunkOptionsI): boolean => {
    try {
        const storage = getStorage();

        const lastReloadAt = Number(storage.getItem(LAST_RELOAD_STORAGE_KEY) || 0);

        if (now() - lastReloadAt < RELOAD_COOLDOWN_MILLISECONDS) {
            return false;
        }

        storage.setItem(LAST_RELOAD_STORAGE_KEY, String(now()));
    } catch {
        // Without storage there is no loop guard, so leave the error to the route error page.
        return false;
    }

    reload();

    return true;
};

export const registerStaleChunkReload = () => {
    window.addEventListener('vite:preloadError', () => {
        reloadOnStaleChunk({
            getStorage: () => window.sessionStorage,
            reload: () => window.location.reload(),
        });
    });
};
