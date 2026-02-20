import Button from '@/components/Button/Button';
import {MultiSelectOptionType} from '@/components/MultiSelect/MultiSelect';
import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ArrayProperty from '@/pages/platform/workflow-editor/components/properties/ArrayProperty';
import ObjectProperty from '@/pages/platform/workflow-editor/components/properties/ObjectProperty';
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
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import getInputHTMLType from '@/pages/platform/workflow-editor/utils/getInputHTMLType';
import {
    GetClusterElementParameterDisplayConditions200Response,
    Option,
} from '@/shared/middleware/platform/configuration';
import {ArrayPropertyType, PropertyAllType, SelectOptionType} from '@/shared/types';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {UseQueryResult} from '@tanstack/react-query';
import {CircleQuestionMarkIcon, PlusIcon, XIcon} from 'lucide-react';
import {ReactNode, useMemo, useRef, useState} from 'react';
import {Control, Controller, FieldValues, FormState, useFormContext, useWatch} from 'react-hook-form';
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
}

interface ControlledArrayItemsProps {
    /* eslint-disable @typescript-eslint/no-explicit-any */
    control: Control<any, any>;
    controlPath: string;
    formState?: FormState<FieldValues>;
    property: PropertyAllType;
}

/**
 * Renders array items in controlled (react-hook-form) mode.
 * Must be rendered within a FormProvider ancestor (e.g., shadcn's Form component).
 */
const ControlledArrayItems = ({control, controlPath, formState, property}: ControlledArrayItemsProps) => {
    const {setValue} = useFormContext();
    const watchedValue = useWatch({control, name: controlPath});
    const itemKeysRef = useRef<string[]>([]);

    const items = Array.isArray(watchedValue) ? (watchedValue as unknown[]) : [];
    const itemDefinition = property.items?.[0];

    // Sync stable keys with items (handles initial load and external resets)
    while (itemKeysRef.current.length < items.length) {
        itemKeysRef.current.push(crypto.randomUUID());
    }

    itemKeysRef.current.length = items.length;

    if (!itemDefinition) {
        return null;
    }

    return (
        <>
            <ul className="ml-2 flex flex-col space-y-4 border-l border-l-border/50">
                {items.map((item, index) => (
                    <li className="flex items-center gap-1" key={itemKeysRef.current[index]}>
                        <Property
                            control={control}
                            controlPath={controlPath}
                            customClassName="w-full pl-2"
                            deletePropertyButton={
                                <Button
                                    icon={<XIcon />}
                                    onClick={() => {
                                        itemKeysRef.current.splice(index, 1);

                                        setValue(
                                            controlPath,
                                            items.filter((_, itemIndex) => itemIndex !== index)
                                        );
                                    }}
                                    size="iconXs"
                                    variant="destructiveGhost"
                                />
                            }
                            formState={formState}
                            parameterValue={item}
                            property={
                                {
                                    ...itemDefinition,
                                    defaultValue: item,
                                    label: `Item ${index}`,
                                    name: String(index),
                                } as PropertyAllType
                            }
                        />
                    </li>
                ))}
            </ul>

            <Button
                className="mt-3 rounded-sm"
                icon={<PlusIcon />}
                label="Add item"
                onClick={() => {
                    itemKeysRef.current.push(crypto.randomUUID());

                    setValue(controlPath, [...items, '']);
                }}
                size="sm"
                variant="secondary"
            />
        </>
    );
};

interface ControlledObjectEntriesProps {
    /* eslint-disable @typescript-eslint/no-explicit-any */
    control: Control<any, any>;
    controlPath: string;
    formState?: FormState<FieldValues>;
    property: PropertyAllType;
}

/**
 * Renders dynamic key-value entries for objects in controlled (react-hook-form) mode.
 * Must be rendered within a FormProvider ancestor (e.g., shadcn's Form component).
 */
const ControlledObjectEntries = ({control, controlPath, formState, property}: ControlledObjectEntriesProps) => {
    const {setValue} = useFormContext();
    const watchedValue = useWatch({control, name: controlPath});
    const [newEntryKey, setNewEntryKey] = useState('');

    const entries = useMemo(() => {
        if (watchedValue && typeof watchedValue === 'object' && !Array.isArray(watchedValue)) {
            return Object.entries(watchedValue as Record<string, unknown>);
        }

        return [];
    }, [watchedValue]);

    const itemDefinition = property.additionalProperties?.[0];

    const handleAddEntry = () => {
        const trimmedKey = newEntryKey.trim();

        if (!trimmedKey) {
            return;
        }

        const currentObject = (watchedValue as Record<string, unknown>) || {};

        if (trimmedKey in currentObject) {
            return;
        }

        setValue(controlPath, {
            ...currentObject,
            [trimmedKey]: '',
        });

        setNewEntryKey('');
    };

    return (
        <>
            <ul className="ml-2 flex flex-col space-y-4 border-l border-l-border/50">
                {entries.map(([entryKey, entryValue]) => (
                    <li className="flex items-center gap-1" key={`${controlPath}_${entryKey}`}>
                        <Property
                            control={control}
                            controlPath={controlPath}
                            customClassName="w-full pl-2"
                            deletePropertyButton={
                                <Button
                                    icon={<XIcon />}
                                    onClick={() => {
                                        const currentObject = {...(watchedValue as Record<string, unknown>)};

                                        delete currentObject[entryKey];

                                        setValue(controlPath, currentObject);
                                    }}
                                    size="iconXs"
                                    variant="destructiveGhost"
                                />
                            }
                            formState={formState}
                            parameterValue={entryValue}
                            property={
                                {
                                    ...itemDefinition,
                                    controlType: 'TEXT',
                                    defaultValue: entryValue,
                                    label: entryKey,
                                    name: entryKey,
                                    type: itemDefinition?.type || 'STRING',
                                } as PropertyAllType
                            }
                        />
                    </li>
                ))}
            </ul>

            <div className="mt-3 flex items-center gap-2">
                <input
                    className="h-8 flex-1 rounded-md border bg-background px-2 text-sm"
                    onChange={(event) => setNewEntryKey(event.target.value)}
                    onKeyDown={(event) => {
                        if (event.key === 'Enter') {
                            event.preventDefault();

                            handleAddEntry();
                        }
                    }}
                    placeholder="Key name"
                    type="text"
                    value={newEntryKey}
                />

                <Button
                    className="rounded-sm"
                    disabled={!newEntryKey.trim()}
                    icon={<PlusIcon />}
                    label="Add"
                    onClick={handleAddEntry}
                    size="sm"
                    variant="secondary"
                />
            </div>
        </>
    );
};

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
}: PropertyProps) => {
    const {
        calculatedPath,
        controlType,
        currentComponent,
        currentNode,
        defaultValue,
        description,
        displayCondition,
        editorRef,
        errorMessage,
        formattedOptions,
        handleCodeEditorChange,
        handleDeleteCustomPropertyClick,
        handleFromAiClick,
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
        isDisplayConditionsPending,
        isFetchingCurrentDisplayCondition,
        isFormulaMode,
        isFromAi,
        isNumericalInput,
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
    });

    const setDataPillPanelOpen = useDataPillPanelStore((state) => state.setDataPillPanelOpen);

    if (hidden && !control) {
        return <></>;
    }

    if (displayCondition && isDisplayConditionsPending && type !== 'ARRAY' && type !== 'OBJECT') {
        return (
            <div className={twMerge('flex flex-col space-y-1', objectName && 'ml-2 mt-1')}>
                <Skeleton className="h-5 w-1/4" />

                <Skeleton className="h-9 w-full" />
            </div>
        );
    }

    if (
        displayConditionsQuery &&
        displayCondition &&
        currentComponent?.displayConditions?.[displayCondition] &&
        (isFetchingCurrentDisplayCondition || isDisplayConditionsPending) &&
        type !== 'ARRAY' &&
        type !== 'OBJECT'
    ) {
        return (
            <div className={twMerge('flex flex-col space-y-1', objectName && 'ml-2 mt-1')}>
                <Skeleton className="h-5 w-1/4" />

                <Skeleton className="h-9 w-full" />
            </div>
        );
    }

    if (displayCondition && !currentComponent?.displayConditions?.[displayCondition]) {
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
                    {((controlType === 'OBJECT_BUILDER' && name !== '__item') ||
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
                                                    <TooltipContent className="max-w-md">{description}</TooltipContent>
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

                    {control && controlType === 'ARRAY_BUILDER' && calculatedPath && (
                        <ControlledArrayItems
                            control={control}
                            controlPath={calculatedPath}
                            formState={formState}
                            property={property}
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
                                    />
                                ))}
                            </ul>
                        )}

                    {control &&
                        (controlType === 'OBJECT_BUILDER' || type === 'FILE_ENTRY') &&
                        calculatedPath &&
                        !property.properties?.length && (
                            <ControlledObjectEntries
                                control={control}
                                controlPath={calculatedPath}
                                formState={formState}
                                property={property}
                            />
                        )}

                    {control && (isValidControlType || isNumericalInput) && calculatedPath && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            name={calculatedPath}
                            render={({field}) => (
                                <PropertyInput
                                    description={description}
                                    error={hasError}
                                    errorMessage={errorMessage}
                                    label={label || name}
                                    leadingIcon={typeIcon}
                                    onFocus={() => setDataPillPanelOpen(true)}
                                    placeholder={placeholder}
                                    required={required}
                                    type={hidden ? 'hidden' : getInputHTMLType(controlType)}
                                    {...field}
                                />
                            )}
                            rules={{required}}
                        />
                    )}

                    {control &&
                        controlType === 'SELECT' &&
                        type !== 'BOOLEAN' &&
                        calculatedPath &&
                        optionsDataSource &&
                        workflow.id &&
                        currentNode?.name && (
                            <Controller
                                control={control}
                                defaultValue={defaultValue}
                                name={calculatedPath}
                                render={({field: {name: fieldName, onBlur, onChange, value: fieldValue}}) => (
                                    <PropertyComboBox
                                        arrayIndex={arrayIndex}
                                        defaultValue={defaultValue}
                                        description={description}
                                        label={label || fieldName}
                                        leadingIcon={typeIcon}
                                        lookupDependsOnPaths={optionsDataSource.optionsLookupDependsOn?.map(
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
                                        value={fieldValue !== undefined ? fieldValue : selectValue}
                                        workflowId={workflow.id!}
                                        workflowNodeName={currentNode!.name}
                                    />
                                )}
                                rules={{required}}
                            />
                        )}

                    {control &&
                        controlType === 'SELECT' &&
                        type !== 'BOOLEAN' &&
                        calculatedPath &&
                        !optionsDataSource && (
                            <Controller
                                control={control}
                                defaultValue={defaultValue}
                                name={calculatedPath}
                                render={({field: {name, onChange}}) => (
                                    <PropertySelect
                                        description={description}
                                        label={label || name}
                                        leadingIcon={typeIcon}
                                        name={name}
                                        onValueChange={(value) => {
                                            onChange(value);

                                            setSelectValue(value);
                                        }}
                                        options={options as Array<SelectOptionType>}
                                        required={required}
                                        value={selectValue}
                                    />
                                )}
                                rules={{required}}
                            />
                        )}

                    {control && controlType === 'SELECT' && type === 'BOOLEAN' && calculatedPath && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            name={calculatedPath}
                            render={({field: {name, onChange}}) => (
                                <PropertySelect
                                    description={description}
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
                                />
                            )}
                            rules={{required}}
                        />
                    )}

                    {control && controlType === 'TEXT_AREA' && calculatedPath && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            name={calculatedPath}
                            render={({field}) => (
                                <PropertyTextArea
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

                    {control && controlType === 'MULTI_SELECT' && calculatedPath && workflow.id && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue || []}
                            name={calculatedPath}
                            render={({field: {onChange, value}}) => (
                                <PropertyMultiSelect
                                    defaultValue={(value as string[]) || []}
                                    deletePropertyButton={null}
                                    leadingIcon={typeIcon}
                                    onChange={onChange}
                                    options={(formattedOptions as MultiSelectOptionType[]) || []}
                                    optionsDataSource={optionsDataSource}
                                    path={calculatedPath}
                                    property={property}
                                    showInputTypeSwitchButton={false}
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
                            options={(formattedOptions as Array<Option>) || undefined || []}
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

            {type === 'DYNAMIC_PROPERTIES' && currentNode && (
                <PropertyDynamicProperties
                    currentOperationName={operationName}
                    enabled={
                        !!(currentNode?.connectionId && currentNode?.connections) ||
                        currentNode?.connections?.length === 0
                    }
                    lookupDependsOnPaths={propertiesDataSource?.propertiesLookupDependsOn}
                    lookupDependsOnValues={lookupDependsOnValues}
                    name={name}
                    parameterValue={propertyParameterValue}
                    path={calculatedPath}
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
