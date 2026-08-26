import {createContext, useContext} from 'react';

/**
 * True while a `<Property>` tree is rendered ON THE CANVAS rather than inside the workflow node
 * details panel — today that is `GraphTransitionPopover`, the inline editor for one graph transition.
 *
 * The shared mentions input opens the Data Pill Panel when it takes focus, which is right in the
 * details panel: the two sit side by side there, and the panel is how a pill gets picked. A popover
 * pinned to an edge has neither property — the panel slides in over the canvas the popover is
 * anchored to, and it is keyed on the graph container rather than on the transition being edited.
 *
 * Carried by context rather than by prop because Property nests itself through several
 * intermediaries (object sub-properties, array items, dynamic sub-properties), and a prop any one of
 * them forgets to forward silently restores the panel underneath it.
 */
const CanvasPropertyEditorContext = createContext(false);

export const CanvasPropertyEditorProvider = CanvasPropertyEditorContext.Provider;

export const useCanvasPropertyEditorContext = () => useContext(CanvasPropertyEditorContext);
