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

package org.springframework.web.servlet.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.Ordered;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 该类实现了【获得请求对应的处理器和拦截器们】的骨架逻辑，而暴露getHandlerInternal方法交由子类实现。
 *
 * 它的子类分为：
 * 1、AbstractUrlHandlerMapping 系，基于 URL 进行匹配。当然实际开发我们已经不用了
 * 2、AbstractHandlerMethodMapping 系，基于 Method 进行匹配。例如，我们所熟知的 @RequestMapping 等注解的方式。
 */
public abstract class AbstractHandlerMapping extends WebApplicationObjectSupport
		implements HandlerMapping, Ordered, BeanNameAware {

	/**
	 * 默认处理器。在获得不到处理器时，可使用该属性。
	 */
	@Nullable
	private Object defaultHandler;

	@Nullable
	private PathPatternParser patternParser;

	/**
	 * URL 路径工具类
	 */
	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	/**
	 * 路径匹配器
	 */
	private PathMatcher pathMatcher = new AntPathMatcher();

	/**
	 * 配置的拦截器数组.
	 *
	 * 在 {@link #initInterceptors()} 方法中，初始化到 {@link #adaptedInterceptors} 中
	 *
	 * 添加方式有两种：
	 *
	 * 1. {@link #setInterceptors(Object...)} 方法
	 * 2. {@link #extendInterceptors(List)} 方法
	 */
	private final List<Object> interceptors = new ArrayList<>();

	/**
	 * 初始化后的拦截器 HandlerInterceptor 数组
	 */
	private final List<HandlerInterceptor> adaptedInterceptors = new ArrayList<>();

	@Nullable
	private CorsConfigurationSource corsConfigurationSource;

	private CorsProcessor corsProcessor = new DefaultCorsProcessor();

	/**
	 * 顺序
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

	/**
	 * Bean 名字
	 */
	@Nullable
	private String beanName;


	/**
	 * Set the default handler for this handler mapping.
	 * This handler will be returned if no specific mapping was found.
	 * <p>Default is {@code null}, indicating no default handler.
	 */
	public void setDefaultHandler(@Nullable Object defaultHandler) {
		this.defaultHandler = defaultHandler;
	}

	/**
	 * Return the default handler for this handler mapping,
	 * or {@code null} if none.
	 */
	@Nullable
	public Object getDefaultHandler() {
		return this.defaultHandler;
	}

	/**
	 * Enable use of pre-parsed {@link PathPattern}s as an alternative to
	 * String pattern matching with {@link AntPathMatcher}. The syntax is
	 * largely the same but the {@code PathPattern} syntax is more tailored for
	 * web applications, and its implementation is more efficient.
	 * <p>This property is mutually exclusive with the following others which
	 * are effectively ignored when this is set:
	 * <ul>
	 * <li>{@link #setAlwaysUseFullPath} -- {@code PathPatterns} always use the
	 * full path and ignore the servletPath/pathInfo which are decoded and
	 * partially normalized and therefore not comparable against the
	 * {@link HttpServletRequest#getRequestURI() requestURI}.
	 * <li>{@link #setRemoveSemicolonContent} -- {@code PathPatterns} always
	 * ignore semicolon content for path matching purposes, but path parameters
	 * remain available for use in controllers via {@code @MatrixVariable}.
	 * <li>{@link #setUrlDecode} -- {@code PathPatterns} match one decoded path
	 * segment at a time and never need the full decoded path which can cause
	 * issues due to decoded reserved characters.
	 * <li>{@link #setUrlPathHelper} -- the request path is pre-parsed globally
	 * by the {@link org.springframework.web.servlet.DispatcherServlet
	 * DispatcherServlet} or by
	 * {@link org.springframework.web.filter.ServletRequestPathFilter
	 * ServletRequestPathFilter} using {@link ServletRequestPathUtils} and saved
	 * in a request attribute for re-use.
	 * <li>{@link #setPathMatcher} -- patterns are parsed to {@code PathPatterns}
	 * and used instead of String matching with {@code PathMatcher}.
	 * </ul>
	 * <p>By default this is not set.
	 * @param patternParser the parser to use
	 * @since 5.3
	 */
	public void setPatternParser(PathPatternParser patternParser) {
		this.patternParser = patternParser;
	}

	/**
	 * Return the {@link #setPatternParser(PathPatternParser) configured}
	 * {@code PathPatternParser}, or {@code null}.
	 * @since 5.3
	 */
	@Nullable
	public PathPatternParser getPatternParser() {
		return this.patternParser;
	}

	/**
	 * Shortcut to same property on the configured {@code UrlPathHelper}.
	 * <p><strong>Note:</strong> This property is mutually exclusive with and
	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
	 * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath(boolean)
	 */
	@SuppressWarnings("deprecation")
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setAlwaysUseFullPath(alwaysUseFullPath);
		}
	}

	/**
	 * Shortcut to same property on the underlying {@code UrlPathHelper}.
	 * <p><strong>Note:</strong> This property is mutually exclusive with and
	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
	 * @see org.springframework.web.util.UrlPathHelper#setUrlDecode(boolean)
	 */
	@SuppressWarnings("deprecation")
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setUrlDecode(urlDecode);
		}
	}

	/**
	 * Shortcut to same property on the underlying {@code UrlPathHelper}.
	 * <p><strong>Note:</strong> This property is mutually exclusive with and
	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
	 * @see org.springframework.web.util.UrlPathHelper#setRemoveSemicolonContent(boolean)
	 */
	@SuppressWarnings("deprecation")
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setRemoveSemicolonContent(removeSemicolonContent);
		}
	}

	/**
	 * Configure the UrlPathHelper to use for resolution of lookup paths.
	 * <p><strong>Note:</strong> This property is mutually exclusive with and
	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
	 */
	@SuppressWarnings("deprecation")
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setUrlPathHelper(urlPathHelper);
		}
	}

	/**
	 * Return the {@link #setUrlPathHelper configured} {@code UrlPathHelper}.
	 */
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * Configure the PathMatcher to use.
	 * <p><strong>Note:</strong> This property is mutually exclusive with and
	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
	 * <p>By default this is {@link AntPathMatcher}.
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setPathMatcher(pathMatcher);
		}
	}

	/**
	 * Return the {@link #setPathMatcher configured} {@code PathMatcher}.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * Set the interceptors to apply for all handlers mapped by this handler mapping.
	 * <p>Supported interceptor types are {@link HandlerInterceptor},
	 * {@link WebRequestInterceptor}, and {@link MappedInterceptor}.
	 * Mapped interceptors apply only to request URLs that match its path patterns.
	 * Mapped interceptor beans are also detected by type during initialization.
	 * @param interceptors array of handler interceptors
	 * @see #adaptInterceptor
	 * @see org.springframework.web.servlet.HandlerInterceptor
	 * @see org.springframework.web.context.request.WebRequestInterceptor
	 * @see MappedInterceptor
	 */
	public void setInterceptors(Object... interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
	}

	/**
	 * Set "global" CORS configuration mappings. The first matching URL pattern
	 * determines the {@code CorsConfiguration} to use which is then further
	 * {@link CorsConfiguration#combine(CorsConfiguration) combined} with the
	 * {@code CorsConfiguration} for the selected handler.
	 * <p>This is mutually exclusie with
	 * {@link #setCorsConfigurationSource(CorsConfigurationSource)}.
	 * @since 4.2
	 * @see #setCorsProcessor(CorsProcessor)
	 */
	public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
		if (CollectionUtils.isEmpty(corsConfigurations)) {
			this.corsConfigurationSource = null;
			return;
		}
		UrlBasedCorsConfigurationSource source;
		if (getPatternParser() != null) {
			source = new UrlBasedCorsConfigurationSource(getPatternParser());
			source.setCorsConfigurations(corsConfigurations);
		}
		else {
			source = new UrlBasedCorsConfigurationSource();
			source.setCorsConfigurations(corsConfigurations);
			source.setPathMatcher(this.pathMatcher);
			source.setUrlPathHelper(this.urlPathHelper);
		}
		setCorsConfigurationSource(source);
	}

	/**
	 * Set a {@code CorsConfigurationSource} for "global" CORS config. The
	 * {@code CorsConfiguration} determined by the source is
	 * {@link CorsConfiguration#combine(CorsConfiguration) combined} with the
	 * {@code CorsConfiguration} for the selected handler.
	 * <p>This is mutually exclusie with {@link #setCorsConfigurations(Map)}.
	 * @since 5.1
	 * @see #setCorsProcessor(CorsProcessor)
	 */
	public void setCorsConfigurationSource(CorsConfigurationSource source) {
		Assert.notNull(source, "CorsConfigurationSource must not be null");
		this.corsConfigurationSource = source;
		if (source instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) source).setAllowInitLookupPath(false);
		}
	}

	/**
	 * Return the {@link #setCorsConfigurationSource(CorsConfigurationSource)
	 * configured} {@code CorsConfigurationSource}, if any.
	 * @since 5.3
	 */
	@Nullable
	public CorsConfigurationSource getCorsConfigurationSource() {
		return this.corsConfigurationSource;
	}

	/**
	 * Configure a custom {@link CorsProcessor} to use to apply the matched
	 * {@link CorsConfiguration} for a request.
	 * <p>By default {@link DefaultCorsProcessor} is used.
	 * @since 4.2
	 */
	public void setCorsProcessor(CorsProcessor corsProcessor) {
		Assert.notNull(corsProcessor, "CorsProcessor must not be null");
		this.corsProcessor = corsProcessor;
	}

	/**
	 * Return the configured {@link CorsProcessor}.
	 */
	public CorsProcessor getCorsProcessor() {
		return this.corsProcessor;
	}

	/**
	 * Specify the order value for this HandlerMapping bean.
	 * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	protected String formatMappingName() {
		return this.beanName != null ? "'" + this.beanName + "'" : "<unknown>";
	}


	/**
	 * 初始化拦截器。
	 *
	 * 该方法，是对 WebApplicationObjectSupport 的覆写，而 WebApplicationObjectSupport 的继承关系是
	 * WebApplicationObjectSupport => ApplicationObjectSupport => ApplicationContextAware 。
	 */
	@Override
	protected void initApplicationContext() throws BeansException {

		// <1> 空方法。交给子类实现，用于注册自定义的拦截器到 interceptors 中。目前暂无子类实现。
		extendInterceptors(this.interceptors);

		// <2> 扫描已注册的 MappedInterceptor 的 Bean 们，添加到 mappedInterceptors 中
		detectMappedInterceptors(this.adaptedInterceptors);

		// <3> 将 interceptors 初始化成 HandlerInterceptor 类型，添加到 mappedInterceptors 中
		initInterceptors();
	}

	/**
	 * Extension hook that subclasses can override to register additional interceptors,
	 * given the configured interceptors (see {@link #setInterceptors}).
	 * <p>Will be invoked before {@link #initInterceptors()} adapts the specified
	 * interceptors into {@link HandlerInterceptor} instances.
	 * <p>The default implementation is empty.
	 * @param interceptors the configured interceptor List (never {@code null}), allowing
	 * to add further interceptors before as well as after the existing interceptors
	 */
	protected void extendInterceptors(List<Object> interceptors) {
	}

	/**
	 * 扫描已注册的 MappedInterceptor 的 Bean 们，添加到 mappedInterceptors 中。
	 */
	protected void detectMappedInterceptors(List<HandlerInterceptor> mappedInterceptors) {

		// MappedInterceptor 会根据请求路径做匹配，是否进行拦截。
		mappedInterceptors.addAll(BeanFactoryUtils.beansOfTypeIncludingAncestors(
				obtainApplicationContext(), MappedInterceptor.class, true, false).values());
	}

	/**
	 * 调用 #initInterceptors() 方法，将 interceptors 初始化成 HandlerInterceptor 类型，添加到 mappedInterceptors 中。
	 */
	protected void initInterceptors() {
		if (!this.interceptors.isEmpty()) {

			// 遍历 interceptors 数组
			for (int i = 0; i < this.interceptors.size(); i++) {

				// 获得 interceptor 对象
				Object interceptor = this.interceptors.get(i);
				if (interceptor == null) { // 若为空，抛出 IllegalArgumentException 异常
					throw new IllegalArgumentException("Entry number " + i + " in interceptors array is null");
				}

				// 将 interceptors 初始化成 HandlerInterceptor 类型，添加到 mappedInterceptors 中
				// 注意，HandlerInterceptor 无需进行路径匹配，直接拦截全部
				this.adaptedInterceptors.add(adaptInterceptor(interceptor));
			}
		}
	}

	/**
	 * 将 interceptors 初始化成 HandlerInterceptor 类型。
	 */
	protected HandlerInterceptor adaptInterceptor(Object interceptor) {

		// HandlerInterceptor 类型，直接返回
		if (interceptor instanceof HandlerInterceptor) {
			return (HandlerInterceptor) interceptor;
		}

		// WebRequestInterceptor 类型，适配成 WebRequestHandlerInterceptorAdapter 对象，然后返回
		else if (interceptor instanceof WebRequestInterceptor) {
			return new WebRequestHandlerInterceptorAdapter((WebRequestInterceptor) interceptor);
		}

		else { // 错误类型，抛出 IllegalArgumentException 异常
			throw new IllegalArgumentException("Interceptor type not supported: " + interceptor.getClass().getName());
		}
	}

	/**
	 * Return the adapted interceptors as {@link HandlerInterceptor} array.
	 * @return the array of {@link HandlerInterceptor HandlerInterceptor}s,
	 * or {@code null} if none
	 */
	@Nullable
	protected final HandlerInterceptor[] getAdaptedInterceptors() {
		return (!this.adaptedInterceptors.isEmpty() ?
				this.adaptedInterceptors.toArray(new HandlerInterceptor[0]) : null);
	}

	/**
	 * Return all configured {@link MappedInterceptor}s as an array.
	 * @return the array of {@link MappedInterceptor}s, or {@code null} if none
	 */
	@Nullable
	protected final MappedInterceptor[] getMappedInterceptors() {
		List<MappedInterceptor> mappedInterceptors = new ArrayList<>(this.adaptedInterceptors.size());
		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
			if (interceptor instanceof MappedInterceptor) {
				mappedInterceptors.add((MappedInterceptor) interceptor);
			}
		}
		return (!mappedInterceptors.isEmpty() ? mappedInterceptors.toArray(new MappedInterceptor[0]) : null);
	}


	/**
	 * Return "true" if this {@code HandlerMapping} has been
	 * {@link #setPatternParser enabled} to use parsed {@code PathPattern}s.
	 */
	@Override
	public boolean usesPathPatterns() {
		return getPatternParser() != null;
	}

	/**
	 * 获得请求对应的 HandlerExecutionChain 对象。
	 */
	@Override
	@Nullable
	public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {

		// <1> 获得处理器。该方法是抽象方法，由子类实现
		Object handler = getHandlerInternal(request);

		// <2> 获得不到，则使用默认处理器
		if (handler == null) {
			handler = getDefaultHandler();
		}

		// <3> 还是获得不到，则返回 null
		if (handler == null) {
			return null;
		}

		// <4> 如果找到的处理器是 String 类型，则从容器中找到 String 对应的 Bean 类型作为处理器。
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = obtainApplicationContext().getBean(handlerName);
		}

		// <5> 获得 HandlerExecutionChain 对象
		HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);

		if (logger.isTraceEnabled()) {
			logger.trace("Mapped to " + handler);
		}
		else if (logger.isDebugEnabled() && !request.getDispatcherType().equals(DispatcherType.ASYNC)) {
			logger.debug("Mapped to " + executionChain.getHandler());
		}

		if (hasCorsConfigurationSource(handler) || CorsUtils.isPreFlightRequest(request)) {
			CorsConfiguration config = getCorsConfiguration(handler, request);
			if (getCorsConfigurationSource() != null) {
				CorsConfiguration globalConfig = getCorsConfigurationSource().getCorsConfiguration(request);
				config = (globalConfig != null ? globalConfig.combine(config) : config);
			}
			if (config != null) {
				config.validateAllowCredentials();
			}
			executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
		}

		return executionChain;
	}

	/**
	 * Look up a handler for the given request, returning {@code null} if no
	 * specific one is found. This method is called by {@link #getHandler};
	 * a {@code null} return value will lead to the default handler, if one is set.
	 * <p>On CORS pre-flight requests this method should return a match not for
	 * the pre-flight request but for the expected actual request based on the URL
	 * path, the HTTP methods from the "Access-Control-Request-Method" header, and
	 * the headers from the "Access-Control-Request-Headers" header thus allowing
	 * the CORS configuration to be obtained via {@link #getCorsConfiguration(Object, HttpServletRequest)},
	 * <p>Note: This method may also return a pre-built {@link HandlerExecutionChain},
	 * combining a handler object with dynamically determined interceptors.
	 * Statically specified interceptors will get merged into such an existing chain.
	 * @param request current HTTP request
	 * @return the corresponding handler instance, or {@code null} if none found
	 * @throws Exception if there is an internal error
	 */
	@Nullable
	protected abstract Object getHandlerInternal(HttpServletRequest request) throws Exception;

	/**
	 * Initialize the path to use for request mapping.
	 * <p>When parsed patterns are {@link #usesPathPatterns() enabled} a parsed
	 * {@code RequestPath} is expected to have been
	 * {@link ServletRequestPathUtils#parseAndCache(HttpServletRequest) parsed}
	 * externally by the {@link org.springframework.web.servlet.DispatcherServlet}
	 * or {@link org.springframework.web.filter.ServletRequestPathFilter}.
	 * <p>Otherwise for String pattern matching via {@code PathMatcher} the
	 * path is {@link UrlPathHelper#resolveAndCacheLookupPath resolved} by this
	 * method.
	 * @since 5.3
	 */
	protected String initLookupPath(HttpServletRequest request) {
		if (usesPathPatterns()) {
			request.removeAttribute(UrlPathHelper.PATH_ATTRIBUTE);
			RequestPath requestPath = ServletRequestPathUtils.getParsedRequestPath(request);
			String lookupPath = requestPath.pathWithinApplication().value();
			return UrlPathHelper.defaultInstance.removeSemicolonContent(lookupPath);
		}
		else {
			return getUrlPathHelper().resolveAndCacheLookupPath(request);
		}
	}

	/**
	 * 获得 HandlerExecutionChain 对象。
	 */
	protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {

		// 创建 HandlerExecutionChain 对象
		HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain ?
				(HandlerExecutionChain) handler : new HandlerExecutionChain(handler));

		// 遍历 adaptedInterceptors 数组，获得请求匹配的拦截器
		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {

			// 需要匹配，若路径匹配，则添加到 chain 中
			if (interceptor instanceof MappedInterceptor) {
				MappedInterceptor mappedInterceptor = (MappedInterceptor) interceptor;
				if (mappedInterceptor.matches(request)) {
					chain.addInterceptor(mappedInterceptor.getInterceptor());
				}
			}
			else { // 无需匹配，直接添加到 chain 中
				chain.addInterceptor(interceptor);
			}
		}
		return chain;
	}

	/**
	 * Return {@code true} if there is a {@link CorsConfigurationSource} for this handler.
	 * @since 5.2
	 */
	protected boolean hasCorsConfigurationSource(Object handler) {
		if (handler instanceof HandlerExecutionChain) {
			handler = ((HandlerExecutionChain) handler).getHandler();
		}
		return (handler instanceof CorsConfigurationSource || this.corsConfigurationSource != null);
	}

	/**
	 * Retrieve the CORS configuration for the given handler.
	 * @param handler the handler to check (never {@code null}).
	 * @param request the current request.
	 * @return the CORS configuration for the handler, or {@code null} if none
	 * @since 4.2
	 */
	@Nullable
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
		Object resolvedHandler = handler;
		if (handler instanceof HandlerExecutionChain) {
			resolvedHandler = ((HandlerExecutionChain) handler).getHandler();
		}
		if (resolvedHandler instanceof CorsConfigurationSource) {
			return ((CorsConfigurationSource) resolvedHandler).getCorsConfiguration(request);
		}
		return null;
	}

	/**
	 * Update the HandlerExecutionChain for CORS-related handling.
	 * <p>For pre-flight requests, the default implementation replaces the selected
	 * handler with a simple HttpRequestHandler that invokes the configured
	 * {@link #setCorsProcessor}.
	 * <p>For actual requests, the default implementation inserts a
	 * HandlerInterceptor that makes CORS-related checks and adds CORS headers.
	 * @param request the current request
	 * @param chain the handler chain
	 * @param config the applicable CORS configuration (possibly {@code null})
	 * @since 4.2
	 */
	protected HandlerExecutionChain getCorsHandlerExecutionChain(HttpServletRequest request,
			HandlerExecutionChain chain, @Nullable CorsConfiguration config) {

		if (CorsUtils.isPreFlightRequest(request)) {
			HandlerInterceptor[] interceptors = chain.getInterceptors();
			return new HandlerExecutionChain(new PreFlightHandler(config), interceptors);
		}
		else {
			chain.addInterceptor(0, new CorsInterceptor(config));
			return chain;
		}
	}


	private class PreFlightHandler implements HttpRequestHandler, CorsConfigurationSource {

		@Nullable
		private final CorsConfiguration config;

		public PreFlightHandler(@Nullable CorsConfiguration config) {
			this.config = config;
		}

		@Override
		public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
			corsProcessor.processRequest(this.config, request, response);
		}

		@Override
		@Nullable
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			return this.config;
		}
	}


	private class CorsInterceptor implements HandlerInterceptor, CorsConfigurationSource {

		@Nullable
		private final CorsConfiguration config;

		public CorsInterceptor(@Nullable CorsConfiguration config) {
			this.config = config;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
				throws Exception {

			// Consistent with CorsFilter, ignore ASYNC dispatches
			WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
			if (asyncManager.hasConcurrentResult()) {
				return true;
			}

			return corsProcessor.processRequest(this.config, request, response);
		}

		@Override
		@Nullable
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			return this.config;
		}
	}

}
