/*
 * Copyright (c) 2025, Melxin <https://github.com/melxin>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.melxin.asm.transformer;

import lombok.extern.slf4j.Slf4j;
import net.melxin.asm.transformer.transformers.runelite.NamedAnnotationsRemoverTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;

@Slf4j
public class Main
{
	// Map to hold non-class entries
	private static final Map<String, byte[]> nonClassEntries = new HashMap<>();

	// List to hold class entries as ClassNode objects
	private static final List<ClassNode> classNodes = new ArrayList<>();

	public static void main(String[] args)
	{
		args = new String[]{"input.jar", "output.jar"};

		if (args.length != 2)
		{
			System.out.println("Usage: java -jar asm-transformer.jar <input.jar> <output.jar>");
			return;
		}

		final String inputJarPath = args[0];
		final String outputJarPath = args[1];

		// Load entries
		loadJar(new File(inputJarPath));

		// Apply transformations
		applyTransformers();

		// Write output jar
		writeOutputJar(new File(outputJarPath));

		log.info("Transformation complete: {}", outputJarPath);
	}

	private static void loadJar(File inputJar)
	{
		try (JarFile jarFile = new JarFile(inputJar))
		{
			for (Enumeration<JarEntry> it = jarFile.entries(); it.hasMoreElements(); )
			{
				final JarEntry entry = it.nextElement();
				try (InputStream is = jarFile.getInputStream(entry))
				{
					if (!entry.getName().endsWith(".class"))
					{
						// Store non-class entries
						final byte[] data = is.readAllBytes();
						nonClassEntries.put(entry.getName(), data);
						log.debug("Load non-class entry: {}", entry.getName());
					}
					else
					{
						// Store class entries
						final ClassReader reader = new ClassReader(is);
						final ClassNode classNode = new ClassNode(Opcodes.ASM9);
						reader.accept(classNode, ClassReader.SKIP_FRAMES);
						classNodes.add(classNode);
						log.debug("Load class node: {}", classNode.name);
					}
				}
			}
		}
		catch (IOException e)
		{
			log.error("Failed to load jar: {}", inputJar.getAbsolutePath(), e);
		}
	}

	private static void applyTransformers()
	{
		final NamedAnnotationsRemoverTransformer namedAnnotationsRemover = new NamedAnnotationsRemoverTransformer();
		namedAnnotationsRemover.transform(classNodes);
	}

	private static void writeOutputJar(File outputJar)
	{
		try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJar)))
		{
			// (Optional) set compression level (0-9)
			jos.setLevel(Deflater.BEST_COMPRESSION);

			// Write non-class entries
			for (Map.Entry<String, byte[]> nonClassEntry : nonClassEntries.entrySet())
			{
				final JarEntry newEntry = new JarEntry(nonClassEntry.getKey());
				newEntry.setMethod(ZipEntry.DEFLATED); // Optional
				jos.putNextEntry(newEntry);
				jos.write(nonClassEntry.getValue());
				jos.closeEntry();
				log.debug("Write non-class entry: {}", newEntry.getName());
			}

			// Write class entries
			for (ClassNode classNode : classNodes)
			{
				final JarEntry newEntry = new JarEntry(classNode.name.replace('.', '/') + ".class");
				newEntry.setMethod(ZipEntry.DEFLATED); // Optional
				jos.putNextEntry(newEntry);
				final ClassWriter writer = new ClassWriter(0);
				classNode.accept(writer);
				byte[] classFileBuffer = writer.toByteArray();
				validateDataFlow(newEntry.getName(), classFileBuffer);
				jos.write(classFileBuffer);
				jos.closeEntry();
				log.debug("Write class entry: {}", newEntry.getName());
			}
		}
		catch (IOException e)
		{
			log.error("Failed to write output jar: {}", outputJar.getAbsolutePath(), e);
		}
	}

	private static void validateDataFlow(String name, byte[] data)
	{
		try
		{
			ClassReader cr = new ClassReader(data);
			ClassWriter cw = new ClassWriter(cr, 0);
			ClassVisitor cv = new CheckClassAdapter(cw, true);
			cr.accept(cv, 0);
		}
		catch (Exception ex)
		{
			log.warn("Class: {} failed validation", name, ex);
		}
	}
}