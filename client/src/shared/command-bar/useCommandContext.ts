import {type CommandContextI} from '@/shared/command-bar/types';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useCallback, useMemo, useRef} from 'react';
import {useLocation} from 'react-router-dom';

export function useCommandContext(): CommandContextI {
    const featureFlagsRef = useRef<((featureFlag: string) => boolean) | undefined>(undefined);

    const edition = useApplicationInfoStore((state) => state.application?.edition);
    const featureFlags = useFeatureFlagsStore();

    const {pathname} = useLocation();

    featureFlagsRef.current = featureFlags;

    // useFeatureFlagsStore returns a fresh function every render, so depending on its identity would
    // make the memo below inert. The ref keeps the accessor identity stable while still reading the
    // latest flag state.
    const isFeatureEnabled = useCallback((featureFlag: string) => featureFlagsRef.current!(featureFlag), []);

    return useMemo(() => ({edition, featureFlags: isFeatureEnabled, pathname}), [edition, isFeatureEnabled, pathname]);
}
