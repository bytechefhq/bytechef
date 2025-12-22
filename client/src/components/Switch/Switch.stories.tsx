/* eslint-disable react-hooks/rules-of-hooks, sort-keys */
import {Meta, StoryObj} from '@storybook/react';
import * as React from 'react';

import Switch from './Switch';

const labels = {
    'Label 1': 'Enable notifications',
    'Label 2': 'Dark mode',
    'Label 3': 'Auto-save',
    'Label 4': 'Public profile',
} as const;

const descriptions = {
    'Description 1': 'Receive email notifications for important updates',
    'Description 2': 'Switch to dark theme for better visibility',
    'Description 3': 'Automatically save your work every 5 minutes',
    'Description 4': 'Make your profile visible to other users',
} as const;

const meta = {
    title: 'Components/Switch',
    component: Switch,
    parameters: {
        controls: {
            include: ['variant', 'checked', 'label', 'description', 'alignment', 'disabled'],
        },
        docs: {
            description: {
                component:
                    'A customizable switch component with various variants and alignment options. Built with shadcn/ui and uses arbitrary Tailwind variants for precise styling control. Supports labeled switches with optional descriptions, plain switches without labels, and different visual variants (default, box, small).',
            },
        },
        layout: 'centered',
    },
    argTypes: {
        alignment: {
            control: {type: 'select'},
            description:
                'Alignment of the switch relative to the label - **only** applies when label is provided. "start" places switch before label, "end" places switch after label.',
            if: {arg: 'label', neq: undefined},
            options: ['start', 'end'],
            table: {
                defaultValue: {summary: 'start'},
                type: {summary: 'start | end'},
            },
        },
        checked: {
            control: {type: 'boolean'},
            description: 'Whether the switch is in the checked (on) state',
            table: {
                defaultValue: {summary: 'false'},
                type: {summary: 'boolean'},
            },
        },
        description: {
            control: {type: 'select'},
            defaultValue: 'no-description',
            description:
                'Optional description text displayed below the label - **only** applies when label is provided and variant is not "small".',
            if: {arg: 'label', neq: undefined},
            mapping: {'no-description': undefined, ...descriptions},
            options: ['no-description', ...Object.keys(descriptions)],
            table: {
                type: {summary: 'ReactNode'},
            },
        },
        disabled: {
            control: {type: 'boolean'},
            description: 'Whether the switch is disabled',
            table: {
                defaultValue: {summary: 'false'},
                type: {summary: 'boolean'},
            },
        },
        label: {
            control: {type: 'select'},
            description:
                'Label text displayed next to the switch - when provided, the switch becomes a labeled switch with optional description and alignment options.',
            mapping: {'no-label': undefined, ...labels},
            options: ['no-label', ...Object.keys(labels)],
            table: {
                type: {summary: 'ReactNode'},
            },
        },
        variant: {
            control: {type: 'select'},
            description:
                'Visual variant of the switch - "default" and "box" are similar in size but "box" has a bordered container, "small" is a compact variant with rounded-[7px] border radius.',
            options: ['default', 'box', 'small'],
            table: {
                defaultValue: {summary: 'default'},
                type: {summary: 'default | box | small'},
            },
        },
    },
    tags: ['autodocs'],
} satisfies Meta<typeof Switch>;

export default meta;
// eslint-disable-next-line @typescript-eslint/naming-convention
type Story = StoryObj<typeof Switch>;

export const DefaultSwitch: Story = {
    args: {
        alignment: 'start',
        checked: false,
        description: undefined,
        disabled: false,
        label: 'Label 1',
        variant: 'default',
    },
    render: function Render(args) {
        const [checked, setChecked] = React.useState(args.checked ?? false);

        React.useEffect(() => {
            setChecked(args.checked ?? false);
        }, [args.checked]);

        return <Switch {...args} checked={checked} onCheckedChange={setChecked} />;
    },
};

export const SwitchVariants: Story = {
    parameters: {
        docs: {
            description: {
                story: 'Visual comparison of available switch variants. Click to toggle!',
            },
        },
    },
    render: () => {
        const [checked1, setChecked1] = React.useState(false);
        const [checked2, setChecked2] = React.useState(false);
        const [checked3, setChecked3] = React.useState(false);

        return (
            <div className="grid grid-cols-[140px_1fr] gap-x-6 gap-y-4 text-sm">
                <span className="col-start-2 font-semibold">Example</span>

                <span className="font-medium">Default</span>

                <Switch
                    checked={checked1}
                    label="Enable notifications"
                    onCheckedChange={setChecked1}
                    variant="default"
                />

                <span className="font-medium">Box</span>

                <Switch checked={checked2} label="Enable notifications" onCheckedChange={setChecked2} variant="box" />

                <span className="font-medium">Small</span>

                <Switch checked={checked3} label="Enable" onCheckedChange={setChecked3} variant="small" />
            </div>
        );
    },
};

export const PlainSwitches: Story = {
    parameters: {
        docs: {
            description: {
                story: 'Plain switches without labels - can be used standalone or in custom layouts. Remember to provide aria-label for accessibility.',
            },
        },
    },
    render: () => (
        <div className="flex flex-col gap-4">
            <div className="flex items-center gap-4">
                <Switch aria-label="Default switch" variant="default" />

                <Switch aria-label="Small switch" variant="small" />
            </div>

            <div className="flex items-center gap-4">
                <Switch aria-label="Default switch checked" checked variant="default" />

                <Switch aria-label="Small switch checked" checked variant="small" />
            </div>
        </div>
    ),
};

export const SwitchAlignmentVariants: Story = {
    parameters: {
        docs: {
            description: {
                story: 'Comparison of switch alignment options across variants. "start" places the switch before the label, "end" places it after.',
            },
        },
    },
    render: () => {
        const label = 'Auto-save';
        const [defaultStart, setDefaultStart] = React.useState(false);
        const [defaultEnd, setDefaultEnd] = React.useState(false);
        const [boxStart, setBoxStart] = React.useState(false);
        const [boxEnd, setBoxEnd] = React.useState(false);
        const [smallStart, setSmallStart] = React.useState(false);
        const [smallEnd, setSmallEnd] = React.useState(false);

        return (
            <div className="grid grid-cols-[120px_1fr_1fr] gap-4 text-sm">
                <span className="col-start-2 font-semibold">Start</span>

                <span className="font-semibold">End</span>

                <span className="font-medium">Default</span>

                <Switch
                    alignment="start"
                    checked={defaultStart}
                    label={label}
                    onCheckedChange={setDefaultStart}
                    variant="default"
                />

                <Switch
                    alignment="end"
                    checked={defaultEnd}
                    label={label}
                    onCheckedChange={setDefaultEnd}
                    variant="default"
                />

                <span className="font-medium">Box</span>

                <Switch
                    alignment="start"
                    checked={boxStart}
                    label={label}
                    onCheckedChange={setBoxStart}
                    variant="box"
                />

                <Switch alignment="end" checked={boxEnd} label={label} onCheckedChange={setBoxEnd} variant="box" />

                <span className="font-medium">Small</span>

                <Switch
                    alignment="start"
                    checked={smallStart}
                    label={label}
                    onCheckedChange={setSmallStart}
                    variant="small"
                />

                <Switch
                    alignment="end"
                    checked={smallEnd}
                    label={label}
                    onCheckedChange={setSmallEnd}
                    variant="small"
                />
            </div>
        );
    },
};

export const SwitchesWithDescriptions: Story = {
    parameters: {
        docs: {
            description: {
                story: 'Comparison of switch variants with descriptions. Descriptions are shown for default and box variants, but not for the small variant.',
            },
        },
    },
    render: () => {
        const [checked1, setChecked1] = React.useState(false);
        const [checked2, setChecked2] = React.useState(false);
        const [checked3, setChecked3] = React.useState(false);

        return (
            <div className="grid grid-cols-[160px_1fr] gap-x-6 gap-y-4 text-sm">
                <span className="col-start-2 font-semibold">Example</span>

                <span className="font-medium">Default</span>

                <Switch
                    checked={checked1}
                    description="Receive email notifications for important updates"
                    label="Enable notifications"
                    onCheckedChange={setChecked1}
                    variant="default"
                />

                <span className="font-medium">Box</span>

                <Switch
                    checked={checked2}
                    description="Switch to dark theme for better visibility"
                    label="Dark mode"
                    onCheckedChange={setChecked2}
                    variant="box"
                />

                <span className="font-medium">Small (no description)</span>

                <Switch checked={checked3} label="Enable notifications" onCheckedChange={setChecked3} variant="small" />
            </div>
        );
    },
};

export const SwitchStates: Story = {
    parameters: {
        docs: {
            description: {
                story: 'Comparison of switch states (unchecked vs checked) across variants. Notice the box variant changes background color when checked.',
            },
        },
    },
    render: () => (
        <div className="grid grid-cols-[140px_1fr_1fr] gap-x-6 gap-y-4 text-sm">
            <span className="col-start-2 font-semibold">Unchecked</span>

            <span className="font-semibold">Checked</span>

            <span className="font-medium">Default</span>

            <Switch label="Unchecked" variant="default" />

            <Switch checked label="Checked" variant="default" />

            <span className="font-medium">Box</span>

            <Switch label="Unchecked" variant="box" />

            <Switch checked label="Checked" variant="box" />

            <span className="font-medium">Small</span>

            <Switch label="Unchecked" variant="small" />

            <Switch checked label="Checked" variant="small" />
        </div>
    ),
};

export const SwitchDisabledStates: Story = {
    parameters: {
        docs: {
            description: {
                story: 'Disabled states for different switch variants and values. Disabled switches cannot be toggled.',
            },
        },
    },
    render: () => (
        <div className="grid grid-cols-[160px_1fr_1fr] gap-x-6 gap-y-4 text-sm">
            <span className="col-start-2 font-semibold">Disabled off</span>

            <span className="font-semibold">Disabled on</span>

            <span className="font-medium">Default</span>

            <Switch disabled label="Off" variant="default" />

            <Switch checked disabled label="On" variant="default" />

            <span className="font-medium">Box</span>

            <Switch disabled label="Off" variant="box" />

            <Switch checked disabled label="On" variant="box" />

            <span className="font-medium">Small</span>

            <Switch disabled label="Off" variant="small" />

            <Switch checked disabled label="On" variant="small" />

            <span className="font-medium">Plain</span>

            <Switch aria-label="Disabled off" disabled />

            <Switch aria-label="Disabled on" checked disabled />
        </div>
    ),
};

export const SwitchUseCases: Story = {
    parameters: {
        docs: {
            description: {
                story: 'Common real-world use cases for switches in different contexts.',
            },
        },
    },
    render: () => {
        const [emailNotifications, setEmailNotifications] = React.useState(true);
        const [publicProfile, setPublicProfile] = React.useState(false);
        const [autoSave1, setAutoSave1] = React.useState(true);

        const [darkMode, setDarkMode] = React.useState(true);
        const [notifications, setNotifications] = React.useState(false);
        const [autoSave2, setAutoSave2] = React.useState(true);

        const [advancedMode, setAdvancedMode] = React.useState(true);
        const [betaFeatures, setBetaFeatures] = React.useState(false);

        return (
            <div className="grid gap-8 text-sm">
                <div>
                    <h3 className="mb-3 font-semibold">Settings panel</h3>

                    <div className="grid gap-4 rounded-lg border border-stroke-neutral-secondary p-4">
                        <Switch
                            checked={emailNotifications}
                            description="Receive email notifications for important updates"
                            label="Email notifications"
                            onCheckedChange={setEmailNotifications}
                        />

                        <Switch
                            checked={publicProfile}
                            description="Allow other users to see your profile"
                            label="Public profile"
                            onCheckedChange={setPublicProfile}
                        />

                        <Switch
                            checked={autoSave1}
                            description="Automatically save your work"
                            label="Auto-save"
                            onCheckedChange={setAutoSave1}
                        />
                    </div>
                </div>

                <div>
                    <h3 className="mb-3 font-semibold">Compact settings (small variant)</h3>

                    <div className="grid max-w-sm gap-2">
                        <Switch checked={darkMode} label="Dark mode" onCheckedChange={setDarkMode} variant="small" />

                        <Switch
                            checked={notifications}
                            label="Notifications"
                            onCheckedChange={setNotifications}
                            variant="small"
                        />

                        <Switch checked={autoSave2} label="Auto-save" onCheckedChange={setAutoSave2} variant="small" />
                    </div>
                </div>

                <div>
                    <h3 className="mb-3 font-semibold">Feature toggles (box variant)</h3>

                    <div className="grid max-w-md gap-3">
                        <Switch
                            checked={advancedMode}
                            description="Enable advanced features and options"
                            label="Advanced mode"
                            onCheckedChange={setAdvancedMode}
                            variant="box"
                        />

                        <Switch
                            checked={betaFeatures}
                            description="Enable beta and experimental options"
                            label="Beta features"
                            onCheckedChange={setBetaFeatures}
                            variant="box"
                        />
                    </div>
                </div>
            </div>
        );
    },
};
