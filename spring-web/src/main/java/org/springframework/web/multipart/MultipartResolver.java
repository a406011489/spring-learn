/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.multipart;

import javax.servlet.http.HttpServletRequest;

/**
 * 内容类型( Content-Type )为 multipart/* 的请求的解析器接口。
 *
 * 例如：
 * 文件上传请求，MultipartResolver 会将 HttpServletRequest 封装成 MultipartHttpServletRequest ，
 * 这样从 MultipartHttpServletRequest 中获得上传的文件。
 *
 * MultipartResolver ⽤于上传请求，通过将普通的请求包装成 MultipartHttpServletRequest 来实现。
 * MultipartHttpServletRequest 可以通过 getFile() ⽅法 直接获得⽂件。如果上传多个⽂件，
 * 还可以调⽤ getFileMap()⽅法得到Map<FileName，File>这样的结构，MultipartResolver 的作用就是封装普通的请求，使其拥有⽂件上传的功能。
 */
public interface MultipartResolver {

	/**
	 * 是否为 multipart 请求
	 */
	boolean isMultipart(HttpServletRequest request);

	/**
	 * 将 HttpServletRequest 请求封装成 MultipartHttpServletRequest 对象
	 */
	MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException;

	/**
	 * 清理处理 multipart 产生的资源，例如临时文件
	 *
	 */
	void cleanupMultipart(MultipartHttpServletRequest request);

}
