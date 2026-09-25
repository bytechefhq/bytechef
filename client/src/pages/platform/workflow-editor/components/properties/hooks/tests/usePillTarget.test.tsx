import usePillTarget from '@/pages/platform/workflow-editor/components/properties/hooks/usePillTarget';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const outerInsertPill = vi.fn();

const PillTargetHarness = ({onOuterDrop}: {onOuterDrop?: (defaultPrevented: boolean) => void}) => {
    const {targetProps} = usePillTarget({acceptsPill: () => true, insertPill: outerInsertPill});

    return (
        <div onDrop={(event) => onOuterDrop?.(event.defaultPrevented)}>
            <div {...targetProps}>
                <input aria-label="own field" />

                <div data-pill-target="">
                    <input aria-label="nested field" />
                </div>
            </div>
        </div>
    );
};

const dataPillTransfer = {
    getData: () => JSON.stringify({mentionId: 'trigger_1.name'}),
    types: ['application/bytechef-datapill'],
};

describe('usePillTarget', () => {
    beforeEach(() => {
        outerInsertPill.mockReset();

        useWorkflowNodeDetailsPanelStore.setState({pillTarget: null});
    });

    it('ignores a focus that originates inside a nested pill target', () => {
        render(<PillTargetHarness />);

        fireEvent.focus(screen.getByLabelText('nested field'));

        expect(useWorkflowNodeDetailsPanelStore.getState().pillTarget).toBeNull();

        fireEvent.focus(screen.getByLabelText('own field'));

        expect(useWorkflowNodeDetailsPanelStore.getState().pillTarget).not.toBeNull();
    });

    it('ignores a drop that originates inside a nested pill target', () => {
        render(<PillTargetHarness />);

        fireEvent.drop(screen.getByLabelText('nested field'), {dataTransfer: dataPillTransfer});

        expect(outerInsertPill).not.toHaveBeenCalled();
    });

    it('ignores a drop another handler already took', () => {
        render(<PillTargetHarness />);

        const ownField = screen.getByLabelText('own field');

        ownField.addEventListener('drop', (event) => event.preventDefault());

        fireEvent.drop(ownField, {dataTransfer: dataPillTransfer});

        expect(outerInsertPill).not.toHaveBeenCalled();
    });

    it('stops a handled drop from reaching the ancestors', () => {
        const onOuterDrop = vi.fn();

        render(<PillTargetHarness onOuterDrop={onOuterDrop} />);

        fireEvent.drop(screen.getByLabelText('own field'), {dataTransfer: dataPillTransfer});

        expect(outerInsertPill).toHaveBeenCalledWith('trigger_1.name');
        expect(onOuterDrop).not.toHaveBeenCalled();
    });

    it('clears its own registration on unmount', () => {
        const {unmount} = render(<PillTargetHarness />);

        fireEvent.focus(screen.getByLabelText('own field'));

        unmount();

        expect(useWorkflowNodeDetailsPanelStore.getState().pillTarget).toBeNull();
    });
});
