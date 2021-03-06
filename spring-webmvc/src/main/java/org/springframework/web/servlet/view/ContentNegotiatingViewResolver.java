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

package org.springframework.web.servlet.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * ???????????? ViewResolver ?????????
 *
 * ????????????????????????????????? View ??? ViewResolver ????????????
 * ?????????????????????????????? "Content-Type" ??????????????????
 */
public class ContentNegotiatingViewResolver extends WebApplicationObjectSupport
		implements ViewResolver, Ordered, InitializingBean {

	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	/**
	 * ContentNegotiationManager ???????????????????????? {@link #contentNegotiationManager} ??????
	 */
	private final ContentNegotiationManagerFactoryBean cnmFactoryBean = new ContentNegotiationManagerFactoryBean();

	/**
	 * ???????????? View ?????????????????? {@link #NOT_ACCEPTABLE_VIEW}
	 */
	private boolean useNotAcceptableStatusCode = false;

	/**
	 * ?????? View ??????
	 */
	@Nullable
	private List<View> defaultViews;

	/**
	 * ViewResolver ??????
	 */
	@Nullable
	private List<ViewResolver> viewResolvers;

	/**
	 * ????????????????????????
	 */
	private int order = Ordered.HIGHEST_PRECEDENCE;


	/**
	 * Set the {@link ContentNegotiationManager} to use to determine requested media types.
	 * <p>If not set, ContentNegotiationManager's default constructor will be used,
	 * applying a {@link org.springframework.web.accept.HeaderContentNegotiationStrategy}.
	 * @see ContentNegotiationManager#ContentNegotiationManager()
	 */
	public void setContentNegotiationManager(@Nullable ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * Return the {@link ContentNegotiationManager} to use to determine requested media types.
	 * @since 4.1.9
	 */
	@Nullable
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	/**
	 * Indicate whether a {@link HttpServletResponse#SC_NOT_ACCEPTABLE 406 Not Acceptable}
	 * status code should be returned if no suitable view can be found.
	 * <p>Default is {@code false}, meaning that this view resolver returns {@code null} for
	 * {@link #resolveViewName(String, Locale)} when an acceptable view cannot be found.
	 * This will allow for view resolvers chaining. When this property is set to {@code true},
	 * {@link #resolveViewName(String, Locale)} will respond with a view that sets the
	 * response status to {@code 406 Not Acceptable} instead.
	 */
	public void setUseNotAcceptableStatusCode(boolean useNotAcceptableStatusCode) {
		this.useNotAcceptableStatusCode = useNotAcceptableStatusCode;
	}

	/**
	 * Whether to return HTTP Status 406 if no suitable is found.
	 */
	public boolean isUseNotAcceptableStatusCode() {
		return this.useNotAcceptableStatusCode;
	}

	/**
	 * Set the default views to use when a more specific view can not be obtained
	 * from the {@link ViewResolver} chain.
	 */
	public void setDefaultViews(List<View> defaultViews) {
		this.defaultViews = defaultViews;
	}

	public List<View> getDefaultViews() {
		return (this.defaultViews != null ? Collections.unmodifiableList(this.defaultViews) :
				Collections.emptyList());
	}

	/**
	 * Sets the view resolvers to be wrapped by this view resolver.
	 * <p>If this property is not set, view resolvers will be detected automatically.
	 */
	public void setViewResolvers(List<ViewResolver> viewResolvers) {
		this.viewResolvers = viewResolvers;
	}

	public List<ViewResolver> getViewResolvers() {
		return (this.viewResolvers != null ? Collections.unmodifiableList(this.viewResolvers) :
				Collections.emptyList());
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	protected void initServletContext(ServletContext servletContext) {

		// <1> ???????????? ViewResolver ??? Bean ???
		Collection<ViewResolver> matchingBeans =
				BeanFactoryUtils.beansOfTypeIncludingAncestors(obtainApplicationContext(), ViewResolver.class).values();

		// <1.1> ?????????????????? viewResolvers ??????????????? matchingBeans ?????? viewResolvers ???
		if (this.viewResolvers == null) {
			this.viewResolvers = new ArrayList<>(matchingBeans.size());
			for (ViewResolver viewResolver : matchingBeans) {
				if (this != viewResolver) {
					this.viewResolvers.add(viewResolver);
				}
			}
		}

		// <1.2> ?????????????????? viewResolvers ??????????????? matchingBeans ???????????????????????????????????????????????????????????????????????????
		else {
			for (int i = 0; i < this.viewResolvers.size(); i++) {
				ViewResolver vr = this.viewResolvers.get(i);

				// ???????????? matchingBeans ??????????????????????????????????????? continue
				if (matchingBeans.contains(vr)) {
					continue;
				}

				// ???????????? matchingBeans ????????????????????????????????????????????????
				String name = vr.getClass().getName() + i;
				obtainApplicationContext().getAutowireCapableBeanFactory().initializeBean(vr, name);
			}

		}

		// <1.3> ?????? viewResolvers ??????
		AnnotationAwareOrderComparator.sort(this.viewResolvers);

		// <2> ?????? cnmFactoryBean ??? servletContext ??????
		this.cnmFactoryBean.setServletContext(servletContext);
	}

	@Override
	public void afterPropertiesSet() {

		// ?????? contentNegotiationManager ????????????????????????
		if (this.contentNegotiationManager == null) {
			this.contentNegotiationManager = this.cnmFactoryBean.build();
		}
		if (this.viewResolvers == null || this.viewResolvers.isEmpty()) {
			logger.warn("No ViewResolvers configured");
		}
	}


	@Override
	@Nullable
	public View resolveViewName(String viewName, Locale locale) throws Exception {
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		Assert.state(attrs instanceof ServletRequestAttributes, "No current ServletRequestAttributes");

		// <1> ?????? MediaType ??????
		List<MediaType> requestedMediaTypes = getMediaTypes(((ServletRequestAttributes) attrs).getRequest());
		if (requestedMediaTypes != null) {

			// <2.1> ??????????????? View ??????
			List<View> candidateViews = getCandidateViews(viewName, locale, requestedMediaTypes);

			// <2.2> ?????????????????? View ??????
			View bestView = getBestView(candidateViews, requestedMediaTypes, attrs);

			// ??????????????????????????????
			if (bestView != null) {
				return bestView;
			}
		}

		String mediaTypeInfo = logger.isDebugEnabled() && requestedMediaTypes != null ?
				" given " + requestedMediaTypes.toString() : "";

		// <3> ?????????????????? View ?????????????????? useNotAcceptableStatusCode ????????? NOT_ACCEPTABLE_VIEW ??? null ???
		if (this.useNotAcceptableStatusCode) {
			if (logger.isDebugEnabled()) {
				logger.debug("Using 406 NOT_ACCEPTABLE" + mediaTypeInfo);
			}
			return NOT_ACCEPTABLE_VIEW;
		}
		else {
			logger.debug("View remains unresolved" + mediaTypeInfo);
			return null;
		}
	}

	/**
	 * ?????? MediaType ?????????
	 */
	@Nullable
	protected List<MediaType> getMediaTypes(HttpServletRequest request) {
		Assert.state(this.contentNegotiationManager != null, "No ContentNegotiationManager set");
		try {

			// ?????? ServletWebRequest ??????
			ServletWebRequest webRequest = new ServletWebRequest(request);

			// ????????????????????????????????? MediaType ??????????????????????????????????????? ACCEPT ?????????
			List<MediaType> acceptableMediaTypes = this.contentNegotiationManager.resolveMediaTypes(webRequest);

			// ?????????????????? MediaType ??????
			List<MediaType> producibleMediaTypes = getProducibleMediaTypes(request);

			// ?????? acceptableTypes ???????????????????????? producibleType ????????? mediaTypesToUse ???????????????
			Set<MediaType> compatibleMediaTypes = new LinkedHashSet<>();
			for (MediaType acceptable : acceptableMediaTypes) {
				for (MediaType producible : producibleMediaTypes) {
					if (acceptable.isCompatibleWith(producible)) {
						compatibleMediaTypes.add(getMostSpecificMediaType(acceptable, producible));
					}
				}
			}

			// ?????? MediaType ??? specificity???quality ??????
			List<MediaType> selectedMediaTypes = new ArrayList<>(compatibleMediaTypes);
			MediaType.sortBySpecificityAndQuality(selectedMediaTypes);
			return selectedMediaTypes;
		}
		catch (HttpMediaTypeNotAcceptableException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug(ex.getMessage());
			}
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private List<MediaType> getProducibleMediaTypes(HttpServletRequest request) {
		Set<MediaType> mediaTypes = (Set<MediaType>)
				request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			return new ArrayList<>(mediaTypes);
		}
		else {
			return Collections.singletonList(MediaType.ALL);
		}
	}

	/**
	 * Return the more specific of the acceptable and the producible media types
	 * with the q-value of the former.
	 */
	private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
		produceType = produceType.copyQualityValue(acceptType);
		return (MediaType.SPECIFICITY_COMPARATOR.compare(acceptType, produceType) < 0 ? acceptType : produceType);
	}

	/**
	 * ??????????????? View ?????????
	 */
	private List<View> getCandidateViews(String viewName, Locale locale, List<MediaType> requestedMediaTypes)
			throws Exception {

		// ?????? View ??????
		List<View> candidateViews = new ArrayList<>();

		// <1> ?????????????????? viewResolvers ????????? View ???????????????????????? candidateViews ???
		if (this.viewResolvers != null) {
			Assert.state(this.contentNegotiationManager != null, "No ContentNegotiationManager set");

			// <1.1> ?????? viewResolvers ??????
			for (ViewResolver viewResolver : this.viewResolvers) {

				// <1.2> ?????????????????? View ?????????????????? candidateViews ???
				View view = viewResolver.resolveViewName(viewName, locale);
				if (view != null) {
					candidateViews.add(view);
				}

				// <1.3> ??????????????????????????????????????????????????? View ?????????????????? candidateViews ???
				// <1.3.1> ?????? MediaType ??????
				for (MediaType requestedMediaType : requestedMediaTypes) {

					// <1.3.2> ?????? MediaType ??????????????????????????????
					List<String> extensions = this.contentNegotiationManager.resolveFileExtensions(requestedMediaType);

					// <1.3.3> ???????????????????????????
					for (String extension : extensions) {

						// <1.3.4> ??????????????????????????????????????? View ?????????????????? candidateViews ???
						String viewNameWithExtension = viewName + '.' + extension;
						view = viewResolver.resolveViewName(viewNameWithExtension, locale);
						if (view != null) {
							candidateViews.add(view);
						}
					}
				}
			}
		}

		// <2> ?????????????????? defaultViews ??? candidateViews ???
		if (!CollectionUtils.isEmpty(this.defaultViews)) {
			candidateViews.addAll(this.defaultViews);
		}
		return candidateViews;
	}

	/**
	 * ?????????????????? View ?????????
	 */
	@Nullable
	private View getBestView(List<View> candidateViews, List<MediaType> requestedMediaTypes, RequestAttributes attrs) {

		// <1> ?????? candidateView ?????????????????????????????? View ??????????????????????????????????????????????????? View ?????????????????????
		for (View candidateView : candidateViews) {
			if (candidateView instanceof SmartView) {
				SmartView smartView = (SmartView) candidateView;
				if (smartView.isRedirectView()) {
					return candidateView;
				}
			}
		}

		// <2> ?????? requestedMediaTypes ??????
		for (MediaType mediaType : requestedMediaTypes) {
			for (View candidateView : candidateViews) {
				if (StringUtils.hasText(candidateView.getContentType())) {

					// <2.1> ?????? MediaType ??????????????????????????? View ??????
					MediaType candidateContentType = MediaType.parseMediaType(candidateView.getContentType());
					if (mediaType.isCompatibleWith(candidateContentType)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Selected '" + mediaType + "' given " + requestedMediaTypes);
						}
						attrs.setAttribute(View.SELECTED_CONTENT_TYPE, mediaType, RequestAttributes.SCOPE_REQUEST);
						return candidateView;
					}
				}
			}
		}
		return null;
	}


	private static final View NOT_ACCEPTABLE_VIEW = new View() {

		@Override
		@Nullable
		public String getContentType() {
			return null;
		}

		@Override
		public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
			response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
		}
	};

}
