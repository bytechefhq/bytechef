import {type Page, expect, mergeTests} from '@playwright/test';

import {importWorkflowTest, loginTest, projectTest} from '../../fixtures';
import {WorkflowPage} from '../../pages/workflowPage';
import {clickAndExpectToBeVisible} from '../../utils/clickAndExpectToBeVisible';
import {type TestProjectI, type TestWorkflowI} from '../../utils/projectUtils';
import {
    addObjectSubPropertyViaPopover,
    getWorkflowDefinition,
    openPropertiesTab,
    reopenConfigurationPanel,
} from '../../utils/workflowUtils';

export const test = mergeTests(loginTest(), projectTest, importWorkflowTest);

test.describe('ObjectProperty - Object property type (ObjectProperty.tsx)', () => {
    let authenticatedPage: Page;
    let project: TestProjectI;
    let workflow: TestWorkflowI;
    let workflowPage: WorkflowPage;

    test.beforeEach(async ({authenticatedPage: page, project: testProject, workflow: testWorkflow}) => {
        authenticatedPage = page;
        project = testProject;
        workflow = testWorkflow;

        workflowPage = new WorkflowPage(authenticatedPage);

        await test.step('Navigate to workflow editor and wait for load', async () => {
            await workflowPage.goToWorkflowEditor(project.id, workflow.workflowId);
        });

        await test.step('Open var_1 node configuration panel', async () => {
            await clickAndExpectToBeVisible({
                target: workflowPage.firstTaskComponentConfigurationPanel,
                trigger: workflowPage.firstNode,
            });
        });

        await test.step('Switch to the Properties tab and expand value', async () => {
            await openPropertiesTab(workflowPage.firstTaskComponentConfigurationPanel);

            await expect(workflowPage.parentObjectSubPropertyList).toBeVisible();
        });
    });

    test.describe('Rendering from workflow definition', () => {
        test('should render top-level subproperties equal to keys found in the definition', async () => {
            expect(workflowPage.objectPropertyParameters).toBeDefined();

            await expect(workflowPage.subPropertyListItems).toHaveCount(
                workflowPage.objectPropertyParameterKeys.length
            );
        });

        test('should verify workflow definition metadata dynamicPropertyTypes includes a type entry for each top-level value key', async () => {
            expect(workflowPage.dynamicPropertyTypes).toBeDefined();

            for (const propertyName of workflowPage.objectPropertyParameterKeys) {
                const metadataKey = `value.${propertyName}`;

                expect(workflowPage.dynamicPropertyTypes![metadataKey]).toBeDefined();
            }
        });

        test('should verify subproperties render with or without their default values', async () => {
            const expectedValues = WorkflowPage.getExpectedPropertyValues(workflowPage.workflowDefinition, 'var_1');

            await test.step('STRING subProperty', async () => {
                const stringProperty = workflowPage.firstTaskComponentConfigurationPanel.getByLabel('String property');

                const stringInput = stringProperty.getByRole('textbox');

                await expect(stringInput).toHaveText(expectedValues.String);
            });

            await test.step('INTEGER subProperty', async () => {
                const integerProperty =
                    workflowPage.firstTaskComponentConfigurationPanel.getByLabel('Integer property');

                const integerInput = integerProperty.getByRole('textbox');

                await expect(integerInput).toHaveValue(expectedValues.Integer);
            });

            await test.step('NUMBER subProperty', async () => {
                const numberProperty = workflowPage.firstTaskComponentConfigurationPanel.getByLabel('Number property');

                const numberInput = numberProperty.getByRole('textbox');

                await expect(numberInput).toHaveValue(expectedValues.Number);
            });

            await test.step('BOOLEAN subProperty', async () => {
                const booleanProperty =
                    workflowPage.firstTaskComponentConfigurationPanel.getByLabel('Boolean property');

                const booleanSelect = booleanProperty.getByRole('combobox');

                await expect(booleanSelect).toHaveText(expectedValues.Boolean);
            });

            await test.step('DATA subProperty', async () => {
                const dateProperty = workflowPage.firstTaskComponentConfigurationPanel.getByLabel('Date property');

                const dateInput = dateProperty.locator('input[type="date"]');

                await expect(dateInput).toHaveValue(expectedValues.Date);
            });

            await test.step('DATE_TIME subProperty', async () => {
                const dateTimeProperty =
                    workflowPage.firstTaskComponentConfigurationPanel.getByLabel('DateTime property');

                const dateTimeInput = dateTimeProperty.locator('input[type="datetime-local"]');

                await expect(dateTimeInput).toHaveValue(expectedValues.DateTime);
            });

            await test.step('TIME subProperty', async () => {
                const timeProperty = workflowPage.firstTaskComponentConfigurationPanel.getByLabel('Time property');

                const timeInput = timeProperty.locator('input[type="time"]');

                await expect(timeInput).toHaveValue(expectedValues.Time);
            });

            await test.step('FILE_ENTRY entry subProperty', async () => {
                const fileEntryProperty =
                    workflowPage.firstTaskComponentConfigurationPanel.getByLabel('FileEntry property');

                await expect(fileEntryProperty).toBeVisible();
                await expect(fileEntryProperty.getByRole('textbox')).toBeVisible();
            });

            await test.step('ARRAY subProperty', async () => {
                await expect(workflowPage.arrayProperty).toBeVisible();

                const arrayPropertyItem = workflowPage.arrayProperty.getByLabel('Array property item at index 0');

                const arrayItemInput = arrayPropertyItem.getByRole('textbox');

                await expect(arrayItemInput).toHaveText(expectedValues.Array);
            });

            await test.step('OBJECT subProperty', async () => {
                const objectProperty = workflowPage.firstTaskComponentConfigurationPanel.getByRole('listitem', {
                    name: 'Object property',
                });

                const nestedObjectList = objectProperty.locator('ul[aria-label="Object object properties"]');

                await expect(objectProperty).toBeVisible();
                await expect(nestedObjectList).toBeAttached();
            });
        });
    });

    test.describe('Add object subproperty', () => {
        test('should show "Add object property" button on the object property', async () => {
            const addButton = workflowPage.parentObjectProperty.getByRole('button', {
                name: /Add value object property/i,
            });

            await expect(addButton).toBeVisible();
        });

        test('should add new property when using the popover', async () => {
            const newPropertyName = 'ByteChef';

            const initialSubpropertyCount = await workflowPage.subPropertyListItems.count();

            await test.step('Clicking on the Add object property opens the SubPropertyPopover', async () => {
                const addButton = workflowPage.parentObjectProperty.getByRole('button', {
                    name: /Add value object property/i,
                });

                const popover = authenticatedPage.getByRole('dialog', {name: /value object property popover/i});

                await clickAndExpectToBeVisible({
                    target: popover,
                    trigger: addButton,
                });
            });

            await test.step('Add a STRING subproperty', async () => {
                await addObjectSubPropertyViaPopover({
                    itemType: 'STRING',
                    objectProperty: workflowPage.parentObjectProperty,
                    page: authenticatedPage,
                    propertyName: newPropertyName,
                });
            });

            await test.step('Verify new property appears in the list', async () => {
                await expect(workflowPage.subPropertyListItems).toHaveCount(initialSubpropertyCount + 1);

                const newSubProperty = workflowPage.parentObjectSubPropertyList.getByRole('listitem', {
                    exact: true,
                    name: `${newPropertyName} property`,
                });

                await expect(newSubProperty).toBeVisible();
            });

            await test.step('Verify workflow definition contains updated object property', async () => {
                const updatedWorkflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

                const varTask = updatedWorkflowDefinition.tasks?.find((task) => task.name === 'var_1');

                const objectPropertyParameters = varTask?.parameters?.value as Record<string, unknown> | undefined;

                expect(objectPropertyParameters).toBeDefined();
                expect(objectPropertyParameters![newPropertyName]).toBeDefined();
            });
        });

        test('should block adding a subproperty whose name matches an existing one', async () => {
            const existingPropertyName = workflowPage.objectPropertyParameterKeys[0];

            expect(existingPropertyName, 'sample workflow must expose at least one existing subproperty').toBeTruthy();

            const initialSubpropertyCount = await workflowPage.subPropertyListItems.count();

            const addButton = workflowPage.parentObjectProperty.getByRole('button', {
                name: /Add value object property/i,
            });

            const popover = authenticatedPage.getByRole('dialog', {name: /value object property popover/i});

            await test.step('Open Add object property popover', async () => {
                await clickAndExpectToBeVisible({
                    target: popover,
                    trigger: addButton,
                });
            });

            const nameInput = popover.getByRole('textbox', {name: /name/i});
            const submitButton = popover.getByRole('button', {exact: true, name: 'Add'});
            const duplicateError = popover.getByText('A property with this name already exists.');

            await test.step('Typing an existing subproperty name shows an inline duplicate-name error', async () => {
                await nameInput.fill(existingPropertyName);

                await expect(duplicateError).toBeVisible();
            });

            await test.step('Add button is disabled while the name is a duplicate', async () => {
                await expect(submitButton).toBeDisabled();
            });

            await test.step('Subproperty list stays unchanged while the duplicate name is in place', async () => {
                await expect(workflowPage.subPropertyListItems).toHaveCount(initialSubpropertyCount);
            });

            const uniquePropertyName = `${existingPropertyName}Unique`;

            await test.step('Editing the name to a unique value clears the duplicate-name error', async () => {
                await nameInput.fill(uniquePropertyName);

                await expect(duplicateError).toBeHidden();
            });

            await test.step('Submitting the unique name adds the subproperty', async () => {
                await addObjectSubPropertyViaPopover({
                    itemType: 'STRING',
                    objectProperty: workflowPage.parentObjectProperty,
                    page: authenticatedPage,
                    propertyName: uniquePropertyName,
                });

                await expect(workflowPage.subPropertyListItems).toHaveCount(initialSubpropertyCount + 1);
            });
        });

        test('should prefix the property name with an underscore in the UI after creation if it starts with a number', async () => {
            const numericPropertyName = '123property';
            const expectedPrefixedName = '_123property';

            const initialSubpropertyCount = await workflowPage.subPropertyListItems.count();

            await test.step('Open Add object property popover', async () => {
                const addButton = workflowPage.parentObjectProperty.getByRole('button', {
                    name: /Add value object property/i,
                });

                const popover = authenticatedPage.getByRole('dialog', {name: /value object property popover/i});

                await clickAndExpectToBeVisible({
                    target: popover,
                    trigger: addButton,
                });
            });

            await test.step('Add a subproperty with a number-prefixed name', async () => {
                await addObjectSubPropertyViaPopover({
                    itemType: 'STRING',
                    objectProperty: workflowPage.parentObjectProperty,
                    page: authenticatedPage,
                    propertyName: numericPropertyName,
                });
            });

            await test.step('Verify the created property has its name prefixed with an underscore in the list', async () => {
                await expect(workflowPage.subPropertyListItems).toHaveCount(initialSubpropertyCount + 1);

                const newSubProperty = workflowPage.parentObjectSubPropertyList.getByRole('listitem', {
                    exact: true,
                    name: `${expectedPrefixedName} property`,
                });

                await expect(newSubProperty).toBeVisible();
            });
        });
    });

    test.describe('Remove object subproperty', () => {
        test('should show Delete property button on custom subproperties', async () => {
            const newPropertyName = 'ByteChef';

            await test.step('Add a custom subproperty first', async () => {
                const addButton = workflowPage.parentObjectProperty.getByRole('button', {
                    name: /Add value object property/i,
                });

                const popover = authenticatedPage.getByRole('dialog', {name: /value object property popover/i});

                await clickAndExpectToBeVisible({
                    target: popover,
                    trigger: addButton,
                });

                await addObjectSubPropertyViaPopover({
                    itemType: 'STRING',
                    objectProperty: workflowPage.parentObjectProperty,
                    page: authenticatedPage,
                    propertyName: newPropertyName,
                });
            });

            await test.step('Verify delete button is visible on the custom subproperty', async () => {
                const customProperty = workflowPage.parentObjectSubPropertyList.getByRole('listitem', {
                    exact: true,
                    name: `${newPropertyName} property`,
                });

                const deleteButton = customProperty.getByRole('button', {name: `Delete ${newPropertyName}`});

                await expect(deleteButton).toBeVisible();
            });
        });

        test('should remove property from UI and properly persist changes after clicking Delete', async () => {
            const newPropertyName = 'ByteChef';

            await test.step('Add a custom subproperty first', async () => {
                const addButton = workflowPage.parentObjectProperty.getByRole('button', {
                    name: /Add value object property/i,
                });

                const popover = authenticatedPage.getByRole('dialog', {name: /value object property popover/i});

                await clickAndExpectToBeVisible({
                    target: popover,
                    trigger: addButton,
                });

                await addObjectSubPropertyViaPopover({
                    itemType: 'STRING',
                    objectProperty: workflowPage.parentObjectProperty,
                    page: authenticatedPage,
                    propertyName: newPropertyName,
                });
            });

            const customProperty = workflowPage.parentObjectSubPropertyList.getByRole('listitem', {
                exact: true,
                name: `${newPropertyName} property`,
            });

            const deleteButton = customProperty.getByRole('button', {name: `Delete ${newPropertyName}`});

            const initialSubpropertyCount = await workflowPage.subPropertyListItems.count();

            await test.step('Click delete button and verify immediate UI removal', async () => {
                await deleteButton.click();

                await expect(customProperty).toBeHidden();
                await expect(workflowPage.subPropertyListItems).toHaveCount(initialSubpropertyCount - 1);
            });

            await test.step('Verify workflow definition contains updated object property after debounced save', async () => {
                await authenticatedPage.waitForTimeout(WorkflowPage.LONG_DEBOUNCE_MS);

                const updatedWorkflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

                const varTask = updatedWorkflowDefinition.tasks?.find((task) => task.name === 'var_1');

                const objectPropertyParameters = varTask?.parameters?.value as Record<string, unknown> | undefined;

                expect(objectPropertyParameters).toBeDefined();
                expect(objectPropertyParameters![newPropertyName]).toBeUndefined();
            });
        });
    });

    test.describe('Edit object subproperty', () => {
        test('should allow editing value in object subproperty', async () => {
            const newValue = 'edited object value';

            await test.step('Change value in String property', async () => {
                const stringProperty = workflowPage.firstTaskComponentConfigurationPanel.getByLabel('String property');
                const stringInput = stringProperty.getByRole('textbox');

                await stringInput.fill(newValue);
            });

            await test.step('Wait for debounced save', async () => {
                await authenticatedPage.waitForTimeout(WorkflowPage.LONG_DEBOUNCE_MS);
            });
        });

        test('should persist edited object subproperty value to workflow definition', async () => {
            const newValue = 'persisted object value';

            await test.step('Edit String property', async () => {
                const stringProperty = workflowPage.firstTaskComponentConfigurationPanel.getByLabel('String property');
                const stringInput = stringProperty.getByRole('textbox');

                await stringInput.fill(newValue);
            });

            await test.step('Wait for debounced save', async () => {
                await authenticatedPage.waitForTimeout(WorkflowPage.LONG_DEBOUNCE_MS);
            });

            await test.step('Verify workflow definition has new value', async () => {
                const updatedWorkflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

                const varTask = updatedWorkflowDefinition.tasks?.find((task) => task.name === 'var_1');

                const objectPropertyParameters = varTask?.parameters?.value as Record<string, unknown> | undefined;

                expect(objectPropertyParameters).toBeDefined();
                expect(objectPropertyParameters!['String']).toBe(newValue);
            });
        });

        test('should persist after reload when object subproperty value was edited', async () => {
            const newValue = 'reload persisted object value';

            await test.step('Edit String property', async () => {
                const stringProperty = workflowPage.firstTaskComponentConfigurationPanel.getByLabel('String property');
                const stringInput = stringProperty.getByRole('textbox');

                await stringInput.fill(newValue);
            });

            await test.step('Wait for debounced save', async () => {
                await authenticatedPage.waitForTimeout(WorkflowPage.LONG_DEBOUNCE_MS);
            });

            await test.step('Reload and verify value persists', async () => {
                await authenticatedPage.reload();
                await authenticatedPage.waitForLoadState('domcontentloaded');
                await authenticatedPage.waitForTimeout(2000);

                await reopenConfigurationPanel(authenticatedPage);

                await openPropertiesTab(workflowPage.firstTaskComponentConfigurationPanel);

                const stringProperty = workflowPage.firstTaskComponentConfigurationPanel.getByLabel('String property');
                const stringInput = stringProperty.getByRole('textbox');

                await expect(stringInput).toContainText(newValue);
            });
        });
    });

    test.describe('Nested object operations', () => {
        test('should allow creating a nested object property and adding subproperties inside it', async () => {
            const nestedObjectName = 'NestedObject';
            const nestedSubpropertyName = 'NestedString';

            await test.step('Add an OBJECT property called "NestedObject"', async () => {
                const addButton = workflowPage.parentObjectProperty.getByRole('button', {
                    name: /Add value object property/i,
                });

                const popover = authenticatedPage.getByRole('dialog', {name: /value object property popover/i});

                await clickAndExpectToBeVisible({
                    target: popover,
                    trigger: addButton,
                });

                await addObjectSubPropertyViaPopover({
                    itemType: 'OBJECT',
                    objectProperty: workflowPage.parentObjectProperty,
                    page: authenticatedPage,
                    propertyName: nestedObjectName,
                });

                await authenticatedPage.waitForTimeout(WorkflowPage.LONG_DEBOUNCE_MS);
            });

            await test.step('Verify the nested OBJECT property is rendered with an empty subproperty list and an Add button', async () => {
                const nestedObjectPropertyRow = workflowPage.parentObjectSubPropertyList.getByRole('listitem', {
                    exact: true,
                    name: `${nestedObjectName} property`,
                });

                await expect(nestedObjectPropertyRow).toBeVisible();

                const nestedAddButton = nestedObjectPropertyRow.getByRole('button', {
                    name: new RegExp(`Add ${nestedObjectName} object property`, 'i'),
                });

                await expect(nestedAddButton).toBeVisible();
            });

            await test.step('Add a STRING subproperty inside the newly created nested OBJECT', async () => {
                const nestedObjectPropertyRow = workflowPage.parentObjectSubPropertyList.getByRole('listitem', {
                    exact: true,
                    name: `${nestedObjectName} property`,
                });

                const nestedAddButton = nestedObjectPropertyRow.getByRole('button', {
                    name: new RegExp(`Add ${nestedObjectName} object property`, 'i'),
                });

                const popover = authenticatedPage.getByRole('dialog', {
                    name: new RegExp(`${nestedObjectName} object property popover`, 'i'),
                });

                await clickAndExpectToBeVisible({
                    target: popover,
                    trigger: nestedAddButton,
                });

                await addObjectSubPropertyViaPopover({
                    itemType: 'STRING',
                    objectProperty: nestedObjectPropertyRow,
                    page: authenticatedPage,
                    propertyName: nestedSubpropertyName,
                });
            });

            await test.step('Verify the subproperty is added inside the nested object', async () => {
                const nestedObjectPropertyRow = workflowPage.parentObjectSubPropertyList.getByRole('listitem', {
                    exact: true,
                    name: `${nestedObjectName} property`,
                });

                const nestedObjectList = nestedObjectPropertyRow.getByRole('list', {
                    name: `${nestedObjectName} object properties`,
                });

                const newNestedSubProperty = nestedObjectList.getByRole('listitem', {
                    exact: true,
                    name: `${nestedSubpropertyName} property`,
                });

                await expect(newNestedSubProperty).toBeVisible();
            });

            await test.step('Verify workflow definition contains the nested hierarchy', async () => {
                await authenticatedPage.waitForTimeout(WorkflowPage.LONG_DEBOUNCE_MS);

                const updatedWorkflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);
                const varTask = updatedWorkflowDefinition.tasks?.find((task) => task.name === 'var_1');
                const objectPropertyParameters = varTask?.parameters?.value as Record<string, unknown> | undefined;

                expect(objectPropertyParameters).toBeDefined();

                const nestedObjectValue = objectPropertyParameters![nestedObjectName] as
                    | Record<string, unknown>
                    | undefined;

                expect(nestedObjectValue).toBeDefined();
                expect(nestedObjectValue![nestedSubpropertyName]).toBeDefined();
            });
        });
    });
});
