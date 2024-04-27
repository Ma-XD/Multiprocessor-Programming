import java.util.concurrent.atomic.*

/**
 * @author Dzyubenko Maxim
 */
class Solution(val env: Environment) : Lock<Solution.Node> {
    private val tail = AtomicReference<Node?>(null)

    override fun lock(): Node {
        val my = Node() // сделали узел
        my.locked.value = true
        val prev = tail.getAndSet(my)
        prev?.let {
            it.next.value = my
            while (my.locked.value) env.park()
        }

        return my // вернули узел
    }

    override fun unlock(node: Node) {
        if (node.next.value == null) {
            if (tail.compareAndSet(node, null)) return
            while (node.next.value == null);
        }
        val next = node .next.value!!
        next.locked.value = false
        env.unpark(next.thread)
    }

    class Node {
        val thread = Thread.currentThread() // запоминаем поток, которые создал узел
        val locked = AtomicReference(false)
        val next = AtomicReference<Node?>(null)
    }
}