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

import com.google.auto.service.AutoService;
import com.google.common.collect.Multimaps;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import com.markelliot.barista.annotations.Param;
import com.markelliot.barista.authz.VerifiedAuthToken;
import com.markelliot.barista.endpoints.HttpRedirect;
import com.markelliot.barista.processor.EndpointHandlerGenerator.EndpointHandlerDefinition;
import com.markelliot.barista.processor.EndpointHandlerGenerator.EndpointHandlerDefinition.HttpMethod;
import com.markelliot.barista.processor.EndpointHandlerGenerator.EndpointHandlerDefinition.ReturnType;
import com.markelliot.barista.processor.EndpointHandlerGenerator.ParameterDefinition;
import com.markelliot.barista.processor.EndpointHandlerGenerator.ParameterDefinition.ParamType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@AutoService(Processor.class)
@SupportedAnnotationTypes({
    "com.markelliot.barista.annotations.Http.Get",
    "com.markelliot.barista.annotations.Http.Post",
    "com.markelliot.barista.annotations.Http.Put",
    "com.markelliot.barista.annotations.Http.Delete",
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public final class EndpointHandlerProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            if (!roundEnv.processingOver()) {
                Set<JavaFile> filesFromRound = processImpl(annotations, roundEnv);
                for (JavaFile jf : filesFromRound) {
                    String output = format(jf);
                    writeTo(jf.packageName, jf.typeSpec, output, processingEnv.getFiler());
                }
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            error("An error occurred during annotation processing: " + sw, null);
        }
        return false;
    }

    /**
     * Formats the provided JavaFile, or, if the formatter throws an exception, returns the
     * JavaFile's default format.
     */
    private String format(JavaFile jf) {
        String javaFileAsString = jf.toString();
        try {
            return new Formatter(JavaFormatterOptions.builder().style(Style.AOSP).build())
                    .formatSourceAndFixImports(javaFileAsString);
        } catch (Exception e) {
            // if the formatter throws an exception, opt to output code anyway
            return javaFileAsString;
        }
    }

    // Copied from JavaFile
    public void writeTo(String packageName, TypeSpec typeSpec, String formattedSource, Filer filer)
            throws IOException {
        String fileName = packageName.isEmpty() ? typeSpec.name : packageName + "." + typeSpec.name;
        List<Element> originatingElements = typeSpec.originatingElements;
        JavaFileObject filerSourceFile =
                filer.createSourceFile(fileName, originatingElements.toArray(new Element[0]));
        try (Writer writer = filerSourceFile.openWriter()) {
            writer.write(formattedSource);
        } catch (Exception e) {
            try {
                filerSourceFile.delete();
            } catch (Exception ignored) {
            }
            throw e;
        }
    }

    public Set<JavaFile> processImpl(
            Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<EndpointHandlerDefinition> definitions = new LinkedHashSet<>();
        for (Element element :
                roundEnv.getElementsAnnotatedWithAny(AnnotationHelpers.endpointAnnotations())) {
            if (element.getKind() != ElementKind.METHOD) {
                error("Authed.Get is only applicable for methods", element);
            } else {
                ExecutableElement methodElement = (ExecutableElement) element;
                definitions.add(
                        processMethod(
                                methodElement,
                                AnnotationHelpers.httpMethod(methodElement),
                                AnnotationHelpers.path(methodElement)));

                // TODO(markelliot): lint method parameters
            }
        }

        Set<JavaFile> filesFromRound = new LinkedHashSet<>();
        Multimaps.index(definitions, EndpointHandlerDefinition::className)
                .asMap()
                .forEach(
                        (resource, handlers) ->
                                filesFromRound.add(
                                        EndpointHandlerGenerator.generate(resource, handlers)));
        return filesFromRound;
    }

    private EndpointHandlerDefinition processMethod(
            ExecutableElement methodElement, HttpMethod httpMethod, String path) {
        TypeElement classElement = (TypeElement) methodElement.getEnclosingElement();
        TypeName returnType = ClassName.get(methodElement.getReturnType());
        Set<String> pathParamNames = pathParamNames(path);
        Set<ParameterDefinition> parameters = new LinkedHashSet<>();
        for (VariableElement paramElement : methodElement.getParameters()) {
            String paramName = paramElement.getSimpleName().toString();
            TypeName paramClass = ClassName.get(paramElement.asType());
            if (pathParamNames.contains(paramName)) {
                parameters.add(
                        new ParameterDefinition(paramName, paramName, paramClass, ParamType.PATH));
            } else if (paramElement.getAnnotation(Param.Query.class) != null) {
                Param.Query queryParam = paramElement.getAnnotation(Param.Query.class);
                parameters.add(
                        new ParameterDefinition(
                                paramName,
                                queryParam.value() != null ? queryParam.value() : paramName,
                                paramClass,
                                ParamType.QUERY));
            } else if (paramElement.getAnnotation(Param.Cookie.class) != null) {
                Param.Cookie cookie = paramElement.getAnnotation(Param.Cookie.class);
                parameters.add(
                        new ParameterDefinition(
                                paramName,
                                cookie.value() != null ? cookie.value() : paramName,
                                paramClass,
                                ParamType.COOKIE));
            } else if (paramElement.getAnnotation(Param.Header.class) != null) {
                Param.Header header = paramElement.getAnnotation(Param.Header.class);
                parameters.add(
                        new ParameterDefinition(
                                paramName,
                                header.value() != null ? header.value() : paramName,
                                paramClass,
                                ParamType.HEADER));
            } else if (ClassName.get(VerifiedAuthToken.class).equals(paramClass)) {
                parameters.add(
                        new ParameterDefinition(paramName, null, paramClass, ParamType.TOKEN));
            } else {
                // must be a body parameter
                parameters.add(
                        new ParameterDefinition(paramName, null, paramClass, ParamType.BODY));
            }
        }

        return new EndpointHandlerDefinition(
                ClassName.get(classElement),
                methodElement.getSimpleName().toString(),
                path,
                httpMethod,
                parameters,
                toReturnType(returnType));
    }

    private static Set<String> pathParamNames(String route) {
        Set<String> names = new LinkedHashSet<>();
        int start = 0;
        int index = -1;
        while ((index = route.indexOf('{', start)) != -1) {
            int endIndex = route.indexOf('}', index + 1);
            names.add(route.substring(index + 1, endIndex));
            start = endIndex + 1;
        }
        return names;
    }

    private static ReturnType toReturnType(TypeName returnType) {
        if (returnType.equals(ClassName.get(HttpRedirect.class))) {
            return ReturnType.REDIRECT;
        }
        if (returnType.equals(ClassName.VOID)) {
            return ReturnType.EMPTY;
        }
        if (returnType.equals(ClassName.get(InputStream.class))) {
            return ReturnType.BYTE_STREAM;
        }
        // everything else is an opaque object type
        return ReturnType.OBJECT;
    }

    private void error(String message, Element element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
