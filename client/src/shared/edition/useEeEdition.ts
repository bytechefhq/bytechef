import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';

/**
 * True only when the edition is positively known to be Enterprise, mirroring `EEVersion` (which renders its
 * Enterprise children on `application?.edition === 'EE'` and the upsell otherwise).
 *
 * This is deliberately NOT `!useCeEdition()`, and the reason is not a startup race: `main.tsx` awaits
 * `getApplicationInfo()` before it calls `root.render(...)`, so on the normal boot path nothing mounts while
 * `application` is still null.
 *
 * The reason is a *failed* `/actuator/info`. `useApplicationInfoStore.getApplicationInfo` assigns `application`
 * only inside its `response.status === 200` branch, leaves `loading` true on any other status, and early-returns
 * while `loading` — so it never retries. One non-200 or one network failure therefore leaves `application` null
 * for the entire session. Under a `!useCeEdition()` gate a Community install in that state reads as "not CE", so
 * every project open would fire the EE-only `myWorkspaceRole` / `myWorkspaceScopes` query, `graphqlFetcher` would
 * throw on the `errors` array the CE schema returns, and the user would get back exactly the pair of toasts this
 * gate exists to remove. Requiring positively-established Enterprise fails the other way: the query is simply
 * never sent, and the scope hooks fail closed on an unresolved edition — no toasts, and no affordance offered
 * that the server would refuse.
 */
export default function useEeEdition(): boolean {
    const application = useApplicationInfoStore((state) => state.application);

    return application?.edition === EditionType.EE;
}
