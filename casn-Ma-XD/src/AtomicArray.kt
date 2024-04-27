import kotlinx.atomicfu.*

/**
 * @author Dzyubenko Maxim
 */
class AtomicArray<E>(size: Int, initialValue: E) {
    private val array = Array(size) { Ref(initialValue) }

    fun get(index: Int) =
        array[index].value

    fun set(index: Int, value: E) {
        array[index].value = value
    }

    fun cas(index: Int, expected: E, update: E) =
        array[index].cas(expected, update)

    fun cas2(
        index1: Int, expected1: E, update1: E,
        index2: Int, expected2: E, update2: E
    ): Boolean {
        if (index1 == index2) {
            return if (expected1 == expected2) cas(index1, expected1, update2)
            else false
        }

        val t1 = Triple(array[index1], expected1, update1)
        val t2 = Triple(array[index2], expected2, update2)

        val (a, expectedA, updateA) = if (index1 < index2) t1 else t2
        val (b, expectedB, updateB) = if (index1 < index2) t2 else t1

        val descriptor = CAS2Descriptor(a, expectedA, updateA, b, expectedB, updateB)

        return if (a.cas(expectedA, descriptor)) {
            descriptor.complete()
            descriptor.outcome.value == Success
        } else false
    }

    private interface Descriptor {
        fun complete()
    }

    class Ref<T>(initial: T) {
        val v = atomic<Any?>(initial)
        var value: T
            get() {
                while (true) {
                    val cur = v.value
                    if (cur is Descriptor) {
                        cur.complete()
                        continue
                    }
                    return cur as T
                }
            }
            set(upd) {
                while (true) {
                    val cur = v.value
                    if (cur is Descriptor) {
                        cur.complete()
                        continue
                    }
                    if (v.compareAndSet(cur, upd)) return
                }
            }

        fun cas(expected: Any?, update: Any?): Boolean {
            while (true) {
                val cur = v.value
                when {
                    cur is Descriptor -> cur.complete()
                    cur != expected -> return false
                    else -> {
                        if (v.compareAndSet(cur, update)) return true
                    }
                }
            }
        }
    }

    private class RDCSSDescriptor<A, B>(
        val a: Ref<A>, val expectedA: A, val updateA: Any?,
        val b: Ref<B>, val expectedB: B,
    ) : Descriptor {
        val outcome = atomic<Outcome>(Undecided)

        override fun complete() {
            val updOutcome = if (b.value == expectedB) Success else Fail
            outcome.compareAndSet(Undecided, updOutcome)

            val updA = if (outcome.value == Success) updateA else expectedA
            a.v.compareAndSet(this, updA)
        }
    }


    private class CAS2Descriptor<E>(
        val a: Ref<E>, val expectedA: E, val updateA: E,
        val b: Ref<E>, val expectedB: E, val updateB: E,
    ) : Descriptor {
        val outcome = Ref<Outcome>(Undecided)

        override fun complete() {
            if (b.v.value != this) dcss(b, expectedB, this, outcome, Undecided)
            val updOutcome = if (b.v.value == this) Success else Fail
            outcome.v.compareAndSet(Undecided, updOutcome)

            val res = outcome.value == Success
            val updA = if (res) updateA else expectedA
            a.v.compareAndSet(this, updA)
            val updB = if (res) updateB else expectedB
            b.v.compareAndSet(this, updB)
        }

    }

    companion object {
        sealed interface Outcome
        data object Undecided : Outcome
        data object Fail : Outcome
        data object Success : Outcome

        fun <A, B> dcss(
            a: Ref<A>, expectedA: A, updateA: Any?,
            b: Ref<B>, expectedB: B,
        ): Boolean {
            val descriptor = RDCSSDescriptor(a, expectedA, updateA, b, expectedB)
            return if (a.cas(expectedA, descriptor)) {
                descriptor.complete()
                descriptor.outcome.value == Success
            } else false
        }
    }
}