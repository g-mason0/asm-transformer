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
package net.melxin.asm.transformer.basic;

import lombok.extern.slf4j.Slf4j;
import net.melxin.asm.transformer.JarEntryGroup;
import net.melxin.asm.transformer.JarEntryTransformer;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

@Slf4j
public class UnusedFieldRemover implements JarEntryTransformer
{
	private final Set<String> used = new HashSet<>();
	private int removedFieldCount;

	@Override
	public void transform(JarEntryGroup jarEntryGroup)
	{
		final Set<ClassNode> classNodes = jarEntryGroup.getClassNodes();

		// Check for field usages
		for (ClassNode classNode : classNodes)
		{
			for (MethodNode method : classNode.methods)
			{
				for (AbstractInsnNode in : method.instructions)
				{
					if (in instanceof FieldInsnNode)
					{
						final FieldInsnNode fi = (FieldInsnNode) in;
						used.add(fi.owner + "#" + fi.name + "#" + fi.desc);
					}
				}
			}
		}

		// Check if field is unused and remove it
		for (ClassNode classNode : classNodes)
		{
			for (ListIterator<FieldNode> iter = classNode.fields.listIterator(); iter.hasNext(); )
			{
				final FieldNode field = iter.next();
				if (!used.contains(classNode.name + "#" + field.name + "#" + field.desc))
				{
					iter.remove();
					removedFieldCount++;
					log.debug("Removed unused field: {} {}.{} {}", Modifier.toString(field.access), classNode.name, field.name, field.desc);
				}
			}
		}
		log.info("Removed: {} unused fields", removedFieldCount);
	}
}
