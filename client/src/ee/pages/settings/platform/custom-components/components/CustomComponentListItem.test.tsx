import {TooltipProvider} from '@/components/ui/tooltip';
import {CustomComponent, CustomComponentLanguage, CustomComponentStatus} from '@/shared/middleware/graphql';
import {fireEvent, render, resetAll, screen} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import CustomComponentListItem from './CustomComponentListItem';

const hoisted = vi.hoisted(() => ({
    mockNavigate: vi.fn(),
    mockUseCustomComponentDefinitionQuery: vi.fn(),
    mockUseDeleteCustomComponentMutation: vi.fn(),
    mockUseEnableCustomComponentMutation: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('react-router-dom');

    return {
        ...actual,
        useNavigate: () => hoisted.mockNavigate,
    };
});

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useCustomComponentDefinitionQuery: hoisted.mockUseCustomComponentDefinitionQuery,
        useDeleteCustomComponentMutation: hoisted.mockUseDeleteCustomComponentMutation,
        useEnableCustomComponentMutation: hoisted.mockUseEnableCustomComponentMutation,
    };
});

const createCustomComponent = (
    language: CustomComponentLanguage,
    status: CustomComponentStatus = CustomComponentStatus.Draft
): CustomComponent => ({
    componentVersion: 1,
    createdBy: null,
    createdDate: null,
    description: null,
    enabled: true,
    icon: null,
    id: '1',
    language,
    lastModifiedBy: null,
    lastModifiedDate: null,
    name: 'my-component',
    status,
    title: 'My Component',
    version: 1,
});

const renderListItem = (customComponent: CustomComponent) =>
    render(
        <TooltipProvider>
            <MemoryRouter initialEntries={['/custom-components']}>
                <Routes>
                    <Route element={<CustomComponentListItem customComponent={customComponent} />} path="*" />
                </Routes>
            </MemoryRouter>
        </TooltipProvider>
    );

beforeEach(() => {
    hoisted.mockUseCustomComponentDefinitionQuery.mockReturnValue({data: undefined, isLoading: false});
    hoisted.mockUseDeleteCustomComponentMutation.mockReturnValue({mutate: vi.fn()});
    hoisted.mockUseEnableCustomComponentMutation.mockReturnValue({mutate: vi.fn()});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('CustomComponentListItem', () => {
    it('navigates to the detail route when a non-Java component row is clicked', async () => {
        const customComponent = createCustomComponent(CustomComponentLanguage.Javascript);

        renderListItem(customComponent);

        await screen.getByText('My Component').click();

        expect(hoisted.mockNavigate).toHaveBeenCalledWith('1');
    });

    it('does not navigate when a Java component title is clicked', async () => {
        const customComponent = createCustomComponent(CustomComponentLanguage.Java);

        renderListItem(customComponent);

        await screen.getByText('My Component').click();

        expect(hoisted.mockNavigate).not.toHaveBeenCalled();
    });

    it('expands the definition view instead of navigating when the Java component trigger is clicked', async () => {
        const customComponent = createCustomComponent(CustomComponentLanguage.Java);

        hoisted.mockUseCustomComponentDefinitionQuery.mockReturnValue({
            data: {
                customComponentDefinition: {
                    actions: [{description: null, name: 'doSomething', title: 'Do Something'}],
                    triggers: [],
                },
            },
            isLoading: false,
        });

        renderListItem(customComponent);

        await screen.getAllByRole('button')[0].click();

        expect(hoisted.mockNavigate).not.toHaveBeenCalled();
        expect(await screen.findByText('Do Something')).toBeInTheDocument();
    });

    it('navigates to the detail route when a non-Java component row is activated by keyboard (Enter)', async () => {
        const customComponent = createCustomComponent(CustomComponentLanguage.Javascript);

        renderListItem(customComponent);

        const rowElement = screen.getByText('My Component').closest('div[role="button"]');

        fireEvent.keyDown(rowElement!, {key: 'Enter'});

        expect(hoisted.mockNavigate).toHaveBeenCalledWith('1');
    });

    it('navigates to the detail route when a non-Java component row is activated by keyboard (Space)', async () => {
        const customComponent = createCustomComponent(CustomComponentLanguage.Javascript);

        renderListItem(customComponent);

        const rowElement = screen.getByText('My Component').closest('div[role="button"]');

        fireEvent.keyDown(rowElement!, {key: ' '});

        expect(hoisted.mockNavigate).toHaveBeenCalledWith('1');
    });

    it('renders a Draft badge for draft components and a Published badge otherwise', () => {
        const draftCustomComponent = createCustomComponent(
            CustomComponentLanguage.Javascript,
            CustomComponentStatus.Draft
        );

        const {unmount} = renderListItem(draftCustomComponent);

        expect(screen.getByText('Draft')).toBeInTheDocument();
        expect(screen.queryByText('Published')).not.toBeInTheDocument();

        unmount();

        const publishedCustomComponent = createCustomComponent(
            CustomComponentLanguage.Javascript,
            CustomComponentStatus.Published
        );

        renderListItem(publishedCustomComponent);

        expect(screen.getByText('Published')).toBeInTheDocument();
        expect(screen.queryByText('Draft')).not.toBeInTheDocument();
    });
});
