/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans;

import java.beans.PropertyDescriptor;

/**
 * BeanWrapper 是一个从 BeanDefinition 到 Bean 直接的中间产物，我们可以称它为”低级 bean“。
 * 在一般情况下，我们不会在实际项目中用到它。而是通过 BeanFactory 或者 DataBinder 隐式使用。
 * 它提供分析和操作标准 JavaBeans 的操作：获取和设置属性值、获取属性描述符以及查询属性的可读性/可写性的能力。
 *
 * BeanWrapper 是 Spring 框架中重要的组件类，它就相当于一个代理类，
 * Spring 委托 BeanWrapper 完成 Bean 属性的填充工作。在 Bean 实例被 InstantiationStrategy 创建出来后，
 * Spring 容器会将 Bean 实例通过 BeanWrapper 包裹起来
 *
 * 该接口主要继承核心三个接口
 *
 * PropertyAccessor：
 * 可以访问属性的通用型接口（例如对象的 bean 属性或者对象中的字段），作为 BeanWrapper 的基础接口。
 *
 * PropertyEditorRegistr：
 * 用于注册 JavaBean 的 PropertyEditors，
 * 对 PropertyEditorRegistrar 起核心作用的中心接口。由 BeanWrapper 扩展，BeanWrapperImpl 和 DataBinder 实现。
 *
 * TypeConverter：
 * 定义类型转换的接口，通常与 PropertyEditorRegistry 接口一起实现（但不是必须），
 * 但由于 TypeConverter 是基于线程不安全的 PropertyEditors ，因此 TypeConverters 本身也不被视为线程安全。
 * 在 Spring 3 后，不在采用 PropertyEditors 类作为 Spring 默认的类型转换接口，
 * 而是采用 ConversionService 体系，但 ConversionService 是线程安全的，
 * 所以在 Spring 3 后，如果你所选择的类型转换器是 ConversionService 而不是 PropertyEditors 那么 TypeConverters 则是线程安全的。
 *
 */
public interface BeanWrapper extends ConfigurablePropertyAccessor {

	/**
	 * Specify a limit for array and collection auto-growing.
	 * <p>Default is unlimited on a plain BeanWrapper.
	 * @since 4.1
	 */
	void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

	/**
	 * Return the limit for array and collection auto-growing.
	 * @since 4.1
	 */
	int getAutoGrowCollectionLimit();

	/**
	 * Return the bean instance wrapped by this object.
	 */
	Object getWrappedInstance();

	/**
	 * Return the type of the wrapped bean instance.
	 */
	Class<?> getWrappedClass();

	/**
	 * Obtain the PropertyDescriptors for the wrapped object
	 * (as determined by standard JavaBeans introspection).
	 * @return the PropertyDescriptors for the wrapped object
	 */
	PropertyDescriptor[] getPropertyDescriptors();

	/**
	 * Obtain the property descriptor for a specific property
	 * of the wrapped object.
	 * @param propertyName the property to obtain the descriptor for
	 * (may be a nested path, but no indexed/mapped property)
	 * @return the property descriptor for the specified property
	 * @throws InvalidPropertyException if there is no such property
	 */
	PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException;

}
