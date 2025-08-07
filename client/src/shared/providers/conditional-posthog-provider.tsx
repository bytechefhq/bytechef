import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {PostHog} from 'posthog-js';
import {ReactNode, Suspense, lazy, useEffect, useState} from 'react';

const PostHogProvider = lazy(() => import('posthog-js/react').then((module) => ({default: module.PostHogProvider})));

interface ConditionalPostHogProviderProps {
    children: ReactNode;
}

const PostHogFallback = ({children}: {children: ReactNode}) => <>{children}</>;

export const ConditionalPostHogProvider = ({children}: ConditionalPostHogProviderProps) => {
    const {analytics} = useApplicationInfoStore();
    const [posthog, setPosthog] = useState<PostHog | null>(null);

    useEffect(() => {
        if (analytics.enabled && analytics.postHog.apiKey && analytics.postHog.host) {
            import('posthog-js').then((module) => setPosthog(module.default));
        }
    }, [analytics.enabled, analytics.postHog.apiKey, analytics.postHog.host]);

    if (!analytics.enabled || !analytics.postHog.apiKey || !analytics.postHog.host) {
        return <PostHogFallback>{children}</PostHogFallback>;
    }

    if (!posthog) {
        return <PostHogFallback>{children}</PostHogFallback>;
    }

    return (
        <Suspense fallback={<PostHogFallback>{children}</PostHogFallback>}>
            <PostHogProvider client={posthog}>{children}</PostHogProvider>
        </Suspense>
    );
};
