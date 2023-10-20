import {memo, useState} from 'react';
import {
    Handle,
    Position,
    NodeProps,
    useReactFlow,
    getConnectedEdges,
} from 'reactflow';
import styles from './NodeTypes.module.css';
import useNodeClickHandler from '../hooks/useNodeClick';
import Button from 'components/Button/Button';
import DropdownMenu from 'components/DropdownMenu/DropdownMenu';
import EditNodeDialog from '../components/EditNodeDialog';

const WorkflowNode = ({id, data}: NodeProps) => {
    const [showEditNodeDialog, setShowEditNodeDialog] = useState(false);

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

    return (
        <div
            className="relative flex cursor-pointer items-center justify-center rounded-md border-2 border-gray-300 bg-white shadow hover:bg-gray-200"
            title="Click to add a child node"
        >
            <div className="absolute left-[-32px]">
                <DropdownMenu
                    menuItems={[
                        {
                            label: 'Edit',
                            onClick: handleEditNodeClick,
                        },
                        {
                            separator: true,
                        },
                        {
                            danger: true,
                            label: 'Delete',
                            onClick: handleDeleteNodeClick,
                        },
                    ]}
                />
            </div>

            <Button
                className="p-4"
                displayType="icon"
                icon={data.icon}
                onClick={handleNodeClick}
            />

            <div className="absolute left-[80px] flex w-full min-w-max flex-col items-start">
                <span className="text-sm text-gray-900">{data.label}</span>

                <span className="text-xs text-gray-500">{data.name}</span>
            </div>

            {showEditNodeDialog && (
                <EditNodeDialog
                    visible={showEditNodeDialog}
                    onClose={() => setShowEditNodeDialog(false)}
                />
            )}

            <Handle
                className={styles.handle}
                type="target"
                position={Position.Top}
                isConnectable={false}
            />

            <Handle
                className={styles.handle}
                type="source"
                position={Position.Bottom}
                isConnectable={false}
            />
        </div>
    );
};

export default memo(WorkflowNode);
