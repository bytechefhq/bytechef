import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';

interface CaptureRenderErrorOptionsI {
    componentStack?: string | null;
    source: 'error_boundary' | 'route_error_page';
}

export const captureRenderError = async (error: Error, {componentStack, source}: CaptureRenderErrorOptionsI) => {
    if (!applicationInfoStore.getState().analytics.enabled) {
        return;
    }

    try {
        const {default: posthog} = await import('posthog-js');

        if (posthog.__loaded) {
            posthog.captureException(error, {componentStack, source});
        }
    } catch (captureError) {
        console.warn('Could not report the render error:', captureError);
    }
};
