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
import net.melxin.asm.transformer.JarEntryTransformer;
import net.melxin.asm.transformer.JarEntryGroup;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

@Slf4j
public class ExprArgOrder implements JarEntryTransformer
{
	private int reorderedCount;

	@Override
	public void transform(JarEntryGroup jarEntryGroup)
	{
		for (ClassNode classNode : jarEntryGroup.getClassNodes())
		{
			for (MethodNode method : classNode.methods)
			{
				final AbstractInsnNode[] instructions = method.instructions.toArray();
				for (int i = 0; i < instructions.length; i++)
				{
					final AbstractInsnNode in = instructions[i];
					if (in instanceof FieldInsnNode)
					{
						final FieldInsnNode fin = (FieldInsnNode) in;
						if (!fin.desc.equals("I") && !fin.desc.equals("J"))
						{
							continue;
						}

						/**
						 * LDC * Field -> Field * LDC
						 */
						if (in.getNext().getOpcode() == Opcodes.IMUL || in.getNext().getOpcode() == Opcodes.LMUL)
						{
							LdcInsnNode LDCInsn = null;
							if (in.getPrevious() instanceof LdcInsnNode)
							{
								LDCInsn = (LdcInsnNode) in.getPrevious();
							}
							else if (in.getPrevious().getPrevious() instanceof LdcInsnNode)
							{
								LDCInsn = (LdcInsnNode) in.getPrevious().getPrevious();
							}

							if (LDCInsn != null)
							{
								method.instructions.remove(LDCInsn);
								method.instructions.insert(in, LDCInsn);
								reorderedCount++;
							}
						}

						/**
						 * FIELD = LDC * EXPRESSION % VALUE -> FIELD = EXPRESSION % VALUE * LDC
						 *
						 *              ldc 173807000 (java.lang.Integer)
						 *              getstatic client.uq:int
						 *              ldc 656928497 (java.lang.Integer)
						 *              imul
						 *              iconst_1
						 *              iadd
						 *              ldc -1288700607 (java.lang.Integer)
						 *              irem
						 *              imul
						 *              putstatic client.uq:int
						 *
						 *              ->
						 *
						 *              getstatic client.uq:int
						 *              ldc 656928497 (java.lang.Integer)
						 *              imul
						 *              iconst_1
						 *              iadd
						 *              ldc -1288700607 (java.lang.Integer)
						 *              irem
						 *              ldc 173807000 (java.lang.Integer)
						 *              imul
						 *              putstatic client.uq:int
						 */
						/*if (in.getPrevious() != null
							&& in.getPrevious() instanceof LdcInsnNode
							&& in.getNext() instanceof LdcInsnNode
							&& (in.getNext().getNext().getOpcode() == Opcodes.IMUL || in.getNext().getNext().getOpcode() == Opcodes.LMUL))
						{
							AbstractInsnNode ldc = in.getPrevious();
							AbstractInsnNode putField = in.getNext().getNext().getNext().getNext().getNext().getNext().getNext().getNext();
							AbstractInsnNode mul = in.getNext().getNext().getNext().getNext().getNext().getNext().getNext();
							if (putField.getOpcode() == Opcodes.PUTSTATIC && mul.getOpcode() == Opcodes.IMUL)
							{
								method.instructions.remove(ldc);
								method.instructions.insertBefore(mul, ldc);
								reorderedCount++;
							}
						}*/

						/*if (in.getOpcode() == Opcodes.PUTSTATIC && in.getPrevious() != null && (in.getPrevious().getOpcode() == Opcodes.IMUL || in.getPrevious().getOpcode() == Opcodes.LMUL)
							&& in.getPrevious().getPrevious() != null && in.getPrevious().getPrevious().getOpcode() == Opcodes.IREM)
						{
							for (int j = i; j > 1; j--)
							{
								AbstractInsnNode in2 = instructions[j];
								if (in2 instanceof LineNumberNode && in2.getNext() instanceof LdcInsnNode)
								{
									LdcInsnNode ldcInsn = (LdcInsnNode) in2.getNext();
									AbstractInsnNode mul = in.getPrevious();
									method.instructions.remove(ldcInsn);
									method.instructions.insertBefore(mul, ldcInsn);
									break;
								}
							}
						}*/

						//iconst_1
						//getstatic client.uq:int
						//ldc 434688723 (java.lang.Integer)
						//imul
						//iadd
						/*if (in.getPrevious() != null && in.getPrevious().getOpcode() == Opcodes.ICONST_0)
						{
							if (in.getNext().getNext().getNext() != null && in.getNext().getNext().getNext().getOpcode() == Opcodes.IADD)
							{
								method.instructions.remove(in.getPrevious());
								method.instructions.insertBefore(in.getNext().getNext(), in.getPrevious());
							}
						}*/
					}
				}
			}
		}
		log.info("Reordered {} constants", reorderedCount);
	}
}
