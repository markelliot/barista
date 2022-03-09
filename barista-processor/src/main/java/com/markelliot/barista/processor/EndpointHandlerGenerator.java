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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.markelliot.barista.HttpMethod;
import com.markelliot.barista.SerDe.ByteRepr;
import com.markelliot.barista.authz.VerifiedAuthToken;
import com.markelliot.barista.endpoints.EndpointHandler;
import com.markelliot.barista.endpoints.EndpointRuntime;
import com.markelliot.barista.endpoints.Endpoints;
import com.markelliot.barista.endpoints.HttpError;
import com.markelliot.barista.processor.EndpointHandlerGenerator.ParameterDefinition.ParamType;
import com.markelliot.result.Result;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

public final class EndpointHandlerGenerator {

    public static JavaFile generate(
            ClassName className, Collection<EndpointHandlerDefinition> handlers) {
        ClassName resourceClassName = endpointsClassName(className);
        TypeSpec resourceClass =
                TypeSpec.classBuilder(resourceClassName)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addSuperinterface(ClassName.get(Endpoints.class))
                        .addField(className, "delegate", Modifier.PRIVATE, Modifier.FINAL)
                        .addMethod(
                                MethodSpec.constructorBuilder()
                                        .addModifiers(Modifier.PUBLIC)
                                        .addParameter(className, "delegate")
                                        .addStatement("this.$N = $N", "delegate", "delegate")
                                        .build())
                        .addMethod(generateEndpointsMethod(className, handlers))
                        .addTypes(
                                handlers.stream()
                                        .map(h -> generateEndpointHandlers(className, h))
                                        .collect(Collectors.toList()))
                        .build();
        return JavaFile.builder(className.packageName(), resourceClass).build();
    }

    private static MethodSpec generateEndpointsMethod(
            ClassName className, Collection<EndpointHandlerDefinition> handlers) {
        return MethodSpec.methodBuilder("endpoints")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(ImmutableSet.class, EndpointHandler.class))
                .addStatement(
                        "return $T.of($L)",
                        ImmutableSet.class,
                        CodeBlock.join(
                                handlers.stream()
                                        .map(h -> endpointHandlerClassName(className, h))
                                        .map(cn -> CodeBlock.of("new $T(delegate)", cn))
                                        .collect(Collectors.toList()),
                                ", "))
                .build();
    }

    private static TypeSpec generateEndpointHandlers(
            ClassName className, EndpointHandlerDefinition definition) {
        return TypeSpec.classBuilder(endpointHandlerClassName(className, definition))
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                .addSuperinterface(ClassName.get(EndpointHandler.class))
                .addField(className, "delegate", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(
                        MethodSpec.constructorBuilder()
                                .addParameter(className, "delegate")
                                .addStatement("this.$N = $N", "delegate", "delegate")
                                .build())
                .addMethod(
                        MethodSpec.methodBuilder("method")
                                .addAnnotation(Override.class)
                                .addModifiers(Modifier.PUBLIC)
                                .returns(ClassName.get(HttpMethod.class))
                                .addStatement(
                                        "return $T.$N",
                                        HttpMethod.class,
                                        definition.httpMethod().toString())
                                .build())
                .addMethod(
                        MethodSpec.methodBuilder("route")
                                .addAnnotation(Override.class)
                                .addModifiers(Modifier.PUBLIC)
                                .returns(ClassName.get(String.class))
                                .addStatement("return $S", definition.route())
                                .build())
                .addMethod(
                        MethodSpec.methodBuilder("handler")
                                .addAnnotation(Override.class)
                                .addModifiers(Modifier.PUBLIC)
                                .addParameter(EndpointRuntime.class, "runtime")
                                .returns(ClassName.get("io.undertow.server", "HttpHandler"))
                                .addCode(
                                        CodeBlock.builder()
                                                .beginControlFlow("return exchange ->")
                                                .add(generateHttpHandler(definition))
                                                .endControlFlow()
                                                // because we're returning a lambda and want a
                                                // terminating ";"
                                                .addStatement("")
                                                .build())
                                .build())
                .build();
    }

    private static CodeBlock generateHttpHandler(EndpointHandlerDefinition definition) {
        CodeBlock returnStatement =
                switch (definition.returnType()) {
                    case BYTE_STREAM -> throw new IllegalStateException("Unsupported");
                    case EMPTY, OBJECT -> CodeBlock.builder()
                            .addStatement(
                                    "$N.handle(() -> $N.$N($L), $N)",
                                    "runtime",
                                    "delegate",
                                    definition.methodName(),
                                    argumentList(definition),
                                    "exchange")
                            .build();
                    case REDIRECT -> CodeBlock.builder()
                            .addStatement(
                                    "$N.redirect(() -> $N.$N($L), $N)",
                                    "runtime",
                                    "delegate",
                                    definition.methodName(),
                                    argumentList(definition),
                                    "exchange")
                            .build();
                };

        CodeBlock.Builder handler = CodeBlock.builder();

        // add auth block
        definition.parameters().stream()
                .filter(p -> p.type() == ParamType.TOKEN)
                .findAny()
                .ifPresent(authParam -> handler.add(authBlock(authParam.argumentName())));

        // handle the rest of the parameters
        definition.parameters().stream()
                .filter(p -> p.type() != ParamType.TOKEN)
                .filter(p -> p.type() != ParamType.BODY)
                .forEach(param -> handler.add(paramBlock(param)));

        // TODO(markelliot): add code that checks required parameters are present

        // produce result; when body is present we need to stitch in a requestReceiver
        definition.parameters.stream()
                .filter(p -> p.type() == ParamType.BODY)
                .findAny()
                .ifPresentOrElse(
                        bodyParam ->
                                handler.add(
                                        CodeBlock.builder()
                                                .beginControlFlow(
                                                        "$N.getRequestReceiver().receiveFullString((bodyExchange, body_) ->",
                                                        "exchange")
                                                .addStatement(
                                                        "$T $N = $N.serde().deserialize(new $T(body_), $T.class)",
                                                        bodyParam.className(),
                                                        bodyParam.argumentName(),
                                                        "runtime",
                                                        ByteRepr.class,
                                                        bodyParam.className())
                                                .add(returnStatement)
                                                .endControlFlow(")")
                                                .build()),
                        () -> handler.add(returnStatement));
        return handler.build();
    }

    private static CodeBlock authBlock(String authParamName) {
        return CodeBlock.builder()
                .addStatement(
                        "$T<$T, $T> $N = $N.verifyAuth($N)",
                        Result.class,
                        VerifiedAuthToken.class,
                        HttpError.class,
                        authParamName,
                        "runtime",
                        "exchange")
                .beginControlFlow("if ($N.isError())", authParamName)
                .addStatement(
                        "$N.error($N.error().get(), $N)", "runtime", authParamName, "exchange")
                .addStatement("return")
                .endControlFlow()
                .build();
    }

    private static CodeBlock paramBlock(ParameterDefinition param) {
        String methodName =
                switch (param.type()) {
                    case PATH -> "pathParameter";
                    case QUERY -> "queryParameter";
                    case HEADER -> "headerParameter";
                    case COOKIE -> "cookieParameter";
                    default -> throw new IllegalStateException(
                            "Processor invariant failed due to programmer error");
                };
        return CodeBlock.builder()
                .addStatement(
                        "$T $N = $N.$N($S, $N)",
                        isOptional(param.className())
                                ? param.className()
                                : ParameterizedTypeName.get(
                                        ClassName.get(Optional.class), param.className()),
                        param.argumentName(),
                        "runtime",
                        methodName,
                        param.httpName(),
                        "exchange")
                .build();
    }

    private static CodeBlock argumentList(EndpointHandlerDefinition definition) {
        return CodeBlock.join(
                definition.parameters().stream()
                        .map(
                                param ->
                                        switch (param.type()) {
                                            case TOKEN -> CodeBlock.of(
                                                    "$N.unwrap()", param.argumentName());
                                            case BODY -> CodeBlock.of("$N", param.argumentName());
                                            default -> isOptional(param.className())
                                                    ? CodeBlock.of("$N", param.argumentName())
                                                    : CodeBlock.of(
                                                            "$N.get()", param.argumentName());
                                        })
                        .collect(Collectors.toList()),
                ", ");
    }

    private static boolean isOptional(TypeName name) {
        return (name instanceof ParameterizedTypeName className)
                && className.rawType.equals(ClassName.get(Optional.class));
    }

    private static ClassName endpointsClassName(ClassName className) {
        return ClassName.get(className.packageName(), className.simpleName() + "Endpoints");
    }

    private static ClassName endpointHandlerClassName(
            ClassName className, EndpointHandlerDefinition definition) {
        return endpointsClassName(className)
                .nestedClass(ucfirst(definition.methodName()) + "EndpointHandler");
    }

    private static String ucfirst(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public record EndpointHandlerDefinition(
            ClassName className,
            String methodName,
            String route,
            HttpMethod httpMethod,
            Set<ParameterDefinition> parameters,
            ReturnType returnType) {

        public EndpointHandlerDefinition {
            Preconditions.checkArgument(
                    parameters.stream().filter(p -> p.type().equals(ParamType.BODY)).count() <= 1,
                    "At most one body-type parameter allowed");
        }

        enum HttpMethod {
            GET,
            PUT,
            POST,
            DELETE
        }

        enum ReturnType {
            EMPTY,
            OBJECT,
            BYTE_STREAM,
            REDIRECT,
            //RESULT
        }
    }

    public record ParameterDefinition(
            String argumentName, String httpName, TypeName className, ParamType type) {
        enum ParamType {
            BODY,
            PATH,
            QUERY,
            COOKIE,
            HEADER,
            TOKEN
        }
    }
}
