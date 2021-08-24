/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.env;

import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.lang.Nullable;

/**
 * 提供属性类型转换的功能
 */
public interface ConfigurablePropertyResolver extends PropertyResolver {

	// 返回执行类型转换时使用的 ConfigurableConversionService
	ConfigurableConversionService getConversionService();

	// 设置 ConfigurableConversionService
	void setConversionService(ConfigurableConversionService conversionService);

	// 设置占位符前缀
	void setPlaceholderPrefix(String placeholderPrefix);

	// 设置占位符后缀
	void setPlaceholderSuffix(String placeholderSuffix);

	// 设置占位符与默认值之间的分隔符
	void setValueSeparator(@Nullable String valueSeparator);

	// 设置当遇到嵌套在给定属性值内的不可解析的占位符时是否抛出异常
	// 当属性值包含不可解析的占位符时，getProperty(String)及其变体的实现必须检查此处设置的值以确定正确的行为。
	void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders);

	// 指定必须存在哪些属性，以便由validateRequiredProperties（）验证
	void setRequiredProperties(String... requiredProperties);

	// 验证setRequiredProperties指定的每个属性是否存在并解析为非null值
	void validateRequiredProperties() throws MissingRequiredPropertiesException;

}
