import * as DropdownMenu from '@radix-ui/react-dropdown-menu';

export const Dropdown = () => {
    return (
        <DropdownMenu.Root>
            <DropdownMenu.Trigger asChild>
                <button className="IconButton" aria-label="Customise options">
                    ...
                </button>
            </DropdownMenu.Trigger>

            <DropdownMenu.Portal>
                <DropdownMenu.Content
                    className="DropdownMenuContent"
                    sideOffset={5}
                >
                    <DropdownMenu.Item className="DropdownMenuItem">
                        Edit
                    </DropdownMenu.Item>
                    <DropdownMenu.Item className="DropdownMenuItem">
                        Enable
                    </DropdownMenu.Item>
                    <DropdownMenu.Item className="DropdownMenuItem">
                        Duplicate
                    </DropdownMenu.Item>
                    <DropdownMenu.Item className="DropdownMenuItem">
                        New Workflow
                    </DropdownMenu.Item>
                    <DropdownMenu.Separator className="DropdownMenuSeparator" />

                    <DropdownMenu.Item className="DropdownMenuItem">
                        Delete
                    </DropdownMenu.Item>

                    <DropdownMenu.Arrow className="DropdownMenuArrow" />
                </DropdownMenu.Content>
            </DropdownMenu.Portal>
        </DropdownMenu.Root>
    );
};
