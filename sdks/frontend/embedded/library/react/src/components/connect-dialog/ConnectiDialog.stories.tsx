import type {Meta} from '@storybook/react';

import ConnectDialog from './ConnectDialog.tsx';

const meta = {
    title: 'Components/ConnectDialog',
    component: ConnectDialog,
    parameters: {
        layout: 'centered',
    },
    tags: ['autodocs'],
    args: {},
} satisfies Meta<typeof ConnectDialog>;

export default meta;
