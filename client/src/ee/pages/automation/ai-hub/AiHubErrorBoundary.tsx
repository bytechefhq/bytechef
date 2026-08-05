import {Component, type ErrorInfo, type PropsWithChildren, type ReactNode} from 'react';

interface AiHubErrorBoundaryProps extends PropsWithChildren {
    open: boolean;
}

interface AiHubErrorBoundaryStateI {
    hasError: boolean;
    previousOpen: boolean;
}

const KNOWN_UNMOUNT_ERROR = 'unmount a fiber that is already unmounted';

/**
 * Error boundary that catches unmount errors from @assistant-ui/react's
 * AssistantRuntimeProviderImpl. The library throws "Tried to unmount a fiber
 * that is already unmounted" when conditionally rendered in React 19.
 *
 * Wraps the AI Hub runtime provider so the boundary stays mounted when
 * the inner runtime tree unmounts.
 */
class AiHubErrorBoundary extends Component<AiHubErrorBoundaryProps, AiHubErrorBoundaryStateI> {
    state: AiHubErrorBoundaryStateI = {hasError: false, previousOpen: false};

    static getDerivedStateFromError(): AiHubErrorBoundaryStateI {
        return {hasError: true, previousOpen: false};
    }

    static getDerivedStateFromProps(
        props: AiHubErrorBoundaryProps,
        state: AiHubErrorBoundaryStateI
    ): Partial<AiHubErrorBoundaryStateI> | null {
        // Reset error state only when open transitions from false to true,
        // preventing infinite error→retry loops when children are always truthy.
        if (props.open && !state.previousOpen) {
            return {hasError: false, previousOpen: true};
        }

        if (!props.open && state.previousOpen) {
            return {previousOpen: false};
        }

        return null;
    }

    componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
        const isKnownUnmountError = error.message?.includes(KNOWN_UNMOUNT_ERROR);

        if (!isKnownUnmountError) {
            console.error('AiHubErrorBoundary caught an unexpected error:', error, errorInfo);
        }
    }

    render(): ReactNode {
        if (this.state.hasError) {
            return null;
        }

        return this.props.children;
    }
}

export default AiHubErrorBoundary;
