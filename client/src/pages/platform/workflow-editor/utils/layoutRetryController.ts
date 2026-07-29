/**
 * Tiny state machine deciding whether a failed workflow layout should be
 * retried. A rejected layout promise (or a throw applying its result) would
 * otherwise leave the canvas frozen at the previous direction/engine, so the
 * effect retries once — but ONLY once per failure streak, or a persistently
 * failing layout would re-trigger the effect forever.
 *
 * The reset is deliberately its own event (`onLayoutSuccess`) rather than being
 * folded into the failure path: it must fire only after nodes are applied
 * without throwing. Resetting before the work (e.g. at the top of the success
 * handler) re-arms the retry even when the success handler itself throws,
 * producing an infinite retry loop.
 */
export interface LayoutRetryStateI {
    hasRetried: boolean;
}

export function createLayoutRetryState(): LayoutRetryStateI {
    return {hasRetried: false};
}

/**
 * Records a successful layout — clears the guard so a genuinely new failure
 * later is allowed its own single retry. Call this only after the layout
 * result has been applied without throwing.
 */
export function onLayoutSuccess(state: LayoutRetryStateI): void {
    state.hasRetried = false;
}

/**
 * Records a failed layout and reports whether a retry should be triggered.
 * Returns true at most once per failure streak.
 */
export function onLayoutFailure(state: LayoutRetryStateI): boolean {
    if (state.hasRetried) {
        return false;
    }

    state.hasRetried = true;

    return true;
}
