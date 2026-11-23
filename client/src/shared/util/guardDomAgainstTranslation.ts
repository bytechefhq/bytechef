const guardedMethods = new WeakSet<object>();

export const guardDomAgainstTranslation = (nodePrototype: Node = Node.prototype) => {
    if (guardedMethods.has(nodePrototype.removeChild) && guardedMethods.has(nodePrototype.insertBefore)) {
        return;
    }

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
            console.warn(
                'Appended a node whose insertBefore reference was moved outside React, for example by a translator'
            );

            return originalInsertBefore.call(this, newNode, null) as T;
        }

        return originalInsertBefore.call(this, newNode, referenceNode) as T;
    };

    guardedMethods.add(nodePrototype.removeChild);
    guardedMethods.add(nodePrototype.insertBefore);
};
