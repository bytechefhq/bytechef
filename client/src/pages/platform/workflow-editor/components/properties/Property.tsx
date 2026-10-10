import {MultiSelectOptionType} from '@/components/MultiSelect/MultiSelect';
import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipPortal, TooltipTrigger} from '@/components/ui/tooltip';
import ArrayProperty from '@/pages/platform/workflow-editor/components/properties/ArrayProperty';
import {useClusterElementContext} from '@/pages/platform/workflow-editor/components/properties/ClusterElementContext';
import {useFormDisplayConditionsContext} from '@/pages/platform/workflow-editor/components/properties/FormDisplayConditionsContext';
import ObjectProperty from '@/pages/platform/workflow-editor/components/properties/ObjectProperty';
import FormControlledArrayItems from '@/pages/platform/workflow-editor/components/properties/components/FormControlledArrayItems';
import FormControlledObjectEntries from '@/pages/platform/workflow-editor/components/properties/components/FormControlledObjectEntries';
import FormLookupValuesWatcher from '@/pages/platform/workflow-editor/components/properties/components/FormLookupValuesWatcher';
import FromAiToggleButton from '@/pages/platform/workflow-editor/components/properties/components/FromAiToggleButton';
import PropertyComboBox from '@/pages/platform/workflow-editor/components/properties/components/PropertyComboBox';
import PropertyDynamicProperties from '@/pages/platform/workflow-editor/components/properties/components/PropertyDynamicProperties';
import PropertyDynamicSwitch from '@/pages/platform/workflow-editor/components/properties/components/PropertyDynamicSwitch';
import PropertyFormulaSwitch from '@/pages/platform/workflow-editor/components/properties/components/PropertyFormulaSwitch';
import PropertyMultiSelect from '@/pages/platform/workflow-editor/components/properties/components/PropertyMultiSelect';
import PropertySelect from '@/pages/platform/workflow-editor/components/properties/components/PropertySelect';
import PropertyTextArea from '@/pages/platform/workflow-editor/components/properties/components/PropertyTextArea';
import PropertyCodeEditor from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/PropertyCodeEditor';
import PropertyInput from '@/pages/platform/workflow-editor/components/properties/components/property-input/PropertyInput';
import PropertyJsonSchemaBuilder from '@/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/PropertyJsonSchemaBuilder';
import PropertyMentionsInput from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInput';
import {reconstructControlledExpressionValue} from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/controlledExpressionValue';
import {getMentionsInputPlaceholder} from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/mentionsInputPlaceholder';
import getControlledToolFieldState from '@/pages/platform/workflow-editor/components/properties/getControlledToolFieldState';
import getPropertyInputPlaceholder from '@/pages/platform/workflow-editor/components/properties/getPropertyInputPlaceholder';
import getPropertyKey from '@/pages/platform/workflow-editor/components/properties/getPropertyKey';
import usePillTarget from '@/pages/platform/workflow-editor/components/properties/hooks/usePillTarget';
import useProperty from '@/pages/platform/workflow-editor/components/properties/hooks/useProperty';
import isDynamicPropertiesQueryEnabled from '@/pages/platform/workflow-editor/components/properties/isDynamicPropertiesQueryEnabled';
import {isEmptyPillContainerValue} from '@/pages/platform/workflow-editor/components/properties/pillContainerValue';
import {isStringSelect} from '@/pages/platform/workflow-editor/components/properties/propertyInputMode';
import useOpenDataPillPanel from '@/pages/platform/workflow-editor/hooks/useOpenDataPillPanel';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {encodeParameters, encodePath, safeResolvePath} from '@/pages/platform/workflow-editor/utils/encodingUtils';
import getInputHTMLType from '@/pages/platform/workflow-editor/utils/getInputHTMLType';
import resolveExpressionValue from '@/pages/platform/workflow-editor/utils/resolveExpressionValue';
import {ERROR_MESSAGES} from '@/shared/errorMessages';
import {
    GetClusterElementParameterDisplayConditions200Response,
    Option,
} from '@/shared/middleware/platform/configuration';
import {ArrayPropertyType, PropertyAllType, SelectOptionType} from '@/shared/types';
import {UseQueryResult} from '@tanstack/react-query';
import {CircleQuestionMarkIcon, SquareFunctionIcon, XIcon} from 'lucide-react';
import {KeyboardEvent, MouseEvent, ReactNode, useCallback, useRef, useState} from 'react';
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
    hideFromAi?: boolean;
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
    hideFromAi,
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
        controlledExpressionExitRef,
        controlledFormulaOnChangeRef,
        controlledFromAi,
        currentNode,
        defaultValue,
        description,
        displayCondition,
        editorFocusRequest,
        editorPendingSaveCancelRef,
        editorRef,
        errorMessage,
        expressionEnabled,
        formattedOptions,
        fromAiExpression,
        handleCodeEditorChange,
        handleControlledBlur,
        handleControlledBuilderFormulaSwitch,
        handleControlledFormulaSwitch,
        handleControlledNativeFromAiClick,
        handleControlledNativeKeyDown,
        handleDeleteCustomPropertyClick,
        handleDynamicSwitch,
        handleFormulaSwitch,
        handleFromAiClick,
        handleFromAiToggle,
        handleInputChange,
        handleInputClear,
        handleJsonSchemaBuilderChange,
        handleMentionInputValueChange,
        handleMultiSelectChange,
        handleNativeKeyDown,
        handleSelectChange,
        handleSelectKeyDown,
        handleSinglePillAbandoned,
        hasDynamicSwitch,
        hasError,
        hidden,
        inputMode,
        inputRef,
        inputValue,
        insertPillValue,
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
        optionsLoadedDynamically,
        placeholder,
        propertiesDataSource,
        propertyParameterValue,
        required,
        selectValue,
        setIsFormulaMode,
        setLookupDependsOnValues,
        setSelectValue,
        showFormulaSwitch,
        startPillEntry,
        type,
        typeIcon,
        validateMentionInputValue,
        validatePropertyValue,
        workflow,
    } = useProperty({
        arrayIndex,
        arrayName,
        control,
        controlPath,
        displayConditionsQuery,
        dynamicPropertySource,
        formState,
        hideFromAi,
        objectName,
        operationName,
        parameterValue,
        parentArrayItems,
        path,
        property,
        toolsMode,
    });

    const [containerHasLocalEntries, setContainerHasLocalEntries] = useState(false);

    const containerHasLocalEntriesRef = useRef(false);

    const workflowNodeDetailsPanelOpen = useWorkflowNodeDetailsPanelStore(
        (state) => state.workflowNodeDetailsPanelOpen
    );

    const clusterElementContext = useClusterElementContext();

    const formDisplayConditions = useFormDisplayConditionsContext();

    const openDataPillPanel = useOpenDataPillPanel();

    const isPillContainer =
        controlType === 'OBJECT_BUILDER' ||
        controlType === 'ARRAY_BUILDER' ||
        controlType === 'JSON_SCHEMA_BUILDER' ||
        type === 'FILE_ENTRY';

    const isPillContainerEmpty = () => {
        const parameters = currentNode?.parameters;

        const containerValue =
            parameters && calculatedPath
                ? safeResolvePath(encodeParameters(parameters), encodePath(calculatedPath))
                : undefined;

        return isEmptyPillContainerValue({
            controlType: type === 'FILE_ENTRY' ? 'FILE_ENTRY' : controlType,
            definedPropertyNames: property.properties
                ?.map((subProperty) => subProperty.name)
                .filter((subPropertyName): subPropertyName is string => !!subPropertyName),
            value: containerValue,
        });
    };

    const acceptsNativePill = () =>
        !control &&
        expressionEnabled !== false &&
        !isFromAi &&
        (!isPillContainer || (!containerHasLocalEntriesRef.current && isPillContainerEmpty()));

    const nativePillTarget = usePillTarget({
        acceptsPill: acceptsNativePill,
        insertPill: insertPillValue,
    });

    const handleContainerMouseDown = (event: MouseEvent<HTMLElement>) => {
        const picked = nativePillTarget.onContainerMouseDown(event);

        if (picked && acceptsNativePill() && workflowNodeDetailsPanelOpen) {
            openDataPillPanel();
        }
    };

    const handleContainerKeyDown = (event: KeyboardEvent<HTMLElement>) => {
        if (
            (event.key !== '$' && event.key !== '=') ||
            !nativePillTarget.isContainerKeyDown(event) ||
            !acceptsNativePill() ||
            (event.key === '=' && !showFormulaSwitch)
        ) {
            return;
        }

        event.preventDefault();

        if (event.key === '=') {
            handleFormulaSwitch();

            return;
        }

        startPillEntry('$');
    };

    const handleContainerLocalEntriesChange = useCallback((hasLocalEntries: boolean) => {
        containerHasLocalEntriesRef.current = hasLocalEntries;

        setContainerHasLocalEntries(hasLocalEntries);
    }, []);

    const showContainerPillHint =
        isPillContainer &&
        !control &&
        expressionEnabled !== false &&
        !isFromAi &&
        !isFormulaMode &&
        !containerHasLocalEntries &&
        isPillContainerEmpty();

    const showDynamicSwitch =
        hasDynamicSwitch &&
        !isFromAi &&
        !isFormulaMode &&
        (controlType === 'SELECT' || mentionInput || (!containerHasLocalEntries && isPillContainerEmpty()));

    const dynamicSwitch = showDynamicSwitch ? (
        <PropertyDynamicSwitch dynamic={mentionInput} handleClick={handleDynamicSwitch} />
    ) : undefined;

    // An array item's object has no label row to hold a Dynamic switch, so it keeps the hint.
    const containerPillHint =
        showContainerPillHint && !hasDynamicSwitch ? (
            <p
                className={twMerge(
                    'mb-2 rounded-md border border-dashed border-stroke-neutral-secondary px-3 py-2 text-xs text-muted-foreground outline-none',
                    nativePillTarget.isRegistered && 'ring-2 ring-ring'
                )}
                tabIndex={-1}
            >
                Drop or click a data pill, or type $
            </p>
        ) : null;

    const requiredRule = required ? ERROR_MESSAGES.PROPERTY.FIELD_REQUIRED : false;

    const offersFromAi = !!isToolsClusterElement && expressionEnabled !== false;

    const nativeFromAiButton =
        !control && offersFromAi && handleFromAiClick ? (
            <FromAiToggleButton isFromAi={false} onToggle={handleFromAiClick} />
        ) : undefined;

    const getControlledFromAiButton = (fieldOnChange: (value: string) => void) =>
        control && offersFromAi ? (
            <FromAiToggleButton isFromAi={false} onToggle={() => handleControlledNativeFromAiClick(fieldOnChange)} />
        ) : undefined;

    const withFromAiButton = (fromAiButton: ReactNode) =>
        fromAiButton ? (
            <>
                {fromAiButton}

                {deletePropertyButton}
            </>
        ) : (
            deletePropertyButton
        );

    if (hidden && !control) {
        return <></>;
    }

    if (isLoadingDisplayCondition) {
        return (
            <div className={twMerge('flex flex-col space-y-1', objectName && 'mt-1 ml-2')}>
                <Skeleton className="h-5 w-1/4" />

                <Skeleton className="h-9 w-full" />
            </div>
        );
    }

    if (!control && displayCondition && !currentNode?.displayConditions?.[displayCondition]) {
        return <></>;
    }

    if (control && displayCondition && formDisplayConditions && !formDisplayConditions[displayCondition]) {
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
            key={`${currentNode?.name}_${currentNode?.operationName}_${name}`}
        >
            {control &&
                !!(
                    propertiesDataSource?.propertiesLookupDependsOn?.length ||
                    optionsDataSource?.optionsLookupDependsOn?.length
                ) && (
                    <FormLookupValuesWatcher
                        arrayIndex={arrayIndex}
                        control={control}
                        optionsLookupDependsOn={optionsDataSource?.optionsLookupDependsOn}
                        propertiesLookupDependsOn={propertiesDataSource?.propertiesLookupDependsOn}
                        setLookupDependsOnValues={setLookupDependsOnValues}
                    />
                )}

            {mentionInput && currentNode && type !== 'DYNAMIC_PROPERTIES' && controlType !== 'CODE_EDITOR' && (
                <PropertyMentionsInput
                    cancelPendingSaveRef={editorPendingSaveCancelRef}
                    controlType={controlType || 'TEXT'}
                    defaultValue={parameterValue !== undefined ? parameterValue : defaultValue}
                    deletePropertyButton={deletePropertyButton}
                    description={description}
                    dynamicSwitch={dynamicSwitch}
                    error={hasError}
                    errorMessage={errorMessage}
                    expressionEnabled={expressionEnabled}
                    focusRequest={editorFocusRequest}
                    handleFromAiClick={handleFromAiClick}
                    handleInputTypeSwitchButtonClick={handleFormulaSwitch}
                    isFormulaMode={isFormulaMode}
                    isFromAi={isFromAi}
                    label={label || name}
                    leadingIcon={typeIcon}
                    onSinglePillAbandoned={handleSinglePillAbandoned}
                    onValueChange={handleMentionInputValueChange}
                    path={calculatedPath}
                    pillRequired={isStringSelect(controlType, type) && !isFormulaMode}
                    placeholder={placeholder}
                    ref={editorRef}
                    required={required}
                    setIsFormulaMode={setIsFormulaMode}
                    showInputTypeSwitchButton={showFormulaSwitch}
                    singlePill={inputMode.singlePill}
                    toolProperty={isToolsClusterElement}
                    type={type}
                    validateBeforeSave={validateMentionInputValue}
                    value={mentionInputValue}
                />
            )}

            {!mentionInput && (
                <div
                    className="contents"
                    {...nativePillTarget.targetProps}
                    onKeyDown={isPillContainer && !control ? handleContainerKeyDown : undefined}
                    onMouseDown={isPillContainer && !control ? handleContainerMouseDown : undefined}
                >
                    {!isFormulaMode &&
                        ((controlType === 'OBJECT_BUILDER' && name !== '__item') ||
                            controlType === 'ARRAY_BUILDER' ||
                            controlType === 'NULL') && (
                            <div className={twMerge('flex items-center', controlType !== 'NULL' && 'pb-1')}>
                                {typeIcon && controlType !== 'NULL' && (
                                    <span className={twMerge(label ? 'pr-2' : 'pr-1')} title={type}>
                                        {typeIcon}
                                    </span>
                                )}

                                {(label || description || dynamicSwitch || (showFormulaSwitch && !control)) && (
                                    <div className="flex w-full items-center justify-between">
                                        <div className="flex items-center">
                                            {label && (
                                                <Label className="gap-0 leading-normal">
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
                                            {dynamicSwitch}

                                            {showFormulaSwitch && !control && (
                                                <PropertyFormulaSwitch
                                                    formulaMode={isFormulaMode}
                                                    handleClick={handleFormulaSwitch}
                                                />
                                            )}

                                            {control && showFormulaSwitch && (
                                                <PropertyFormulaSwitch
                                                    formulaMode={isFormulaMode}
                                                    handleClick={handleControlledBuilderFormulaSwitch}
                                                />
                                            )}

                                            {deletePropertyButton}
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}

                    {containerPillHint}

                    {!control && controlType === 'ARRAY_BUILDER' && calculatedPath && (
                        <ArrayProperty
                            onDeleteClick={handleDeleteCustomPropertyClick}
                            onHasItemsChange={handleContainerLocalEntriesChange}
                            parentArrayItems={parentArrayItems}
                            path={calculatedPath}
                            property={property}
                        />
                    )}

                    {control && isFormulaMode && calculatedPath && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            name={calculatedPath}
                            render={({field}) => {
                                controlledFormulaOnChangeRef.current = field.onChange;

                                const displayValue = typeof field.value === 'string' ? field.value : '';
                                const valueIsFromAi =
                                    typeof field.value === 'string' && field.value.startsWith('=fromAi(');

                                const isFieldFromAi =
                                    isToolsClusterElement &&
                                    (controlledFromAi !== undefined ? controlledFromAi : valueIsFromAi);

                                const {onChange: fieldOnChange} = field;

                                return (
                                    <PropertyMentionsInput
                                        controlType={controlType || 'TEXT'}
                                        deletePropertyButton={deletePropertyButton}
                                        description={description}
                                        disableAutoSave
                                        error={hasError}
                                        errorMessage={errorMessage}
                                        expressionEnabled={expressionEnabled}
                                        focusRequest={editorFocusRequest}
                                        handleFromAiClick={
                                            isToolsClusterElement
                                                ? (fromAi) => handleFromAiToggle(fromAi, fieldOnChange)
                                                : undefined
                                        }
                                        handleInputTypeSwitchButtonClick={() =>
                                            handleControlledFormulaSwitch(field.value, fieldOnChange)
                                        }
                                        isFormulaMode
                                        isFromAi={isFieldFromAi}
                                        label={label || name}
                                        leadingIcon={typeIcon}
                                        onValueChange={(value) =>
                                            fieldOnChange(reconstructControlledExpressionValue(value))
                                        }
                                        path={calculatedPath}
                                        placeholder={placeholder}
                                        required={required}
                                        setIsFormulaMode={(formulaMode) => {
                                            if (!formulaMode) {
                                                controlledExpressionExitRef.current = true;

                                                setIsFormulaMode(false);

                                                fieldOnChange('');
                                            }
                                        }}
                                        showInputTypeSwitchButton={showFormulaSwitch && !isFieldFromAi}
                                        toolProperty={isToolsClusterElement}
                                        type={type}
                                        value={displayValue}
                                    />
                                );
                            }}
                            rules={{required: requiredRule}}
                        />
                    )}

                    {control && !isFormulaMode && controlType === 'ARRAY_BUILDER' && calculatedPath && (
                        <FormControlledArrayItems
                            control={control}
                            controlPath={calculatedPath}
                            formState={formState}
                            hideFromAi={hideFromAi}
                            property={property}
                            toolsMode={isToolsClusterElement}
                        />
                    )}

                    {!control && (controlType === 'OBJECT_BUILDER' || type === 'FILE_ENTRY') && (
                        <ObjectProperty
                            arrayIndex={arrayIndex}
                            arrayName={arrayName}
                            onDeleteClick={handleDeleteCustomPropertyClick}
                            onHasCustomEntriesChange={handleContainerLocalEntriesChange}
                            operationName={operationName}
                            path={calculatedPath}
                            property={property}
                        />
                    )}

                    {control &&
                        !isFormulaMode &&
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
                                        hideFromAi={hideFromAi}
                                        key={
                                            getPropertyKey(subProperty.name, subProperty.displayCondition) ||
                                            `${property.name}_${index}`
                                        }
                                        property={subProperty}
                                        toolsMode={isToolsClusterElement}
                                    />
                                ))}
                            </ul>
                        )}

                    {control &&
                        !isFormulaMode &&
                        (controlType === 'OBJECT_BUILDER' || type === 'FILE_ENTRY') &&
                        calculatedPath &&
                        !property.properties?.length && (
                            <FormControlledObjectEntries
                                control={control}
                                controlPath={calculatedPath}
                                formState={formState}
                                hideFromAi={hideFromAi}
                                property={property}
                                toolsMode={isToolsClusterElement}
                            />
                        )}

                    {control && !isFormulaMode && (isValidControlType || isNumericalInput) && calculatedPath && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            name={calculatedPath}
                            render={({field, fieldState}) => {
                                const {
                                    displayValue,
                                    isExpressionMode,
                                    isFieldFromAi,
                                    showFromAi,
                                    strippedDisplayValue,
                                    strippedFromAiValue,
                                } = getControlledToolFieldState({
                                    controlledFromAi,
                                    fieldValue: field.value,
                                    fromAiExpression,
                                    isToolsClusterElement,
                                    type,
                                });

                                const {onChange: fieldOnChange, ...fieldRest} = field;

                                let inputPlaceholder =
                                    placeholder || `Type ${isNumericalInput ? 'a number' : 'something'}...`;

                                if (isNumericalInput && minValue && maxValue) {
                                    inputPlaceholder = `From ${minValue} to ${maxValue}`;
                                } else if (showFromAi) {
                                    inputPlaceholder = getMentionsInputPlaceholder({
                                        expressionEnabled,
                                        placeholder,
                                        toolProperty: true,
                                    });
                                }

                                return (
                                    <>
                                        {showFromAi && (isExpressionMode || isFieldFromAi) ? (
                                            <PropertyMentionsInput
                                                autoFocus={displayValue === '='}
                                                controlType={controlType || 'TEXT'}
                                                deletePropertyButton={deletePropertyButton}
                                                description={description}
                                                disableAutoSave
                                                error={!!fieldState.error || !!controlledBlurError}
                                                errorMessage={fieldState.error?.message || controlledBlurError}
                                                expressionEnabled={expressionEnabled}
                                                handleFromAiClick={(fromAi) =>
                                                    handleFromAiToggle(fromAi, fieldOnChange)
                                                }
                                                handleInputTypeSwitchButtonClick={() =>
                                                    handleControlledFormulaSwitch(field.value, fieldOnChange)
                                                }
                                                isFormulaMode
                                                isFromAi={isFieldFromAi}
                                                label={label || name}
                                                leadingIcon={typeIcon}
                                                onValueChange={(value) =>
                                                    fieldOnChange(reconstructControlledExpressionValue(value))
                                                }
                                                path={calculatedPath}
                                                placeholder={placeholder}
                                                required={required}
                                                setIsFormulaMode={(formulaMode) => {
                                                    if (!formulaMode) {
                                                        controlledExpressionExitRef.current = true;

                                                        fieldOnChange('');
                                                    }
                                                }}
                                                showInputTypeSwitchButton={showFormulaSwitch && !isFieldFromAi}
                                                toolProperty
                                                type={type}
                                                value={displayValue}
                                            />
                                        ) : (
                                            <PropertyInput
                                                {...fieldRest}
                                                autoFocus={controlledExpressionExitRef.current}
                                                deletePropertyButton={deletePropertyButton}
                                                description={description}
                                                disabled={isFieldFromAi}
                                                error={!!fieldState.error || !!controlledBlurError}
                                                errorMessage={fieldState.error?.message || controlledBlurError}
                                                expressionPrefix={showFromAi}
                                                fieldsetClassName={objectName && arrayName && 'ml-2'}
                                                handleInputTypeSwitchButtonClick={
                                                    showFormulaSwitch
                                                        ? () =>
                                                              handleControlledFormulaSwitch(field.value, fieldOnChange)
                                                        : undefined
                                                }
                                                inputOverlay={
                                                    isFieldFromAi ? (
                                                        <span className="flex h-full flex-1 items-center pl-property-input-position text-sm font-medium text-muted-foreground italic">
                                                            Automatically defined by the model
                                                        </span>
                                                    ) : undefined
                                                }
                                                label={label || name}
                                                leadingIcon={
                                                    isFieldFromAi ? <SquareFunctionIcon className="size-4" /> : typeIcon
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
                                                onKeyDown={(event) =>
                                                    handleControlledNativeKeyDown(event, field.value, fieldOnChange)
                                                }
                                                placeholder={inputPlaceholder}
                                                required={required}
                                                showInputTypeSwitchButton={showFormulaSwitch}
                                                title={type}
                                                trailingAction={
                                                    showFromAi
                                                        ? expressionEnabled !== false && (
                                                              <FromAiToggleButton
                                                                  isFromAi={!!isFieldFromAi}
                                                                  onToggle={(fromAi) =>
                                                                      handleFromAiToggle(fromAi, fieldOnChange)
                                                                  }
                                                              />
                                                          )
                                                        : getControlledFromAiButton(fieldOnChange)
                                                }
                                                type={hidden ? 'hidden' : getInputHTMLType(controlType)}
                                                value={isFieldFromAi ? strippedFromAiValue : strippedDisplayValue}
                                            />
                                        )}

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

                    {control && !isFormulaMode && controlType === 'SELECT' && type !== 'BOOLEAN' && calculatedPath && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            name={calculatedPath}
                            render={({field: {name: fieldName, onBlur, onChange, value: fieldValue}, fieldState}) => (
                                <PropertyComboBox
                                    arrayIndex={arrayIndex}
                                    defaultValue={defaultValue}
                                    deletePropertyButton={withFromAiButton(getControlledFromAiButton(onChange))}
                                    description={description}
                                    error={!!fieldState.error || hasError}
                                    errorMessage={fieldState.error?.message || errorMessage}
                                    handleInputTypeSwitchButtonClick={() =>
                                        handleControlledFormulaSwitch(fieldValue, onChange)
                                    }
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
                                    optionsLoadedDynamically={optionsLoadedDynamically}
                                    path={calculatedPath}
                                    propertyName={name}
                                    required={required}
                                    showInputTypeSwitchButton={showFormulaSwitch}
                                    value={fieldValue !== undefined ? fieldValue : selectValue}
                                    workflowId={workflow.id!}
                                    workflowNodeName={currentNode?.name ?? ''}
                                />
                            )}
                            rules={{required: requiredRule}}
                        />
                    )}

                    {control && !isFormulaMode && controlType === 'SELECT' && type === 'BOOLEAN' && calculatedPath && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            name={calculatedPath}
                            render={({field: {name, onChange, value: fieldValue}}) => (
                                <PropertySelect
                                    deletePropertyButton={withFromAiButton(getControlledFromAiButton(onChange))}
                                    description={description}
                                    handleInputTypeSwitchButtonClick={() =>
                                        handleControlledFormulaSwitch(fieldValue, onChange)
                                    }
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
                                    showInputTypeSwitchButton={showFormulaSwitch}
                                    value={fieldValue !== undefined ? fieldValue : selectValue}
                                />
                            )}
                            rules={{required: requiredRule}}
                        />
                    )}

                    {control && !isFormulaMode && controlType === 'TEXT_AREA' && calculatedPath && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            name={calculatedPath}
                            render={({field, fieldState}) => {
                                const {
                                    displayValue,
                                    isExpressionMode,
                                    isFieldFromAi,
                                    showFromAi,
                                    strippedDisplayValue,
                                } = getControlledToolFieldState({
                                    controlledFromAi,
                                    fieldValue: field.value,
                                    fromAiExpression,
                                    isToolsClusterElement,
                                    type,
                                });

                                const {onChange: fieldOnChange, ...fieldRest} = field;

                                if (showFromAi && (isExpressionMode || isFieldFromAi)) {
                                    return (
                                        <PropertyMentionsInput
                                            autoFocus={displayValue === '='}
                                            controlType="TEXT_AREA"
                                            deletePropertyButton={deletePropertyButton}
                                            description={description}
                                            disableAutoSave
                                            error={!!fieldState.error || !!controlledBlurError}
                                            errorMessage={fieldState.error?.message || controlledBlurError}
                                            expressionEnabled={expressionEnabled}
                                            handleFromAiClick={(fromAi) => handleFromAiToggle(fromAi, fieldOnChange)}
                                            handleInputTypeSwitchButtonClick={() =>
                                                handleControlledFormulaSwitch(field.value, fieldOnChange)
                                            }
                                            isFormulaMode
                                            isFromAi={isFieldFromAi}
                                            label={label || name}
                                            leadingIcon={typeIcon}
                                            onValueChange={(value) =>
                                                fieldOnChange(reconstructControlledExpressionValue(value))
                                            }
                                            path={calculatedPath}
                                            placeholder={placeholder}
                                            required={required}
                                            setIsFormulaMode={(formulaMode) => {
                                                if (!formulaMode) {
                                                    controlledExpressionExitRef.current = true;

                                                    fieldOnChange('');
                                                }
                                            }}
                                            showInputTypeSwitchButton={showFormulaSwitch && !isFieldFromAi}
                                            toolProperty
                                            type={type}
                                            value={displayValue}
                                        />
                                    );
                                }

                                return (
                                    <PropertyTextArea
                                        {...fieldRest}
                                        deletePropertyButton={deletePropertyButton}
                                        description={description}
                                        error={!!fieldState.error || hasError}
                                        errorMessage={fieldState.error?.message || errorMessage}
                                        label={label || name}
                                        leadingIcon={typeIcon}
                                        onChange={(event) => {
                                            if (!showFromAi) {
                                                fieldOnChange(event);

                                                return;
                                            }

                                            fieldOnChange(resolveExpressionValue(event.target.value, field.value));
                                        }}
                                        placeholder={
                                            showFromAi
                                                ? getMentionsInputPlaceholder({
                                                      expressionEnabled,
                                                      placeholder,
                                                      toolProperty: true,
                                                  })
                                                : placeholder
                                        }
                                        required={required}
                                        trailingAction={
                                            showFromAi && expressionEnabled !== false ? (
                                                <FromAiToggleButton
                                                    isFromAi={!!isFieldFromAi}
                                                    onToggle={(fromAi) => handleFromAiToggle(fromAi, fieldOnChange)}
                                                />
                                            ) : undefined
                                        }
                                        value={strippedDisplayValue}
                                    />
                                );
                            }}
                            rules={{required: requiredRule}}
                        />
                    )}

                    {control && !isFormulaMode && controlType === 'MULTI_SELECT' && calculatedPath && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue || []}
                            name={calculatedPath}
                            render={({field: {onChange, value}}) => (
                                <PropertyMultiSelect
                                    defaultValue={(value as string[]) || []}
                                    deletePropertyButton={withFromAiButton(getControlledFromAiButton(onChange))}
                                    handleInputTypeSwitchButtonClick={() =>
                                        handleControlledFormulaSwitch(value, onChange)
                                    }
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
                                    showInputTypeSwitchButton={showFormulaSwitch}
                                    value={(value as string[]) || []}
                                    workflowId={workflow.id!}
                                />
                            )}
                            rules={{required: requiredRule}}
                        />
                    )}

                    {!control && (isValidControlType || isNumericalInput) && calculatedPath && (
                        <PropertyInput
                            deletePropertyButton={deletePropertyButton}
                            description={description}
                            error={hasError}
                            errorMessage={errorMessage}
                            fieldsetClassName={objectName && arrayName && 'ml-2'}
                            handleInputTypeSwitchButtonClick={handleFormulaSwitch}
                            label={label || name}
                            leadingIcon={typeIcon}
                            max={maxValue}
                            maxLength={maxLength}
                            min={minValue}
                            minLength={minLength}
                            name={calculatedPath}
                            onChange={handleInputChange}
                            onKeyDown={handleNativeKeyDown}
                            placeholder={getPropertyInputPlaceholder({
                                defaultValue,
                                isNumericalInput,
                                maxValue,
                                minValue,
                                placeholder,
                                required,
                            })}
                            ref={inputRef}
                            required={required}
                            showInputTypeSwitchButton={showFormulaSwitch}
                            title={type}
                            trailingAction={
                                // Chrome's <input type="time"> has no native clear button.
                                controlType === 'TIME' && inputValue && !hidden ? (
                                    <>
                                        <button
                                            aria-label="Clear time"
                                            className="flex items-center px-2 text-muted-foreground hover:text-foreground"
                                            onClick={handleInputClear}
                                            type="button"
                                        >
                                            <XIcon className="size-4" />
                                        </button>

                                        {nativeFromAiButton}
                                    </>
                                ) : (
                                    nativeFromAiButton
                                )
                            }
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
                            dynamicSwitch={dynamicSwitch}
                            error={hasError}
                            errorMessage={errorMessage}
                            handleInputTypeSwitchButtonClick={handleFormulaSwitch}
                            label={label || name}
                            leadingIcon={typeIcon}
                            name={name!}
                            onChange={(value) => {
                                handleContainerLocalEntriesChange(
                                    !isEmptyPillContainerValue({controlType: 'JSON_SCHEMA_BUILDER', value})
                                );

                                handleJsonSchemaBuilderChange(value);
                            }}
                            schema={inputValue ? JSON.parse(inputValue) : undefined}
                            title={label || name}
                        />
                    )}

                    {!control && controlType === 'SELECT' && type !== 'BOOLEAN' && (
                        <PropertyComboBox
                            arrayIndex={arrayIndex}
                            defaultValue={defaultValue}
                            deletePropertyButton={withFromAiButton(nativeFromAiButton)}
                            description={description}
                            dynamicSwitch={dynamicSwitch}
                            error={hasError}
                            errorMessage={errorMessage}
                            handleInputTypeSwitchButtonClick={handleFormulaSwitch}
                            label={label || name}
                            leadingIcon={typeIcon}
                            lookupDependsOnPaths={optionsDataSource?.optionsLookupDependsOn?.map(
                                (optionLookupDependency) => optionLookupDependency.replace('[index]', `[${arrayIndex}]`)
                            )}
                            lookupDependsOnValues={lookupDependsOnValues}
                            name={name}
                            onTriggerKeyDown={handleSelectKeyDown}
                            onValueChange={(value: string) => handleSelectChange(value, name!)}
                            options={(formattedOptions as Array<Option>) || []}
                            optionsDataSource={optionsDataSource}
                            optionsLoadedDynamically={optionsLoadedDynamically}
                            path={calculatedPath}
                            propertyName={name}
                            required={required}
                            showInputTypeSwitchButton={showFormulaSwitch}
                            value={selectValue}
                            workflowId={workflow.id!}
                            workflowNodeName={currentNode?.name ?? ''}
                        />
                    )}

                    {!control && controlType === 'SELECT' && type === 'BOOLEAN' && (
                        <PropertySelect
                            defaultValue={defaultValue?.toString()}
                            deletePropertyButton={withFromAiButton(nativeFromAiButton)}
                            description={description}
                            dynamicSwitch={dynamicSwitch}
                            handleInputTypeSwitchButtonClick={handleFormulaSwitch}
                            label={label || name}
                            leadingIcon={typeIcon}
                            name={name}
                            onTriggerKeyDown={handleSelectKeyDown}
                            onValueChange={(value: string) => handleSelectChange(value, name!)}
                            options={[
                                {label: 'True', value: 'true'},
                                {label: 'False', value: 'false'},
                            ]}
                            required={required}
                            showInputTypeSwitchButton={showFormulaSwitch}
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
                            placeholder={placeholder}
                            required={required}
                            value={inputValue}
                        />
                    )}

                    {!control && controlType === 'MULTI_SELECT' && (
                        <PropertyMultiSelect
                            defaultValue={propertyParameterValue as string[]}
                            deletePropertyButton={withFromAiButton(nativeFromAiButton)}
                            handleInputTypeSwitchButtonClick={handleFormulaSwitch}
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
                            showInputTypeSwitchButton={showFormulaSwitch}
                            value={multiSelectValue}
                            workflowId={workflow.id!}
                        />
                    )}

                    {controlType === 'NULL' && <span>NULL</span>}
                </div>
            )}

            {type === 'DYNAMIC_PROPERTIES' && (currentNode || clusterElementContext) && (
                <PropertyDynamicProperties
                    control={control}
                    controlPath={controlPath}
                    currentOperationName={operationName}
                    enabled={isDynamicPropertiesQueryEnabled({
                        clusterElementContext: !!clusterElementContext,
                        connectionId: currentNode?.connectionId,
                        connections: currentNode?.connections,
                    })}
                    formState={formState}
                    hideFromAi={hideFromAi}
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
