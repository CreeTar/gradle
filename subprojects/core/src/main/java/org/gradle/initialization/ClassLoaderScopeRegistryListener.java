/*
 * Copyright 2019 the original author or authors.
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

import org.gradle.internal.classpath.ClassPath;


/**
 * Listens to changes to the ClassLoaderScope tree.
 *
 * Must be registered with the {@link org.gradle.internal.event.ListenerManager} during
 * a {@link org.gradle.internal.service.scopes.BuildScopeListenerManagerAction}.
 *
 * @see ClassLoaderScopeRegistry
 * @see org.gradle.api.internal.initialization.ClassLoaderScope
 */
public interface ClassLoaderScopeRegistryListener {

    void rootScopeCreated(String scopeId);

    void childScopeCreated(String parentId, String childId);

    void localClasspathAdded(String scopeId, ClassPath localClassPath);

    void exportClasspathAdded(String scopeId, ClassPath exportClassPath);

    ClassLoaderScopeRegistryListener NULL = new ClassLoaderScopeRegistryListener() {
        @Override
        public void rootScopeCreated(String scopeId) {
        }

        @Override
        public void childScopeCreated(String parentId, String childId) {
        }

        @Override
        public void localClasspathAdded(String scopeId, ClassPath localClassPath) {
        }

        @Override
        public void exportClasspathAdded(String scopeId, ClassPath exportClassPath) {
        }
    };
}
