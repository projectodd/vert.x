/*
 * Copyright 2011-2012 the original author or authors.
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

package org.vertx.java.deploy.impl.dynjs;

import java.io.File;
import java.io.FileNotFoundException;

import org.dynjs.Config;
import org.dynjs.exception.ThrowException;
import org.dynjs.runtime.AbstractNativeFunction;
import org.dynjs.runtime.DynJS;
import org.dynjs.runtime.ExecutionContext;
import org.dynjs.runtime.GlobalObject;
import org.dynjs.runtime.GlobalObjectFactory;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.deploy.Verticle;
import org.vertx.java.deploy.impl.ModuleClassLoader;
import org.vertx.java.deploy.impl.VerticleFactory;
import org.vertx.java.deploy.impl.VerticleManager;

/**
 * @author Lance Ball lball@redhat.com
 */
public class DynJSVerticleFactory implements VerticleFactory {

    private VerticleManager mgr;
    private ModuleClassLoader mcl;

    public DynJSVerticleFactory() {
    }

    @Override
    public void init(VerticleManager mgr, ModuleClassLoader mcl) {
        this.mgr = mgr;
        this.mcl = mcl;
    }

    @Override
    public Verticle createVerticle(String main) throws Exception {
        Verticle app = new DynJSVerticle(main);
        return app;
    }

    @Override
    public void reportException(Throwable t) {

        Logger logger = mgr.getLogger();

        if (t instanceof ThrowException) {
            ThrowException je = (ThrowException) t;

            logger.error("Exception in DynJS JavaScript verticle:\n"
                    + je.getLocalizedMessage() +
                    "\n" + je.getStackTrace());
        } else {
            logger.error("Exception in DynJS JavaScript verticle", t);
        }
    }


    private class DynJSVerticle extends Verticle {

        private final String scriptName;

        DynJSVerticle(String scriptName) {
            this.scriptName = scriptName;
        }

        @Override
        public void start() throws Exception {
            Config config = new Config();
            config.setGlobalObjectFactory(new GlobalObjectFactory() {
                @Override
                public GlobalObject newGlobalObject(DynJS runtime) {
                    final GlobalObject globalObject = new GlobalObject(runtime);
                    globalObject.defineGlobalProperty("__dirname", System.getProperty("user.dir"));
                    globalObject.defineGlobalProperty("load", new AbstractNativeFunction(globalObject) {
                        @Override
                        public Object call(ExecutionContext context, Object self, Object... args) {                            
                            return loadScript(context.getGlobalObject().getRuntime(), (String) args[0]);
                        }
                    });
                    return globalObject;
                }
            });
            DynJS runtime = new DynJS(config);
            loadScript(runtime, this.scriptName);
        }

        public Object loadScript(DynJS runtime, String scriptName) {
            File scriptFile = new File(scriptName);
            if (scriptFile.exists()) {
                try {
                    return runtime.newRunner().withSource(scriptFile).execute();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        public void stop() throws Exception {
            // What should go here?
        }
    }


    @Override
    public void close() {
    }
}
