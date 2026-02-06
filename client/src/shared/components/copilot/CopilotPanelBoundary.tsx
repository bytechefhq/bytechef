import {Component, type PropsWithChildren, type ReactNode} from 'react';

interface CopilotPanelBoundaryStateI {
    hasError: boolean;
}

/**
 * Error boundary that catches unmount errors from @assistant-ui/react's
 * AssistantRuntimeProviderImpl. The library throws "Tried to unmount a fiber
 * that is already unmounted" when conditionally rendered in React 19.
 *
 * Used internally by CopilotPanel to wrap conditionally-rendered content
 * so the boundary stays mounted when the child unmounts.
 */
class CopilotPanelBoundary extends Component<PropsWithChildren, CopilotPanelBoundaryStateI> {
    state: CopilotPanelBoundaryStateI = {hasError: false};

    static getDerivedStateFromError(): CopilotPanelBoundaryStateI {
        return {hasError: true};
    }

    static getDerivedStateFromProps(
        props: PropsWithChildren,
        state: CopilotPanelBoundaryStateI
    ): CopilotPanelBoundaryStateI | null {
        if (state.hasError && props.children) {
            return {hasError: false};
        }

        return null;
    }

    render(): ReactNode {
        if (this.state.hasError) {
            return null;
        }

        return this.props.children;
    }
}

export default CopilotPanelBoundary;
