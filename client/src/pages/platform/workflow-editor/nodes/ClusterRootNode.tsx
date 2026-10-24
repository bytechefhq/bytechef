import {NodeDataType} from '@/shared/types';
import {memo} from 'react';

import {useWorkflowEditorReadOnly} from '../providers/workflowEditorReadOnlyContext';
import AiAgentNode from './AiAgentNode';
import ReadOnlyClusterRootNode from './ReadOnlyClusterRootNode';
import WorkflowNode from './WorkflowNode';

const ClusterRootNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const readOnly = useWorkflowEditorReadOnly();

    if (!data.clusterFrame) {
        return <AiAgentNode data={data} id={id} />;
    }

    return readOnly ? <ReadOnlyClusterRootNode data={data} id={id} /> : <WorkflowNode data={data} id={id} />;
};

export default memo(ClusterRootNode);
