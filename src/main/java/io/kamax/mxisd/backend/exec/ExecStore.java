/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxisd.backend.exec;

import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.config.ExecConfig;
import io.kamax.mxisd.exception.InternalServerError;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ExecStore {

    public static final String JsonType = "json";
    public static final String PlainType = "plain";

    private static final Logger log = LoggerFactory.getLogger(ExecStore.class);

    protected static String toJson(Object o) {
        return GsonUtil.get().toJson(o);
    }

    private final ExecConfig cfg;
    private Supplier<ProcessExecutor> executorSupplier = () -> new ProcessExecutor().readOutput(true);

    public ExecStore(ExecConfig cfg) {
        this.cfg = cfg;
    }

    public void setExecutorSupplier(Supplier<ProcessExecutor> supplier) {
        executorSupplier = supplier;
    }

    public class Processor<V> {

        private ExecConfig.Process cfg;

        private Supplier<Optional<String>> inputSupplier;
        private Function<String, String> inputTypeMapper;
        private Function<String, String> inputUnknownTypeMapper;
        private Map<String, Supplier<String>> inputTypeSuppliers;

        private Map<String, Function<ExecConfig.Token, String>> inputTypeTemplates;
        private Supplier<String> inputTypeNoTemplateHandler;
        private Map<String, Supplier<String>> tokenMappers;
        private Function<String, String> tokenHandler;

        private Consumer<ProcessResult> onExitHandler;
        private Consumer<ProcessResult> successHandler;
        private Map<String, Function<String, V>> successMappers;
        private Function<String, V> successDefault;
        private Consumer<ProcessResult> failureHandler;
        private Map<String, Function<String, V>> failureMappers;
        private Function<String, V> failureDefault;
        private Consumer<ProcessResult> unknownHandler;
        private Map<String, Function<String, V>> unknownMappers;
        private Function<String, V> unknownDefault;

        public Processor(ExecConfig.Process cfg) {
            this();
            withConfig(cfg);
        }

        public Processor() {
            tokenMappers = new HashMap<>();
            inputTypeSuppliers = new HashMap<>();
            inputTypeTemplates = new HashMap<>();

            withTokenHandler(tokenHandler = input -> {
                for (Map.Entry<String, Supplier<String>> entry : tokenMappers.entrySet()) {
                    input = input.replace(entry.getKey(), entry.getValue().get());
                }
                return input;
            });

            inputTypeNoTemplateHandler = () -> cfg.getInput().getType()
                    .map(type -> inputTypeTemplates.get(type).apply(cfg.getToken()))
                    .orElse("");

            inputUnknownTypeMapper = type -> tokenHandler.apply(cfg.getInput().getTemplate().orElseGet(inputTypeNoTemplateHandler));

            inputTypeMapper = type -> {
                if (!inputTypeSuppliers.containsKey(type)) {
                    return inputUnknownTypeMapper.apply(type);
                }

                return inputTypeSuppliers.get(type).get();
            };

            inputSupplier = () -> cfg.getInput().getType().map(type -> inputTypeMapper.apply(type));

            withExitHandler(pr -> {
            });

            successHandler = pr -> {
            };
            successMappers = new HashMap<>();
            successDefault = output -> {
                log.info("{} stdout: {}{}", cfg.getCommand(), System.lineSeparator(), output);
                throw new InternalServerError("Exec command has no success handler configured. This is a bug. Please report.");
            };

            failureHandler = pr -> {
            };
            failureMappers = new HashMap<>();
            failureDefault = output -> {
                log.info("{} stdout: {}{}", cfg.getCommand(), System.lineSeparator(), output);
                throw new InternalServerError("Exec command has no failure handler configured. This is a bug. Please report.");
            };

            unknownHandler = pr -> log.warn("Unexpected exit status: {}", pr.getExitValue());
            unknownMappers = new HashMap<>();
            withUnknownDefault(output -> {
                log.error("{} stdout:{}{}", cfg.getCommand(), System.lineSeparator(), output);
                throw new InternalServerError("Exec command returned with unexpected exit status");
            });
        }

        public void withConfig(ExecConfig.Process cfg) {
            this.cfg = cfg;
        }

        public void addTokenMapper(String token, Supplier<String> data) {
            tokenMappers.put(token, data);
        }

        public void withTokenHandler(Function<String, String> handler) {
            tokenHandler = handler;
        }

        public void addInput(String type, Supplier<String> handler) {
            inputTypeSuppliers.put(type, handler);
        }

        protected void addInputTemplate(String type, Function<ExecConfig.Token, String> template) {
            inputTypeTemplates.put(type, template);
        }

        public void addJsonInputTemplate(Function<ExecConfig.Token, Object> template) {
            inputTypeTemplates.put(JsonType, token -> GsonUtil.get().toJson(template.apply(token)));
        }

        public void withExitHandler(Consumer<ProcessResult> handler) {
            onExitHandler = handler;
        }

        public void withSuccessHandler(Consumer<ProcessResult> handler) {
            successHandler = handler;
        }

        public void addSuccessMapper(String type, Function<String, V> mapper) {
            successMappers.put(type, mapper);
        }

        public void withSuccessDefault(Function<String, V> mapper) {
            successDefault = mapper;
        }

        public void withFailureHandler(Consumer<ProcessResult> handler) {
            failureHandler = handler;
        }

        public void addFailureMapper(String type, Function<String, V> mapper) {
            failureMappers.put(type, mapper);
        }

        public void withFailureDefault(Function<String, V> mapper) {
            failureDefault = mapper;
        }

        public void addUnknownMapper(String type, Function<String, V> mapper) {
            unknownMappers.put(type, mapper);
        }

        public void withUnknownDefault(Function<String, V> mapper) {
            unknownDefault = mapper;
        }

        public V execute() {
            log.info("Executing {}", cfg.getCommand());

            try {
                ProcessExecutor psExec = executorSupplier.get();

                List<String> args = new ArrayList<>();
                args.add(tokenHandler.apply(cfg.getCommand()));
                args.addAll(cfg.getArgs().stream().map(arg -> tokenHandler.apply(arg)).collect(Collectors.toList()));
                psExec.command(args);

                psExec.environment(new HashMap<>(cfg.getEnv()).entrySet().stream()
                        .peek(e -> e.setValue(tokenHandler.apply(e.getValue())))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

                inputSupplier.get().ifPresent(input -> psExec.redirectInput(IOUtils.toInputStream(input, StandardCharsets.UTF_8)));

                ProcessResult psResult = psExec.execute();
                String output = psResult.outputUTF8();
                onExitHandler.accept(psResult);

                if (cfg.getExit().getSuccess().contains(psResult.getExitValue())) {
                    successHandler.accept(psResult);

                    return cfg.getOutput().getType()
                            .map(type -> successMappers.getOrDefault(type, successDefault).apply(output))
                            .orElseGet(() -> successDefault.apply(output));
                } else if (cfg.getExit().getFailure().contains(psResult.getExitValue())) {
                    failureHandler.accept(psResult);

                    return cfg.getOutput().getType()
                            .map(type -> failureMappers.getOrDefault(type, failureDefault).apply(output))
                            .orElseGet(() -> failureDefault.apply(output));
                } else {
                    unknownHandler.accept(psResult);

                    return cfg.getOutput().getType()
                            .map(type -> unknownMappers.getOrDefault(type, unknownDefault).apply(output))
                            .orElseGet(() -> unknownDefault.apply(output));
                }
            } catch (RuntimeException | IOException | InterruptedException | TimeoutException e) {
                log.error("Failed to execute {}", cfg.getCommand());
                log.debug("Internal exception:", e);
                throw new InternalServerError(e);
            }
        }

    }

}
