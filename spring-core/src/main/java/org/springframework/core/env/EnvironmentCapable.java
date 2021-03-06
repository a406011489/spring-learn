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

package org.springframework.core.env;

/**
 * 提供当前系统环境 Environment 组件。
 * 提供了一个 #getEnvironment() 方法，用于返回 Environment 实例对象。
 * 该方法在 AbstractApplicationContext 实现。
 */
public interface EnvironmentCapable {

	/**
	 * Return the {@link Environment} associated with this component.
	 */
	Environment getEnvironment();

}
