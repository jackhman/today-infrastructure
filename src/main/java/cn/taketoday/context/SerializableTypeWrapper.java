/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.taketoday.context;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.ReflectionUtils;

/**
 * Internal utility class that can be used to obtain wrapped {@link Serializable}
 * variants of {@link Type java.lang.reflect.Types}.
 *
 * <p>{@link #forField(Field) Fields} or {@link #forMethodParameter(MethodParameter)
 * MethodParameters} can be used as the root source for a serializable type.
 * Alternatively, a regular {@link Class} can also be used as source.
 *
 * <p>The returned type will either be a {@link Class} or a serializable proxy of
 * {@link GenericArrayType}, {@link ParameterizedType}, {@link TypeVariable} or
 * {@link WildcardType}. With the exception of {@link Class} (which is final) calls
 * to methods that return further {@link Type Types} (for example
 * {@link GenericArrayType#getGenericComponentType()}) will be automatically wrapped.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author TODAY
 * @since 3.0
 */
final class SerializableTypeWrapper {

  private static final Class<?>[] SUPPORTED_SERIALIZABLE_TYPES = {
          GenericArrayType.class, ParameterizedType.class, TypeVariable.class, WildcardType.class };

  static final Map<Type, Type> cache = new ConcurrentHashMap<>(256);

  private SerializableTypeWrapper() { }

  /**
   * Return a {@link Serializable} variant of {@link Field#getGenericType()}.
   */
  public static Type forField(Field field) {
    return forTypeProvider(new FieldTypeProvider(field));
  }

  /**
   * Unwrap the given type, effectively returning the original non-serializable type.
   *
   * @param type
   *         the type to unwrap
   *
   * @return the original non-serializable type
   */
  @SuppressWarnings("unchecked")
  public static <T extends Type> T unwrap(T type) {
    Type unwrapped = null;
    if (type instanceof SerializableTypeProxy) {
      unwrapped = ((SerializableTypeProxy) type).getTypeProvider().getType();
    }
    return (unwrapped != null ? (T) unwrapped : type);
  }

  /**
   * Return a {@link Serializable} {@link Type} backed by a {@link TypeProvider} .
   * <p>If type artifacts are generally not serializable in the current runtime
   * environment, this delegate will simply return the original {@code Type} as-is.
   */
  static Type forTypeProvider(TypeProvider provider) {
    Type providedType = provider.getType();
    if (providedType == null || providedType instanceof Serializable) {
      // No serializable type wrapping necessary (e.g. for java.lang.Class)
      return providedType;
    }

    // Obtain a serializable type proxy for the given provider...
    Type cached = cache.get(providedType);
    if (cached != null) {
      return cached;
    }
    for (Class<?> type : SUPPORTED_SERIALIZABLE_TYPES) {
      if (type.isInstance(providedType)) {
        ClassLoader classLoader = provider.getClass().getClassLoader();
        Class<?>[] interfaces = new Class<?>[] { type, SerializableTypeProxy.class, Serializable.class };
        InvocationHandler handler = new TypeProxyInvocationHandler(provider);
        cached = (Type) Proxy.newProxyInstance(classLoader, interfaces, handler);
        cache.put(providedType, cached);
        return cached;
      }
    }
    throw new IllegalArgumentException("Unsupported Type class: " + providedType.getClass().getName());
  }

  /**
   * Additional interface implemented by the type proxy.
   */
  interface SerializableTypeProxy {

    /**
     * Return the underlying type provider.
     */
    TypeProvider getTypeProvider();
  }

  /**
   * A {@link Serializable} interface providing access to a {@link Type}.
   */
  @SuppressWarnings("serial")
  interface TypeProvider extends Serializable {

    /**
     * Return the (possibly non {@link Serializable}) {@link Type}.
     */

    Type getType();

    /**
     * Return the source of the type, or {@code null} if not known.
     * <p>The default implementations returns {@code null}.
     */

    default Object getSource() {
      return null;
    }
  }

  /**
   * {@link Serializable} {@link InvocationHandler} used by the proxied {@link Type}.
   * Provides serialization support and enhances any methods that return {@code Type}
   * or {@code Type[]}.
   */
  @SuppressWarnings("serial")
  static class TypeProxyInvocationHandler implements InvocationHandler, Serializable {
    final TypeProvider provider;

    public TypeProxyInvocationHandler(TypeProvider provider) {
      this.provider = provider;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      switch (method.getName()) {
        case "equals":
          Object other = args[0];
          // Unwrap proxies for speed
          if (other instanceof Type) {
            other = unwrap((Type) other);
          }
          return Objects.equals(this.provider.getType(), other);
        case "hashCode":
          return Objects.hashCode(this.provider.getType());
        case "getTypeProvider":
          return this.provider;
        default:
          break;
      }

      if (Type.class == method.getReturnType() && ObjectUtils.isEmpty(args)) {
        return forTypeProvider(new MethodInvokeTypeProvider(this.provider, method, -1));
      }
      else if (Type[].class == method.getReturnType() && ObjectUtils.isEmpty(args)) {
        Type[] result = new Type[((Type[]) method.invoke(this.provider.getType())).length];
        for (int i = 0; i < result.length; i++) {
          result[i] = forTypeProvider(new MethodInvokeTypeProvider(this.provider, method, i));
        }
        return result;
      }

      try {
        return method.invoke(this.provider.getType(), args);
      }
      catch (InvocationTargetException ex) {
        throw ex.getTargetException();
      }
    }
  }

  /**
   * {@link TypeProvider} for {@link Type Types} obtained from a {@link Field}.
   */
  @SuppressWarnings("serial")
  static class FieldTypeProvider implements TypeProvider {
    private final String fieldName;
    private final Class<?> declaringClass;
    private transient Field field;

    public FieldTypeProvider(Field field) {
      this.fieldName = field.getName();
      this.declaringClass = field.getDeclaringClass();
      this.field = field;
    }

    @Override
    public Type getType() {
      return this.field.getGenericType();
    }

    @Override
    public Object getSource() {
      return this.field;
    }

    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
      inputStream.defaultReadObject();
      try {
        this.field = this.declaringClass.getDeclaredField(this.fieldName);
      }
      catch (Throwable ex) {
        throw new IllegalStateException("Could not find original class structure", ex);
      }
    }
  }

  /**
   * {@link TypeProvider} for {@link Type Types} obtained from a {@link Parameter}.
   */
  @SuppressWarnings("serial")
  static class ParameterTypeProvider implements TypeProvider {

    private final String methodName;
    private final Class<?>[] parameterTypes;
    private final Class<?> declaringClass;
    private final int parameterIndex;
    private transient Parameter methodParameter;

    public ParameterTypeProvider(Parameter parameter) {
      this(parameter, ClassUtils.getParameterIndex(parameter));
    }

    public ParameterTypeProvider(Parameter parameter, int parameterIndex) {
      final Executable executable = parameter.getDeclaringExecutable();
      this.methodParameter = parameter;
      this.parameterIndex = parameterIndex;
      this.methodName = executable.getName();
      this.parameterTypes = executable.getParameterTypes();
      this.declaringClass = executable.getDeclaringClass();
    }

    @Override
    public Type getType() {
      return this.methodParameter.getParameterizedType();
    }

    @Override
    public Object getSource() {
      return this.methodParameter;
    }

    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
      inputStream.defaultReadObject();
      try {
        if (this.methodName != null) {
          final Method declaredMethod = this.declaringClass.getDeclaredMethod(this.methodName, this.parameterTypes);
          this.methodParameter = declaredMethod.getParameters()[parameterIndex];
        }
        else {
          final Constructor<?> constructor = this.declaringClass.getDeclaredConstructor(this.parameterTypes);
          this.methodParameter = constructor.getParameters()[parameterIndex];
        }
      }
      catch (Throwable ex) {
        throw new IllegalStateException("Could not find original class structure", ex);
      }
    }
  }

  /**
   * {@link TypeProvider} for {@link Type Types} obtained by invoking a no-arg method.
   */
  @SuppressWarnings("serial")
  static class MethodInvokeTypeProvider implements TypeProvider {

    private final TypeProvider provider;
    private final String methodName;
    private final Class<?> declaringClass;
    private final int index;
    private transient Method method;
    private transient volatile Object result;

    public MethodInvokeTypeProvider(TypeProvider provider, Method method, int index) {
      this.provider = provider;
      this.methodName = method.getName();
      this.declaringClass = method.getDeclaringClass();
      this.index = index;
      this.method = method;
    }

    @Override
    public Type getType() {
      Object result = this.result;
      if (result == null) {
        // Lazy invocation of the target method on the provided type
        result = ReflectionUtils.invokeMethod(this.method, this.provider.getType());
        // Cache the result for further calls to getType()
        this.result = result;
      }
      return (result instanceof Type[] ? ((Type[]) result)[this.index] : (Type) result);
    }

    @Override
    public Object getSource() {
      return null;
    }

    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
      inputStream.defaultReadObject();
      Method method = ReflectionUtils.findMethod(this.declaringClass, this.methodName);
      if (method == null) {
        throw new IllegalStateException("Cannot find method on deserialization: " + this.methodName);
      }
      if (method.getReturnType() != Type.class && method.getReturnType() != Type[].class) {
        throw new IllegalStateException(
                "Invalid return type on deserialized method - needs to be Type or Type[]: " + method);
      }
      this.method = method;
    }
  }

}
