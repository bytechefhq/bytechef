import {MultiSelectOptionType} from '@/components/MultiSelect/MultiSelect';
import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ArrayProperty from '@/pages/platform/workflow-editor/components/properties/ArrayProperty';
import {useClusterElementContext} from '@/pages/platform/workflow-editor/components/properties/ClusterElementContext';
import ObjectProperty from '@/pages/platform/workflow-editor/components/properties/ObjectProperty';
import FormControlledArrayItems from '@/pages/platform/workflow-editor/components/properties/components/FormControlledArrayItems';
import FormControlledObjectEntries from '@/pages/platform/workflow-editor/components/properties/components/FormControlledObjectEntries';
import FromAiToggleButton from '@/pages/platform/workflow-editor/components/properties/components/FromAiToggleButton';
import InputTypeSwitchButton from '@/pages/platform/workflow-editor/components/properties/components/InputTypeSwitchButton';
import PropertyComboBox from '@/pages/platform/workflow-editor/components/properties/components/PropertyComboBox';
import PropertyDynamicProperties from '@/pages/platform/workflow-editor/components/properties/components/PropertyDynamicProperties';
import PropertyMultiSelect from '@/pages/platform/workflow-editor/components/properties/components/PropertyMultiSelect';
import PropertySelect from '@/pages/platform/workflow-editor/components/properties/components/PropertySelect';
import PropertyTextArea from '@/pages/platform/workflow-editor/components/properties/components/PropertyTextArea';
import PropertyCodeEditor from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/PropertyCodeEditor';
import PropertyInput from '@/pages/platform/workflow-editor/components/properties/components/property-input/PropertyInput';
import PropertyJsonSchemaBuilder from '@/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/PropertyJsonSchemaBuilder';
import PropertyMentionsInput from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInput';
import useProperty from '@/pages/platform/workflow-editor/components/properties/hooks/useProperty';
import getInputHTMLType from '@/pages/platform/workflow-editor/utils/getInputHTMLType';
import resolveExpressionValue from '@/pages/platform/workflow-editor/utils/resolveExpressionValue';
import {ERROR_MESSAGES} from '@/shared/errorMessages';
import {
    GetClusterElementParameterDisplayConditions200Response,
    Option,
} from '@/shared/middleware/platform/configuration';
import {ArrayPropertyType, PropertyAllType, SelectOptionType} from '@/shared/types';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {UseQueryResult} from '@tanstack/react-query';
import {CircleQuestionMarkIcon, EqualIcon} from 'lucide-react';
import {ReactNode} from 'react';
import {Control, Controller, FieldValues, FormState} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';

interface PropertyProps {
    arrayIndex?: number;
    arrayName?: string;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    control?: Control<any, any>;
    controlPath?: string;
    customClassName?: string;
    deletePropertyButton?: ReactNode;
    displayConditionsQuery?: UseQueryResult<GetClusterElementParameterDisplayConditions200Response, Error>;
    dynamicPropertySource?: string;
    formState?: FormState<FieldValues>;
    objectName?: string;
    operationName?: string;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    parameterValue?: any;
    parentArrayItems?: Array<ArrayPropertyType>;
    path?: string;
    property: PropertyAllType;
    toolsMode?: boolean;
}

const Property = ({
    arrayIndex,
    arrayName,
    control,
    controlPath = 'parameters',
    customClassName,
    deletePropertyButton,
    displayConditionsQuery,
    dynamicPropertySource,
    formState,
    objectName,
    operationName,
    parameterValue,
    parentArrayItems,
    path,
    property,
    toolsMode,
}: PropertyProps) => {
    const {
        calculatedPath,
        controlType,
        controlledBlurError,
        controlledDynamicMode,
        controlledDynamicOnChangeRef,
        controlledFromAi,
        currentComponent,
        currentNode,
        defaultValue,
        description,
        displayCondition,
        editorRef,
        errorMessage,
        expressionEnabled,
        formattedOptions,
        fromAiExpression,
        handleCodeEditorChange,
        handleControlledBlur,
        handleControlledModeSwitch,
        handleDeleteCustomPropertyClick,
        handleFromAiClick,
        handleFromAiToggle,
        handleInputChange,
        handleInputTypeSwitchButtonClick,
        handleJsonSchemaBuilderChange,
        handleMentionInputValueChange,
        handleMultiSelectChange,
        handleSelectChange,
        hasError,
        hidden,
        inputRef,
        inputValue,
        isFormulaMode,
        isFromAi,
        isLoadingDisplayCondition,
        isNumericalInput,
        isToolsClusterElement,
        isValidControlType,
        label,
        languageId,
        lookupDependsOnValues,
        maxLength,
        maxValue,
        mentionInput,
        mentionInputValue,
        minLength,
        minValue,
        multiSelectValue,
        name,
        options,
        optionsDataSource,
        placeholder,
        propertiesDataSource,
        propertyParameterValue,
        required,
        selectValue,
        setDataPillPanelOpen,
        setIsFormulaMode,
        setSelectValue,
        showInputTypeSwitchButton,
        type,
        typeIcon,
        validatePropertyValue,
        workflow,
    } = useProperty({
        arrayIndex,
        control,
        controlPath,
        displayConditionsQuery,
        dynamicPropertySource,
        formState,
        objectName,
        operationName,
        parameterValue,
        parentArrayItems,
        path,
        property,
        toolsMode,
    });

    const clusterElementContext = useClusterElementContext();

    if (hidden && !control) {
        return <></>;
    }

    if (isLoadingDisplayCondition) {
        return (
            <div className={twMerge('flex flex-col space-y-1', objectName && 'ml-2 mt-1')}>
                <Skeleton className="h-5 w-1/4" />

                <Skeleton className="h-9 w-full" />
            </div>
        );
    }

    if (!control && displayCondition && !currentComponent?.displayConditions?.[displayCondition]) {
        return <></>;
    }

    return (
        <li
            aria-label={`${name} property`}
            className={twMerge(
                'w-full',
                hidden && 'hidden',
                controlType === 'OBJECT_BUILDER' && 'flex-col',
                controlType === 'ARRAY_BUILDER' && 'flex-col',
                customClassName
            )}
            key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
        >
            {mentionInput && currentComponent && type !== 'DYNAMIC_PROPERTIES' && controlType !== 'CODE_EDITOR' && (
                <PropertyMentionsInput
                    controlType={controlType || 'TEXT'}
                    defaultValue={defaultValue}
                    deletePropertyButton={deletePropertyButton}
                    description={description}
                    error={hasError}
                    errorMessage={errorMessage}
                    expressionEnabled={expressionEnabled}
                    handleFromAiClick={handleFromAiClick}
                    handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                    isFormulaMode={isFormulaMode}
                    isFromAi={isFromAi}
                    label={label || name}
                    leadingIcon={typeIcon}
                    onValueChange={handleMentionInputValueChange}
                    path={calculatedPath}
                    placeholder={placeholder}
                    ref={editorRef}
                    required={required}
                    setIsFormulaMode={setIsFormulaMode}
                    showInputTypeSwitchButton={showInputTypeSwitchButton}
                    type={type}
                    validateBeforeSave={validatePropertyValue}
                    value={mentionInputValue}
                />
            )}

            {!mentionInput && (
                <>
                    {!controlledDynamicMode &&
                        ((controlType === 'OBJECT_BUILDER' && name !== '__item') ||
                            controlType === 'ARRAY_BUILDER' ||
                            controlType === 'NULL') && (
                            <div className={twMerge('flex items-center', controlType !== 'NULL' && 'pb-1')}>
                                {typeIcon && controlType !== 'NULL' && (
                                    <span className={twMerge(label ? 'pr-2' : 'pr-1')} title={type}>
                                        {typeIcon}
                                    </span>
                                )}

                                {(label || description || showInputTypeSwitchButton) && (
                                    <div className="flex w-full items-center justify-between">
                                        <div className="flex items-center">
                                            {label && (
                                                <Label className="leading-normal">
                                                    {label}

                                                    {required && <RequiredMark />}
                                                </Label>
                                            )}

                                            {!label && arrayIndex !== undefined && (
                                                <Label className="leading-normal">Item</Label>
                                            )}

                                            {description && (
                                                <Tooltip>
                                                    <TooltipTrigger>
                                                        <CircleQuestionMarkIcon className="ml-1 size-4 text-muted-foreground" />
                                                    </TooltipTrigger>

                                                    <TooltipPortal>
                                                        <TooltipContent className="max-w-md">
                                                            {description}
                                                        </TooltipContent>
                                                    </TooltipPortal>
                                                </Tooltip>
                                            )}
                                        </div>

                                        <div className="flex items-center gap-1">
                                            {showInputTypeSwitchButton && (
                                                <InputTypeSwitchButton
                                                    handleClick={handleInputTypeSwitchButtonClick}
                                                    mentionInput={mentionInput}
                                                />
                                            )}

                                            {!showInputTypeSwitchButton && control && isToolsClusterElement && (
                                                <InputTypeSwitchButton
                                                    handleClick={() =>
                                                        handleControlledModeSwitch(!controlledDynamicMode)
                                                    }
                                                    mentionInput={controlledDynamicMode}
                                                />
                                            )}

                                            {deletePropertyButton}
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}

                    {!control && controlType === 'ARRAY_BUILDER' && calculatedPath && (
                        <ArrayProperty
                            onDeleteClick={handleDeleteCustomPropertyClick}
                            parentArrayItems={parentArrayItems}
                            path={calculatedPath}
                            property={property}
                        />
                    )}

                    {control && controlledDynamicMode && calculatedPath && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            name={calculatedPath}
                            render={({field}) => {
                                controlledDynamicOnChangeRef.current = field.onChange;

                                const displayValue = typeof field.value === 'string' ? field.value : '';
                                const valueIsFromAi =
                                    typeof field.value === 'string' && field.value.startsWith('=fromAi(');

                                const isFieldFromAi =
                                    isToolsClusterElement &&
                                    (controlledFromAi !== undefined ? controlledFromAi : valueIsFromAi);

                                const isExpressionMode = displayValue.startsWith('=');
                                const strippedDisplayValue = isExpressionMode
                                    ? displayValue.substring(1)
                                    : displayValue;
                                const strippedFromAiValue = fromAiExpression.startsWith('=')
                                    ? fromAiExpression.substring(1)
                                    : fromAiExpression;

                                const {onChange: fieldOnChange, ...fieldRest} = field;

                                return (
                                    <PropertyInput
                                        {...fieldRest}
                                        deletePropertyButton={deletePropertyButton}
                                        description={description}
                                        disabled={isFieldFromAi}
                                        error={hasError}
                                        errorMessage={errorMessage}
                                        expressionPrefix
                                        handleInputTypeSwitchButtonClick={() => {
                                            fieldOnChange('');
                                            handleControlledModeSwitch(false);
                                        }}
                                        inputOverlay={
                                            isFieldFromAi ? (
                                                <span className="flex h-full flex-1 items-center pl-property-input-position text-sm font-medium italic text-muted-foreground">
                                                    Automatically defined by the model
                                                </span>
                                            ) : undefined
                                        }
                                        label={label || name}
                                        leadingIcon={
                                            isExpressionMode || isFieldFromAi ? (
                                                <EqualIcon className="size-4" />
                                            ) : (
                                                typeIcon
                                            )
                                        }
                                        mentionInput
                                        onChange={(event) => {
                                            fieldOnChange(resolveExpressionValue(event.target.value, field.value));
                                        }}
                                        onFocus={() => setDataPillPanelOpen(true)}
                                        placeholder="Use '=' for an expression"
                                        required={required}
                                        showInputTypeSwitchButton
                                        trailingAction={
                                            isToolsClusterElement && expressionEnabled !== false ? (
                                                <FromAiToggleButton
                                                    isFromAi={!!isFieldFromAi}
                                                    onToggle={(fromAi) => handleFromAiToggle(fromAi, fieldOnChange)}
                                                />
                                            ) : undefined
                                        }
                                        type={hidden ? 'hidden' : 'text'}
                                        value={isFieldFromAi ? strippedFromAiValue : strippedDisplayValue}
                                    />
                                );
                            }}
                            rules={{required}}
                        />
                    )}

                    {control && !controlledDynamicMode && controlType === 'ARRAY_BUILDER' && calculatedPath && (
                        <FormControlledArrayItems
                            control={control}
                            controlPath={calculatedPath}
                            formState={formState}
                            property={property}
                            toolsMode={isToolsClusterElement}
                        />
                    )}

                    {!control && (controlType === 'OBJECT_BUILDER' || type === 'FILE_ENTRY') && (
                        <ObjectProperty
                            arrayIndex={arrayIndex}
                            arrayName={arrayName}
                            onDeleteClick={handleDeleteCustomPropertyClick}
                            operationName={operationName}
                            path={calculatedPath}
                            property={property}
                        />
                    )}

                    {control &&
                        !controlledDynamicMode &&
                        (controlType === 'OBJECT_BUILDER' || type === 'FILE_ENTRY') &&
                        calculatedPath &&
                        !!property.properties?.length && (
                            <ul className={twMerge('space-y-4', label && 'ml-2 border-l border-l-border/50')}>
                                {(property.properties as PropertyAllType[]).map((subProperty, index) => (
                                    <Property
                                        control={control}
                                        controlPath={calculatedPath}
                                        customClassName="w-full pl-2"
                                        formState={formState}
                                        key={subProperty.name || `${property.name}_${index}`}
                                        property={subProperty}
                                        toolsMode={isToolsClusterElement}
                                    />
                                ))}
                            </ul>
                        )}

                    {control &&
                        !controlledDynamicMode &&
                        (controlType === 'OBJECT_BUILDER' || type === 'FILE_ENTRY') &&
                        calculatedPath &&
                        !property.properties?.length && (
                            <FormControlledObjectEntries
                                control={control}
                                controlPath={calculatedPath}
                                formState={formState}
                                property={property}
                                toolsMode={isToolsClusterElement}
                            />
                        )}

                    {control &&
                        !controlledDynamicMode &&
                        (isValidControlType || isNumericalInput) &&
                        calculatedPath && (
                            <Controller
                                control={control}
                                defaultValue={defaultValue}
                                name={calculatedPath}
                                render={({field, fieldState}) => {
                                    const showControlledSwitch = isToolsClusterElement && type !== 'STRING';
                                    const showFromAi = isToolsClusterElement && type === 'STRING';

                                    const valueIsFromAi =
                                        showFromAi &&
                                        typeof field.value === 'string' &&
                                        field.value.startsWith('=fromAi(');

                                    const isFieldFromAi =
                                        showFromAi &&
                                        (controlledFromAi !== undefined ? controlledFromAi : valueIsFromAi);

                                    const displayValue =
                                        typeof field.value === 'string'
                                            ? field.value
                                            : field.value != null
                                              ? String(field.value)
                                              : '';

                                    const isExpressionMode = showFromAi && displayValue.startsWith('=');
                                    const strippedDisplayValue = isExpressionMode
                                        ? displayValue.substring(1)
                                        : displayValue;
                                    const strippedFromAiValue = fromAiExpression.startsWith('=')
                                        ? fromAiExpression.substring(1)
                                        : fromAiExpression;

                                    const {onChange: fieldOnChange, ...fieldRest} = field;

                                    return (
                                        <>
                                            <PropertyInput
                                                {...fieldRest}
                                                deletePropertyButton={deletePropertyButton}
                                                description={description}
                                                disabled={isFieldFromAi}
                                                error={!!fieldState.error || !!controlledBlurError}
                                                errorMessage={fieldState.error?.message || controlledBlurError}
                                                expressionPrefix={showFromAi}
                                                fieldsetClassName={objectName && arrayName && 'ml-2'}
                                                handleInputTypeSwitchButtonClick={
                                                    showControlledSwitch
                                                        ? () => {
                                                              fieldOnChange('=');
                                                              handleControlledModeSwitch(true);
                                                          }
                                                        : undefined
                                                }
                                                inputOverlay={
                                                    isFieldFromAi ? (
                                                        <span className="flex h-full flex-1 items-center pl-property-input-position text-sm font-medium italic text-muted-foreground">
                                                            Automatically defined by the model
                                                        </span>
                                                    ) : undefined
                                                }
                                                label={label || name}
                                                leadingIcon={
                                                    isExpressionMode || isFieldFromAi ? (
                                                        <EqualIcon className="size-4" />
                                                    ) : (
                                                        typeIcon
                                                    )
                                                }
                                                max={maxValue}
                                                maxLength={maxLength}
                                                min={minValue}
                                                minLength={minLength}
                                                onBlur={() => {
                                                    field.onBlur();
                                                    handleControlledBlur(field.value);
                                                }}
                                                onChange={(event) => {
                                                    if (!showFromAi) {
                                                        if (isNumericalInput && event.target.value !== '') {
                                                            fieldOnChange(
                                                                type === 'INTEGER'
                                                                    ? parseInt(event.target.value, 10)
                                                                    : parseFloat(event.target.value)
                                                            );
                                                        } else {
                                                            fieldOnChange(event);
                                                        }

                                                        return;
                                                    }

                                                    fieldOnChange(
                                                        resolveExpressionValue(event.target.value, field.value)
                                                    );
                                                }}
                                                placeholder={
                                                    isNumericalInput && minValue && maxValue
                                                        ? `From ${minValue} to ${maxValue}`
                                                        : placeholder ||
                                                          `Type a ${isNumericalInput ? 'number' : 'something'} ...`
                                                }
                                                required={required}
                                                showInputTypeSwitchButton={showControlledSwitch}
                                                title={type}
                                                trailingAction={
                                                    showFromAi && expressionEnabled !== false ? (
                                                        <FromAiToggleButton
                                                            isFromAi={!!isFieldFromAi}
                                                            onToggle={(fromAi) =>
                                                                handleFromAiToggle(fromAi, fieldOnChange)
                                                            }
                                                        />
                                                    ) : undefined
                                                }
                                                type={hidden ? 'hidden' : getInputHTMLType(controlType)}
                                                value={isFieldFromAi ? strippedFromAiValue : strippedDisplayValue}
                                            />

                                            {!!options?.length && (
                                                <PropertySelect
                                                    deletePropertyButton={deletePropertyButton}
                                                    description={description}
                                                    label={label || name}
                                                    leadingIcon={typeIcon}
                                                    name={name}
                                                    onValueChange={(value) => {
                                                        field.onChange(value);

                                                        setSelectValue(value);
                                                    }}
                                                    options={options as Array<SelectOptionType>}
                                                    required={required}
                                                    value={selectValue}
                                                />
                                            )}
                                        </>
                                    );
                                }}
                                rules={{
                                    required: required ? ERROR_MESSAGES.PROPERTY.FIELD_REQUIRED : false,
                                    validate: (value: string | number) => {
                                        if (value === '' || value == null) {
                                            return true;
                                        }

                                        return validatePropertyValue(value) || ERROR_MESSAGES.PROPERTY.INCORRECT_VALUE;
                                    },
                                }}
                            />
                        )}

                    {control &&
                        !controlledDynamicMode &&
                        controlType === 'SELECT' &&
                        type !== 'BOOLEAN' &&
                        calculatedPath && (
                            <Controller
                                control={control}
                                defaultValue={defaultValue}
                                name={calculatedPath}
                                render={({field: {name: fieldName, onBlur, onChange, value: fieldValue}}) => (
                                    <PropertyComboBox
                                        arrayIndex={arrayIndex}
                                        defaultValue={defaultValue}
                                        deletePropertyButton={deletePropertyButton}
                                        description={description}
                                        handleInputTypeSwitchButtonClick={() => {
                                            onChange('=');
                                            handleControlledModeSwitch(true);
                                        }}
                                        label={label || fieldName}
                                        leadingIcon={typeIcon}
                                        lookupDependsOnPaths={optionsDataSource?.optionsLookupDependsOn?.map(
                                            (optionLookupDependency) =>
                                                optionLookupDependency.replace('[index]', `[${arrayIndex}]`)
                                        )}
                                        lookupDependsOnValues={lookupDependsOnValues}
                                        name={fieldName}
                                        onBlur={onBlur}
                                        onValueChange={(value) => {
                                            onChange(value);

                                            setSelectValue(value);
                                        }}
                                        options={(formattedOptions as Array<Option>) || []}
                                        optionsDataSource={optionsDataSource}
                                        path={calculatedPath}
                                        required={required}
                                        showInputTypeSwitchButton={isToolsClusterElement}
                                        value={fieldValue !== undefined ? fieldValue : selectValue}
                                        workflowId={workflow.id!}
                                        workflowNodeName={currentNode!.name}
                                    />
                                )}
                                rules={{required}}
                            />
                        )}

                    {control &&
                        !controlledDynamicMode &&
                        controlType === 'SELECT' &&
                        type === 'BOOLEAN' &&
                        calculatedPath && (
                            <Controller
                                control={control}
                                defaultValue={defaultValue}
                                name={calculatedPath}
                                render={({field: {name, onChange, value: fieldValue}}) => (
                                    <PropertySelect
                                        deletePropertyButton={deletePropertyButton}
                                        description={description}
                                        handleInputTypeSwitchButtonClick={() => {
                                            onChange('=');
                                            handleControlledModeSwitch(true);
                                        }}
                                        label={label || name}
                                        leadingIcon={typeIcon}
                                        name={name}
                                        onValueChange={(value) => {
                                            onChange(value);

                                            setSelectValue(value);
                                        }}
                                        options={[
                                            {label: 'True', value: 'true'},
                                            {label: 'False', value: 'false'},
                                        ]}
                                        showInputTypeSwitchButton={isToolsClusterElement}
                                        value={fieldValue !== undefined ? fieldValue : selectValue}
                                    />
                                )}
                                rules={{required}}
                            />
                        )}

                    {control && !controlledDynamicMode && controlType === 'TEXT_AREA' && calculatedPath && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            name={calculatedPath}
                            render={({field}) => (
                                <PropertyTextArea
                                    deletePropertyButton={deletePropertyButton}
                                    description={description}
                                    error={hasError}
                                    errorMessage={errorMessage}
                                    label={label || name}
                                    leadingIcon={typeIcon}
                                    required={required}
                                    {...field}
                                />
                            )}
                            rules={{required}}
                        />
                    )}

                    {control && !controlledDynamicMode && controlType === 'MULTI_SELECT' && calculatedPath && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue || []}
                            name={calculatedPath}
                            render={({field: {onChange, value}}) => (
                                <PropertyMultiSelect
                                    defaultValue={(value as string[]) || []}
                                    deletePropertyButton={deletePropertyButton}
                                    handleInputTypeSwitchButtonClick={() => {
                                        handleControlledModeSwitch(true);
                                    }}
                                    leadingIcon={typeIcon}
                                    lookupDependsOnPaths={optionsDataSource?.optionsLookupDependsOn?.map(
                                        (optionLookupDependency) =>
                                            optionLookupDependency.replace('[index]', `[${arrayIndex}]`)
                                    )}
                                    lookupDependsOnValues={lookupDependsOnValues}
                                    onChange={onChange}
                                    options={(formattedOptions as MultiSelectOptionType[]) || []}
                                    optionsDataSource={optionsDataSource}
                                    path={calculatedPath}
                                    property={property}
                                    showInputTypeSwitchButton={isToolsClusterElement}
                                    value={(value as string[]) || []}
                                    workflowId={workflow.id!}
                                />
                            )}
                            rules={{required}}
                        />
                    )}

                    {!control && (isValidControlType || isNumericalInput) && calculatedPath && (
                        <PropertyInput
                            deletePropertyButton={deletePropertyButton}
                            description={description}
                            error={hasError}
                            errorMessage={errorMessage}
                            fieldsetClassName={objectName && arrayName && 'ml-2'}
                            handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                            label={label || name}
                            leadingIcon={typeIcon}
                            max={maxValue}
                            maxLength={maxLength}
                            min={minValue}
                            minLength={minLength}
                            name={calculatedPath}
                            onChange={handleInputChange}
                            placeholder={
                                isNumericalInput && minValue && maxValue
                                    ? `From ${minValue} to ${maxValue}`
                                    : placeholder || `Type a ${isNumericalInput ? 'number' : 'something'} ...`
                            }
                            ref={inputRef}
                            required={required}
                            showInputTypeSwitchButton={showInputTypeSwitchButton}
                            title={type}
                            type={hidden ? 'hidden' : getInputHTMLType(controlType)}
                            value={inputValue}
                        />
                    )}

                    {!control && (isValidControlType || isNumericalInput) && !!options?.length && (
                        <PropertySelect
                            deletePropertyButton={deletePropertyButton}
                            description={description}
                            label={label || name}
                            leadingIcon={typeIcon}
                            name={name}
                            onValueChange={(value) => handleSelectChange(value, name!)}
                            options={options as Array<SelectOptionType>}
                            required={required}
                            value={selectValue}
                        />
                    )}

                    {!control && controlType === 'JSON_SCHEMA_BUILDER' && (
                        <PropertyJsonSchemaBuilder
                            description={description}
                            error={hasError}
                            errorMessage={errorMessage}
                            handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                            label={label || name}
                            leadingIcon={typeIcon}
                            name={name!}
                            onChange={(value) => handleJsonSchemaBuilderChange(value)}
                            schema={inputValue ? JSON.parse(inputValue) : undefined}
                            title={label || name}
                        />
                    )}

                    {!control && controlType === 'SELECT' && type !== 'BOOLEAN' && (
                        <PropertyComboBox
                            arrayIndex={arrayIndex}
                            defaultValue={defaultValue}
                            deletePropertyButton={deletePropertyButton}
                            description={description}
                            handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                            label={label || name}
                            leadingIcon={typeIcon}
                            lookupDependsOnPaths={optionsDataSource?.optionsLookupDependsOn?.map(
                                (optionLookupDependency) => optionLookupDependency.replace('[index]', `[${arrayIndex}]`)
                            )}
                            lookupDependsOnValues={lookupDependsOnValues}
                            name={name}
                            onValueChange={(value: string) => handleSelectChange(value, name!)}
                            options={(formattedOptions as Array<Option>) || []}
                            optionsDataSource={optionsDataSource}
                            path={calculatedPath}
                            required={required}
                            showInputTypeSwitchButton={showInputTypeSwitchButton}
                            value={selectValue}
                            workflowId={workflow.id!}
                            workflowNodeName={currentNode?.name ?? ''}
                        />
                    )}

                    {!control && controlType === 'SELECT' && type === 'BOOLEAN' && (
                        <PropertySelect
                            defaultValue={defaultValue?.toString()}
                            deletePropertyButton={deletePropertyButton}
                            description={description}
                            handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                            label={label || name}
                            leadingIcon={typeIcon}
                            name={name}
                            onValueChange={(value: string) => handleSelectChange(value, name!)}
                            options={[
                                {label: 'True', value: 'true'},
                                {label: 'False', value: 'false'},
                            ]}
                            required={required}
                            showInputTypeSwitchButton={showInputTypeSwitchButton}
                            value={selectValue}
                        />
                    )}

                    {!control && controlType === 'TEXT_AREA' && (
                        <PropertyTextArea
                            description={description}
                            error={hasError}
                            errorMessage={errorMessage}
                            label={label || name}
                            leadingIcon={typeIcon}
                            name={name!}
                            onChange={handleInputChange}
                            required={required}
                            value={inputValue}
                        />
                    )}

                    {!control && controlType === 'MULTI_SELECT' && (
                        <PropertyMultiSelect
                            defaultValue={propertyParameterValue as string[]}
                            deletePropertyButton={deletePropertyButton}
                            handleInputTypeSwitchButtonClick={() => handleInputTypeSwitchButtonClick()}
                            leadingIcon={typeIcon}
                            lookupDependsOnPaths={optionsDataSource?.optionsLookupDependsOn?.map(
                                (optionLookupDependency) => optionLookupDependency.replace('[index]', `[${arrayIndex}]`)
                            )}
                            lookupDependsOnValues={lookupDependsOnValues}
                            onChange={(value) => handleMultiSelectChange(value)}
                            options={formattedOptions as MultiSelectOptionType[]}
                            optionsDataSource={optionsDataSource}
                            path={calculatedPath}
                            property={property}
                            showInputTypeSwitchButton={showInputTypeSwitchButton}
                            value={multiSelectValue}
                            workflowId={workflow.id!}
                        />
                    )}

                    {controlType === 'NULL' && <span>NULL</span>}
                </>
            )}

            {type === 'DYNAMIC_PROPERTIES' && (currentNode || clusterElementContext) && (
                <PropertyDynamicProperties
                    control={control}
                    controlPath={controlPath}
                    currentOperationName={operationName}
                    enabled={
                        !!clusterElementContext ||
                        !!(currentNode?.connectionId && currentNode?.connections) ||
                        currentNode?.connections?.length === 0
                    }
                    formState={formState}
                    lookupDependsOnPaths={propertiesDataSource?.propertiesLookupDependsOn}
                    lookupDependsOnValues={lookupDependsOnValues}
                    name={name}
                    parameterValue={propertyParameterValue}
                    path={calculatedPath}
                    toolsMode={toolsMode}
                />
            )}

            {controlType === 'CODE_EDITOR' && (
                <PropertyCodeEditor
                    defaultValue={defaultValue}
                    description={description}
                    error={hasError}
                    errorMessage={errorMessage}
                    label={label || name}
                    language={languageId!}
                    leadingIcon={typeIcon}
                    name={name!}
                    onChange={handleCodeEditorChange}
                    required={required}
                    value={propertyParameterValue}
                    workflow={workflow}
                    workflowNodeName={currentNode?.name ?? ''}
                />
            )}
        </li>
    );
};

export default Property;
