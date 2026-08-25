import {Workflow} from '@/shared/middleware/platform/configuration';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {renderToStaticMarkup} from 'react-dom/server';
import {describe, expect, it} from 'vitest';

import {getDataPillIconSource} from './getDataPillIconSource';

const workflow = {} as Workflow;

const variableIconDataUri = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(
    renderToStaticMarkup(TYPE_ICONS.VARIABLE)
)}`;
const defaultStringIconDataUri = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(
    renderToStaticMarkup(TYPE_ICONS.STRING)
)}`;

describe('getDataPillIconSource', () => {
    it('returns the variable icon for a raw vars.NAME mention display', () => {
        expect(getDataPillIconSource({mentionDisplay: 'vars.API_URL', workflow})).toBe(variableIconDataUri);
    });

    it('returns the variable icon for a wrapped ${vars.NAME} mention display', () => {
        expect(getDataPillIconSource({mentionDisplay: '${vars.API_URL}', workflow})).toBe(variableIconDataUri);
    });

    it('returns the variable icon for the bare vars node name', () => {
        expect(getDataPillIconSource({mentionDisplay: 'vars', workflow})).toBe(variableIconDataUri);
    });

    it('does not treat a component name merely starting with "vars" as the variables node', () => {
        // A hypothetical component pill 'varsomething_1_action' must fall through to the default icon, not be
        // misidentified as a variable.
        expect(getDataPillIconSource({mentionDisplay: 'varsomething_1_action', workflow})).toBe(
            defaultStringIconDataUri
        );
    });
});
