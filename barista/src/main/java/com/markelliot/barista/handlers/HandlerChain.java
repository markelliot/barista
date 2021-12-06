/*
 * (c) Copyright 2021 Mark Elliot. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.markelliot.barista.handlers;

import com.google.common.collect.Lists;
import io.undertow.server.HttpHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Creates a sequence of delegating handlers that apply in first-then..-last order.
 *
 * <p>This class is sugar for converting nested handlers into a more readable sequence of handler
 * constructors, terminating with the inner-most (and thus most-delegated-to) handler.
 *
 * <p>It's logically equivalent to construct sequences such as: <code>
 *   new FirstHandler(new SecondHandler(new ThirdHandler()))
 * </code> and <code>
 *   HandlerChain
 *      .of(FirstHandler::new)
 *      .then(SecondHandler::new)
 *      .last(new ThirdHandler())
 * </code>
 */
public final class HandlerChain {
    private final List<Function<HttpHandler, HttpHandler>> handlers = new ArrayList<>();

    private HandlerChain(Function<HttpHandler, HttpHandler> first) {
        handlers.add(first);
    }

    public static HandlerChain of(Function<HttpHandler, HttpHandler> first) {
        return new HandlerChain(first);
    }

    public HandlerChain then(Function<HttpHandler, HttpHandler> next) {
        handlers.add(next);
        return this;
    }

    public HttpHandler last(HttpHandler last) {
        HttpHandler current = last;
        for (Function<HttpHandler, HttpHandler> fn : Lists.reverse(handlers)) {
            current = fn.apply(current);
        }
        return current;
    }
}
