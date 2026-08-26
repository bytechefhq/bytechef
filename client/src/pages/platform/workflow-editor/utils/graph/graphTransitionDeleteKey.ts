import {GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {Edge} from '@xyflow/react';

/**
 * The graph transition a Backspace/Delete press should remove, identified by its graph and its
 * declaration index.
 *
 * The canvas turns React Flow's own delete key off wholesale (`deleteKeyCode={null}` in
 * `WorkflowEditor`), because deleting a node or a structural chain edge by keypress is not a
 * behaviour this editor offers. A transition is the one exception: it is a row in
 * `parameters.transitions`, and removing it removes nothing else. So the key handler looks for
 * exactly that edge type and ignores every other selection.
 */
export function findSelectedGraphTransition(edges: Edge[]): {graphId: string; index: number} | undefined {
    const selectedTransitionEdge = edges.find((edge) => edge.selected && edge.type === GRAPH_TRANSITION_EDGE_TYPE);

    if (!selectedTransitionEdge) {
        return undefined;
    }

    const {graphId, index} = (selectedTransitionEdge.data ?? {}) as {graphId?: unknown; index?: unknown};

    if (typeof graphId !== 'string' || typeof index !== 'number') {
        return undefined;
    }

    return {graphId, index};
}

/**
 * Whether the focused element is somewhere the user types, in which case Backspace/Delete belongs
 * to that field and must not reach the canvas.
 *
 * `contenteditable` is checked as well as the form tags: the transition condition is edited through
 * `PropertyMentionsInput`, a TipTap editor whose input surface is a contenteditable div, not an
 * `<input>`. Backspacing a character out of a condition would otherwise delete the transition being
 * edited. jsdom leaves `isContentEditable` unimplemented, so the attribute is read as well.
 */
export function isTextEntryElement(element: Element | null): boolean {
    if (!element) {
        return false;
    }

    if (element.tagName === 'INPUT' || element.tagName === 'TEXTAREA' || element.tagName === 'SELECT') {
        return true;
    }

    if ((element as HTMLElement).isContentEditable) {
        return true;
    }

    const contentEditableAttribute = element.getAttribute('contenteditable');

    return (
        contentEditableAttribute === '' ||
        contentEditableAttribute === 'true' ||
        contentEditableAttribute === 'plaintext-only'
    );
}

/**
 * Marks the transition editor's root so the delete-key handler can tell that focus is inside it.
 * The editor is portalled into React Flow's edge-label layer, so it is not a DOM descendant of
 * anything else the handler could test for.
 */
export const GRAPH_TRANSITION_EDITOR_ATTRIBUTE = 'data-graph-transition-editor';

/**
 * Layered surfaces a Backspace/Delete press belongs to rather than to the canvas underneath.
 *
 * The listener sits on `document`, so it hears the key wherever focus is. Text entry is the
 * obvious case, but not the only one: Radix renders the transition editor's To picker as a
 * `<button role="combobox">`, which is not text entry at all, so without this a Delete pressed with
 * the picker focused would delete the very transition the open editor is editing. A dialog opened
 * over the canvas with an edge still selected is the same failure one layer up.
 */
const LAYERED_SURFACE_SELECTOR = [
    '[role="dialog"]',
    '[role="alertdialog"]',
    '[role="listbox"]',
    '[role="menu"]',
    '[role="combobox"]',
    `[${GRAPH_TRANSITION_EDITOR_ATTRIBUTE}]`,
].join(',');

/**
 * Whether a Backspace/Delete press should reach the canvas: nothing focused, or focus on something
 * that is neither a text entry nor part of a surface layered over the canvas.
 *
 * "Nothing focused" is withdrawn while a transition editor is open, and that is the whole reason
 * this function knows about the DOM at all. The editor's condition is a contenteditable, and when a
 * relayout rebuilds the edge underneath it the focus that was inside it lands back on `document.body`
 * — indistinguishable, to `document.activeElement`, from a user who clicked the bare canvas. The
 * user is still typing; the next Delete deleted the very transition they had open. There is nothing
 * to lose by standing down: the shortcut could only ever fire while that editor was showing, since
 * selecting the transition is what opens it, and the editor carries its own delete button.
 */
export function isCanvasDeleteKeyTarget(element: Element | null): boolean {
    if (!element || element === document.body) {
        return !document.querySelector(`[${GRAPH_TRANSITION_EDITOR_ATTRIBUTE}]`);
    }

    if (isTextEntryElement(element)) {
        return false;
    }

    return !element.closest(LAYERED_SURFACE_SELECTOR);
}
