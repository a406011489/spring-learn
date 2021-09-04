/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * 该类包含处理器(handler)和拦截器们(HandlerInterceptor数组)。
 */
public class HandlerExecutionChain {

	private static final Log logger = LogFactory.getLog(HandlerExecutionChain.class);

	/**
	 * 处理器，就是我们所知道的Controller
	 */
	private final Object handler;

	/**
	 * 拦截器数组
	 */
	private final List<HandlerInterceptor> interceptorList = new ArrayList<>();

	private int interceptorIndex = -1;


	/**
	 * Create a new HandlerExecutionChain.
	 * @param handler the handler object to execute
	 */
	public HandlerExecutionChain(Object handler) {
		this(handler, (HandlerInterceptor[]) null);
	}

	/**
	 * Create a new HandlerExecutionChain.
	 * @param handler the handler object to execute
	 * @param interceptors the array of interceptors to apply
	 * (in the given order) before the handler itself executes
	 */
	public HandlerExecutionChain(Object handler, @Nullable HandlerInterceptor... interceptors) {
		this(handler, (interceptors != null ? Arrays.asList(interceptors) : Collections.emptyList()));
	}

	/**
	 * Create a new HandlerExecutionChain.
	 * @param handler the handler object to execute
	 * @param interceptorList the list of interceptors to apply
	 * (in the given order) before the handler itself executes
	 * @since 5.3
	 */
	public HandlerExecutionChain(Object handler, List<HandlerInterceptor> interceptorList) {
		if (handler instanceof HandlerExecutionChain) {
			HandlerExecutionChain originalChain = (HandlerExecutionChain) handler;
			this.handler = originalChain.getHandler();

			// 初始化到 interceptorList 中
			this.interceptorList.addAll(originalChain.interceptorList);
		}
		else {
			this.handler = handler;
		}
		this.interceptorList.addAll(interceptorList);
	}


	/**
	 * Return the handler object to execute.
	 */
	public Object getHandler() {
		return this.handler;
	}

	/**
	 * Add the given interceptor to the end of this chain.
	 */
	public void addInterceptor(HandlerInterceptor interceptor) {
		this.interceptorList.add(interceptor);
	}

	/**
	 * 添加拦截器到 interceptorList 中。
	 */
	public void addInterceptor(int index, HandlerInterceptor interceptor) {
		this.interceptorList.add(index, interceptor);
	}

	/**
	 * Add the given interceptors to the end of this chain.
	 */
	public void addInterceptors(HandlerInterceptor... interceptors) {
		CollectionUtils.mergeArrayIntoCollection(interceptors, this.interceptorList);
	}

	/**
	 * Return the array of interceptors to apply (in the given order).
	 * @return the array of HandlerInterceptors instances (may be {@code null})
	 */
	@Nullable
	public HandlerInterceptor[] getInterceptors() {
		// 将 interceptorList 初始化到 interceptors 中
		return (!this.interceptorList.isEmpty() ? this.interceptorList.toArray(new HandlerInterceptor[0]) : null);
	}

	/**
	 * 获得 interceptors 数组。
	 */
	public List<HandlerInterceptor> getInterceptorList() {
		return (!this.interceptorList.isEmpty() ? Collections.unmodifiableList(this.interceptorList) :
				Collections.emptyList());
	}


	/**
	 * 应用拦截器的前置处理。
	 */
	boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {

		// <2> 遍历拦截器数组
		for (int i = 0; i < this.interceptorList.size(); i++) {
			HandlerInterceptor interceptor = this.interceptorList.get(i);

			// <3> 前置处理
			if (!interceptor.preHandle(request, response, this.handler)) {

				// <3.1> 触发已完成处理
				triggerAfterCompletion(request, response, null);

				// 返回 false ，前置处理失败
				return false;
			}

			// <3.2> 标记 interceptorIndex 位置
			this.interceptorIndex = i;
		}

		// <4> 返回 true ，前置处理成功
		return true;
	}

	/**
	 * 应用拦截器的后置处理。
	 */
	void applyPostHandle(HttpServletRequest request, HttpServletResponse response, @Nullable ModelAndView mv)
			throws Exception {

		// 同样的，倒序后置处理
		for (int i = this.interceptorList.size() - 1; i >= 0; i--) {
			HandlerInterceptor interceptor = this.interceptorList.get(i);
			interceptor.postHandle(request, response, this.handler, mv);
		}
	}

	/**
	 * 触发拦截器的已完成处理。
	 */
	void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response, @Nullable Exception ex) {

		// 进行倒序处理！！！
		for (int i = this.interceptorIndex; i >= 0; i--) {
			HandlerInterceptor interceptor = this.interceptorList.get(i);
			try {
				// 已完成处理
				interceptor.afterCompletion(request, response, this.handler, ex);
			}
			catch (Throwable ex2) { // 注意，如果执行失败，仅仅会打印错误日志，不会结束循环
				logger.error("HandlerInterceptor.afterCompletion threw exception", ex2);
			}
		}
	}

	/**
	 * Apply afterConcurrentHandlerStarted callback on mapped AsyncHandlerInterceptors.
	 */
	void applyAfterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response) {
		for (int i = this.interceptorList.size() - 1; i >= 0; i--) {
			HandlerInterceptor interceptor = this.interceptorList.get(i);
			if (interceptor instanceof AsyncHandlerInterceptor) {
				try {
					AsyncHandlerInterceptor asyncInterceptor = (AsyncHandlerInterceptor) interceptor;
					asyncInterceptor.afterConcurrentHandlingStarted(request, response, this.handler);
				}
				catch (Throwable ex) {
					if (logger.isErrorEnabled()) {
						logger.error("Interceptor [" + interceptor + "] failed in afterConcurrentHandlingStarted", ex);
					}
				}
			}
		}
	}


	/**
	 * Delegates to the handler's {@code toString()} implementation.
	 */
	@Override
	public String toString() {
		return "HandlerExecutionChain with [" + getHandler() + "] and " + this.interceptorList.size() + " interceptors";
	}

}
