import {Node} from '@xyflow/react';

export default function shiftNodesHorizontally(nodes: Node[], shift: number): Node[] {
    return nodes.map((node) =>
        node.parentId
            ? node
            : {
                  ...node,
                  position: {
                      ...node.position,
                      x: node.position.x + shift,
                  },
              }
    );
}
