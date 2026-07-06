import Button from '@/components/Button/Button';
import {Panel} from '@xyflow/react';
import {EllipsisVerticalIcon, MousePointerClickIcon, XIcon} from 'lucide-react';
import {useCallback, useEffect, useState} from 'react';

const NODE_ACTIONS_HINT_STORAGE_KEY = 'bytechef.workflow-node-actions-hint-seen';

const NodeActionsHint = () => {
    const [visible, setVisible] = useState(false);

    const handleDismiss = useCallback(() => {
        setVisible(false);

        try {
            localStorage.setItem(NODE_ACTIONS_HINT_STORAGE_KEY, 'true');
        } catch {
            console.error('Failed to save hint dismissal state to localStorage');
        }
    }, []);

    useEffect(() => {
        try {
            if (localStorage.getItem(NODE_ACTIONS_HINT_STORAGE_KEY) !== 'true') {
                setVisible(true);
            }
        } catch {
            setVisible(true);
        }
    }, []);

    if (!visible) {
        return null;
    }

    return (
        <Panel className="m-2" position="top-center">
            <div className="flex items-center gap-2 rounded-md border border-stroke-neutral-tertiary bg-surface-main px-3 py-1.5 shadow-sm">
                <MousePointerClickIcon className="size-4 shrink-0 text-content-neutral-secondary" />

                <span className="flex items-center text-xs font-medium whitespace-nowrap text-content-neutral-primary">
                    Right-click a node or use the
                    <EllipsisVerticalIcon className="mx-1 inline size-4 align-text-bottom" />
                    menu for actions.
                </span>

                <Button
                    aria-label="Dismiss hint"
                    icon={<XIcon className="size-auto" />}
                    onClick={handleDismiss}
                    size="iconXs"
                    title="Dismiss"
                    variant="destructiveGhost"
                />
            </div>
        </Panel>
    );
};

export default NodeActionsHint;
