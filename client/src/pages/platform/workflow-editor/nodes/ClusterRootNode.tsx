import {NodeDataType} from '@/shared/types';
import {memo} from 'react';

import AiAgentNode from './AiAgentNode';
import WorkflowNode from './WorkflowNode';

const ClusterRootNode = ({data, id}: {data: NodeDataType; id: string}) =>
    data.clusterFrame ? <WorkflowNode data={data} id={id} /> : <AiAgentNode data={data} id={id} />;

export default memo(ClusterRootNode);
