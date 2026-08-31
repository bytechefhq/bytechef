import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';

/**
 * True only when the edition is positively known to be Community.
 *
 * The application info is fetched from `/actuator/info` after startup, so `application` is null
 * until it resolves. Testing for CE rather than for `!== 'EE'` keeps that pre-resolution window
 * behaving like Enterprise, which is the safe direction: CE branches hide role controls and grant
 * ROLE_ADMIN, and neither should happen on an edition that has not been established yet.
 */
export default function useCeEdition(): boolean {
    const application = useApplicationInfoStore((state) => state.application);

    return application?.edition === EditionType.CE;
}
