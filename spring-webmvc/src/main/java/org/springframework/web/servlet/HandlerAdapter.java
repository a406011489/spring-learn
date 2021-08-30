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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * 处理器适配器接口。
 * 因为处理器 handler 的类型是 Object 类型，需要有一个调用者来实现 handler 是怎么被使用，怎么被执行。
 * 而 HandlerAdapter 的用途就在于此。
 */
public interface HandlerAdapter {

	/**
	 * 是否支持该处理器
	 */
	boolean supports(Object handler);

	/**
	 * 执行处理器，返回 ModelAndView 结果
	 */
	@Nullable
	ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;

	/**
	 * 返回请求的最新更新时间。
	 *
	 * 如果不支持该操作，则返回 -1 即可
	 */
	long getLastModified(HttpServletRequest request, Object handler);

}
