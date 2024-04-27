import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * @author Dzyubenko Maxim
 */
open class TreiberStackWithElimination<E> : Stack<E> {
    private val stack = TreiberStack<E>()

    private val eliminationArray = AtomicReferenceArray<Any?>(Array<Any?>(ELIMINATION_ARRAY_SIZE) { CELL_STATE_EMPTY })
    private val random = ThreadLocalRandom.current()

    override fun push(element: E) {
        if (tryPushElimination(element)) return
        stack.push(element)
    }

    protected open fun tryPushElimination(element: E): Boolean {
        val randInt = random.nextInt(ELIMINATION_ARRAY_SIZE)
        if (eliminationArray.compareAndSet(randInt, CELL_STATE_EMPTY, element)) {
            repeat(ELIMINATION_WAIT_CYCLES) {
                if (eliminationArray.compareAndSet(randInt, CELL_STATE_RETRIEVED, CELL_STATE_EMPTY)) {
                    return true
                }
            }
            return eliminationArray.getAndSet(randInt, CELL_STATE_EMPTY) == CELL_STATE_RETRIEVED
        }
        return false
    }

    override fun pop(): E? = tryPopElimination() ?: stack.pop()

    private fun tryPopElimination(): E? {
        val randInt = random.nextInt(ELIMINATION_ARRAY_SIZE)
        val any = eliminationArray.get(randInt) ?: return null
        if (any != CELL_STATE_RETRIEVED && eliminationArray.compareAndSet(randInt, any, CELL_STATE_RETRIEVED)) {
            return any as E
        }
        return null
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(eliminationArray.length())

    companion object {
        private const val ELIMINATION_ARRAY_SIZE = 2 // Do not change!
        private const val ELIMINATION_WAIT_CYCLES = 1 // Do not change!

        // Initially, all cells are in EMPTY state.
        private val CELL_STATE_EMPTY = null

        // `tryPopElimination()` moves the cell state
        // to `RETRIEVED` if the cell contains element.
        private val CELL_STATE_RETRIEVED = Any()
    }
}
