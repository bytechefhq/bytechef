import {SPACE} from '@/shared/constants';
import {UpdateWorkflowMutationType, WorkflowStickyNoteType} from '@/shared/types';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {
    STICKY_NOTE_DEFAULT_HEIGHT,
    STICKY_NOTE_DEFAULT_WIDTH,
    addStickyNote,
    buildStickyNoteNode,
    buildStickyNoteNodes,
    compensateStickyNotePosition,
    createStickyNote,
    deleteStickyNote,
    extractStickyNotes,
    isDarkHexColor,
    normalizeHexColor,
    saveStickyNotes,
    splitStickyNoteContent,
    updateStickyNote,
} from './stickyNoteUtils';
import {
    clearAllWorkflowMutations,
    drainPendingSaves,
    enqueuePendingSave,
    hasPendingSaves,
    isWorkflowMutating,
    setPendingDefinition,
    setWorkflowMutating,
} from './workflowMutationGuard';

function makeStickyNote(overrides: Partial<WorkflowStickyNoteType> = {}): WorkflowStickyNoteType {
    return {
        color: 'yellow',
        content: 'A note',
        id: 'stickyNote_1',
        position: {x: 100, y: 200},
        ...overrides,
    };
}

function makeDefinition(stickyNotes?: Array<WorkflowStickyNoteType>): string {
    return JSON.stringify(
        {
            label: 'Test Workflow',
            ...(stickyNotes ? {metadata: {ui: {stickyNotes}}} : {}),
            tasks: [],
        },
        null,
        SPACE
    );
}

describe('extractStickyNotes', () => {
    it('should return an empty array for a missing definition', () => {
        expect(extractStickyNotes(undefined)).toEqual([]);
    });

    it('should return an empty array for invalid JSON', () => {
        expect(extractStickyNotes('{not json')).toEqual([]);
    });

    it('should return an empty array when the definition has no sticky notes metadata', () => {
        expect(extractStickyNotes(makeDefinition())).toEqual([]);
    });

    it('should return sticky notes from the definition', () => {
        const stickyNote = makeStickyNote();

        expect(extractStickyNotes(makeDefinition([stickyNote]))).toEqual([stickyNote]);
    });

    it('should filter out malformed entries', () => {
        const definition = JSON.stringify({
            metadata: {
                ui: {
                    stickyNotes: [
                        makeStickyNote(),
                        {content: 'missing id and position'},
                        null,
                        {id: 'x', position: {}},
                    ],
                },
            },
        });

        expect(extractStickyNotes(definition)).toEqual([makeStickyNote()]);
    });

    it('should filter out entries with missing content or mistyped optional fields', () => {
        const definition = JSON.stringify({
            metadata: {
                ui: {
                    stickyNotes: [
                        {id: 'noContent', position: {x: 0, y: 0}},
                        makeStickyNote({content: 42 as unknown as string, id: 'numberContent'}),
                        makeStickyNote({color: 7 as unknown as string, id: 'numberColor'}),
                        makeStickyNote({id: 'partialSize', size: {width: 200} as {height: number; width: number}}),
                        makeStickyNote({id: 'valid', size: {height: 120, width: 200}}),
                    ],
                },
            },
        });

        expect(extractStickyNotes(definition).map((stickyNote) => stickyNote.id)).toEqual(['valid']);
    });
});

describe('buildStickyNoteNode', () => {
    it('should apply the cross-axis shift on x for top-to-bottom layout', () => {
        const node = buildStickyNoteNode({
            crossAxis: 'x',
            crossAxisShift: 50,
            readOnly: false,
            stickyNote: makeStickyNote(),
        });

        expect(node.position).toEqual({x: 150, y: 200});
    });

    it('should apply the cross-axis shift on y for left-to-right layout', () => {
        const node = buildStickyNoteNode({
            crossAxis: 'y',
            crossAxisShift: 50,
            readOnly: false,
            stickyNote: makeStickyNote(),
        });

        expect(node.position).toEqual({x: 100, y: 250});
    });

    it('should default size and color when absent', () => {
        const node = buildStickyNoteNode({
            crossAxis: 'x',
            crossAxisShift: 0,
            readOnly: false,
            stickyNote: makeStickyNote({color: undefined}),
        });

        expect(node.width).toBe(STICKY_NOTE_DEFAULT_WIDTH);
        expect(node.height).toBe(STICKY_NOTE_DEFAULT_HEIGHT);
        expect(node.data.color).toBe('yellow');
    });

    it('should use the stored size when present', () => {
        const node = buildStickyNoteNode({
            crossAxis: 'x',
            crossAxisShift: 0,
            readOnly: false,
            stickyNote: makeStickyNote({size: {height: 300, width: 400}}),
        });

        expect(node.width).toBe(400);
        expect(node.height).toBe(300);
    });

    it('should disable dragging and selection in read-only mode', () => {
        const node = buildStickyNoteNode({
            crossAxis: 'x',
            crossAxisShift: 0,
            readOnly: true,
            stickyNote: makeStickyNote(),
        });

        expect(node.draggable).toBe(false);
        expect(node.selectable).toBe(false);
        expect(node.data.readOnly).toBe(true);
    });

    it('should render below task nodes and edges', () => {
        const node = buildStickyNoteNode({
            crossAxis: 'x',
            crossAxisShift: 0,
            readOnly: false,
            stickyNote: makeStickyNote(),
        });

        expect(node.zIndex).toBe(-1);
        expect(node.type).toBe('stickyNote');
    });
});

describe('buildStickyNoteNodes', () => {
    it('should build one node per sticky note in the definition', () => {
        const definition = makeDefinition([makeStickyNote(), makeStickyNote({id: 'stickyNote_2'})]);

        const nodes = buildStickyNoteNodes({crossAxis: 'x', crossAxisShift: 0, definition, readOnly: false});

        expect(nodes.map((node) => node.id)).toEqual(['stickyNote_1', 'stickyNote_2']);
    });
});

describe('createStickyNote', () => {
    it('should create an empty yellow note with a unique prefixed id', () => {
        const firstStickyNote = createStickyNote({x: 1, y: 2});
        const secondStickyNote = createStickyNote({x: 1, y: 2});

        expect(firstStickyNote.id).toMatch(/^stickyNote_/);
        expect(firstStickyNote.id).not.toBe(secondStickyNote.id);
        expect(firstStickyNote.color).toBe('yellow');
        expect(firstStickyNote.content).toBe('');
        expect(firstStickyNote.position).toEqual({x: 1, y: 2});
        expect(firstStickyNote.size).toEqual({
            height: STICKY_NOTE_DEFAULT_HEIGHT,
            width: STICKY_NOTE_DEFAULT_WIDTH,
        });
    });
});

describe('splitStickyNoteContent', () => {
    it('should return plain content as a single markdown segment', () => {
        expect(splitStickyNoteContent('Just **markdown**')).toEqual([{type: 'markdown', value: 'Just **markdown**'}]);
    });

    it('should turn a youtube marker with a video id into an embed segment', () => {
        expect(splitStickyNoteContent('@[youtube](dQw4w9WgXcQ)')).toEqual([{type: 'youtube', videoId: 'dQw4w9WgXcQ'}]);
    });

    it('should split mixed content around the marker', () => {
        expect(splitStickyNoteContent('Intro\n\n@[youtube](dQw4w9WgXcQ)\n\nOutro')).toEqual([
            {type: 'markdown', value: 'Intro\n\n'},
            {type: 'youtube', videoId: 'dQw4w9WgXcQ'},
            {type: 'markdown', value: '\n\nOutro'},
        ]);
    });

    it('should resolve video ids from common youtube url forms', () => {
        for (const url of [
            'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
            'https://youtu.be/dQw4w9WgXcQ',
            'https://www.youtube.com/embed/dQw4w9WgXcQ',
            'https://www.youtube.com/shorts/dQw4w9WgXcQ',
        ]) {
            expect(splitStickyNoteContent(`@[youtube](${url})`)).toEqual([{type: 'youtube', videoId: 'dQw4w9WgXcQ'}]);
        }
    });

    it('should leave markers with unresolvable video ids in the markdown text', () => {
        expect(splitStickyNoteContent('@[youtube](https://example.com/nope)')).toEqual([
            {type: 'markdown', value: '@[youtube](https://example.com/nope)'},
        ]);
    });

    it('should reject video ids with unsafe characters', () => {
        expect(splitStickyNoteContent('@[youtube](abc"def<script)')).toEqual([
            {type: 'markdown', value: '@[youtube](abc"def<script)'},
        ]);
    });
});

describe('normalizeHexColor', () => {
    it('should accept a 6-digit hex color with or without a leading hash', () => {
        expect(normalizeHexColor('#A1B2C3')).toBe('#a1b2c3');
        expect(normalizeHexColor('a1b2c3')).toBe('#a1b2c3');
    });

    it('should reject invalid values', () => {
        expect(normalizeHexColor('#abc')).toBeUndefined();
        expect(normalizeHexColor('#12345g')).toBeUndefined();
        expect(normalizeHexColor('yellow')).toBeUndefined();
        expect(normalizeHexColor('')).toBeUndefined();
    });
});

describe('isDarkHexColor', () => {
    it('should classify dark colors as dark', () => {
        expect(isDarkHexColor('#000000')).toBe(true);
        expect(isDarkHexColor('#123456')).toBe(true);
    });

    it('should classify light colors as light', () => {
        expect(isDarkHexColor('#ffffff')).toBe(false);
        expect(isDarkHexColor('#ffd97a')).toBe(false);
    });

    it('should expand 3-digit hex colors', () => {
        expect(isDarkHexColor('#000')).toBe(true);
        expect(isDarkHexColor('#fff')).toBe(false);
    });

    it('should treat invalid values as light', () => {
        expect(isDarkHexColor('not-a-color')).toBe(false);
    });
});

describe('compensateStickyNotePosition', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({savedPositionCrossAxisShift: 40});
    });

    it('should subtract the shift from x in top-to-bottom layout', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'TB'});

        expect(compensateStickyNotePosition({x: 100, y: 200})).toEqual({x: 60, y: 200});
    });

    it('should subtract the shift from y in left-to-right layout', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'LR'});

        expect(compensateStickyNotePosition({x: 100, y: 200})).toEqual({x: 100, y: 160});
    });
});

describe('saveStickyNotes', () => {
    const workflowId = 'workflow_1';

    let mutateMock: ReturnType<typeof vi.fn>;
    let updateWorkflowMutation: UpdateWorkflowMutationType;

    beforeEach(() => {
        clearAllWorkflowMutations();

        mutateMock = vi.fn();
        updateWorkflowMutation = {mutate: mutateMock} as unknown as UpdateWorkflowMutationType;

        useWorkflowDataStore.setState((state) => ({
            workflow: {
                ...state.workflow,
                definition: makeDefinition(),
                id: workflowId,
                version: 3,
            },
        }));
    });

    it('should write sticky notes under metadata.ui and fire the mutation', () => {
        saveStickyNotes({
            updateWorkflowMutation,
            updater: (stickyNotes) => [...stickyNotes, makeStickyNote()],
        });

        const storedDefinition = useWorkflowDataStore.getState().workflow.definition!;

        expect(JSON.parse(storedDefinition).metadata.ui.stickyNotes).toEqual([makeStickyNote()]);

        expect(mutateMock).toHaveBeenCalledTimes(1);
        expect(mutateMock.mock.calls[0][0]).toEqual({
            id: workflowId,
            workflow: {
                definition: storedDefinition,
                version: 3,
            },
        });
    });

    it('should prune the empty metadata containers when the last note is deleted', () => {
        useWorkflowDataStore.setState((state) => ({
            workflow: {
                ...state.workflow,
                definition: makeDefinition([makeStickyNote()]),
            },
        }));

        saveStickyNotes({
            updateWorkflowMutation,
            updater: () => [],
        });

        const storedDefinition = useWorkflowDataStore.getState().workflow.definition!;

        expect('metadata' in JSON.parse(storedDefinition)).toBe(false);
        expect(mutateMock).toHaveBeenCalledTimes(1);
    });

    it('should preserve unrelated metadata when the last note is deleted', () => {
        const definitionWithExtraMetadata = JSON.stringify(
            {
                label: 'Test Workflow',
                metadata: {other: 'keep', ui: {stickyNotes: [makeStickyNote()], theme: 'dark'}},
                tasks: [],
            },
            null,
            SPACE
        );

        useWorkflowDataStore.setState((state) => ({
            workflow: {
                ...state.workflow,
                definition: definitionWithExtraMetadata,
            },
        }));

        saveStickyNotes({
            updateWorkflowMutation,
            updater: () => [],
        });

        const parsedDefinition = JSON.parse(useWorkflowDataStore.getState().workflow.definition!);

        expect(parsedDefinition.metadata).toEqual({other: 'keep', ui: {theme: 'dark'}});
    });

    it('should not fire a mutation when the updater changes nothing', () => {
        useWorkflowDataStore.setState((state) => ({
            workflow: {
                ...state.workflow,
                definition: makeDefinition([makeStickyNote()]),
            },
        }));

        saveStickyNotes({
            updateWorkflowMutation,
            updater: (stickyNotes) => stickyNotes,
        });

        expect(mutateMock).not.toHaveBeenCalled();
    });

    it('should wait in the shared save queue while another mutation is in flight', () => {
        setWorkflowMutating(workflowId, true);

        const definitionBeforeSave = useWorkflowDataStore.getState().workflow.definition;

        saveStickyNotes({
            updateWorkflowMutation,
            updater: (stickyNotes) => [...stickyNotes, makeStickyNote()],
        });

        expect(mutateMock).not.toHaveBeenCalled();
        expect(useWorkflowDataStore.getState().workflow.definition).toBe(definitionBeforeSave);
        expect(hasPendingSaves(workflowId)).toBe(true);

        setWorkflowMutating(workflowId, false);

        drainPendingSaves(workflowId);

        const storedDefinition = useWorkflowDataStore.getState().workflow.definition!;

        expect(JSON.parse(storedDefinition).metadata.ui.stickyNotes).toEqual([makeStickyNote()]);
        expect(mutateMock).toHaveBeenCalledTimes(1);
        expect(mutateMock.mock.calls[0][0].workflow.definition).toBe(storedDefinition);
    });

    it('should apply a queued note change on top of the definition saved in the meantime', () => {
        setWorkflowMutating(workflowId, true);

        saveStickyNotes({
            updateWorkflowMutation,
            updater: (stickyNotes) => [...stickyNotes, makeStickyNote()],
        });

        useWorkflowDataStore.setState((state) => ({
            workflow: {
                ...state.workflow,
                definition: JSON.stringify({label: 'Test Workflow', tasks: [{name: 'task_1'}]}, null, SPACE),
            },
        }));

        setWorkflowMutating(workflowId, false);

        drainPendingSaves(workflowId);

        const parsedDefinition = JSON.parse(useWorkflowDataStore.getState().workflow.definition!);

        expect(parsedDefinition.tasks).toEqual([{name: 'task_1'}]);
        expect(parsedDefinition.metadata.ui.stickyNotes).toEqual([makeStickyNote()]);
    });

    it('should run saves queued behind the note mutation once it settles', () => {
        const queuedSave = vi.fn();

        saveStickyNotes({
            updateWorkflowMutation,
            updater: (stickyNotes) => [...stickyNotes, makeStickyNote()],
        });

        expect(isWorkflowMutating(workflowId)).toBe(true);

        enqueuePendingSave(workflowId, queuedSave);

        mutateMock.mock.calls[0][1].onSettled();

        expect(isWorkflowMutating(workflowId)).toBe(false);
        expect(queuedSave).toHaveBeenCalledTimes(1);
    });

    it('should skip the save when the definition cannot be parsed', () => {
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

        useWorkflowDataStore.setState((state) => ({workflow: {...state.workflow, definition: '{not json'}}));

        saveStickyNotes({updateWorkflowMutation, updater: (stickyNotes) => [...stickyNotes, makeStickyNote()]});

        expect(mutateMock).not.toHaveBeenCalled();
        expect(consoleErrorSpy).toHaveBeenCalled();

        consoleErrorSpy.mockRestore();
    });

    it('should patch, remove and append notes through the named helpers', () => {
        useWorkflowDataStore.setState((state) => ({
            workflow: {...state.workflow, definition: makeDefinition([makeStickyNote()])},
        }));

        const settleLastMutation = () => mutateMock.mock.calls.at(-1)![1].onSettled();

        updateStickyNote({id: 'stickyNote_1', patch: {content: 'Patched'}, updateWorkflowMutation});

        expect(extractStickyNotes(useWorkflowDataStore.getState().workflow.definition)[0].content).toBe('Patched');

        settleLastMutation();

        addStickyNote({position: {x: 10, y: 20}, updateWorkflowMutation});

        const afterAdd = extractStickyNotes(useWorkflowDataStore.getState().workflow.definition);

        expect(afterAdd).toHaveLength(2);
        expect(afterAdd[1].position).toEqual({x: 10, y: 20});

        settleLastMutation();

        deleteStickyNote({id: 'stickyNote_1', updateWorkflowMutation});

        expect(extractStickyNotes(useWorkflowDataStore.getState().workflow.definition).map((note) => note.id)).toEqual([
            afterAdd[1].id,
        ]);
    });

    it('should roll the definition back when the save fails', () => {
        const definitionBeforeSave = useWorkflowDataStore.getState().workflow.definition;

        saveStickyNotes({updateWorkflowMutation, updater: (stickyNotes) => [...stickyNotes, makeStickyNote()]});

        mutateMock.mock.calls[0][1].onError();

        expect(useWorkflowDataStore.getState().workflow.definition).toBe(definitionBeforeSave);
    });

    it('should keep a newer note edit when an earlier save fails', () => {
        saveStickyNotes({updateWorkflowMutation, updater: (stickyNotes) => [...stickyNotes, makeStickyNote()]});

        const newerDefinition = makeDefinition([makeStickyNote({content: 'Newer'})]);

        useWorkflowDataStore.setState((state) => ({workflow: {...state.workflow, definition: newerDefinition}}));

        mutateMock.mock.calls[0][1].onError();

        expect(useWorkflowDataStore.getState().workflow.definition).toBe(newerDefinition);
    });

    it('should store the version returned by a successful save', () => {
        saveStickyNotes({updateWorkflowMutation, updater: (stickyNotes) => [...stickyNotes, makeStickyNote()]});

        mutateMock.mock.calls[0][1].onSuccess({version: 7});

        expect(useWorkflowDataStore.getState().workflow.version).toBe(7);
    });

    it('should roll a failed retry back to the last definition the server confirmed', () => {
        saveStickyNotes({updateWorkflowMutation, updater: (stickyNotes) => [...stickyNotes, makeStickyNote()]});

        const confirmedDefinition = useWorkflowDataStore.getState().workflow.definition;

        mutateMock.mock.calls[0][1].onSuccess({version: 7});

        setPendingDefinition(workflowId, makeDefinition([makeStickyNote({position: {x: 5, y: 5}})]));

        mutateMock.mock.calls[0][1].onSettled();

        const retriedDefinition = mutateMock.mock.calls[1][0].workflow.definition;

        useWorkflowDataStore.setState((state) => ({workflow: {...state.workflow, definition: retriedDefinition}}));

        mutateMock.mock.calls[1][1].onError();

        expect(useWorkflowDataStore.getState().workflow.definition).toBe(confirmedDefinition);
    });

    it('should send a queued position definition before draining other saves', () => {
        const queuedSave = vi.fn();

        saveStickyNotes({
            updateWorkflowMutation,
            updater: (stickyNotes) => [...stickyNotes, makeStickyNote()],
        });

        setPendingDefinition(workflowId, makeDefinition([makeStickyNote({position: {x: 5, y: 5}})]));

        enqueuePendingSave(workflowId, queuedSave);

        mutateMock.mock.calls[0][1].onSettled();

        expect(mutateMock).toHaveBeenCalledTimes(2);
        expect(queuedSave).not.toHaveBeenCalled();

        mutateMock.mock.calls[1][1].onSettled();

        expect(queuedSave).toHaveBeenCalledTimes(1);
    });
});
