import {type Locator, type Page} from '@playwright/test';

export const DYNAMIC_PROPERTIES_LOOKUP_LABEL = 'dynamicPropertiesLookup property';
export const DYNAMIC_PROPERTY_LABEL = 'dynamicProperty property';

export const DYNAMIC_SUB_PROPERTY_ONE_LABEL = 'dynamicProperty1 property';
export const DYNAMIC_SUB_PROPERTY_TWO_LABEL = 'dynamicProperty2 property';

export const DYNAMIC_SUB_PROPERTY_ONE_NAME = 'dynamicProperty1';

// GET issued to resolve the dynamic sub-properties once the lookup dependency has a value.
const DYNAMIC_PROPERTIES_URL_FRAGMENT = '/workflow-nodes/propertyTesting_1/dynamic-properties/dynamicProperty';

export const DYNAMIC_PROPERTIES_ROUTE = /\/workflow-nodes\/propertyTesting_1\/dynamic-properties\/dynamicProperty/;

export function getDynamicPropertyContainer(configurationPanel: Locator): Locator {
    return configurationPanel.getByLabel(DYNAMIC_PROPERTY_LABEL, {exact: true});
}

export function dynamicPropertiesRequestPromise(page: Page) {
    return page.waitForRequest(
        (request) => request.method() === 'GET' && request.url().includes(DYNAMIC_PROPERTIES_URL_FRAGMENT)
    );
}

interface SetDynamicPropertiesLookupValueProps {
    configurationPanel: Locator;
    page: Page;
    value: string;
    waitForRequest?: boolean;
}

export async function setDynamicPropertiesLookupValue({
    configurationPanel,
    page,
    value,
    waitForRequest = true,
}: SetDynamicPropertiesLookupValueProps): Promise<void> {
    const lookupProperty = configurationPanel.getByLabel(DYNAMIC_PROPERTIES_LOOKUP_LABEL, {exact: true});

    const lookupInput = lookupProperty.getByRole('textbox');

    const requestPromise = waitForRequest ? dynamicPropertiesRequestPromise(page) : undefined;

    await lookupInput.clear();
    await lookupInput.fill(value);

    if (requestPromise) {
        await requestPromise;
    }
}
