import {describe, expect, it, vi} from 'vitest';

import isCollapsibleRowClick, {createCollapsibleRowClickHandler} from '../utils/isCollapsibleRowClick';

import type {MouseEvent} from 'react';

const createRow = () => {
    const rowElement = document.createElement('div');
    const labelElement = document.createElement('span');
    const buttonElement = document.createElement('button');
    const iconElement = document.createElementNS('http://www.w3.org/2000/svg', 'svg');

    rowElement.append(labelElement, buttonElement, iconElement);

    return {buttonElement, iconElement, labelElement, rowElement};
};

const createClickEvent = (currentTarget: Element, target: Element) =>
    ({currentTarget, target}) as unknown as MouseEvent;

describe('isCollapsibleRowClick', () => {
    it('toggles when the row label is clicked', () => {
        const {labelElement, rowElement} = createRow();

        expect(isCollapsibleRowClick(createClickEvent(rowElement, labelElement))).toBe(true);
    });

    it('toggles when the row icon is clicked', () => {
        const {iconElement, rowElement} = createRow();

        expect(isCollapsibleRowClick(createClickEvent(rowElement, iconElement))).toBe(true);
    });

    it('does not toggle when a button inside the row is clicked', () => {
        const {buttonElement, rowElement} = createRow();

        expect(isCollapsibleRowClick(createClickEvent(rowElement, buttonElement))).toBe(false);
    });

    it('does not toggle when portaled content outside the row is clicked', () => {
        const {rowElement} = createRow();

        const menuItemElement = document.createElement('div');

        expect(isCollapsibleRowClick(createClickEvent(rowElement, menuItemElement))).toBe(false);
    });

    it('honors custom interactive selectors', () => {
        const {iconElement, rowElement} = createRow();

        expect(isCollapsibleRowClick(createClickEvent(rowElement, iconElement), ['svg'])).toBe(false);
    });
});

describe('createCollapsibleRowClickHandler', () => {
    it('flips the expanded state when the row is clicked', () => {
        const {labelElement, rowElement} = createRow();
        const setExpanded = vi.fn();

        createCollapsibleRowClickHandler(setExpanded)(createClickEvent(rowElement, labelElement));

        expect(setExpanded).toHaveBeenCalledTimes(1);

        const toggle = setExpanded.mock.calls[0][0] as (expanded: boolean) => boolean;

        expect(toggle(false)).toBe(true);
        expect(toggle(true)).toBe(false);
    });

    it('leaves the expanded state alone when a button inside the row is clicked', () => {
        const {buttonElement, rowElement} = createRow();
        const setExpanded = vi.fn();

        createCollapsibleRowClickHandler(setExpanded)(createClickEvent(rowElement, buttonElement));

        expect(setExpanded).not.toHaveBeenCalled();
    });
});
