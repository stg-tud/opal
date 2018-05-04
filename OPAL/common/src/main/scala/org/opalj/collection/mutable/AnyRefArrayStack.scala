/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj.collection.mutable

import scala.collection.mutable
import scala.collection.generic
import scala.collection.AbstractIterator

/**
 * An array based implementation of a mutable stack of `ref` values which has a
 * given initial size. If the stack is non-empty, the index of the top value is `0` and the
 * index of the bottom value is (`length-1`).
 *
 * @note __This stack generally keeps all references and is only intended to be used to
 *       store elements that outlive the stack otherwise, garbage collection may be prevented.__
 *
 * @param data The array containing the values.
 * @param size0 The number of stored values.
 * @author Michael Eichberg
 */
final class AnyRefArrayStack[N >: Null <: AnyRef] private (
        private var data:  Array[AnyRef],
        private var size0: Int
) extends mutable.IndexedSeq[N]
    with mutable.IndexedSeqLike[N, AnyRefArrayStack[N]]
    with mutable.Cloneable[AnyRefArrayStack[N]]
    with Serializable { stack ⇒

    def this(initialSize: Int = 4) { this(new Array[AnyRef](initialSize), 0) }

    /**
     * Resets the size of the stack, but does not clear the underlying array; hence,
     * the stack may prevent the garbage collection of the still referenced values.
     * This is generally not a problem, if the stack is only used locally and the
     * referenced objects outlive the lifetime of the stack!
     */
    def resetSize(): Unit = size0 = 0

    override def size: Int = size0
    override def length: Int = size0
    override def isEmpty: Boolean = size0 == 0
    override def nonEmpty: Boolean = size0 > 0

    override def apply(index: Int): N = {
        val size0 = this.size0
        val valueIndex = size0 - 1 - index
        if (valueIndex < 0 || valueIndex >= size0)
            throw new IndexOutOfBoundsException(s"$index (size: $size0)");

        data(valueIndex).asInstanceOf[N]
    }

    override def update(index: Int, v: N): Unit = data(size0 - 1 - index) = v

    override def newBuilder: mutable.Builder[N, AnyRefArrayStack[N]] = AnyRefArrayStack.newBuilder[N]

    /** The same as push but additionally returns `this`. */
    final def +=(i: N): this.type = {
        push(i)
        this
    }

    final def ++=(is: Traversable[N]): this.type = {
        is.foreach(push)
        this
    }

    def push(i: N): Unit = {
        val size0 = this.size0
        var data = this.data
        if (data.length == size0) {
            val newData = new Array[AnyRef]((size0 + 1) * 2)
            System.arraycopy(data, 0, newData, 0, size0)
            data = newData
            this.data = newData
        }

        data(size0) = i
        this.size0 = size0 + 1
    }

    /**
     * Pushes the value of the given stack on this stack while maintaining the order
     * in which the values were pushed on the given stack. I.e.,
     * if this contains the values `[1|2->` and the given one the values `[3,4->`
     * then the resulting stack will contain the values `[1|2|3|4...`.
     *
     * @note In case of `++` the order of the values is reversed.
     */
    def push(that: AnyRefArrayStack[N]): Unit = {
        val thatSize = that.size0

        if (thatSize == 0) {
            return ;
        }

        val thisSize = this.size0
        var thisData = this.data

        val newSize = thisSize + thatSize
        if (newSize > thisData.length) {
            val newData = new Array[AnyRef](newSize + 10)
            System.arraycopy(thisData, 0, newData, 0, thisSize)
            thisData = newData
            this.data = thisData
        }

        System.arraycopy(that.data, 0, thisData, thisSize, thatSize)

        this.size0 = newSize
    }

    /**
     * Returns and virtually removes the top most value from the stack.
     *
     * @note If the stack is empty a `NoSuchElementException` will be thrown.
     */
    def pop(): N = {
        val index = this.size0 - 1
        if (index < 0)
            throw new NoSuchElementException("the stack is empty");

        val i = this.data(index)
        this.size0 = index
        i.asInstanceOf[N]
    }

    /**
     * Returns the stack's top-most value.
     *
     * @note If the stack is empty a `NoSuchElementException` will be thrown.
     */
    def top: N = {
        val index = this.size0 - 1
        if (index < 0)
            throw new NoSuchElementException("the stack is empty");

        this.data(index).asInstanceOf[N]
    }

    /** @see `top` */
    final def peek: N = top

    /**
     * Same as `top`.
     */
    override /*TraversableLike*/ def head: N = top

    override /*TraversableLike*/ def last: N = {
        if (this.size0 == 0)
            throw new NoSuchElementException("the stack is empty");

        this.data(0).asInstanceOf[N]
    }

    override def foreach[U](f: N ⇒ U): Unit = {
        val data = this.data
        var i = this.size0 - 1
        while (i >= 0) {
            f(data(i).asInstanceOf[N])
            i -= 1
        }
    }

    override def foldLeft[B](z: B)(f: (B, N) ⇒ B): B = {
        val data = this.data
        var v = z
        var i = this.size0 - 1
        while (i >= 0) {
            v = f(v, data(i).asInstanceOf[N])
            i -= 1
        }
        v
    }

    /**
     * Returns an iterator which produces the values in LIFO order.
     *
     * @note    The `next` method will throw an `IndexOutOfBoundsException`
     *          when all elements are already returned.
     */
    override def iterator: Iterator[N] = {
        new AbstractIterator[N] {
            var currentIndex = stack.size0 - 1
            def hasNext: Boolean = currentIndex >= 0

            def next(): N = {
                val currentIndex = this.currentIndex
                val r = stack.data(currentIndex)
                this.currentIndex = currentIndex - 1
                r.asInstanceOf[N]
            }

        }
    }

    override def clone(): AnyRefArrayStack[N] = new AnyRefArrayStack(data.clone(), size0)

    override def toString: String = {
        s"AnyRefArrayStack(/*size=$size0;*/data=${data.take(size0).mkString("[", ",", "→")})"
    }
}

/**
 * Factory to create [[AnyRefArrayStack]]s.
 */
object AnyRefArrayStack {

    implicit def canBuildFrom[N >: Null <: AnyRef]: generic.CanBuildFrom[AnyRefArrayStack[N], N, AnyRefArrayStack[N]] = {
        new generic.CanBuildFrom[AnyRefArrayStack[N], N, AnyRefArrayStack[N]] {
            def apply(): mutable.Builder[N, AnyRefArrayStack[N]] = newBuilder
            def apply(from: AnyRefArrayStack[N]): mutable.Builder[N, AnyRefArrayStack[N]] = newBuilder
        }
    }

    def newBuilder[N >: Null <: AnyRef]: mutable.Builder[N, AnyRefArrayStack[N]] = {
        new mutable.ArrayBuffer[N] mapResult fromSeq
    }

    /**
     * Creates a new stack based on a given sequence. The last value of the sequence will
     * be the top value of the stack.
     */
    def fromSeq[N >: Null <: AnyRef](seq: TraversableOnce[N]): AnyRefArrayStack[N] = {
        seq.foldLeft(new AnyRefArrayStack[N](8))(_ += _)
    }

    def empty[N >: Null <: AnyRef]: AnyRefArrayStack[N] = new AnyRefArrayStack
}
