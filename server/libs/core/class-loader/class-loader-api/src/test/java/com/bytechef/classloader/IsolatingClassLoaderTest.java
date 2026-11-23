/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.classloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * @author Ivica Cardic
 */
class IsolatingClassLoaderTest {

    private static final String CACHE_NAME = IsolatingClassLoader.class.getName() + ".isolatingClassLoader";
    private static final String MARKER_RESOURCE = "marker.txt";

    @TempDir
    private Path tempDirectory;

    private URL directoryUrl;

    @BeforeEach
    void beforeEach() throws IOException {
        Files.writeString(tempDirectory.resolve(MARKER_RESOURCE), "marker");

        directoryUrl = tempDirectory.toUri()
            .toURL();
    }

    @Test
    void testGetKeepsCachedClassLoaderOpen() {
        CacheManager cacheManager = new ConcurrentMapCacheManager();

        TestIsolatingClassLoader testIsolatingClassLoader = new TestIsolatingClassLoader(directoryUrl, cacheManager);

        TestValue testValue = testIsolatingClassLoader.get("key");

        assertThat(testValue.classLoader()).isSameAs(testIsolatingClassLoader);
        assertThat(testIsolatingClassLoader.getResource(MARKER_RESOURCE)).isNotNull();
    }

    @Test
    void testInvalidateCacheClosesEvictedClassLoader() {
        CacheManager cacheManager = new ConcurrentMapCacheManager();

        TestIsolatingClassLoader cachedClassLoader = new TestIsolatingClassLoader(directoryUrl, cacheManager);

        cachedClassLoader.get("key");

        TestIsolatingClassLoader evictingClassLoader = new TestIsolatingClassLoader(directoryUrl, cacheManager);

        evictingClassLoader.invalidateCache("key");

        Cache cache = cacheManager.getCache(CACHE_NAME);

        assertThat(cache).isNotNull();
        assertThat(cache.get("key")).isNull();
        assertThat(cachedClassLoader.getResource(MARKER_RESOURCE)).isNull();
    }

    @Test
    void testApplyUncachedClosesClassLoaderAndLeavesCacheEmpty() {
        CacheManager cacheManager = new ConcurrentMapCacheManager();

        TestIsolatingClassLoader testIsolatingClassLoader = new TestIsolatingClassLoader(directoryUrl, cacheManager);

        Boolean resourceReadableWhileOpen = testIsolatingClassLoader.applyUncached(
            "key", testValue -> {
                IsolatingClassLoader<?> classLoader = testValue.classLoader();

                return classLoader.getResource(MARKER_RESOURCE) != null;
            });

        Cache cache = cacheManager.getCache(CACHE_NAME);

        assertThat(resourceReadableWhileOpen).isTrue();
        assertThat(cache).isNotNull();
        assertThat(cache.get("key")).isNull();
        assertThat(testIsolatingClassLoader.getResource(MARKER_RESOURCE)).isNull();
    }

    @Test
    void testLoadClassTakesJdkClassOutsideJavaPackagesFromPlatform() throws ClassNotFoundException {
        TestIsolatingClassLoader testIsolatingClassLoader = new TestIsolatingClassLoader(
            directoryUrl, new ConcurrentMapCacheManager());

        Class<?> sslPeerUnverifiedExceptionClass = testIsolatingClassLoader.loadClass(
            "javax.net.ssl.SSLPeerUnverifiedException");

        assertThat(sslPeerUnverifiedExceptionClass).isSameAs(javax.net.ssl.SSLPeerUnverifiedException.class);
    }

    @Test
    void testGetResourceTakesJdkClassFileOutsideJavaPackagesFromPlatform() throws IOException {
        TestIsolatingClassLoader testIsolatingClassLoader = new TestIsolatingClassLoader(
            directoryUrl, new ConcurrentMapCacheManager());

        String resourceName = "javax/net/ssl/SSLPeerUnverifiedException.class";

        assertThat(testIsolatingClassLoader.getResource(resourceName)).isNotNull();
        assertThat(testIsolatingClassLoader.getResources(resourceName)
            .hasMoreElements()).isTrue();
    }

    @Test
    void testLoadClassIgnoresJarCopyOfJdkPackage() throws IOException {
        writeJarEntry("org/w3c/dom/BundledNode.class", "not a class");

        TestIsolatingClassLoader testIsolatingClassLoader = new TestIsolatingClassLoader(
            directoryUrl, new ConcurrentMapCacheManager());

        assertThatThrownBy(() -> testIsolatingClassLoader.loadClass("org.w3c.dom.BundledNode"))
            .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void testGetResourceIgnoresJarCopyOfJdkPackage() throws IOException {
        writeJarEntry("org/w3c/dom/bundled.txt", "bundled");

        TestIsolatingClassLoader testIsolatingClassLoader = new TestIsolatingClassLoader(
            directoryUrl, new ConcurrentMapCacheManager());

        assertThat(testIsolatingClassLoader.getResource("org/w3c/dom/bundled.txt")).isNull();
    }

    @Test
    void testLoadClassTakesNonJdkJavaxPackageFromJar() throws IOException {
        writeJarEntry("javax/annotation/Bundled.class", "not a class");

        TestIsolatingClassLoader testIsolatingClassLoader = new TestIsolatingClassLoader(
            directoryUrl, new ConcurrentMapCacheManager());

        assertThatThrownBy(() -> testIsolatingClassLoader.loadClass("javax.annotation.Bundled"))
            .isInstanceOf(ClassFormatError.class);
    }

    @Test
    void testGetResourceTakesNonJdkJavaxPackageFromJar() throws IOException {
        writeJarEntry("javax/annotation/bundled.txt", "bundled");

        TestIsolatingClassLoader testIsolatingClassLoader = new TestIsolatingClassLoader(
            directoryUrl, new ConcurrentMapCacheManager());

        assertThat(testIsolatingClassLoader.getResource("javax/annotation/bundled.txt")).isNotNull();
    }

    @Test
    void testGetResourceKeepsApplicationResourcesIsolated() {
        TestIsolatingClassLoader testIsolatingClassLoader = new TestIsolatingClassLoader(
            directoryUrl, new ConcurrentMapCacheManager());

        assertThat(testIsolatingClassLoader.getResource("org/assertj/core/api/Assertions.class")).isNull();
    }

    @Test
    void testLoadClassKeepsApplicationClassesIsolated() {
        TestIsolatingClassLoader testIsolatingClassLoader = new TestIsolatingClassLoader(
            directoryUrl, new ConcurrentMapCacheManager());

        assertThatThrownBy(() -> testIsolatingClassLoader.loadClass("org.assertj.core.api.Assertions"))
            .isInstanceOf(ClassNotFoundException.class);
    }

    private void writeJarEntry(String name, String content) throws IOException {
        Path path = tempDirectory.resolve(name);

        Files.createDirectories(Objects.requireNonNull(path.getParent()));

        Files.writeString(path, content);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    private record TestValue(IsolatingClassLoader<?> classLoader) implements IsolatingClassLoader.ClassLoaderOwner {
    }

    private static class TestIsolatingClassLoader extends IsolatingClassLoader<TestValue> {

        private TestIsolatingClassLoader(URL url, CacheManager cacheManager) {
            super(
                new URL[] {
                    url
                }, IsolatingClassLoaderTest.class.getClassLoader(), cacheManager,
                (key, classLoader) -> new TestValue(classLoader));
        }
    }
}
