import {guardDomAgainstTranslation} from '@/shared/util/guardDomAgainstTranslation';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const originalRemoveChild = Node.prototype.removeChild;
const originalInsertBefore = Node.prototype.insertBefore;

describe('guardDomAgainstTranslation', () => {
    let consoleWarnSpy: ReturnType<typeof vi.spyOn>;

    beforeEach(() => {
        consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

        guardDomAgainstTranslation();
    });

    afterEach(() => {
        Node.prototype.removeChild = originalRemoveChild;
        Node.prototype.insertBefore = originalInsertBefore;

        vi.restoreAllMocks();
    });

    it('removes a child that is still under its parent', () => {
        const parent = document.createElement('div');
        const child = parent.appendChild(document.createTextNode('text'));

        parent.removeChild(child);

        expect(child.parentNode).toBeNull();
        expect(consoleWarnSpy).not.toHaveBeenCalled();
    });

    it('skips removing a child that a translator moved to another parent', () => {
        const parent = document.createElement('div');
        const translatorWrapper = document.createElement('font');
        const child = parent.appendChild(document.createTextNode('text'));

        translatorWrapper.appendChild(child);
        parent.appendChild(translatorWrapper);

        expect(() => parent.removeChild(child)).not.toThrow();
        expect(child.parentNode).toBe(translatorWrapper);
        expect(consoleWarnSpy).toHaveBeenCalledTimes(1);
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

    it('appends the new node when a translator moved the reference node to another parent', () => {
        const parent = document.createElement('div');
        const translatorWrapper = document.createElement('font');
        const referenceNode = parent.appendChild(document.createTextNode('reference'));
        const newNode = document.createElement('span');

        translatorWrapper.appendChild(referenceNode);
        parent.appendChild(translatorWrapper);

        expect(() => parent.insertBefore(newNode, referenceNode)).not.toThrow();
        expect(newNode.parentNode).toBe(parent);
        expect(parent.lastChild).toBe(newNode);
        expect(consoleWarnSpy).toHaveBeenCalledTimes(1);
    });

    it('does not wrap the methods again when called a second time', () => {
        const guardedRemoveChild = Node.prototype.removeChild;
        const guardedInsertBefore = Node.prototype.insertBefore;

        guardDomAgainstTranslation();

        expect(Node.prototype.removeChild).toBe(guardedRemoveChild);
        expect(Node.prototype.insertBefore).toBe(guardedInsertBefore);
    });
});
