import {Edge} from '@xyflow/react';

import {FINAL_PLACEHOLDER_NODE_ID} from './constants';

const defaultEdges: Edge[] = [
    {
        id: `manual=>${FINAL_PLACEHOLDER_NODE_ID}`,
        source: 'manual',
        target: FINAL_PLACEHOLDER_NODE_ID,
        type: 'placeholder',
    },
];

export default defaultEdges;
