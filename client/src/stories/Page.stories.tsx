import {Meta, StoryFn} from '@storybook/react';
import {userEvent, within} from '@storybook/testing-library';
import React from 'react';

import {Page} from './Page';

export default {
    component: Page,
    parameters: {
        // More on Story layout: https://storybook.js.org/docs/react/configure/story-layout
        layout: 'fullscreen',
    },
    title: 'Example/Page',
} as Meta<typeof Page>;

const Template: StoryFn<typeof Page> = (args) => <Page {...args} />;

export const LoggedOut = Template.bind({});

export const LoggedIn = Template.bind({});

// More on interaction testing: https://storybook.js.org/docs/react/writing-tests/interaction-testing
LoggedIn.play = async ({canvasElement}) => {
    const canvas = within(canvasElement);
    const loginButton = await canvas.getByRole('button', {name: /Log in/i});
    await userEvent.click(loginButton);
};
