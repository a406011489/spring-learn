/*
 * Copyright 2002-2014 the original author or authors.
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
 * FlashMap 管理器接口，负责重定向时，保存参数到临时存储中。
 *
 * 默认情况下，这个临时存储会是 Session 。
 * 也就是说：重定向前，保存参数到 Seesion 中。重定向后，从 Session 中获得参数，并移除。
 *
 * 当然，实际场景下，使用的非常少，特别是前后端分离之后。
 */
public interface FlashMapManager {

	/**
	 * 恢复参数，并将恢复过的和超时的参数从保存介质中删除
	 */
	@Nullable
	FlashMap retrieveAndUpdate(HttpServletRequest request, HttpServletResponse response);

	/**
	 * 将参数保存起来
	 */
	void saveOutputFlashMap(FlashMap flashMap, HttpServletRequest request, HttpServletResponse response);

}
