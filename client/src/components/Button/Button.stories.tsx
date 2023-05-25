import {MixIcon} from '@radix-ui/react-icons';
import {ComponentMeta, ComponentStory} from '@storybook/react';
import React from 'react';

import Button from './Button';

export default {
    component: Button,
    title: 'Button',
} as ComponentMeta<typeof Button>;

const Template: ComponentStory<typeof Button> = (args) => <Button {...args} />;

export const Primary = Template.bind({});

Primary.args = {
    label: 'Button',
};

export const Secondary = Template.bind({});

Secondary.args = {
    displayType: 'secondary',
    label: 'Button',
};

export const Danger = Template.bind({});

Danger.args = {
    displayType: 'danger',
    label: 'Button',
};
export const Unstyled = Template.bind({});

Unstyled.args = {
    displayType: 'unstyled',
    label: 'Button',
};

export const Large = Template.bind({});

Large.args = {
    label: 'Button',
    size: 'large',
};

export const Small = Template.bind({});

Small.args = {
    label: 'Button',
    size: 'small',
};
export const Icon = Template.bind({});

Icon.storyName = 'Button with an icon';
Icon.args = {
    icon: <MixIcon />,
};
