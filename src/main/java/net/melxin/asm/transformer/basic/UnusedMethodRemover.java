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
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

@Slf4j
public class UnusedMethodRemover implements JarEntryTransformer
{
	private final Set<String> used = new HashSet<>();
	private int removedMethodCount;

	@Override
	public void transform(JarEntryGroup jarEntryGroup)
	{
		final Set<ClassNode> classNodes = jarEntryGroup.getClassNodes();

		// Check for method usages
		for (ClassNode classNode : classNodes)
		{
			for (MethodNode method : classNode.methods)
			{
				for (AbstractInsnNode insn : method.instructions)
				{
					if (insn instanceof MethodInsnNode)
					{
						final MethodInsnNode methodInsn = (MethodInsnNode) insn;
						used.add(methodInsn.owner + "#" + methodInsn.name + "#" + methodInsn.desc);
					}
					else if (insn instanceof InvokeDynamicInsnNode)
					{
						final InvokeDynamicInsnNode invokeDynamicInsn = (InvokeDynamicInsnNode) insn;
						final Handle bootstrapHandle = invokeDynamicInsn.bsm;
						if (bootstrapHandle != null)
						{
							//used.add(classNode.name + "#" + method.name + "#" + method.desc);
							log.debug("Invoke dynamic with bootstrap: {}.{} in method: {}.{}", bootstrapHandle.getOwner(), bootstrapHandle.getOwner(), classNode.name, method.name);
							for (Object arg : invokeDynamicInsn.bsmArgs)
							{
								if (arg instanceof Handle)
								{
									final Handle handle = (Handle) arg;
									used.add(handle.getOwner() + "#" + handle.getName() + "#" + handle.getDesc());
									log.debug("Invoke dynamic handle: {}.{} {}", handle.getOwner(), handle.getName(), handle.getDesc());
								}
							}
						}
					}
				}
			}
		}

		// Check for reflection usages
		/*for (ClassNode classNode : classNodes)
		{
			for (MethodNode method : classNode.methods)
			{
				final ASMUtils.ObjectStack objectStack = new ASMUtils.ObjectStack();

				for (AbstractInsnNode insn : method.instructions)
				{
					if (insn instanceof LdcInsnNode)
					{
						final Object cst = ((LdcInsnNode) insn).cst;
						if (cst instanceof String)
						{
							objectStack.push(cst);
						}
					}
					else if (insn instanceof MethodInsnNode)
					{
						final MethodInsnNode mInsn = (MethodInsnNode) insn;

						if (mInsn.owner.equals("java/lang/Class") &&
							(mInsn.name.equals("getMethod") || mInsn.name.equals("getDeclaredMethod")))
						{
							final String methodName = (String) objectStack.pop();
							if (methodName != null)
							{
								used.add(classNode.name + "#" + methodName + "#(Ljava/lang/String;)");
							}
						}
						else if (mInsn.owner.equals("java/lang/reflect/Method") && mInsn.name.equals("invoke"))
						{
							used.add(classNode.name + "#" + method.name + "#" + method.desc);
							log.debug("Method.invoke in method: {}.{}", classNode.name, method.name);
						}
					}
				}
			}
		}*/

		// Check if method is unused and remove it
		for (ClassNode classNode : classNodes)
		{
			for (ListIterator<MethodNode> iter = classNode.methods.listIterator(); iter.hasNext(); )
			{
				final MethodNode method = iter.next();
				if (method.name.equals("<init>")
					|| method.name.equals("<clinit>")
					|| method.name.length() != 2)
				{
					continue;
				}

				if (!used.contains(classNode.name + "#" + method.name + "#" + method.desc))
				{
					iter.remove();
					removedMethodCount++;
					log.debug("Removed unused method: {} {}.{} {}", Modifier.toString(method.access), classNode.name, method.name, method.desc);
				}
			}
		}
		log.info("Removed: {} unused methods", removedMethodCount);
	}
}
