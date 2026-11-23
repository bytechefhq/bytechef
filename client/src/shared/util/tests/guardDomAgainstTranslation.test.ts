import {guardDomAgainstTranslation} from '@/shared/util/guardDomAgainstTranslation';
import {beforeAll, describe, expect, it, vi} from 'vitest';

describe('guardDomAgainstTranslation', () => {
    beforeAll(() => {
        guardDomAgainstTranslation();

        vi.spyOn(console, 'warn').mockImplementation(() => {});
    });

    it('removes a child that is still under its parent', () => {
        const parent = document.createElement('div');
        const child = parent.appendChild(document.createTextNode('text'));

        parent.removeChild(child);

        expect(child.parentNode).toBeNull();
    });

    it('does not throw when a translator moved the child to another parent', () => {
        const parent = document.createElement('div');
        const translatorWrapper = document.createElement('font');
        const child = parent.appendChild(document.createTextNode('text'));

        translatorWrapper.appendChild(child);
        parent.appendChild(translatorWrapper);

        expect(() => parent.removeChild(child)).not.toThrow();
        expect(child.parentNode).toBe(translatorWrapper);
    });

    it('inserts before a reference node that is still under its parent', () => {
        const parent = document.createElement('div');
        const referenceNode = parent.appendChild(document.createTextNode('reference'));
        const newNode = document.createElement('span');

        parent.insertBefore(newNode, referenceNode);

        expect(parent.firstChild).toBe(newNode);
    });

    it('appends when the reference node is null', () => {
        const parent = document.createElement('div');
        const newNode = document.createElement('span');

        parent.insertBefore(newNode, null);

        expect(parent.lastChild).toBe(newNode);
    });

    it('does not throw when a translator moved the reference node to another parent', () => {
        const parent = document.createElement('div');
        const translatorWrapper = document.createElement('font');
        const referenceNode = parent.appendChild(document.createTextNode('reference'));

        translatorWrapper.appendChild(referenceNode);
        parent.appendChild(translatorWrapper);

        expect(() => parent.insertBefore(document.createElement('span'), referenceNode)).not.toThrow();
    });
});
