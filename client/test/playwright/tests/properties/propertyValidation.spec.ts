import {type Locator, expect, mergeTests} from '@playwright/test';

import {ERROR_MESSAGES} from '../../../../src/shared/errorMessages';
import {importWorkflowTest, loginTest, projectTest} from '../../fixtures';
import {
    assertPropertyValidation,
    fillPropertyInput,
    openPropertyTestingPanelAndPropertiesTab,
} from '../../utils/propertyValidationUtils';

export const test = mergeTests(loginTest(), projectTest, importWorkflowTest);

const ARRAY_MAX_ITEMS = 3;

test.describe('Property validation - string', () => {
    let configurationPanel: Locator;

    test.beforeEach(async ({authenticatedPage: page, project, workflow}) => {
        configurationPanel = await openPropertyTestingPanelAndPropertiesTab(
            page,
            project.id,
            workflow.workflowId,
            'stringRegEx property'
        );
    });

    test.describe('String Regular Expression', () => {
        test('should show validation error when value does not match regex (only letters allowed)', async () => {
            await test.step('Enter invalid value', async () => {
                await fillPropertyInput(configurationPanel, 'stringRegEx property', '123');
            });

            await test.step('Assert validation error', async () => {
                await assertPropertyValidation(
                    configurationPanel,
                    'stringRegEx property',
                    ERROR_MESSAGES.PROPERTY.VALUE_DOES_NOT_MATCH_PATTERN
                );
            });
        });

        test('should clear regex validation error when value matches (letters only)', async () => {
            await test.step('Enter invalid value', async () => {
                await fillPropertyInput(configurationPanel, 'stringRegEx property', '12');
            });

            await test.step('Assert validation error', async () => {
                await assertPropertyValidation(
                    configurationPanel,
                    'stringRegEx property',
                    ERROR_MESSAGES.PROPERTY.VALUE_DOES_NOT_MATCH_PATTERN
                );
            });

            await test.step('Enter valid value', async () => {
                await fillPropertyInput(configurationPanel, 'stringRegEx property', 'ab');
            });

            await test.step('Assert no validation error', async () => {
                await assertPropertyValidation(configurationPanel, 'stringRegEx property');
            });
        });

        test('should not show validation error when value matches regex', async () => {
            await test.step('Enter valid value', async () => {
                await fillPropertyInput(configurationPanel, 'stringRegEx property', 'abc');
            });

            await test.step('Assert no validation error', async () => {
                await assertPropertyValidation(configurationPanel, 'stringRegEx property');
            });
        });
    });

    test.describe('String Min Length', () => {
        test('should show validation error when string length is below minLength', async () => {
            await test.step('Enter value below minLength', async () => {
                await fillPropertyInput(configurationPanel, 'stringMinLength property', 'a');
            });

            await test.step('Assert validation error', async () => {
                await assertPropertyValidation(
                    configurationPanel,
                    'stringMinLength property',
                    ERROR_MESSAGES.PROPERTY.INCORRECT_VALUE
                );
            });
        });

        test('should not show validation error when string length meets minLength', async () => {
            await test.step('Enter value meeting minLength', async () => {
                await fillPropertyInput(configurationPanel, 'stringMinLength property', 'abcde');
            });

            await test.step('Assert no validation error', async () => {
                await assertPropertyValidation(configurationPanel, 'stringMinLength property');
            });
        });
    });

    test.describe('String Max Length', () => {
        test('should show validation error when string length exceeds maxLength', async () => {
            await test.step('Enter value exceeding maxLength', async () => {
                await fillPropertyInput(configurationPanel, 'stringMaxLength property', 'abcdef');
            });

            await test.step('Assert validation error', async () => {
                await assertPropertyValidation(
                    configurationPanel,
                    'stringMaxLength property',
                    ERROR_MESSAGES.PROPERTY.INCORRECT_VALUE
                );
            });
        });

        test('should not show validation error when string length is within maxLength', async () => {
            await test.step('Enter value within maxLength', async () => {
                await fillPropertyInput(configurationPanel, 'stringMaxLength property', 'abc');
            });

            await test.step('Assert no validation error', async () => {
                await assertPropertyValidation(configurationPanel, 'stringMaxLength property');
            });
        });
    });
});

test.describe('Property validation - numeric', () => {
    let configurationPanel: Locator;

    test.beforeEach(async ({authenticatedPage: page, project, workflow}) => {
        configurationPanel = await openPropertyTestingPanelAndPropertiesTab(
            page,
            project.id,
            workflow.workflowId,
            'integerMaxValue property'
        );
    });

    test.describe('Integer Max Value', () => {
        test('should show validation error when value exceeds maxValue', async () => {
            await test.step('Enter value exceeding maxValue', async () => {
                await fillPropertyInput(configurationPanel, 'integerMaxValue property', '11');
            });

            await test.step('Assert validation error', async () => {
                await assertPropertyValidation(
                    configurationPanel,
                    'integerMaxValue property',
                    ERROR_MESSAGES.PROPERTY.INCORRECT_VALUE
                );
            });
        });

        test('should not show validation error when value is within maxValue', async () => {
            await test.step('Enter value within maxValue', async () => {
                await fillPropertyInput(configurationPanel, 'integerMaxValue property', '10');
            });

            await test.step('Assert no validation error', async () => {
                await assertPropertyValidation(configurationPanel, 'integerMaxValue property');
            });
        });
    });

    test.describe('Integer Min Value', () => {
        test('should show validation error when value is below minValue', async () => {
            await test.step('Enter value below minValue', async () => {
                await fillPropertyInput(configurationPanel, 'integerMinValue property', '9');
            });

            await test.step('Assert validation error', async () => {
                await assertPropertyValidation(
                    configurationPanel,
                    'integerMinValue property',
                    ERROR_MESSAGES.PROPERTY.INCORRECT_VALUE
                );
            });
        });

        test('should not show validation error when value meets minValue', async () => {
            await test.step('Enter value meeting minValue', async () => {
                await fillPropertyInput(configurationPanel, 'integerMinValue property', '10');
            });

            await test.step('Assert no validation error', async () => {
                await assertPropertyValidation(configurationPanel, 'integerMinValue property');
            });
        });
    });

    test.describe('Number Max Value', () => {
        test('should show validation error when value exceeds maxValue', async () => {
            await test.step('Enter value exceeding maxValue', async () => {
                await fillPropertyInput(configurationPanel, 'numberMaxValue property', '6');
            });

            await test.step('Assert validation error', async () => {
                await assertPropertyValidation(
                    configurationPanel,
                    'numberMaxValue property',
                    ERROR_MESSAGES.PROPERTY.INCORRECT_VALUE
                );
            });
        });

        test('should not show validation error when value is within maxValue', async () => {
            await test.step('Enter value within maxValue', async () => {
                await fillPropertyInput(configurationPanel, 'numberMaxValue property', '5');
            });

            await test.step('Assert no validation error', async () => {
                await assertPropertyValidation(configurationPanel, 'numberMaxValue property');
            });
        });
    });

    test.describe('Number Min Value', () => {
        test('should show validation error when value is below minValue', async () => {
            await test.step('Enter value below minValue', async () => {
                await fillPropertyInput(configurationPanel, 'numberMinValue property', '4');
            });

            await test.step('Assert validation error', async () => {
                await assertPropertyValidation(
                    configurationPanel,
                    'numberMinValue property',
                    ERROR_MESSAGES.PROPERTY.INCORRECT_VALUE
                );
            });
        });

        test('should not show validation error when value meets minValue', async () => {
            await test.step('Enter value meeting minValue', async () => {
                await fillPropertyInput(configurationPanel, 'numberMinValue property', '5');
            });

            await test.step('Assert no validation error', async () => {
                await assertPropertyValidation(configurationPanel, 'numberMinValue property');
            });
        });
    });

    test.describe('Number max decimal places', () => {
        test('should show validation error when decimal places exceed maxNumberPrecision', async () => {
            await test.step('Enter value with too many decimal places', async () => {
                await fillPropertyInput(configurationPanel, 'numberMaxNumPrecision property', '1.123');
            });

            await test.step('Assert validation error', async () => {
                await assertPropertyValidation(
                    configurationPanel,
                    'numberMaxNumPrecision property',
                    ERROR_MESSAGES.PROPERTY.MAX_DECIMAL_PLACES(2)
                );
            });
        });

        test('should not show validation error when decimal places are within maxNumberPrecision', async () => {
            await test.step('Enter value within max decimal places', async () => {
                await fillPropertyInput(configurationPanel, 'numberMaxNumPrecision property', '1.12');
            });

            await test.step('Assert no validation error', async () => {
                await assertPropertyValidation(configurationPanel, 'numberMaxNumPrecision property');
            });
        });
    });

    test.describe('Number min decimal places', () => {
        test('should show validation error when decimal places are below minNumberPrecision', async () => {
            await test.step('Enter value with too few decimal places', async () => {
                await fillPropertyInput(configurationPanel, 'numberMinNumPrecision property', '1.1');
            });

            await test.step('Assert validation error', async () => {
                await assertPropertyValidation(
                    configurationPanel,
                    'numberMinNumPrecision property',
                    ERROR_MESSAGES.PROPERTY.MIN_DECIMAL_PLACES(2)
                );
            });
        });

        test('should not show validation error when decimal places meet minNumberPrecision', async () => {
            await test.step('Enter value meeting min decimal places', async () => {
                await fillPropertyInput(configurationPanel, 'numberMinNumPrecision property', '1.12');
            });

            await test.step('Assert no validation error', async () => {
                await assertPropertyValidation(configurationPanel, 'numberMinNumPrecision property');
            });
        });
    });

    test.describe('Number precision', () => {
        test('should show validation error when decimal places exceed numberPrecision', async () => {
            await test.step('Enter value with too many decimal places', async () => {
                await fillPropertyInput(configurationPanel, 'numberPrecision property', '1.1234');
            });

            await test.step('Assert validation error', async () => {
                await assertPropertyValidation(
                    configurationPanel,
                    'numberPrecision property',
                    ERROR_MESSAGES.PROPERTY.MAX_DECIMAL_PLACES(3)
                );
            });
        });

        test('should not show validation error when decimal places are within numberPrecision', async () => {
            await test.step('Enter value within number precision', async () => {
                await fillPropertyInput(configurationPanel, 'numberPrecision property', '1.123');
            });

            await test.step('Assert no validation error', async () => {
                await assertPropertyValidation(configurationPanel, 'numberPrecision property');
            });
        });
    });
});

test.describe('Property validation - array', () => {
    let configurationPanel: Locator;

    test.beforeEach(async ({authenticatedPage: page, project, workflow}) => {
        configurationPanel = await openPropertyTestingPanelAndPropertiesTab(
            page,
            project.id,
            workflow.workflowId,
            'arrayMaxItems property'
        );
    });

    test.describe('Array Max Items', () => {
        test('should disable Add button when maxItems reached', async () => {
            const arrayProperty = configurationPanel.getByLabel('arrayMaxItems property');
            const addButton = arrayProperty.getByRole('button', {name: 'Add array item'});

            await test.step('Add items up to maxItems', async () => {
                await addButton.click();
                await addButton.click();
                await addButton.click();
            });

            await test.step('Verify item count and Add button disabled', async () => {
                const arrayItems = arrayProperty.getByLabel(/Array property item at index \d+/);
                await expect(arrayItems).toHaveCount(ARRAY_MAX_ITEMS);
                await expect(addButton).toBeDisabled();
            });
        });

        test('should allow adding items when below maxItems', async () => {
            const arrayProperty = configurationPanel.getByLabel('arrayMaxItems property');
            const addButton = arrayProperty.getByRole('button', {name: 'Add array item'});

            await test.step('Verify Add button enabled', async () => {
                await expect(addButton).toBeEnabled();
            });

            await test.step('Add item and verify visible', async () => {
                await addButton.click();
                await expect(arrayProperty.getByLabel('Array property item at index 0')).toBeVisible();
            });
        });
    });

    test.describe('Array Min Items', () => {
        test('should show Add button when array is empty (below minItems)', async () => {
            const arrayProperty = configurationPanel.getByLabel('arrayMinItems property');

            await test.step('Verify no items and Add button visible', async () => {
                const arrayItems = arrayProperty.getByLabel(/Array property item at index \d+/);
                await expect(arrayItems).toHaveCount(0);
                const addButton = arrayProperty.getByRole('button', {name: 'Add array item'});
                await expect(addButton).toBeEnabled();
            });
        });

        test('should allow adding items until minItems reached and Add stays enabled', async () => {
            const arrayProperty = configurationPanel.getByLabel('arrayMinItems property');
            const addButton = arrayProperty.getByRole('button', {name: 'Add array item'});

            await test.step('Add two items', async () => {
                await addButton.click();
                await addButton.click();
            });

            await test.step('Verify both items visible and Add still enabled', async () => {
                await expect(arrayProperty.getByLabel('Array property item at index 0')).toBeVisible();
                await expect(arrayProperty.getByLabel('Array property item at index 1')).toBeVisible();
                await expect(addButton).toBeEnabled();
            });
        });
    });
});
