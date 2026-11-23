export const guardDomAgainstTranslation = (nodePrototype: Node = Node.prototype) => {
    const originalRemoveChild = nodePrototype.removeChild;
    const originalInsertBefore = nodePrototype.insertBefore;

    nodePrototype.removeChild = function <T extends Node>(this: Node, child: T): T {
        if (child.parentNode !== this) {
            console.warn('Skipped removeChild for a node that was moved outside React, for example by a translator');

            return child;
        }

        return originalRemoveChild.call(this, child) as T;
    };

    nodePrototype.insertBefore = function <T extends Node>(this: Node, newNode: T, referenceNode: Node | null): T {
        if (referenceNode && referenceNode.parentNode !== this) {
            console.warn('Skipped insertBefore for a node that was moved outside React, for example by a translator');

            return newNode;
        }

        return originalInsertBefore.call(this, newNode, referenceNode) as T;
    };
};
