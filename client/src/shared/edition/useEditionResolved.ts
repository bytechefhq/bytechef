import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';

/**
 * True once `/actuator/info` has answered and the edition is positively known to be either Community or Enterprise.
 *
 * `useCeEdition` and `useEeEdition` both answer `false` while the edition is unresolved, which is the right fail-closed
 * posture for gating but leaves callers unable to tell "the server refused you" from "nobody ever asked". That matters
 * because the unresolved window is not always brief: `useApplicationInfoStore.getApplicationInfo` assigns `application`
 * only inside its `response.status === 200` branch, leaves `loading` true on any other status, and early-returns while
 * `loading` — so a single non-200 or network failure leaves the edition unresolved for the whole session with no retry.
 *
 * Permission state hooks report this as a distinct flag so a control disabled in that window can say its check never
 * completed instead of asserting a denial that no server ever made.
 */
export default function useEditionResolved(): boolean {
    const application = useApplicationInfoStore((state) => state.application);

    return application != null;
}
