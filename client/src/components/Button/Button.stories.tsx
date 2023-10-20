import {MixIcon} from '@radix-ui/react-icons';
import {ComponentMeta, ComponentStory} from '@storybook/react';
import React from 'react';

import Button from './Button';

export default {
    title: 'Button',
    component: Button,
} as ComponentMeta<typeof Button>;

const Template: ComponentStory<typeof Button> = (args) => <Button {...args} />;

export const Primary = Template.bind({});

Primary.args = {
    label: 'Button',
};

export const Secondary = Template.bind({});

Secondary.args = {
    label: 'Button',
    displayType: 'secondary',
};

export const Danger = Template.bind({});

Danger.args = {
    label: 'Button',
    displayType: 'danger',
};
export const Unstyled = Template.bind({});

Unstyled.args = {
    label: 'Button',
    displayType: 'unstyled',
};

export const Large = Template.bind({});

Large.args = {
    size: 'large',
    label: 'Button',
};

export const Small = Template.bind({});

Small.args = {
    size: 'small',
    label: 'Button',
};
export const Icon = Template.bind({});

Icon.storyName = 'Button with an icon';
Icon.args = {
    icon: <MixIcon />,
};
