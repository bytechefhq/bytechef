import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpServerListItemClick from '../hooks/useMcpServerListItemClick';

import type {MouseEvent} from 'react';

const createClickEvent = (currentTarget: HTMLElement, target: HTMLElement) =>
    ({currentTarget, target}) as unknown as MouseEvent;

describe('useMcpServerListItemClick', () => {
    let listItemElement: HTMLDivElement;
    let toolsCollapsibleTriggerElement: HTMLButtonElement;

    beforeEach(() => {
        listItemElement = document.createElement('div');
        toolsCollapsibleTriggerElement = document.createElement('button');

        toolsCollapsibleTriggerElement.click = vi.fn();
    });

    it('toggles the tools collapsible when the list item itself is clicked', () => {
        const {result} = renderHook(() => useMcpServerListItemClick());

        result.current.toolsCollapsibleTriggerRef.current = toolsCollapsibleTriggerElement;

        result.current.handleMcpServerListItemClick(createClickEvent(listItemElement, listItemElement));

        expect(toolsCollapsibleTriggerElement.click).toHaveBeenCalledTimes(1);
    });

    // Dropdown menu items are portaled outside the list item, but their clicks still bubble through the React tree.
    it('does not toggle the tools collapsible when a portaled dropdown menu item is clicked', () => {
        const {result} = renderHook(() => useMcpServerListItemClick());

        result.current.toolsCollapsibleTriggerRef.current = toolsCollapsibleTriggerElement;

        const dropdownMenuItemElement = document.createElement('div');

        dropdownMenuItemElement.setAttribute('role', 'menuitem');

        result.current.handleMcpServerListItemClick(createClickEvent(listItemElement, dropdownMenuItemElement));

        expect(toolsCollapsibleTriggerElement.click).not.toHaveBeenCalled();
    });

    it('does not toggle the tools collapsible when an interactive element inside the list item is clicked', () => {
        const {result} = renderHook(() => useMcpServerListItemClick());

        result.current.toolsCollapsibleTriggerRef.current = toolsCollapsibleTriggerElement;

        const switchElement = document.createElement('button');

        listItemElement.appendChild(switchElement);

        result.current.handleMcpServerListItemClick(createClickEvent(listItemElement, switchElement));

        expect(toolsCollapsibleTriggerElement.click).not.toHaveBeenCalled();
    });
});
