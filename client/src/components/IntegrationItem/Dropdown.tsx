import {
    Arrow,
    Content,
    Item,
    Portal,
    Root,
    Separator,
    Trigger,
} from '@radix-ui/react-dropdown-menu';
import {DotsHorizontalIcon} from '@radix-ui/react-icons';

export const Dropdown: React.FC<{
    id: string;
}> = ({id}) => {
    return (
        <Root>
            <Trigger asChild>
                <DotsHorizontalIcon />
            </Trigger>

            <Portal>
                <Content className="DropdownMenuContent" sideOffset={5}>
                    <Item className="DropdownMenuItem">Edit</Item>

                    <Item className="DropdownMenuItem">Enable</Item>

                    <Item className="DropdownMenuItem">Duplicate</Item>

                    <Item className="DropdownMenuItem">New Workflow</Item>

                    <Separator className="DropdownMenuSeparator" />

                    <Item className="DropdownMenuItem">Delete</Item>

                    <Arrow className="DropdownMenuArrow" />
                </Content>
            </Portal>
        </Root>
    );
};
