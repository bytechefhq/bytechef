import {handleActivationKeyDown} from '@/shared/util/keyboard-utils';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const mockOnClick = vi.fn();

const renderActivatableElement = () =>
    render(
        <div onClick={mockOnClick} onKeyDown={handleActivationKeyDown} role="button" tabIndex={0}>
            Row
            <button type="button">Nested</button>
        </div>
    );

beforeEach(() => {
    vi.clearAllMocks();
});

describe('handleActivationKeyDown', () => {
    it('clicks the focused element on Enter', () => {
        renderActivatableElement();

        fireEvent.keyDown(screen.getByRole('button', {name: /Row/}), {key: 'Enter'});

        expect(mockOnClick).toHaveBeenCalledTimes(1);
    });

    it('clicks the focused element on Space and prevents the page from scrolling', () => {
        renderActivatableElement();

        const notCancelled = fireEvent.keyDown(screen.getByRole('button', {name: /Row/}), {key: ' '});

        expect(mockOnClick).toHaveBeenCalledTimes(1);
        expect(notCancelled).toBe(false);
    });

    it('ignores other keys', () => {
        renderActivatableElement();

        fireEvent.keyDown(screen.getByRole('button', {name: /Row/}), {key: 'a'});

        expect(mockOnClick).not.toHaveBeenCalled();
    });

    it('ignores keys pressed on a nested element so the row does not act twice', () => {
        renderActivatableElement();

        fireEvent.keyDown(screen.getByRole('button', {name: 'Nested'}), {key: 'Enter'});

        expect(mockOnClick).not.toHaveBeenCalled();
    });
});
