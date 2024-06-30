interface Tree<T, ST : Tree<T, ST>> {
    val self: ST

    var value: T?

    val children: Map<String, ST>

    fun getOrCreate(token: String): ST

    operator fun get(name: List<String>): ST? = when (name.size) {
        0 -> self
        1 -> children[name.first()]
        else -> get(listOf(name.first()))?.get(name.drop(1))
    }

    fun getOrCreate(name: List<String>): ST = when (name.size) {
        0 -> self
        1 -> getOrCreate(name.first())
        else -> getOrCreate(name.first()).getOrCreate(name.drop(1))
    }

    fun setValue(name: List<String>, value: T): Unit = when (name.size) {
        0 -> error("Empty name!")
        1 -> this.value = value
        else -> getOrCreate(name.first()).value = value
    }

    operator fun set(token: String, item: ST)

    fun setBranch(name: List<String>, branch: Tree<T, *>) {
        getOrCreate(name).fill(branch)
    }

    operator fun set(name: List<String>, item: ST): Unit = when (name.size) {
        0 -> error("Empty name!")
        1 -> set(name.first(), item)
        else -> getOrCreate(name.first()).set(name.drop(1), item)
    }

    fun asSequence(): Sequence<Pair<List<String>, T>> =
        sequence {
            value?.let { yield(emptyList<String>() to it) }
            children.forEach { (token, branch) ->
                val branchSequence: Sequence<Pair<List<String>, T>> = branch.asSequence().map {
                    (listOf(token, *it.first.toTypedArray())) to it.second
                }
                yieldAll(branchSequence)
            }
        }

    fun fill(branch: Tree<T, *>) {
        branch.asSequence().forEach {
            setValue(it.first, it.second)
        }
    }
}

class MapTree<T>(
    override var value: T?,
    override val children: MutableMap<String, MapTree<T>> = mutableMapOf(),
) : Tree<T, MapTree<T>> {

    override val self: MapTree<T> get() = this

    override fun getOrCreate(token: String): MapTree<T> = children.getOrPut(token) { MapTree(null) }

    override fun set(token: String, item: MapTree<T>) {
        children[token] = item
    }
}