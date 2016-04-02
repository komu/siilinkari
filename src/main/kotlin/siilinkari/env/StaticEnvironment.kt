package siilinkari.env

import siilinkari.types.Type
import java.util.*

/**
 * Mapping from variable names to [Binding]s.
 */
abstract class StaticEnvironment(private val parent: StaticEnvironment?) {

    private val bindings = HashMap<String, Binding>()

    /**
     * Returns the binding of given variable.
     */
    operator fun get(name: String): Binding? =
        bindings[name] ?: parent?.get(name)

    /**
     * Create a new binding for variable having given [name] and [type].
     * For global environment, this creates a new entry in the global mappings.
     * For local environments, this allocates a slot in current environment frame.
     *
     * @throws VariableAlreadyBoundException if variable is already bound in this scope
     */
    fun bind(name: String, type: Type): Binding {
        if (name in bindings) throw VariableAlreadyBoundException(name)

        val binding = newBinding(name, type)
        bindings[name] = binding
        return binding
    }

    /**
     * Create a new binding to be installed in this environment.
     */
    protected abstract fun newBinding(name: String, type: Type): Binding

    /**
     * Returns a new child scope for current environment.
     *
     * Child environment inherits all bindings of the parent environment,
     * but may rebind variables. The variables defined inside the nested
     * environment are not visible outside.
     */
    abstract fun newScope(): StaticEnvironment
}

/**
 * Global environment.
 */
class GlobalStaticEnvironment : StaticEnvironment(null) {
    override fun newBinding(name: String, type: Type): Binding.Global = Binding.Global(name, type)
    override fun newScope(): StaticEnvironment = LocalFrameEnvironment(this)
}

/**
 * Top level local environment. When there are multiple scopes that can share
 * the same frame, the top level scope will be [LocalFrameEnvironment] and the
 * child scopes will be [LocalFrameChildScope]s. This means that each child scooe
 * may shadow bindings created above, but they will still get unique indices.
 */
private class LocalFrameEnvironment(parent: StaticEnvironment) : StaticEnvironment(parent) {
    private var bindingIndexSequence = 0

    public override fun newBinding(name: String, type: Type) = Binding.Local(name, type, bindingIndexSequence++)
    override fun newScope(): StaticEnvironment = LocalFrameChildScope(this, this)
}

/**
 * Environment for child scopes which share frame with their parent, but still provide
 * new scope for names.
 */
private class LocalFrameChildScope(parent: StaticEnvironment, val frame: LocalFrameEnvironment) : StaticEnvironment(parent) {
    override fun newBinding(name: String, type: Type): Binding.Local = frame.newBinding(name, type)
    override fun newScope() = LocalFrameChildScope(this, frame)
}
