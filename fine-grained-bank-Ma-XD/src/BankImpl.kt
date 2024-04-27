import java.util.concurrent.locks.ReentrantLock

/**
 * Bank implementation.
 *
 * :TODO: This implementation has to be made thread-safe.
 *
 * @author Dzyubenko Maxim
 */
class BankImpl(n: Int) : Bank {
    private val accounts: Array<Account> = Array(n) { Account() }

    override val numberOfAccounts: Int
        get() = accounts.size

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun getAmount(index: Int): Long {
        val account = accounts[index]
        account.lock.lock()
        val amount = account.amount
        account.lock.unlock()
        return amount
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override val totalAmount: Long
        get() {
            accounts.forEach { it.lock.lock() }
            val sum = accounts.sumOf { it.amount }
            accounts.forEach { it.lock.unlock() }
            return sum
        }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun deposit(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        account.lock.lock()
        check(!(amount > Bank.MAX_AMOUNT || account.amount + amount > Bank.MAX_AMOUNT)) {
            account.lock.unlock()
            "Overflow"
        }
        account.amount += amount
        val newAmount = account.amount
        account.lock.unlock()
        return newAmount
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun withdraw(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        account.lock.lock()
        check(account.amount - amount >= 0) {
            account.lock.unlock()
            "Underflow"
        }
        account.amount -= amount
        val newAmount = account.amount
        account.lock.unlock()
        return newAmount
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun transfer(fromIndex: Int, toIndex: Int, amount: Long) {
        require(amount > 0) { "Invalid amount: $amount" }
        require(fromIndex != toIndex) { "fromIndex == toIndex" }
        val from = accounts[fromIndex]
        val to = accounts[toIndex]
        val (lock1, lock2) = if (fromIndex <= toIndex)
            listOf(from.lock, to.lock)
        else
            listOf(to.lock, from.lock)

        lock1.lock()
        lock2.lock()
        check(amount <= from.amount) {
            lock2.unlock()
            lock1.unlock()
            "Underflow"
        }
        check(!(amount > Bank.MAX_AMOUNT || to.amount + amount > Bank.MAX_AMOUNT)) {
            lock2.unlock()
            lock1.unlock()
            "Overflow"
        }
        from.amount -= amount
        to.amount += amount
        lock2.unlock()
        lock1.unlock()
    }

    /**
     * Private account data structure.
     */
    class Account {
        /**
         * Amount of funds in this account.
         */
        var amount: Long = 0
        val lock = ReentrantLock()
    }
}