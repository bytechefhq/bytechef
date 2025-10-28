import {Meta, StoryFn} from '@storybook/react-vite';

import PropertyInput from './PropertyInput';

// More on default export: https://storybook.js.org/docs/react/writing-stories/introduction#default-export
export default {
    // More on argTypes: https://storybook.js.org/docs/react/api/argtypes
    argTypes: {
        type: {defaultValue: 'text'},
    },
    component: PropertyInput,
    title: 'Input',
} as Meta<typeof PropertyInput>;

// More on component templates: https://storybook.js.org/docs/react/writing-stories/introduction#using-args
/* eslint-disable @typescript-eslint/no-explicit-any */
const Template: StoryFn<typeof PropertyInput> = (args: any) => <PropertyInput {...args} />;

export const Normal = Template.bind({});
// More on args: https://storybook.js.org/docs/react/writing-stories/args
Normal.args = {
    label: 'Name',
    name: 'name',
};
