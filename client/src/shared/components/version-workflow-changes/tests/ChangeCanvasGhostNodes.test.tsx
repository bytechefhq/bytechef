import useLayoutDirectionStore from '@/pages/platform/workflow-editor/stores/useLayoutDirectionStore';
import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {
    ChangeCanvasBottomGhostNode,
    ChangeCanvasLeftGhostNode,
    ChangeCanvasTopGhostNode,
} from '../ChangeCanvasGhostNodes';

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    Handle: ({className, id, position, type}: {className: string; id: string; position: string; type: string}) => (
        <div className={className} data-position={position} data-testid={id} data-type={type} />
    ),
}));

const getHandle = (id: string) => screen.getByTestId(id);

describe('ChangeCanvasGhostNodes', () => {
    beforeEach(() => {
        useLayoutDirectionStore.setState({layoutDirection: 'TB'});
    });

    it('gives a top bar a target on top, sources on both sides and a source at the bottom', () => {
        render(<ChangeCanvasTopGhostNode id="condition_1-top-ghost" />);

        expect(getHandle('condition_1-top-ghost-top')).toHaveAttribute('data-type', 'target');
        expect(getHandle('condition_1-top-ghost-left')).toHaveAttribute('data-type', 'source');
        expect(getHandle('condition_1-top-ghost-left')).toHaveClass('left-8');
        expect(getHandle('condition_1-top-ghost-right')).toHaveAttribute('data-type', 'source');
        expect(getHandle('condition_1-top-ghost-right')).toHaveClass('right-8');
        expect(getHandle('condition_1-top-ghost-bottom')).toHaveAttribute('data-type', 'source');
    });

    it('gives a bottom bar targets on both sides', () => {
        render(<ChangeCanvasBottomGhostNode id="condition_1-bottom-ghost" />);

        expect(getHandle('condition_1-bottom-ghost-left')).toHaveAttribute('data-type', 'target');
        expect(getHandle('condition_1-bottom-ghost-left')).toHaveClass('left-8');
        expect(getHandle('condition_1-bottom-ghost-right')).toHaveAttribute('data-type', 'target');
        expect(getHandle('condition_1-bottom-ghost-right')).toHaveClass('right-8');
    });

    it('turns the bars and moves the side handles for a left-to-right layout', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'LR'});

        const {container} = render(
            <>
                <ChangeCanvasTopGhostNode id="top" />

                <ChangeCanvasBottomGhostNode id="bottom" />
            </>
        );

        expect(container.querySelector('[data-nodetype="taskDispatcherTopGhostNode"]')).toHaveClass('flex-col');
        expect(getHandle('top-top')).toHaveAttribute('data-position', 'left');
        expect(getHandle('top-left')).toHaveClass('top-8');
        expect(getHandle('top-right')).toHaveClass('bottom-8');
        expect(getHandle('bottom-right')).toHaveClass('bottom-8');
        expect(getHandle('bottom-left')).toHaveClass('top-8');
    });

    it('renders a left ghost with a target above and a source below', () => {
        render(<ChangeCanvasLeftGhostNode id="branch_1" />);

        expect(getHandle('branch_1-left-ghost-top')).toHaveAttribute('data-type', 'target');
        expect(getHandle('branch_1-left-ghost-top')).toHaveClass('top-8');
        expect(getHandle('branch_1-left-ghost-bottom')).toHaveAttribute('data-type', 'source');
    });

    it('lays a left ghost on its side for a left-to-right layout', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'LR'});

        const {container} = render(<ChangeCanvasLeftGhostNode id="branch_1" />);

        expect(container.querySelector('[data-nodetype="taskDispatcherLeftGhostNode"]')).toHaveClass('w-4');
        expect(getHandle('branch_1-left-ghost-top')).toHaveClass('left-8');
    });
});
