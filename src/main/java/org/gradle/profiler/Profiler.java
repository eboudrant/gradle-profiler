/*
 * Copyright 2003-2012 the original author or authors.
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
package org.gradle.profiler;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.gradle.profiler.bs.BuildScanController;
import org.gradle.profiler.hp.HonestProfilerArgs;
import org.gradle.profiler.hp.HonestProfilerControl;
import org.gradle.profiler.hp.HonestProfilerJvmArgsCalculator;
import org.gradle.profiler.jfr.JFRArgs;
import org.gradle.profiler.jfr.JFRControl;
import org.gradle.profiler.jfr.JFRJvmArgsCalculator;

import java.io.File;
import java.io.IOException;

public enum Profiler {
    none,
    jfr {
        @Override
        public ProfilerController newController(final String pid, final InvocationSettings settings, final BuildInvoker invoker) {
            JFRArgs jfrArgs = new JFRArgs(pid, new File(settings.getOutputDir(), "profile.jfr"));
            return new JFRControl(jfrArgs);
        }

        @Override
        public JvmArgsCalculator newJvmArgsCalculator(InvocationSettings settings) {
            return new JFRJvmArgsCalculator();
        }
    },
    hp {
        @Override
        public Object newConfigObject(final OptionSet parsedOptions) {
            File tmpLog = new File(System.getProperty("java.io.tmpdir"), "hp.log");
            int i = 0;
            while (tmpLog.exists()) {
                tmpLog = new File(System.getProperty("java.io.tmpdir"), "hp.log."+(++i));
            }
            HonestProfilerArgs args = new HonestProfilerArgs(
                    new File((String) parsedOptions.valueOf("hp-home")),
                    tmpLog,
                    Integer.valueOf((String) parsedOptions.valueOf("hp-port")),
                    Integer.valueOf((String) parsedOptions.valueOf("hp-interval"))
            );
            return args;
        }

        @Override
        public ProfilerController newController(final String pid, final InvocationSettings settings, final BuildInvoker invoker) {
            HonestProfilerArgs args = (HonestProfilerArgs) settings.getProfilerOptions();
            return new HonestProfilerControl(args, settings.getOutputDir());
        }

        @Override
        public JvmArgsCalculator newJvmArgsCalculator(InvocationSettings settings) {
            return new HonestProfilerJvmArgsCalculator((HonestProfilerArgs) settings.getProfilerOptions());
        }

        @Override
        public void addOptions(final OptionParser parser) {
            parser.accepts("hp-port", "Honest Profiler port")
                    .availableIf("profile")
                    .withOptionalArg()
                    .defaultsTo("18000");
            parser.accepts("hp-home", "Honest Profiler home directory")
                    .availableIf("profile")
                    .withOptionalArg()
                    .defaultsTo(System.getenv().getOrDefault("HP_HOME_DIR", ""));
            parser.accepts("hp-interval", "Honest Profiler sampling interval")
                    .availableIf("profile")
                    .withOptionalArg()
                    .defaultsTo("7");
        }
    },
    buildscan {
        @Override
        public ProfilerController newController(final String pid, final InvocationSettings settings, final BuildInvoker invoker) {
            try {
                return new BuildScanController(invoker, (String) settings.getProfilerOptions());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object newConfigObject(final OptionSet parsedOptions) {
            return parsedOptions.valueOf("buildscan-version");
        }

        @Override
        void addOptions(final OptionParser parser) {
            parser.accepts("buildscan-version", "Version of the Build Scan plugin")
                    .availableIf("profile")
                    .withOptionalArg();
        }
    };

    public ProfilerController newController(final String pid, final InvocationSettings settings, final BuildInvoker invoker) {
        return ProfilerController.EMPTY;
    }

    public JvmArgsCalculator newJvmArgsCalculator(final InvocationSettings settings) {
        return JvmArgsCalculator.DEFAULT;
    }

    public Object newConfigObject(OptionSet parsedOptions) {
        return null;
    }

    void addOptions(OptionParser parser) {

    }

    static void configureParser(OptionParser parser) {
        for (Profiler profiler : values()) {
            profiler.addOptions(parser);
        }
    }
}
