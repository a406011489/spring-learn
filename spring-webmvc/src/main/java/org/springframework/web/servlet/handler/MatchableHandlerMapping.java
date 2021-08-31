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

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 定义判断请求和指定 pattern 路径是否匹配的接口方法。
 */
public interface MatchableHandlerMapping extends HandlerMapping {

	/**
	 * Return the parser of this {@code HandlerMapping}, if configured in which
	 * case pre-parsed patterns are used.
	 * @since 5.3
	 */
	@Nullable
	default PathPatternParser getPatternParser() {
		return null;
	}

	/**
	 * 判断请求和指定 `pattern` 路径是否匹配的接口方法
	 */
	@Nullable
	RequestMatchResult match(HttpServletRequest request, String pattern);

}
