import {beforeEach, describe, expect, it} from 'vitest';

import {useResponseSchemaNoteStore} from '../useResponseSchemaNoteStore';

describe('useResponseSchemaNoteStore', () => {
    beforeEach(() => {
        localStorage.clear();

        useResponseSchemaNoteStore.setState({showResponseSchemaNote: true});
    });

    it('shows the note by default', () => {
        expect(useResponseSchemaNoteStore.getState().showResponseSchemaNote).toBe(true);
    });

    it('persists a dismissal to local storage', () => {
        useResponseSchemaNoteStore.getState().setShowResponseSchemaNote(false);

        expect(useResponseSchemaNoteStore.getState().showResponseSchemaNote).toBe(false);
        expect(JSON.parse(localStorage.getItem('bytechef.response-schema-note') ?? '{}').state).toEqual({
            showResponseSchemaNote: false,
        });
    });
});
