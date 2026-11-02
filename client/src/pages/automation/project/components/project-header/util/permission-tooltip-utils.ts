/**
 * Picks the tooltip for a header control that is disabled either because the user is missing a permission scope or
 * for an ordinary business reason (nothing to publish, project not published yet).
 *
 * Only a scope set that has loaded and turned out to be missing a scope is a denial. While the scopes are in flight,
 * after the fetch failed, or when the check never ran at all, the user has not been refused anything: telling an
 * entitled member "you do not have permission" in any of those windows states something false, whereas a quiet
 * disabled control merely withholds an affordance. `useWorkspaceScopeState` exists to keep those four cases apart —
 * this is the rendering half of it.
 *
 * `permissionsUnknown` is the case that has no fetch behind it: the scope query is gated on positively-known
 * Enterprise, and `/actuator/info` never retries after a non-200, so an install that failed that one call spends the
 * whole session with nothing loading, nothing errored and no stored scopes. Without this branch that state fell
 * through to `deniedMessage` — reintroducing the false statement the split exists to remove.
 */
export const getDisabledControlTooltip = ({
    deniedMessage,
    granted,
    permissionsError,
    permissionsLoading,
    permissionsUnknown,
    unmetPreconditionMessage,
}: {
    deniedMessage: string;
    granted: boolean;
    permissionsError: boolean;
    permissionsLoading: boolean;
    permissionsUnknown: boolean;
    unmetPreconditionMessage: string;
}): string => {
    if (permissionsLoading) {
        return 'Checking your permissions';
    }

    if (permissionsUnknown) {
        return 'Still determining your permissions';
    }

    if (permissionsError) {
        return 'Could not verify your permissions';
    }

    if (!granted) {
        return deniedMessage;
    }

    return unmetPreconditionMessage;
};
