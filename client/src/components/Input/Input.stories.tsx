import {ComponentMeta, ComponentStory} from '@storybook/react';
import React from 'react';

import Input from './Input';

// More on default export: https://storybook.js.org/docs/react/writing-stories/introduction#default-export
export default {
    // More on argTypes: https://storybook.js.org/docs/react/api/argtypes
    argTypes: {
        type: {defaultValue: 'text'},
    },
    component: Input,
    title: 'Input',
} as ComponentMeta<typeof Input>;

// More on component templates: https://storybook.js.org/docs/react/writing-stories/introduction#using-args
const Template: ComponentStory<typeof Input> = (args) => <Input {...args} />;

export const Normal = Template.bind({});
// More on args: https://storybook.js.org/docs/react/writing-stories/args
Normal.args = {
    label: 'Name',
    name: 'name',
};
