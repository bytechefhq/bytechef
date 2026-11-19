import {TooltipProvider} from '@/components/ui/tooltip';
import {CustomComponent, CustomComponentLanguage, CustomComponentStatus} from '@/shared/middleware/graphql';
import {fireEvent, render, resetAll, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import CustomComponentListItem from '../CustomComponentListItem';

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

        await screen.getByRole('button', {name: 'Show actions and triggers'}).click();

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

        expect(screen.getByText('DRAFT')).toBeInTheDocument();
        expect(screen.queryByText('PUBLISHED')).not.toBeInTheDocument();

        unmount();

        const publishedCustomComponent = createCustomComponent(
            CustomComponentLanguage.Javascript,
            CustomComponentStatus.Published
        );

        renderListItem(publishedCustomComponent);

        expect(screen.getByText('PUBLISHED')).toBeInTheDocument();
        expect(screen.queryByText('DRAFT')).not.toBeInTheDocument();
    });

    it('renders the language badge before the version badge', () => {
        renderListItem(createCustomComponent(CustomComponentLanguage.Java));

        const versionBadge = screen.getByText('V1');
        const languageBadge = screen.getByText('Java');

        expect(languageBadge.compareDocumentPosition(versionBadge) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        expect(screen.getByText('My Component').parentElement).not.toContainElement(versionBadge);
    });

    it('shows actions and triggers of a Java component in separate tabs', async () => {
        hoisted.mockUseCustomComponentDefinitionQuery.mockReturnValue({
            data: {
                customComponentDefinition: {
                    actions: [{description: 'Fetches a ticket', name: 'getTicket', title: 'Get Ticket'}],
                    triggers: [{description: null, name: 'newTicket', title: 'New Ticket'}],
                },
            },
            isLoading: false,
        });

        renderListItem(createCustomComponent(CustomComponentLanguage.Java));

        await userEvent.click(screen.getByRole('button', {name: 'Show actions and triggers'}));

        expect(screen.getByRole('tab', {name: 'Actions (1)'})).toHaveAttribute('aria-selected', 'true');
        expect(screen.getByText('Get Ticket')).toBeInTheDocument();
        expect(screen.queryByText('New Ticket')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('tab', {name: 'Triggers (1)'}));

        expect(screen.getByText('New Ticket')).toBeInTheDocument();
        expect(screen.queryByText('Get Ticket')).not.toBeInTheDocument();
    });

    it('shows the empty message in the triggers tab when a Java component has no triggers', async () => {
        hoisted.mockUseCustomComponentDefinitionQuery.mockReturnValue({
            data: {customComponentDefinition: {actions: [], triggers: []}},
            isLoading: false,
        });

        renderListItem(createCustomComponent(CustomComponentLanguage.Java));

        await userEvent.click(screen.getByRole('button', {name: 'Show actions and triggers'}));

        expect(screen.getByText('No actions defined')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('tab', {name: 'Triggers (0)'}));

        expect(screen.getByText('No triggers defined')).toBeInTheDocument();
    });

    it('expands the actions and triggers of a JavaScript component without opening its editor', async () => {
        hoisted.mockUseCustomComponentDefinitionQuery.mockReturnValue({
            data: {
                customComponentDefinition: {
                    actions: [{description: null, name: 'createTicket', title: 'Create Ticket'}],
                    triggers: [],
                },
            },
            isError: false,
            isLoading: false,
        });

        renderListItem(createCustomComponent(CustomComponentLanguage.Javascript));

        await userEvent.click(screen.getByRole('button', {name: 'Show actions and triggers'}));

        expect(hoisted.mockNavigate).not.toHaveBeenCalled();
        expect(screen.getByText('Create Ticket')).toBeInTheDocument();
        expect(hoisted.mockUseCustomComponentDefinitionQuery).toHaveBeenLastCalledWith({id: '1'}, {enabled: true});
    });

    it('loads no definition until a component is expanded', () => {
        renderListItem(createCustomComponent(CustomComponentLanguage.Python));

        expect(hoisted.mockUseCustomComponentDefinitionQuery).toHaveBeenLastCalledWith({id: '1'}, {enabled: false});
    });

    it('tells the user to fix the source when a JavaScript component definition cannot be loaded', async () => {
        hoisted.mockUseCustomComponentDefinitionQuery.mockReturnValue({
            data: undefined,
            isError: true,
            isLoading: false,
        });

        renderListItem(createCustomComponent(CustomComponentLanguage.Javascript));

        await userEvent.click(screen.getByRole('button', {name: 'Show actions and triggers'}));

        expect(
            screen.getByText('The component definition could not be loaded. Open the component to fix its source.')
        ).toBeInTheDocument();
        expect(screen.queryByRole('tab')).not.toBeInTheDocument();
    });

    it('underlines the title on row hover only for components that open a details page', () => {
        const {unmount} = renderListItem(createCustomComponent(CustomComponentLanguage.Javascript));

        expect(screen.getByText('My Component')).toHaveClass('group-hover:underline');

        unmount();

        renderListItem(createCustomComponent(CustomComponentLanguage.Java));

        expect(screen.getByText('My Component')).not.toHaveClass('group-hover:underline');
    });
});
