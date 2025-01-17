/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.internal.execution.steps;

import com.google.common.collect.ImmutableSortedMap;
import org.apache.commons.io.FileUtils;
import org.gradle.caching.BuildCacheKey;
import org.gradle.caching.internal.command.BuildCacheCommandFactory;
import org.gradle.caching.internal.command.BuildCacheCommandFactory.LoadMetadata;
import org.gradle.caching.internal.controller.BuildCacheController;
import org.gradle.caching.internal.origin.OriginMetadata;
import org.gradle.internal.Try;
import org.gradle.internal.execution.CurrentSnapshotResult;
import org.gradle.internal.execution.ExecutionOutcome;
import org.gradle.internal.execution.IncrementalChangesContext;
import org.gradle.internal.execution.Step;
import org.gradle.internal.execution.UnitOfWork;
import org.gradle.internal.execution.caching.CachingState;
import org.gradle.internal.fingerprint.CurrentFileCollectionFingerprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

public class CacheStep implements Step<IncrementalChangesContext, CurrentSnapshotResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheStep.class);

    private final BuildCacheController buildCache;
    private final BuildCacheCommandFactory commandFactory;
    private final Step<? super IncrementalChangesContext, ? extends CurrentSnapshotResult> delegate;

    public CacheStep(
        BuildCacheController buildCache,
        BuildCacheCommandFactory commandFactory,
        Step<? super IncrementalChangesContext, ? extends CurrentSnapshotResult> delegate
    ) {
        this.buildCache = buildCache;
        this.commandFactory = commandFactory;
        this.delegate = delegate;
    }

    @Override
    public CurrentSnapshotResult execute(IncrementalChangesContext context) {
        CachingState cachingState = context.getCachingState();
        //noinspection OptionalGetWithoutIsPresent
        return cachingState.getDisabledReasons().isEmpty()
            ? executeWithCache(context, cachingState.getKey().get())
            : executeWithoutCache(context);
    }

    private CurrentSnapshotResult executeWithCache(IncrementalChangesContext context, BuildCacheKey cacheKey) {
        UnitOfWork work = context.getWork();
        return Try.ofFailable(() -> work.isAllowedToLoadFromCache()
                ? buildCache.load(commandFactory.createLoad(cacheKey, work))
                : Optional.<LoadMetadata>empty()
            )
            .map(successfulLoad -> successfulLoad
                .map(cacheHit -> {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Loaded cache entry for {} with cache key {}",
                            work.getDisplayName(), cacheKey.getHashCode());
                    }
                    cleanLocalState(work);
                    OriginMetadata originMetadata = cacheHit.getOriginMetadata();
                    ImmutableSortedMap<String, CurrentFileCollectionFingerprint> finalOutputs = cacheHit.getResultingSnapshots();
                    return (CurrentSnapshotResult) new CurrentSnapshotResult() {
                        @Override
                        public Try<ExecutionOutcome> getOutcome() {
                            return Try.successful(ExecutionOutcome.FROM_CACHE);
                        }

                        @Override
                        public OriginMetadata getOriginMetadata() {
                            return originMetadata;
                        }

                        @Override
                        public boolean isReused() {
                            return true;
                        }

                        @Override
                        public ImmutableSortedMap<String, CurrentFileCollectionFingerprint> getFinalOutputs() {
                            return finalOutputs;
                        }
                    };
                })
                .orElseGet(() -> executeAndStoreInCache(cacheKey, context))
            )
            .getOrMapFailure(loadFailure -> {
                throw new RuntimeException(
                    String.format("Failed to load cache entry for %s",
                        work.getDisplayName()),
                    loadFailure
                );
            });
    }

    private static void cleanLocalState(UnitOfWork work) {
        work.visitLocalState(localStateFile -> {
            try {
                remove(localStateFile);
            } catch (IOException ex) {
                throw new UncheckedIOException(String.format("Failed to clean up local state files for %s: %s", work.getDisplayName(), localStateFile), ex);
            }
        });
    }

    private static void remove(File root) throws IOException {
        if (root.exists()) {
            if (root.isDirectory()) {
                FileUtils.cleanDirectory(root);
            } else {
                FileUtils.forceDelete(root);
            }
        }
    }

    private CurrentSnapshotResult executeAndStoreInCache(BuildCacheKey cacheKey, IncrementalChangesContext context) {
        CurrentSnapshotResult executionResult = executeWithoutCache(context);
        executionResult.getOutcome().ifSuccessfulOrElse(
            outcome -> store(context.getWork(), cacheKey, executionResult),
            failure -> LOGGER.debug("Not storing result of {} in cache because the execution failed", context.getWork().getDisplayName())
        );
        return executionResult;
    }

    private void store(UnitOfWork work, BuildCacheKey cacheKey, CurrentSnapshotResult result) {
        try {
            buildCache.store(commandFactory.createStore(cacheKey, work, result.getFinalOutputs(), result.getOriginMetadata().getExecutionTime()));
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Stored cache entry for {} with cache key {}",
                    work.getDisplayName(), cacheKey.getHashCode());
            }
        } catch (Exception e) {
            throw new RuntimeException(
                String.format("Failed to store cache entry for %s",
                    work.getDisplayName()),
                e);
        }
    }

    private CurrentSnapshotResult executeWithoutCache(IncrementalChangesContext context) {
        return delegate.execute(context);
    }
}
