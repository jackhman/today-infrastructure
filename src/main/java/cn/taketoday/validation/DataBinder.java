/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.validation;

import java.beans.PropertyEditor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.ConfigurablePropertyAccessor;
import cn.taketoday.beans.PropertyAccessException;
import cn.taketoday.beans.PropertyAccessorUtils;
import cn.taketoday.beans.PropertyBatchUpdateException;
import cn.taketoday.beans.PropertyEditorRegistry;
import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.SimpleTypeConverter;
import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.format.Formatter;
import cn.taketoday.format.support.FormatterPropertyEditorAdapter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Binder that allows for setting property values onto a target object,
 * including support for validation and binding result analysis.
 * The binding process can be customized through specifying allowed fields,
 * required fields, custom editors, etc.
 *
 * <p>Note that there are potential security implications in failing to set an array
 * of allowed fields. In the case of HTTP form POST data for example, malicious clients
 * can attempt to subvert an application by supplying values for fields or properties
 * that do not exist on the form. In some cases this could lead to illegal data being
 * set on command objects <i>or their nested objects</i>. For this reason, it is
 * <b>highly recommended to specify the {@link #setAllowedFields allowedFields} property</b>
 * on the DataBinder.
 *
 * <p>The binding results can be examined via the {@link BindingResult} interface,
 * extending the {@link Errors} interface: see the {@link #getBindingResult()} method.
 * Missing fields and property access exceptions will be converted to {@link FieldError FieldErrors},
 * collected in the Errors instance, using the following error codes:
 *
 * <ul>
 * <li>Missing field error: "required"
 * <li>Type mismatch error: "typeMismatch"
 * <li>Method invocation error: "methodInvocation"
 * </ul>
 *
 * <p>By default, binding errors get resolved through the {@link BindingErrorProcessor}
 * strategy, processing for missing fields and property access exceptions: see the
 * {@link #setBindingErrorProcessor} method. You can override the default strategy
 * if needed, for example to generate different error codes.
 *
 * <p>Custom validation errors can be added afterwards. You will typically want to resolve
 * such error codes into proper user-visible error messages; this can be achieved through
 * resolving each error via a {@link cn.taketoday.context.MessageSource}, which is
 * able to resolve an {@link ObjectError}/{@link FieldError} through its
 * {@link cn.taketoday.context.MessageSource#getMessage(cn.taketoday.context.MessageSourceResolvable, java.util.Locale)}
 * method. The list of message codes can be customized through the {@link MessageCodesResolver}
 * strategy: see the {@link #setMessageCodesResolver} method. {@link DefaultMessageCodesResolver}'s
 * javadoc states details on the default resolution rules.
 *
 * <p>This generic data binder can be used in any kind of environment.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #registerCustomEditor
 * @see #setMessageCodesResolver
 * @see #setBindingErrorProcessor
 * @see #bind
 * @see #getBindingResult
 * @see DefaultMessageCodesResolver
 * @see DefaultBindingErrorProcessor
 * @see cn.taketoday.context.MessageSource
 */
public class DataBinder implements PropertyEditorRegistry, TypeConverter {

  /** Default object name used for binding: "target". */
  public static final String DEFAULT_OBJECT_NAME = "target";

  /** Default limit for array and collection growing: 256. */
  public static final int DEFAULT_AUTO_GROW_COLLECTION_LIMIT = 256;

  /**
   * We'll create a lot of DataBinder instances: Let's use a static logger.
   */
  protected static final Logger logger = LoggerFactory.getLogger(DataBinder.class);

  @Nullable
  private final Object target;

  private final String objectName;

  @Nullable
  private AbstractPropertyBindingResult bindingResult;

  private boolean directFieldAccess = false;

  @Nullable
  private SimpleTypeConverter typeConverter;

  private boolean ignoreUnknownFields = true;

  private boolean ignoreInvalidFields = false;

  private boolean autoGrowNestedPaths = true;

  private int autoGrowCollectionLimit = DEFAULT_AUTO_GROW_COLLECTION_LIMIT;

  @Nullable
  private String[] allowedFields;

  @Nullable
  private String[] disallowedFields;

  @Nullable
  private String[] requiredFields;

  @Nullable
  private ConversionService conversionService;

  @Nullable
  private MessageCodesResolver messageCodesResolver;

  private BindingErrorProcessor bindingErrorProcessor = new DefaultBindingErrorProcessor();

  private final ArrayList<Validator> validators = new ArrayList<>();

  /**
   * Create a new DataBinder instance, with default object name.
   *
   * @param target the target object to bind onto (or {@code null}
   * if the binder is just used to convert a plain parameter value)
   * @see #DEFAULT_OBJECT_NAME
   */
  public DataBinder(@Nullable Object target) {
    this(target, DEFAULT_OBJECT_NAME);
  }

  /**
   * Create a new DataBinder instance.
   *
   * @param target the target object to bind onto (or {@code null}
   * if the binder is just used to convert a plain parameter value)
   * @param objectName the name of the target object
   */
  public DataBinder(@Nullable Object target, String objectName) {
    this.target = ObjectUtils.unwrapOptional(target);
    this.objectName = objectName;
  }

  /**
   * Return the wrapped target object.
   */
  @Nullable
  public Object getTarget() {
    return this.target;
  }

  /**
   * Return the name of the bound object.
   */
  public String getObjectName() {
    return this.objectName;
  }

  /**
   * Set whether this binder should attempt to "auto-grow" a nested path that contains a null value.
   * <p>If "true", a null path location will be populated with a default object value and traversed
   * instead of resulting in an exception. This flag also enables auto-growth of collection elements
   * when accessing an out-of-bounds index.
   * <p>Default is "true" on a standard DataBinder. Note that this feature is supported
   * for bean property access (DataBinder's default mode) and field access.
   *
   * @see #initBeanPropertyAccess()
   * @see cn.taketoday.beans.BeanWrapper#setAutoGrowNestedPaths
   */
  public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
    Assert.state(this.bindingResult == null,
            "DataBinder is already initialized - call setAutoGrowNestedPaths before other configuration methods");
    this.autoGrowNestedPaths = autoGrowNestedPaths;
  }

  /**
   * Return whether "auto-growing" of nested paths has been activated.
   */
  public boolean isAutoGrowNestedPaths() {
    return this.autoGrowNestedPaths;
  }

  /**
   * Specify the limit for array and collection auto-growing.
   * <p>Default is 256, preventing OutOfMemoryErrors in case of large indexes.
   * Raise this limit if your auto-growing needs are unusually high.
   *
   * @see #initBeanPropertyAccess()
   * @see cn.taketoday.beans.BeanWrapper#setAutoGrowCollectionLimit
   */
  public void setAutoGrowCollectionLimit(int autoGrowCollectionLimit) {
    Assert.state(this.bindingResult == null,
            "DataBinder is already initialized - call setAutoGrowCollectionLimit before other configuration methods");
    this.autoGrowCollectionLimit = autoGrowCollectionLimit;
  }

  /**
   * Return the current limit for array and collection auto-growing.
   */
  public int getAutoGrowCollectionLimit() {
    return this.autoGrowCollectionLimit;
  }

  /**
   * Initialize standard JavaBean property access for this DataBinder.
   * <p>This is the default; an explicit call just leads to eager initialization.
   *
   * @see #initDirectFieldAccess()
   * @see #createBeanPropertyBindingResult()
   */
  public void initBeanPropertyAccess() {
    Assert.state(this.bindingResult == null,
            "DataBinder is already initialized - call initBeanPropertyAccess before other configuration methods");
    this.directFieldAccess = false;
  }

  /**
   * Create the {@link AbstractPropertyBindingResult} instance using standard
   * JavaBean property access.
   */
  protected AbstractPropertyBindingResult createBeanPropertyBindingResult() {
    BeanPropertyBindingResult result = new BeanPropertyBindingResult(getTarget(),
            getObjectName(), isAutoGrowNestedPaths(), getAutoGrowCollectionLimit());

    if (this.conversionService != null) {
      result.initConversion(this.conversionService);
    }
    if (this.messageCodesResolver != null) {
      result.setMessageCodesResolver(this.messageCodesResolver);
    }

    return result;
  }

  /**
   * Initialize direct field access for this DataBinder,
   * as alternative to the default bean property access.
   *
   * @see #initBeanPropertyAccess()
   * @see #createDirectFieldBindingResult()
   */
  public void initDirectFieldAccess() {
    Assert.state(this.bindingResult == null,
            "DataBinder is already initialized - call initDirectFieldAccess before other configuration methods");
    this.directFieldAccess = true;
  }

  /**
   * Create the {@link AbstractPropertyBindingResult} instance using direct
   * field access.
   */
  protected AbstractPropertyBindingResult createDirectFieldBindingResult() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(getTarget(),
            getObjectName(), isAutoGrowNestedPaths());

    if (this.conversionService != null) {
      result.initConversion(this.conversionService);
    }
    if (this.messageCodesResolver != null) {
      result.setMessageCodesResolver(this.messageCodesResolver);
    }

    return result;
  }

  /**
   * Return the internal BindingResult held by this DataBinder,
   * as an AbstractPropertyBindingResult.
   */
  protected AbstractPropertyBindingResult getInternalBindingResult() {
    if (this.bindingResult == null) {
      this.bindingResult = (this.directFieldAccess ?
                            createDirectFieldBindingResult() : createBeanPropertyBindingResult());
    }
    return this.bindingResult;
  }

  /**
   * Return the underlying PropertyAccessor of this binder's BindingResult.
   */
  protected ConfigurablePropertyAccessor getPropertyAccessor() {
    return getInternalBindingResult().getPropertyAccessor();
  }

  /**
   * Return this binder's underlying SimpleTypeConverter.
   */
  protected SimpleTypeConverter getSimpleTypeConverter() {
    if (this.typeConverter == null) {
      this.typeConverter = new SimpleTypeConverter();
      if (this.conversionService != null) {
        this.typeConverter.setConversionService(this.conversionService);
      }
    }
    return this.typeConverter;
  }

  /**
   * Return the underlying TypeConverter of this binder's BindingResult.
   */
  protected PropertyEditorRegistry getPropertyEditorRegistry() {
    if (getTarget() != null) {
      return getInternalBindingResult().getPropertyAccessor();
    }
    else {
      return getSimpleTypeConverter();
    }
  }

  /**
   * Return the underlying TypeConverter of this binder's BindingResult.
   */
  protected TypeConverter getTypeConverter() {
    if (getTarget() != null) {
      return getInternalBindingResult().getPropertyAccessor();
    }
    else {
      return getSimpleTypeConverter();
    }
  }

  /**
   * Return the BindingResult instance created by this DataBinder.
   * This allows for convenient access to the binding results after
   * a bind operation.
   *
   * @return the BindingResult instance, to be treated as BindingResult
   * or as Errors instance (Errors is a super-interface of BindingResult)
   * @see Errors
   * @see #bind
   */
  public BindingResult getBindingResult() {
    return getInternalBindingResult();
  }

  /**
   * Set whether to ignore unknown fields, that is, whether to ignore bind
   * parameters that do not have corresponding fields in the target object.
   * <p>Default is "true". Turn this off to enforce that all bind parameters
   * must have a matching field in the target object.
   * <p>Note that this setting only applies to <i>binding</i> operations
   * on this DataBinder, not to <i>retrieving</i> values via its
   * {@link #getBindingResult() BindingResult}.
   *
   * @see #bind
   */
  public void setIgnoreUnknownFields(boolean ignoreUnknownFields) {
    this.ignoreUnknownFields = ignoreUnknownFields;
  }

  /**
   * Return whether to ignore unknown fields when binding.
   */
  public boolean isIgnoreUnknownFields() {
    return this.ignoreUnknownFields;
  }

  /**
   * Set whether to ignore invalid fields, that is, whether to ignore bind
   * parameters that have corresponding fields in the target object which are
   * not accessible (for example because of null values in the nested path).
   * <p>Default is "false". Turn this on to ignore bind parameters for
   * nested objects in non-existing parts of the target object graph.
   * <p>Note that this setting only applies to <i>binding</i> operations
   * on this DataBinder, not to <i>retrieving</i> values via its
   * {@link #getBindingResult() BindingResult}.
   *
   * @see #bind
   */
  public void setIgnoreInvalidFields(boolean ignoreInvalidFields) {
    this.ignoreInvalidFields = ignoreInvalidFields;
  }

  /**
   * Return whether to ignore invalid fields when binding.
   */
  public boolean isIgnoreInvalidFields() {
    return this.ignoreInvalidFields;
  }

  /**
   * Register fields that should be allowed for binding. Default is all fields.
   * Restrict this for example to avoid unwanted modifications by malicious
   * users when binding HTTP request parameters.
   * <p>Supports "xxx*", "*xxx", "*xxx*" and "xxx*yyy" matches (with an
   * arbitrary number of pattern parts), as well as direct equality. More
   * sophisticated matching can be implemented by overriding the
   * {@code isAllowed} method.
   * <p>Alternatively, specify a list of <i>disallowed</i> fields.
   *
   * @param allowedFields array of field names
   * @see #setDisallowedFields
   * @see #isAllowed(String)
   */
  public void setAllowedFields(@Nullable String... allowedFields) {
    this.allowedFields = PropertyAccessorUtils.canonicalPropertyNames(allowedFields);
  }

  /**
   * Return the fields that should be allowed for binding.
   *
   * @return array of field names
   */
  @Nullable
  public String[] getAllowedFields() {
    return this.allowedFields;
  }

  /**
   * Register fields that should <i>not</i> be allowed for binding. Default
   * is none. Mark fields as disallowed for example to avoid unwanted
   * modifications by malicious users when binding HTTP request parameters.
   * <p>Supports "xxx*", "*xxx", "*xxx*" and "xxx*yyy" matches (with an
   * arbitrary number of pattern parts), as well as direct equality.
   * More sophisticated matching can be implemented by overriding the
   * {@code isAllowed} method.
   * <p>Alternatively, specify a list of <i>allowed</i> fields.
   *
   * @param disallowedFields array of field names
   * @see #setAllowedFields
   * @see #isAllowed(String)
   */
  public void setDisallowedFields(@Nullable String... disallowedFields) {
    this.disallowedFields = PropertyAccessorUtils.canonicalPropertyNames(disallowedFields);
  }

  /**
   * Return the fields that should <i>not</i> be allowed for binding.
   *
   * @return array of field names
   */
  @Nullable
  public String[] getDisallowedFields() {
    return this.disallowedFields;
  }

  /**
   * Register fields that are required for each binding process.
   * <p>If one of the specified fields is not contained in the list of
   * incoming property values, a corresponding "missing field" error
   * will be created, with error code "required" (by the default
   * binding error processor).
   *
   * @param requiredFields array of field names
   * @see #setBindingErrorProcessor
   * @see DefaultBindingErrorProcessor#MISSING_FIELD_ERROR_CODE
   */
  public void setRequiredFields(@Nullable String... requiredFields) {
    this.requiredFields = PropertyAccessorUtils.canonicalPropertyNames(requiredFields);
    if (logger.isDebugEnabled()) {
      logger.debug("DataBinder requires binding of required fields [{}]", StringUtils.arrayToString(requiredFields));
    }
  }

  /**
   * Return the fields that are required for each binding process.
   *
   * @return array of field names
   */
  @Nullable
  public String[] getRequiredFields() {
    return this.requiredFields;
  }

  /**
   * Set the strategy to use for resolving errors into message codes.
   * Applies the given strategy to the underlying errors holder.
   * <p>Default is a DefaultMessageCodesResolver.
   *
   * @see BeanPropertyBindingResult#setMessageCodesResolver
   * @see DefaultMessageCodesResolver
   */
  public void setMessageCodesResolver(@Nullable MessageCodesResolver messageCodesResolver) {
    Assert.state(this.messageCodesResolver == null, "DataBinder is already initialized with MessageCodesResolver");
    this.messageCodesResolver = messageCodesResolver;
    if (this.bindingResult != null && messageCodesResolver != null) {
      this.bindingResult.setMessageCodesResolver(messageCodesResolver);
    }
  }

  /**
   * Set the strategy to use for processing binding errors, that is,
   * required field errors and {@code PropertyAccessException}s.
   * <p>Default is a DefaultBindingErrorProcessor.
   *
   * @see DefaultBindingErrorProcessor
   */
  public void setBindingErrorProcessor(BindingErrorProcessor bindingErrorProcessor) {
    Assert.notNull(bindingErrorProcessor, "BindingErrorProcessor must not be null");
    this.bindingErrorProcessor = bindingErrorProcessor;
  }

  /**
   * Return the strategy for processing binding errors.
   */
  public BindingErrorProcessor getBindingErrorProcessor() {
    return this.bindingErrorProcessor;
  }

  /**
   * Set the Validator to apply after each binding step.
   *
   * @see #addValidators(Validator...)
   * @see #replaceValidators(Validator...)
   */
  public void setValidator(@Nullable Validator validator) {
    assertValidators(validator);
    this.validators.clear();
    if (validator != null) {
      this.validators.add(validator);
    }
  }

  private void assertValidators(Validator... validators) {
    Object target = getTarget();
    for (Validator validator : validators) {
      if (validator != null && (target != null && !validator.supports(target.getClass()))) {
        throw new IllegalStateException("Invalid target for Validator [" + validator + "]: " + target);
      }
    }
  }

  /**
   * Add Validators to apply after each binding step.
   *
   * @see #setValidator(Validator)
   * @see #replaceValidators(Validator...)
   */
  public void addValidators(Validator... validators) {
    assertValidators(validators);
    this.validators.addAll(Arrays.asList(validators));
  }

  /**
   * Replace the Validators to apply after each binding step.
   *
   * @see #setValidator(Validator)
   * @see #addValidators(Validator...)
   */
  public void replaceValidators(Validator... validators) {
    assertValidators(validators);
    this.validators.clear();
    this.validators.addAll(Arrays.asList(validators));
  }

  /**
   * Return the primary Validator to apply after each binding step, if any.
   */
  @Nullable
  public Validator getValidator() {
    return (!this.validators.isEmpty() ? this.validators.get(0) : null);
  }

  /**
   * Return the Validators to apply after data binding.
   */
  public List<Validator> getValidators() {
    return Collections.unmodifiableList(this.validators);
  }

  //---------------------------------------------------------------------
  // Implementation of PropertyEditorRegistry/TypeConverter interface
  //---------------------------------------------------------------------

  /**
   * Specify a ConversionService to use for converting
   * property values, as an alternative to JavaBeans PropertyEditors.
   */
  public void setConversionService(@Nullable ConversionService conversionService) {
    Assert.state(this.conversionService == null, "DataBinder is already initialized with ConversionService");
    this.conversionService = conversionService;
    if (this.bindingResult != null && conversionService != null) {
      this.bindingResult.initConversion(conversionService);
    }
  }

  /**
   * Return the associated ConversionService, if any.
   */
  @Nullable
  public ConversionService getConversionService() {
    return this.conversionService;
  }

  /**
   * Add a custom formatter, applying it to all fields matching the
   * {@link Formatter}-declared type.
   * <p>Registers a corresponding {@link PropertyEditor} adapter underneath the covers.
   *
   * @param formatter the formatter to add, generically declared for a specific type
   * @see #registerCustomEditor(Class, PropertyEditor)
   */
  public void addCustomFormatter(Formatter<?> formatter) {
    FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
    getPropertyEditorRegistry().registerCustomEditor(adapter.getFieldType(), adapter);
  }

  /**
   * Add a custom formatter for the field type specified in {@link Formatter} class,
   * applying it to the specified fields only, if any, or otherwise to all fields.
   * <p>Registers a corresponding {@link PropertyEditor} adapter underneath the covers.
   *
   * @param formatter the formatter to add, generically declared for a specific type
   * @param fields the fields to apply the formatter to, or none if to be applied to all
   * @see #registerCustomEditor(Class, String, PropertyEditor)
   */
  public void addCustomFormatter(Formatter<?> formatter, String... fields) {
    FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
    Class<?> fieldType = adapter.getFieldType();
    if (ObjectUtils.isEmpty(fields)) {
      getPropertyEditorRegistry().registerCustomEditor(fieldType, adapter);
    }
    else {
      for (String field : fields) {
        getPropertyEditorRegistry().registerCustomEditor(fieldType, field, adapter);
      }
    }
  }

  /**
   * Add a custom formatter, applying it to the specified field types only, if any,
   * or otherwise to all fields matching the {@link Formatter}-declared type.
   * <p>Registers a corresponding {@link PropertyEditor} adapter underneath the covers.
   *
   * @param formatter the formatter to add (does not need to generically declare a
   * field type if field types are explicitly specified as parameters)
   * @param fieldTypes the field types to apply the formatter to, or none if to be
   * derived from the given {@link Formatter} implementation class
   * @see #registerCustomEditor(Class, PropertyEditor)
   */
  public void addCustomFormatter(Formatter<?> formatter, Class<?>... fieldTypes) {
    FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
    PropertyEditorRegistry editorRegistry = getPropertyEditorRegistry();
    if (ObjectUtils.isEmpty(fieldTypes)) {
      editorRegistry.registerCustomEditor(adapter.getFieldType(), adapter);
    }
    else {
      for (Class<?> fieldType : fieldTypes) {
        editorRegistry.registerCustomEditor(fieldType, adapter);
      }
    }
  }

  @Override
  public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
    getPropertyEditorRegistry().registerCustomEditor(requiredType, propertyEditor);
  }

  @Override
  public void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String field, PropertyEditor propertyEditor) {
    getPropertyEditorRegistry().registerCustomEditor(requiredType, field, propertyEditor);
  }

  @Override
  @Nullable
  public PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath) {
    return getPropertyEditorRegistry().findCustomEditor(requiredType, propertyPath);
  }

  @Override
  @Nullable
  public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException {
    return getTypeConverter().convertIfNecessary(value, requiredType);
  }

  @Override
  @Nullable
  public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
                                  @Nullable MethodParameter methodParam) throws TypeMismatchException {

    return getTypeConverter().convertIfNecessary(value, requiredType, methodParam);
  }

  @Override
  @Nullable
  public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
          throws TypeMismatchException {

    return getTypeConverter().convertIfNecessary(value, requiredType, field);
  }

  @Nullable
  @Override
  public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
                                  @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

    return getTypeConverter().convertIfNecessary(value, requiredType, typeDescriptor);
  }

  /**
   * Bind the given property values to this binder's target.
   * <p>This call can create field errors, representing basic binding
   * errors like a required field (code "required"), or type mismatch
   * between value and bean property (code "typeMismatch").
   * <p>Note that the given PropertyValues should be a throwaway instance:
   * For efficiency, it will be modified to just contain allowed fields if it
   * implements the PropertyValues interface; else, an internal mutable
   * copy will be created for this purpose. Pass in a copy of the PropertyValues
   * if you want your original instance to stay unmodified in any case.
   *
   * @param pvs property values to bind
   * @see #doBind(cn.taketoday.beans.PropertyValues)
   */
  public void bind(PropertyValues pvs) {
    doBind(pvs);
  }

  /**
   * Actual implementation of the binding process, working with the
   * passed-in PropertyValues instance.
   *
   * @param mpvs the property values to bind,
   * as PropertyValues instance
   * @see #checkAllowedFields
   * @see #checkRequiredFields
   * @see #applyPropertyValues
   */
  protected void doBind(PropertyValues mpvs) {
    checkAllowedFields(mpvs);
    checkRequiredFields(mpvs);
    applyPropertyValues(mpvs);
  }

  /**
   * Check the given property values against the allowed fields,
   * removing values for fields that are not allowed.
   *
   * @param mpvs the property values to be bound (can be modified)
   * @see #getAllowedFields
   * @see #isAllowed(String)
   */
  protected void checkAllowedFields(PropertyValues mpvs) {
    for (PropertyValue pv : mpvs) {
      String field = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
      if (!isAllowed(field)) {
        mpvs.remove(pv);
        getBindingResult().recordSuppressedField(field);
        if (logger.isDebugEnabled()) {
          logger.debug("Field [{}] has been removed from PropertyValues " +
                  "and will not be bound, because it has not been found in the list of allowed fields", field);
        }
      }
    }
  }

  /**
   * Return if the given field is allowed for binding.
   * Invoked for each passed-in property value.
   * <p>The default implementation checks for "xxx*", "*xxx", "*xxx*" and "xxx*yyy"
   * matches (with an arbitrary number of pattern parts), as well as direct equality,
   * in the specified lists of allowed fields and disallowed fields. A field matching
   * a disallowed pattern will not be accepted even if it also happens to match a
   * pattern in the allowed list.
   * <p>Can be overridden in subclasses.
   *
   * @param field the field to check
   * @return if the field is allowed
   * @see #setAllowedFields
   * @see #setDisallowedFields
   * @see cn.taketoday.util.StringUtils#simpleMatch(String, String)
   */
  protected boolean isAllowed(String field) {
    String[] allowed = getAllowedFields();
    String[] disallowed = getDisallowedFields();
    return (ObjectUtils.isEmpty(allowed) || StringUtils.simpleMatch(allowed, field))
            && (ObjectUtils.isEmpty(disallowed) || !StringUtils.simpleMatch(disallowed, field));
  }

  /**
   * Check the given property values against the required fields,
   * generating missing field errors where appropriate.
   *
   * @param mpvs the property values to be bound (can be modified)
   * @see #getRequiredFields
   * @see #getBindingErrorProcessor
   * @see BindingErrorProcessor#processMissingFieldError
   */
  protected void checkRequiredFields(PropertyValues mpvs) {
    String[] requiredFields = getRequiredFields();
    if (ObjectUtils.isNotEmpty(requiredFields)) {
      HashMap<String, PropertyValue> propertyValues = new HashMap<>();
      for (PropertyValue pv : mpvs) {
        String canonicalName = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
        propertyValues.put(canonicalName, pv);
      }
      for (String field : requiredFields) {
        PropertyValue pv = propertyValues.get(field);
        boolean empty = pv == null || pv.getValue() == null;
        if (!empty) {
          if (pv.getValue() instanceof String) {
            empty = !StringUtils.hasText((String) pv.getValue());
          }
          else if (pv.getValue() instanceof String[] values) {
            empty = values.length == 0 || !StringUtils.hasText(values[0]);
          }
        }
        if (empty) {
          // Use bind error processor to create FieldError.
          getBindingErrorProcessor().processMissingFieldError(field, getInternalBindingResult());
          // Remove property from property values to bind:
          // It has already caused a field error with a rejected value.
          if (pv != null) {
            mpvs.remove(pv);
            propertyValues.remove(field);
          }
        }
      }
    }
  }

  /**
   * Apply given property values to the target object.
   * <p>Default implementation applies all of the supplied property
   * values as bean property values. By default, unknown fields will
   * be ignored.
   *
   * @param mpvs the property values to be bound (can be modified)
   * @see #getTarget
   * @see #getPropertyAccessor
   * @see #isIgnoreUnknownFields
   * @see #getBindingErrorProcessor
   * @see BindingErrorProcessor#processPropertyAccessException
   */
  protected void applyPropertyValues(PropertyValues mpvs) {
    try {
      // Bind request parameters onto target object.
      getPropertyAccessor().setPropertyValues(mpvs, isIgnoreUnknownFields(), isIgnoreInvalidFields());
    }
    catch (PropertyBatchUpdateException ex) {
      // Use bind error processor to create FieldErrors.
      for (PropertyAccessException pae : ex.getPropertyAccessExceptions()) {
        getBindingErrorProcessor().processPropertyAccessException(pae, getInternalBindingResult());
      }
    }
  }

  /**
   * Invoke the specified Validators, if any.
   *
   * @see #setValidator(Validator)
   * @see #getBindingResult()
   */
  public void validate() {
    Object target = getTarget();
    Assert.state(target != null, "No target to validate");
    BindingResult bindingResult = getBindingResult();
    // Call each validator with the same binding result
    for (Validator validator : getValidators()) {
      validator.validate(target, bindingResult);
    }
  }

  /**
   * Invoke the specified Validators, if any, with the given validation hints.
   * <p>Note: Validation hints may get ignored by the actual target Validator.
   *
   * @param validationHints one or more hint objects to be passed to a {@link SmartValidator}
   * @see #setValidator(Validator)
   * @see SmartValidator#validate(Object, Errors, Object...)
   */
  public void validate(Object... validationHints) {
    Object target = getTarget();
    Assert.state(target != null, "No target to validate");
    BindingResult bindingResult = getBindingResult();
    // Call each validator with the same binding result
    for (Validator validator : getValidators()) {
      if (ObjectUtils.isNotEmpty(validationHints) && validator instanceof SmartValidator) {
        ((SmartValidator) validator).validate(target, bindingResult, validationHints);
      }
      else if (validator != null) {
        validator.validate(target, bindingResult);
      }
    }
  }

  /**
   * Close this DataBinder, which may result in throwing
   * a BindException if it encountered any errors.
   *
   * @return the model Map, containing target object and Errors instance
   * @throws BindException if there were any errors in the bind operation
   * @see BindingResult#getModel()
   */
  public Map<?, ?> close() throws BindException {
    if (getBindingResult().hasErrors()) {
      throw new BindException(getBindingResult());
    }
    return getBindingResult().getModel();
  }

}
