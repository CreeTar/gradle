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

package org.gradle.instantexecution.serialization.codecs

import org.gradle.api.internal.artifacts.transform.ConsumerProvidedVariantFiles
import org.gradle.api.internal.file.FileCollectionFactory
import org.gradle.api.internal.file.FileCollectionInternal
import org.gradle.api.internal.file.FileCollectionStructureVisitor
import org.gradle.api.internal.file.FileTreeInternal
import org.gradle.api.internal.file.collections.MinimalFileSet
import org.gradle.api.tasks.util.PatternSet
import org.gradle.instantexecution.serialization.Codec
import org.gradle.instantexecution.serialization.ReadContext
import org.gradle.instantexecution.serialization.WriteContext
import java.io.File


internal
class FileCollectionCodec(
    private val fileCollectionFactory: FileCollectionFactory
) : Codec<FileCollectionInternal> {

    override suspend fun WriteContext.encode(value: FileCollectionInternal) {
        runCatching {
            val visitor = CollectingVisitor()
            value.visitStructure(visitor)
            visitor.elements
        }.apply {
            onSuccess { elements ->
                writeBoolean(true)
                write(elements)
            }
            onFailure { ex ->
                writeBoolean(false)
                writeString(ex.message)
            }
        }
    }

    override suspend fun ReadContext.decode(): FileCollectionInternal =
        if (readBoolean()) fileCollectionFactory.fixed((read() as Collection<Any>).fold(mutableListOf<File>()) { list, element ->
            if (element is File) {
                list.add(element)
            }
            // Otherwise, ignore for now
            list
        })
        else fileCollectionFactory.create(ErrorFileSet(readString()))
}


private
class CollectingVisitor : FileCollectionStructureVisitor {
    val elements: MutableSet<Any> = mutableSetOf()

    override fun prepareForVisit(source: FileCollectionInternal.Source): FileCollectionStructureVisitor.VisitType {
        return if (source is ConsumerProvidedVariantFiles && source.scheduledNodes.isNotEmpty()) {
            // Visit the source only for scheduled transforms
            FileCollectionStructureVisitor.VisitType.NoContents
        } else {
            FileCollectionStructureVisitor.VisitType.Visit
        }
    }

    override fun visitCollection(source: FileCollectionInternal.Source, contents: Iterable<File>) {
        if (source is ConsumerProvidedVariantFiles) {
            elements.addAll(source.scheduledNodes)
        } else {
            elements.addAll(contents)
        }
    }

    override fun visitGenericFileTree(fileTree: FileTreeInternal) {
        // TODO - should serialize a spec for the tree instead of its current elements
        elements.addAll(fileTree)
    }

    override fun visitFileTree(root: File, patterns: PatternSet, fileTree: FileTreeInternal) {
        // TODO - should serialize a spec for the tree instead of its current elements
        elements.addAll(fileTree)
    }

    override fun visitFileTreeBackedByFile(file: File, fileTree: FileTreeInternal) {
        // TODO - should serialize a spec for the tree instead of its current elements
        elements.addAll(fileTree)
    }
}


private
class ErrorFileSet(private val error: String) : MinimalFileSet {

    override fun getDisplayName() =
        "error-file-collection"

    override fun getFiles() =
        throw Exception(error)
}
