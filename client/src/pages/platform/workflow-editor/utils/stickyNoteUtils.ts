import {SPACE} from '@/shared/constants';
import {UpdateWorkflowMutationType, WorkflowStickyNoteType} from '@/shared/types';
import {Node} from '@xyflow/react';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore, {runWithoutHistory} from '../stores/useWorkflowDataStore';
import {
    consumePendingDefinition,
    isWorkflowMutating,
    setPendingDefinition,
    setWorkflowMutating,
} from './workflowMutationGuard';

export const STICKY_NOTE_NODE_TYPE = 'stickyNote';

export const STICKY_NOTE_DEFAULT_HEIGHT = 160;
export const STICKY_NOTE_DEFAULT_WIDTH = 240;
export const STICKY_NOTE_MIN_HEIGHT = 100;
export const STICKY_NOTE_MIN_WIDTH = 150;

export type StickyNoteNodeDataType = {
    color: NonNullable<WorkflowStickyNoteType['color']>;
    content: string;
    readOnly?: boolean;
};

/**
 * Normalizes user input to a `#rrggbb` hex color, tolerating a missing leading
 * hash. Returns undefined for anything that is not a valid 6-digit hex color.
 */
export function normalizeHexColor(value: string): string | undefined {
    const withHash = value.startsWith('#') ? value : `#${value}`;

    return /^#[0-9a-fA-F]{6}$/.test(withHash) ? withHash.toLowerCase() : undefined;
}

/**
 * Relative-luminance check used to keep note text readable on custom colors:
 * dark backgrounds switch the content to inverted (light) typography.
 */
export function isDarkHexColor(hexColor: string): boolean {
    const hexDigits = hexColor.replace('#', '');

    const normalizedHexDigits =
        hexDigits.length === 3 ? [...hexDigits].map((hexDigit) => hexDigit + hexDigit).join('') : hexDigits;

    if (!/^[0-9a-fA-F]{6}$/.test(normalizedHexDigits)) {
        return false;
    }

    const red = parseInt(normalizedHexDigits.slice(0, 2), 16);
    const green = parseInt(normalizedHexDigits.slice(2, 4), 16);
    const blue = parseInt(normalizedHexDigits.slice(4, 6), 16);

    return (0.299 * red + 0.587 * green + 0.114 * blue) / 255 < 0.5;
}

export type StickyNoteContentSegmentType = {type: 'markdown'; value: string} | {type: 'youtube'; videoId: string};

const YOUTUBE_MARKER_REGEX = /@\[youtube\]\(([^()\s]+)\)/g;
const YOUTUBE_VIDEO_ID_REGEX = /^[A-Za-z0-9_-]{5,20}$/;

/**
 * Resolves a YouTube video id from a `@[youtube](...)` marker argument, which
 * may be a raw video id (n8n syntax) or a common YouTube URL form. The strict
 * id charset also keeps the value safe to interpolate into the embed URL.
 */
function resolveYoutubeVideoId(value: string): string | undefined {
    if (YOUTUBE_VIDEO_ID_REGEX.test(value)) {
        return value;
    }

    let url: URL;

    try {
        url = new URL(value);
    } catch {
        return undefined;
    }

    const hostname = url.hostname.replace(/^www\./, '');

    let candidateVideoId: string | null = null;

    if (hostname === 'youtu.be') {
        candidateVideoId = url.pathname.slice(1);
    } else if (hostname === 'youtube.com' || hostname === 'm.youtube.com' || hostname === 'youtube-nocookie.com') {
        if (url.pathname === '/watch') {
            candidateVideoId = url.searchParams.get('v');
        } else if (url.pathname.startsWith('/embed/') || url.pathname.startsWith('/shorts/')) {
            candidateVideoId = url.pathname.split('/')[2];
        }
    }

    return candidateVideoId && YOUTUBE_VIDEO_ID_REGEX.test(candidateVideoId) ? candidateVideoId : undefined;
}

/**
 * Splits note content into markdown and YouTube-embed segments. `@[youtube](id-or-url)`
 * markers (n8n sticky note syntax) become embed segments; everything else stays
 * markdown. Markers with unresolvable video ids are left in the markdown text.
 */
export function splitStickyNoteContent(content: string): Array<StickyNoteContentSegmentType> {
    const segments: Array<StickyNoteContentSegmentType> = [];

    let lastIndex = 0;

    for (const match of content.matchAll(YOUTUBE_MARKER_REGEX)) {
        const videoId = resolveYoutubeVideoId(match[1]);

        if (!videoId) {
            continue;
        }

        const precedingText = content.slice(lastIndex, match.index);

        if (precedingText.trim()) {
            segments.push({type: 'markdown', value: precedingText});
        }

        segments.push({type: 'youtube', videoId});

        lastIndex = match.index + match[0].length;
    }

    const remainingText = content.slice(lastIndex);

    if (remainingText.trim()) {
        segments.push({type: 'markdown', value: remainingText});
    }

    if (segments.length === 0 && content) {
        segments.push({type: 'markdown', value: content});
    }

    return segments;
}

/**
 * Parses the sticky notes out of a workflow definition JSON string. Notes are
 * decorative canvas annotations stored under the workflow-level `metadata.ui`
 * key — the same convention as per-task `metadata.ui.nodePosition` — which both
 * client save paths and the server round-trip intact.
 */
export function extractStickyNotes(definition?: string): Array<WorkflowStickyNoteType> {
    if (!definition) {
        return [];
    }

    try {
        const parsedDefinition = JSON.parse(definition);

        const stickyNotes = parsedDefinition.metadata?.ui?.stickyNotes;

        if (!Array.isArray(stickyNotes)) {
            return [];
        }

        return stickyNotes.filter(
            (stickyNote: WorkflowStickyNoteType) =>
                stickyNote &&
                typeof stickyNote.id === 'string' &&
                typeof stickyNote.position?.x === 'number' &&
                typeof stickyNote.position?.y === 'number'
        );
    } catch {
        return [];
    }
}

/**
 * Builds a ReactFlow node for a sticky note. Stored positions are canonical;
 * rendering adds the saved-position cross-axis shift, mirroring how
 * `applySavedPositions` treats pinned task nodes.
 */
export function buildStickyNoteNode({
    crossAxis,
    crossAxisShift,
    readOnly,
    stickyNote,
}: {
    crossAxis: 'x' | 'y';
    crossAxisShift: number;
    readOnly: boolean;
    stickyNote: WorkflowStickyNoteType;
}): Node {
    const nodeData: StickyNoteNodeDataType = {
        color: stickyNote.color ?? 'yellow',
        content: stickyNote.content,
        readOnly,
    };

    return {
        data: nodeData,
        draggable: !readOnly,
        height: stickyNote.size?.height ?? STICKY_NOTE_DEFAULT_HEIGHT,
        id: stickyNote.id,
        position: {
            x: stickyNote.position.x + (crossAxis === 'x' ? crossAxisShift : 0),
            y: stickyNote.position.y + (crossAxis === 'y' ? crossAxisShift : 0),
        },
        selectable: !readOnly,
        type: STICKY_NOTE_NODE_TYPE,
        width: stickyNote.size?.width ?? STICKY_NOTE_DEFAULT_WIDTH,
        // Below tasks and edges so an overlapping note never blocks workflow interaction
        zIndex: -1,
    };
}

export function buildStickyNoteNodes({
    crossAxis,
    crossAxisShift,
    definition,
    readOnly,
}: {
    crossAxis: 'x' | 'y';
    crossAxisShift: number;
    definition?: string;
    readOnly: boolean;
}): Array<Node> {
    return extractStickyNotes(definition).map((stickyNote) =>
        buildStickyNoteNode({crossAxis, crossAxisShift, readOnly, stickyNote})
    );
}

/**
 * Converts a canvas position back to the canonical stored position by removing
 * the current cross-axis shift (the inverse of what `buildStickyNoteNode` adds),
 * mirroring `handleNodeDragStop`'s pre-compensation for task nodes.
 */
export function compensateStickyNotePosition(position: {x: number; y: number}): {x: number; y: number} {
    const savedPositionCrossAxisShift = useWorkflowDataStore.getState().savedPositionCrossAxisShift;
    const layoutDirection = useLayoutDirectionStore.getState().layoutDirection;

    const crossAxis = layoutDirection === 'TB' ? 'x' : 'y';

    return {
        ...position,
        [crossAxis]: position[crossAxis] - savedPositionCrossAxisShift,
    };
}

export function createStickyNote(position: {x: number; y: number}): WorkflowStickyNoteType {
    return {
        color: 'yellow',
        content: '',
        id: `stickyNote_${crypto.randomUUID()}`,
        position,
        size: {height: STICKY_NOTE_DEFAULT_HEIGHT, width: STICKY_NOTE_DEFAULT_WIDTH},
    };
}

interface SaveStickyNotesProps {
    updateWorkflowMutation: UpdateWorkflowMutationType;
    updater: (stickyNotes: Array<WorkflowStickyNoteType>) => Array<WorkflowStickyNoteType>;
}

/**
 * Single funnel for all sticky note mutations: applies `updater` to the
 * definition's `metadata.ui.stickyNotes` array, records the change as an undo
 * step through the store, and persists it honoring the pending-definition queue
 * so overlapping decorative saves are never lost.
 */
export function saveStickyNotes({updateWorkflowMutation, updater}: SaveStickyNotesProps) {
    const {workflow} = useWorkflowDataStore.getState();

    if (!workflow.definition) {
        return;
    }

    let workflowDefinition;

    try {
        workflowDefinition = JSON.parse(workflow.definition);
    } catch (error) {
        console.error('Failed to parse workflow definition:', error);

        return;
    }

    const currentStickyNotes: Array<WorkflowStickyNoteType> = Array.isArray(
        workflowDefinition.metadata?.ui?.stickyNotes
    )
        ? workflowDefinition.metadata.ui.stickyNotes
        : [];

    const updatedStickyNotes = updater(currentStickyNotes);

    if (updatedStickyNotes.length > 0) {
        workflowDefinition.metadata = {
            ...workflowDefinition.metadata,
            ui: {
                ...workflowDefinition.metadata?.ui,
                stickyNotes: updatedStickyNotes,
            },
        };
    } else if (workflowDefinition.metadata?.ui?.stickyNotes) {
        // Prune empty containers so a note-less definition carries no metadata residue
        delete workflowDefinition.metadata.ui.stickyNotes;

        if (Object.keys(workflowDefinition.metadata.ui).length === 0) {
            delete workflowDefinition.metadata.ui;
        }

        if (Object.keys(workflowDefinition.metadata).length === 0) {
            delete workflowDefinition.metadata;
        }
    }

    const updatedDefinition = JSON.stringify(workflowDefinition, null, SPACE);

    if (updatedDefinition === workflow.definition) {
        return;
    }

    const previousDefinition = workflow.definition;

    // Recorded as an undo step by the temporal store's definition equality
    useWorkflowDataStore.setState((state) => ({
        workflow: {
            ...state.workflow,
            definition: updatedDefinition,
        },
    }));

    if (isWorkflowMutating(workflow.id!)) {
        // Queue the definition so it can be sent when the current mutation settles
        setPendingDefinition(workflow.id!, updatedDefinition);

        return;
    }

    fireStickyNoteMutation({
        definition: updatedDefinition,
        previousDefinition,
        updateWorkflowMutation,
        version: workflow.version,
        workflowId: workflow.id!,
    });
}

interface UpdateStickyNoteProps {
    id: string;
    patch: Partial<Omit<WorkflowStickyNoteType, 'id'>>;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

export function updateStickyNote({id, patch, updateWorkflowMutation}: UpdateStickyNoteProps) {
    saveStickyNotes({
        updateWorkflowMutation,
        updater: (stickyNotes) =>
            stickyNotes.map((stickyNote) => (stickyNote.id === id ? {...stickyNote, ...patch} : stickyNote)),
    });
}

export function deleteStickyNote({
    id,
    updateWorkflowMutation,
}: {
    id: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}) {
    saveStickyNotes({
        updateWorkflowMutation,
        updater: (stickyNotes) => stickyNotes.filter((stickyNote) => stickyNote.id !== id),
    });
}

export function addStickyNote({
    position,
    updateWorkflowMutation,
}: {
    position: {x: number; y: number};
    updateWorkflowMutation: UpdateWorkflowMutationType;
}) {
    saveStickyNotes({
        updateWorkflowMutation,
        updater: (stickyNotes) => [...stickyNotes, createStickyNote(position)],
    });
}

interface FireStickyNoteMutationProps {
    definition: string;
    previousDefinition: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    version?: number;
    workflowId: string;
}

function fireStickyNoteMutation({
    definition,
    previousDefinition,
    updateWorkflowMutation,
    version,
    workflowId,
}: FireStickyNoteMutationProps) {
    setWorkflowMutating(workflowId, true);

    updateWorkflowMutation.mutate(
        {
            id: workflowId,
            workflow: {
                definition,
                version,
            },
        },
        {
            onError: () => {
                // Reverting a failed decorative save must not become an undo step
                runWithoutHistory(() => {
                    useWorkflowDataStore.setState((state) => ({
                        workflow: {
                            ...state.workflow,
                            definition: previousDefinition,
                        },
                    }));
                });
            },
            onSettled: () => {
                setWorkflowMutating(workflowId, false);

                // If another decorative save was skipped while this mutation was
                // in flight, fire it now with the latest queued definition.
                const pendingDefinition = consumePendingDefinition(workflowId);

                if (pendingDefinition) {
                    const currentWorkflow = useWorkflowDataStore.getState().workflow;

                    fireStickyNoteMutation({
                        definition: pendingDefinition,
                        previousDefinition: currentWorkflow.definition!,
                        updateWorkflowMutation,
                        version: currentWorkflow.version,
                        workflowId,
                    });
                }
            },
            onSuccess: (updatedWorkflow) => {
                const currentWorkflow = useWorkflowDataStore.getState().workflow;

                useWorkflowDataStore.getState().setWorkflow({
                    ...currentWorkflow,
                    version: updatedWorkflow.version,
                });
            },
        }
    );
}
