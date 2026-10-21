import type {CallOptions} from './connections';
import {type Requester, untilAborted} from './request';
import {expectObject} from './response';
import type {AppContext} from './types';

export interface ContextApiI {
    /**
     * The calling app, its instance, the instance configuration and the sign-in settings, from `GET /context`. Kept per
     * client for `contextTtlMs` after it arrives; concurrent callers share one fetch, and a failed fetch is not kept.
     */
    get(options?: CallOptions): Promise<AppContext>;
    /** Drops the kept context, so the next `get` fetches it again. */
    invalidate(): void;
}

interface ContextEntryI {
    context: Promise<AppContext>;
    expiresAt?: number;
}

export function createContextApi(
    request: Requester,
    getTtlMs: () => number,
    now: () => number = () => Date.now()
): ContextApiI {
    let entry: ContextEntryI | undefined;

    const load = async (): Promise<AppContext> => {
        const path = '/context';

        return expectObject<AppContext>(await request({method: 'GET', path}), path);
    };

    const isFresh = (current: ContextEntryI) => current.expiresAt === undefined || now() < current.expiresAt;

    return {
        get(options: CallOptions = {}): Promise<AppContext> {
            if (entry === undefined || !isFresh(entry)) {
                const current: ContextEntryI = {context: load()};

                entry = current;

                current.context.then(
                    () => {
                        current.expiresAt = now() + getTtlMs();
                    },
                    () => {
                        if (entry === current) {
                            entry = undefined;
                        }
                    }
                );
            }

            return options.signal ? untilAborted(entry.context, options.signal) : entry.context;
        },

        invalidate(): void {
            entry = undefined;
        },
    };
}
