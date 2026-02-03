import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialogRightPanelConnectionsPopover from '../PropertyCodeEditorDialogRightPanelConnectionsPopover';

const hoisted = vi.hoisted(() => {
    return {
        mockSetOpen: vi.fn(),
        storeState: {
            componentDefinitions: [
                {icon: 'slack-icon', name: 'slack', title: 'Slack', version: 1},
                {icon: 'github-icon', name: 'github', title: 'GitHub', version: 2},
            ],
            open: false,
        },
    };
});

vi.mock('../hooks/usePropertyCodeEditorDialogRightPanelConnectionsPopover', () => ({
    default: () => ({
        componentDefinitions: hoisted.storeState.componentDefinitions,
        form: {
            control: {},
            formState: {errors: {}},
            handleSubmit: (callback: (values: unknown) => void) => (event?: {preventDefault?: () => void}) => {
                event?.preventDefault?.();
                callback({componentName: 'slack', componentVersion: 1, name: 'mySlack'});
            },
            reset: vi.fn(),
            setValue: vi.fn(),
        },
        open: hoisted.storeState.open,
        setOpen: hoisted.mockSetOpen,
    }),
}));

vi.mock('@/components/ComboBox', () => ({
    default: ({
        items,
        name,
        onChange,
        value,
    }: {
        items: Array<{label: string; value: string}>;
        name: string;
        onChange: (item: {componentDefinition: {name: string; version: number}}) => void;
        value?: string;
    }) => (
        <div data-testid="combo-box">
            <select
                data-testid={`combo-box-select-${name}`}
                onChange={(event) => {
                    const selectedItem = items.find((item) => item.value === event.target.value);

                    if (selectedItem) {
                        onChange({componentDefinition: {name: selectedItem.value, version: 1}});
                    }
                }}
                value={value}
            >
                {items.map((item) => (
                    <option key={item.value} value={item.value}>
                        {item.label}
                    </option>
                ))}
            </select>
        </div>
    ),
}));

vi.mock('@/components/ui/form', () => ({
    Form: ({children, ...props}: {children: React.ReactNode}) => <form {...props}>{children}</form>,
    FormControl: ({children}: {children: React.ReactNode}) => <div data-testid="form-control">{children}</div>,
    FormField: ({
        name,
        render,
    }: {
        control: unknown;
        name: string;
        render: (props: {field: {onBlur: () => void; value: string}}) => React.ReactNode;
    }) => <div data-testid={`form-field-${name}`}>{render({field: {onBlur: vi.fn(), value: ''}})}</div>,
    FormItem: ({children}: {children: React.ReactNode}) => <div data-testid="form-item">{children}</div>,
    FormLabel: ({children}: {children: React.ReactNode}) => <label>{children}</label>,
    FormMessage: () => <span data-testid="form-message" />,
}));

vi.mock('@/components/ui/popover', () => ({
    Popover: ({children, open}: {children: React.ReactNode; onOpenChange: (open: boolean) => void; open: boolean}) => (
        <div data-open={open} data-testid="popover">
            {children}
        </div>
    ),
    PopoverContent: ({children}: {children: React.ReactNode}) => <div data-testid="popover-content">{children}</div>,
    PopoverTrigger: ({children}: {asChild?: boolean; children: React.ReactNode}) => (
        <div data-testid="popover-trigger">{children}</div>
    ),
}));

vi.mock('@radix-ui/react-popover', () => ({
    PopoverClose: ({children, onClick}: {asChild?: boolean; children: React.ReactNode; onClick?: () => void}) => (
        <div data-testid="popover-close" onClick={onClick}>
            {children}
        </div>
    ),
}));

describe('PropertyCodeEditorDialogRightPanelConnectionsPopover', () => {
    const defaultProps = {
        onSubmit: vi.fn(),
    };

    beforeEach(() => {
        windowResizeObserver();
        hoisted.storeState.open = false;
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    describe('rendering', () => {
        it('should render the popover', () => {
            render(<PropertyCodeEditorDialogRightPanelConnectionsPopover {...defaultProps} />);

            expect(screen.getByTestId('popover')).toBeInTheDocument();
        });

        it('should render the trigger button', () => {
            render(<PropertyCodeEditorDialogRightPanelConnectionsPopover {...defaultProps} />);

            expect(screen.getByTestId('popover-trigger')).toBeInTheDocument();
        });

        it('should render Add Component header', () => {
            render(<PropertyCodeEditorDialogRightPanelConnectionsPopover {...defaultProps} />);

            // There are two "Add Component" texts - one in header, one in button
            expect(screen.getAllByText('Add Component').length).toBeGreaterThanOrEqual(1);
        });

        it('should render name form field', () => {
            render(<PropertyCodeEditorDialogRightPanelConnectionsPopover {...defaultProps} />);

            expect(screen.getByTestId('form-field-name')).toBeInTheDocument();
            expect(screen.getByText('Name')).toBeInTheDocument();
        });

        it('should render componentName form field', () => {
            render(<PropertyCodeEditorDialogRightPanelConnectionsPopover {...defaultProps} />);

            expect(screen.getByTestId('form-field-componentName')).toBeInTheDocument();
            expect(screen.getByText('Component')).toBeInTheDocument();
        });

        it('should render Add button', () => {
            render(<PropertyCodeEditorDialogRightPanelConnectionsPopover {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Add'})).toBeInTheDocument();
        });
    });

    describe('custom trigger node', () => {
        it('should render custom trigger when provided', () => {
            render(
                <PropertyCodeEditorDialogRightPanelConnectionsPopover
                    {...defaultProps}
                    triggerNode={<button data-testid="custom-trigger">Custom Trigger</button>}
                />
            );

            expect(screen.getByTestId('custom-trigger')).toBeInTheDocument();
        });
    });

    describe('form submission', () => {
        it('should call onSubmit when form is submitted', async () => {
            const user = userEvent.setup();
            const mockOnSubmit = vi.fn();

            render(<PropertyCodeEditorDialogRightPanelConnectionsPopover {...defaultProps} onSubmit={mockOnSubmit} />);

            await user.click(screen.getByRole('button', {name: 'Add'}));

            expect(mockOnSubmit).toHaveBeenCalledWith({
                componentName: 'slack',
                componentVersion: 1,
                name: 'mySlack',
            });
        });
    });

    describe('close button', () => {
        it('should render close button', () => {
            render(<PropertyCodeEditorDialogRightPanelConnectionsPopover {...defaultProps} />);

            expect(screen.getByTestId('popover-close')).toBeInTheDocument();
        });
    });
});
