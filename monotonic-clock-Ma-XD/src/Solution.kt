/**
 * В теле класса решения разрешено использовать только переменные делегированные в класс RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author : Dzyubenko Maxim
 */
class Solution : MonotonicClock {
    private var l1 by RegularInt(0)
    private var l2 by RegularInt(0)
    private var l3 by RegularInt(0)
    private var r1 by RegularInt(0)
    private var r2 by RegularInt(0)
    private var r3 by RegularInt(0)


    override fun write(time: Time) {
        // write right-to-left
        r1 = time.d1
        r2 = time.d2
        r3 = time.d3

        l3 = r3
        l2 = r2
        l1 = r1
    }

    override fun read(): Time {
        val v1 = l1
        val v2 = l2
        val w3 = l3
        val w2 = r2
        val w1 = r1
        return when {
            v1 == w1 && v2 == w2 -> Time(w1, w2, w3)
            v1 == w1 -> Time(w1, w2, 0)
            else -> Time(w1, 0, 0)
        }
    }
}