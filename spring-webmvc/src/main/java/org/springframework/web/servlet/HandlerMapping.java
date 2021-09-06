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

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;

/**
 * 处理器匹配接口，根据请求(handler)获得其的处理器(handler)和拦截器们(HandlerInterceptor数组)。
 *
 * 内部维护了一些 <访问路径, 处理器> 映射，负责为请求找到合适的处理器
 * 就相当于里面有<url,Controller>
 */
public interface HandlerMapping {

	/**
	 * Name of the {@link HttpServletRequest} attribute that contains the mapped
	 * handler for the best matching pattern.
	 * @since 4.3.21
	 */
	String BEST_MATCHING_HANDLER_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingHandler";

	/**
	 * Name of the {@link HttpServletRequest} attribute that contains the path
	 * used to look up the matching handler, which depending on the configured
	 * {@link org.springframework.web.util.UrlPathHelper} could be the full path
	 * or without the context path, decoded or not, etc.
	 * @since 5.2
	 * @deprecated as of 5.3 in favor of
	 * {@link org.springframework.web.util.UrlPathHelper#PATH_ATTRIBUTE} and
	 * {@link org.springframework.web.util.ServletRequestPathUtils#PATH_ATTRIBUTE}.
	 * To access the cached path used for request mapping, use
	 * {@link org.springframework.web.util.ServletRequestPathUtils#getCachedPathValue(ServletRequest)}.
	 */
	@Deprecated
	String LOOKUP_PATH = HandlerMapping.class.getName() + ".lookupPath";

	/**
	 * Name of the {@link HttpServletRequest} attribute that contains the path
	 * within the handler mapping, in case of a pattern match, or the full
	 * relevant URI (typically within the DispatcherServlet's mapping) else.
	 * <p>Note: This attribute is not required to be supported by all
	 * HandlerMapping implementations. URL-based HandlerMappings will
	 * typically support it, but handlers should not necessarily expect
	 * this request attribute to be present in all scenarios.
	 */
	String PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE = HandlerMapping.class.getName() + ".pathWithinHandlerMapping";

	/**
	 * Name of the {@link HttpServletRequest} attribute that contains the
	 * best matching pattern within the handler mapping.
	 * <p>Note: This attribute is not required to be supported by all
	 * HandlerMapping implementations. URL-based HandlerMappings will
	 * typically support it, but handlers should not necessarily expect
	 * this request attribute to be present in all scenarios.
	 */
	String BEST_MATCHING_PATTERN_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingPattern";

	/**
	 * Name of the boolean {@link HttpServletRequest} attribute that indicates
	 * whether type-level mappings should be inspected.
	 * <p>Note: This attribute is not required to be supported by all
	 * HandlerMapping implementations.
	 */
	String INTROSPECT_TYPE_LEVEL_MAPPING = HandlerMapping.class.getName() + ".introspectTypeLevelMapping";

	/**
	 * Name of the {@link HttpServletRequest} attribute that contains the URI
	 * templates map, mapping variable names to values.
	 * <p>Note: This attribute is not required to be supported by all
	 * HandlerMapping implementations. URL-based HandlerMappings will
	 * typically support it, but handlers should not necessarily expect
	 * this request attribute to be present in all scenarios.
	 */
	String URI_TEMPLATE_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".uriTemplateVariables";

	/**
	 * Name of the {@link HttpServletRequest} attribute that contains a map with
	 * URI variable names and a corresponding MultiValueMap of URI matrix
	 * variables for each.
	 * <p>Note: This attribute is not required to be supported by all
	 * HandlerMapping implementations and may also not be present depending on
	 * whether the HandlerMapping is configured to keep matrix variable content
	 */
	String MATRIX_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".matrixVariables";

	/**
	 * Name of the {@link HttpServletRequest} attribute that contains the set of
	 * producible MediaTypes applicable to the mapped handler.
	 * <p>Note: This attribute is not required to be supported by all
	 * HandlerMapping implementations. Handlers should not necessarily expect
	 * this request attribute to be present in all scenarios.
	 */
	String PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE = HandlerMapping.class.getName() + ".producibleMediaTypes";


	/**
	 * Whether this {@code HandlerMapping} instance has been enabled to use parsed
	 * {@link org.springframework.web.util.pattern.PathPattern}s in which case
	 * the {@link DispatcherServlet} automatically
	 * {@link org.springframework.web.util.ServletRequestPathUtils#parseAndCache parses}
	 * the {@code RequestPath} to make it available for
	 * {@link org.springframework.web.util.ServletRequestPathUtils#getParsedRequestPath
	 * access} in {@code HandlerMapping}s, {@code HandlerInterceptor}s, and
	 * other components.
	 * @since 5.3
	 */
	default boolean usesPathPatterns() {
		return false;
	}

	/**
	 * 获得请求对应的处理器和拦截器们
	 *
	 * 返回的对象类型是 HandlerExecutionChain
	 * 它包含处理器( handler )和拦截器们( HandlerInterceptor 数组 )。
	 */
	@Nullable
	HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;

}
