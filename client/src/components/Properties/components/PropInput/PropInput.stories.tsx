import {ComponentMeta, ComponentStory} from '@storybook/react';

import PropInput from './PropInput';

// More on default export: https://storybook.js.org/docs/react/writing-stories/introduction#default-export
export default {
    // More on argTypes: https://storybook.js.org/docs/react/api/argtypes
    argTypes: {
        type: {defaultValue: 'text'},
    },
    component: PropInput,
    title: 'Input',
} as ComponentMeta<typeof PropInput>;

// More on component templates: https://storybook.js.org/docs/react/writing-stories/introduction#using-args
const Template: ComponentStory<typeof PropInput> = (args) => <PropInput {...args} />;

export const Normal = Template.bind({});
// More on args: https://storybook.js.org/docs/react/writing-stories/args
Normal.args = {
    label: 'Name',
    name: 'name',
};
