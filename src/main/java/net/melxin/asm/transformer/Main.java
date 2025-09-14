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
import net.melxin.asm.transformer.basic.ExprArgOrder;
import net.melxin.asm.transformer.basic.IllegalStateExceptionRemover;
import net.melxin.asm.transformer.basic.UnreachableInstructionRemover;
import net.melxin.asm.transformer.basic.RedundantGotoRemover;
import net.melxin.asm.transformer.basic.RuntimeTryCatchExceptionBlockRemover;
import net.melxin.asm.transformer.basic.UnusedClassRemover;
import net.melxin.asm.transformer.basic.UnusedFieldRemover;
import net.melxin.asm.transformer.basic.UnusedMethodParametersRemover;
import net.melxin.asm.transformer.basic.UnusedMethodRemover;
import net.melxin.asm.transformer.runelite.RuneLiteNamedAnnotationsRemover;
import net.melxin.asm.transformer.basic.SortMembersByName;
import java.io.File;
import java.util.List;

@Slf4j
public class Main
{
	public static void main(String[] args)
	{
		if (args.length != 2)
		{
			args = new String[]{"input.jar", "output.jar"};
			System.out.println("Usage: java -jar asm-transformer.jar <input.jar> <output.jar>");
		}

		final String inputJarPath = args[0];
		final String outputJarPath = args[1];

		// Load entries
		final JarEntryGroup jarEntryGroup = new JarEntryGroup(new File(inputJarPath));

		// Apply transformations
		jarEntryTransformers.forEach(transformer ->
		{
			transformer.transform(jarEntryGroup);
		});

		// Write output jar
		jarEntryGroup.writeOutputJar(new File(outputJarPath));

		log.info("Transformation completed!");
	}

	private static final List<JarEntryTransformer> jarEntryTransformers = List.of(
		// Basic
		new UnreachableInstructionRemover(),
		new RedundantGotoRemover(),
		new RuntimeTryCatchExceptionBlockRemover(),
		new SortMembersByName(),
		new UnusedFieldRemover(),
		new UnusedMethodRemover(),
		new UnusedClassRemover(),
		new ExprArgOrder(),
		new IllegalStateExceptionRemover(),
		//new UnusedMethodParametersRemover(),
		// RuneLite
		new RuneLiteNamedAnnotationsRemover()
	);
}