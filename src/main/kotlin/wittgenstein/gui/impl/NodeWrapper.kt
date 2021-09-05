package wittgenstein.gui.impl

import javafx.scene.Node
import javafx.scene.control.Control
import javafx.scene.control.Skin

abstract class NodeWrapper<R: Node> : Control() {
    lateinit var root: R
        private set

    protected fun setRoot(node: R) {
        root = node
        skin = null
        skin = object : Skin<NodeWrapper<R>> {
            override fun getSkinnable(): NodeWrapper<R> = this@NodeWrapper

            override fun getNode(): Node = node

            override fun dispose() {}
        }
    }

    override fun toString(): String = "NodeWrapper [ root = $root ]"
}