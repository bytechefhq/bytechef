import {SuggestionProps} from '@tiptap/suggestion';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {createSuggestionPopupRenderer} from '../suggestionPopupRenderer';

const reactRendererInstances: Array<{
    destroy: ReturnType<typeof vi.fn>;
    ref: {onKeyDown: ReturnType<typeof vi.fn>};
    updateProps: ReturnType<typeof vi.fn>;
}> = [];
const tippyInstances: Array<{
    destroy: ReturnType<typeof vi.fn>;
    hide: ReturnType<typeof vi.fn>;
    popper: {addEventListener: ReturnType<typeof vi.fn>};
    setProps: ReturnType<typeof vi.fn>;
}> = [];

vi.mock('@tiptap/react', () => ({
    ReactRenderer: vi.fn(function ReactRendererMock(this: Record<string, unknown>) {
        const instance = {
            destroy: vi.fn(),
            element: document.createElement('div'),
            ref: {onKeyDown: vi.fn(() => true)},
            updateProps: vi.fn(),
        };

        reactRendererInstances.push(instance);

        Object.assign(this, instance);
    }),
}));

const tippyOptions: Array<Record<string, () => unknown>> = [];

vi.mock('tippy.js', () => ({
    default: vi.fn((_target: string, options: Record<string, () => unknown>) => {
        tippyOptions.push(options);

        const instance = {
            destroy: vi.fn(),
            hide: vi.fn(),
            popper: {addEventListener: vi.fn()},
            setProps: vi.fn(),
        };

        tippyInstances.push(instance);

        return [instance];
    }),
}));

const listComponent = (() => null) as never;

function createEditor(isFocused = true) {
    return {
        isFocused,
        storage: {MentionStorage: {dataPills: [], suggestionOpen: false}},
    };
}

const CARET_RECT = (() => ({})) as unknown as () => DOMRect;

function createStartProps(editor: ReturnType<typeof createEditor>, clientRect: (() => DOMRect) | null = CARET_RECT) {
    return {clientRect, editor, items: [], query: ''} as unknown as SuggestionProps<unknown>;
}

describe('createSuggestionPopupRenderer', () => {
    beforeEach(() => {
        reactRendererInstances.length = 0;
        tippyInstances.length = 0;
        tippyOptions.length = 0;
    });

    it('does not open a popup while the editor is unfocused', () => {
        const renderer = createSuggestionPopupRenderer(listComponent)();

        renderer.onStart!(createStartProps(createEditor(false)));

        expect(reactRendererInstances).toHaveLength(0);
        expect(tippyInstances).toHaveLength(0);
    });

    it('tears the renderer down again when the caret has no client rect', () => {
        const renderer = createSuggestionPopupRenderer(listComponent)();

        renderer.onStart!(createStartProps(createEditor(), null));

        expect(reactRendererInstances[0].destroy).toHaveBeenCalled();
        expect(tippyInstances).toHaveLength(0);
    });

    it('opens the popup, intercepts wheel events and flags the suggestion as open', () => {
        const editor = createEditor();
        const renderer = createSuggestionPopupRenderer(listComponent)();

        renderer.onStart!(createStartProps(editor));

        expect(tippyInstances).toHaveLength(1);
        expect(tippyInstances[0].popper.addEventListener).toHaveBeenCalledWith(
            'wheel',
            expect.any(Function),
            expect.objectContaining({capture: true, passive: true})
        );
        expect(editor.storage.MentionStorage.suggestionOpen).toBe(true);
    });

    it('hides the popup on Escape and otherwise delegates to the list', () => {
        const renderer = createSuggestionPopupRenderer(listComponent)();

        expect(renderer.onKeyDown!({event: {key: 'ArrowDown'}} as never)).toBe(false);

        renderer.onStart!(createStartProps(createEditor()));

        expect(renderer.onKeyDown!({event: {key: 'Escape'}} as never)).toBe(true);
        expect(tippyInstances[0].hide).toHaveBeenCalled();

        expect(renderer.onKeyDown!({event: {key: 'ArrowDown'}} as never)).toBe(true);
        expect(reactRendererInstances[0].ref.onKeyDown).toHaveBeenCalled();
    });

    it('anchors the popup on the body at the caret rect', () => {
        const renderer = createSuggestionPopupRenderer(listComponent)();

        renderer.onStart!(createStartProps(createEditor()));

        expect(tippyOptions[0].appendTo()).toBe(document.body);
        expect(tippyOptions[0].getReferenceClientRect()).toEqual(CARET_RECT());
    });

    it('repositions the popup on update, and keeps the last rect when one is missing', () => {
        const updatedRect = {top: 42} as unknown as DOMRect;
        const renderer = createSuggestionPopupRenderer(listComponent)();

        renderer.onStart!(createStartProps(createEditor()));

        renderer.onUpdate!(createStartProps(createEditor(), () => updatedRect));

        expect(reactRendererInstances[0].updateProps).toHaveBeenCalled();
        expect(tippyInstances[0].setProps).toHaveBeenCalledTimes(1);

        const [{getReferenceClientRect}] = tippyInstances[0].setProps.mock.calls[0];

        expect(getReferenceClientRect()).toBe(updatedRect);

        renderer.onUpdate!(createStartProps(createEditor(), null));

        expect(tippyInstances[0].setProps).toHaveBeenCalledTimes(1);
        // The popup keeps pointing at the last rect it was given rather than jumping to the origin.
        expect(getReferenceClientRect()).toBe(updatedRect);
    });

    it('clears the open flag and destroys the popup on exit', () => {
        const editor = createEditor();
        const renderer = createSuggestionPopupRenderer(listComponent)();

        renderer.onStart!(createStartProps(editor));
        renderer.onExit!(createStartProps(editor));

        expect(editor.storage.MentionStorage.suggestionOpen).toBe(false);
        expect(tippyInstances[0].destroy).toHaveBeenCalled();
        expect(reactRendererInstances[0].destroy).toHaveBeenCalled();
    });

    it('exits cleanly when the popup was never opened', () => {
        const renderer = createSuggestionPopupRenderer(listComponent)();

        expect(() => renderer.onExit!(createStartProps(createEditor(false)))).not.toThrow();
    });
});
