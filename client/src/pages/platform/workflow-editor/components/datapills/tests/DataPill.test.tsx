import {TooltipProvider} from '@/components/ui/tooltip';
import {type PillTargetI} from '@/pages/platform/workflow-editor/components/datapills/pillTarget';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {PropertyAllType} from '@/shared/types';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ComponentProps} from 'react';
import {type Mock, afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DataPill from '../DataPill';

vi.mock('@/assets/TwoHashesIcon.svg?react', () => ({
    default: () => <svg />,
}));

const LONG_SAMPLE_VALUE = 'This sample value is clearly longer than twenty-seven characters';

const USER_PROPERTY = {
    name: 'user',
    properties: [
        {description: 'The primary e-mail address', name: 'email', type: 'STRING'},
        {name: 'first-name', type: 'STRING'},
        {items: [{type: 'STRING'}], name: 'tags', type: 'ARRAY'},
        {name: 'bio', type: 'STRING'},
        {name: 'manager', type: 'STRING'},
        {name: 'settings', properties: [], type: 'OBJECT'},
    ],
    type: 'OBJECT',
} as unknown as PropertyAllType;

const SAMPLE_OUTPUT = {
    user: {
        bio: LONG_SAMPLE_VALUE,
        email: 'ann@example.com',
        'first-name': 'Ann',
        manager: null,
        settings: {},
        tags: ['vip'],
    },
};

const createPillTarget = (acceptsPill = true): PillTargetI & {insertPill: Mock<(mentionId: string) => void>} => ({
    acceptsPill: () => acceptsPill,
    insertPill: vi.fn<(mentionId: string) => void>(),
    owner: {},
});

const renderDataPill = (props: Partial<ComponentProps<typeof DataPill>> = {}) =>
    render(
        <TooltipProvider delayDuration={0}>
            <ul>
                <DataPill property={USER_PROPERTY} sampleOutput={SAMPLE_OUTPUT} workflowNodeName="node_1" {...props} />
            </ul>
        </TooltipProvider>
    );

const getPill = (label: string) => screen.getByText(label, {selector: 'span'}).parentElement as HTMLElement;

const getSampleValueOf = (label: string) => getPill(label).parentElement!.parentElement!.lastElementChild;

beforeEach(() => {
    useWorkflowNodeDetailsPanelStore.setState({pillTarget: null});
    useDataPillPanelStore.setState({isDraggingDataPill: false});
});

afterEach(() => {
    useWorkflowNodeDetailsPanelStore.setState({pillTarget: null});
});

describe('DataPill', () => {
    describe('inserting a pill into the focused field', () => {
        it('inserts the reference of the clicked property', async () => {
            const pillTarget = createPillTarget();

            useWorkflowNodeDetailsPanelStore.setState({pillTarget});

            renderDataPill();

            await userEvent.click(getPill('user'));
            await userEvent.click(getPill('email'));

            expect(pillTarget.insertPill.mock.calls).toEqual([['node_1.user'], ['node_1.user.email']]);
        });

        it('quotes a property name that is not a plain identifier', async () => {
            const pillTarget = createPillTarget();

            useWorkflowNodeDetailsPanelStore.setState({pillTarget});

            renderDataPill();

            await userEvent.click(getPill('first-name'));

            expect(pillTarget.insertPill).toHaveBeenCalledWith("node_1.user['first-name']");
        });

        it('inserts the first element of an array for its item pill', async () => {
            const pillTarget = createPillTarget();

            useWorkflowNodeDetailsPanelStore.setState({pillTarget});

            renderDataPill();

            const tagsList = getPill('tags').parentElement!.querySelector('ul') as HTMLElement;

            await userEvent.click(within(tagsList).getByText('[index]'));

            expect(pillTarget.insertPill).toHaveBeenCalledWith('node_1.user.tags[0]');
        });

        it('inserts the bare node name from a root pill', async () => {
            const pillTarget = createPillTarget();

            useWorkflowNodeDetailsPanelStore.setState({pillTarget});

            renderDataPill({property: {type: 'OBJECT'} as PropertyAllType, root: true});

            await userEvent.click(screen.getByText('node_1'));

            expect(pillTarget.insertPill).toHaveBeenCalledWith('node_1');
        });

        it('inserts nothing when the focused field refuses a pill', async () => {
            const pillTarget = createPillTarget(false);

            useWorkflowNodeDetailsPanelStore.setState({pillTarget});

            renderDataPill();

            await userEvent.click(getPill('email'));

            expect(pillTarget.insertPill).not.toHaveBeenCalled();
        });

        it('names an unnamed array builder item "[index]"', async () => {
            const pillTarget = createPillTarget();

            useWorkflowNodeDetailsPanelStore.setState({pillTarget});

            renderDataPill({
                path: 'items/[index]',
                property: {controlType: 'ARRAY_BUILDER', type: 'ARRAY'} as PropertyAllType,
                sampleOutput: undefined,
            });

            await userEvent.click(screen.getByText('[index]'));

            expect(pillTarget.insertPill).toHaveBeenCalledWith('node_1.items[0]');
        });
    });

    describe('dragging a pill', () => {
        const createDataTransfer = () => ({
            effectAllowed: 'none',
            setData: vi.fn(),
            setDragImage: vi.fn(),
        });

        it('carries the pill reference and flags the drag while it lasts', async () => {
            renderDataPill();

            const dataTransfer = createDataTransfer();
            const emailPill = getPill('email');

            fireEvent.dragStart(emailPill, {dataTransfer});

            expect(dataTransfer.setData).toHaveBeenCalledWith(
                'application/bytechef-datapill',
                JSON.stringify({mentionId: 'node_1.user.email'})
            );
            expect(dataTransfer.effectAllowed).toBe('copy');
            expect(useDataPillPanelStore.getState().isDraggingDataPill).toBe(true);

            fireEvent.dragEnd(emailPill);

            expect(useDataPillPanelStore.getState().isDraggingDataPill).toBe(false);

            await waitFor(() => {
                expect(document.body.querySelectorAll('[data-name="email"]')).toHaveLength(1);
            });
        });

        it('uses a detached copy of the pill as the drag image', async () => {
            renderDataPill({property: {type: 'OBJECT'} as PropertyAllType, root: true});

            const dataTransfer = createDataTransfer();

            fireEvent.dragStart(screen.getByText('node_1').parentElement!, {dataTransfer});

            const [dragImage] = dataTransfer.setDragImage.mock.calls[0];

            expect(dragImage).toHaveTextContent('node_1');
            expect(dragImage.style.top).toBe('-9999px');

            await waitFor(() => {
                expect(document.body.contains(dragImage)).toBe(false);
            });
        });
    });

    describe('sample values', () => {
        it('shows the sample value of each sub-property next to its pill', () => {
            renderDataPill();

            expect(getSampleValueOf('email')).toHaveTextContent('ann@example.com');
            expect(getSampleValueOf('first-name')).toHaveTextContent('Ann');
            expect(getSampleValueOf('manager')).toHaveTextContent('null');
        });

        it('shows the sample value of an array item', () => {
            renderDataPill();

            const tagsList = getPill('tags').parentElement!.querySelector('ul') as HTMLElement;

            expect(within(tagsList).getByText('vip')).toBeInTheDocument();
        });

        it('shows a scalar sample value next to a root pill but not an object', () => {
            const {unmount} = renderDataPill({
                property: {type: 'STRING'} as PropertyAllType,
                root: true,
                sampleOutput: 'plain value',
            });

            expect(screen.getByText('plain value')).toBeInTheDocument();

            unmount();

            renderDataPill({property: {type: 'OBJECT'} as PropertyAllType, root: true, sampleOutput: {a: 1}});

            expect(screen.getByText('node_1').parentElement!.nextElementSibling).toBeNull();
        });

        it('reveals the whole of a long nested sample value on hover', async () => {
            renderDataPill();

            const sampleValue = getSampleValueOf('bio') as HTMLElement;

            await userEvent.hover(sampleValue);

            expect(await screen.findByRole('tooltip')).toHaveTextContent(LONG_SAMPLE_VALUE);
        });

        it('shows an empty object sample value as {}', () => {
            renderDataPill();

            expect(getSampleValueOf('settings')).toHaveTextContent(/^\{\}$/);
        });

        it('shows the property description on hover', async () => {
            renderDataPill();

            await userEvent.hover(getPill('email'));

            expect(await screen.findByRole('tooltip')).toHaveTextContent('The primary e-mail address');
        });
    });
});
