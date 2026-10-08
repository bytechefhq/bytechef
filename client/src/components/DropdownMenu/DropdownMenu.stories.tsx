import Button from '@/components/Button/Button';
import {
    ArrowLeftRightIcon,
    BlendIcon,
    ClipboardPlusIcon,
    CopyIcon,
    InfoIcon,
    PencilIcon,
    TextCursorInputIcon,
    Trash2Icon,
    UploadIcon,
} from 'lucide-react';
import {useState} from 'react';

import {
    DropdownMenu,
    DropdownMenuCheckboxItem,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuPortal,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuSeparator,
    DropdownMenuShortcut,
    DropdownMenuSub,
    DropdownMenuSubContent,
    DropdownMenuSubTrigger,
    DropdownMenuTrigger,
} from './DropdownMenu';

import type {Meta, StoryObj} from '@storybook/react-vite';

const meta = {
    component: DropdownMenu,
    parameters: {
        layout: 'centered',
    },
    tags: ['!autodocs'],
    title: 'Components/DropdownMenu',
} satisfies Meta<typeof DropdownMenu>;

export default meta;

// eslint-disable-next-line @typescript-eslint/naming-convention
type Story = StoryObj<typeof meta>;

export const NodeMenu: Story = {
    render: () => (
        <DropdownMenu defaultOpen>
            <DropdownMenuTrigger asChild>
                <Button label="Node actions" variant="outline" />
            </DropdownMenuTrigger>

            <DropdownMenuContent className="w-64">
                <DropdownMenuItem icon={<ArrowLeftRightIcon />} label="Replace" />

                <DropdownMenuItem icon={<TextCursorInputIcon />} label="Rename" />

                <DropdownMenuItem icon={<InfoIcon />} label="Info" />

                <DropdownMenuSeparator />

                <DropdownMenuItem icon={<Trash2Icon />} label="Delete" variant="destructive" />
            </DropdownMenuContent>
        </DropdownMenu>
    ),
};

export const ItemStates: Story = {
    render: () => (
        <DropdownMenu defaultOpen>
            <DropdownMenuTrigger asChild>
                <Button label="Item states" variant="outline" />
            </DropdownMenuTrigger>

            <DropdownMenuContent>
                <DropdownMenuItem label="Label only" />

                <DropdownMenuItem icon={<PencilIcon />} label="With icon" />

                <DropdownMenuItem
                    icon={<UploadIcon className="text-content-brand-primary" />}
                    label="Own icon colour"
                />

                <DropdownMenuItem disabled icon={<CopyIcon />} label="Disabled" />

                <DropdownMenuSeparator />

                <DropdownMenuItem icon={<Trash2Icon />} label="Destructive" variant="destructive" />

                <DropdownMenuItem disabled icon={<Trash2Icon />} label="Disabled destructive" variant="destructive" />
            </DropdownMenuContent>
        </DropdownMenu>
    ),
};

export const LabelsAndShortcuts: Story = {
    render: () => (
        <DropdownMenu defaultOpen>
            <DropdownMenuTrigger asChild>
                <Button label="Workflow" variant="outline" />
            </DropdownMenuTrigger>

            <DropdownMenuContent className="w-56">
                <DropdownMenuLabel>Workflow</DropdownMenuLabel>

                <DropdownMenuGroup>
                    <DropdownMenuItem>
                        <CopyIcon />

                        <span>Copy</span>

                        <DropdownMenuShortcut>⌘C</DropdownMenuShortcut>
                    </DropdownMenuItem>

                    <DropdownMenuItem>
                        <ClipboardPlusIcon />

                        <span>Paste</span>

                        <DropdownMenuShortcut>⌘V</DropdownMenuShortcut>
                    </DropdownMenuItem>
                </DropdownMenuGroup>
            </DropdownMenuContent>
        </DropdownMenu>
    ),
};

export const Submenu: Story = {
    render: () => (
        <DropdownMenu defaultOpen>
            <DropdownMenuTrigger asChild>
                <Button label="Settings" variant="outline" />
            </DropdownMenuTrigger>

            <DropdownMenuContent className="w-56">
                <DropdownMenuSub>
                    <DropdownMenuSubTrigger icon={<BlendIcon />} label="Mode" />

                    <DropdownMenuPortal>
                        <DropdownMenuSubContent>
                            <DropdownMenuRadioGroup value="automation">
                                <DropdownMenuRadioItem label="Automation" value="automation" />

                                <DropdownMenuRadioItem label="Embedded" value="embedded" />
                            </DropdownMenuRadioGroup>
                        </DropdownMenuSubContent>
                    </DropdownMenuPortal>
                </DropdownMenuSub>
            </DropdownMenuContent>
        </DropdownMenu>
    ),
};

function CheckboxItemsExample() {
    const [selectedCategories, setSelectedCategories] = useState<string[]>(['Triggers']);

    const toggleCategory = (category: string) =>
        setSelectedCategories((previousCategories) =>
            previousCategories.includes(category)
                ? previousCategories.filter((previousCategory) => previousCategory !== category)
                : [...previousCategories, category]
        );

    return (
        <DropdownMenu defaultOpen>
            <DropdownMenuTrigger asChild>
                <Button label="Filter" variant="outline" />
            </DropdownMenuTrigger>

            <DropdownMenuContent className="w-56">
                {['Actions', 'Triggers', 'Flows'].map((category) => (
                    <DropdownMenuCheckboxItem
                        checked={selectedCategories.includes(category)}
                        key={category}
                        label={category}
                        onCheckedChange={() => toggleCategory(category)}
                    />
                ))}
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

export const CheckboxItems: Story = {
    render: () => <CheckboxItemsExample />,
};

export const CustomRow: Story = {
    render: () => (
        <DropdownMenu defaultOpen>
            <DropdownMenuTrigger asChild>
                <Button label="Node actions" variant="outline" />
            </DropdownMenuTrigger>

            <DropdownMenuContent className="w-64">
                <DropdownMenuItem>
                    <div className="flex w-full flex-col items-start gap-1">
                        <div className="flex items-center gap-2">
                            <ClipboardPlusIcon />

                            <span>Paste After</span>
                        </div>

                        <span className="text-xs text-content-neutral-secondary">HTTP Client (httpClient_1)</span>
                    </div>
                </DropdownMenuItem>

                <DropdownMenuSeparator />

                <DropdownMenuItem icon={<Trash2Icon />} label="Delete" variant="destructive" />
            </DropdownMenuContent>
        </DropdownMenu>
    ),
};
