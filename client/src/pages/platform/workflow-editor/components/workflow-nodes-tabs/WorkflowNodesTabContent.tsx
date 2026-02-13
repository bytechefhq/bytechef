import {ScrollArea} from '@/components/ui/scroll-area';
import {TabsContent} from '@/components/ui/tabs';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {ClickedDefinitionType, DefinitionType} from '@/shared/types';

import WorkflowNodesTabsItem from './WorkflowNodesTabsItem';

interface WorkflowNodesTabContentProps {
    emptyMessage: string;
    items: Array<ComponentDefinitionBasic | DefinitionType> | undefined;
    itemsDraggable: boolean;
    onItemClick?: (clickedItem: ClickedDefinitionType) => void;
    selectedComponentName?: string;
    tabValue: string;
}

const WorkflowNodesTabContent = ({
    emptyMessage,
    items,
    itemsDraggable,
    onItemClick,
    selectedComponentName,
    tabValue,
}: WorkflowNodesTabContentProps) => (
    <ScrollArea className="overflow-y-auto px-3" id="popover-components">
        <TabsContent className="mt-0 w-full flex-1" value={tabValue}>
            <ul className="space-y-2" role="list">
                {(!items || items.length === 0) && (
                    <span className="block px-3 py-2 text-xs text-content-neutral-secondary">{emptyMessage}</span>
                )}

                {items?.map((item) => (
                    <WorkflowNodesTabsItem
                        draggable={itemsDraggable}
                        handleClick={() => onItemClick && onItemClick(item as ClickedDefinitionType)}
                        key={item.name}
                        node={item as DefinitionType}
                        selected={tabValue === 'components' ? selectedComponentName === item.name : undefined}
                    />
                ))}
            </ul>
        </TabsContent>
    </ScrollArea>
);

export default WorkflowNodesTabContent;
