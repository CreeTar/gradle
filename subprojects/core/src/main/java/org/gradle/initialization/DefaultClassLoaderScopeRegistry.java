/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.initialization;

import org.gradle.api.internal.initialization.ClassLoaderScope;
import org.gradle.api.internal.initialization.RootClassLoaderScope;
import org.gradle.api.internal.initialization.loadercache.ClassLoaderCache;

public class DefaultClassLoaderScopeRegistry implements ClassLoaderScopeRegistry {

    public static final String CORE_NAME = "core";
    public static final String CORE_AND_PLUGINS_NAME = "coreAndPlugins";

    private final ClassLoaderScope coreAndPluginsScope;
    private final ClassLoaderScope coreScope;

    public DefaultClassLoaderScopeRegistry(ClassLoaderRegistry loaderRegistry, ClassLoaderCache classLoaderCache, ClassLoaderScopeRegistryListener listener) {
        this.coreScope = new RootClassLoaderScope(CORE_NAME, loaderRegistry.getRuntimeClassLoader(), loaderRegistry.getGradleCoreApiClassLoader(), classLoaderCache, listener);
        this.coreAndPluginsScope = new RootClassLoaderScope(CORE_AND_PLUGINS_NAME, loaderRegistry.getPluginsClassLoader(), loaderRegistry.getGradleApiClassLoader(), classLoaderCache, listener);
        rootScopesCreated(listener);
    }

    @Override
    public ClassLoaderScope getCoreAndPluginsScope() {
        return coreAndPluginsScope;
    }

    @Override
    public ClassLoaderScope getCoreScope() {
        return coreScope;
    }

    private void rootScopesCreated(ClassLoaderScopeRegistryListener listener) {
        listener.rootScopeCreated(CORE_NAME);
        listener.rootScopeCreated(CORE_AND_PLUGINS_NAME);
    }
}
