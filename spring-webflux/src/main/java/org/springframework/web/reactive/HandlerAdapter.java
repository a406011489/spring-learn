/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive;

import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;

/**
 * HandlerAdapter是⼀个适配器。因为Spring MVC中Handler可以是任意形式的，只要能处理请求即可。
 * 但是把请求交给 Servlet 的时候，由于 Servlet 的⽅法结构都是doService(HttpServletRequest req,HttpServletResponse resp)形式的，
 * 要让固定的Servlet处理方法调用 Handler 来进⾏处理，便是 HandlerAdapter 的职责。
 *
 * 比如用户处理器可以实现 Controller 接口，
 * 也可以用 @RequestMapping 注解将方法作为一个处理器等，
 * 这就导致 Spring 不知道怎么调用用户的处理器逻辑。
 * 所以这里需要一个处理器适配器，由处理器适配器去调用处理器的逻辑。
 */
public interface HandlerAdapter {

	/**
	 * Whether this {@code HandlerAdapter} supports the given {@code handler}.
	 * @param handler the handler object to check
	 * @return whether or not the handler is supported
	 */
	boolean supports(Object handler);

	/**
	 * Handle the request with the given handler.
	 * <p>Implementations are encouraged to handle exceptions resulting from the
	 * invocation of a handler in order and if necessary to return an alternate
	 * result that represents an error response.
	 * <p>Furthermore since an async {@code HandlerResult} may produce an error
	 * later during result handling implementations are also encouraged to
	 * {@link HandlerResult#setExceptionHandler(Function) set an exception
	 * handler} on the {@code HandlerResult} so that may also be applied later
	 * after result handling.
	 * @param exchange current server exchange
	 * @param handler the selected handler which must have been previously
	 * checked via {@link #supports(Object)}
	 * @return {@link Mono} that emits a single {@code HandlerResult} or none if
	 * the request has been fully handled and doesn't require further handling.
	 */
	Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler);

}
