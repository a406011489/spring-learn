/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Map;

/**
 * 提供设置激活的 profile 和默认的 profile 的功能以及操作 Properties 的工具
 */
public interface ConfigurableEnvironment extends Environment, ConfigurablePropertyResolver {

	// 指定该环境下的 profile 集
	void setActiveProfiles(String... profiles);

	// 增加此环境的 profile
	void addActiveProfile(String profile);

	// 设置默认的 profile
	void setDefaultProfiles(String... profiles);

	// 返回此环境的 PropertySources
	MutablePropertySources getPropertySources();

	// 尝试返回 System.getenv() 的值，若失败则返回通过 System.getenv(string) 的来访问各个键的映射
	Map<String, Object> getSystemEnvironment();

	// 尝试返回 System.getProperties() 的值，若失败则返回通过 System.getProperties(string) 的来访问各个键的映射
	Map<String, Object> getSystemProperties();

	void merge(ConfigurableEnvironment parent);

}
