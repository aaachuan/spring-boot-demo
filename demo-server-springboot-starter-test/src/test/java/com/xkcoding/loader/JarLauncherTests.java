package com.xkcoding.loader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.loader.JarLauncher;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

public class JarLauncherTests {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private File createJarArchive(String name, String entryPrefix) throws IOException {
        File archive = this.temp.newFile(name);
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(archive));
        jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/"));
        jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/classes/"));
        jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/lib/"));
        JarEntry libFoo = new JarEntry(entryPrefix + "/lib/foo.jar");
        libFoo.setMethod(ZipEntry.STORED);
        ByteArrayOutputStream fooJarStream = new ByteArrayOutputStream();
        new JarOutputStream(fooJarStream).close();
        libFoo.setSize(fooJarStream.size());
        CRC32 crc32 = new CRC32();
        crc32.update(fooJarStream.toByteArray());
        libFoo.setCrc(crc32.getValue());
        jarOutputStream.putNextEntry(libFoo);
        jarOutputStream.write(fooJarStream.toByteArray());
        jarOutputStream.close();
        return archive;
    }

    private File explode(File archive) throws IOException {
        File exploded = this.temp.newFolder("exploded");
        JarFile jarFile = new JarFile(archive);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            File entryFile = new File(exploded, entry.getName());
            if (entry.isDirectory()) {
                entryFile.mkdirs();
            }
            else {
                FileCopyUtils.copy(jarFile.getInputStream(entry),
                    new FileOutputStream(entryFile));
            }
        }
        jarFile.close();
        return exploded;
    }

    private Set<URL> getUrls(List<Archive> archives) throws MalformedURLException {
        Set<URL> urls = new HashSet<>(archives.size());
        for (Archive archive : archives) {
            urls.add(archive.getUrl());
        }
        return urls;
    }

    @Test
    public void explodedJarHasOnlyBootInfClassesAndContentsOfBootInfLibOnClasspath()
        throws Exception {
        File explodedRoot = explode(createJarArchive("archive.jar", "BOOT-INF"));
        Archive archive = new ExplodedArchive(explodedRoot, true);

        /**
         * protected的构造函数和方法被迫使用反射
         */

        // 获取 JarLauncher 的构造函数
        Constructor<JarLauncher> constructor = JarLauncher.class.getDeclaredConstructor(Archive.class);

        // 设置构造函数可访问
        constructor.setAccessible(true);

        // 通过反射实例化 JarLauncher
        JarLauncher launcher = constructor.newInstance(archive);

        // 获取父类的 Class 对象
        Class<?> parentClass = launcher.getClass().getSuperclass();

        // 获取父类的 protected 方法
        Method protectedMethod = parentClass.getDeclaredMethod("getClassPathArchives");
        // 设置方法可访问
        protectedMethod.setAccessible(true);

        /**
         * 过滤的逻辑主要是这个：
         * 将BOOT-INF/lib/
         *   BOOT-INF/lib/foo.jar
         *   BOOT-INF/classes/
         *   过滤出BOOT-INF/lib/foo.jar和BOOT-INF/classes/
         * 	protected boolean isNestedArchive(Archive.Entry entry) {
         * 		if (entry.isDirectory()) {
         * 			return entry.getName().equals(BOOT_INF_CLASSES);
         *                }
         * 		return entry.getName().startsWith(BOOT_INF_LIB);* 	}
         */
        // 调用父类的 protected 方法
        List<Archive> archives = (List<Archive>) protectedMethod.invoke(launcher);

    }


}
