/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.core.bytecode.transform.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.core.CodeGenerationException;
import cn.taketoday.core.bytecode.transform.ClassEmitterTransformer;

/**
 * @author Juozas Baliuka
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AddDelegateTransformer extends ClassEmitterTransformer {

  private static final String DELEGATE = "$todayDelegate";
  private static final MethodSignature CSTRUCT_OBJECT = MethodSignature.from("void <init>(Object)");

  private final Class[] delegateIf;
  private final Class delegateImpl;
  private final Type delegateType;

  /** Creates a new instance of AddDelegateTransformer */
  public AddDelegateTransformer(Class[] delegateIf, Class delegateImpl) {
    try {
      delegateImpl.getConstructor(Object.class);
      this.delegateIf = delegateIf;
      this.delegateImpl = delegateImpl;
      delegateType = Type.fromClass(delegateImpl);
    }
    catch (NoSuchMethodException e) {
      throw new CodeGenerationException(e);
    }
  }

  @Override
  public void beginClass(int version,
                         int access,
                         String className,
                         Type superType,
                         Type[] interfaces,
                         String sourceFile) //
  {

    if (Modifier.isInterface(access)) {
      super.beginClass(version, access, className, superType, interfaces, sourceFile);
    }
    else {
      Type[] all = Type.add(interfaces, Type.getTypes(delegateIf));
      super.beginClass(version, access, className, superType, all, sourceFile);

      declare_field(Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT, DELEGATE, delegateType, null);

      for (Class aClass : delegateIf) {
        Method[] methods = aClass.getMethods();
        for (Method method : methods) {
          if (Modifier.isAbstract(method.getModifiers())) {
            addDelegate(method);
          }
        }
      }
    }
  }

  @Override
  public CodeEmitter beginMethod(int access, MethodSignature sig, Type... exceptions) {

    final CodeEmitter e = super.beginMethod(access, sig, exceptions);
    if (sig.getName().equals(MethodSignature.CONSTRUCTOR_NAME)) {

      return new CodeEmitter(e) {
        private boolean transformInit = true;

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
          super.visitMethodInsn(opcode, owner, name, desc, itf);
          if (transformInit && opcode == Opcodes.INVOKESPECIAL) {
            loadThis();
            newInstance(delegateType);
            dup();
            loadThis();
            invokeConstructor(delegateType, CSTRUCT_OBJECT);
            putField(DELEGATE);
            transformInit = false;
          }
        }
      };
    }
    return e;
  }

  private void addDelegate(Method m) {
    Method delegate;
    try {
      delegate = delegateImpl.getMethod(m.getName(), m.getParameterTypes());
      if (!delegate.getReturnType().getName().equals(m.getReturnType().getName())) {
        throw new IllegalArgumentException("Invalid delegate signature " + delegate);
      }
    }
    catch (NoSuchMethodException e) {
      throw new CodeGenerationException(e);
    }

    final MethodSignature sig = MethodSignature.from(m);
    Type[] exceptions = Type.getTypes(m.getExceptionTypes());
    CodeEmitter e = super.beginMethod(Opcodes.ACC_PUBLIC, sig, exceptions);
    e.loadThis();
    e.getField(DELEGATE);
    e.loadArgs();
    e.invokeVirtual(delegateType, sig);
    e.returnValue();
    e.end_method();
  }
}
