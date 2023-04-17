/*
 * (c) Copyright 2022 Mark Elliot. All rights reserved.
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

package com.markelliot.barista.processor;

import com.google.common.collect.ImmutableSet;
import com.markelliot.barista.annotations.Http;
import com.markelliot.barista.processor.EndpointHandlerGenerator.EndpointHandlerDefinition.HttpMethod;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;

public final class AnnotationHelpers {

    record EndpointAnnotation(
            Class<? extends Annotation> annontationClass, HttpMethod httpMethod, Function<Annotation, String> pathFn) {}

    private static final Set<EndpointAnnotation> ENDPOINT_ANNOTATIONS = ImmutableSet.of(
            new EndpointAnnotation(Http.Get.class, HttpMethod.GET, a -> ((Http.Get) a).value()),
            new EndpointAnnotation(Http.Post.class, HttpMethod.POST, a -> ((Http.Post) a).value()),
            new EndpointAnnotation(Http.Put.class, HttpMethod.PUT, a -> ((Http.Put) a).value()),
            new EndpointAnnotation(Http.Delete.class, HttpMethod.DELETE, a -> ((Http.Delete) a).value()));

    private static Set<Class<? extends Annotation>> ENDPOINT_ANNOTATION_CLASSES = ENDPOINT_ANNOTATIONS.stream()
            .map(EndpointAnnotation::annontationClass)
            .collect(Collectors.toSet());

    public static Set<Class<? extends Annotation>> endpointAnnotations() {
        return ENDPOINT_ANNOTATION_CLASSES;
    }

    public static HttpMethod httpMethod(ExecutableElement method) {
        for (EndpointAnnotation annotation : ENDPOINT_ANNOTATIONS) {
            Annotation inst = method.getAnnotation(annotation.annontationClass());
            if (inst != null) {
                return annotation.httpMethod();
            }
        }
        throw new IllegalStateException("Invariant violation: No matching annotations.");
    }

    public static String path(ExecutableElement method) {
        for (EndpointAnnotation annotation : ENDPOINT_ANNOTATIONS) {
            Annotation inst = method.getAnnotation(annotation.annontationClass());
            if (inst != null) {
                return annotation.pathFn().apply(inst);
            }
        }
        throw new IllegalStateException("Invariant violation: No matching annotations.");
    }
}
