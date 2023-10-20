import {PencilIcon, TrashIcon} from '@heroicons/react/24/outline';
import Button from 'components/Button/Button';
import {memo, useState} from 'react';
import {
    Handle,
    NodeProps,
    Position,
    getConnectedEdges,
    useReactFlow,
} from 'reactflow';
import {twMerge} from 'tailwind-merge';

import EditNodeDialog from '../components/EditNodeDialog';
import useNodeClickHandler from '../hooks/useNodeClick';
import styles from './NodeTypes.module.css';

const WorkflowNode = ({id, data}: NodeProps) => {
    const [showEditNodeDialog, setShowEditNodeDialog] = useState(false);
    const [isHovered, setIsHovered] = useState(false);

    const handleNodeClick = useNodeClickHandler(data, id);

    const {getEdges, getNode, getNodes, setNodes, setEdges} = useReactFlow();

    const handleDeleteNodeClick = () => {
        const nodes = getNodes();
        const node = getNode(id);

        if (!node) {
            return;
        }

        const edges = getEdges();

        setNodes((nodes) => nodes.filter((node) => node.id !== id));

        const connectedEdges = getConnectedEdges([node], edges);

        const previousNode = nodes.find(
            (node) => node.id === connectedEdges[0].source
        );

        const nextNode = nodes.find(
            (node) => node.id === connectedEdges[1].target
        );

        if (previousNode && nextNode) {
            const connectedEdgeIds = connectedEdges.map((edge) => edge.id);

            setEdges((edges) => {
                const leftoverEdges = edges.filter(
                    (edge) => !connectedEdgeIds.includes(edge.id)
                );

                return [
                    ...leftoverEdges,
                    {
                        id: `${previousNode.id}=>${nextNode.id}`,
                        source: previousNode.id,
                        target: nextNode.id,
                        type: 'workflow',
                    },
                ];
            });
        }
    };

    const handleEditNodeClick = () => {
        setShowEditNodeDialog(true);
    };

    const nodes = getNodes();

    let isFirstNode;

    if (nodes[0].id === id) {
        isFirstNode = true;
    }

    return (
        <div
            className="relative flex min-w-[240px] cursor-pointer items-center justify-center"
            onMouseOver={() => setIsHovered(true)}
            onMouseOut={() => setIsHovered(false)}
        >
            {!isFirstNode && !!isHovered && (
                <div className="absolute left-[-46px] pr-4">
                    {data.type === 'trigger' ? (
                        <Button
                            className="bg-white p-2 shadow-md hover:text-blue-500 hover:shadow-sm"
                            displayType="icon"
                            icon={<PencilIcon className="h-4 w-4" />}
                            onClick={handleEditNodeClick}
                            title="Edit a node"
                        />
                    ) : (
                        <Button
                            className="bg-white p-2 shadow-md hover:text-red-500 hover:shadow-sm"
                            displayType="icon"
                            icon={<TrashIcon className="h-4 w-4" />}
                            onClick={handleDeleteNodeClick}
                            title="Delete a node"
                        />
                    )}
                </div>
            )}

            <Button
                className="rounded-md border-2 border-gray-300 bg-white p-4 shadow hover:bg-gray-200"
                displayType="icon"
                icon={data.icon}
                onClick={handleNodeClick}
            />

            <div className="ml-2 flex w-full min-w-max flex-col items-start">
                <span className="text-sm text-gray-900">{data.label}</span>

                <span className="text-xs text-gray-500">{data.name}</span>
            </div>

            {showEditNodeDialog && (
                <EditNodeDialog
                    onClose={() => setShowEditNodeDialog(false)}
                    visible={showEditNodeDialog}
                />
            )}

            <Handle
                className={twMerge('left-[36px]', styles.handle)}
                isConnectable={false}
                position={Position.Top}
                type="target"
            />

            <Handle
                className={twMerge('left-[36px]', styles.handle)}
                isConnectable={false}
                position={Position.Bottom}
                type="source"
            />
        </div>
    );
};

export default memo(WorkflowNode);
