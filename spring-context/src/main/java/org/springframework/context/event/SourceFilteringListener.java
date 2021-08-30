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

package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * 实现 GenericApplicationListener、SmartApplicationListener 监听器，
 * 实现将原始对象触发的事件，转发给指定监听器。
 */
public class SourceFilteringListener implements GenericApplicationListener, SmartApplicationListener {

	/**
	 * 原始类
	 */
	private final Object source;

	/**
	 * 代理的监听器
	 */
	@Nullable
	private GenericApplicationListener delegate;


	/**
	 * Create a SourceFilteringListener for the given event source.
	 * @param source the event source that this listener filters for,
	 * only processing events from this source
	 * @param delegate the delegate listener to invoke with event
	 * from the specified source
	 */
	public SourceFilteringListener(Object source, ApplicationListener<?> delegate) {
		this.source = source;
		this.delegate = (delegate instanceof GenericApplicationListener ?
				(GenericApplicationListener) delegate : new GenericApplicationListenerAdapter(delegate));
	}

	/**
	 * Create a SourceFilteringListener for the given event source,
	 * expecting subclasses to override the {@link #onApplicationEventInternal}
	 * method (instead of specifying a delegate listener).
	 * @param source the event source that this listener filters for,
	 * only processing events from this source
	 */
	protected SourceFilteringListener(Object source) {
		this.source = source;
	}


	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event.getSource() == this.source) {
			onApplicationEventInternal(event);
		}
	}

	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		return (this.delegate == null || this.delegate.supportsEventType(eventType));
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return supportsEventType(ResolvableType.forType(eventType));
	}

	@Override
	public boolean supportsSourceType(@Nullable Class<?> sourceType) {
		return (sourceType != null && sourceType.isInstance(this.source));
	}

	@Override
	public int getOrder() {
		return (this.delegate != null ? this.delegate.getOrder() : Ordered.LOWEST_PRECEDENCE);
	}


	/**
	 * Actually process the event, after having filtered according to the
	 * desired event source already.
	 * <p>The default implementation invokes the specified delegate, if any.
	 * @param event the event to process (matching the specified source)
	 */
	protected void onApplicationEventInternal(ApplicationEvent event) {
		if (this.delegate == null) {
			throw new IllegalStateException(
					"Must specify a delegate object or override the onApplicationEventInternal method");
		}
		this.delegate.onApplicationEvent(event);
	}

}
