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
import {useProperty} from '@/pages/platform/workflow-editor/components/properties/useProperty';
import getInputHTMLType from '@/pages/platform/workflow-editor/utils/getInputHTMLType';
import {
    GetClusterElementParameterDisplayConditions200Response,
    Option,
} from '@/shared/middleware/platform/configuration';
import {ArrayPropertyType, PropertyAllType, SelectOptionType} from '@/shared/types';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {UseQueryResult} from '@tanstack/react-query';
import {CircleQuestionMarkIcon} from 'lucide-react';
import {ReactNode} from 'react';
import {Control, Controller, FieldValues, FormState} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';

interface PropertyProps {
    arrayIndex?: number;
    arrayName?: string;
    control?: Control<FieldValues, FieldValues>;
    controlPath?: string;
    customClassName?: string;
    deletePropertyButton?: ReactNode;
    displayConditionsQuery?: UseQueryResult<GetClusterElementParameterDisplayConditions200Response, Error>;
    formState?: FormState<FieldValues>;
    objectName?: string;
    operationName?: string;
    parameterValue?: unknown;
    parentArrayItems?: Array<ArrayPropertyType>;
    path?: string;
    property: PropertyAllType;
}

const Property = ({
    arrayIndex,
    arrayName,
    control,
    controlPath = 'parameters',
    customClassName,
    deletePropertyButton,
    displayConditionsQuery,
    formState,
    objectName,
    operationName,
    parameterValue,
    parentArrayItems,
    path,
    property,
}: PropertyProps) => {
    const {
        controlType,
        currentComponent,
        currentNode,
        defaultValue,
        description,
        displayCondition,
        editorRef,
        errorMessage,
        formattedOptions,
        getFetchedState,
        handleCodeEditorChange,
        handleDeleteCustomPropertyClick,
        handleInputChange,
        handleInputTypeSwitchButtonClick,
        handleJsonSchemaBuilderChange,
        handleMultiSelectChange,
        handleSelectChange,
        hasError,
        hidden,
        inputRef,
        inputValue,
        isDisplayConditionsPending,
        isFetchingCurrentDisplayCondition,
        isFormulaMode,
        isNumericalInput,
        isValidControlType,
        label,
        languageId,
        lookupDependsOnValues,
        maxLength,
        maxValue,
        memoizedPath,
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
        workflow,
    } = useProperty({
        arrayIndex,
        arrayName,
        control,
        controlPath,
        displayConditionsQuery,
        formState,
        objectName,
        operationName,
        parameterValue,
        parentArrayItems,
        path,
        property,
    });

    if (hidden && !control) {
        return <></>;
    }

    const isFetched = getFetchedState({
        currentComponent,
        displayCondition,
        displayConditionsQuery,
        isDisplayConditionsPending,
        isFetchingCurrentDisplayCondition,
        type,
    });

    if (isFetched) {
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
            className={twMerge(
                'w-full',
                hidden && 'hidden',
                controlType === 'OBJECT_BUILDER' && 'flex-col',
                controlType === 'ARRAY_BUILDER' && 'flex-col',
                customClassName
            )}
            key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
        >
            {Boolean(
                mentionInput && currentComponent && type !== 'DYNAMIC_PROPERTIES' && controlType !== 'CODE_EDITOR'
            ) && (
                <PropertyMentionsInput
                    controlType={controlType || 'TEXT'}
                    defaultValue={defaultValue}
                    deletePropertyButton={deletePropertyButton}
                    description={description}
                    handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                    isFormulaMode={isFormulaMode}
                    label={label || name}
                    leadingIcon={typeIcon}
                    path={memoizedPath}
                    placeholder={placeholder}
                    ref={editorRef}
                    required={required}
                    setIsFormulaMode={setIsFormulaMode}
                    showInputTypeSwitchButton={showInputTypeSwitchButton}
                    type={type}
                    value={mentionInputValue}
                />
            )}

            {!mentionInput && (
                <>
                    {((controlType === 'OBJECT_BUILDER' && name !== '__item') ||
                        controlType === 'ARRAY_BUILDER' ||
                        controlType === 'NULL') && (
                        <div className={twMerge('flex items-center', controlType !== 'NULL' && 'pb-1')}>
                            {Boolean(typeIcon && controlType !== 'NULL') && (
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

                    {controlType === 'ARRAY_BUILDER' && memoizedPath && (
                        <ArrayProperty
                            onDeleteClick={handleDeleteCustomPropertyClick}
                            parentArrayItems={parentArrayItems}
                            path={memoizedPath}
                            property={property}
                        />
                    )}

                    {(controlType === 'OBJECT_BUILDER' || type === 'FILE_ENTRY') && (
                        <ObjectProperty
                            arrayIndex={arrayIndex}
                            arrayName={arrayName}
                            onDeleteClick={handleDeleteCustomPropertyClick}
                            operationName={operationName}
                            path={memoizedPath}
                            property={property}
                        />
                    )}

                    {control && (isValidControlType || isNumericalInput) && memoizedPath && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            name={memoizedPath}
                            render={({field}) => (
                                <PropertyInput
                                    description={description}
                                    error={hasError}
                                    errorMessage={errorMessage}
                                    label={label || name}
                                    leadingIcon={typeIcon}
                                    placeholder={placeholder}
                                    required={required}
                                    type={hidden ? 'hidden' : getInputHTMLType(controlType)}
                                    {...field}
                                />
                            )}
                            rules={{required}}
                        />
                    )}

                    {control && controlType === 'SELECT' && type !== 'BOOLEAN' && memoizedPath && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            name={memoizedPath}
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

                    {control && controlType === 'SELECT' && type === 'BOOLEAN' && memoizedPath && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            name={memoizedPath}
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

                    {control && controlType === 'TEXT_AREA' && memoizedPath && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            name={memoizedPath}
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

                    {!control && (isValidControlType || isNumericalInput) && path && (
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
                            name={memoizedPath}
                            onChange={handleInputChange}
                            placeholder={
                                isNumericalInput && minValue && maxValue
                                    ? `From ${minValue} to ${maxValue}`
                                    : placeholder || `Type ${isNumericalInput ? 'a number' : 'something...'}`
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
                            path={memoizedPath}
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
                            defaultValue={multiSelectValue}
                            deletePropertyButton={deletePropertyButton}
                            handleInputTypeSwitchButtonClick={() => handleInputTypeSwitchButtonClick()}
                            leadingIcon={typeIcon}
                            lookupDependsOnPaths={optionsDataSource?.optionsLookupDependsOn?.map(
                                (optionLookupDependency: string) =>
                                    optionLookupDependency.replace('[index]', `[${arrayIndex}]`)
                            )}
                            lookupDependsOnValues={lookupDependsOnValues}
                            onChange={(value) => handleMultiSelectChange(value)}
                            options={formattedOptions as Array<SelectOptionType>}
                            optionsDataSource={optionsDataSource}
                            path={memoizedPath}
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
                    path={memoizedPath}
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
                    value={inputValue}
                    workflow={workflow}
                    workflowNodeName={currentNode?.name ?? ''}
                />
            )}
        </li>
    );
};

export default Property;
