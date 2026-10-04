import {McpServer} from '@/shared/middleware/graphql';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpServerListItem from './useMcpServerListItem';

import type {MouseEvent} from 'react';

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteEmbeddedMcpServerMutation: () => ({mutate: vi.fn()}),
    useUpdateMcpServerMutation: () => ({mutate: vi.fn()}),
    useUpdateMcpServerTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: vi.fn()}),
}));

const mcpServer = {id: '1', name: 'mcpserver1'} as McpServer;

const createClickEvent = (currentTarget: HTMLElement, target: HTMLElement) =>
    ({currentTarget, target}) as unknown as MouseEvent;

describe('useMcpServerListItem', () => {
    let listItemElement: HTMLDivElement;
    let toolsCollapsibleTriggerElement: HTMLButtonElement;

    beforeEach(() => {
        listItemElement = document.createElement('div');
        toolsCollapsibleTriggerElement = document.createElement('button');

        toolsCollapsibleTriggerElement.click = vi.fn();
    });

    it('toggles the tools collapsible when the list item itself is clicked', () => {
        const {result} = renderHook(() => useMcpServerListItem(mcpServer));

        result.current.toolsCollapsibleTriggerRef.current = toolsCollapsibleTriggerElement;

        result.current.handleMcpServerListItemClick(createClickEvent(listItemElement, listItemElement));

        expect(toolsCollapsibleTriggerElement.click).toHaveBeenCalledTimes(1);
    });

    // Dropdown menu items are portaled outside the list item, but their clicks still bubble through the React tree.
    it('does not toggle the tools collapsible when a portaled dropdown menu item is clicked', () => {
        const {result} = renderHook(() => useMcpServerListItem(mcpServer));

        result.current.toolsCollapsibleTriggerRef.current = toolsCollapsibleTriggerElement;

        const dropdownMenuItemElement = document.createElement('div');

        dropdownMenuItemElement.setAttribute('role', 'menuitem');

        result.current.handleMcpServerListItemClick(createClickEvent(listItemElement, dropdownMenuItemElement));

        expect(toolsCollapsibleTriggerElement.click).not.toHaveBeenCalled();
    });
});
