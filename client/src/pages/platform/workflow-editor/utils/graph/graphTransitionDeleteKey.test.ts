import {GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {Edge} from '@xyflow/react';
import {afterEach, describe, expect, it} from 'vitest';

import {
    GRAPH_TRANSITION_EDITOR_ATTRIBUTE,
    findSelectedGraphTransition,
    isCanvasDeleteKeyTarget,
    isTextEntryElement,
} from './graphTransitionDeleteKey';

function buildTransitionEdge(overrides: Partial<Edge> = {}): Edge {
    return {
        data: {graphId: 'graph_1', index: 2},
        id: 'graph_1-transition-2',
        selected: true,
        source: 'task_1',
        target: 'task_2',
        type: GRAPH_TRANSITION_EDGE_TYPE,
        ...overrides,
    };
}

describe('findSelectedGraphTransition', () => {
    it('identifies the selected transition by its graph and declaration index', () => {
        expect(findSelectedGraphTransition([buildTransitionEdge()])).toEqual({graphId: 'graph_1', index: 2});
    });

    it('ignores a transition edge that is not selected', () => {
        expect(findSelectedGraphTransition([buildTransitionEdge({selected: false})])).toBeUndefined();
    });

    it('ignores a selected edge of any other type', () => {
        expect(findSelectedGraphTransition([buildTransitionEdge({type: 'workflow'})])).toBeUndefined();
    });

    it('ignores a transition edge carrying no graph id or index', () => {
        expect(findSelectedGraphTransition([buildTransitionEdge({data: {}})])).toBeUndefined();
    });
});

describe('isTextEntryElement', () => {
    it('reports nothing focused as not a text entry', () => {
        expect(isTextEntryElement(null)).toBe(false);
    });

    it.each(['input', 'textarea', 'select'])('reports a focused %s as a text entry', (tagName) => {
        expect(isTextEntryElement(document.createElement(tagName))).toBe(true);
    });

    it('reports a contenteditable element as a text entry', () => {
        const editableElement = document.createElement('div');

        editableElement.setAttribute('contenteditable', 'true');

        expect(isTextEntryElement(editableElement)).toBe(true);
    });

    it('reports a plain element as not a text entry', () => {
        expect(isTextEntryElement(document.createElement('div'))).toBe(false);
    });
});

describe('isCanvasDeleteKeyTarget', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    function renderFocusTarget(html: string): Element {
        document.body.innerHTML = html;

        return document.body.querySelector('[data-focus]')!;
    }

    it('lets the press through when nothing is focused', () => {
        expect(isCanvasDeleteKeyTarget(null)).toBe(true);
    });

    it('lets the press through when focus rests on the document body', () => {
        expect(isCanvasDeleteKeyTarget(document.body)).toBe(true);
    });

    // Focus inside the editor's contenteditable condition lands back on the body when a relayout
    // rebuilds the edge underneath it — which `document.activeElement` cannot tell apart from a click
    // on the bare canvas, so the next Delete removed the transition being typed into.
    it('holds the press back on the body while a transition editor is open', () => {
        document.body.innerHTML = `<div ${GRAPH_TRANSITION_EDITOR_ATTRIBUTE}></div>`;

        expect(isCanvasDeleteKeyTarget(document.body)).toBe(false);
        expect(isCanvasDeleteKeyTarget(null)).toBe(false);
    });

    it('lets the press through for a plain focused element on the canvas', () => {
        expect(isCanvasDeleteKeyTarget(renderFocusTarget('<div data-focus tabindex="0"></div>'))).toBe(true);
    });

    it('keeps the press away from a text entry', () => {
        expect(isCanvasDeleteKeyTarget(renderFocusTarget('<input data-focus />'))).toBe(false);
    });

    // Radix renders the transition editor's To picker as a button, not a text entry — pressing
    // Delete with it focused must not delete the transition the open editor is editing.
    it('keeps the press away from a focused select trigger', () => {
        expect(isCanvasDeleteKeyTarget(renderFocusTarget('<button data-focus role="combobox">task_2</button>'))).toBe(
            false
        );
    });

    it('keeps the press away from anything inside the transition editor', () => {
        expect(
            isCanvasDeleteKeyTarget(
                renderFocusTarget(`<div ${GRAPH_TRANSITION_EDITOR_ATTRIBUTE}><button data-focus>Delete</button></div>`)
            )
        ).toBe(false);
    });

    it('keeps the press away from a dialog opened over the canvas', () => {
        expect(
            isCanvasDeleteKeyTarget(renderFocusTarget('<div role="dialog"><button data-focus>Save</button></div>'))
        ).toBe(false);
    });

    it('keeps the press away from an open listbox', () => {
        expect(
            isCanvasDeleteKeyTarget(renderFocusTarget('<div role="listbox"><div data-focus role="option"></div></div>'))
        ).toBe(false);
    });
});
