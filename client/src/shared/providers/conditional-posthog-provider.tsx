import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {PostHog} from 'posthog-js';
import {ReactNode, Suspense, lazy, useEffect, useState} from 'react';

const PostHogProvider = lazy(() => import('posthog-js/react').then((module) => ({default: module.PostHogProvider})));

interface ConditionalPostHogProviderProps {
    children: ReactNode;
}

const PostHogFallback = ({children}: {children: ReactNode}) => <>{children}</>;

export const ConditionalPostHogProvider = ({children}: ConditionalPostHogProviderProps) => {
    const [posthog, setPosthog] = useState<PostHog | null>(null);

    const analytics = useApplicationInfoStore((state) => state.analytics);

    useEffect(() => {
        if (analytics.enabled && analytics.postHog.apiKey && analytics.postHog.host) {
            import('posthog-js').then((module) => setPosthog(module.default));
        }
    }, [analytics.enabled, analytics.postHog.apiKey, analytics.postHog.host]);

    useEffect(() => {
        if (posthog && analytics.postHog.apiKey && analytics.postHog.host) {
            posthog.init(analytics.postHog.apiKey, {
                api_host: analytics.postHog.host,
                capture_pageview: false,
                person_profiles: 'identified_only',
            });
        }
    }, [posthog, analytics.postHog.apiKey, analytics.postHog.host]);

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
