import useProjectDeploymentWorkflowSheetStore from '@/pages/automation/project-deployments/stores/useProjectDeploymentWorkflowSheetStore';
import useProjectHeaderDialogsStore from '@/pages/automation/project/stores/useProjectHeaderDialogsStore';
import {act, render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import EditorGettingStartedOverlay from '../EditorGettingStartedOverlay';

const mocks = vi.hoisted(() => ({
    disconnect: vi.fn(),
    observe: vi.fn(),
}));

vi.mock('../EditorGettingStartedCard', () => ({
    default: () => <div data-testid="editor-getting-started-card" />,
}));

const getPortalWrapper = () => screen.getByTestId('editor-getting-started-card').parentElement!.parentElement!;

describe('EditorGettingStartedOverlay', () => {
    beforeEach(() => {
        vi.stubGlobal(
            'ResizeObserver',
            class {
                disconnect = mocks.disconnect;
                observe = mocks.observe;
            }
        );

        vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue({
            height: 400,
            left: 24,
            top: 64,
        } as DOMRect);

        useProjectHeaderDialogsStore.setState({deploymentsSheetState: undefined});
        useProjectDeploymentWorkflowSheetStore.setState({projectDeploymentWorkflowSheetOpen: false});
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.restoreAllMocks();
        mocks.disconnect.mockReset();
        mocks.observe.mockReset();
    });

    it('positions the card from the placeholder and keeps it below the sheets by default', () => {
        render(<EditorGettingStartedOverlay projectId={5} projectWorkflowId={7} />);

        const portalWrapper = getPortalWrapper();

        expect(portalWrapper.parentElement).toBe(document.body);
        expect(portalWrapper).toHaveClass('z-10');
        expect(portalWrapper).toHaveStyle({height: '400px', left: '24px', top: '64px'});

        const placeholder = screen.getByTestId('editor-getting-started-placeholder');

        expect(mocks.observe).toHaveBeenCalledWith(placeholder);
        expect(mocks.observe).toHaveBeenCalledWith(placeholder.parentElement);
    });

    it('raises the card above the deployments sheet while it is open', () => {
        render(<EditorGettingStartedOverlay projectId={5} projectWorkflowId={7} />);

        act(() => {
            useProjectHeaderDialogsStore.setState({deploymentsSheetState: {}});
        });

        expect(getPortalWrapper()).toHaveClass('z-[60]');
    });

    it('raises the card above the executions sheet while it is open', () => {
        render(<EditorGettingStartedOverlay projectId={5} projectWorkflowId={7} />);

        act(() => {
            useProjectDeploymentWorkflowSheetStore.setState({projectDeploymentWorkflowSheetOpen: true});
        });

        expect(getPortalWrapper()).toHaveClass('z-[60]');
    });

    it('stops observing the placeholder and listening for resizes on unmount', () => {
        const removeEventListenerSpy = vi.spyOn(window, 'removeEventListener');

        const {unmount} = render(<EditorGettingStartedOverlay projectId={5} projectWorkflowId={7} />);

        unmount();

        expect(mocks.disconnect).toHaveBeenCalled();
        expect(removeEventListenerSpy).toHaveBeenCalledWith('resize', expect.any(Function));
    });
});
