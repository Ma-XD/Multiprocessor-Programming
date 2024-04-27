import java.util.concurrent.atomic.*

/**
 * @author Dzyubenko Maxim
 */
class FAABasedQueue<E> : Queue<E> {
    private val head: AtomicReference<Segment>
    private val tail: AtomicReference<Segment>

    init {
        val segment = Segment(0)
        head = AtomicReference(segment)
        tail = AtomicReference(segment)
    }

    override fun enqueue(element: E) {
        while (true) {
            val curTail = tail.get()
            val i = curTail.enqIdx.getAndIncrement().toInt()
            if (i >= SEGMENT_SIZE) {
                val newTail = Segment(curTail.id + 1)
                newTail.enqIdx.getAndIncrement()
                newTail.cells.compareAndSet(0, null, element)
                if (curTail.next.compareAndSet(null, newTail)) {
                    tail.compareAndSet(curTail, newTail)
                    return
                } else {
                    tail.compareAndSet(curTail, curTail.next.get())
                }
            } else {
                if (curTail.cells.compareAndSet(i, null, element)) return
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        while (true) {
            val curHead = head.get()
            val i = curHead.deqIdx.getAndIncrement().toInt()
            if (i >= SEGMENT_SIZE) {
                val newHead = curHead.next.get() ?: return null
                head.compareAndSet(curHead, newHead)
            } else {
                if (curHead.cells.compareAndSet(i, null, POISONED)) continue
                val res = curHead.cells.getAndSet(i, null)
                return res as? E
            }
        }
    }
}

private class Segment(val id: Long) {
    val next = AtomicReference<Segment?>(null)
    val cells = AtomicReferenceArray<Any?>(SEGMENT_SIZE)
    val enqIdx = AtomicLong(0)
    val deqIdx = AtomicLong(0)
}

// DO NOT CHANGE THIS CONSTANT
private const val SEGMENT_SIZE = 2
private val POISONED = Any()
