/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.test.fixtures.gradle

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode

@CompileStatic
@EqualsAndHashCode
class DependencySpec {
    String group
    String module
    String version
    String preferredVersion
    String strictVersion
    boolean forSubgraph
    List<String> rejects
    List<ExcludeSpec> exclusions = []
    String reason
    Map<String, Object> attributes
    List<CapabilitySpec> requestedCapabilities = []

    DependencySpec(String g, String m, String v, String preferredVersion, String strictVersion, Boolean forSubgraph, List<String> rejects, Collection<Map> excludes, String reason, Map<String, Object> attributes) {
        group = g
        module = m
        version = v
        this.preferredVersion = preferredVersion
        this.strictVersion = strictVersion
        this.forSubgraph = forSubgraph
        this.rejects = rejects?:Collections.<String>emptyList()
        if (excludes) {
            exclusions = excludes.collect { Map exclusion ->
                String group = exclusion.get('group')?.toString()
                String module = exclusion.get('module')?.toString()
                new ExcludeSpec(group, module)
            }
        }
        this.reason = reason
        this.attributes = attributes
    }

    DependencySpec attribute(String name, Object value) {
        attributes[name] = value
        this
    }

    DependencySpec exclude(String group, String module) {
        exclusions << new ExcludeSpec(group, module)
        this
    }

    DependencySpec requestedCapability(String group, String name, String version) {
        requestedCapabilities << new CapabilitySpec(group, name, version)
        this
    }
}
