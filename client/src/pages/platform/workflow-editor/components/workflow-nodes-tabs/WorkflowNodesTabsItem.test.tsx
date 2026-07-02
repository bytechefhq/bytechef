import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {expect, it, vi} from 'vitest';

import WorkflowNodesTabsItem from './WorkflowNodesTabsItem';

const mockNode = {
    clusterElement: false,
    description: 'This is a test node description',
    name: 'testNode',
    taskDispatcher: false,
    title: 'Test Node Title',
    trigger: false,
} as unknown as ComponentDefinitionBasic & {
    clusterElement?: boolean;
    taskDispatcher: boolean;
    trigger: boolean;
};

it('renders the workflow node item correctly with title and description', () => {
    render(<WorkflowNodesTabsItem draggable={true} node={mockNode} />);

    const titleElement = screen.getByText('Test Node Title');
    const descriptionElement = screen.getByText('This is a test node description');

    expect(titleElement).toBeInTheDocument();
    expect(descriptionElement).toBeInTheDocument();
});

it('triggers handleClick when clicked', () => {
    const handleClick = vi.fn();

    render(<WorkflowNodesTabsItem draggable={true} handleClick={handleClick} node={mockNode} />);

    const listItem = screen.getByRole('listitem');

    fireEvent.click(listItem);

    expect(handleClick).toHaveBeenCalledTimes(1);
});

it('triggers onDragStart and sets correct drag data and drag image', () => {
    render(<WorkflowNodesTabsItem draggable={true} node={mockNode} />);

    const setData = vi.fn();
    const setDragImage = vi.fn();
    const dataTransfer = {
        effectAllowed: '',
        setData,
        setDragImage,
    };

    const listItem = screen.getByRole('listitem');

    fireEvent.dragStart(listItem, {
        dataTransfer,
    });

    const formatString = 'application/reactflow';
    const expectedNodeName = 'testNode';

    expect(setData).toHaveBeenCalledWith(formatString, expectedNodeName);
    expect(dataTransfer.effectAllowed).toBe('move');
    expect(setDragImage).toHaveBeenCalledTimes(1);
});

it('maps the trigger node name correctly with suffix --trigger', () => {
    const triggerNode = {
        ...mockNode,
        trigger: true,
    } as unknown as ComponentDefinitionBasic & {
        clusterElement?: boolean;
        taskDispatcher: boolean;
        trigger: boolean;
    };

    render(<WorkflowNodesTabsItem draggable={true} node={triggerNode} />);

    const setData = vi.fn();
    const setDragImage = vi.fn();
    const dataTransfer = {
        effectAllowed: '',
        setData,
        setDragImage,
    };

    const listItem = screen.getByRole('listitem');

    fireEvent.dragStart(listItem, {
        dataTransfer,
    });

    const formatString = 'application/reactflow';
    const expectedNodeName = 'testNode--trigger';

    expect(setData).toHaveBeenCalledWith(formatString, expectedNodeName);
});
