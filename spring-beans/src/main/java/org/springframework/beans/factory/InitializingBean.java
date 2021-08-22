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

package org.springframework.beans.factory;

/**
 * 为 bean 提供了定义初始化方法的方式，它仅包含了一个方法：#afterPropertiesSet() 。
 *
 * Spring 在完成实例化后，设置完所有属性，
 * 进行 “Aware 接口” 和 “BeanPostProcessor 前置处理”之后，
 * 会接着检测当前 bean 对象是否实现了 InitializingBean 接口。
 * 如果是，则会调用其 #afterPropertiesSet() 方法，进一步调整 bean 实例对象的状态。
 */
public interface InitializingBean {

	/**
	 * 该方法在 BeanFactory 设置完了所有属性之后被调用
	 * 该方法允许 bean 实例设置了所有 bean 属性时执行初始化工作，如果该过程出现了错误则需要抛出异常
	 */
	void afterPropertiesSet() throws Exception;

}
