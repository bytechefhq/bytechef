import {act, renderHook} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import useButtonGroupDropdownAlign from '../useButtonGroupDropdownAlign';

const stubLeftEdge = (element: HTMLElement, left: number) => {
    element.getBoundingClientRect = () => ({left}) as DOMRect;
};

describe('useButtonGroupDropdownAlign', () => {
    const renderWithAttachedRefs = (buttonGroupLeft: number, dropdownMenuTriggerLeft: number) => {
        const {result} = renderHook(() => useButtonGroupDropdownAlign());

        const buttonGroup = document.createElement('div');
        const dropdownMenuTrigger = document.createElement('button');

        stubLeftEdge(buttonGroup, buttonGroupLeft);
        stubLeftEdge(dropdownMenuTrigger, dropdownMenuTriggerLeft);

        result.current.buttonGroupRef.current = buttonGroup;
        result.current.dropdownMenuTriggerRef.current = dropdownMenuTrigger;

        return {buttonGroup, dropdownMenuTrigger, result};
    };

    it('starts with no offset', () => {
        const {result} = renderHook(() => useButtonGroupDropdownAlign());

        expect(result.current.alignOffset).toBe(0);
    });

    it('shifts the menu left by the distance between the group and the trigger when opened', () => {
        const {result} = renderWithAttachedRefs(100, 260);

        act(() => result.current.handleOpenChange(true));

        expect(result.current.alignOffset).toBe(-160);
    });

    it('keeps the measured offset when the menu closes', () => {
        const {result} = renderWithAttachedRefs(100, 260);

        act(() => result.current.handleOpenChange(true));
        act(() => result.current.handleOpenChange(false));

        expect(result.current.alignOffset).toBe(-160);
    });

    it('remeasures on every open, so a wider label moves the menu further left', () => {
        const {dropdownMenuTrigger, result} = renderWithAttachedRefs(100, 260);

        act(() => result.current.handleOpenChange(true));

        stubLeftEdge(dropdownMenuTrigger, 300);

        act(() => result.current.handleOpenChange(true));

        expect(result.current.alignOffset).toBe(-200);
    });

    it('leaves the offset alone while the refs are unattached', () => {
        const {result} = renderHook(() => useButtonGroupDropdownAlign());

        act(() => result.current.handleOpenChange(true));

        expect(result.current.alignOffset).toBe(0);
    });
});
