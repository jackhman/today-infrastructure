/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *   
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.InvalidPathException;

import org.junit.Test;

import cn.taketoday.context.io.JarEntryResource;
import cn.taketoday.context.io.Resource;

/**
 * @author TODAY <br>
 *         2019-05-15 14:04
 */
public class ResourceUtilsTest {

    @Test
    public void testGetRelativePath() throws IOException {
        final String relativePath = ResourceUtils.getRelativePath("D:/java/", "1.txt");
        final String relativePath1 = ResourceUtils.getRelativePath("D:/java", "1.txt");
        final String relativePath2 = ResourceUtils.getRelativePath("D:/java/2.txt", "1.txt");

        System.err.println(relativePath);
        assert relativePath.equals("D:/java/1.txt");

        System.err.println(relativePath1);
        assert relativePath1.equals("D:/1.txt");

        System.err.println(relativePath2);
        assert relativePath2.equals("D:/java/1.txt");

        assert ResourceUtils.getRelativePath("index", "TODAY").equals("TODAY");

    }

    @Test
    public void testGetResource() throws IOException {

//		final Resource resource = ResourceUtils.getResource("/META-INF/maven/cn.taketoday/today-expression/pom.properties");
        Resource resource = ResourceUtils.getResource("classpath:/META-INF/maven/org.slf4j/slf4j-api/pom.properties");

        System.err.println(resource);
        Resource createRelative = resource.createRelative("pom.xml");
        System.err.println(createRelative);

        assert createRelative.exists();
        assert resource.exists();

        resource = ResourceUtils.getResource("file:/D:/Projects/Git/github/today-context/src/main/resources/META-INF/ignore/jar-prefix");

        System.err.println(resource);

        assert resource.exists();

        System.err.println(StringUtils.readAsText(resource.getInputStream()));

        resource = ResourceUtils.getResource("jar:file:/D:/Projects/Git/github/today-context/src/test/resources/test.jar!/META-INF/");
        System.err.println(resource);

        if (resource instanceof JarEntryResource) {

            JarEntryResource jarEntryResource = (JarEntryResource) resource.createRelative(
                                                                                           "/maven/cn.taketoday/today-expression/pom.properties");
            if (jarEntryResource.exists()) {
                System.out.println(StringUtils.readAsText(jarEntryResource.getInputStream()));
            }

            System.err.println(jarEntryResource);
        }
        // location is empty
        final Resource classpath = ResourceUtils.getResource("");
        assert classpath.createRelative("info.properties").exists();
        // start with '/'
        assert ResourceUtils.getResource("info.properties").exists();
        assert ResourceUtils.getResource("classpath:info.properties").exists();

        try {
            ResourceUtils.getResource("today://info");
        }
        catch (InvalidPathException e) {
            System.err.println(e);
        }
        ResourceUtils.getResource("info.properties");

//        final Resource resource2 = ResourceUtils.getResource("info"); // ConfigurationException

        // getResource(URL)

//        final Resource taketoday = ResourceUtils.getResource(new URL("https://taketoday.cn"));
//
//        assert taketoday.exists();
//        assert StringUtils.readAsText(taketoday.getInputStream()) != null;
//        System.err.println(StringUtils.readAsText(taketoday.getInputStream()));

    }
    
    // spring
    // ----------------------------------------

    @Test
    public void isJarURL() throws Exception {
        assertThat(ResourceUtils.isJarURL(new URL("jar:file:myjar.jar!/mypath"))).isTrue();
        assertThat(ResourceUtils.isJarURL(new URL(null, "zip:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isTrue();
        assertThat(ResourceUtils.isJarURL(new URL(null, "wsjar:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isTrue();
        assertThat(ResourceUtils.isJarURL(new URL(null, "jar:war:file:mywar.war*/myjar.jar!/mypath", new DummyURLStreamHandler()))).isTrue();
        assertThat(ResourceUtils.isJarURL(new URL("file:myjar.jar"))).isFalse();
        assertThat(ResourceUtils.isJarURL(new URL("http:myserver/myjar.jar"))).isFalse();
    }

    @Test
    public void extractJarFileURL() throws Exception {
        assertThat(ResourceUtils.extractJarFileURL(new URL("jar:file:myjar.jar!/mypath"))).isEqualTo(new URL("file:myjar.jar"));
        assertThat(ResourceUtils.extractJarFileURL(new URL(null, "jar:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(new URL("file:/myjar.jar"));
        assertThat(ResourceUtils.extractJarFileURL(new URL(null, "zip:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(new URL("file:myjar.jar"));
        assertThat(ResourceUtils.extractJarFileURL(new URL(null, "wsjar:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(new URL("file:myjar.jar"));

        assertThat(ResourceUtils.extractJarFileURL(new URL("file:myjar.jar"))).isEqualTo(new URL("file:myjar.jar"));
        assertThat(ResourceUtils.extractJarFileURL(new URL("jar:file:myjar.jar!/"))).isEqualTo(new URL("file:myjar.jar"));
        assertThat(ResourceUtils.extractJarFileURL(new URL(null, "zip:file:myjar.jar!/", new DummyURLStreamHandler()))).isEqualTo(new URL("file:myjar.jar"));
        assertThat(ResourceUtils.extractJarFileURL(new URL(null, "wsjar:file:myjar.jar!/", new DummyURLStreamHandler()))).isEqualTo(new URL("file:myjar.jar"));
    }

    @Test
    public void extractArchiveURL() throws Exception {
        assertThat(ResourceUtils.extractArchiveURL(new URL("jar:file:myjar.jar!/mypath"))).isEqualTo(new URL("file:myjar.jar"));
        assertThat(ResourceUtils.extractArchiveURL(new URL(null, "jar:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(new URL("file:/myjar.jar"));
        assertThat(ResourceUtils.extractArchiveURL(new URL(null, "zip:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(new URL("file:myjar.jar"));
        assertThat(ResourceUtils.extractArchiveURL(new URL(null, "wsjar:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(new URL("file:myjar.jar"));
        assertThat(ResourceUtils.extractArchiveURL(new URL(null, "jar:war:file:mywar.war*/myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(new URL("file:mywar.war"));

        assertThat(ResourceUtils.extractArchiveURL(new URL("file:myjar.jar"))).isEqualTo(new URL("file:myjar.jar"));
        assertThat(ResourceUtils.extractArchiveURL(new URL("jar:file:myjar.jar!/"))).isEqualTo(new URL("file:myjar.jar"));
        assertThat(ResourceUtils.extractArchiveURL(new URL(null, "zip:file:myjar.jar!/", new DummyURLStreamHandler()))).isEqualTo(new URL("file:myjar.jar"));
        assertThat(ResourceUtils.extractArchiveURL(new URL(null, "wsjar:file:myjar.jar!/", new DummyURLStreamHandler()))).isEqualTo(new URL("file:myjar.jar"));
        assertThat(ResourceUtils.extractArchiveURL(new URL(null, "jar:war:file:mywar.war*/myjar.jar!/", new DummyURLStreamHandler()))).isEqualTo(new URL("file:mywar.war"));
    }


    /**
     * Dummy URLStreamHandler that's just specified to suppress the standard
     * {@code java.net.URL} URLStreamHandler lookup, to be able to
     * use the standard URL class for parsing "rmi:..." URLs.
     */
    private static class DummyURLStreamHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            throw new UnsupportedOperationException();
        }
    }


}
