import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * @author Dzyubenko Maxim
 */
class FlatCombiningQueue<E> : Queue<E> {
    private val queue = ArrayDeque<E>() // sequential queue
    private val combinerLock = AtomicBoolean(false) // unlocked initially
    private val tasksForCombiner = AtomicReferenceArray<Any?>(TASKS_FOR_COMBINER_SIZE)

    override fun enqueue(element: E) {
        if (tryLock()) {
            queue.addLast(element)
            help()
            return
        }
        wait(Enqueue(element))
    }

    override fun dequeue(): E? {
        if (tryLock()) {
            val res = queue.removeFirstOrNull()
            help()
            return res
        }
        return wait(Dequeue)
    }

    private fun help() {
        repeat(TASKS_FOR_COMBINER_SIZE) { i ->
            val task = tasksForCombiner.get(i)
            if (task !is Task) return@repeat

            val res = Result(
                when (task) {
                    is Dequeue -> queue.removeFirstOrNull()
                    is Enqueue<*> -> {
                        queue.addLast(task.value as E)
                        null
                    }
                }
            )
            tasksForCombiner.set(i, res)
        }
        unlock()
    }

    private fun wait(task: Task): E? {
        val i = randomCellIndex()
        while (true) {
            if (tasksForCombiner.compareAndSet(i, EMPTY, task)) {
                break
            }
        }
        while (true) {
            val res = tasksForCombiner.get(i)
            if (res is Result<*>) {
                tasksForCombiner.compareAndSet(i, res, EMPTY)
                return res.value as? E
            }
            if (tryLock()) {
                help()
            }
        }
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(tasksForCombiner.length())

    private fun tryLock() = combinerLock.compareAndSet(false, true)

    private fun unlock() = combinerLock.getAndSet(false)
}

private const val TASKS_FOR_COMBINER_SIZE = 3 // Do not change this constant!

sealed interface Task
data object Dequeue : Task
class Enqueue<V>(val value: V) : Task

private class Result<V>(
    val value: V
)

private val EMPTY = null